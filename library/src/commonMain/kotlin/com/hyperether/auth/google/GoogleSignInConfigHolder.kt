package com.hyperether.auth.google

object GoogleSignInConfigHolder {

    private var _config: GoogleSignInConfig? = null

    /**
     * Configure the Google Sign-In globally.
     * Safe to call multiple times — will only keep the first config.
     */
    fun configure(config: GoogleSignInConfig) {
        if (_config != null) {
            println("GoogleSignInConfigHolder already configured, ignoring reconfiguration.")
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
     */
    fun getOrThrow(): GoogleSignInConfig {
        return _config ?: error(
            "GoogleSignInConfigHolder is not configured. " +
                    "Call GoogleSignInConfigHolder.configure(...) at app startup."
        )
    }
}
