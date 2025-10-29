package com.hyperether.auth.apple

data class AppleSignInConfig(
    val iosServiceId: String,
    val redirectUri: String? = null // only required for Web/Android
)