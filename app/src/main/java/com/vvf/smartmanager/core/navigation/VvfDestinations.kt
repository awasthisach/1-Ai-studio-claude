package com.vvf.smartmanager.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.vvf.smartmanager.R

sealed class VvfDestination(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    data object Files : VvfDestination("files", R.string.nav_files, Icons.Filled.Folder)
    data object Search : VvfDestination("search", R.string.nav_search, Icons.Filled.Search)
    data object Vault : VvfDestination("vault", R.string.nav_vault, Icons.Filled.Lock)
    data object Storage : VvfDestination("storage", R.string.nav_storage, Icons.Filled.PieChart)
    data object Duplicates : VvfDestination("duplicates", R.string.nav_duplicates, Icons.Filled.Delete)

    companion object {
        val bottomBarItems = listOf(Files, Search, Duplicates, Vault, Storage)
    }
}
