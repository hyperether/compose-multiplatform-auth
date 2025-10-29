# Compose Multiplatform Auth Library

**A plug-and-play Compose Multiplatform library for Google, Apple, and Microsoft Sign-In across Android, iOS, and Web(JS/WasmJs).**

## Features

- Compose UI buttons for Google, Apple, and Microsoft
- Unified sign-in result handling
- Works across Android, iOS, and Web
- Simple configuration with flexible setup

---

## Installation

Add the dependency to your `common` module's `build.gradle.kts`:

```kotlin
implementation("com.hyperether.auth:library:<version>")
```

---

## Initialization

Call the config setup once in your `App.kt` or app entry point:

```kotlin
GoogleSignInConfigHolder.configure(
    config = GoogleSignInConfig(
        webClientId = "your_web_client_id.googleusercontent.com",
        iosClientId = "your_ios_client_id.apps.googleusercontent.com",
        redirectUri = "https://your.domain.com/google-callback",
    )
)
```
- `redirectUri` make sure the domain and redirect URI are registered
in [Google Cloud Console](https://console.cloud.google.com/apis/credentials)

```kotlin
AppleSignInConfigHolder.configure(
    config = AppleSignInConfig(
        iosServiceId = "your.domain.com",
        redirectUri = "https://your.domain.com/apple-callback"
    )
)
```
- `redirectUri` make sure the domain and redirect URI are registered 
in [Apple Developer Console > Identifiers > Services](https://developer.apple.com/account/resources/identifiers/)

```kotlin
MicrosoftSignInConfigHolder.configure(
    config = MicrosoftSignInConfig(
        clientId = "your_microsoft_client_id",
        redirectUri = "https://your.domain.com/"
    )
)
```
- `redirectUri` make sure the domain and redirect URI are registered
in [Microsoft Azure](https://portal.azure.com)

---

## Usage

Inside your Compose UI, simply add the buttons:

```kotlin
GoogleButtonContainer(
    modifier = Modifier.height(44.dp),
    onResult = { result ->
        result.fold(
            onSuccess = { user -> println("Google Signed in: $user") },
            onFailure = { error -> println("Google Sign-In failed: ${error.message}") }
        )
    }
)

AppleButtonContainer(
    modifier = Modifier.height(44.dp),
    onResult = { result ->
        result.fold(
            onSuccess = { user -> println("Apple Signed in: $user") },
            onFailure = { error -> println("Apple Sign-In failed: ${error.message}") }
        )
    }
)

MicrosoftButtonContainer(
    modifier = Modifier.height(44.dp),
    onResult = { result ->
        result.fold(
            onSuccess = { user -> println("Microsoft Signed in: $user") },
            onFailure = { error -> println("Microsoft Sign-In failed: ${error.message}") }
        )
    }
)
```

---

## Platform-Specific Configuration


### Apple Sign-In

#### Android
In your `MainActivity.kt`, forward the redirect:

```kotlin
override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    AppleSignInResultHandler.handleRedirect(intent)
}
```

#### iOS
In your `iosApp.entitlements`:

```xml
<key>com.apple.developer.applesignin</key>
<array>
    <string>Default</string>
</array>
```

#### Web
In `main.kt`, call:
```kotlin
AppleSignInResultHandler.handleRedirect()
```

---

### Google Sign-In

#### iOS

**`Info.plist`**
```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>com.googleusercontent.apps.YOUR_IOS_CLIENT_ID</string>
        </array>
    </dict>
</array>
```

**`AppDelegate.swift`**
```swift
import GoogleSignIn

func application(_ app: UIApplication,
                 open url: URL,
                 options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
    return GIDSignIn.sharedInstance.handle(url)
}
```

**`Podfile`**
```ruby
pod 'GoogleSignIn'
```

#### Web

In `main.kt`, call:
```kotlin
GoogleSignInResultHandler.handleRedirect()
```

---

### Microsoft Sign-In

#### Azure portal setup
After registering your app, go to Manage > Authentication.
On select "Add platform" and create:

1. Singe-page application

    -Register your web Redirect URI

2. Mobile and desktop applications

    -Select MSAL Redirect URI


#### Android
Update your `AndroidManifest.xml`:
```xml
<activity android:name="com.microsoft.identity.client.BrowserTabActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="msalYOUR_CLIENT_ID" android:host="auth" />
    </intent-filter>
</activity>
```

#### Web
Add this to `index.html`:
```html
<script src="https://alcdn.msauth.net/browser/2.19.0/js/msal-browser.min.js"></script>
```

#### iOS
_Not implemented_

---

## Output

Each sign-in button returns a `Result<T>` specific to the provider:

```kotlin
Result<AppleSignInResult>
Result<GoogleSignInResult>
Result<MicrosoftSignInResult>
```
### AppleSignInResult
```kotlin
data class AppleSignInResult(
    val idToken: String,
    val authCode: String? = null,
    val userId: String? = null,
    val email: String? = null,
    val fullName: FullName? = null,
    val platform: Platform
)
```
### GoogleSignInResult
```kotlin
data class GoogleSignInResult(
    val idToken: String,
    val userId: String? = null,
    val email: String? = null,
    val fullName: String? = null,
    val platform: Platform
)
```

### MicrosoftSignInResult
```kotlin
data class MicrosoftSignInResult(
   val token: String? = null,
   val accessToken: String? = null,
   val email: String? = null,
   val tenantId: String? = null,
   val platform: Platform
)
```

- Each Result<T> provides either a successful sign-in result or an exception via:
```kotlin
result.fold(
    onSuccess = { user -> /* use the sign-in result */ },
    onFailure = { error -> /* handle the error */ }
)
```

---

## License

MIT – free for personal and commercial use.

---

## Support

Need help integrating or configuring? Feel free to open an issue or contact the maintainers.