package com.hyperether.auth.apple

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

@Composable
actual fun AppleButtonContainer(
    modifier: Modifier,
    mode: AppleButtonMode,
    text: String,
    shape: Shape,
    linkAccount: Boolean,
    requestScopes: List<AppleSignInRequestScope>,
    onResult: (Result<AppleSignInResult?>) -> Unit
) {
}