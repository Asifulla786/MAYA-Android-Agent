package com.asifulla.maya.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureConfig(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "maya_secrets",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun putProviderKey(provider: String, value: String) {
        prefs.edit().putString(keyName(provider), value.trim()).apply()
    }

    fun getProviderKey(provider: String): String? =
        prefs.getString(keyName(provider), null)?.takeIf { it.isNotBlank() }

    fun hasProviderKey(provider: String): Boolean = !getProviderKey(provider).isNullOrBlank()

    fun removeProviderKey(provider: String) {
        prefs.edit().remove(keyName(provider)).apply()
    }

    private fun keyName(provider: String) = "key_${provider.lowercase()}"
}
