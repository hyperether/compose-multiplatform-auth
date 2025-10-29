package com.hyperether.auth.microsoft

import com.hyperether.auth.Platform

fun signIn(
    onResult: (Result<MicrosoftSignInResult?>) -> Unit
) {
    try {
        console.log("Calling signInWithCallback...")
        MicrosoftAuth.signInWithCallback(
            successCallback = { token, email ->
                onResult.invoke(
                    Result.success(
                        MicrosoftSignInResult(
                            token = token,
                            accessToken = token,
                            email = email,
                            platform = Platform.WEB
                        )
                    )
                )
            },
            errorCallback = { error ->
                onResult(Result.failure(Exception(error)))
            }
        )
    } catch (e: Throwable) {
        onResult(Result.failure(Exception("Exception signIn call: ${e.cause} - ${e.message}")))
    }
}

fun initMicrosoftLogin(clientId: String, redirectUri: String) {
    configureMsal(clientId, redirectUri)
}

@JsName("configureMsal")
external fun configureMsal(clientId: String, redirectUri: String)

@JsName("microsoftAuth")
external object MicrosoftAuth {
    fun signInWithCallback(
        successCallback: (String, String) -> Unit,
        errorCallback: (String) -> Unit
    )
}