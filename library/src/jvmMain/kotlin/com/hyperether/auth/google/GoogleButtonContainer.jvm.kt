package com.hyperether.auth.google

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

@Composable
actual fun GoogleButtonContainer(
    modifier: Modifier,
    mode: GoogleButtonMode,
    text: String,
    shape: Shape,
    filterByAuthorizedAccounts: Boolean,
    requestScopes: List<GoogleSignInRequestScope>,
    onResult: (Result<GoogleSignInResult?>) -> Unit
) {
}