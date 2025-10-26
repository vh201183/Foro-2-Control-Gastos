@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.composegrades.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api

@Composable
fun LoginScreen(
    state: LoginState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginEmail: () -> Unit,
    onRegister: () -> Unit,
    onLoginGoogle: (Boolean) -> Unit,
) {
    val activity = LocalContext.current as ComponentActivity
    val vm = androidx.lifecycle.viewmodel.compose.viewModel<LoginVM>()

    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        vm.handleGoogleResult(res.resultCode, res.data)
    }
    DisposableEffect(Unit) {
        vm.pendingGoogleLaunch = {
            val intent = AuthRepository().buildGoogleIntent(activity)
            googleLauncher.launch(intent)
        }
        onDispose { vm.pendingGoogleLaunch = null }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Control de Gastos", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(value = state.email, onValueChange = onEmailChange, label = { Text("Correo") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            OutlinedTextField(value = state.password, onValueChange = onPasswordChange, label = { Text("Contraseña") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation())
            if (state.error != null) Text(state.error!!, color = MaterialTheme.colorScheme.error)
            Button(onClick = onLoginEmail, modifier = Modifier.fillMaxWidth(), enabled = !state.loading) { Text("Iniciar sesión") }
            OutlinedButton(onClick = onRegister, modifier = Modifier.fillMaxWidth(), enabled = !state.loading) { Text("Crear cuenta") }
            Divider(Modifier.padding(vertical = 8.dp))
            Button(onClick = { vm.loginWithGoogle { ok -> onLoginGoogle(ok) } }, modifier = Modifier.fillMaxWidth(), enabled = !state.loading) { Text("Iniciar con Google") }
        }
    }
}
