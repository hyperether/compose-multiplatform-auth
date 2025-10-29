package com.hyperether.auth.microsoft

import com.hyperether.auth.Platform

fun signIn(
    onResult: (Result<MicrosoftSignInResult?>) -> Unit
) {
    try {
        MicrosoftAuthInterop.signInWithCallback(
            onSuccess = { token, accessToken, tenantId, email ->
                onResult.invoke(
                    Result.success(
                        MicrosoftSignInResult(
                            token = token,
                            accessToken = accessToken,
                            email = email,
                            tenantId = tenantId,
                            platform = Platform.WEB
                        )
                    )
                )
            },
            onError = { error ->
                onResult(Result.failure(Exception(error)))
            }
        )
    } catch (e: Throwable) {
        onResult(Result.failure(Exception("Exception during signIn call: ${e.cause} - ${e.message}")))
    }
}

fun initMicrosoftLogin(clientId: String, redirectUri: String) {
    configureMsal(clientId, redirectUri)
}

fun configureMsal(clientId: String, redirectUri: String) {
    MicrosoftAuthInterop.configureMsal(clientId, redirectUri)
}