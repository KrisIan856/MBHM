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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.R
import com.example.mbhm.data.entity.UserEntity
import com.example.mbhm.data.repository.AppRepository
import com.example.mbhm.model.BoarderDetails
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
                onLogin = { username, password ->
                    errorMsg = null
                    infoMsg = null
                    if (username.isBlank() || password.isBlank()) {
                        errorMsg = "Please enter username and password"
                        return@LoginScreenContent
                    }
                    scope.launch {
                        val user = appRepository.authenticateUser(username, password)
                        if (user != null) {
                            onLoginSuccess(user)
                        } else {
                            errorMsg = "Invalid username or password"
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
                onRegister = { username, password, guardianDetails, boarderDetails, isGuardian ->
                    errorMsg = null
                    infoMsg = null
                    scope.launch {
                        val role = if (isGuardian) Role.GUARDIAN else Role.BOARDER
                        val newUser = appRepository.registerUser(username, password, role, guardianDetails, boarderDetails)
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
    onLogin: (String, String) -> Unit,
    onRegisterClick: () -> Unit,
    onForgotClick: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

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
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(C.primary),
                contentAlignment = Alignment.Center
            ) {
                Text("🏠", fontSize = 32.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "BOARDING HOUSE MANAGEMENT",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = C.text,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(32.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                        color = Color(0xFFD94F4F)
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
                        .clip(RoundedCornerShape(14.dp))
                        .background(C.primary)
                        .clickable { onLogin(username, password) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LOGIN",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account? ",
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
    onRegister: (String, String, GuardianDetails?, BoarderDetails?, Boolean) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var guardianDetails by remember { mutableStateOf(GuardianDetails()) }
    var boarderDetails by remember { mutableStateOf(BoarderDetails()) }
    var registerAsGuardian by remember { mutableStateOf(false) }

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
            text = "Create Account",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = C.text
        )
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoleOption(
                label = "Boarder",
                icon = "👤",
                selected = !registerAsGuardian,
                onClick = { registerAsGuardian = false },
                modifier = Modifier.weight(1f)
            )
            RoleOption(
                label = "Guardian",
                icon = "🛡️",
                selected = registerAsGuardian,
                onClick = { registerAsGuardian = true },
                modifier = Modifier.weight(1f)
            )
        }
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
                text = if (registerAsGuardian) "GUARDIAN DETAILS" else "BOARDER DETAILS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = C.muted,
                letterSpacing = 0.5.sp
            )
            if (registerAsGuardian) {
                AuthField(
                    label = "Full Name",
                    value = guardianDetails.fullName,
                    onValueChange = { guardianDetails = guardianDetails.copy(fullName = it) },
                    placeholder = "Full name"
                )
                AuthField(
                    label = "Phone Number",
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
            } else {
                AuthField(
                    label = "Full Name",
                    value = boarderDetails.name.ifEmpty { "Enter name" },
                    onValueChange = { boarderDetails = boarderDetails.copy(name = it) },
                    placeholder = "Full name"
                )
                AuthField(
                    label = "Room",
                    value = boarderDetails.room,
                    onValueChange = { boarderDetails = boarderDetails.copy(room = it) },
                    placeholder = "e.g. 101"
                )
                AuthField(
                    label = "Floor",
                    value = boarderDetails.floor,
                    onValueChange = { boarderDetails = boarderDetails.copy(floor = it) },
                    placeholder = "e.g. 1st Floor"
                )
                AuthField(
                    label = "Phone Number",
                    value = boarderDetails.phone,
                    onValueChange = { boarderDetails = boarderDetails.copy(phone = it) },
                    keyboardType = KeyboardType.Phone,
                    placeholder = "0917XXXXXX"
                )
                AuthField(
                    label = "Email Address",
                    value = boarderDetails.email,
                    onValueChange = { boarderDetails = boarderDetails.copy(email = it) },
                    keyboardType = KeyboardType.Email,
                    placeholder = "boarder@email.com"
                )
                AuthField(
                    label = "Join Date",
                    value = boarderDetails.joinDate,
                    onValueChange = { boarderDetails = boarderDetails.copy(joinDate = it) },
                    placeholder = "e.g. Sep 15, 2026"
                )
                AuthField(
                    label = "Monthly Rate (₱)",
                    value = boarderDetails.monthlyRate,
                    onValueChange = { boarderDetails = boarderDetails.copy(monthlyRate = it) },
                    keyboardType = KeyboardType.Number,
                    placeholder = "e.g. 5000"
                )
                AuthField(
                    label = "Due Day (1-28)",
                    value = boarderDetails.dueDay,
                    onValueChange = { boarderDetails = boarderDetails.copy(dueDay = it) },
                    keyboardType = KeyboardType.Number,
                    placeholder = "e.g. 5"
                )
                AuthField(
                    label = "Job / Occupation",
                    value = boarderDetails.job,
                    onValueChange = { boarderDetails = boarderDetails.copy(job = it) },
                    placeholder = "e.g. Student"
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        if (!registerAsGuardian) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "EMERGENCY CONTACT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.muted,
                    letterSpacing = 0.5.sp
                )
                AuthField(
                    label = "Contact Name",
                    value = boarderDetails.emergencyContactName,
                    onValueChange = { boarderDetails = boarderDetails.copy(emergencyContactName = it) }
                )
                AuthField(
                    label = "Relationship",
                    value = boarderDetails.emergencyContactRelationship,
                    onValueChange = { boarderDetails = boarderDetails.copy(emergencyContactRelationship = it) }
                )
                AuthField(
                    label = "Phone Number",
                    value = boarderDetails.emergencyContactPhone,
                    onValueChange = { boarderDetails = boarderDetails.copy(emergencyContactPhone = it) },
                    keyboardType = KeyboardType.Phone
                )
                AuthField(
                    label = "Secondary Phone (optional)",
                    value = boarderDetails.emergencyContactSecondaryPhone,
                    onValueChange = { boarderDetails = boarderDetails.copy(emergencyContactSecondaryPhone = it) },
                    keyboardType = KeyboardType.Phone
                )
                AuthField(
                    label = "Address (optional)",
                    value = boarderDetails.emergencyContactAddress,
                    onValueChange = { boarderDetails = boarderDetails.copy(emergencyContactAddress = it) }
                )
            }

            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "GUARDIAN / HOUSE OWNER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.muted,
                    letterSpacing = 0.5.sp
                )
                AuthField(
                    label = "Guardian Full Name",
                    value = boarderDetails.guardianContactName,
                    onValueChange = { boarderDetails = boarderDetails.copy(guardianContactName = it) }
                )
                AuthField(
                    label = "Relationship to Boarder",
                    value = boarderDetails.guardianContactRelationship,
                    onValueChange = { boarderDetails = boarderDetails.copy(guardianContactRelationship = it) }
                )
                AuthField(
                    label = "Guardian Contact Number",
                    value = boarderDetails.guardianContactPhone,
                    onValueChange = { boarderDetails = boarderDetails.copy(guardianContactPhone = it) },
                    keyboardType = KeyboardType.Phone
                )
                AuthField(
                    label = "Alternative Number (optional)",
                    value = boarderDetails.guardianContactAltPhone,
                    onValueChange = { boarderDetails = boarderDetails.copy(guardianContactAltPhone = it) },
                    keyboardType = KeyboardType.Phone
                )
                AuthField(
                    label = "Guardian Address (optional)",
                    value = boarderDetails.guardianContactAddress,
                    onValueChange = { boarderDetails = boarderDetails.copy(guardianContactAddress = it) }
                )
            }
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
                else -> "Weak" to Color(0xFFD94F4F)
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
                color = Color(0xFFD94F4F)
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(C.primary)
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
                    if (password.length < 6) {
                        formError = "Password must be at least 6 characters"
                        return@clickable
                    }
                    if (password != confirm) {
                        formError = "Passwords do not match"
                        return@clickable
                    }
                    if (!registerAsGuardian) {
                        if (boarderDetails.emergencyContactName.isBlank() ||
                            boarderDetails.emergencyContactPhone.isBlank()
                        ) {
                            formError = "Please enter emergency contact name and phone"
                            return@clickable
                        }
                        if (boarderDetails.guardianContactName.isBlank() ||
                            boarderDetails.guardianContactPhone.isBlank()
                        ) {
                            formError = "Please enter guardian contact name and phone"
                            return@clickable
                        }
                    }
                    if (registerAsGuardian && guardianDetails.fullName.isBlank()) {
                        formError = "Please enter your full name"
                        return@clickable
                    }
                    formError = null
                    if (registerAsGuardian) {
                        onRegister(trimmed, password, guardianDetails, null, true)
                    } else {
                        onRegister(trimmed, password, null, boarderDetails, false)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "REGISTER",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
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
            text = "Please contact the administrator to reset your password.",
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = C.muted,
            letterSpacing = 0.5.sp
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(C.card)
                .border(1.dp, C.border, RoundedCornerShape(14.dp))
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
                        text = if (showPassword) "👁" else "👁‍🗨",
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
