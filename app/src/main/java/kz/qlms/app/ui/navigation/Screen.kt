package kz.qlms.app.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")

    data object Home : Screen("home")
    data object Feed : Screen("feed")
    data object History : Screen("history")
    data object Profile : Screen("profile")

    data object ReportIncident : Screen("report_incident")
    data object MedicalInfo : Screen("medical_info")
    data object EmergencyContacts : Screen("emergency_contacts")
    data object Settings : Screen("settings")
    data object CheckIn : Screen("check_in")

    data object IncidentDetail : Screen("incident_detail/{incidentId}") {
        fun createRoute(incidentId: String) = "incident_detail/$incidentId"
        const val ARG_INCIDENT_ID = "incidentId"
    }
}

/** The four bottom-navigation destinations, in display order. */
val bottomNavScreens = listOf(Screen.Home, Screen.Feed, Screen.History, Screen.Profile)
