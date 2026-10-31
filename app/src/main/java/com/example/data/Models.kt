package com.example.data

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val gender: String = "Male",
    val birthday: String = "1995-01-15",
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

data class Country(
    val code: String,
    val label: String,
    val dialCode: String
)
