package com.hyperether.auth.microsoft

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun MicrosoftButtonContainer(
    modifier: Modifier,
    mode: MicrosoftButtonMode,
    text: String,
    onResult: (Result<MicrosoftSignInResult?>) -> Unit
) {
    val context = LocalContext.current

    MicrosoftSignInButton(
        modifier = modifier,
        mode = mode,
        text = text,
        onClick = {
                signIn(
                    context,
                    onResult = onResult
                )
        }
    )
}