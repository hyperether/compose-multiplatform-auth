import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.kotlinNativeCocoaPods)
    kotlin("plugin.serialization") version "2.1.21"
    id("maven-publish") // TODO for local publishing
}

group = "com.hyperether.auth"
version = "1.0.1"

kotlin {
    jvm()
    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    js(IR) {
        browser ()
        binaries.executable()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        ios.deploymentTarget = "14.0"
        framework {
            baseName = "HyperAuthKMP"
            isStatic = true
        }
        pod("GoogleSignIn")
    }

    sourceSets {

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.browser)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.playServicesAuth)
            implementation(libs.android.legacy.playServicesAuth)
            implementation(libs.googleIdIdentity)
            implementation(libs.androidx.startup.runtime)
            implementation("com.microsoft.identity.client:msal:5.2.0") {
                exclude(group = "com.microsoft.device.display", module = "display-mask")
            }
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlinSerialization)
        }

        wasmJsMain.dependencies {

        }
    }
}

android {
    namespace = "com.hyperether.auth"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "library", version.toString())

    pom {
        name = "HyperEther Auth Lib CMP"
        description = "A Kotlin Multiplatform authentication library."
        inceptionYear = "2025"
        url = "https://github.com/hyperether/compose-multiplatform-auth/"
        licenses {
            license {
                name = "GPL-3.0 license"
                url = "https://github.com/hyperether/compose-multiplatform-auth/blob/master/LICENSE"
            }
        }
        developers {
            developer {
                id = "hyperether"
                name = "HyperEther"
                email = "info@hyperether.com"
            }
        }
        scm {
            url = "https://github.com/hyperether/compose-multiplatform-auth"
            connection = "scm:git:git://github.com/hyperether/compose-multiplatform-auth.git"
            developerConnection = "scm:git:ssh://git@github.com/hyperether/compose-multiplatform-auth.git"
        }
    }
}

// GitHub Packages Publishing
val githubProperties = Properties()
val githubPropsFile = rootProject.file("github.properties")
if (githubPropsFile.exists()) {
    FileInputStream(githubPropsFile).use { githubProperties.load(it) }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/hyperether/compose-multiplatform-auth")
            credentials {
                username = githubProperties.getProperty("gpr.usr") ?: System.getenv("GPR_USER")
                password = githubProperties.getProperty("gpr.key") ?: System.getenv("GPR_API_KEY")
            }
        }
    }
}
