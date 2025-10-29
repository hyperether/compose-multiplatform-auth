package com.hyperether.auth.microsoft

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun MicrosoftButtonContainer(
    modifier: Modifier = Modifier,
    mode: MicrosoftButtonMode = MicrosoftButtonMode.WhiteWithOutline,
    text: String = "Sign in with Microsoft",
    onResult: (Result<MicrosoftSignInResult?>) -> Unit,
)