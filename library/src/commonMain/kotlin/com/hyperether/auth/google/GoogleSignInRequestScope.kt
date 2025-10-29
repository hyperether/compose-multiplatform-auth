package com.hyperether.auth.google

sealed interface GoogleSignInRequestScope {
    data object Profile : GoogleSignInRequestScope
    data object Email : GoogleSignInRequestScope
}