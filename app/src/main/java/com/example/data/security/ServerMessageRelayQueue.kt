package com.example.data.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Envelope stored temporarily on the server while waiting for offline recipient to connect.
 * Note:
 * - Server NEVER sees or stores plaintext.
 * - Server has NO access to recipient private keys.
 * - Contains only ciphertext, IV, sender public key, sender/recipient routing identifiers, and TTL.
 */
data class EncryptedMessageEnvelope(
    val messageId: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderPhone: String,
    val recipientId: String,
    val recipientPhone: String,
    val senderPublicKeyBase64: String,
    val ivBase64: String,
    val ciphertextBase64: String,
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000L), // Maximum 7 days TTL
    val status: EnvelopeStatus = EnvelopeStatus.QUEUED_ON_SERVER
)

enum class EnvelopeStatus {
    QUEUED_ON_SERVER,
    DELIVERED_AND_PURGED,
    EXPIRED_AND_PURGED
}

data class ServerQueueStats(
    val currentlyQueuedCount: Int = 0,
    val totalDeliveredAndPurgedCount: Int = 0,
    val totalExpiredAndPurgedCount: Int = 0,
    val lastPurgeTimestamp: Long = System.currentTimeMillis()
)

/**
 * Server Message Relay Queue for Offline Recipients
 * Manages zero-knowledge temporary queueing of encrypted envelopes.
 * Implements strict 7-day TTL auto-purge and immediate post-delivery deletion.
 */
object ServerMessageRelayQueue {

    // 7 days in milliseconds
    const val MAX_RETENTION_TTL_MS = 7L * 24 * 60 * 60 * 1000L

    // In-memory server relay store for pending messages
    private val queueLock = Any()
    private val pendingEnvelopes = mutableListOf<EncryptedMessageEnvelope>()

    private val _stats = MutableStateFlow(ServerQueueStats())
    val stats: StateFlow<ServerQueueStats> = _stats.asStateFlow()

    /**
     * Enqueues an encrypted envelope on the server for an offline recipient.
     * Enforces the 7-day maximum TTL rule.
     */
    fun enqueueEnvelope(envelope: EncryptedMessageEnvelope): Boolean {
        synchronized(queueLock) {
            // First run auto-purge of any expired messages
            purgeExpiredMessagesInternal()

            // Ensure expiration does not exceed 7 days
            val safeExpiry = minOf(envelope.expiresAt, System.currentTimeMillis() + MAX_RETENTION_TTL_MS)
            val validatedEnvelope = envelope.copy(
                expiresAt = safeExpiry,
                status = EnvelopeStatus.QUEUED_ON_SERVER
            )
            pendingEnvelopes.add(validatedEnvelope)
            updateStats()
            return true
        }
    }

    /**
     * Polls pending encrypted envelopes addressed to a specific recipient.
     */
    fun getPendingEnvelopesForRecipient(recipientPhone: String): List<EncryptedMessageEnvelope> {
        synchronized(queueLock) {
            purgeExpiredMessagesInternal()
            return pendingEnvelopes.filter { it.recipientPhone == recipientPhone }
        }
    }

    /**
     * Called when the recipient device acknowledges successful delivery.
     * CRITICAL RULE: Immediately and permanently deletes the message from the server queue.
     * No ciphertext, attachments, or metadata are retained.
     */
    fun acknowledgeDelivery(messageId: String): Boolean {
        synchronized(queueLock) {
            val index = pendingEnvelopes.indexOfFirst { it.messageId == messageId }
            if (index != -1) {
                pendingEnvelopes.removeAt(index)
                val current = _stats.value
                _stats.value = current.copy(
                    currentlyQueuedCount = pendingEnvelopes.size,
                    totalDeliveredAndPurgedCount = current.totalDeliveredAndPurgedCount + 1
                )
                return true
            }
            return false
        }
    }

    /**
     * Automatically purges messages that remain undelivered after 7 days.
     * Guarantees zero retention past the 7-day limit.
     */
    fun purgeExpiredMessages(): Int {
        synchronized(queueLock) {
            return purgeExpiredMessagesInternal()
        }
    }

    private fun purgeExpiredMessagesInternal(): Int {
        val now = System.currentTimeMillis()
        val expired = pendingEnvelopes.filter { it.expiresAt <= now }
        if (expired.isNotEmpty()) {
            pendingEnvelopes.removeAll(expired)
            val current = _stats.value
            _stats.value = current.copy(
                currentlyQueuedCount = pendingEnvelopes.size,
                totalExpiredAndPurgedCount = current.totalExpiredAndPurgedCount + expired.size,
                lastPurgeTimestamp = now
            )
        }
        return expired.size
    }

    private fun updateStats() {
        val current = _stats.value
        _stats.value = current.copy(
            currentlyQueuedCount = pendingEnvelopes.size
        )
    }

    /**
     * For administrative security audit: check queue size
     */
    fun getQueueSize(): Int {
        synchronized(queueLock) {
            return pendingEnvelopes.size
        }
    }

    /**
     * Clear all queued messages (for test or secure wipe)
     */
    fun clearQueue() {
        synchronized(queueLock) {
            pendingEnvelopes.clear()
            updateStats()
        }
    }
}
