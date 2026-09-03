package kz.qlms.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kz.qlms.app.ui.auth.ForgotPasswordScreen
import kz.qlms.app.ui.auth.LoginScreen
import kz.qlms.app.ui.auth.RegisterScreen
import kz.qlms.app.ui.checkin.CheckInScreen
import kz.qlms.app.ui.contacts.EmergencyContactsScreen
import kz.qlms.app.ui.feed.FeedScreen
import kz.qlms.app.ui.history.HistoryScreen
import kz.qlms.app.ui.home.HomeScreen
import kz.qlms.app.ui.medical.MedicalInfoScreen
import kz.qlms.app.ui.onboarding.OnboardingScreen
import kz.qlms.app.ui.profile.IncidentDetailScreen
import kz.qlms.app.ui.profile.ProfileScreen
import kz.qlms.app.ui.report.ReportIncidentScreen
import kz.qlms.app.ui.settings.SettingsScreen

@Composable
fun QlmsNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String,
    deepLinkIncidentId: String?,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(deepLinkIncidentId, startDestination) {
        if (deepLinkIncidentId != null && startDestination == Screen.Home.route) {
            navController.navigate(Screen.IncidentDetail.createRoute(deepLinkIncidentId))
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            if (isTabSwitch()) fadeIn(tween(160)) else slideInHorizontally(tween(220)) { it / 4 } + fadeIn(tween(220))
        },
        exitTransition = {
            if (isTabSwitch()) fadeOut(tween(120)) else fadeOut(tween(140))
        },
        popEnterTransition = { fadeIn(tween(180)) },
        popExitTransition = {
            slideOutHorizontally(tween(220)) { it / 4 } + fadeOut(tween(220))
        },
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onFinished = { navController.navigateAndClear(Screen.Login.route) })
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { navController.navigateAndClear(Screen.Home.route) },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { navController.navigateAndClear(Screen.Home.route) },
                onNavigateToLogin = { navController.popBackStack() },
            )
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToReport = { navController.navigate(Screen.ReportIncident.route) },
                onNavigateToCheckIn = { navController.navigate(Screen.CheckIn.route) },
                onIncidentClick = { id -> navController.navigate(Screen.IncidentDetail.createRoute(id)) },
            )
        }
        composable(Screen.Feed.route) {
            FeedScreen(onIncidentClick = { id -> navController.navigate(Screen.IncidentDetail.createRoute(id)) })
        }
        composable(Screen.History.route) {
            HistoryScreen(onIncidentClick = { id -> navController.navigate(Screen.IncidentDetail.createRoute(id)) })
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToMedical = { navController.navigate(Screen.MedicalInfo.route) },
                onNavigateToContacts = { navController.navigate(Screen.EmergencyContacts.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onSignOut = { navController.navigateAndClear(Screen.Login.route) },
            )
        }

        composable(Screen.ReportIncident.route) {
            ReportIncidentScreen(
                onSubmitted = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.MedicalInfo.route) {
            MedicalInfoScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.EmergencyContacts.route) {
            EmergencyContactsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignedOut = { navController.navigateAndClear(Screen.Login.route) },
            )
        }
        composable(Screen.CheckIn.route) {
            CheckInScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.IncidentDetail.route,
            arguments = listOf(navArgument(Screen.IncidentDetail.ARG_INCIDENT_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString(Screen.IncidentDetail.ARG_INCIDENT_ID).orEmpty()
            IncidentDetailScreen(incidentId = id, onNavigateBack = { navController.popBackStack() })
        }
    }
}

private fun NavHostController.navigateAndClear(route: String) {
    navigate(route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

/** Peer tabs (Home/Feed/History/Profile) crossfade; everything else is a "push" and slides. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean {
    val fromRoute = initialState.destination.route
    val toRoute = targetState.destination.route
    return bottomNavScreens.any { it.route == fromRoute } && bottomNavScreens.any { it.route == toRoute }
}
