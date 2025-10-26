
package com.example.composegrades.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.composegrades.ui.screens.*

sealed class Route(val route: String) {
    data object Login: Route("login")
    data object Home: Route("home")
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Route.Login.route) {
        composable(Route.Login.route) {
            val vm: LoginVM = viewModel()
            LoginScreen(
                state = vm.state,
                onEmailChange = vm::onEmailChange,
                onPasswordChange = vm::onPasswordChange,
                onLoginEmail = { vm.loginWithEmail(onSuccess = { nav.navigate(Route.Home.route) }, onError = {}) },
                onRegister = { vm.registerWithEmail(onSuccess = { nav.navigate(Route.Home.route) }, onError = {}) },
                onLoginGoogle = { ok -> if (ok) nav.navigate(Route.Home.route) }
            )
        }
        composable(Route.Home.route) {
            val vm: ExpensesVM = viewModel()
            HomeScreen(
                state = vm.state,
                onAddClick = vm::startAdd,
                onEditClick = { vm.startEdit(it) },
                onDeleteClick = vm::deleteExpense,
                onMonthChange = vm::setMonth,
                onSave = vm::saveCurrent,
                onFieldChange = vm::onCurrentChange,
                onLogout = { vm.logout(); nav.navigate(Route.Login.route) { popUpTo(Route.Login.route) { inclusive = true } } }
            )
        }
    }
}
