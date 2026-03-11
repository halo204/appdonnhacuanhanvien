package com.example.donvesinhcuanv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.donvesinhcuanv.navigation.NavGraph
import com.example.donvesinhcuanv.navigation.Screen
import com.example.donvesinhcuanv.ui.theme.DonvesinhcuanvTheme
import com.example.donvesinhcuanv.viewmodel.AuthViewModel
import com.example.donvesinhcuanv.viewmodel.JobViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DonvesinhcuanvTheme {
                WorkerApp()
            }
        }
    }
}

@Composable
fun WorkerApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val jobViewModel: JobViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    
    // Auto navigate to Home if logged in
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && currentRoute == Screen.Login.route) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }
    
    // Ẩn bottom bar ở màn hình login và register
    val showBottomBar = currentRoute !in listOf(
        Screen.Login.route,
        Screen.ForgotPassword.route,
        Screen.RegistrationMethod.route,
        Screen.EmailRegister.route,
        Screen.PhoneRegister.route
    )
    
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    contentColor = Color(0xFF4CAF50)
                ) {
                    val currentDestination = navBackStackEntry?.destination
                    
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF4CAF50),
                                selectedTextColor = Color(0xFF4CAF50),
                                indicatorColor = Color(0xFFE8F5E9),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            authViewModel = authViewModel,
            jobViewModel = jobViewModel,
            modifier = if (showBottomBar) Modifier.padding(innerPadding) else Modifier
        )
    }
}

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, Icons.Default.Home, "Trang chủ"),
    BottomNavItem(Screen.JobList.route, Icons.Default.Build, "Dịch vụ"),
    BottomNavItem(Screen.MyJobs.route, Icons.Default.DateRange, "Theo dõi"),
    BottomNavItem(Screen.Profile.route, Icons.Default.Person, "Hồ sơ")
)