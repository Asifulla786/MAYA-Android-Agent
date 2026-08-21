package com.asifulla.maya.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureConfig(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context, "maya_secrets", MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    fun putProviderKey(provider: String, value: String) = prefs.edit().putString("key_$provider", value).apply()
    fun getProviderKey(provider: String): String? = prefs.getString("key_$provider", null)
    fun removeProviderKey(provider: String) = prefs.edit().remove("key_$provider").apply()
}
