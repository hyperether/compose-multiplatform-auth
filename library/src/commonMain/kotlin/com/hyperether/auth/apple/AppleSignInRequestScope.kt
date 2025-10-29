package com.hyperether.auth.apple

sealed interface AppleSignInRequestScope {
    data object FullName : AppleSignInRequestScope
    data object Email : AppleSignInRequestScope
}