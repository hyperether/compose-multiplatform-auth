package com.hyperether.auth.apple

import kotlinx.browser.window

object AppleSignInResultHandler {

    fun handleRedirect() {
        val hash = window.location.hash
        val href = window.location.href

        val isPopup = window.opener != null && window != window.opener

        if (isPopup &&
            href.contains("apple-callback") &&
            hash.contains("id_token") &&
            hash.contains("code")
        ) {
            window.opener.asDynamic().postMessage(hash, window.location.origin)
            window.close()
        }
    }
}