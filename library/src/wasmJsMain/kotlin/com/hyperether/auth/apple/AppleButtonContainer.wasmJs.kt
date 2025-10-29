package com.hyperether.auth.apple

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import kotlinx.browser.window
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.hyperether.auth.Platform
import org.w3c.dom.MessageEvent
import org.w3c.dom.Window
import org.w3c.dom.events.Event
import org.w3c.dom.url.URLSearchParams
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

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

    DisposableEffect(Unit) {
        val listener = getMessageEventListener(onResult)

        window.addEventListener("message", listener)

        onDispose {
            window.removeEventListener("message", listener)
        }
    }

    AppleSignInButton(
        modifier = modifier,
        mode = mode,
        text = text,
        shape = shape,
        onClick = {
            signInWithPopup(
                onResult = onResult
            )
        })
}

private fun signInWithPopup(
    onResult: (Result<AppleSignInResult?>) -> Unit
) {
    try {
        val config = AppleSignInConfigHolder.getOrThrow()
        val clientId = config.iosServiceId
        val redirectUri = config.redirectUri

        if (clientId.isEmpty() || redirectUri.isNullOrEmpty()) {
            onResult(Result.failure(IllegalStateException("AppleSignInConfigHolder is not configured properly")))
            return
        }

        val authUrl = buildAppleSignInUrl(clientId, redirectUri)

        val popup = getAppleSignInPopup()
        val safeUrl = authUrl.replace("\"", "\\\"")

        openAppleSignInPopup(popup, safeUrl)
    } catch (e: Exception) {
        onResult(Result.failure(e))
    }
}

private fun getMessageEventListener(onResult: (Result<AppleSignInResult?>) -> Unit): (Event) -> Unit {
    val listener: (Event) -> Unit = { rawEvent ->
        val event = rawEvent as? MessageEvent
        if (event != null) {
            val data = event.data as Any
            if (data is String) {
                if (data.contains("id_token") && data.contains("code")) {
                    try {
                        val params = URLSearchParams(emptyJs())

                        data.split("&").forEach {
                            val parts = it.split("=")
                            if (parts.size == 2) params.append(parts[0], parts[1])
                        }

                        val idToken = params.get("id_token")
                        val accessToken = params.get("code")
                        val googleData = parseGoogleIdToken(idToken!!)

                        onResult(
                            Result.success(
                                AppleSignInResult(
                                    idToken = idToken,
                                    authCode = accessToken,
                                    userId = googleData.sub,
                                    email = googleData.email,
                                    platform = Platform.WEB
                                )
                            )
                        )
                    } catch (e: Exception) {
                        onResult(Result.failure(e))
                    }
                }
            }
        }
    }
    return listener
}

private fun openAppleSignInPopup(popup: Window, url: String) {
    popup.document.open()
    popup.document.write(
        """
        <!DOCTYPE html>
        <html>
        <head><title>Signing in...</title></head>
        <body>
        <script>
            const authUrl = "$url";

            location.href = authUrl;

            const poll = setInterval(() => {
                try {
                    const hash = location.hash;
                    if (hash.includes("id_token")) {
                        clearInterval(poll);
                        window.opener.postMessage(hash, window.location.origin);
                        window.close();
                    }
                } catch (e) {
                    // Ignore cross-origin error
                }
            }, 200);
        </script>
        </body>
        </html>
        """.trimIndent()
    )
    popup.document.close()
}

private fun getAppleSignInPopup(): Window {
    val popupWidth = 500
    val popupHeight = 600

    val left = window.screenX + (window.outerWidth - popupWidth) / 2
    val top = window.screenY + (window.outerHeight - popupHeight) / 2

    val features =
        "width=$popupWidth,height=$popupHeight,left=$left,top=$top,resizable,scrollbars=yes"

    return window.open(
        url = "",
        target = "_blank",
        features = features
    ) ?: throw IllegalStateException("Popup blocked")
}

@Serializable
private data class GoogleJwtClaims(
    val email: String? = null,
    val sub: String? = null
)

private fun parseGoogleIdToken(idToken: String): GoogleJwtClaims {
    val parts = idToken.split(".")
    if (parts.size != 3) throw IllegalArgumentException("Invalid JWT")

    val payloadBase64 = parts[1]
    val jsonString = decodeBase64UrlSafe(payloadBase64)

    val claims = Json.decodeFromString<GoogleJwtClaims>(jsonString)
    return GoogleJwtClaims(
        email = claims.email,
        sub = claims.sub
    )
}

@OptIn(ExperimentalEncodingApi::class)
private fun decodeBase64UrlSafe(input: String): String {
    val fixed = input
        .replace("-", "+")
        .replace("_", "/")
        .let { it.padEnd((it.length + 3) / 4 * 4, '=') }

    return Base64.decode(fixed.encodeToByteArray()).decodeToString()
}

private fun buildAppleSignInUrl(
    clientId: String,
    redirectUri: String,
): String {

    val encodedRedirectUri = encodeURIComponent(redirectUri)

    return buildString {
        append("https://appleid.apple.com/auth/authorize")
        append("?response_type=code%20id_token")
        append("&response_mode=fragment")
        append("&client_id=$clientId")
        append("&redirect_uri=$encodedRedirectUri")
    }
}

private fun emptyJs(): JsAny = js("({})")

@JsName("encodeURIComponent")
private external fun encodeURIComponent(component: String): String