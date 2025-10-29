package com.hyperether.auth.apple

import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

@Composable
expect fun AppleButtonContainer(
    modifier: Modifier = Modifier,
    mode: AppleButtonMode = AppleButtonMode.WhiteWithOutline,
    text: String = "Sign in with Apple",
    shape: Shape = ButtonDefaults.shape,
    linkAccount: Boolean = false,
    requestScopes: List<AppleSignInRequestScope> = listOf(
        AppleSignInRequestScope.FullName,
        AppleSignInRequestScope.Email
    ),
    onResult: (Result<AppleSignInResult?>) -> Unit
)