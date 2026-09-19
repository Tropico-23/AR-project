package com.tropico.moneyflow.ui.navigation

sealed class NavRoutes(val route: String, val label: String) {
    data object Home : NavRoutes("home", "Home")
    data object Transactions : NavRoutes("transactions", "Transactions")
    data object Insights : NavRoutes("insights", "Insights")
    data object Budget : NavRoutes("budget", "Budget")
    data object Settings : NavRoutes("settings", "Settings")
}
