package com.hyperether.auth.microsoft

sealed interface MicrosoftSignInRequestScope {
    data object Profile : MicrosoftSignInRequestScope //todo
    data object Email : MicrosoftSignInRequestScope
}