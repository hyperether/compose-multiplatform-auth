package com.hyperether.auth.apple

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

@Composable
actual fun AppleButtonContainer(
    modifier: Modifier,
    mode: AppleButtonMode,
    text: String,
    shape: Shape,
    linkAccount: Boolean,
    requestScopes: List<AppleSignInRequestScope>,
    onResult: (Result<AppleSignInResult?>) -> Unit
) {
    val context = LocalContext.current

    AppleSignInButton(
        modifier = modifier,
        mode = mode,
        text = text,
        shape = shape,
        onClick = {
            signIn(
                context = context,
                onResult = onResult
            )
        })
}

private fun signIn(
    context: Context,
    onResult: (Result<AppleSignInResult?>) -> Unit
) {
    try {
        val config = AppleSignInConfigHolder.getOrThrow()
        val nonce = UUID.randomUUID().toString()
        val hashedNonce = sha256(nonce)

        AppleSignInResultHandler.onResult = onResult

        if (config.iosServiceId.isEmpty() || config.redirectUri.isNullOrEmpty()) {
            AppleSignInResultHandler.onResult?.invoke(Result.failure(Exception("Missing Apple Sign In config.")))
            return
        }

        val uri = Uri.Builder()
            .scheme("https")
            .authority("appleid.apple.com")
            .path("auth/authorize")
            .appendQueryParameter("response_type", "code id_token")
            .appendQueryParameter("response_mode", "fragment")
            .appendQueryParameter("client_id", config.iosServiceId)
            .appendQueryParameter("redirect_uri", config.redirectUri)
            .appendQueryParameter("state", nonce)
            .appendQueryParameter("nonce", hashedNonce)
            .build()

        val intent = CustomTabsIntent.Builder().build()
        intent.launchUrl(context, uri)
    } catch (e: Exception) {
        AppleSignInResultHandler.onResult?.invoke(Result.failure(e))
    }
}

fun sha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
    return Base64.encodeToString(hash, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}