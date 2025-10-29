package com.hyperether.auth.apple

import kotlinx.serialization.Serializable
import com.hyperether.auth.Platform

@Serializable
data class AppleSignInResult(
    val idToken: String,
    val authCode: String? = null,
    val userId: String? = null,
    val email: String? = null,
    val fullName: FullName? = null,
    val platform: Platform
) {
    override fun toString(): String {
        return buildString {
            appendLine("AppleSignInResult(")
            appendLine("  idToken = $idToken")
            appendLine("  authorizationCode = $authCode")
            appendLine("  userId = $userId")
            appendLine("  email = $email")
            appendLine("  fullName = $fullName")
            appendLine("  platform = $platform")
            append(")")
        }
    }
}

@Serializable
data class FullName(
    val givenName: String? = null,
    val familyName: String? = null,
) {
    override fun toString(): String {
        return buildString {
            appendLine("FullName(")
            appendLine("    givenName = $givenName.")
            appendLine("    familyName = $familyName")
            append("  )")
        }
    }
}

