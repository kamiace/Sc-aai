package sh.swurlz.core.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/** Stores provider secrets outside ordinary DataStore preferences using Android Keystore-backed encryption. */
object SecretStore {
    private const val FILE = "swurlz_secrets"
    private const val KEY_MENTOR_TOKEN = "mentor_api_token"

    private fun prefs(ctx: Context) = EncryptedSharedPreferences.create(
        FILE,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        ctx,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun mentorToken(ctx: Context): String = prefs(ctx).getString(KEY_MENTOR_TOKEN, "").orEmpty()

    fun setMentorToken(ctx: Context, token: String) {
        prefs(ctx).edit().putString(KEY_MENTOR_TOKEN, token.trim()).apply()
    }
}
