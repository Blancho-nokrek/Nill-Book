package com.example.data

enum class UserPresence {
    ONLINE,
    OFFLINE,
    AWAY
}

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val username: String = "caller_${id.takeLast(4)}",
    val gender: String = "Male",
    val birthday: String = "1995-01-15",
    val bio: String = "Connecting on NILLBOOK caller directory.",
    val about: String = "Verified Nill Book member. Using dual online & offline smart communication.",
    val location: String = "Dhaka, Bangladesh",
    val photo: String? = null,
    val coverPhoto: String? = null,
    val showNumberPublic: Boolean = false,
    val presence: UserPresence = UserPresence.ONLINE,
    val lastSeen: String = "Just now",
    val twoFactorEnabled: Boolean = true,
    val createdAt: String = "2024-01-01"
)

enum class ContactGroup(val label: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    FAMILY("Family"),
    WORK("Work"),
    FRIENDS("Friends"),
    VIP("VIP")
}

enum class SyncStatus {
    SYNCED,
    PENDING_SYNC,
    FAILED
}

data class Contact(
    val id: String,
    val name: String,
    val phone: String,
    val secondaryPhone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val birthday: String? = null,
    val photo: String? = null,
    val isFavorite: Boolean = false,
    val group: ContactGroup = ContactGroup.ALL,
    val isRegisteredOnNillBook: Boolean = false,
    val isOnline: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

data class SearchResult(
    val number: String,
    val name: String,
    val count: Int,
    val registered: Boolean,
    val userId: String? = null,
    val friendStatus: String = "none", // "none", "pending_out", "pending_in", "friends", "self"
    val requestId: String? = null,
    val numberVisible: Boolean = false,
    val photo: String? = null
)

data class FriendItem(
    val userId: String,
    val name: String,
    val phone: String? = null,
    val photo: String? = null,
    val requestId: String? = null,
    val status: String = "friends", // "friends", "pending_in", "pending_out"
    val isOnline: Boolean = true,
    val lastSeen: String = "Just now"
)

data class FriendSuggestion(
    val userId: String,
    val name: String,
    val subtitle: String,
    val mutualFriendsCount: Int = 1,
    val phone: String,
    val isOnline: Boolean = true
)

data class Country(
    val code: String,
    val label: String,
    val dialCode: String
)

enum class MessageChannel(val displayName: String) {
    INTERNET("Internet Message"),
    SMS("Carrier SMS"),
    MMS("Carrier MMS")
}

enum class MessageDeliveryStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

data class MessageReaction(
    val emoji: String,
    val count: Int = 1,
    val userIds: List<String> = emptyList()
)

data class ChatMessage(
    val id: String,
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean,
    val channel: MessageChannel = MessageChannel.INTERNET,
    val status: MessageDeliveryStatus = MessageDeliveryStatus.READ,
    val attachmentUrl: String? = null,
    val attachmentType: String? = null, // "image", "audio", "document"
    val audioDurationSeconds: Int? = null,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val reactions: List<MessageReaction> = emptyList(),
    val isEdited: Boolean = false
)

data class ChatConversation(
    val id: String,
    val contactName: String,
    val phone: String,
    val lastMessage: String,
    val lastTimestamp: String,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false,
    val isOnline: Boolean = true,
    val messages: List<ChatMessage> = emptyList()
)

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED
}

enum class CallMode(val displayName: String) {
    INTERNET_AUDIO("Internet Audio Call"),
    INTERNET_VIDEO("Internet Video Call"),
    SIM_CARRIER("SIM / Carrier Call")
}

data class CallLogEntry(
    val id: String,
    val name: String,
    val phone: String,
    val type: CallType,
    val timestamp: String,
    val duration: String = "",
    val mode: CallMode = CallMode.INTERNET_AUDIO,
    val simSlot: String = "Internet" // "SIM 1", "SIM 2", or "Internet"
)

data class ActiveCallState(
    val callId: String,
    val contactName: String,
    val phone: String,
    val mode: CallMode, // INTERNET_AUDIO or INTERNET_VIDEO
    val isIncoming: Boolean,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isVideoDisabled: Boolean = false,
    val durationSeconds: Int = 0,
    val isConnected: Boolean = false
)

enum class NetworkTransport(val displayName: String) {
    WIFI("Wi-Fi"),
    CELLULAR("Mobile Data"),
    ETHERNET("Ethernet"),
    OTHER("Connected"),
    NONE("No Connection")
}

data class NetworkState(
    val isOnline: Boolean,
    val isValidated: Boolean,
    val transport: NetworkTransport,
    val label: String
)

enum class SyncItemType {
    CONTACT,
    MESSAGE,
    PROFILE_UPDATE,
    FRIEND_ACTION
}

data class PendingSyncItem(
    val id: String,
    val type: SyncItemType,
    val title: String,
    val subtitle: String,
    val timestamp: String,
    val payload: String = ""
)

enum class OfflineScreen(val titleKey: String) {
    CONTACTS("contacts"),
    SMS_MMS("sms_mms"),
    SIM_CALLS("sim_calls"),
    PENDING_SYNC("pending_sync"),
    LOCAL_PROFILE("local_profile")
}

