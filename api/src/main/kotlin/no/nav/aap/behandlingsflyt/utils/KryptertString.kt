package no.nav.aap.behandlingsflyt.utils

import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import java.util.Base64

class KryptertString(secretKey: ByteArray) {
    init {
        require(secretKey.size == 32) { "AES-256 requires a 32-byte key" }
    }

    private val key = SecretKeySpec(secretKey, "AES")

    private companion object {
        const val NONCE_SIZE = 12   // 96-bit nonce, standard for GCM
        const val TAG_SIZE = 128    // 128-bit auth tag
    }

    /** Returns a new, uncorrelatable encrypted value every time it is called */
    fun encode(value: String): String {
        val nonce = ByteArray(NONCE_SIZE).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_SIZE, nonce))
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))

        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(nonce + encrypted)
    }

    /** Decodes an encrypted value back to the original string. Throws if tampered. */
    fun decode(encoded: String): String {
        val bytes = runCatching {
            Base64.getUrlDecoder().decode(encoded)
        }.getOrElse { throw IllegalArgumentException("Invalid kelvinId format") }

        require(bytes.size > NONCE_SIZE) { "Invalid kelvinId length" }

        val nonce = bytes.sliceArray(0 until NONCE_SIZE)
        val encrypted = bytes.sliceArray(NONCE_SIZE until bytes.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE, nonce))

        // doFinal throws AEADBadTagException if the encoded value has been tampered with
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }
}
