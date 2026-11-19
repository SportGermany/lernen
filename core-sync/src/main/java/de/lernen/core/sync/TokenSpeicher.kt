package de.lernen.core.sync

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Speichert den GitHub-Token verschlüsselt (AES-GCM, Schlüssel im Android Keystore, nicht exportierbar).
 * Direkt über den Keystore statt Tink: spart eine Bibliothek und reicht für genau einen Wert.
 * Der Token landet nie in DataStore, Room oder der Datensicherung.
 */
@Singleton
class TokenSpeicher @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("geheim", Context.MODE_PRIVATE)
    private val _vorhanden = MutableStateFlow(prefs.contains(SCHLUESSEL))
    val vorhanden: StateFlow<Boolean> = _vorhanden.asStateFlow()

    private fun schluessel(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    fun speichere(token: String) {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, schluessel()) }
        val daten = c.iv + c.doFinal(token.trim().toByteArray())
        prefs.edit().putString(SCHLUESSEL, Base64.encodeToString(daten, Base64.NO_WRAP)).apply()
        _vorhanden.value = true
    }

    fun lies(): String? = runCatching {
        val daten = Base64.decode(prefs.getString(SCHLUESSEL, null) ?: return null, Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, schluessel(), GCMParameterSpec(128, daten, 0, 12))
        String(c.doFinal(daten, 12, daten.size - 12))
    }.getOrNull()

    fun loesche() {
        prefs.edit().remove(SCHLUESSEL).apply()
        _vorhanden.value = false
    }

    private companion object {
        const val ALIAS = "lernen-github-token"
        const val SCHLUESSEL = "token"
    }
}
