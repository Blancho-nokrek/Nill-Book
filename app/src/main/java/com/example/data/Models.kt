package com.example.data

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val gender: String = "Male",
    val birthday: String = "1995-01-15",
    val bio: String = "Connecting on NILLBOOK caller directory.",
    val location: String = "Dhaka, Bangladesh",
    val photo: String? = null,
    val showNumberPublic: Boolean = false,
    val createdAt: String = "2024-01-01"
)

data class Contact(
    val id: String,
    val name: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val birthday: String? = null,
    val photo: String? = null
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
    val status: String = "friends" // "friends", "pending_in", "pending_out"
)

data class FriendSuggestion(
    val userId: String,
    val name: String,
    val subtitle: String,
    val mutualFriendsCount: Int = 1,
    val phone: String
)

data class Country(
    val code: String,
    val label: String,
    val dialCode: String
)

data class ChatMessage(
    val id: String,
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean
)

data class ChatConversation(
    val id: String,
    val contactName: String,
    val phone: String,
    val lastMessage: String,
    val lastTimestamp: String,
    val unreadCount: Int = 0,
    val messages: List<ChatMessage> = emptyList()
)

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED
}

data class CallLogEntry(
    val id: String,
    val name: String,
    val phone: String,
    val type: CallType,
    val timestamp: String,
    val duration: String = ""
)

