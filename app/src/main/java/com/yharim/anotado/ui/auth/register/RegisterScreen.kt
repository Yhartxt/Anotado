package com.yharim.anotado.ui.auth.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
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

private val Green = Color(0xFF1D7958)
private val Ink = Color(0xFF203D33)
private val Muted = Color(0xFF7D8D87)

@Composable
fun RegisterScreen(viewModel: RegisterViewModel, onNavigateToLogin: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RegisterContent(
        state = state,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmationChange = viewModel::onConfirmationChange,
        onTogglePassword = viewModel::onTogglePassword,
        onToggleConfirmation = viewModel::onToggleConfirmation,
        onRegisterClick = viewModel::onRegisterClick,
        onNavigateToLogin = onNavigateToLogin
    )
}

@Composable
internal fun RegisterContent(
    state: RegisterUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onToggleConfirmation: () -> Unit,
    onRegisterClick: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val focus = LocalFocusManager.current
    val submit = { focus.clearFocus(); onRegisterClick() }
    val next = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) })
    Column(
        Modifier.fillMaxSize().background(Color.White).safeDrawingPadding().imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = onNavigateToLogin, enabled = !state.isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFF1F8F4))
            ) {
                Icon(painterResource(R.drawable.ic_arrow_back), "Volver a iniciar sesión", tint = Ink)
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(34.dp).rotate(-8f), shape = RoundedCornerShape(10.dp), color = Green) {
                    Icon(painterResource(R.drawable.ic_check), null, Modifier.padding(7.dp), tint = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Text("Anotado", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(48.dp))
        }
        HorizontalDivider(color = Color(0xFFE8EEEA))
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().weight(1f)
                .verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 28.dp)
        ) {
            Text("EMPIEZA HOY", color = Green, fontSize = 11.sp,
                letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("Crea tu cuenta", color = Ink, fontSize = 29.sp,
                modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(8.dp))
            Text("Tus pendientes, prioridades y avances en un solo lugar.",
                color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
            Spacer(Modifier.height(28.dp))
            RegisterField("Nombre", "Tu nombre", state.name, onNameChange,
                state.nameError, !state.isLoading,
                KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next), next)
            Spacer(Modifier.height(18.dp))
            RegisterField("Correo electrónico", "nombre@correo.com", state.email, onEmailChange,
                state.emailError, !state.isLoading,
                KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next), next)
            Spacer(Modifier.height(18.dp))
            RegisterField("Contraseña", "Mínimo 8 caracteres", state.password, onPasswordChange,
                state.passwordError, !state.isLoading,
                KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next), next,
                passwordVisible = state.passwordVisible, onToggleVisibility = onTogglePassword)
            Spacer(Modifier.height(18.dp))
            RegisterField("Confirmar contraseña", "Repite tu contraseña", state.confirmPassword, onConfirmationChange,
                state.confirmationError, !state.isLoading,
                KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                KeyboardActions(onDone = { submit() }),
                passwordVisible = state.confirmationVisible, onToggleVisibility = onToggleConfirmation)
            state.generalError?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            Spacer(Modifier.height(26.dp))
            Button(
                onClick = submit, enabled = !state.isLoading && !state.registerSuccess,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green,
                    disabledContainerColor = Green.copy(alpha = 0.65f), disabledContentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Creando cuenta…")
                } else {
                    Text("Crear cuenta", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(14.dp))
                    Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onNavigateToLogin, enabled = !state.isLoading,
                modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Ya tengo una cuenta", color = Green, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RegisterField(
    label: String, placeholder: String, value: String, onValueChange: (String) -> Unit,
    error: String?, enabled: Boolean, keyboardOptions: KeyboardOptions, keyboardActions: KeyboardActions,
    passwordVisible: Boolean = false, onToggleVisibility: (() -> Unit)? = null
) {
    Text(label, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        singleLine = true, enabled = enabled, shape = RoundedCornerShape(14.dp),
        placeholder = { Text(placeholder, fontSize = 15.sp) }, isError = error != null,
        supportingText = error?.let { message -> { Text(message) } },
        keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
        visualTransformation = if (onToggleVisibility != null && !passwordVisible)
            PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = onToggleVisibility?.let { toggle -> {
            IconButton(onClick = toggle, enabled = enabled) {
                Icon(painterResource(if (passwordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye),
                    if (passwordVisible) "Ocultar $label" else "Mostrar $label", tint = Muted)
            }
        } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Ink, unfocusedTextColor = Ink,
            focusedPlaceholderColor = Muted, unfocusedPlaceholderColor = Muted,
            focusedContainerColor = Color(0xFFFAFCFB), unfocusedContainerColor = Color(0xFFFAFCFB),
            disabledContainerColor = Color(0xFFFAFCFB), errorContainerColor = Color(0xFFFAFCFB),
            focusedBorderColor = Green, unfocusedBorderColor = Color(0xFFE1E8E4), cursorColor = Green
        )
    )
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Preview(showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.3f)
@Composable
private fun RegisterPreview() {
    AnotadoTheme { RegisterContent(RegisterUiState(), {}, {}, {}, {}, {}, {}, {}, {}) }
}
