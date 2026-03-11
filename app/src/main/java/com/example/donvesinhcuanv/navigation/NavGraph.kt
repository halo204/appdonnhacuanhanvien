package com.example.donvesinhcuanv.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.donvesinhcuanv.screens.*
import com.example.donvesinhcuanv.viewmodel.AuthViewModel
import com.example.donvesinhcuanv.viewmodel.JobViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object ForgotPassword : Screen("forgot_password")
    object RegistrationMethod : Screen("registration_method")
    object EmailRegister : Screen("email_register")
    object PhoneRegister : Screen("phone_register")
    object Home : Screen("home")
    object JobList : Screen("job_list")
    object MyJobs : Screen("my_jobs")
    object Profile : Screen("profile")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    jobViewModel: JobViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.RegistrationMethod.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }
        
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onResetSuccess = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.RegistrationMethod.route) {
            RegistrationMethodScreen(
                onNavigateToEmailRegister = {
                    navController.navigate(Screen.EmailRegister.route)
                },
                onNavigateToPhoneRegister = {
                    navController.navigate(Screen.PhoneRegister.route)
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.EmailRegister.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.RegistrationMethod.route) { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.PhoneRegister.route) {
            PhoneRegisterScreen(
                authViewModel = authViewModel,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.RegistrationMethod.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = jobViewModel,
                onNavigateToJobList = { navController.navigate(Screen.JobList.route) }
            )
        }
        
        composable(Screen.JobList.route) {
            JobListScreen(viewModel = jobViewModel)
        }
        
        composable(Screen.MyJobs.route) {
            MyJobsScreen(viewModel = jobViewModel)
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = jobViewModel,
                onLogout = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
