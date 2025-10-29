package com.hyperether.auth.microsoft

import android.app.Activity
import android.content.Context
import android.util.Log
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.Prompt
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import com.hyperether.auth.Platform
import java.io.File

private var mSingleAccountApp: ISingleAccountPublicClientApplication? = null

fun signIn(
    context: Context,
    onResult: (Result<MicrosoftSignInResult?>) -> Unit
) {
    val clientId = MicrosoftSignInConfigHolder.getOrThrow().clientId
    val configFile = writeMsalConfigFile(context, clientId)
    if (mSingleAccountApp == null) {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            configFile,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    mSingleAccountApp = application
                    clearAccountAndLaunchSignIn(context, onResult)
                }

                override fun onError(exception: MsalException) {
                    Log.e("MSAL", "Failed to create MSAL instance: ${exception.message}")
                    onResult.invoke(Result.failure(Exception(exception.message ?: "Initialization error")))
                }
            }
        )
    } else {
        clearAccountAndLaunchSignIn(context, onResult)
    }
}

fun clearAccountAndLaunchSignIn(context: Context, onResult: (Result<MicrosoftSignInResult?>) -> Unit) {
    isUserSignedIn(onChecked = { result ->
        if (result) {
            signOut(onSignOut = {
                launchSignIn(context, onResult)
            } )
        } else {
            launchSignIn(context, onResult)
        }
    })
}

private fun launchSignIn(
    context: Context,
    onResult: (Result<MicrosoftSignInResult?>) -> Unit
) {
    val activity = context as? Activity ?: return onResult.invoke(Result.failure(Exception("Invalid activity context")))

    val scopes = mutableListOf("openid", "profile", "User.Read")

    val parameters = AcquireTokenParameters.Builder()
        .startAuthorizationFromActivity(activity)
        .withScopes(scopes)
        .withPrompt(Prompt.SELECT_ACCOUNT)
        .withCallback(object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                val accessToken = authenticationResult.accessToken
                val tenantId = authenticationResult.tenantId
                Log.d("MSAL", "Access token: $accessToken")
                onResult(
                    Result.success(
                        MicrosoftSignInResult(
                            token = authenticationResult.account.idToken,
                            accessToken = authenticationResult.accessToken,
                            email = authenticationResult.account.username,
                            tenantId = tenantId,
                            platform = Platform.ANDROID,
                        )
                    )
                )
            }

            override fun onError(exception: MsalException) {
                Log.e("MSAL", "Sign-in error: ${exception.message}")
                onResult.invoke(Result.failure(Exception(exception.message ?: "Sign-in error")))
            }

            override fun onCancel() {
                Log.d("MSAL", "User cancelled sign-in.")
                onResult.invoke(Result.failure(Exception("Sign-in cancelled")))
            }
        })
        .build()

    mSingleAccountApp?.acquireToken(parameters)
        ?: onResult.invoke(Result.failure(Exception("MSAL not initialized")))
}

fun signOut(onSignOut: () -> Unit) {
    mSingleAccountApp?.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
        override fun onSignOut() {
            Log.d("MSAL", "Signed out successfully")
            onSignOut()
            // Navigate to login screen or clear session
        }

        override fun onError(exception: MsalException) {
            Log.e("MSAL", "Sign out failed: ${exception.message}")
            onSignOut()
        }
    })
}

fun isUserSignedIn(onChecked: (Boolean) -> Unit) {
    mSingleAccountApp?.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
        override fun onAccountLoaded(activeAccount: IAccount?) {
            onChecked(activeAccount != null)
        }

        override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
            // Optional: handle if account changes
        }

        override fun onError(exception: MsalException) {
            Log.e("MSAL", "Error checking account: ${exception.message}")
            onChecked(false)
        }
    }) ?: onChecked(false)
}

fun writeMsalConfigFile(context: Context, clientId: String): File {
    val configContent = """
        {
          "client_id": "$clientId",
          "redirect_uri": "msal$clientId://auth",
          "authorization_user_agent": "DEFAULT",
          "account_mode": "SINGLE"
        }
    """.trimIndent()

    val file = File(context.filesDir, "msal_config.json")
    file.writeText(configContent)
    return file
}