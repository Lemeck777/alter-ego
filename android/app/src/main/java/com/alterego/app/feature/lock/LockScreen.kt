package com.alterego.app.feature.lock

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.core.design.PrimaryButton
import com.alterego.app.core.design.QuietButton
import com.alterego.app.domain.models.AppLockMode
import java.util.concurrent.Executor

private fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

/**
 * Optional lock. Nobody glancing at this phone needs to see someone's commitments.
 *
 * The biometric prompt is retryable: a cancelled or failed authentication leaves a visible way back
 * in, rather than stranding the user on a screen with no controls.
 */
@Composable
fun LockScreen(mode: AppLockMode, onVerifyPin: (String) -> Boolean, onUnlocked: () -> Unit) {
    val colors = LocalPersonaColors.current
    var pin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var biometricMessage by remember { mutableStateOf<String?>(null) }
    var promptAttempt by remember { mutableIntStateOf(0) }
    val activity = LocalContext.current.findFragmentActivity()

    if (mode == AppLockMode.BIOMETRIC) {
        LaunchedEffect(promptAttempt) {
            val fragmentActivity = activity
            if (fragmentActivity == null) {
                biometricMessage = "Unlock isn't available here."
                return@LaunchedEffect
            }
            val executor: Executor = ContextCompat.getMainExecutor(fragmentActivity)
            val prompt = BiometricPrompt(
                fragmentActivity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onUnlocked()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        biometricMessage = errString.toString().ifBlank { "Unlock was cancelled." }
                    }

                    override fun onAuthenticationFailed() {
                        biometricMessage = "That didn't match. Try again."
                    }
                },
            )
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Alter Ego")
                    .setSubtitle("Unlock to continue")
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                    )
                    .build(),
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("ALTER EGO", style = MaterialTheme.typography.labelSmall, color = colors.muted)
        Text(
            "Just you.",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBackground,
            modifier = Modifier.padding(top = 12.dp, bottom = 32.dp),
        )

        when (mode) {
            AppLockMode.PIN -> {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) { pin = it; pinError = false } },
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = pinError,
                    singleLine = true,
                )
                if (pinError) {
                    Text(
                        "Not quite.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.accent,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                PrimaryButton(text = "Unlock", modifier = Modifier.padding(top = 24.dp)) {
                    if (!onVerifyPin(pin)) { pinError = true; pin = "" }
                }
            }

            AppLockMode.BIOMETRIC -> {
                biometricMessage?.let { message ->
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 20.dp),
                    )
                    PrimaryButton(text = "Try again") { biometricMessage = null; promptAttempt++ }
                    QuietButton(text = "Close Alter Ego", modifier = Modifier.padding(top = 10.dp)) {
                        activity?.finish()
                    }
                }
            }

            AppLockMode.NONE -> Unit
        }
    }
}
