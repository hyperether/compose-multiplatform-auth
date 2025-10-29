package com.hyperether.auth.microsoft

data class MicrosoftSignInConfig(
    val clientId: String,
    val redirectUri: String? = null // only required for Web
)