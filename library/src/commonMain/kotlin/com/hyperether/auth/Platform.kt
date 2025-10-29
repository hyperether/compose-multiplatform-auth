package com.hyperether.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Platform {
    @SerialName("ios")
    IOS,

    @SerialName("android")
    ANDROID,

    @SerialName("web")
    WEB,

    @SerialName("desktop")
    DESKTOP
}