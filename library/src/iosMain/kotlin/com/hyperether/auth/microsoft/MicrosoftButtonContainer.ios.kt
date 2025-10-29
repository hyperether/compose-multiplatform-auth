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
}