package com.example.data.security

import android.util.Base64
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Industrial-strength cryptographic utilities implementing:
 * - ECDH (Elliptic Curve Diffie-Hellman) over NIST P-256 (secp256r1) for key exchange
 * - SHA-256 HKDF-like key derivation for 256-bit symmetric key generation
 * - AES-256-GCM (Galois/Counter Mode) authenticated encryption with 12-byte IV and 128-bit authentication tag
 */
object CryptoUtils {

    private const val EC_CURVE = "secp256r1"
    private const val AES_GCM_CIPHER = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12

    /**
     * Generates a new 256-bit Elliptic Curve KeyPair (NIST P-256)
     */
    fun generateECKeyPair(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("EC")
        val ecSpec = ECGenParameterSpec(EC_CURVE)
        keyGen.initialize(ecSpec, SecureRandom())
        return keyGen.generateKeyPair()
    }

    /**
     * Serializes EC Public Key to Base64 (X.509 format)
     */
    fun encodePublicKey(publicKey: PublicKey): String {
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    /**
     * Deserializes Base64 (X.509 format) to EC Public Key
     */
    fun decodePublicKey(base64: String): PublicKey {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(bytes)
        val factory = KeyFactory.getInstance("EC")
        return factory.generatePublic(spec)
    }

    /**
     * Serializes EC Private Key to Base64 (PKCS#8 format) for secure local device storage
     */
    fun encodePrivateKey(privateKey: PrivateKey): String {
        return Base64.encodeToString(privateKey.encoded, Base64.NO_WRAP)
    }

    /**
     * Deserializes Base64 (PKCS#8 format) to EC Private Key from local device storage
     */
    fun decodePrivateKey(base64: String): PrivateKey {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        val spec = PKCS8EncodedKeySpec(bytes)
        val factory = KeyFactory.getInstance("EC")
        return factory.generatePrivate(spec)
    }

    /**
     * Performs Elliptic Curve Diffie-Hellman (ECDH) Key Agreement
     * between my private key and the peer's public key
     */
    fun computeSharedSecret(myPrivateKey: PrivateKey, peerPublicKey: PublicKey): ByteArray {
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(myPrivateKey)
        keyAgreement.doPhase(peerPublicKey, true)
        return keyAgreement.generateSecret()
    }

    /**
     * Derives a 256-bit AES symmetric key from the ECDH shared secret using SHA-256
     * with an application-specific domain separation salt.
     */
    fun deriveAesKey(sharedSecret: ByteArray, salt: String = "NILLBOOK_E2EE_PROTOCOL_V1"): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        val keyBytes = digest.digest(sharedSecret)
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts plaintext bytes using AES-256-GCM authenticated encryption.
     * Generates a cryptographically random 12-byte IV for every encryption.
     * @return Pair of (Base64 IV, Base64 Ciphertext with 128-bit auth tag)
     */
    fun encryptAesGcm(plaintext: ByteArray, key: SecretKeySpec): Pair<String, String> {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(AES_GCM_CIPHER)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)

        val ciphertext = cipher.doFinal(plaintext)
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

        return Pair(ivBase64, ciphertextBase64)
    }

    /**
     * Decrypts AES-256-GCM ciphertext bytes and validates the 128-bit authentication tag.
     * If the ciphertext or IV has been tampered with or modified in transit,
     * javax.crypto.AEADBadTagException is thrown, preventing forgery.
     */
    fun decryptAesGcm(ciphertextBase64: String, ivBase64: String, key: SecretKeySpec): ByteArray {
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val ciphertext = Base64.decode(ciphertextBase64, Base64.NO_WRAP)

        val cipher = Cipher.getInstance(AES_GCM_CIPHER)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)

        return cipher.doFinal(ciphertext)
    }

    /**
     * Convenience helper to encrypt UTF-8 text
     */
    fun encryptString(plaintext: String, key: SecretKeySpec): Pair<String, String> {
        return encryptAesGcm(plaintext.toByteArray(Charsets.UTF_8), key)
    }

    /**
     * Convenience helper to decrypt UTF-8 text
     */
    fun decryptString(ciphertextBase64: String, ivBase64: String, key: SecretKeySpec): String {
        val decryptedBytes = decryptAesGcm(ciphertextBase64, ivBase64, key)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
