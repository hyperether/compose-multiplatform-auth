package com.hyperether.auth.apple

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import com.hyperether.auth.Platform
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASAuthorizationScopeFullName
import platform.AuthenticationServices.ASPresentationAnchor
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault
import platform.UIKit.UIApplication
import platform.darwin.NSObject

private var currentNonce: String? = null

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
    val updatedOnResultFunc by rememberUpdatedState(onResult)
    val presentationContextProvider = PresentationContextProvider()
    val asAuthorizationControllerDelegate =
        ASAuthorizationControllerDelegate(linkAccount, updatedOnResultFunc)


    AppleSignInButton(
        modifier = modifier,
        mode = mode,
        text = text,
        shape = shape,
        onClick = {
            signIn(
                requestScopes = requestScopes,
                authorizationController = asAuthorizationControllerDelegate,
                presentationContextProvider = presentationContextProvider
            )
        })
}

private fun signIn(
    requestScopes: List<AppleSignInRequestScope>,
    authorizationController: ASAuthorizationControllerDelegate,
    presentationContextProvider: PresentationContextProvider,
) {
    val appleIdProviderRequest = ASAuthorizationAppleIDProvider().createRequest()
    appleIdProviderRequest.requestedScopes = requestScopes.map {
        when (it) {
            AppleSignInRequestScope.Email -> ASAuthorizationScopeEmail
            AppleSignInRequestScope.FullName -> ASAuthorizationScopeFullName
        }
    }
    val nonce = randomNonceString()
    currentNonce = nonce
    appleIdProviderRequest.nonce = sha256(nonce)
    val requests = listOf(appleIdProviderRequest)
    val controller = ASAuthorizationController(requests)
    controller.delegate = authorizationController
    controller.presentationContextProvider = presentationContextProvider
    controller.performRequests()
}


private fun randomNonceString(length: Int = 32): String {
    require(length > 0) { "Length must be greater than 0" }
    val randomBytes = iosSecureRandomBytes(length)
    val charset = "0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._"
    val nonce = randomBytes.map { byte ->
        charset[(byte.toInt() and 0xFF) % charset.length]
    }.joinToString("")

    return nonce

}


@OptIn(ExperimentalForeignApi::class)
private fun iosSecureRandomBytes(length: Int): ByteArray {
    require(length > 0) { "Length must be greater than 0" }
    return memScoped {
        val randomBytes = allocArray<UByteVar>(length)
        val errorCode = SecRandomCopyBytes(kSecRandomDefault, length.convert(), randomBytes)
        if (errorCode != errSecSuccess) {
            throw RuntimeException("Unable to generate random bytes. SecRandomCopyBytes failed with OSStatus $errorCode")
        }
        randomBytes.readBytes(length)
    }
}

@OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
private fun sha256(input: String): String {
    val hashedData = UByteArray(CC_SHA256_DIGEST_LENGTH)
    val inputData = input.encodeToByteArray()
    inputData.usePinned {
        CC_SHA256(it.addressOf(0), inputData.size.convert(), hashedData.refTo(0))
    }
    return hashedData.toByteArray().toHexString(HexFormat.Default)
}

private class PresentationContextProvider :
    ASAuthorizationControllerPresentationContextProvidingProtocol, NSObject() {

    override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): ASPresentationAnchor {
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        return rootViewController?.view?.window
    }
}

private class ASAuthorizationControllerDelegate(
    private val linkAccount: Boolean,
    private val onResult: (Result<AppleSignInResult?>) -> Unit
) : ASAuthorizationControllerDelegateProtocol, NSObject() {

    @OptIn(BetaInteropApi::class)
    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization,
    ) {
        println("AppleSignIn: authorizationController success function is called")

        val appleIDCredential =
            didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
        if (currentNonce == null) {
            onResult(Result.failure(IllegalStateException("Invalid state: A login callback was received, but no login request was sent.")))
            return
        }
        val appleIdToken = appleIDCredential?.identityToken
        if (appleIdToken == null) {
            onResult(Result.failure(IllegalStateException("Unable to fetch identity token")))
            return
        }

        val idTokenString = NSString.create(appleIdToken, NSUTF8StringEncoding)?.toString()
        if (idTokenString == null) {
            onResult(Result.failure(IllegalStateException("Unable to serialize token string from data")))
            return
        }

        val authorizationCode: String? = appleIDCredential.authorizationCode?.let {
            NSString.create(it, encoding = NSUTF8StringEncoding)?.toString()
        }

        val appleSignInResult = AppleSignInResult(
            idToken = idTokenString,
            authCode = authorizationCode,
            userId = appleIDCredential.user,
            email = appleIDCredential.email, // provided only on first login, null otherwise
            fullName = FullName( // provided only on first login, null otherwise
                givenName = appleIDCredential.fullName?.givenName,
                familyName = appleIDCredential.fullName?.familyName
            ),
            platform = Platform.IOS
        )

        onResult(Result.success(appleSignInResult))
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError: NSError,
    ) {
        onResult(Result.failure(IllegalStateException(didCompleteWithError.localizedFailureReason)))
    }
}