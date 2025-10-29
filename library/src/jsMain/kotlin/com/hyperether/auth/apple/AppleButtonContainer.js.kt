package com.hyperether.auth.apple

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import kotlinx.browser.window
import com.hyperether.auth.Platform
import org.w3c.dom.MessageEvent
import org.w3c.dom.Window
import org.w3c.dom.events.Event
import org.w3c.dom.url.URLSearchParams

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
            val data = event.data as? String
            if (data != null) {
                if (data.contains("id_token") && data.contains("code")) {
                    try {
                        val params = URLSearchParams(data.removePrefix("#"))
                        val idToken = params.get("id_token")
                        val accessToken = params.get("code")

                        if (idToken != null) {
                            val parsedTokenData = parseAppleJWT(idToken)
                            onResult(
                                Result.success(
                                    AppleSignInResult(
                                        idToken = idToken,
                                        authCode = accessToken,
                                        userId = parsedTokenData.userId,
                                        email = parsedTokenData.email,
                                        platform = Platform.WEB
                                    )
                                )
                            )
                        }
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

private data class AppleJWTResult(
    val email: String?,
    val userId: String?
)

private fun parseAppleJWT(idToken: String): AppleJWTResult {
    val parts = idToken.split(".")
    if (parts.size != 3) throw IllegalArgumentException("Invalid JWT")

    val rawPayload = parts[1]
    val base64 = rawPayload
        .replace('-', '+')
        .replace('_', '/')
        .let { it.padEnd((it.length + 3) / 4 * 4, '=') }

    val decodedJson = decodeBase64(base64)
    val json = JSON.parse<dynamic>(decodedJson)

    val email = json.email as? String
    val sub = json.sub as? String

    return AppleJWTResult(
        email = email,
        userId = sub
    )
}

private fun decodeBase64(base64: String): String {
    return js("atob")(base64) as String
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

@JsName("encodeURIComponent")
private external fun encodeURIComponent(component: String): String