package com.hyperether.auth.microsoft

import com.hyperether.auth.Platform

data class MicrosoftSignInResult(
    val token: String? = null,
    val accessToken: String? = null,
    val email: String? = null,
    val tenantId: String? = null,
    val platform: Platform
)
