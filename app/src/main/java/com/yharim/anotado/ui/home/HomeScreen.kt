package com.yharim.anotado.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yharim.anotado.R
import com.yharim.anotado.ui.components.TaskCard
import com.yharim.anotado.ui.theme.AnotadoTheme

private val Green = Color(0xFF1D7958)
private val Ink = Color(0xFF203D33)
private val Muted = Color(0xFF7D8D87)

@Composable
fun HomeScreen(viewModel: HomeViewModel, onNavigateToCreate: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state, viewModel::onLogoutClick, viewModel::onRefresh, viewModel::onFilterChange,
        viewModel::onToggleCompleted, viewModel::onTaskClick, onNavigateToCreate, viewModel::onDismissDialog)
}

@Composable
internal fun HomeContent(
    state: HomeUiState, onLogout: () -> Unit, onRefresh: () -> Unit,
    onFilter: (TaskFilter) -> Unit, onToggle: (Long) -> Unit,
    onTask: (Long) -> Unit, onNew: () -> Unit, onDismiss: () -> Unit
) {
    Scaffold(containerColor = Color(0xFFF8FAF9), topBar = {
        Surface(color = Color.White) {
            Column(Modifier.statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(34.dp).rotate(-8f), shape = RoundedCornerShape(10.dp), color = Green) {
                        Icon(painterResource(R.drawable.ic_check), null, Modifier.padding(7.dp), tint = Color.White)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Anotado", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onLogout, enabled = !state.isLoggingOut,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.textButtonColors(containerColor = Color(0xFFF5F8F6), contentColor = Muted)) {
                        Icon(painterResource(R.drawable.ic_logout), null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Salir", fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider(color = Color(0xFFE6ECE8))
            }
        }
    }, floatingActionButton = {
        FloatingActionButton(onClick = onNew, containerColor = Green, contentColor = Color.White,
            shape = RoundedCornerShape(20.dp)) {
            Icon(painterResource(R.drawable.ic_plus), "Crear tarea")
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("MI ESPACIO", color = Green, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text(state.greeting, color = Ink, fontSize = 29.sp, modifier = Modifier.semantics { heading() })
                if (state.hasLoaded) {
                    Spacer(Modifier.height(10.dp))
                    Text(state.pendingLabel, color = Muted, fontSize = 14.sp)
                }
            }
            if (state.hasLoaded) item {
                OutlinedCard(Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFDDECE4)),
                    colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFECF7F1))) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Progreso de tareas", Modifier.weight(1f), color = Color(0xFF5B776B), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(state.progressLabel, color = Green, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(18.dp))
                        LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth().height(7.dp),
                            color = Color(0xFF27946C), trackColor = Color(0xFFD0E5DA))
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Mis tareas", color = Ink, fontSize = 23.sp, modifier = Modifier.semantics { heading() })
                        if (state.hasLoaded) Text(state.totalLabel, color = Muted, fontSize = 13.sp)
                    }
                    Button(onClick = onNew, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                        Icon(painterResource(R.drawable.ic_plus), null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("Nueva")
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskFilter.entries.forEach { filter ->
                        FilterChip(selected = state.filter == filter, onClick = { onFilter(filter) },
                            label = { Text(filter.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFEDF7F2), selectedLabelColor = Green),
                            border = BorderStroke(1.dp, if (state.filter == filter) Green else Color(0xFFDFE6E1)))
                    }
                }
            }
            if (state.isLoading) item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Green, strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp)); Text("Cargando tareas…", color = Muted)
                }
            }
            state.error?.let { error -> item {
                Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRefresh, enabled = !state.isLoading && state.updatingIds.isEmpty()) { Text("Reintentar") }
            } }
            if (state.hasLoaded && state.tasks.isEmpty() && !state.isLoading) item {
                Text(state.emptyMessage, Modifier.padding(vertical = 24.dp), color = Muted)
            }
            items(state.tasks, key = { it.id }) { task ->
                TaskCard(task, updating = task.id in state.updatingIds, enabled = !state.isLoading && !state.isLoggingOut,
                    onClick = { onTask(task.id) }, onToggleCompleted = { onToggle(task.id) })
            }
            if (state.hasLoaded && !state.isLoading) item {
                TextButton(onClick = onRefresh, enabled = state.updatingIds.isEmpty()) { Text("Actualizar tareas") }
            }
        }
    }
    state.selectedTask?.let { task ->
        AlertDialog(onDismissRequest = onDismiss, title = { Text(task.title) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(task.description)
                Spacer(Modifier.height(12.dp))
                Text("Prioridad: ${task.priorityLabel}")
                Text("Categoría: ${task.category}")
                Text("Fecha límite: ${task.dateLabel}")
                Text(if (task.completed) "Completada" else "Pendiente")
            } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HomePreview() {
    AnotadoTheme {
        HomeContent(HomeUiState(greeting = "Hola, María", hasLoaded = true,
            pendingLabel = "Tienes 3 tareas por completar.", totalLabel = "4 en total", progress = .25f, progressLabel = "25%",
            tasks = listOf(TaskItemUiState(1, "Preparar estructura del proyecto", "", 5, "MUY ALTA", "Estudio", "9 nov 2026", false))),
            {}, {}, {}, {}, {}, {}, {})
    }
}
