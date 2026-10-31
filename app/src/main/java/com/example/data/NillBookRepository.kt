package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object NillBookRepository {

    // Current logged-in user
    private val _currentUser = MutableStateFlow<User?>(
        User(
            id = "user_me",
            name = "Blancho Nokrek",
            email = "blancho@example.com",
            phone = "+8801712345678",
            gender = "Male",
            birthday = "1998-06-20",
            showNumberPublic = false,
            createdAt = "2024-01-10"
        )
    )
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Global registered users
    private val registeredUsers = mutableListOf(
        User(
            id = "user_me",
            name = "Blancho Nokrek",
            email = "blancho@example.com",
            phone = "+8801712345678",
            gender = "Male",
            birthday = "1998-06-20",
            showNumberPublic = false
        ),
        User(
            id = "user_rahim",
            name = "Rahim Bhai",
            email = "rahim@example.com",
            phone = "+8801700000000",
            gender = "Male",
            birthday = "1988-04-12",
            showNumberPublic = false
        ),
        User(
            id = "user_farhana",
            name = "Farhana Akter",
            email = "farhana@example.com",
            phone = "+8801819876543",
            gender = "Female",
            birthday = "1994-09-24",
            showNumberPublic = true
        ),
        User(
            id = "user_tanvir",
            name = "Tanvir Ahmed",
            email = "tanvir@example.com",
            phone = "+8801912334455",
            gender = "Male",
            birthday = "1992-11-05",
            showNumberPublic = false
        ),
        User(
            id = "user_amit",
            name = "Amit Sharma",
            email = "amit@example.com",
            phone = "+919876543210",
            gender = "Male",
            birthday = "1990-03-18",
            showNumberPublic = true
        ),
        User(
            id = "user_sarah",
            name = "Sarah Khan",
            email = "sarah@example.com",
            phone = "+923001234567",
            gender = "Female",
            birthday = "1996-07-30",
            showNumberPublic = false
        )
    )

    // Community crowdsourced phonebook saved names (maps Phone E164 -> list of names saved by different people)
    private val crowdsourcedNames = mutableListOf(
        CrowdsourcedEntry("+8801700000000", "Rahim Bhai", 4),
        CrowdsourcedEntry("+8801700000000", "Rahim Mistri", 2),
        CrowdsourcedEntry("+8801819876543", "Farhana Akter", 7),
        CrowdsourcedEntry("+8801912334455", "Tanvir Ahmed", 5),
        CrowdsourcedEntry("+8801555667788", "Dr. Kamal Hossain", 14),
        CrowdsourcedEntry("+8801300998877", "Shakib Al Hasan Fan Club", 9),
        CrowdsourcedEntry("+8801711223344", "Anisur Rahman Electrician", 3),
        CrowdsourcedEntry("+919876543210", "Amit Sharma Tech", 6),
        CrowdsourcedEntry("+447700900077", "Johnathan Smith London", 8),
        CrowdsourcedEntry("+12025550143", "Emily Watson Washington", 12)
    )

    // User's private saved contacts
    private val _userContacts = MutableStateFlow<List<Contact>>(
        listOf(
            Contact(
                id = "c1",
                name = "Rahim Bhai",
                phone = "+8801700000000",
                email = "rahim@bhai.com",
                address = "Dhanmondi, Dhaka",
                birthday = "1988-04-12"
            ),
            Contact(
                id = "c2",
                name = "Farhana Akter",
                phone = "+8801819876543",
                email = "farhana.akter@gmail.com",
                address = "Gulshan-1, Dhaka",
                birthday = "1994-09-24"
            ),
            Contact(
                id = "c3",
                name = "Dr. Kamal Hossain",
                phone = "+8801555667788",
                email = "dr.kamal@apollohospital.org",
                address = "Apollo Hospital, Bashundhara",
                birthday = "1975-08-10"
            )
        )
    )
    val userContacts: StateFlow<List<Contact>> = _userContacts.asStateFlow()

    // Friendships: requesterId to addresseeId with status ("friends", "pending_in", "pending_out")
    private val _friendships = MutableStateFlow<List<FriendshipEntry>>(
        listOf(
            FriendshipEntry("f1", "user_farhana", "user_me", "ACCEPTED"),
            FriendshipEntry("f2", "user_tanvir", "user_me", "PENDING")
        )
    )
    val friendships: StateFlow<List<FriendshipEntry>> = _friendships.asStateFlow()

    data class CrowdsourcedEntry(val phone: String, val name: String, var count: Int)
    data class FriendshipEntry(val id: String, val requesterId: String, val addresseeId: String, var status: String)

    // Authentication
    fun login(identifier: String, country: String): Boolean {
        val trimmed = identifier.trim()
        val user = if (trimmed.contains("@")) {
            registeredUsers.find { it.email.equals(trimmed, ignoreCase = true) }
        } else {
            val e164 = PhoneUtils.toE164(trimmed, country) ?: trimmed
            registeredUsers.find { it.phone == e164 }
        }
        if (user != null) {
            _currentUser.value = user
            return true
        }
        // If not found in seed, create account on the fly for ease of use
        val isEmail = trimmed.contains("@")
        val newUser = User(
            id = "user_${UUID.randomUUID().toString().take(8)}",
            name = if (isEmail) trimmed.substringBefore("@").replaceFirstChar { it.uppercase() } else "User ${trimmed.takeLast(4)}",
            email = if (isEmail) trimmed else "$trimmed@nillbook.app",
            phone = if (isEmail) "+8801700${(1000..9999).random()}" else (PhoneUtils.toE164(trimmed, country) ?: "+8801700000000"),
            gender = "Male",
            birthday = "1996-01-01"
        )
        registeredUsers.add(newUser)
        _currentUser.value = newUser
        return true
    }

    fun register(name: String, email: String, phone: String, gender: String, birthday: String): Boolean {
        val newUser = User(
            id = "user_${UUID.randomUUID().toString().take(8)}",
            name = name.trim(),
            email = email.trim(),
            phone = phone.trim(),
            gender = gender,
            birthday = birthday
        )
        registeredUsers.add(newUser)
        _currentUser.value = newUser
        return true
    }

    fun logout() {
        _currentUser.value = null
    }

    fun updateProfile(name: String, phone: String, gender: String, birthday: String, showPublic: Boolean) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            name = name,
            phone = phone,
            gender = gender,
            birthday = birthday,
            showNumberPublic = showPublic
        )
        _currentUser.value = updated
        val idx = registeredUsers.indexOfFirst { it.id == current.id }
        if (idx != -1) {
            registeredUsers[idx] = updated
        }
    }

    // Contact Operations
    fun addContact(name: String, phone: String, email: String?, address: String?, birthday: String?): Boolean {
        val current = _userContacts.value
        if (current.any { it.phone == phone }) {
            return false // duplicate
        }
        val newContact = Contact(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            phone = phone.trim(),
            email = email?.trim()?.ifEmpty { null },
            address = address?.trim()?.ifEmpty { null },
            birthday = birthday?.trim()?.ifEmpty { null }
        )
        _userContacts.value = (current + newContact).sortedBy { it.name.lowercase() }

        // Also contribute to community crowdsource directory
        val existingCrowd = crowdsourcedNames.find { it.phone == phone && it.name.equals(name, ignoreCase = true) }
        if (existingCrowd != null) {
            existingCrowd.count++
        } else {
            crowdsourcedNames.add(CrowdsourcedEntry(phone, name, 1))
        }
        return true
    }

    fun updateContact(id: String, name: String, phone: String, email: String?, address: String?, birthday: String?): Boolean {
        val current = _userContacts.value
        val existing = current.find { it.id == id } ?: return false
        if (phone != existing.phone && current.any { it.phone == phone && it.id != id }) {
            return false
        }
        val updated = existing.copy(
            name = name.trim(),
            phone = phone.trim(),
            email = email?.trim()?.ifEmpty { null },
            address = address?.trim()?.ifEmpty { null },
            birthday = birthday?.trim()?.ifEmpty { null }
        )
        _userContacts.value = current.map { if (it.id == id) updated else it }.sortedBy { it.name.lowercase() }
        return true
    }

    fun deleteContact(id: String) {
        _userContacts.value = _userContacts.value.filter { it.id != id }
    }

    fun importVcf(vcfText: String, country: String): Int {
        val parsedList = PhoneUtils.parseVcf(vcfText, country)
        var added = 0
        for (item in parsedList) {
            if (addContact(item.name, item.phone, item.email, item.address, item.birthday)) {
                added++
            }
        }
        return added
    }

    // Search Operations
    fun searchByNumber(query: String, country: String): SearchResult? {
        val me = _currentUser.value
        val meId = me?.id
        val e164 = PhoneUtils.toE164(query, country) ?: query.trim()

        val owner = registeredUsers.find { it.phone == e164 }
        val savedByMe = _userContacts.value.any { it.phone == e164 }
        val friendStatus = getFriendStatus(meId, owner?.id)

        if (owner != null) {
            return SearchResult(
                number = e164,
                name = owner.name,
                count = 0,
                registered = true,
                userId = owner.id,
                friendStatus = friendStatus,
                numberVisible = true // typed the number directly
            )
        }

        // Check crowdsourced names
        val matchingCrowd = crowdsourcedNames.filter { it.phone == e164 }
            .maxByOrNull { it.count }
        if (matchingCrowd != null) {
            return SearchResult(
                number = e164,
                name = matchingCrowd.name,
                count = matchingCrowd.count,
                registered = false,
                friendStatus = "none",
                numberVisible = true
            )
        }

        return null
    }

    fun searchByName(query: String): List<SearchResult> {
        val me = _currentUser.value
        val meId = me?.id
        val q = query.trim()
        if (q.length < 3) return emptyList()

        val mySavedPhones = _userContacts.value.map { it.phone }.toSet()
        val results = mutableListOf<SearchResult>()
        val seenKeys = mutableSetOf<String>()

        // 1. Search registered users
        for (u in registeredUsers) {
            val score = FuzzyMatcher.matchScore(q, u.name)
            if (score > 0) {
                val friendStatus = getFriendStatus(meId, u.id)
                val visible = friendStatus == "self" || friendStatus == "friends" || u.showNumberPublic || mySavedPhones.contains(u.phone)
                val displayNum = if (visible) u.phone else PhoneUtils.maskNumber(u.phone)
                results.add(
                    SearchResult(
                        number = displayNum,
                        name = u.name,
                        count = 0,
                        registered = true,
                        userId = u.id,
                        friendStatus = friendStatus,
                        numberVisible = visible
                    )
                )
                seenKeys.add("${u.phone}|${u.name}")
            }
        }

        // 2. Search crowdsourced entries
        for (entry in crowdsourcedNames) {
            val key = "${entry.phone}|${entry.name}"
            if (seenKeys.contains(key)) continue
            val score = FuzzyMatcher.matchScore(q, entry.name)
            if (score > 0) {
                val visible = mySavedPhones.contains(entry.phone)
                val displayNum = if (visible) entry.phone else PhoneUtils.maskNumber(entry.phone)
                results.add(
                    SearchResult(
                        number = displayNum,
                        name = entry.name,
                        count = entry.count,
                        registered = false,
                        userId = null,
                        friendStatus = "none",
                        numberVisible = visible
                    )
                )
                seenKeys.add(key)
            }
        }

        return results.sortedWith(
            compareByDescending<SearchResult> { it.registered }
                .thenByDescending { it.count }
        ).take(15)
    }

    // Friend Management
    private fun getFriendStatus(meId: String?, otherId: String?): String {
        if (meId == null || otherId == null) return "none"
        if (meId == otherId) return "self"
        val relation = _friendships.value.find {
            (it.requesterId == meId && it.addresseeId == otherId) ||
            (it.requesterId == otherId && it.addresseeId == meId)
        } ?: return "none"

        return if (relation.status == "ACCEPTED") {
            "friends"
        } else if (relation.requesterId == meId) {
            "pending_out"
        } else {
            "pending_in"
        }
    }

    fun sendFriendRequest(targetUserId: String) {
        val me = _currentUser.value ?: return
        if (me.id == targetUserId) return
        val list = _friendships.value.toMutableList()
        val existing = list.find {
            (it.requesterId == me.id && it.addresseeId == targetUserId) ||
            (it.requesterId == targetUserId && it.addresseeId == me.id)
        }
        if (existing != null) {
            if (existing.requesterId == targetUserId) {
                existing.status = "ACCEPTED"
            }
        } else {
            list.add(FriendshipEntry("f_${UUID.randomUUID().toString().take(6)}", me.id, targetUserId, "PENDING"))
        }
        _friendships.value = list
    }

    fun acceptFriendRequest(requestId: String) {
        val list = _friendships.value.toMutableList()
        list.find { it.id == requestId }?.status = "ACCEPTED"
        _friendships.value = list
    }

    fun declineFriendRequest(requestId: String) {
        val list = _friendships.value.filter { it.id != requestId }
        _friendships.value = list
    }

    fun removeFriend(userId: String) {
        val me = _currentUser.value ?: return
        _friendships.value = _friendships.value.filterNot {
            (it.requesterId == me.id && it.addresseeId == userId) ||
            (it.requesterId == userId && it.addresseeId == me.id)
        }
    }

    fun getFriendsList(): List<FriendItem> {
        val me = _currentUser.value ?: return emptyList()
        val out = mutableListOf<FriendItem>()
        for (f in _friendships.value) {
            val otherId = if (f.requesterId == me.id) f.addresseeId else f.requesterId
            val user = registeredUsers.find { it.id == otherId } ?: continue
            val status = if (f.status == "ACCEPTED") "friends" else if (f.addresseeId == me.id) "pending_in" else "pending_out"
            out.add(
                FriendItem(
                    userId = user.id,
                    name = user.name,
                    phone = if (status == "friends") user.phone else PhoneUtils.maskNumber(user.phone),
                    requestId = f.id,
                    status = status
                )
            )
        }
        return out
    }
}
