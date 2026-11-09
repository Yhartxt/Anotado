package com.example.taskmanager.ui.navigation

import android.app.Activity
import com.yharim.anotado.ui.task.TaskFormScreen
import com.yharim.anotado.ui.task.TaskViewModel
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.ViewModelProvider
import com.yharim.anotado.ui.home.HomeViewModel
import com.yharim.anotado.ui.home.HomeScreen
import com.yharim.anotado.ui.auth.register.RegisterScreen
import com.yharim.anotado.ui.auth.register.RegisterViewModel
import com.example.taskmanager.ui.auth.login.LoginScreen
import com.example.taskmanager.ui.auth.login.LoginViewModel

@Composable
fun AppNavigation(appViewModel: AppViewModel, factory: ViewModelProvider.Factory) {
    val state by appViewModel.uiState.collectAsStateWithLifecycle()
    val authenticated = state.isAuthenticated

    key(authenticated) {
        val navController = rememberNavController()
        val entry by navController.currentBackStackEntryAsState()
        val view = LocalView.current
        val currentRoute = entry?.destination?.route ?: if (authenticated) Routes.HOME else Routes.LOGIN
        val lightStatusBar = currentRoute != Routes.LOGIN && currentRoute != Routes.CREATE_TASK
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = lightStatusBar
            }
        }
        NavHost(navController, startDestination = if (authenticated) Routes.HOME else Routes.LOGIN) {
            composable(Routes.LOGIN) {
                val loginViewModel: LoginViewModel = viewModel(factory = factory)
                LoginScreen(
                    loginViewModel,
                    onNavigateToRegister = { navController.navigate(Routes.REGISTER) { launchSingleTop = true } }
                )
            }
            composable(Routes.REGISTER) {
                val registerViewModel: RegisterViewModel = viewModel(factory = factory)
                RegisterScreen(registerViewModel, onNavigateToLogin = { navController.popBackStack() })
            }
            composable(Routes.CREATE_TASK) { formEntry ->
                val taskViewModel: TaskViewModel = viewModel(factory = factory)
                val homeEntry = remember(formEntry) { navController.getBackStackEntry(Routes.HOME) }
                val homeViewModel: HomeViewModel = viewModel(viewModelStoreOwner = homeEntry, factory = factory)
                TaskFormScreen(taskViewModel,
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        homeViewModel.onTaskCreated()
                        navController.popBackStack()
                    })
            }
            composable(Routes.HOME) {
                val homeViewModel: HomeViewModel = viewModel(factory = factory)
                HomeScreen(homeViewModel, onNavigateToCreate = {
                    navController.navigate(Routes.CREATE_TASK) { launchSingleTop = true }
                })
            }
        }
    }
}
