package com.example.lifelogger.data.auth

import android.content.Context
import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

class SessionManager(context: Context) {

    companion object {
        private const val TAG = "SessionManager"
        private const val PREFS_NAME = "lifelogger_session"
        private const val KEY_ACTIVE_USER_ID = "active_user_id"
        private const val KEY_ACTIVE_USERNAME = "active_username"
        private const val KEY_CREDENTIAL_PREFIX = "offline_credential_"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setActiveUser(userId: String, username: String) {
        Log.d(TAG, "[setActiveUser] Setting userId=${userId.take(8)}..., username=$username")
        prefs.edit()
            .putString(KEY_ACTIVE_USER_ID, userId)
            .putString(KEY_ACTIVE_USERNAME, normalize(username))
            .apply()
    }

    fun getActiveUserId(): String {
        val userId = prefs.getString(KEY_ACTIVE_USER_ID, "").orEmpty()
        Log.d(TAG, "[getActiveUserId] Retrieved userId=${if (userId.isEmpty()) "EMPTY" else userId.take(8) + "..."}")
        return userId
    }

    fun getActiveUsername(): String {
        val username = prefs.getString(KEY_ACTIVE_USERNAME, "").orEmpty()
        Log.d(TAG, "[getActiveUsername] Retrieved username=$username")
        return username
    }

    fun clearActiveUser() {
        Log.d(TAG, "[clearActiveUser] Clearing active user")
        prefs.edit()
            .remove(KEY_ACTIVE_USER_ID)
            .remove(KEY_ACTIVE_USERNAME)
            .apply()
    }

    fun saveOfflineLogin(username: String, password: String, userId: String) {
        Log.d(TAG, "[saveOfflineLogin] Saving offline credential for $username (userId=${userId.take(8)}...)")
        val normalized = normalize(username)
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashCredential(normalized, password, salt)
        val payload = listOf(
            base64Encode(salt),
            base64Encode(hash),
            userId
        ).joinToString(":")
        prefs.edit().putString(credentialKey(normalized), payload).apply()
        Log.d(TAG, "[saveOfflineLogin] Offline credential saved")
    }

    fun verifyOfflineLogin(username: String, password: String): String? {
        Log.d(TAG, "[verifyOfflineLogin] Verifying offline credential for $username")
        val normalized = normalize(username)
        val payload = prefs.getString(credentialKey(normalized), null)
        if (payload == null) {
            Log.w(TAG, "[verifyOfflineLogin] No credential found for $username")
            return null
        }
        val parts = payload.split(":", limit = 3)
        if (parts.size != 3) {
            Log.w(TAG, "[verifyOfflineLogin] Credential payload malformed for $username")
            return null
        }

        val salt = base64Decode(parts[0])
        if (salt == null) {
            Log.w(TAG, "[verifyOfflineLogin] Failed to decode salt for $username")
            return null
        }
        val expectedHash = base64Decode(parts[1])
        if (expectedHash == null) {
            Log.w(TAG, "[verifyOfflineLogin] Failed to decode hash for $username")
            return null
        }
        val userId = parts[2]

        val actualHash = hashCredential(normalized, password, salt)
        val matches = MessageDigest.isEqual(expectedHash, actualHash)
        if (matches) {
            Log.d(TAG, "[verifyOfflineLogin] Offline credential verified for $username, userId=${userId.take(8)}...")
            return userId
        } else {
            Log.w(TAG, "[verifyOfflineLogin] Offline credential FAILED for $username (wrong password?)")
            return null
        }
    }

    private fun normalize(username: String): String = username.trim().lowercase(Locale.US)

    private fun credentialKey(username: String): String = "$KEY_CREDENTIAL_PREFIX$username"

    private fun hashCredential(username: String, password: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(username.toByteArray(Charsets.UTF_8))
        digest.update(":".toByteArray(Charsets.UTF_8))
        digest.update(password.toByteArray(Charsets.UTF_8))
        return digest.digest()
    }

    private fun base64Encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun base64Decode(value: String): ByteArray? = runCatching {
        Base64.decode(value, Base64.NO_WRAP)
    }.getOrNull()
}

