package com.yharim.anotado.ui.task

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yharim.anotado.R
import com.yharim.anotado.ui.theme.AnotadoTheme

private val Green = Color(0xFF1D7958)
private val Ink = Color(0xFF203D33)

@Composable
fun TaskFormScreen(viewModel: TaskViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saveSuccess) { if (state.saveSuccess) onSaved() }
    BackHandler(enabled = state.isSaving) {}
    TaskFormContent(state, viewModel::onTitleChange, viewModel::onDescriptionChange,
        viewModel::onPriorityChange, viewModel::onCategoryChange, viewModel::onCategorySelected,
        viewModel::onCategoryMenuChange, viewModel::onDueDateChange, viewModel::onCompletedChange,
        viewModel::onSaveClick, onBack)
}

@Composable
internal fun TaskFormContent(
    state: TaskFormUiState, onTitle: (String) -> Unit, onDescription: (String) -> Unit,
    onPriority: (Int) -> Unit, onCategory: (String) -> Unit, onCategorySelected: (String) -> Unit,
    onCategoryMenu: (Boolean) -> Unit, onDueDate: (String) -> Unit,
    onCompleted: (Boolean) -> Unit, onSave: () -> Unit, onBack: () -> Unit
) {
    val focus = LocalFocusManager.current
    Column(Modifier.fillMaxSize().background(Color.White).imePadding()) {
        Row(Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF176047), Color(0xFF23966C))))
            .statusBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(onClick = onBack, enabled = !state.isSaving,
                shape = RoundedCornerShape(14.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White.copy(alpha = .14f))) {
                Icon(painterResource(R.drawable.ic_arrow_back), "Volver", tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("NUEVA", color = Color.White.copy(alpha = .75f), fontSize = 11.sp,
                    letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Agregar tarea", color = Color.White, fontSize = 25.sp, modifier = Modifier.semantics { heading() })
            }
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
            .navigationBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Column {
                FormField("Título *", "¿Qué necesitas hacer?", state.title, onTitle, state.titleError, !state.isSaving)
                Text(state.titleCounter, color = Color(0xFF7D8D87), fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.End).padding(top = 6.dp))
            }
            FormField("Descripción", "Agrega detalles para recordar…", state.description,
                onDescription, state.descriptionError, !state.isSaving, multiline = true)
            Column {
                FieldLabel("Prioridad")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.priorities.forEach { priority ->
                        OutlinedButton(onClick = { onPriority(priority) }, enabled = !state.isSaving,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (state.priority == priority) Green else Color.White,
                                contentColor = if (state.priority == priority) Color.White else Color(0xFF7D8D87))) {
                            Text(priority.toString(), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                    }
                }
                Text(state.priorityLabel, Modifier.align(Alignment.End).padding(top = 6.dp), color = Green, fontSize = 12.sp)
                state.priorityError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    FormField("Categoría", "General", state.category, onCategory, state.categoryError, !state.isSaving,
                        trailingIcon = {
                            IconButton(onClick = { onCategoryMenu(true) }, enabled = !state.isSaving) {
                                Icon(painterResource(R.drawable.ic_chevron_right), "Elegir categoría", Modifier.size(16.dp).rotate(90f), tint = Ink)
                            }
                        })
                    DropdownMenu(expanded = state.categoryMenuOpen, onDismissRequest = { onCategoryMenu(false) }) {
                        state.categories.forEach { category ->
                            DropdownMenuItem(text = { Text(category) }, onClick = { onCategorySelected(category) })
                        }
                    }
                }
                FormField("Fecha límite", "yyyy-MM-dd", state.dueDate, onDueDate, state.dueDateError,
                    !state.isSaving, modifier = Modifier.weight(1f))
            }
            Surface(color = Color(0xFFF0F8F3), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(40.dp), color = Color.White, shape = RoundedCornerShape(12.dp)) {
                        Icon(painterResource(R.drawable.ic_check), null, Modifier.padding(10.dp), tint = Green)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Marcar como completada", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Puedes cambiarlo después", color = Color(0xFF7D8D87), fontSize = 11.sp)
                    }
                    Checkbox(checked = state.completed, onCheckedChange = onCompleted, enabled = !state.isSaving,
                        modifier = Modifier.semantics { contentDescription = "Marcar como completada" },
                        colors = CheckboxDefaults.colors(checkedColor = Green))
                }
            }
            state.generalError?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, enabled = !state.isSaving,
                    modifier = Modifier.weight(1f).heightIn(min = 54.dp), shape = RoundedCornerShape(14.dp)) { Text("Cancelar") }
                Button(onClick = { focus.clearFocus(); onSave() }, enabled = !state.isSaving && !state.saveSuccess,
                    modifier = Modifier.weight(1.4f).heightIn(min = 54.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                    if (state.isSaving) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Guardando…")
                    } else Text("Guardar tarea")
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = Color(0xFF3C5B4D), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun FormField(label: String, placeholder: String, value: String, onChange: (String) -> Unit,
    error: String?, enabled: Boolean, modifier: Modifier = Modifier, multiline: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null) {
    Column(modifier) {
        FieldLabel(label)
        OutlinedTextField(value = value, onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            enabled = enabled, singleLine = !multiline, minLines = if (multiline) 4 else 1,
            shape = RoundedCornerShape(14.dp), placeholder = { Text(placeholder, fontSize = 13.sp) },
            isError = error != null, supportingText = error?.let { message -> { Text(message) } },
            trailingIcon = trailingIcon,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences,
                imeAction = if (multiline) ImeAction.Default else ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Ink, unfocusedTextColor = Ink,
                focusedContainerColor = Color(0xFFFAFCFB), unfocusedContainerColor = Color(0xFFFAFCFB),
                focusedBorderColor = Green, unfocusedBorderColor = Color(0xFFE1E8E4), cursorColor = Green))
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Preview(showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.3f)
@Composable
private fun TaskFormPreview() {
    AnotadoTheme { TaskFormContent(TaskFormUiState(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) }
}
