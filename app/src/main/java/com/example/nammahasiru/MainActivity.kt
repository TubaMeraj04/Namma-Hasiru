package com.example.nammahasiru

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nammahasiru.data.TreeDatabase
import com.example.nammahasiru.ui.screens.DashboardScreen
import com.example.nammahasiru.ui.screens.GuideScreen
import com.example.nammahasiru.ui.screens.LoginScreen
import com.example.nammahasiru.ui.screens.MapScreen
import com.example.nammahasiru.ui.screens.PlantScreen
import com.example.nammahasiru.ui.theme.NammaHasiruTheme
import androidx.compose.ui.platform.LocalContext

import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NammaHasiruTheme {
                MainScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: @Composable () -> Unit) {
    object Dashboard : Screen("dashboard", "Home", { Icon(Icons.Filled.Home, contentDescription = null) })
    object Map : Screen("map", "Map", { Icon(Icons.Filled.LocationOn, contentDescription = null) })
    object Plant : Screen("plant", "Plant", { Icon(Icons.Filled.Add, contentDescription = null) })
    object Guide : Screen("guide", "Guide", { Icon(Icons.Filled.Info, contentDescription = null) })
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val database = TreeDatabase.getDatabase(context)
    val viewModel: TreeViewModel = viewModel(
        factory = TreeViewModelFactory(database.treeDao())
    )

    val items = listOf(
        Screen.Dashboard,
        Screen.Map,
        Screen.Plant,
        Screen.Guide
    )

    val startDestination = remember {
        if (FirebaseAuth.getInstance().currentUser != null) Screen.Dashboard.route else "login"
    }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            if (currentDestination?.route != "login") {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = screen.icon,
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                if (currentDestination?.route != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = startDestination, Modifier.padding(innerPadding)) {
            composable("login") { LoginScreen(navController) }
            composable(Screen.Dashboard.route) { DashboardScreen(navController, viewModel) }
            composable(Screen.Map.route) { MapScreen(navController, viewModel) }
            composable(Screen.Plant.route + "?treeId={treeId}") { backStackEntry ->
                val treeId = backStackEntry.arguments?.getString("treeId")?.toIntOrNull()
                PlantScreen(navController, viewModel, treeId)
            }
            composable(Screen.Plant.route) { PlantScreen(navController, viewModel) }
            composable(Screen.Guide.route) { GuideScreen() }
        }
    }
}
