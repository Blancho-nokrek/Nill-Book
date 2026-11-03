package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.*
import org.json.JSONObject
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.util.concurrent.ConcurrentHashMap

/**
 * Result of attempting to send an encrypted message
 */
sealed class E2EESendResult {
    data class DeliveredImmediately(
        val messageId: String,
        val localChatMessage: ChatMessage,
        val envelope: EncryptedMessageEnvelope
    ) : E2EESendResult()

    data class QueuedOnServerForOfflineRecipient(
        val messageId: String,
        val localChatMessage: ChatMessage,
        val envelope: EncryptedMessageEnvelope,
        val expiresAt: Long
    ) : E2EESendResult()

    data class Error(val reason: String) : E2EESendResult()
}

/**
 * Core End-to-End Encryption (E2EE) Messaging Architecture Manager
 * - Manages Device-Local ECDH KeyPairs (NIST P-256)
 * - Performs sender-side AES-256-GCM encryption with recipient's public key
 * - Performs recipient-side decryption with recipient's private key
 * - Handles offline recipient server queueing with strict 7-day TTL and zero-knowledge purging
 * - Stores delivered messages locally on the device
 */
object E2EEMessagingManager {

    private const val TAG = "E2EEMessaging"
    private const val PREFS_NAME = "nillbook_e2ee_keystore"
    private const val KEY_PREFIX_PRIV = "priv_key_"
    private const val KEY_PREFIX_PUB = "pub_key_"

    private var sharedPreferences: SharedPreferences? = null

    // In-memory cache of public keys published by users to the directory
    private val directoryPublicKeys = ConcurrentHashMap<String, String>()

    // In-memory cache of locally unlocked private keys for active users
    private val localPrivateKeys = ConcurrentHashMap<String, PrivateKey>()

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Retrieves or generates a secure EC KeyPair for a user on this device.
     * The private key is saved in local device SharedPreferences and NEVER shared with the server.
     * The public key is published to the directory.
     */
    fun getOrCreateKeyPairForUser(userPhone: String): KeyPair {
        val prefs = sharedPreferences
        val storedPriv = prefs?.getString(KEY_PREFIX_PRIV + userPhone, null)
        val storedPub = prefs?.getString(KEY_PREFIX_PUB + userPhone, null)

        if (storedPriv != null && storedPub != null) {
            try {
                val priv = CryptoUtils.decodePrivateKey(storedPriv)
                val pub = CryptoUtils.decodePublicKey(storedPub)
                directoryPublicKeys[userPhone] = storedPub
                localPrivateKeys[userPhone] = priv
                return KeyPair(pub, priv)
            } catch (e: Exception) {
                Log.w(TAG, "Regenerating corrupted keypair for $userPhone: ${e.message}")
            }
        }

        // Generate pristine EC P-256 KeyPair
        val keyPair = CryptoUtils.generateECKeyPair()
        val pubBase64 = CryptoUtils.encodePublicKey(keyPair.public)
        val privBase64 = CryptoUtils.encodePrivateKey(keyPair.private)

        prefs?.edit()
            ?.putString(KEY_PREFIX_PRIV + userPhone, privBase64)
            ?.putString(KEY_PREFIX_PUB + userPhone, pubBase64)
            ?.apply()

        directoryPublicKeys[userPhone] = pubBase64
        localPrivateKeys[userPhone] = keyPair.private
        return keyPair
    }

    /**
     * Look up the recipient's public key from the centralized directory.
     */
    fun getRecipientPublicKey(recipientPhone: String): PublicKey? {
        val pubBase64 = directoryPublicKeys[recipientPhone] ?: run {
            // If user isn't in cache, generate/retrieve their directory key
            getOrCreateKeyPairForUser(recipientPhone).public.let { CryptoUtils.encodePublicKey(it) }
        }
        return try {
            CryptoUtils.decodePublicKey(pubBase64)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode recipient public key: ${e.message}")
            null
        }
    }

    /**
     * Encrypts and transmits a private message from sender to recipient.
     *
     * Privacy Guarantees:
     * - End-to-End Encrypted via ECDH (P-256) + AES-256-GCM.
     * - Server NEVER possesses the plaintext or private keys.
     * - If recipient is online, delivered immediately and removed from server relay.
     * - If recipient is offline, envelope is held in server queue for max 7 days.
     * - Stored permanently ONLY in the user's local device storage.
     */
    fun sendE2EEMessage(
        sender: User,
        recipientPhone: String,
        recipientName: String,
        plaintext: String,
        attachmentUrl: String? = null,
        attachmentType: String? = null,
        audioDuration: Int? = null,
        replyToText: String? = null,
        isRecipientOnline: Boolean = true
    ): E2EESendResult {
        try {
            // 1. Get sender's private key (device-local)
            val senderKeyPair = getOrCreateKeyPairForUser(sender.phone)
            val senderPriv = senderKeyPair.private

            // 2. Fetch recipient's public key from directory
            val recipientPub = getRecipientPublicKey(recipientPhone)
                ?: return E2EESendResult.Error("Recipient public key not found in directory")

            // 3. Compute ECDH shared secret
            val sharedSecret = CryptoUtils.computeSharedSecret(senderPriv, recipientPub)

            // 4. Derive 256-bit AES key using SHA-256
            val aesKey = CryptoUtils.deriveAesKey(sharedSecret)

            // 5. Serialize message payload into JSON
            val payloadObj = JSONObject().apply {
                put("text", plaintext)
                put("timestamp", System.currentTimeMillis())
                put("senderName", sender.name)
                put("attachmentUrl", attachmentUrl ?: "")
                put("attachmentType", attachmentType ?: "")
                put("audioDuration", audioDuration ?: 0)
                put("replyToText", replyToText ?: "")
            }

            // 6. Encrypt with AES-256-GCM (random 12-byte IV + 128-bit auth tag)
            val (ivBase64, ciphertextBase64) = CryptoUtils.encryptString(payloadObj.toString(), aesKey)
            val senderPubBase64 = CryptoUtils.encodePublicKey(senderKeyPair.public)

            // 7. Assemble EncryptedMessageEnvelope (Zero-Knowledge for server)
            val envelope = EncryptedMessageEnvelope(
                senderId = sender.id,
                senderPhone = sender.phone,
                recipientId = recipientPhone,
                recipientPhone = recipientPhone,
                senderPublicKeyBase64 = senderPubBase64,
                ivBase64 = ivBase64,
                ciphertextBase64 = ciphertextBase64,
                timestamp = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + ServerMessageRelayQueue.MAX_RETENTION_TTL_MS
            )

            // 8. Create local ChatMessage for sender's device storage
            val localMessage = ChatMessage(
                id = envelope.messageId,
                text = plaintext,
                timestamp = "Just now",
                isFromMe = true,
                channel = MessageChannel.INTERNET,
                status = if (isRecipientOnline) MessageDeliveryStatus.DELIVERED else MessageDeliveryStatus.SENT,
                attachmentUrl = attachmentUrl,
                attachmentType = attachmentType,
                audioDurationSeconds = audioDuration,
                replyToText = replyToText,
                isEncrypted = true,
                encryptionAlgorithm = "AES-256-GCM / ECDH-P256"
            )

            // 9. Delivery Routing
            return if (isRecipientOnline) {
                // Online delivery: relay to recipient device
                val delivered = deliverEnvelopeToRecipientDevice(envelope)
                if (delivered) {
                    // Immediate purge from server relay queue
                    ServerMessageRelayQueue.acknowledgeDelivery(envelope.messageId)
                    E2EESendResult.DeliveredImmediately(envelope.messageId, localMessage, envelope)
                } else {
                    // Fallback to offline queue
                    ServerMessageRelayQueue.enqueueEnvelope(envelope)
                    E2EESendResult.QueuedOnServerForOfflineRecipient(
                        envelope.messageId,
                        localMessage.copy(status = MessageDeliveryStatus.SENT),
                        envelope,
                        envelope.expiresAt
                    )
                }
            } else {
                // Offline recipient: store temporarily on server queue (max 7 days TTL)
                ServerMessageRelayQueue.enqueueEnvelope(envelope)
                E2EESendResult.QueuedOnServerForOfflineRecipient(
                    envelope.messageId,
                    localMessage.copy(status = MessageDeliveryStatus.SENT),
                    envelope,
                    envelope.expiresAt
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "E2EE Encryption error: ${e.message}", e)
            return E2EESendResult.Error("Encryption failed: ${e.localizedMessage}")
        }
    }

    /**
     * Decrypts an incoming encrypted message envelope on the recipient's device.
     */
    fun decryptEnvelopeOnDevice(
        envelope: EncryptedMessageEnvelope,
        recipientPhone: String
    ): ChatMessage? {
        try {
            // 1. Get recipient's local private key
            val recipientKeyPair = getOrCreateKeyPairForUser(recipientPhone)
            val recipientPriv = recipientKeyPair.private

            // 2. Decode sender's public key from envelope
            val senderPub = CryptoUtils.decodePublicKey(envelope.senderPublicKeyBase64)

            // 3. Compute ECDH shared secret
            val sharedSecret = CryptoUtils.computeSharedSecret(recipientPriv, senderPub)

            // 4. Derive AES-256 key
            val aesKey = CryptoUtils.deriveAesKey(sharedSecret)

            // 5. Decrypt ciphertext & verify 128-bit GCM authentication tag
            val decryptedJson = CryptoUtils.decryptString(envelope.ciphertextBase64, envelope.ivBase64, aesKey)
            val payload = JSONObject(decryptedJson)

            val text = payload.optString("text", "")
            val attachmentUrl = payload.optString("attachmentUrl").takeIf { it.isNotEmpty() }
            val attachmentType = payload.optString("attachmentType").takeIf { it.isNotEmpty() }
            val audioDuration = payload.optInt("audioDuration", 0).takeIf { it > 0 }
            val replyToText = payload.optString("replyToText").takeIf { it.isNotEmpty() }

            return ChatMessage(
                id = envelope.messageId,
                text = text,
                timestamp = "Just now",
                isFromMe = false,
                channel = MessageChannel.INTERNET,
                status = MessageDeliveryStatus.READ,
                attachmentUrl = attachmentUrl,
                attachmentType = attachmentType,
                audioDurationSeconds = audioDuration,
                replyToText = replyToText,
                isEncrypted = true,
                encryptionAlgorithm = "AES-256-GCM / ECDH-P256"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failure (invalid tag or tampering): ${e.message}", e)
            return null
        }
    }

    /**
     * Simulated network delivery to recipient's active device session.
     */
    private fun deliverEnvelopeToRecipientDevice(envelope: EncryptedMessageEnvelope): Boolean {
        // Recipient decrypts and writes to their local message store
        val decrypted = decryptEnvelopeOnDevice(envelope, envelope.recipientPhone)
        return decrypted != null
    }

    /**
     * When an offline recipient reconnects, poll their pending envelopes from server,
     * decrypt them on the device, store them locally, and immediately delete them from server.
     */
    fun syncPendingOfflineMessages(recipientPhone: String): List<ChatMessage> {
        val pending = ServerMessageRelayQueue.getPendingEnvelopesForRecipient(recipientPhone)
        val decryptedList = mutableListOf<ChatMessage>()

        for (envelope in pending) {
            val decrypted = decryptEnvelopeOnDevice(envelope, recipientPhone)
            if (decrypted != null) {
                decryptedList.add(decrypted)
                // RULE: Immediately delete from server queue upon receipt
                ServerMessageRelayQueue.acknowledgeDelivery(envelope.messageId)
            }
        }
        return decryptedList
    }
}
