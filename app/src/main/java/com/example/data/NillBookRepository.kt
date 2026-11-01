package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object NillBookRepository {

    // Online / Offline Dual Mode State
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // Shared message draft preserved across Online <-> Offline mode switching
    private val _activeDraftText = MutableStateFlow("")
    val activeDraftText: StateFlow<String> = _activeDraftText.asStateFlow()

    fun setActiveDraft(draft: String) {
        _activeDraftText.value = draft
    }

    // Pending Sync Queue for items created offline
    private val _pendingSyncItems = MutableStateFlow<List<PendingSyncItem>>(
        listOf(
            PendingSyncItem(
                id = "sync_1",
                type = SyncItemType.MESSAGE,
                title = "Queued message to Rahim Bhai",
                subtitle = "\"Need to check the schedule...\"",
                timestamp = "Today, 10:15 AM",
                payload = "Need to check the schedule..."
            ),
            PendingSyncItem(
                id = "sync_2",
                type = SyncItemType.CONTACT,
                title = "Local contact: Anisur Rahman",
                subtitle = "Phone: +8801711223344",
                timestamp = "Today, 09:30 AM",
                payload = "+8801711223344"
            )
        )
    )
    val pendingSyncItems: StateFlow<List<PendingSyncItem>> = _pendingSyncItems.asStateFlow()

    fun addPendingSyncItem(item: PendingSyncItem) {
        _pendingSyncItems.value = listOf(item) + _pendingSyncItems.value
    }

    fun removePendingSyncItem(id: String) {
        _pendingSyncItems.value = _pendingSyncItems.value.filter { it.id != id }
    }

    fun setOnlineMode(online: Boolean) {
        _isOnline.value = online
        if (online) {
            syncOfflineData()
        }
    }

    fun toggleOnlineMode() {
        setOnlineMode(!_isOnline.value)
    }

    // Active Call State (for In-App Real-Time Call Screen)
    private val _activeCallState = MutableStateFlow<ActiveCallState?>(null)
    val activeCallState: StateFlow<ActiveCallState?> = _activeCallState.asStateFlow()

    // Blocked users set
    private val _blockedUsers = MutableStateFlow<Set<String>>(setOf())
    val blockedUsers: StateFlow<Set<String>> = _blockedUsers.asStateFlow()

    // Current logged-in user
    private val _currentUser = MutableStateFlow<User?>(
        User(
            id = "user_me",
            name = "Blancho Nokrek",
            email = "blancho@example.com",
            phone = "+8801712345678",
            username = "blancho_n",
            gender = "Male",
            birthday = "1998-06-20",
            bio = "Connecting on NILLBOOK caller directory.",
            about = "Senior communicator & tech enthusiast. Active in Bangladesh and international caller communities.",
            location = "Dhaka, Bangladesh",
            showNumberPublic = false,
            presence = UserPresence.ONLINE,
            lastSeen = "Online now",
            twoFactorEnabled = true,
            createdAt = "2024-01-10"
        )
    )
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Global registered directory users
    private val registeredUsers = mutableListOf(
        User(
            id = "user_me",
            name = "Blancho Nokrek",
            email = "blancho@example.com",
            phone = "+8801712345678",
            username = "blancho_n",
            gender = "Male",
            birthday = "1998-06-20",
            showNumberPublic = false,
            presence = UserPresence.ONLINE
        ),
        User(
            id = "user_rahim",
            name = "Rahim Bhai",
            email = "rahim@example.com",
            phone = "+8801700000000",
            username = "rahim_bhai",
            gender = "Male",
            birthday = "1988-04-12",
            showNumberPublic = false,
            presence = UserPresence.ONLINE
        ),
        User(
            id = "user_farhana",
            name = "Farhana Akter",
            email = "farhana@example.com",
            phone = "+8801819876543",
            username = "farhana_a",
            gender = "Female",
            birthday = "1994-09-24",
            showNumberPublic = true,
            presence = UserPresence.ONLINE
        ),
        User(
            id = "user_tanvir",
            name = "Tanvir Ahmed",
            email = "tanvir@example.com",
            phone = "+8801912334455",
            username = "tanvir_ahmed",
            gender = "Male",
            birthday = "1992-11-05",
            showNumberPublic = false,
            presence = UserPresence.AWAY
        ),
        User(
            id = "user_amit",
            name = "Amit Sharma",
            email = "amit@example.com",
            phone = "+919876543210",
            username = "amit_sharma",
            gender = "Male",
            birthday = "1990-03-18",
            showNumberPublic = true,
            presence = UserPresence.OFFLINE
        ),
        User(
            id = "user_sarah",
            name = "Sarah Khan",
            email = "sarah@example.com",
            phone = "+923001234567",
            username = "sarah_khan",
            gender = "Female",
            birthday = "1996-07-30",
            showNumberPublic = false,
            presence = UserPresence.ONLINE
        ),
        User(
            id = "user_nafis",
            name = "Nafis Iqbal",
            email = "nafis@example.com",
            phone = "+8801723456789",
            username = "nafis_iqbal",
            gender = "Male",
            birthday = "1997-03-12",
            showNumberPublic = false,
            presence = UserPresence.ONLINE
        ),
        User(
            id = "user_tasnim",
            name = "Tasnim Jahan",
            email = "tasnim@example.com",
            phone = "+8801834567890",
            username = "tasnim_j",
            gender = "Female",
            birthday = "1999-11-20",
            showNumberPublic = true,
            presence = UserPresence.ONLINE
        ),
        User(
            id = "user_mehedi",
            name = "Mehedi Hasan",
            email = "mehedi@example.com",
            phone = "+8801512345678",
            username = "mehedi_h",
            gender = "Male",
            birthday = "1993-05-15",
            showNumberPublic = false,
            presence = UserPresence.OFFLINE
        )
    )

    // Community crowdsourced phonebook saved names
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

    // User's private saved contacts with groups, favorites, and dual-mode sync status
    private val _userContacts = MutableStateFlow<List<Contact>>(
        listOf(
            Contact(
                id = "c1",
                name = "Rahim Bhai",
                phone = "+8801700000000",
                secondaryPhone = "+8801711000000",
                email = "rahim@bhai.com",
                address = "Dhanmondi, Dhaka",
                birthday = "1988-04-12",
                isFavorite = true,
                group = ContactGroup.FAMILY,
                isRegisteredOnNillBook = true,
                isOnline = true,
                syncStatus = SyncStatus.SYNCED
            ),
            Contact(
                id = "c2",
                name = "Farhana Akter",
                phone = "+8801819876543",
                secondaryPhone = null,
                email = "farhana.akter@gmail.com",
                address = "Gulshan-1, Dhaka",
                birthday = "1994-09-24",
                isFavorite = true,
                group = ContactGroup.FRIENDS,
                isRegisteredOnNillBook = true,
                isOnline = true,
                syncStatus = SyncStatus.SYNCED
            ),
            Contact(
                id = "c3",
                name = "Dr. Kamal Hossain",
                phone = "+8801555667788",
                secondaryPhone = null,
                email = "dr.kamal@apollohospital.org",
                address = "Apollo Hospital, Bashundhara",
                birthday = "1975-08-10",
                isFavorite = false,
                group = ContactGroup.WORK,
                isRegisteredOnNillBook = false,
                isOnline = false,
                syncStatus = SyncStatus.SYNCED
            ),
            Contact(
                id = "c4",
                name = "Tanvir Ahmed",
                phone = "+8801912334455",
                secondaryPhone = null,
                email = "tanvir@tech.com",
                address = "Banani, Dhaka",
                birthday = "1992-11-05",
                isFavorite = false,
                group = ContactGroup.FRIENDS,
                isRegisteredOnNillBook = true,
                isOnline = false,
                syncStatus = SyncStatus.SYNCED
            )
        )
    )
    val userContacts: StateFlow<List<Contact>> = _userContacts.asStateFlow()

    // Friendships: requesterId to addresseeId with status ("ACCEPTED", "PENDING")
    private val _friendships = MutableStateFlow<List<FriendshipEntry>>(
        listOf(
            FriendshipEntry("f1", "user_farhana", "user_me", "ACCEPTED"),
            FriendshipEntry("f2", "user_tanvir", "user_me", "PENDING"),
            FriendshipEntry("f3", "user_me", "user_rahim", "ACCEPTED")
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

    fun deleteAccount() {
        val currentId = _currentUser.value?.id ?: return
        registeredUsers.removeAll { it.id == currentId }
        _friendships.value = _friendships.value.filterNot { it.requesterId == currentId || it.addresseeId == currentId }
        _currentUser.value = null
    }

    fun updateProfile(
        name: String,
        phone: String,
        gender: String,
        birthday: String,
        bio: String,
        location: String,
        showPublic: Boolean,
        about: String = "",
        username: String = ""
    ) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            name = name.trim(),
            phone = phone.trim(),
            gender = gender,
            birthday = birthday,
            bio = bio.trim(),
            about = if (about.isNotBlank()) about.trim() else current.about,
            username = if (username.isNotBlank()) username.trim() else current.username,
            location = location.trim(),
            showNumberPublic = showPublic
        )
        _currentUser.value = updated
        val idx = registeredUsers.indexOfFirst { it.id == current.id }
        if (idx != -1) {
            registeredUsers[idx] = updated
        }
    }

    fun updateProfilePhoto(photoUri: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(photo = photoUri)
        _currentUser.value = updated
        val idx = registeredUsers.indexOfFirst { it.id == current.id }
        if (idx != -1) {
            registeredUsers[idx] = updated
        }
    }

    fun updateCoverPhoto(coverUri: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(coverPhoto = coverUri)
        _currentUser.value = updated
        val idx = registeredUsers.indexOfFirst { it.id == current.id }
        if (idx != -1) {
            registeredUsers[idx] = updated
        }
    }

    fun getUserById(userId: String): User? {
        if (_currentUser.value?.id == userId) return _currentUser.value
        return registeredUsers.find { it.id == userId }
    }

    fun getUserByPhone(phone: String): User? {
        if (_currentUser.value?.phone == phone) return _currentUser.value
        val e164 = PhoneUtils.toE164(phone) ?: phone
        return registeredUsers.find { it.phone == phone || it.phone == e164 }
    }

    // Contact Operations
    fun addContact(
        name: String,
        phone: String,
        email: String? = null,
        address: String? = null,
        birthday: String? = null,
        secondaryPhone: String? = null,
        group: ContactGroup = ContactGroup.ALL,
        isFavorite: Boolean = false
    ): Boolean {
        val current = _userContacts.value
        if (current.any { it.phone == phone }) {
            return false // duplicate phone
        }
        val isRegistered = registeredUsers.any { it.phone == phone }
        val newContact = Contact(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            phone = phone.trim(),
            secondaryPhone = secondaryPhone?.trim()?.ifEmpty { null },
            email = email?.trim()?.ifEmpty { null },
            address = address?.trim()?.ifEmpty { null },
            birthday = birthday?.trim()?.ifEmpty { null },
            isFavorite = isFavorite,
            group = group,
            isRegisteredOnNillBook = isRegistered,
            isOnline = isRegistered,
            syncStatus = if (_isOnline.value) SyncStatus.SYNCED else SyncStatus.PENDING_SYNC
        )
        _userContacts.value = (current + newContact).sortedBy { it.name.lowercase() }

        // Contribute to community crowdsourced directory
        val existingCrowd = crowdsourcedNames.find { it.phone == phone && it.name.equals(name, ignoreCase = true) }
        if (existingCrowd != null) {
            existingCrowd.count++
        } else {
            crowdsourcedNames.add(CrowdsourcedEntry(phone, name, 1))
        }
        return true
    }

    fun updateContact(
        id: String,
        name: String,
        phone: String,
        email: String?,
        address: String?,
        birthday: String?,
        secondaryPhone: String? = null,
        group: ContactGroup = ContactGroup.ALL,
        isFavorite: Boolean = false
    ): Boolean {
        val current = _userContacts.value
        val existing = current.find { it.id == id } ?: return false
        if (phone != existing.phone && current.any { it.phone == phone && it.id != id }) {
            return false
        }
        val isRegistered = registeredUsers.any { it.phone == phone }
        val updated = existing.copy(
            name = name.trim(),
            phone = phone.trim(),
            secondaryPhone = secondaryPhone?.trim()?.ifEmpty { null },
            email = email?.trim()?.ifEmpty { null },
            address = address?.trim()?.ifEmpty { null },
            birthday = birthday?.trim()?.ifEmpty { null },
            group = group,
            isFavorite = isFavorite,
            isRegisteredOnNillBook = isRegistered,
            isOnline = isRegistered,
            syncStatus = if (_isOnline.value) SyncStatus.SYNCED else SyncStatus.PENDING_SYNC
        )
        _userContacts.value = current.map { if (it.id == id) updated else it }.sortedBy { it.name.lowercase() }
        return true
    }

    fun toggleFavorite(id: String) {
        val current = _userContacts.value
        _userContacts.value = current.map {
            if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it
        }
    }

    fun setContactGroup(id: String, group: ContactGroup) {
        val current = _userContacts.value
        _userContacts.value = current.map {
            if (it.id == id) it.copy(group = group) else it
        }
    }

    fun deleteContact(id: String) {
        _userContacts.value = _userContacts.value.filter { it.id != id }
    }

    fun importDeviceContacts(deviceContacts: List<Contact>): Int {
        var added = 0
        for (c in deviceContacts) {
            if (addContact(c.name, c.phone, c.email, c.address, c.birthday)) {
                added++
            }
        }
        return added
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

    // Duplicate detection and merging
    fun findDuplicateContacts(): List<Pair<Contact, Contact>> {
        val list = _userContacts.value
        val duplicates = mutableListOf<Pair<Contact, Contact>>()
        for (i in 0 until list.size) {
            for (j in i + 1 until list.size) {
                val c1 = list[i]
                val c2 = list[j]
                val sameName = c1.name.equals(c2.name, ignoreCase = true)
                val samePhone = c1.phone == c2.phone || (c1.phone.endsWith(c2.phone.takeLast(8)) && c2.phone.length >= 8)
                if (sameName || samePhone) {
                    duplicates.add(Pair(c1, c2))
                }
            }
        }
        return duplicates
    }

    fun mergeContacts(primaryId: String, duplicateId: String): Boolean {
        val current = _userContacts.value
        val primary = current.find { it.id == primaryId } ?: return false
        val duplicate = current.find { it.id == duplicateId } ?: return false

        val merged = primary.copy(
            secondaryPhone = primary.secondaryPhone ?: duplicate.phone.takeIf { it != primary.phone },
            email = primary.email ?: duplicate.email,
            address = primary.address ?: duplicate.address,
            birthday = primary.birthday ?: duplicate.birthday,
            isFavorite = primary.isFavorite || duplicate.isFavorite
        )

        _userContacts.value = current.filterNot { it.id == duplicateId }.map {
            if (it.id == primaryId) merged else it
        }
        return true
    }

    // Search Operations
    fun searchByNumber(query: String, country: String): SearchResult? {
        val me = _currentUser.value
        val meId = me?.id
        val e164 = PhoneUtils.toE164(query, country) ?: query.trim()

        val owner = registeredUsers.find { it.phone == e164 }
        val friendStatus = getFriendStatus(meId, owner?.id)

        if (owner != null) {
            return SearchResult(
                number = e164,
                name = owner.name,
                count = 0,
                registered = true,
                userId = owner.id,
                friendStatus = friendStatus,
                numberVisible = true
            )
        }

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
    fun getFriendStatus(meId: String?, otherId: String?): String {
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

    fun blockUser(phoneOrId: String) {
        _blockedUsers.value = _blockedUsers.value + phoneOrId
    }

    fun unblockUser(phoneOrId: String) {
        _blockedUsers.value = _blockedUsers.value - phoneOrId
    }

    fun getFriendsList(): List<FriendItem> {
        val me = _currentUser.value ?: return emptyList()
        val list = mutableListOf<FriendItem>()
        for (f in _friendships.value) {
            if (f.requesterId == me.id) {
                val targetUser = registeredUsers.find { it.id == f.addresseeId } ?: continue
                val status = if (f.status == "ACCEPTED") "friends" else "pending_out"
                val phoneToShow = if (status == "friends" || targetUser.showNumberPublic) targetUser.phone else PhoneUtils.maskNumber(targetUser.phone)
                list.add(
                    FriendItem(
                        userId = targetUser.id,
                        name = targetUser.name,
                        phone = phoneToShow,
                        requestId = f.id,
                        status = status,
                        isOnline = targetUser.presence == UserPresence.ONLINE,
                        lastSeen = targetUser.lastSeen
                    )
                )
            } else if (f.addresseeId == me.id) {
                val targetUser = registeredUsers.find { it.id == f.requesterId } ?: continue
                val status = if (f.status == "ACCEPTED") "friends" else "pending_in"
                val phoneToShow = if (status == "friends" || targetUser.showNumberPublic) targetUser.phone else PhoneUtils.maskNumber(targetUser.phone)
                list.add(
                    FriendItem(
                        userId = targetUser.id,
                        name = targetUser.name,
                        phone = phoneToShow,
                        requestId = f.id,
                        status = status,
                        isOnline = targetUser.presence == UserPresence.ONLINE,
                        lastSeen = targetUser.lastSeen
                    )
                )
            }
        }
        return list
    }

    fun getFriendSuggestions(): List<FriendSuggestion> {
        val me = _currentUser.value ?: return emptyList()
        val activeIds = _friendships.value.flatMap { listOf(it.requesterId, it.addresseeId) }.toSet()

        return registeredUsers
            .filter { it.id != me.id && !activeIds.contains(it.id) }
            .map { u ->
                FriendSuggestion(
                    userId = u.id,
                    name = u.name,
                    subtitle = "Registered Caller • Verified NILLBOOK Profile",
                    mutualFriendsCount = when (u.id) {
                        "user_amit" -> 2
                        "user_sarah" -> 1
                        "user_nafis" -> 3
                        "user_tasnim" -> 2
                        else -> 1
                    },
                    phone = PhoneUtils.maskNumber(u.phone),
                    isOnline = u.presence == UserPresence.ONLINE
                )
            }
    }

    // Conversations & Smart 3-Option Messaging (Internet, SMS, MMS)
    private val _conversations = MutableStateFlow<List<ChatConversation>>(
        listOf(
            ChatConversation(
                id = "conv_rahim",
                contactName = "Rahim Bhai",
                phone = "+8801700000000",
                lastMessage = "Assalamu Alaikum bhai, kemon achhen?",
                lastTimestamp = "10:45 AM",
                unreadCount = 1,
                isPinned = true,
                isOnline = true,
                messages = listOf(
                    ChatMessage(
                        id = "m1",
                        text = "Hello Rahim bhai",
                        timestamp = "10:30 AM",
                        isFromMe = true,
                        channel = MessageChannel.INTERNET,
                        status = MessageDeliveryStatus.READ
                    ),
                    ChatMessage(
                        id = "m2",
                        text = "Assalamu Alaikum bhai, kemon achhen?",
                        timestamp = "10:45 AM",
                        isFromMe = false,
                        channel = MessageChannel.INTERNET,
                        status = MessageDeliveryStatus.READ
                    )
                )
            ),
            ChatConversation(
                id = "conv_farhana",
                contactName = "Farhana Akter",
                phone = "+8801819876543",
                lastMessage = "Got your number from NILLBOOK!",
                lastTimestamp = "Yesterday",
                unreadCount = 0,
                isPinned = false,
                isOnline = true,
                messages = listOf(
                    ChatMessage(
                        id = "m3",
                        text = "Hi! Are you on NillBook?",
                        timestamp = "Yesterday 4:10 PM",
                        isFromMe = true,
                        channel = MessageChannel.INTERNET,
                        status = MessageDeliveryStatus.READ
                    ),
                    ChatMessage(
                        id = "m4",
                        text = "Yes, got your number from NILLBOOK!",
                        timestamp = "Yesterday 4:15 PM",
                        isFromMe = false,
                        channel = MessageChannel.INTERNET,
                        status = MessageDeliveryStatus.READ
                    )
                )
            ),
            ChatConversation(
                id = "conv_tanvir",
                contactName = "Tanvir Ahmed",
                phone = "+8801912334455",
                lastMessage = "Sent via Carrier SMS",
                lastTimestamp = "Oct 28",
                unreadCount = 0,
                isPinned = false,
                isOnline = false,
                messages = listOf(
                    ChatMessage(
                        id = "m5",
                        text = "Hey Tanvir, let's connect this weekend",
                        timestamp = "Oct 28 2:00 PM",
                        isFromMe = true,
                        channel = MessageChannel.SMS,
                        status = MessageDeliveryStatus.DELIVERED
                    )
                )
            )
        )
    )
    val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    fun sendMessage(
        phone: String,
        contactName: String,
        text: String,
        channel: MessageChannel = if (_isOnline.value) MessageChannel.INTERNET else MessageChannel.SMS,
        attachmentUrl: String? = null,
        attachmentType: String? = null,
        audioDuration: Int? = null,
        replyToText: String? = null
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() && attachmentUrl == null) return

        val deliveryStatus = if (channel == MessageChannel.INTERNET) {
            if (_isOnline.value) MessageDeliveryStatus.SENT else MessageDeliveryStatus.SENDING
        } else {
            MessageDeliveryStatus.SENT // SMS network dispatched
        }

        val newMsg = ChatMessage(
            id = "msg_${UUID.randomUUID().toString().take(8)}",
            text = if (trimmed.isEmpty() && attachmentType == "image") "📷 Photo attachment" else if (trimmed.isEmpty() && attachmentType == "audio") "🎤 Voice message (${audioDuration ?: 4}s)" else trimmed,
            timestamp = "Just now",
            isFromMe = true,
            channel = channel,
            status = deliveryStatus,
            attachmentUrl = attachmentUrl,
            attachmentType = attachmentType,
            audioDurationSeconds = audioDuration,
            replyToText = replyToText
        )

        val current = _conversations.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.phone == phone }

        if (existingIndex != -1) {
            val conv = current[existingIndex]
            current[existingIndex] = conv.copy(
                lastMessage = newMsg.text,
                lastTimestamp = "Just now",
                messages = conv.messages + newMsg
            )
        } else {
            current.add(
                0,
                ChatConversation(
                    id = "conv_${UUID.randomUUID().toString().take(8)}",
                    contactName = contactName,
                    phone = phone,
                    lastMessage = newMsg.text,
                    lastTimestamp = "Just now",
                    unreadCount = 0,
                    messages = listOf(newMsg)
                )
            )
        }
        _conversations.value = current
    }

    fun editMessage(phone: String, messageId: String, newText: String) {
        val current = _conversations.value.toMutableList()
        val idx = current.indexOfFirst { it.phone == phone }
        if (idx != -1) {
            val conv = current[idx]
            val updatedMessages = conv.messages.map {
                if (it.id == messageId) it.copy(text = newText, isEdited = true) else it
            }
            current[idx] = conv.copy(messages = updatedMessages)
            _conversations.value = current
        }
    }

    fun deleteMessage(phone: String, messageId: String) {
        val current = _conversations.value.toMutableList()
        val idx = current.indexOfFirst { it.phone == phone }
        if (idx != -1) {
            val conv = current[idx]
            val updatedMessages = conv.messages.filterNot { it.id == messageId }
            current[idx] = conv.copy(
                messages = updatedMessages,
                lastMessage = updatedMessages.lastOrNull()?.text ?: "No messages"
            )
            _conversations.value = current
        }
    }

    fun toggleReaction(phone: String, messageId: String, emoji: String) {
        val current = _conversations.value.toMutableList()
        val idx = current.indexOfFirst { it.phone == phone }
        if (idx != -1) {
            val conv = current[idx]
            val updatedMessages = conv.messages.map { msg ->
                if (msg.id == messageId) {
                    val existingReaction = msg.reactions.find { it.emoji == emoji }
                    val newReactions = if (existingReaction != null) {
                        msg.reactions.filterNot { it.emoji == emoji }
                    } else {
                        msg.reactions + MessageReaction(emoji, 1)
                    }
                    msg.copy(reactions = newReactions)
                } else msg
            }
            current[idx] = conv.copy(messages = updatedMessages)
            _conversations.value = current
        }
    }

    fun togglePinConversation(phone: String) {
        val current = _conversations.value.toMutableList()
        val idx = current.indexOfFirst { it.phone == phone }
        if (idx != -1) {
            current[idx] = current[idx].copy(isPinned = !current[idx].isPinned)
            _conversations.value = current
        }
    }

    fun toggleArchiveConversation(phone: String) {
        val current = _conversations.value.toMutableList()
        val idx = current.indexOfFirst { it.phone == phone }
        if (idx != -1) {
            current[idx] = current[idx].copy(isArchived = !current[idx].isArchived)
            _conversations.value = current
        }
    }

    fun toggleMuteConversation(phone: String) {
        val current = _conversations.value.toMutableList()
        val idx = current.indexOfFirst { it.phone == phone }
        if (idx != -1) {
            current[idx] = current[idx].copy(isMuted = !current[idx].isMuted)
            _conversations.value = current
        }
    }

    fun clearConversation(phone: String) {
        val current = _conversations.value.toMutableList()
        val idx = current.indexOfFirst { it.phone == phone }
        if (idx != -1) {
            current[idx] = current[idx].copy(messages = emptyList(), lastMessage = "Chat cleared", unreadCount = 0)
            _conversations.value = current
        }
    }

    fun deleteConversation(phone: String) {
        _conversations.value = _conversations.value.filterNot { it.phone == phone }
    }

    // Call History Logs & Real-Time Call Simulation
    private val _callLogs = MutableStateFlow<List<CallLogEntry>>(
        listOf(
            CallLogEntry("log_1", "Rahim Bhai", "+8801700000000", CallType.INCOMING, "Today, 11:42 AM", "2m 15s", CallMode.INTERNET_AUDIO, "Internet"),
            CallLogEntry("log_2", "Farhana Akter", "+8801819876543", CallType.MISSED, "Today, 10:15 AM", "", CallMode.INTERNET_VIDEO, "Internet"),
            CallLogEntry("log_3", "Dr. Kamal Hossain", "+8801555667788", CallType.OUTGOING, "Yesterday, 4:30 PM", "1m 05s", CallMode.SIM_CARRIER, "SIM 1"),
            CallLogEntry("log_4", "Unknown Caller", "+8801912334455", CallType.MISSED, "Yesterday, 1:20 PM", "", CallMode.SIM_CARRIER, "SIM 2"),
            CallLogEntry("log_5", "Shakib Al Hasan Fan Club", "+8801300998877", CallType.INCOMING, "Oct 29, 6:00 PM", "45s", CallMode.INTERNET_AUDIO, "Internet")
        )
    )
    val callLogs: StateFlow<List<CallLogEntry>> = _callLogs.asStateFlow()

    fun recordCall(
        name: String,
        phone: String,
        type: CallType = CallType.OUTGOING,
        mode: CallMode = CallMode.INTERNET_AUDIO,
        simSlot: String = if (mode == CallMode.SIM_CARRIER) "SIM 1" else "Internet",
        duration: String = if (type == CallType.MISSED) "" else "0m 30s"
    ) {
        val newEntry = CallLogEntry(
            id = "log_${UUID.randomUUID().toString().take(8)}",
            name = name,
            phone = phone,
            type = type,
            timestamp = "Just now",
            duration = duration,
            mode = mode,
            simSlot = simSlot
        )
        _callLogs.value = listOf(newEntry) + _callLogs.value
    }

    fun clearCallLogs() {
        _callLogs.value = emptyList()
    }

    // Interactive Real-Time Internet Call Controls
    fun startInternetCall(name: String, phone: String, mode: CallMode) {
        _activeCallState.value = ActiveCallState(
            callId = UUID.randomUUID().toString(),
            contactName = name,
            phone = phone,
            mode = mode,
            isIncoming = false,
            durationSeconds = 0,
            isConnected = true
        )
    }

    fun toggleCallMute() {
        val cur = _activeCallState.value ?: return
        _activeCallState.value = cur.copy(isMuted = !cur.isMuted)
    }

    fun toggleCallSpeaker() {
        val cur = _activeCallState.value ?: return
        _activeCallState.value = cur.copy(isSpeakerOn = !cur.isSpeakerOn)
    }

    fun toggleCallVideo() {
        val cur = _activeCallState.value ?: return
        _activeCallState.value = cur.copy(isVideoDisabled = !cur.isVideoDisabled)
    }

    fun endActiveCall() {
        val cur = _activeCallState.value ?: return
        recordCall(
            name = cur.contactName,
            phone = cur.phone,
            type = CallType.OUTGOING,
            mode = cur.mode,
            simSlot = "Internet",
            duration = "${cur.durationSeconds / 60}m ${cur.durationSeconds % 60}s"
        )
        _activeCallState.value = null
    }

    // Offline data synchronizer
    fun syncOfflineData(): Int {
        var syncedCount = _pendingSyncItems.value.size
        _pendingSyncItems.value = emptyList()

        // 1. Sync pending contacts
        _userContacts.value = _userContacts.value.map {
            if (it.syncStatus == SyncStatus.PENDING_SYNC) {
                syncedCount++
                it.copy(syncStatus = SyncStatus.SYNCED)
            } else it
        }
        // 2. Sync pending messages
        _conversations.value = _conversations.value.map { conv ->
            val updatedMessages = conv.messages.map { msg ->
                if (msg.status == MessageDeliveryStatus.SENDING) {
                    syncedCount++
                    msg.copy(status = MessageDeliveryStatus.SENT)
                } else msg
            }
            conv.copy(messages = updatedMessages)
        }
        return syncedCount
    }
}
