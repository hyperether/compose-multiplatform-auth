package com.hyperether.auth.google

data class GoogleSignInConfig(
    val webClientId: String? = null,
    val iosClientId: String? = null,
    val redirectUri: String? = null // only required for Web
)