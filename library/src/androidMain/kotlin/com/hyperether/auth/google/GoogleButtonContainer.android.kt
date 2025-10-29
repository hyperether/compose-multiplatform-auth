package com.hyperether.auth.google

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.hyperether.auth.Platform
import org.json.JSONObject
import java.util.Base64

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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val launcher = getLegacyLauncher(onResult)

    GoogleSignInButton(
        modifier = modifier,
        mode = mode,
        text = text,
        shape = shape,
        onClick = {
            coroutineScope.launch {
                signIn(
                    context = context,
                    filterByAuthorizedAccounts = filterByAuthorizedAccounts,
                    requestScopes = requestScopes,
                    launchLegacyIntent = {
                        launcher.launch(it)
                    },
                    onResult = onResult
                )
            }
        })
}

private suspend fun signIn(
    context: Context,
    filterByAuthorizedAccounts: Boolean,
    requestScopes: List<GoogleSignInRequestScope>,
    launchLegacyIntent: (Intent) -> Unit,
    onResult: (Result<GoogleSignInResult?>) -> Unit
) {
    try {
        val config = GoogleSignInConfigHolder.getOrThrow()
        val clientId = config.webClientId
        if (clientId.isNullOrEmpty()) {
            onResult(Result.failure(IllegalStateException("Missing webClientId")))
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Credential Manager (API 34+)
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(clientId)
                .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            try {
                val result = credentialManager.getCredential(context, request)
                val credentialBundle = result.credential.data
                val googleCred = GoogleIdTokenCredential.createFrom(credentialBundle)

                val userId = extractSubFromJwt(googleCred.idToken)
                onResult(
                    Result.success(
                        GoogleSignInResult(
                            idToken = googleCred.idToken,
                            userId = userId,
                            email = googleCred.id,
                            fullName = googleCred.displayName,
                            platform = Platform.ANDROID
                        )
                    )
                )
            } catch (e: GetCredentialException) {
                if (e is GetCredentialCancellationException) {
                    onResult(Result.failure(CancellationException("User cancelled Sign-in")))
                    return
                }
                legacyLogin(
                    context = context,
                    clientId = clientId,
                    requestScopes = requestScopes,
                    launchLegacyIntent = launchLegacyIntent
                )
            }
        } else {
            legacyLogin(
                context = context,
                clientId = clientId,
                requestScopes = requestScopes,
                launchLegacyIntent = launchLegacyIntent
            )
        }
    } catch (e: Exception) {
        onResult(Result.failure(e))
    }
}

private fun legacyLogin(
    context: Context,
    clientId: String,
    requestScopes: List<GoogleSignInRequestScope>,
    launchLegacyIntent: (Intent) -> Unit
) {
    // Legacy fallback
    val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(clientId)
    if (requestScopes.contains(GoogleSignInRequestScope.Email)) {
        gsoBuilder.requestEmail()
    }
    val gso = gsoBuilder.build()

    val signInClient = GoogleSignIn.getClient(context, gso)
    signInClient.signOut().addOnCompleteListener {
        launchLegacyIntent(signInClient.signInIntent)
    }
}

@Composable
private fun getLegacyLauncher(
    onResult: (Result<GoogleSignInResult?>) -> Unit
) =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account?.idToken != null) {
                onResult(
                    Result.success(
                        GoogleSignInResult(
                            idToken = account.idToken!!,
                            userId = account.id,
                            email = account.email,
                            fullName = account.displayName,
                            platform = Platform.ANDROID
                        )
                    )
                )
            } else {
                onResult(Result.failure(Exception("Missing idToken")))
            }
        } catch (e: Exception) {
            if (e is ApiException && e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                onResult(Result.failure(CancellationException("User cancelled Sign-In")))
            } else {
                onResult(Result.failure(e))
            }
        }
    }

@RequiresApi(Build.VERSION_CODES.O)
private fun extractSubFromJwt(jwt: String): String? {
    val parts = jwt.split(".")
    if (parts.size != 3) return null

    return try {
        val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]))
        val payload = JSONObject(payloadJson)
        payload.optString("sub")
    } catch (e: Exception) {
        null
    }
}