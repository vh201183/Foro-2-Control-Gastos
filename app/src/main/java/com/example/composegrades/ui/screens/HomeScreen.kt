@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.composegrades.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.ExperimentalMaterial3Api

@Composable
fun HomeScreen(
    state: ExpensesState,
    onAddClick: () -> Unit,
    onEditClick: (Expense) -> Unit,
    onDeleteClick: (Expense) -> Unit,
    onMonthChange: (Long) -> Unit,
    onSave: () -> Unit,
    onFieldChange: (name: String?, amount: String?, category: String?, date: Long?) -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Gastos") }, actions = { TextButton(onClick = onLogout) { Text("Cerrar sesión") } }) },
        floatingActionButton = { FloatingActionButton(onClick = onAddClick) { Text("+") } }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Total del mes: ${"%.2f".format(state.total)}", style = MaterialTheme.typography.titleMedium)
            if (state.error != null) Text(state.error!!, color = MaterialTheme.colorScheme.error)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(state.items) { e ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(e.name, style = MaterialTheme.typography.titleMedium)
                                Text("${e.category} • ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(e.date))}")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("$${"%.2f".format(e.amount)}")
                                TextButton(onClick = { onEditClick(e) }) { Text("Editar") }
                                TextButton(onClick = { onDeleteClick(e) }) { Text("Borrar") }
                            }
                        }
                    }
                }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = state.current.name, onValueChange = { onFieldChange(it, null, null, null) }, label = { Text("Nombre") })
                    OutlinedTextField(value = if (state.current.amount == 0.0) "" else state.current.amount.toString(),
                        onValueChange = { onFieldChange(null, it, null, null) }, label = { Text("Monto") })
                    OutlinedTextField(value = state.current.category, onValueChange = { onFieldChange(null, null, it, null) }, label = { Text("Categoría") })
                    Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Guardar") }
                }
            }
        }
    }
}
