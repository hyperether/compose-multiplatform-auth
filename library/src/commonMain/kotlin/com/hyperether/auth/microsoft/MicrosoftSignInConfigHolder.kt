package com.hyperether.auth.microsoft

object MicrosoftSignInConfigHolder {

    private var _config: MicrosoftSignInConfig? = null

    /**
     * Configure the Microsoft Sign-In globally.
     * Safe to call multiple times — will only keep the first config.
     */
    fun configure(config: MicrosoftSignInConfig) {
        if (_config != null) {
            println("MicrosoftSignInConfigHolder already configured, ignoring reconfiguration.")
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
    fun getOrThrow(): MicrosoftSignInConfig {
        return _config ?: error(
            "MicrosoftSignInConfigHolder is not configured. " +
                    "Call MicrosoftSignInConfigHolder.configure(...) at app startup."
        )
    }
}