package com.tropico.moneyflow

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF15161A)
private val Muted = Color(0xFF777A85)
private val Surface = Color(0xFFF7F7FA)
private val Card = Color.White
private val Accent = Color(0xFF6658D3)
private val AccentSoft = Color(0xFFEDEBFF)
private val Green = Color(0xFF159A68)
private val Red = Color(0xFFD9536A)

private data class Expense(val id: Long, val amount: Double, val title: String, val category: String, val date: String, val note: String, val payment: String)
private val categories = listOf("Food", "Transport", "Shopping", "Bills", "Study", "Health", "Fun", "Other")
private val categoryEmoji = mapOf("Food" to "🍔", "Transport" to "🚕", "Shopping" to "🛍️", "Bills" to "🧾", "Study" to "📚", "Health" to "💊", "Fun" to "🎮", "Other" to "✨")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { MoneyFlowApp() } }
}

@Composable
private fun MoneyFlowApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var expenses by remember { mutableStateOf(loadExpenses(context)) }
    var budget by remember { mutableStateOf(loadBudget(context)) }
    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var showBudget by remember { mutableStateOf(false) }
    var dark by remember { mutableStateOf(false) }
    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Color(0xFFB9AEFF)) else lightColorScheme(primary = Accent)) {
        Surface(Modifier.fillMaxSize(), color = if (dark) Color(0xFF111217) else Surface) {
            Scaffold(containerColor = Color.Transparent, topBar = { TopBar(tab, dark) { dark = !dark } }, bottomBar = { BottomBar(tab) { tab = it } }, floatingActionButton = {
                if (tab == 0 || tab == 1) FloatingActionButton(onClick = { showAdd = true }, containerColor = Accent, contentColor = Color.White) { Icon(Icons.Default.Add, "Add expense") }
            }) { padding -> Box(Modifier.padding(padding)) {
                when (tab) {
                    0 -> Dashboard(expenses, budget, { showAdd = true }, { showBudget = true })
                    1 -> Transactions(expenses) { id -> expenses = expenses.filterNot { it.id == id }; saveExpenses(context, expenses) }
                    2 -> Analytics(expenses)
                    3 -> BudgetScreen(expenses, budget) { showBudget = true }
                }
            } }
        }
    }
    if (showAdd) AddExpenseDialog({ showAdd = false }) { expense -> expenses = listOf(expense) + expenses; saveExpenses(context, expenses); showAdd = false }
    if (showBudget) BudgetDialog(budget, { showBudget = false }) { value -> budget = value; saveBudget(context, value); showBudget = false }
}

@Composable private fun TopBar(tab: Int, dark: Boolean, toggleDark: () -> Unit) {
    val titles = listOf("Good morning 👋", "Transactions", "Insights", "Your budget")
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text("MoneyFlow", 13.sp, color = Accent, fontWeight = FontWeight.Bold); Text(titles[tab], 23.sp, fontWeight = FontWeight.Bold, color = if (dark) Color.White else Ink) }
        IconButton(onClick = toggleDark) { Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, "Theme") }
    }
}

@Composable private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = Card) {
        val items = listOf(Icons.Default.Home to "Home", Icons.Default.ReceiptLong to "Activity", Icons.Default.BarChart to "Insights", Icons.Default.AccountBalanceWallet to "Budget")
        items.forEachIndexed { index, pair -> NavigationBarItem(selected == index, { onSelect(index) }, icon = { Icon(pair.first, pair.second) }, label = { Text(pair.second) }) }
    }
}

@Composable private fun Dashboard(expenses: List<Expense>, budget: Double, onAdd: () -> Unit, onBudget: () -> Unit) {
    val todayTotal = expenses.filter { it.date == dateKey(Date()) }.sumOf { it.amount }
    val monthTotal = expenses.filter { it.date.startsWith(monthKey(Date())) }.sumOf { it.amount }
    val remaining = (budget - monthTotal).coerceAtLeast(0.0)
    val progress = if (budget > 0) (monthTotal / budget).coerceIn(0.0, 1.0).toFloat() else 0f
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Card(RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Accent), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp)) {
            Text("Spent this month", color = Color.White.copy(.75f), fontSize = 14.sp); Text(money(monthTotal), Color.White, 34.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(18.dp))
            LinearProgressIndicator({ progress }, Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(10.dp)), color = Color.White, trackColor = Color.White.copy(.2f)); Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("${(progress * 100).toInt()}% of budget", color = Color.White.copy(.8f), fontSize = 12.sp); Text("${money(remaining)} left", Color.White, 12.sp, fontWeight = FontWeight.Bold) }
        } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { StatCard("Today", money(todayTotal), Icons.Default.Today, Modifier.weight(1f)); StatCard("This month", money(monthTotal), Icons.Default.CalendarMonth, Modifier.weight(1f)) } }
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("Spending this week", 18.sp, fontWeight = FontWeight.Bold); TextButton(onBudget) { Text("Edit budget") } } }
        item { WeeklyChart(expenses) }
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("Recent expenses", 18.sp, fontWeight = FontWeight.Bold); TextButton(onAdd) { Text("Add") } } }
        items(expenses.take(5), key = { it.id }) { ExpenseRow(it) }
        if (expenses.isEmpty()) item { EmptyState(onAdd) }
    }
}

@Composable private fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier) {
    Card(modifier, RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Card)) { Column(Modifier.padding(18.dp)) { Icon(icon, null, tint = Accent); Spacer(Modifier.height(12.dp)); Text(title, color = Muted, fontSize = 12.sp); Text(value, fontWeight = FontWeight.Bold, fontSize = 19.sp) } }
}

@Composable private fun WeeklyChart(expenses: List<Expense>) {
    val cal = Calendar.getInstance()
    val values = (6 downTo 0).map { offset -> val c = cal.clone() as Calendar; c.add(Calendar.DAY_OF_YEAR, -offset); expenses.filter { it.date == dateKey(c.time) }.sumOf { it.amount } }
    val labels = (6 downTo 0).map { offset -> val c = cal.clone() as Calendar; c.add(Calendar.DAY_OF_YEAR, -offset); SimpleDateFormat("EEE", Locale.getDefault()).format(c.time).take(2) }
    Card(RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Card), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) {
        Text("Last 7 days", color = Muted, fontSize = 12.sp); Spacer(Modifier.height(8.dp))
        Canvas(Modifier.fillMaxWidth().height(150.dp)) { val max = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0); val step = size.width / 7f; values.forEachIndexed { i, v -> val x = step * i + step / 2; val h = (v / max).toFloat() * (size.height - 25.dp.toPx()); drawRoundRect(AccentSoft, Offset(x - 12.dp.toPx(), size.height - h), androidx.compose.ui.geometry.Size(24.dp.toPx(), h), 10.dp.toPx()); if (v > 0) drawRoundRect(Accent, Offset(x - 12.dp.toPx(), size.height - h), androidx.compose.ui.geometry.Size(24.dp.toPx(), h), 10.dp.toPx()) } }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { labels.forEach { Text(it, color = Muted, fontSize = 10.sp) } }
    } }
}

@Composable private fun Transactions(expenses: List<Expense>, onDelete: (Long) -> Unit) {
    var query by remember { mutableStateOf("") }; var category by remember { mutableStateOf("All") }
    val filtered = expenses.filter { (query.isBlank() || it.title.contains(query, true) || it.note.contains(query, true)) && (category == "All" || it.category == category) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Search expenses...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(18.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(category == "All", { category = "All" }, label = { Text("All") }); categories.forEach { c -> FilterChip(category == c, { category = c }, label = { Text(c) }) }
        }
        if (filtered.isEmpty()) EmptyState({}) else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 90.dp)) { items(filtered, key = { it.id }) { ExpenseRow(it) { onDelete(it.id) } } }
    }
}

@Composable private fun ExpenseRow(expense: Expense, onDelete: (() -> Unit)? = null) {
    Card(RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Card), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp), Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(AccentSoft), Alignment.Center) { Text(categoryEmoji[expense.category] ?: "✨", fontSize = 21.sp) }; Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(expense.title.ifBlank { expense.category }, fontWeight = FontWeight.SemiBold); Text("${expense.category} • ${expense.date}", color = Muted, fontSize = 12.sp); if (expense.note.isNotBlank()) Text(expense.note, color = Muted, fontSize = 11.sp, maxLines = 1) }
        Text("-${money(expense.amount)}", fontWeight = FontWeight.Bold, color = Ink); if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "Delete", tint = Muted) }
    } }
}

@Composable private fun Analytics(expenses: List<Expense>) {
    val totals = categories.map { it to expenses.filter { e -> e.category == it }.sumOf { e -> e.amount } }.filter { it.second > 0 }.sortedByDescending { it.second }; val total = totals.sumOf { it.second }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Where your money goes", 18.sp, fontWeight = FontWeight.Bold) }
        item { Card(RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Card)) { Column(Modifier.padding(20.dp)) { DonutChart(totals.map { it.second }); Spacer(Modifier.height(12.dp)); totals.forEachIndexed { index, pair -> Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), Alignment.CenterVertically) { Box(Modifier.size(10.dp).clip(CircleShape).background(chartColor(index))); Spacer(Modifier.width(10.dp)); Text("${categoryEmoji[pair.first] ?: "✨"} ${pair.first}", Modifier.weight(1f)); Text("${money(pair.second)}  ${if (total > 0) (pair.second / total * 100).toInt() else 0}%", fontWeight = FontWeight.SemiBold) } } } } }
        item { Text("Spending trend", 18.sp, fontWeight = FontWeight.Bold) }; item { MonthlyLine(expenses) }
    }
}

@Composable private fun DonutChart(values: List<Double>) { Canvas(Modifier.fillMaxWidth().height(210.dp)) { val total = values.sum().coerceAtLeast(1.0); var start = -90f; val stroke = 34.dp.toPx(); values.forEachIndexed { i, value -> val sweep = (value / total * 360).toFloat(); drawArc(chartColor(i), start, sweep - 3, false, style = Stroke(stroke, cap = StrokeCap.Round)); start += sweep }; drawCircle(if (Surface == Color.White) Color.White else Surface, radius = 54.dp.toPx()) } }

@Composable private fun MonthlyLine(expenses: List<Expense>) {
    val now = Calendar.getInstance(); val values = (5 downTo 0).map { offset -> val c = now.clone() as Calendar; c.add(Calendar.MONTH, -offset); val prefix = SimpleDateFormat("yyyy-MM", Locale.US).format(c.time); expenses.filter { it.date.startsWith(prefix) }.sumOf { it.amount } }
    Card(RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Card)) { Canvas(Modifier.fillMaxWidth().height(190.dp).padding(16.dp)) { val max = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0); val path = Path(); values.forEachIndexed { i, v -> val x = if (values.size == 1) 0f else i * size.width / (values.size - 1); val y = size.height - (v / max).toFloat() * (size.height - 20.dp.toPx()); if (i == 0) path.moveTo(x, y) else path.lineTo(x, y); drawCircle(Accent, 5.dp.toPx(), Offset(x, y)) }; drawPath(path, Accent, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round)) } }
}

@Composable private fun BudgetScreen(expenses: List<Expense>, budget: Double, onEdit: () -> Unit) {
    val total = expenses.filter { it.date.startsWith(monthKey(Date())) }.sumOf { it.amount }; val progress = if (budget > 0) (total / budget).coerceIn(0.0, 1.0) else 0.0
    Column(Modifier.fillMaxSize().padding(20.dp)) { Card(RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Card)) { Column(Modifier.padding(22.dp)) { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Column { Text("Monthly budget", color = Muted); Text(money(budget), 30.sp, fontWeight = FontWeight.Bold) }; IconButton(onEdit) { Icon(Icons.Default.Edit, "Edit budget") } }; Spacer(Modifier.height(18.dp)); LinearProgressIndicator({ progress.toFloat() }, Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(10.dp)), color = if (progress > .9) Red else Accent, trackColor = AccentSoft); Spacer(Modifier.height(10.dp)); Text("${money(total)} spent • ${money((budget - total).coerceAtLeast(0.0))} remaining", color = Muted) } }; Spacer(Modifier.height(18.dp)); Text("Helpful habit", fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(8.dp)); Text("Log purchases as they happen. After a week, the Insights tab will reveal patterns you can actually act on.", color = Muted, lineHeight = 22.sp) }
}

@Composable private fun AddExpenseDialog(onDismiss: () -> Unit, onSave: (Expense) -> Unit) {
    var amount by remember { mutableStateOf("") }; var title by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }; var category by remember { mutableStateOf(categories[0]) }; var payment by remember { mutableStateOf("Cash") }; var date by remember { mutableStateOf(dateKey(Date())) }; val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { Button(enabled = amount.toDoubleOrNull()?.let { it > 0 } == true, onClick = { onSave(Expense(System.currentTimeMillis(), amount.toDouble(), title, category, date, note, payment)) }) { Text("Save expense") } }, dismissButton = { TextButton(onDismiss) { Text("Cancel") } }, title = { Text("Add expense", fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(amount, { amount = it }, label = { Text("Amount (TND)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        OutlinedTextField(title, { title = it }, label = { Text("What did you buy?") }, singleLine = true)
        Text("Category", color = Muted, fontSize = 12.sp); Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { categories.forEach { c -> FilterChip(category == c, { category = c }, label = { Text("${categoryEmoji[c]} $c") }) } }
        OutlinedTextField(date, { date = it }, label = { Text("Date") }, readOnly = true, modifier = Modifier.fillMaxWidth().clickable { val c = Calendar.getInstance(); DatePickerDialog(context, { _, y, m, d -> date = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d) }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show() })
        OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, minLines = 2)
        Text("Payment", color = Muted, fontSize = 12.sp); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Cash", "Card", "Transfer").forEach { p -> FilterChip(payment == p, { payment = p }, label = { Text(p) }) } }
    } }, shape = RoundedCornerShape(26.dp))
}

@Composable private fun BudgetDialog(current: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit) { var value by remember { mutableStateOf(if (current == 0.0) "" else current.toString()) }; AlertDialog(onDismissRequest = onDismiss, title = { Text("Monthly budget") }, text = { OutlinedTextField(value, { value = it }, label = { Text("Budget (TND)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true) }, confirmButton = { Button(onClick = { value.toDoubleOrNull()?.let(onSave) }) { Text("Save") } }, dismissButton = { TextButton(onDismiss) { Text("Cancel") } }, shape = RoundedCornerShape(24.dp)) }

@Composable private fun EmptyState(onAdd: () -> Unit) { Card(RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Card), modifier = Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth().padding(28.dp), Alignment.CenterHorizontally) { Text("🌱", fontSize = 42.sp); Spacer(Modifier.height(8.dp)); Text("Your spending story starts here", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("Add your first expense and MoneyFlow will turn it into useful insights.", color = Muted, fontSize = 13.sp); Spacer(Modifier.height(12.dp)); Button(onAdd) { Text("Add first expense") } } } }

private fun chartColor(index: Int) = listOf(Color(0xFF6658D3), Color(0xFF2DB58A), Color(0xFFFF9D5C), Color(0xFFE85D75), Color(0xFF4D9DE0), Color(0xFF9C6ADE), Color(0xFF5EB1BF), Color(0xFFF2C14E))[index % 8]
private fun money(value: Double): String = "${NumberFormat.getNumberInstance(Locale.US).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }.format(value)} TND"
private fun dateKey(date: Date) = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
private fun monthKey(date: Date) = SimpleDateFormat("yyyy-MM", Locale.US).format(date)
private fun loadExpenses(context: Context): List<Expense> = runCatching { val raw = context.getSharedPreferences("moneyflow", Context.MODE_PRIVATE).getString("expenses", "[]") ?: "[]"; val array = JSONArray(raw); List(array.length()) { i -> val o = array.getJSONObject(i); Expense(o.getLong("id"), o.getDouble("amount"), o.optString("title"), o.optString("category", "Other"), o.optString("date"), o.optString("note"), o.optString("payment", "Cash")) } }.getOrDefault(emptyList())
private fun saveExpenses(context: Context, expenses: List<Expense>) { val array = JSONArray(); expenses.forEach { e -> array.put(JSONObject().apply { put("id", e.id); put("amount", e.amount); put("title", e.title); put("category", e.category); put("date", e.date); put("note", e.note); put("payment", e.payment) }) }; context.getSharedPreferences("moneyflow", Context.MODE_PRIVATE).edit().putString("expenses", array.toString()).apply() }
private fun loadBudget(context: Context) = context.getSharedPreferences("moneyflow", Context.MODE_PRIVATE).getFloat("budget", 500f).toDouble()
private fun saveBudget(context: Context, value: Double) = context.getSharedPreferences("moneyflow", Context.MODE_PRIVATE).edit().putFloat("budget", value.toFloat()).apply()
