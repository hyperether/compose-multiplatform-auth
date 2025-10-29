package com.hyperether.auth.google

import kotlinx.browser.window

object GoogleSignInResultHandler {

    fun handleRedirect() {
        val hash = window.location.hash
        val href = window.location.href

        val isPopup = window.opener != null && window != window.opener

        if (isPopup &&
            href.contains("google-callback") &&
            hash.contains("id_token") &&
            hash.contains("access_token")
        ) {
            window.opener.asDynamic().postMessage(hash, window.location.origin)
            window.close()
        }
    }
}