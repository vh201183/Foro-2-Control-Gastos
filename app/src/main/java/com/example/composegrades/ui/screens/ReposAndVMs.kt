
package com.example.composegrades.ui.screens

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.Calendar
import kotlinx.coroutines.channels.awaitClose


data class Expense(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val date: Long = System.currentTimeMillis()
)

suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) cont.resume(task.result) {}
        else cont.resumeWithException(task.exception ?: RuntimeException("Task failed"))
    }
}

class AuthRepository {
    private val auth = Firebase.auth
    fun currentUid(): String? = auth.currentUser?.uid
    suspend fun signInEmail(email: String, pass: String) { auth.signInWithEmailAndPassword(email, pass).await() }
    suspend fun registerEmail(email: String, pass: String) { auth.createUserWithEmailAndPassword(email, pass).await() }
    fun signOut() { auth.signOut() }
    fun buildGoogleIntent(activity: ComponentActivity): android.content.Intent {
        val clientId = activity.getString(com.example.composegrades.R.string.default_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(clientId).requestEmail().build()
        val client = GoogleSignIn.getClient(activity, gso)
        return client.signInIntent
    }
    suspend fun signInWithGoogle(idToken: String) {
        val cred = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(cred).await()
    }
}

class ExpenseRepository {
    private val db = Firebase.firestore
    private fun col(uid: String) = db.collection("users").document(uid).collection("expenses")
    fun observeMonth(uid: String, start: Long, end: Long) = kotlinx.coroutines.flow.callbackFlow<List<Expense>> {
        val reg = col(uid).whereGreaterThanOrEqualTo("date", start).whereLessThan("date", end)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val list = snap?.documents.orEmpty().map { it.toExpense() }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }
    suspend fun upsert(uid: String, e: Expense) {
        if (e.id.isBlank()) col(uid).add(e.copy(userId = uid)).await()
        else col(uid).document(e.id).set(e.copy(userId = uid)).await()
    }
    suspend fun delete(uid: String, id: String) { col(uid).document(id).delete().await() }
}

private fun DocumentSnapshot.toExpense(): Expense =
    Expense(
        id = id,
        userId = getString("userId") ?: "",
        name = getString("name") ?: "",
        amount = getDouble("amount") ?: 0.0,
        category = getString("category") ?: "",
        date = getLong("date") ?: 0L
    )

data class LoginState(val email: String = "", val password: String = "", val error: String? = null, val loading: Boolean = false)

class LoginVM : ViewModel() {
    private val repo = AuthRepository()
    var state by mutableStateOf(LoginState()); private set
    fun onEmailChange(v: String) { state = state.copy(email = v, error = null) }
    fun onPasswordChange(v: String) { state = state.copy(password = v, error = null) }
    fun registerWithEmail(onSuccess: () -> Unit, onError: (Throwable) -> Unit) = viewModelScope.launch {
        try { state = state.copy(loading = true); repo.registerEmail(state.email, state.password); onSuccess() }
        catch (t: Throwable) { state = state.copy(error = t.message); onError(t) }
        finally { state = state.copy(loading = false) }
    }
    fun loginWithEmail(onSuccess: () -> Unit, onError: (Throwable) -> Unit) = viewModelScope.launch {
        try { state = state.copy(loading = true); repo.signInEmail(state.email, state.password); onSuccess() }
        catch (t: Throwable) { state = state.copy(error = t.message); onError(t) }
        finally { state = state.copy(loading = false) }
    }
    fun loginWithGoogle(callback: (Boolean) -> Unit) {
        pendingGoogleCallback = callback
        pendingGoogleLaunch?.invoke()
    }
    private var pendingGoogleCallback: ((Boolean) -> Unit)? = null
    var pendingGoogleLaunch: (() -> Unit)? = null
    fun handleGoogleResult(resultCode: Int, data: android.content.Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                val token = account.idToken ?: ""
                viewModelScope.launch {
                    try { repo.signInWithGoogle(token); pendingGoogleCallback?.invoke(true) }
                    catch (_: Throwable) { pendingGoogleCallback?.invoke(false) }
                }
            } catch (_: Exception) { pendingGoogleCallback?.invoke(false) }
        } else pendingGoogleCallback?.invoke(false)
    }
}

data class ExpensesState(
    val items: List<Expense> = emptyList(),
    val monthMillis: Long = System.currentTimeMillis(),
    val total: Double = 0.0,
    val current: Expense = Expense(),
    val error: String? = null
)

class ExpensesVM : ViewModel() {
    private val auth = AuthRepository()
    private val repo = ExpenseRepository()
    var state by mutableStateOf(ExpensesState()); private set
    init { auth.currentUid()?.let { observeMonth(it, state.monthMillis) } }
    private fun monthRange(millis: Long): Pair<Long, Long> {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = millis; set(java.util.Calendar.DAY_OF_MONTH,1)
            set(java.util.Calendar.HOUR_OF_DAY,0); set(java.util.Calendar.MINUTE,0); set(java.util.Calendar.SECOND,0); set(java.util.Calendar.MILLISECOND,0)
        }
        val start = cal.timeInMillis; cal.add(java.util.Calendar.MONTH,1); val end = cal.timeInMillis; return start to end
    }
    private fun observeMonth(uid: String, millis: Long) {
        val (start, end) = monthRange(millis)
        viewModelScope.launch {
            repo.observeMonth(uid, start, end).collectLatest { list ->
                state = state.copy(items = list, total = list.sumOf { it.amount })
            }
        }
    }
    fun setMonth(millis: Long) { state = state.copy(monthMillis = millis); auth.currentUid()?.let { observeMonth(it, millis) } }
    fun onCurrentChange(name: String? = null, amount: String? = null, category: String? = null, date: Long? = null) {
        val cur = state.current
        val amt = amount?.toDoubleOrNull() ?: cur.amount
        state = state.copy(current = cur.copy(name = name ?: cur.name, amount = amt, category = category ?: cur.category, date = date ?: cur.date))
    }
    fun startAdd() { state = state.copy(current = Expense()) }
    fun startEdit(e: Expense) { state = state.copy(current = e) }
    fun saveCurrent() {
        val uid = auth.currentUid() ?: return
        val e = state.current
        if (e.name.isBlank() || e.amount <= 0.0 || e.category.isBlank()) { state = state.copy(error = "Completa nombre, monto (>0) y categoría."); return }
        viewModelScope.launch {
            try { repo.upsert(uid, e); state = state.copy(current = Expense(), error = null) }
            catch (t: Throwable) { state = state.copy(error = t.message) }
        }
    }
    fun deleteExpense(e: Expense) {
        val uid = auth.currentUid() ?: return
        viewModelScope.launch { try { repo.delete(uid, e.id) } catch (t: Throwable) { state = state.copy(error = t.message) } }
    }
    fun logout() { auth.signOut() }
}
