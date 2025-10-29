package com.hyperether.auth.google

import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

@Composable
expect fun GoogleButtonContainer(
    modifier: Modifier = Modifier,
    mode: GoogleButtonMode = GoogleButtonMode.WhiteWithOutline,
    text: String = "Sign in with Google",
    shape: Shape = ButtonDefaults.shape,
    filterByAuthorizedAccounts: Boolean = false,
    requestScopes: List<GoogleSignInRequestScope> = listOf(
        GoogleSignInRequestScope.Profile,
        GoogleSignInRequestScope.Email
    ),
    onResult: (Result<GoogleSignInResult?>) -> Unit
)