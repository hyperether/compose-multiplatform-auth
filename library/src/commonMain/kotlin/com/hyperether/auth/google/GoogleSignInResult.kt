package com.hyperether.auth.google

import kotlinx.serialization.Serializable
import com.hyperether.auth.Platform

@Serializable
data class GoogleSignInResult(
    val idToken: String,
    val userId: String? = null,
    val email: String? = null,
    val fullName: String? = null,
    val platform: Platform
) {
    override fun toString(): String {
        return buildString {
            appendLine("GoogleSignInResult(")
            appendLine("  idToken = $idToken")
            appendLine("  userId = $userId")
            appendLine("  email = $email")
            appendLine("  fullName = $fullName")
            appendLine("  platform = $platform")
            append(")")
        }
    }
}