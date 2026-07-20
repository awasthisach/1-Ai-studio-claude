package com.vvf.smartmanager.core.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vvf.smartmanager.presentation.duplicate.DuplicateScreen
import com.vvf.smartmanager.presentation.filemanager.FileManagerScreen
import com.vvf.smartmanager.presentation.search.SearchScreen
import com.vvf.smartmanager.presentation.storage.StorageScreen
import com.vvf.smartmanager.presentation.vault.VaultScreen

@Composable
fun VvfNavHost() {
    val navController = rememberNavController()

    Scaffold(
        // Deliberately NOT the default (WindowInsets.safeDrawing) — audit finding, 19 July
        // 2026: with the default, this outer Scaffold's innerPadding AND FileManagerScreen's
        // own nested Scaffold would both apply top status-bar inset padding, double-counting
        // it and pushing FileManagerScreen's TopAppBar down further than the other 4 tabs.
        // Material3's NavigationBar below already self-handles the bottom system-bar inset,
        // so this Scaffold only needs to reserve space for its own bottomBar's height, not
        // system insets on top of that.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                VvfDestination.bottomBarItems.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.labelRes)) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = VvfDestination.Files.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(VvfDestination.Files.route) { FileManagerScreen() }
            composable(VvfDestination.Search.route) { SearchScreen() }
            composable(VvfDestination.Vault.route) { VaultScreen() }
            composable(VvfDestination.Storage.route) { StorageScreen() }
            composable(VvfDestination.Duplicates.route) { DuplicateScreen() }
        }
    }
}
