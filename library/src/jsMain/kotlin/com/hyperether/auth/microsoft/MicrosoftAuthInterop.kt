package com.hyperether.auth.microsoft

import kotlinx.browser.window
import kotlin.js.Promise

object  MicrosoftAuthInterop {

    private var msalInstance: msal.PublicClientApplication? = null

    fun configureMsal(clientId: String, redirectUri: String) {
        val config = js("{}")
        config.auth = js("{}")
        config.auth.clientId = clientId
        config.auth.authority = "https://login.microsoftonline.com/common"
        config.auth.redirectUri = redirectUri

        msalInstance = msal.PublicClientApplication(config)
    }

    fun signInWithCallback(
        onSuccess: (String, String, String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        val instance = msalInstance ?: run {
            onError("MSAL not configured. Call configureMsal first.")
            return
        }

        instance.loginPopup(js("{}"))
            .then { loginResponse ->
                val request = js("{}")
                request.scopes = arrayOf("openid", "profile", "User.Read")
                request.account = loginResponse.account
                request.prompt = "select_account"

                instance.acquireTokenSilent(request)
                    .catch { error ->
                        if (error is msal.InteractionRequiredAuthError) {
                            instance.acquireTokenPopup(request)
                        } else {
                            throw error
                        }
                    }
                    .then { tokenResponse ->
                        val idToken = tokenResponse.asDynamic().idToken as? String
                        println("Token jwt :$idToken")
                        val accessToken = tokenResponse.asDynamic().accessToken as? String
                        if (accessToken == null || idToken == null) {
                            onError("Missing tokens in response")
                            return@then
                        }

                        try {
                            val decoded = decodeJwt(idToken)
                            val tenantId = decoded["tid"] ?: "UNKNOWN"
                            val email = decoded["preferred_username"] ?: decoded["email"] ?: "UNKNOWN"
                            onSuccess(idToken, accessToken, tenantId, email)
                        } catch (e: dynamic) {
                            onError("Failed to parse id token: ${e.message}")
                        }
                    }
                    .catch { error ->
                        console.error("MSAL error: ${error.message}")
                        onError(error.message ?: "Unknown error")
                    }
            }
            .catch { error ->
                console.error("MSAL login error: ${error.message}")
                onError(error.message ?: "Login failed")
            }
    }

    fun isUserSignedIn(onChecked: (Boolean) -> Unit) {
        val instance = msalInstance ?: run {
            onChecked(false)
            return
        }

        try {
            val accounts = instance.asDynamic().getAllAccounts() as Array<dynamic>
            onChecked(accounts.isNotEmpty())
        } catch (e: dynamic) {
            console.error("Error checking sign-in status: ${e.message}")
            onChecked(false)
        }
    }

    fun signOut(
        onResult: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        val instance = msalInstance ?: run {
            onError("MSAL not configured. Call configureMsal first.")
            return
        }

        try {
            val dynamicInstance = instance.asDynamic()
            val accounts = dynamicInstance.getAllAccounts() as Array<dynamic>

            if (accounts.isEmpty()) {
                onResult(false) // No one to sign out
                return
            }

            val logoutRequest = js("{}")
            logoutRequest.account = accounts[0]

            dynamicInstance.logoutPopup(logoutRequest)
                .then {
                    onResult(true) // Successfully signed out
                }
                .catch { error ->
                    val msg = error.asDynamic().message as? String ?: "Logout failed"
                    console.error("Logout error: $msg")
                    onError(msg)
                }

        } catch (e: dynamic) {
            val msg = e.message as? String ?: "Exception during sign-out"
            console.error("Sign-out exception: $msg")
            onError(msg)
        }
    }
}

fun decodeJwt(token: String): Map<String, String> {
    val payload = token.split(".")[1]
    val decodedBytes = window.atob(payload.replace('-', '+').replace('_', '/'))
    val json = JSON.parse<dynamic>(decodedBytes)

    return jsObjectToMap(json)
}

fun jsObjectToMap(obj: dynamic): Map<String, String> {
    val result = mutableMapOf<String, String>()
    js("Object").keys(obj).unsafeCast<Array<String>>().forEach { key ->
        result[key] = obj[key]?.toString() ?: ""
    }
    return result
}

external object msal {
    class PublicClientApplication(config: AuthConfig) {
        fun loginPopup(options: dynamic = definedExternally): Promise<LoginResponse>
        fun acquireTokenSilent(request: TokenRequest): Promise<TokenResponse>
        fun acquireTokenPopup(request: TokenRequest): Promise<TokenResponse>
    }

    class InteractionRequiredAuthError : Throwable
}

external interface AuthConfig {
    var auth: Auth
}

external interface Auth {
    var clientId: String
    var authority: String
    var redirectUri: String
}

external interface LoginResponse {
    val account: dynamic
}

external interface TokenRequest {
    var scopes: Array<String>
    var account: dynamic
}

external interface TokenResponse {
    val accessToken: String
}