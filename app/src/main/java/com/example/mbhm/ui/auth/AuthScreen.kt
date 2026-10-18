package com.example.mbhm.ui.auth

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.R
import com.example.mbhm.data.entity.UserEntity
import com.example.mbhm.data.repository.AppRepository
import com.example.mbhm.model.GuardianDetails
import com.example.mbhm.model.Role
import com.example.mbhm.ui.components.C
import com.example.mbhm.ui.components.PrimaryButton
import com.example.mbhm.ui.components.Screen
import kotlinx.coroutines.launch

private enum class AuthMode {
    LOGIN,
    REGISTER,
    FORGOT
}

private fun roleLabel(role: Role): String = when (role) {
    Role.BOARDER -> "Boarder"
    Role.GUARDIAN -> "Guardian"
}

@Composable
fun AuthScreen(
    appRepository: AppRepository,
    onLoginSuccess: (UserEntity) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var infoMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    BackHandler {
        if (mode != AuthMode.LOGIN) {
            mode = AuthMode.LOGIN
        } else {
            activity?.moveTaskToBack(true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(C.bg)
    ) {
        when (mode) {
            AuthMode.LOGIN -> LoginScreenContent(
                error = errorMsg,
                info = infoMsg,
                onLogin = { username, password, accountType ->
                    errorMsg = null
                    infoMsg = null
                    if (username.isBlank() || password.isBlank()) {
                        errorMsg = if (accountType == Role.BOARDER) {
                            "Please enter your ID number and password"
                        } else {
                            "Please enter username and password"
                        }
                        return@LoginScreenContent
                    }
                    val selectedType = accountType
                    if (selectedType == null) {
                        errorMsg = "Please select your account type (Boarder or Guardian)"
                        return@LoginScreenContent
                    }
                    scope.launch {
                        val user = appRepository.authenticateUser(username, password)
                        when {
                            user == null -> {
                                errorMsg = if (selectedType == Role.BOARDER) {
                                    "Invalid ID number or password"
                                } else {
                                    "Invalid username or password"
                                }
                            }
                            user.role != selectedType -> {
                                errorMsg = "This account is a ${roleLabel(user.role)} account. " +
                                    "Select ${roleLabel(selectedType)} above and try again."
                            }
                            else -> onLoginSuccess(user)
                        }
                    }
                },
                onRegisterClick = {
                    errorMsg = null
                    infoMsg = null
                    mode = AuthMode.REGISTER
                },
                onForgotClick = {
                    errorMsg = null
                    infoMsg = null
                    mode = AuthMode.FORGOT
                }
            )
            AuthMode.REGISTER -> RegisterScreenContent(
                apiError = errorMsg,
                onBack = {
                    errorMsg = null
                    infoMsg = null
                    mode = AuthMode.LOGIN
                },
                onRegister = { username, password, guardianDetails ->
                    errorMsg = null
                    infoMsg = null
                    scope.launch {
                        val newUser = appRepository.registerUser(username, password, guardianDetails)
                        if (newUser != null) {
                            onLoginSuccess(newUser)
                        } else {
                            errorMsg = "Username is already taken"
                        }
                    }
                }
            )
            AuthMode.FORGOT -> ForgotScreenContent(
                onBack = {
                    errorMsg = null
                    infoMsg = null
                    mode = AuthMode.LOGIN
                }
            )
        }
    }
}

@Composable
private fun LoginScreenContent(
    error: String?,
    info: String?,
    onLogin: (String, String, Role?) -> Unit,
    onRegisterClick: () -> Unit,
    onForgotClick: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    var accountType by remember { mutableStateOf<Role?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(8.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(C.primary, Color(0xFF3730A3))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🏠", fontSize = 36.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "BOARDING HOUSE",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = C.text,
                letterSpacing = 1.sp
            )
            Text(
                text = "MANAGEMENT HUB",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = C.primary,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Sign in to view attendance, payments and announcements",
                fontSize = 13.sp,
                color = C.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(Modifier.height(28.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "SIGN IN AS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.muted,
                    letterSpacing = 0.5.sp
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoleOption(
                        label = "Boarder",
                        icon = "👤",
                        selected = accountType == Role.BOARDER,
                        onClick = { accountType = Role.BOARDER },
                        modifier = Modifier.weight(1f)
                    )
                    RoleOption(
                        label = "Guardian",
                        icon = "🛡️",
                        selected = accountType == Role.GUARDIAN,
                        onClick = { accountType = Role.GUARDIAN },
                        modifier = Modifier.weight(1f)
                    )
                }
                val selectedType = accountType
                if (selectedType == null) {
                    Text(
                        text = "Choose the account type you registered with",
                        fontSize = 12.sp,
                        color = C.muted
                    )
                } else {
                    Text(
                        text = if (selectedType == Role.BOARDER) {
                            "Signing in as Boarder — use the ID number given by your guardian"
                        } else {
                            "Signing in as Guardian"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = C.primary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AuthField(
                    label = if (accountType == Role.BOARDER) "ID NUMBER" else "USERNAME",
                    value = username,
                    onValueChange = { username = it },
                    placeholder = if (accountType == Role.BOARDER) "e.g. MBH-0001" else null
                )
                AuthField(
                    label = "PASSWORD",
                    value = password,
                    onValueChange = { password = it },
                    isPassword = true,
                    showPassword = showPassword,
                    onTogglePassword = { showPassword = !showPassword }
                )

                if (!info.isNullOrEmpty()) {
                    Text(
                        text = info,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = C.sage
                    )
                }
                if (!error.isNullOrEmpty()) {
                    Text(
                        text = error,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = C.danger
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { rememberMe = !rememberMe }
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = C.primary,
                                uncheckedColor = C.border
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Remember me",
                            fontSize = 14.sp,
                            color = C.text
                        )
                    }
                    Text(
                        text = "Forgot password?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = C.primary,
                        modifier = Modifier.clickable { onForgotClick() }
                    )
                }

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(6.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(listOf(C.primary, Color(0xFF3730A3)))
                        )
                        .clickable { onLogin(username, password, accountType) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign In",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Guardian? Create an account — ",
                fontSize = 14.sp,
                color = C.muted
            )
            Text(
                text = "REGISTER",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = C.primary,
                modifier = Modifier.clickable { onRegisterClick() }
            )
        }
    }
}

@Composable
private fun RegisterScreenContent(
    apiError: String?,
    onBack: () -> Unit,
    onRegister: (String, String, GuardianDetails) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var guardianDetails by remember { mutableStateOf(GuardianDetails()) }

    val reqLen = password.length >= 6
    val reqUpper = password.any { it.isUpperCase() }
    val reqLower = password.any { it.isLowerCase() }
    val reqDigit = password.any { it.isDigit() }
    val reqSpecial = password.any { !it.isLetterOrDigit() }
    val reqMatch = password.isNotEmpty() && password == confirm

    val strengthScore = listOf(reqLen, reqUpper, reqLower, reqDigit, reqSpecial, reqMatch).count { it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            text = "← Back to login",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = C.primary,
            modifier = Modifier.clickable { onBack() }
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Create Guardian Account",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = C.text
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Only guardians register here — you manage the boarding house and " +
                "create the accounts (ID number + password) for your boarders.",
            fontSize = 13.sp,
            color = C.muted
        )
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "ACCOUNT CREDENTIALS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = C.muted,
                letterSpacing = 0.5.sp
            )
            AuthField(
                label = "USERNAME",
                value = username,
                onValueChange = { username = it }
            )
            AuthField(
                label = "PASSWORD",
                value = password,
                onValueChange = { password = it },
                isPassword = true,
                showPassword = showPassword,
                onTogglePassword = { showPassword = !showPassword }
            )
            AuthField(
                label = "CONFIRM PASSWORD",
                value = confirm,
                onValueChange = { confirm = it },
                isPassword = true,
                showPassword = showPassword,
                onTogglePassword = { showPassword = !showPassword }
            )
        }

        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "GUARDIAN DETAILS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = C.muted,
                letterSpacing = 0.5.sp
            )
            AuthField(
                label = "Full Name",
                value = guardianDetails.fullName,
                onValueChange = { guardianDetails = guardianDetails.copy(fullName = it) },
                placeholder = "Full name"
            )
            AuthField(
                label = "Phone Number (shown to all boarders)",
                value = guardianDetails.phone,
                onValueChange = { guardianDetails = guardianDetails.copy(phone = it) },
                keyboardType = KeyboardType.Phone,
                placeholder = "0917XXXXXX"
            )
            AuthField(
                label = "House Name / Number",
                value = guardianDetails.houseName,
                onValueChange = { guardianDetails = guardianDetails.copy(houseName = it) },
                placeholder = "e.g. House 12, Brgy. San Antonio"
            )
            AuthField(
                label = "House Address",
                value = guardianDetails.houseAddress,
                onValueChange = { guardianDetails = guardianDetails.copy(houseAddress = it) },
                placeholder = "Full address"
            )
        }

        // Password strength card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(C.card)
                .border(1.dp, C.border, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            val (strengthText, strengthColor) = when {
                strengthScore == 6 -> "Strong" to C.sage
                strengthScore >= 3 -> "Medium" to C.warn
                else -> "Weak" to C.danger
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PASSWORD STRENGTH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.muted
                )
                Text(
                    text = strengthText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = strengthColor
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(C.bg)
            ) {
                val progressFraction = strengthScore / 6f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(strengthColor)
                )
            }
            Spacer(Modifier.height(10.dp))
            RequirementItem("At least 6 characters", reqLen)
            RequirementItem("One uppercase letter", reqUpper)
            RequirementItem("One lowercase letter", reqLower)
            RequirementItem("One number", reqDigit)
            RequirementItem("One special character", reqSpecial)
            RequirementItem("Passwords match", reqMatch)
        }

        Spacer(Modifier.height(4.dp))

        val displayError = formError ?: apiError
        if (!displayError.isNullOrEmpty()) {
            Text(
                text = displayError,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = C.danger
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(6.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(listOf(C.primary, Color(0xFF3730A3)))
                )
                .clickable {
                    val trimmed = username.trim()
                    if (trimmed.isEmpty()) {
                        formError = "Username is required"
                        return@clickable
                    }
                    if (trimmed.length < 3) {
                        formError = "Username must be at least 3 characters"
                        return@clickable
                    }
                    if (!trimmed.matches(Regex("^[a-zA-Z0-9_]+$"))) {
                        formError = "Username can only contain letters, numbers, and underscores"
                        return@clickable
                    }
                    if (password.isEmpty()) {
                        formError = "Password is required"
                        return@clickable
                    }
                    if (!reqLen) {
                        formError = "Password must be at least 6 characters"
                        return@clickable
                    }
                    if (!reqUpper || !reqLower) {
                        formError = "Password must contain uppercase and lowercase letters"
                        return@clickable
                    }
                    if (!reqDigit) {
                        formError = "Password must contain at least one number"
                        return@clickable
                    }
                    if (!reqSpecial) {
                        formError = "Password must contain at least one special character (e.g. !@#)"
                        return@clickable
                    }
                    if (!reqMatch) {
                        formError = "Passwords do not match"
                        return@clickable
                    }
                    if (guardianDetails.fullName.isBlank()) {
                        formError = "Please enter your full name"
                        return@clickable
                    }
                    if (guardianDetails.phone.isBlank()) {
                        formError = "Please enter your phone number"
                        return@clickable
                    }
                    if (guardianDetails.houseName.isBlank()) {
                        formError = "Please enter your house name or number"
                        return@clickable
                    }
                    formError = null
                    onRegister(trimmed, password, guardianDetails)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Create Guardian Account",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun ForgotScreenContent(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            text = "← Back to login",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = C.primary,
            modifier = Modifier.clickable { onBack() }
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Forgot password",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = C.text
        )
        Spacer(Modifier.height(6.dp))
            Text(
                text = "Boarders: ask your guardian to reset your password.\n" +
                    "Guardians: contact the administrator to reset your password.",
                fontSize = 14.sp,
                color = C.muted
            )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(C.primary)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Back to Login",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun RoleOption(
    label: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) C.primary.copy(alpha = 0.10f) else C.card)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) C.primary else C.border,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) C.primary else C.text
        )
    }
}

@Composable
private fun RequirementItem(label: String, met: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = if (met) "✓" else "○",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (met) C.sage else C.muted
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (met) FontWeight.Medium else FontWeight.Normal,
            color = if (met) C.sage else C.muted
        )
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: () -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (focused) C.primary else C.muted,
            letterSpacing = 0.5.sp
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(if (focused) 4.dp else 0.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(C.card)
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color = if (focused) C.primary else C.border,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    interactionSource = interactionSource,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = if (value.isEmpty() && !isPassword) C.muted else C.text
                    ),
                    cursorBrush = SolidColor(C.primary),
                    visualTransformation = if (isPassword && !showPassword) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isPassword) KeyboardType.Password else keyboardType
                    ),
                    decorationBox = { inner ->
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, color = C.muted, fontSize = 14.sp)
                        }
                        inner()
                    }
                )
                if (isPassword) {
                    Text(
                        text = if (showPassword) "👁" else "👁\u200D🗨",
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clickable { onTogglePassword() }
                            .padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
