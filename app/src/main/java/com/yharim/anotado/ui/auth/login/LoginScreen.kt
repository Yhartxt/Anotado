package com.example.taskmanager.ui.auth.login

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yharim.anotado.R
import com.yharim.anotado.ui.theme.AnotadoTheme

private val HeaderTop = Color(0xFF176047)
private val HeaderBottom = Color(0xFF23966C)
private val Primary = Color(0xFF1D7958)
private val TextDark = Color(0xFF203D33)
private val TextMuted = Color(0xFF7D8D87)
private val FieldBackground = Color(0xFFFAFCFB)
private val FieldBorder = Color(0xFFE1E8E4)

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToRegister: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoginContent(
        state, viewModel::onEmailChange, viewModel::onPasswordChange,
        viewModel::onLoginClick, onNavigateToRegister, viewModel::onTogglePassword
    )
}

@Composable
internal fun LoginContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onTogglePassword: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val submit = { focusManager.clearFocus(); onLoginClick() }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextDark, unfocusedTextColor = TextDark,
        focusedPlaceholderColor = TextMuted, unfocusedPlaceholderColor = TextMuted,
        focusedContainerColor = FieldBackground, unfocusedContainerColor = FieldBackground,
        disabledContainerColor = FieldBackground, errorContainerColor = FieldBackground,
        focusedBorderColor = Primary, unfocusedBorderColor = FieldBorder,
        cursorColor = Primary
    )

    BoxWithConstraints(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(HeaderTop, HeaderBottom)))
            .statusBarsPadding().imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        val headerHeight = (maxHeight * 0.29f).coerceIn(150.dp, 240.dp)
        val bodyHeight = (maxHeight - headerHeight).coerceAtLeast(0.dp)
        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(headerHeight)) {
                Canvas(Modifier.matchParentSize()) {
                    val center = Offset(size.width * 0.92f, size.height * 0.18f)
                    drawCircle(Color.White.copy(alpha = 0.07f), size.width * 0.25f, center)
                    drawCircle(Color.White.copy(alpha = 0.11f), size.width * 0.25f, center, style = Stroke(1.dp.toPx()))
                    drawCircle(Color.White.copy(alpha = 0.07f), size.width * 0.39f, center, style = Stroke(1.dp.toPx()))
                }
                Column(Modifier.padding(horizontal = 28.dp, vertical = 32.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(38.dp).rotate(-8f),
                            shape = RoundedCornerShape(11.dp), color = Color.White
                        ) {
                            Icon(painterResource(R.drawable.ic_check), null,
                                Modifier.padding(8.dp), tint = Primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Anotado", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("Organiza tu día, una tarea a\nla vez.", color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp, lineHeight = 24.sp)
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(min = bodyHeight),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White
            ) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 28.dp, vertical = 32.dp)) {
                    Text("BIENVENIDO DE NUEVO", color = Primary, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Inicia sesión", color = TextDark, fontSize = 29.sp,
                        fontWeight = FontWeight.Normal, modifier = Modifier.semantics { heading() })
                    Spacer(Modifier.height(8.dp))
                    Text("Accede para continuar con tus tareas.", color = TextMuted,
                        fontSize = 14.sp, lineHeight = 21.sp)
                    Spacer(Modifier.height(30.dp))
                    FieldLabel("Correo electrónico")
                    OutlinedTextField(
                        value = state.email, onValueChange = onEmailChange,
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Correo electrónico" },
                        singleLine = true, enabled = !state.isLoading,
                        shape = RoundedCornerShape(14.dp),
                        placeholder = { Text("tucorreo@ejemplo.com", fontSize = 15.sp) },
                        isError = state.emailError != null,
                        supportingText = state.emailError?.let { message -> { Text(message) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        colors = fieldColors
                    )
                    Spacer(Modifier.height(20.dp))
                    FieldLabel("Contraseña")
                    OutlinedTextField(
                        value = state.password, onValueChange = onPasswordChange,
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Contraseña" },
                        singleLine = true, enabled = !state.isLoading,
                        shape = RoundedCornerShape(14.dp),
                        placeholder = { Text("Tu contraseña", fontSize = 15.sp) },
                        isError = state.passwordError != null,
                        supportingText = state.passwordError?.let { message -> { Text(message) } },
                        visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = onTogglePassword, enabled = !state.isLoading) {
                                Icon(painterResource(if (state.passwordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye),
                                    contentDescription = if (state.passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                    tint = TextMuted)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        colors = fieldColors
                    )
                    state.generalError?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = submit, enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary,
                            disabledContainerColor = Primary.copy(alpha = 0.65f), disabledContentColor = Color.White),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Iniciando sesión…", color = Color.White)
                        } else {
                            Text("Iniciar sesión", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(16.dp))
                            Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("¿Aún no tienes una cuenta?", color = TextMuted, fontSize = 13.sp)
                        TextButton(onClick = onNavigateToRegister, enabled = !state.isLoading) {
                            Text("Crear una cuenta", color = Primary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 10.dp))
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Preview(showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.3f)
@Composable
private fun LoginPreview() {
    AnotadoTheme { LoginContent(LoginUiState(), {}, {}, {}, {}, {}) }
}
