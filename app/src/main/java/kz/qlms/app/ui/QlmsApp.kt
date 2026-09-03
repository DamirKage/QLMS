package kz.qlms.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kz.qlms.app.core.rememberAppContainer
import kz.qlms.app.ui.navigation.QlmsBottomBar
import kz.qlms.app.ui.navigation.QlmsNavGraph
import kz.qlms.app.ui.navigation.Screen
import kz.qlms.app.ui.navigation.bottomNavScreens

private sealed class StartState {
    data object Loading : StartState()
    data class Ready(val route: String) : StartState()
}

@Composable
fun QlmsApp(deepLinkIncidentId: String? = null) {
    val container = rememberAppContainer()

    val startState by produceState<StartState>(initialValue = StartState.Loading) {
        container.settingsDataStore.onboardingDoneFlow
            .combine(container.authRepository.authStateFlow().map { it != null }) { onboardingDone, loggedIn ->
                when {
                    !onboardingDone -> Screen.Onboarding.route
                    !loggedIn -> Screen.Login.route
                    else -> Screen.Home.route
                }
            }
            .collect { value = StartState.Ready(it) }
    }

    val state = startState
    if (state is StartState.Ready) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val showBottomBar = bottomNavScreens.any { screen ->
            backStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true
        }

        Scaffold(
            bottomBar = { if (showBottomBar) QlmsBottomBar(navController) },
        ) { padding ->
            QlmsNavGraph(
                navController = navController,
                startDestination = state.route,
                deepLinkIncidentId = deepLinkIncidentId,
                modifier = Modifier.padding(padding),
            )
        }
    }
}
