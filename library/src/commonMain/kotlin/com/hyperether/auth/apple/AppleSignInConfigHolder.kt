package com.hyperether.auth.apple

object AppleSignInConfigHolder {

    private var _config: AppleSignInConfig? = null

    /**
     * Configure the Apple Sign-In globally.
     * Safe to call multiple times — will only keep the first config.
     */
    fun configure(config: AppleSignInConfig) {
        if (_config != null) {
            println("AppleSignInConfigHolder already configured, ignoring reconfiguration.")
            return
        }
        _config = config
    }

    /**
     * Clears the stored config.
     */
    fun clear() {
        _config = null
    }

    /**
     * Returns true if the config has been set.
     */
    fun isConfigured(): Boolean = _config != null

    /**
     * Returns the config or throws an error if not configured.
     * Use this inside button containers.
     */
    fun getOrThrow(): AppleSignInConfig {
        return _config ?: error(
            "AppleSignInConfigHolder is not configured. " +
                    "Call AppleSignInConfigHolder.configure(...) at app startup."
        )
    }
}
