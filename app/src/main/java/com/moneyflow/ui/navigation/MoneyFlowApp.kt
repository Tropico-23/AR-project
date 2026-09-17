package com.moneyflow.ui.navigation

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.moneyflow.MoneyFlowApplication
import com.moneyflow.ui.components.AddExpenseSheet
import com.moneyflow.ui.components.ConfirmDeleteDialog
import com.moneyflow.ui.screens.BudgetScreen
import com.moneyflow.ui.screens.HomeScreen
import com.moneyflow.ui.screens.InsightsScreen
import com.moneyflow.ui.screens.SettingsScreen
import com.moneyflow.ui.screens.TransactionsScreen
import com.moneyflow.viewmodel.BudgetViewModel
import com.moneyflow.viewmodel.HomeViewModel
import com.moneyflow.viewmodel.InsightsViewModel
import com.moneyflow.viewmodel.SettingsViewModel
import com.moneyflow.viewmodel.TransactionsViewModel
import com.moneyflow.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyFlowApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = (context as Activity).application as MoneyFlowApplication
    val factory = remember { ViewModelFactory(app.appContainer.repository, app.appContainer.preferencesRepository) }

    val homeViewModel: HomeViewModel = viewModel(factory = factory)
    val transactionsViewModel: TransactionsViewModel = viewModel(factory = factory)
    val insightsViewModel: InsightsViewModel = viewModel(factory = factory)
    val budgetViewModel: BudgetViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

    val homeState by homeViewModel.uiState
    val txState by transactionsViewModel.uiState
    val insightsState by insightsViewModel.uiState
    val budgetState by budgetViewModel.uiState
    val settingsState by settingsViewModel.uiState

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddSheet by remember { mutableStateOf(false) }
    var editExpense by remember { mutableStateOf<com.moneyflow.model.Expense?>(null) }
    var pendingDelete by remember { mutableStateOf<com.moneyflow.model.Expense?>(null) }

    val bottomItems = listOf(NavRoutes.Home, NavRoutes.Transactions, NavRoutes.Insights, NavRoutes.Budget)
    val icons = mapOf(
        NavRoutes.Home.route to Icons.Outlined.Home,
        NavRoutes.Transactions.route to Icons.Outlined.List,
        NavRoutes.Insights.route to Icons.Outlined.BarChart,
        NavRoutes.Budget.route to Icons.Outlined.Wallet,
        NavRoutes.Settings.route to Icons.Outlined.Settings
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (navController.currentBackStackEntryAsState().value?.destination?.route != NavRoutes.Settings.route) {
                FloatingActionButton(onClick = { showAddSheet = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add expense")
                }
            }
        },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            NavigationBar {
                bottomItems.forEach { route ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == route.route } == true,
                        onClick = {
                            navController.navigate(route.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icons[route.route]!!, contentDescription = route.label) },
                        label = { Text(route.label) }
                    )
                }
                NavigationBarItem(
                    selected = currentDestination?.hierarchy?.any { it.route == NavRoutes.Settings.route } == true,
                    onClick = { navController.navigate(NavRoutes.Settings.route) },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        AppNavHost(
            navController = navController,
            paddingValues = padding,
            homeState = homeState,
            txState = txState,
            insightsState = insightsState,
            budgetState = budgetState,
            settingsState = settingsState,
            onNavigateToTransactions = { navController.navigate(NavRoutes.Transactions.route) },
            onTxQuery = transactionsViewModel::updateQuery,
            onTxCategory = transactionsViewModel::setCategory,
            onTxPayment = transactionsViewModel::setPaymentMethod,
            onTxSort = transactionsViewModel::setSortOrder,
            onTxSelect = transactionsViewModel::selectExpense,
            onTxDelete = { pendingDelete = it },
            onTxEditSave = transactionsViewModel::updateExpense,
            onStartEdit = { editExpense = it; showAddSheet = true },
            onRangeChanged = insightsViewModel::setRange,
            onSetMonthlyBudget = budgetViewModel::setMonthlyBudget,
            onSetCategoryBudget = budgetViewModel::setCategoryBudget,
            onThemeChanged = settingsViewModel::setTheme,
            onCurrencyChanged = settingsViewModel::setCurrency,
            onDefaultPayment = settingsViewModel::setDefaultPayment,
            onWeekStartChanged = settingsViewModel::setWeekStart,
            onClearData = settingsViewModel::clearAllData,
            onInsertDemoData = settingsViewModel::insertDemoData,
            onExportCsv = settingsViewModel::exportCsv,
            onImportCsv = settingsViewModel::importCsv,
            contentResolver = context.contentResolver
        )
    }

    if (showAddSheet) {
        AddExpenseSheet(
            initialExpense = editExpense,
            defaultPaymentMethod = settingsState.preferences.defaultPaymentMethod,
            onDismiss = {
                showAddSheet = false
                editExpense = null
            },
            onSave = { expense ->
                if (editExpense == null) {
                    homeViewModel.addExpense(expense) {
                        scope.launch { snackbarHostState.showSnackbar("Expense saved") }
                    }
                } else {
                    transactionsViewModel.updateExpense(expense)
                }
                showAddSheet = false
                editExpense = null
            }
        )
    }

    pendingDelete?.let { expense ->
        ConfirmDeleteDialog(
            onDismiss = { pendingDelete = null },
            onConfirm = {
                transactionsViewModel.deleteExpense(expense)
                pendingDelete = null
            }
        )
    }
}

@Composable
private fun AppNavHost(
    navController: androidx.navigation.NavHostController,
    paddingValues: PaddingValues,
    homeState: com.moneyflow.viewmodel.HomeUiState,
    txState: com.moneyflow.viewmodel.TransactionsUiState,
    insightsState: com.moneyflow.viewmodel.InsightsUiState,
    budgetState: com.moneyflow.viewmodel.BudgetUiState,
    settingsState: com.moneyflow.viewmodel.SettingsUiState,
    onNavigateToTransactions: () -> Unit,
    onTxQuery: (String) -> Unit,
    onTxCategory: (com.moneyflow.model.ExpenseCategory?) -> Unit,
    onTxPayment: (com.moneyflow.model.PaymentMethod?) -> Unit,
    onTxSort: (com.moneyflow.model.SortOrder) -> Unit,
    onTxSelect: (com.moneyflow.model.Expense?) -> Unit,
    onTxDelete: (com.moneyflow.model.Expense) -> Unit,
    onTxEditSave: (com.moneyflow.model.Expense) -> Unit,
    onStartEdit: (com.moneyflow.model.Expense) -> Unit,
    onRangeChanged: (com.moneyflow.model.TrendRange) -> Unit,
    onSetMonthlyBudget: (Long) -> Unit,
    onSetCategoryBudget: (com.moneyflow.model.ExpenseCategory, Long) -> Unit,
    onThemeChanged: (com.moneyflow.model.ThemeMode) -> Unit,
    onCurrencyChanged: (com.moneyflow.model.AppCurrency) -> Unit,
    onDefaultPayment: (com.moneyflow.model.PaymentMethod) -> Unit,
    onWeekStartChanged: (com.moneyflow.model.WeekStart) -> Unit,
    onClearData: () -> Unit,
    onInsertDemoData: () -> Unit,
    onExportCsv: (android.net.Uri, android.content.ContentResolver, List<com.moneyflow.model.Expense>) -> Unit,
    onImportCsv: (android.net.Uri, android.content.ContentResolver) -> Unit,
    contentResolver: android.content.ContentResolver
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Home.route,
        modifier = Modifier.padding(paddingValues)
    ) {
        composable(NavRoutes.Home.route) {
            AnimatedContent(targetState = homeState, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "home") {
                HomeScreen(state = it, onSeeAll = onNavigateToTransactions)
            }
        }
        composable(NavRoutes.Transactions.route) {
            TransactionsScreen(
                state = txState,
                onQueryChange = onTxQuery,
                onCategoryChange = onTxCategory,
                onPaymentMethodChange = onTxPayment,
                onSortChange = onTxSort,
                onSelectExpense = onTxSelect,
                onDeleteExpense = onTxDelete,
                onEditExpense = onTxEditSave,
                onStartEdit = onStartEdit
            )
        }
        composable(NavRoutes.Insights.route) { InsightsScreen(state = insightsState, onRangeChanged = onRangeChanged) }
        composable(NavRoutes.Budget.route) {
            BudgetScreen(
                state = budgetState,
                onSetMonthlyBudget = onSetMonthlyBudget,
                onSetCategoryBudget = onSetCategoryBudget
            )
        }
        composable(NavRoutes.Settings.route) {
            SettingsScreen(
                state = settingsState,
                allExpenses = homeState.expenses,
                contentResolver = contentResolver,
                onThemeChanged = onThemeChanged,
                onCurrencyChanged = onCurrencyChanged,
                onDefaultPaymentChanged = onDefaultPayment,
                onWeekStartChanged = onWeekStartChanged,
                onClearData = onClearData,
                onInsertDemoData = onInsertDemoData,
                onExportCsv = onExportCsv,
                onImportCsv = onImportCsv
            )
        }
    }
}
