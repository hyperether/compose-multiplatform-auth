package com.hyperether.auth.google

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import cocoapods.GoogleSignIn.GIDConfiguration
import cocoapods.GoogleSignIn.GIDSignIn
import kotlinx.cinterop.ExperimentalForeignApi
import com.hyperether.auth.Platform
import platform.UIKit.UIApplication

@Composable
actual fun GoogleButtonContainer(
    modifier: Modifier,
    mode: GoogleButtonMode,
    text: String,
    shape: Shape,
    filterByAuthorizedAccounts: Boolean,
    requestScopes: List<GoogleSignInRequestScope>,
    onResult: (Result<GoogleSignInResult?>) -> Unit
) {

    GoogleSignInButton(
        modifier = modifier,
        mode = mode,
        text = text,
        shape = shape,
        onClick = {
            signIn(
                onResult = onResult
            )
        })
}

@OptIn(ExperimentalForeignApi::class)
private fun signIn(
    onResult: (Result<GoogleSignInResult?>) -> Unit
) {
    val presentingController = UIApplication.sharedApplication.keyWindow?.rootViewController
    if (presentingController == null) {
        onResult(Result.failure(IllegalStateException("No rootViewController available")))
        return
    }

    try {
        val config = GoogleSignInConfigHolder.getOrThrow()
        val clientId = config.iosClientId
        if (clientId.isNullOrEmpty()) {
            onResult(Result.failure(IllegalStateException("Missing iosClientId")))
            return
        }

        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID = clientId)
        GIDSignIn.sharedInstance.signInWithPresentingViewController(
            presentingViewController = presentingController,
            completion = { signInResult, error ->
                if (error != null || signInResult == null) {
                    onResult(Result.failure(Exception("Sign in cancelled")))
                    return@signInWithPresentingViewController
                }

                val user = signInResult.user
                val idToken = user.idToken?.tokenString
                if (idToken != null) {
                    onResult(
                        Result.success(
                            GoogleSignInResult(
                                idToken = idToken,
                                userId =  user.userID,
                                email = user.profile?.email,
                                fullName = user.profile?.name,
                                platform = Platform.IOS
                            )
                        )
                    )
                } else {
                    onResult(Result.failure(Exception("Missing idToken")))
                }
            }
        )
    } catch (e: Exception) {
        onResult(Result.failure(e))
    }
}