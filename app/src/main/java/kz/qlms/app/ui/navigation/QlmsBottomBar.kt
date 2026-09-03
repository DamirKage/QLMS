package kz.qlms.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
import kz.qlms.app.R

private data class BottomItem(val screen: Screen, val labelRes: Int, val filledIcon: androidx.compose.ui.graphics.vector.ImageVector, val outlinedIcon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun QlmsBottomBar(navController: NavHostController) {
    val items = listOf(
        BottomItem(Screen.Home, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
        BottomItem(Screen.Feed, R.string.nav_feed, Icons.Filled.Notifications, Icons.Outlined.Notifications),
        BottomItem(Screen.History, R.string.nav_history, Icons.Filled.History, Icons.Outlined.History),
        BottomItem(Screen.Profile, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person),
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(if (selected) item.filledIcon else item.outlinedIcon, contentDescription = null) },
                label = { Text(stringResource(item.labelRes)) },
            )
        }
    }
}
