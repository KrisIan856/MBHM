package com.example.mbhm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.mbhm.boarder.BoarderApp
import com.example.mbhm.data.SessionManager
import com.example.mbhm.data.entity.UserEntity
import com.example.mbhm.data.repository.AppRepository
import com.example.mbhm.guardian.GuardianApp
import com.example.mbhm.model.Role
import com.example.mbhm.ui.auth.AuthScreen
import com.example.mbhm.ui.components.C
import com.example.mbhm.ui.components.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MbhmApp() {
    val context = LocalContext.current
    val appRepository = remember { AppRepository.getInstance(context) }
    val sessionManager = remember { SessionManager(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var authenticatedUser by remember { mutableStateOf<UserEntity?>(null) }
    var sessionToastMsg by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    val savedUsername = sessionManager.getLoggedInUsername()
                    if (savedUsername != null) {
                        if (sessionManager.isSessionExpired()) {
                            sessionManager.clearSession()
                            authenticatedUser = null
                            sessionToastMsg = "Session expired due to 10 minutes of inactivity."
                        } else {
                            sessionManager.clearBackgroundTime()
                            if (authenticatedUser == null) {
                                scope.launch(Dispatchers.IO) {
                                    val user = appRepository.getUserByUsername(savedUsername)
                                    withContext(Dispatchers.Main) {
                                        if (user != null) {
                                            authenticatedUser = user
                                        } else {
                                            sessionManager.clearSession()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    sessionManager.recordBackgroundTime()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(C.bg)
            .safeDrawingPadding()
    ) {
        when (val user = authenticatedUser) {
            null -> AuthScreen(
                appRepository = appRepository,
                onLoginSuccess = { loggedInUser ->
                    sessionManager.saveSession(loggedInUser.username)
                    authenticatedUser = loggedInUser
                }
            )
            else -> when (user.role) {
                Role.GUARDIAN -> GuardianApp(onLogout = {
                    sessionManager.clearSession()
                    authenticatedUser = null
                })
                Role.BOARDER -> BoarderApp(
                    user = user,
                    appRepository = appRepository,
                    onLogout = {
                        sessionManager.clearSession()
                        authenticatedUser = null
                    }
                )
            }
        }

        sessionToastMsg?.let { msg ->
            Toast(message = msg, onDone = { sessionToastMsg = null })
        }
    }
}
