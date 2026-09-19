package com.tropico.moneyflow.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tropico.moneyflow.model.AppCurrency
import com.tropico.moneyflow.model.Expense
import com.tropico.moneyflow.model.ExpenseCategory
import com.tropico.moneyflow.model.PaymentMethod
import com.tropico.moneyflow.util.DateUtils
import com.tropico.moneyflow.util.MoneyFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ExpenseRow(
    expense: Expense,
    currency: AppCurrency,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Expense ${expense.title}" }
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${expense.category.emoji}  ${expense.title}", fontWeight = FontWeight.SemiBold)
                Text(
                    "${expense.category.label} • ${expense.date.format(DateUtils.dateFormatter)}",
                    style = MaterialTheme.typography.bodySmall
                )
                expense.note?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                text = "-${MoneyFormat.formatMinor(expense.amountMinor, currency)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    initialExpense: Expense? = null,
    defaultPaymentMethod: PaymentMethod,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit
) {
    var amountInput by remember { mutableStateOf(initialExpense?.amountMinor?.div(100.0)?.toString() ?: "") }
    var title by remember { mutableStateOf(initialExpense?.title.orEmpty()) }
    var category by remember { mutableStateOf(initialExpense?.category ?: ExpenseCategory.Food) }
    var dateInput by remember { mutableStateOf((initialExpense?.date ?: LocalDate.now()).toString()) }
    var timeInput by remember { mutableStateOf(initialExpense?.time?.format(DateUtils.timeFormatter).orEmpty()) }
    var note by remember { mutableStateOf(initialExpense?.note.orEmpty()) }
    var paymentMethod by remember { mutableStateOf(initialExpense?.paymentMethod ?: defaultPaymentMethod) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(if (initialExpense == null) "Add expense" else "Edit expense", style = MaterialTheme.typography.headlineSmall)
            }
            item {
                OutlinedTextField(value = amountInput, onValueChange = { amountInput = it }, label = { Text("Amount") }, placeholder = { Text("12.50") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                Text("Category")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExpenseCategory.entries.forEach {
                        AssistChip(onClick = { category = it }, label = { Text("${it.emoji} ${it.label}") })
                    }
                }
            }
            item {
                OutlinedTextField(value = dateInput, onValueChange = { dateInput = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(value = timeInput, onValueChange = { timeInput = it }, label = { Text("Time (HH:mm, optional)") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        value = paymentMethod.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment method") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        PaymentMethod.entries.forEach {
                            DropdownMenuItem(text = { Text(it.label) }, onClick = { paymentMethod = it; expanded = false })
                        }
                    }
                }
            }
            item {
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())
            }
            if (error != null) {
                item { Text(error ?: "", color = MaterialTheme.colorScheme.error) }
            }
            item {
                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                    val amountMinor = MoneyFormat.parseMajorToMinor(amountInput)
                    val date = runCatching { LocalDate.parse(dateInput) }.getOrNull()
                    val time = timeInput.takeIf { it.isNotBlank() }?.let(DateUtils::parseTime)
                    when {
                        amountMinor == null || amountMinor <= 0 -> error = "Enter a valid positive amount"
                        title.isBlank() -> error = "Description is required"
                        date == null -> error = "Invalid date"
                        timeInput.isNotBlank() && time == null -> error = "Invalid time format"
                        else -> {
                            onSave(
                                Expense(
                                    id = initialExpense?.id ?: 0,
                                    amountMinor = amountMinor,
                                    title = title.trim(),
                                    category = category,
                                    date = date,
                                    time = time,
                                    note = note.ifBlank { null },
                                    paymentMethod = paymentMethod,
                                    createdAt = initialExpense?.createdAt ?: LocalDateTime.now()
                                )
                            )
                        }
                    }
                }) { Text(if (initialExpense == null) "Save expense" else "Save changes") }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete expense") },
        text = { Text("Are you sure you want to delete this expense?") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun BudgetProgressCard(title: String, spent: Long, budget: Long, currency: AppCurrency) {
    val progress by animateFloatAsState(if (budget <= 0) 0f else (spent.toFloat() / budget.toFloat()).coerceIn(0f, 1f), label = "budgetProgress")
    Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(MoneyFormat.formatMinor(spent, currency), style = MaterialTheme.typography.headlineMedium)
            Text("Budget ${MoneyFormat.formatMinor(budget, currency)}")
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), strokeCap = StrokeCap.Round)
            Text("${"%.0f".format(progress * 100)}% used")
        }
    }
}

@Composable
fun Last7DaysBarChart(points: List<Pair<LocalDate, Long>>, modifier: Modifier = Modifier) {
    val max = (points.maxOfOrNull { it.second } ?: 1L).coerceAtLeast(1L)
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Last 7 days", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                points.takeLast(7).forEach { (_, value) ->
                    Box(
                        Modifier
                            .size(width = 24.dp, height = (80f * (value.toFloat() / max)).coerceAtLeast(6f).dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun DonutChart(data: List<Pair<ExpenseCategory, Long>>, modifier: Modifier = Modifier) {
    val total = data.sumOf { it.second }.coerceAtLeast(1L).toFloat()
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.errorContainer
    )
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Category breakdown", style = MaterialTheme.typography.titleMedium)
            Canvas(modifier = Modifier
                .size(180.dp)
                .align(Alignment.CenterHorizontally)) {
                var start = -90f
                data.forEachIndexed { idx, pair ->
                    val sweep = (pair.second / total) * 360f
                    drawArc(
                        color = colors[idx % colors.size],
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 40f)
                    )
                    start += sweep
                }
            }
            data.forEachIndexed { idx, (category, amount) ->
                val pct = (amount / total) * 100f
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${category.emoji} ${category.label}")
                    Text("${"%.0f".format(pct)}%")
                }
            }
        }
    }
}

@Composable
fun TrendLineChart(data: List<Pair<LocalDate, Long>>, modifier: Modifier = Modifier) {
    val max = (data.maxOfOrNull { it.second } ?: 1L).toFloat().coerceAtLeast(1f)
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Spending trend", style = MaterialTheme.typography.titleMedium)
            Canvas(modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(top = 12.dp)) {
                if (data.size < 2) return@Canvas
                val step = size.width / (data.size - 1)
                data.zipWithNext().forEachIndexed { index, (current, next) ->
                    val x1 = index * step
                    val y1 = size.height - ((current.second / max) * size.height)
                    val x2 = (index + 1) * step
                    val y2 = size.height - ((next.second / max) * size.height)
                    drawLine(MaterialTheme.colorScheme.primary, Offset(x1, y1), Offset(x2, y2), strokeWidth = 6f)
                }
            }
        }
    }
}

@Composable
fun TransactionDetailDialog(
    expense: Expense,
    currency: AppCurrency,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(expense.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Amount: ${MoneyFormat.formatMinor(expense.amountMinor, currency)}")
                Text("Category: ${expense.category.label}")
                Text("Date: ${expense.date}")
                expense.time?.let { Text("Time: $it") }
                Text("Payment: ${expense.paymentMethod.label}")
                expense.note?.let { Text("Note: $it") }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                    Text("Edit")
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                    Text("Delete")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
