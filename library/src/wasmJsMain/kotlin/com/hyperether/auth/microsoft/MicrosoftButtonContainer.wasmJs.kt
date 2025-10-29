package com.hyperether.auth.microsoft

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun MicrosoftButtonContainer(
    modifier: Modifier,
    mode: MicrosoftButtonMode,
    text: String,
    onResult: (Result<MicrosoftSignInResult?>) -> Unit
) {
    val config = MicrosoftSignInConfigHolder.getOrThrow()
    initMicrosoftLogin(config.clientId, config.redirectUri!!) //todo !!
    MicrosoftSignInButton(
        modifier = modifier,
        mode=mode,
        text = text,
        onClick = {
            signIn(
                onResult = onResult
            )
        }
    )
}

external object console {
    fun log(message: String)
    fun error(message: String)
//    fun warn(message: String)
//    fun info(message: String)
//    fun debug(message: String)
}