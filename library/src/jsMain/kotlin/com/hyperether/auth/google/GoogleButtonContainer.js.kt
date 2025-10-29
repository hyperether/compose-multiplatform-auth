package com.hyperether.auth.google

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
actual fun GoogleButtonContainer(
    modifier: Modifier,
    mode: GoogleButtonMode,
    text: String,
    shape: Shape,
    filterByAuthorizedAccounts: Boolean,
    requestScopes: List<GoogleSignInRequestScope>,
    onResult: (Result<GoogleSignInResult?>) -> Unit
) {

    DisposableEffect(Unit) {
        val listener = getMessageEventListener(onResult)

        window.addEventListener("message", listener)
        onDispose {
            window.removeEventListener("message", listener)
        }
    }

    GoogleSignInButton(
        modifier = modifier,
        mode = mode,
        text = text,
        shape = shape,
        onClick = {
            signInWithPopup(
                requestScopes = requestScopes,
                onResult = onResult
            )
        }
    )
}

private fun signInWithPopup(
    requestScopes: List<GoogleSignInRequestScope>,
    onResult: (Result<GoogleSignInResult?>) -> Unit
) {
    try {
        val config = GoogleSignInConfigHolder.getOrThrow()
        val clientId = config.webClientId
        val redirectUri = config.redirectUri

        if (clientId.isNullOrEmpty() || redirectUri.isNullOrEmpty()) {
            onResult(Result.failure(IllegalStateException("GoogleSignInConfigHolder is not configured properly")))
            return
        }

        val authUrl = buildGoogleSignInUrl(clientId, redirectUri, requestScopes)

        val popup = getGooglePopup()
        val safeUrl = authUrl.replace("\"", "\\\"")

        openGooglePopup(popup, safeUrl)
    } catch (e: Exception) {
        onResult(Result.failure(e))
    }
}

private fun getMessageEventListener(onResult: (Result<GoogleSignInResult?>) -> Unit): (Event) -> Unit {
    val listener: (Event) -> Unit = { rawEvent ->
        val event = rawEvent as? MessageEvent
        if (event != null) {
            val data = event.data as? String
            if (data != null) {
                if (data.contains("id_token") && data.contains("access_token")) {
                    try {
                        val params = URLSearchParams(data.removePrefix("#"))
                        val idToken = params.get("id_token")
                        val googleData = parseGoogleIdToken(idToken!!)

                        onResult(
                            Result.success(
                                GoogleSignInResult(
                                    idToken = idToken,
                                    userId = googleData.userId,
                                    email = googleData.email,
                                    fullName = googleData.name,
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

private fun openGooglePopup(popup: Window, url: String) {
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

private fun getGooglePopup(): Window {
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

private data class GoogleJWT(
    val email: String?,
    val name: String?,
    val userId: String?
)

private fun parseGoogleIdToken(idToken: String): GoogleJWT {
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
    val name = json.name as? String
    val sub = json.sub as? String
    return GoogleJWT(
        email = email,
        name = name,
        userId = sub
    )
}

private fun decodeBase64(base64: String): String {
    return js("atob")(base64) as String
}

private fun buildGoogleSignInUrl(
    clientId: String,
    redirectUri: String,
    requestScopes: List<GoogleSignInRequestScope>
): String {

    val nonce = generateNonce()
    val encodedRedirectUri = encodeURIComponent(redirectUri)
    val scopeString = buildScope(requestScopes)
    val encodedScope = encodeURIComponent(scopeString)

    return buildString {
        append("https://accounts.google.com/o/oauth2/v2/auth")
        append("?response_type=id_token token")
        append("&client_id=$clientId")
        append("&redirect_uri=$encodedRedirectUri")
        append("&scope=$encodedScope")
        append("&nonce=$nonce")
        append("&prompt=select_account")
        append("&response_mode=fragment")
    }
}

private fun buildScope(requestScopes: List<GoogleSignInRequestScope>): String {
    val baseScopes = listOf("openid")
    val extra = requestScopes.map {
        when (it) {
            GoogleSignInRequestScope.Email -> "email"
            GoogleSignInRequestScope.Profile -> "profile"
        }
    }
    return (baseScopes + extra).distinct().joinToString(" ")
}

private fun generateNonce(): String {
    val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return List(16) { chars.random() }.joinToString("")
}

@JsName("encodeURIComponent")
private external fun encodeURIComponent(str: String): String