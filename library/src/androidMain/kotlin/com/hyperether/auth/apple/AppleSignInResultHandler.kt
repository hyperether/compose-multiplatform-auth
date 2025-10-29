package com.hyperether.auth.apple

import android.content.Intent
import android.net.Uri
import android.util.Base64
import com.hyperether.auth.Platform
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object AppleSignInResultHandler {
    var onResult: ((Result<AppleSignInResult?>) -> Unit)? = null

    fun handleRedirect(intent: Intent) {
        val uri = intent.data
        if (uri == null || uri.path != "/apple-callback") {
            onResult = null
            return
        }

        try {
            val fragment = uri.fragment ?: ""
            val params = fragment.split("&").associate {
                val (key, value) = it.split("=")
                key to Uri.decode(value)
            }

            val idToken = params["id_token"]
            val code = params["code"]
            val userId = idToken?.let { parseSubFromIdToken(it) }
            val email = idToken?.let { parseEmailFromIdToken(it) }

            if (idToken != null && userId != null) {
                val result = AppleSignInResult(
                    idToken = idToken,
                    authCode = code,
                    userId = userId,
                    email = email,
                    platform = Platform.ANDROID
                )
                onResult?.invoke(Result.success(result))
            } else {
                onResult?.invoke(Result.failure(Exception("Missing idToken or userId")))
            }
        } catch (e: Exception) {
            onResult?.invoke(Result.failure(e))
        } finally {
            onResult = null
        }
    }

    private fun parseSubFromIdToken(idToken: String?): String? {
        val parts = idToken?.split(".")
        if (parts?.size == 3) {
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE), StandardCharsets.UTF_8)
            val json = JSONObject(payload)
            return json.optString("sub")
        }
        return null
    }

    private fun parseEmailFromIdToken(idToken: String): String? {
        return try {
            val payload = idToken.split(".").getOrNull(1)
                ?.let {
                    val padded = it.padEnd((it.length + 3) / 4 * 4, '=')
                    Base64.decode(
                        padded,
                        Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
                    )
                }
                ?.toString(Charsets.UTF_8)

            val regex = """"email"\s*:\s*"([^"]+)"""".toRegex()
            regex.find(payload ?: "")?.groupValues?.getOrNull(1)
        } catch (e: Exception) {
            null
        }
    }
}