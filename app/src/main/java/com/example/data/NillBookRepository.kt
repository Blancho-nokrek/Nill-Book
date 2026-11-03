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
    private val _pendingSyncItems = MutableStateFlow<List<PendingSyncItem>>(emptyList())
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

    // Current logged-in user (null on launch: user must log in or create an account)
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Global registered directory users (Populated dynamically upon user registration)
    private val registeredUsers = mutableListOf<User>()

    // Community crowdsourced phonebook saved names (Contributed dynamically)
    private val crowdsourcedNames = mutableListOf<CrowdsourcedEntry>()

    // User's private saved contacts with groups, favorites, and dual-mode sync status
    private val _userContacts = MutableStateFlow<List<Contact>>(emptyList())
    val userContacts: StateFlow<List<Contact>> = _userContacts.asStateFlow()

    // Friendships: requesterId to addresseeId with status ("ACCEPTED", "PENDING")
    private val _friendships = MutableStateFlow<List<FriendshipEntry>>(emptyList())
    val friendships: StateFlow<List<FriendshipEntry>> = _friendships.asStateFlow()

    data class CrowdsourcedEntry(val phone: String, val name: String, var count: Int)
    data class FriendshipEntry(val id: String, val requesterId: String, val addresseeId: String, var status: String)

    init {
        syncFriendsToContacts()
    }

    fun isUserRegistered(phone: String): Boolean {
        val clean = phone.replace(" ", "").replace("-", "")
        return registeredUsers.any { it.phone.replace(" ", "").replace("-", "") == clean } ||
                _userContacts.value.any { it.phone.replace(" ", "").replace("-", "") == clean && it.isRegisteredOnNillBook }
    }

    fun syncFriendsToContacts() {
        val me = _currentUser.value ?: return
        val current = _userContacts.value.toMutableList()
        val friendList = getFriendsList().filter { it.status == "friends" }
        var changed = false
        for (friend in friendList) {
            val phone = friend.phone ?: continue
            val cleanPhone = phone.replace(" ", "").replace("-", "")
            val existingIndex = current.indexOfFirst {
                val c = it.phone.replace(" ", "").replace("-", "")
                c == cleanPhone || it.name.equals(friend.name, ignoreCase = true)
            }
            if (existingIndex == -1) {
                current.add(
                    Contact(
                        id = "friend_${friend.userId}",
                        name = friend.name,
                        phone = phone,
                        isFavorite = true,
                        group = ContactGroup.FRIENDS,
                        isRegisteredOnNillBook = true,
                        isOnline = friend.isOnline,
                        syncStatus = SyncStatus.SYNCED
                    )
                )
                changed = true
            } else {
                val ex = current[existingIndex]
                if (!ex.isRegisteredOnNillBook) {
                    current[existingIndex] = ex.copy(isRegisteredOnNillBook = true, isOnline = friend.isOnline)
                    changed = true
                }
            }
        }
        if (changed) {
            _userContacts.value = current
        }
    }

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
            phone = if (isEmail) "" else (PhoneUtils.toE164(trimmed, country) ?: trimmed),
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

    fun getContactByPhone(phone: String): Contact? {
        val clean = phone.replace(" ", "").replace("-", "")
        return _userContacts.value.firstOrNull {
            it.phone.replace(" ", "").replace("-", "") == clean
        }
    }

    fun getPhotoForPhone(phone: String): String? {
        if (_currentUser.value?.phone == phone) return _currentUser.value?.photo
        val clean = phone.replace(" ", "").replace("-", "")
        val contactPhoto = _userContacts.value.firstOrNull {
            it.phone.replace(" ", "").replace("-", "") == clean
        }?.photo
        if (!contactPhoto.isNullOrBlank()) return contactPhoto
        val userPhoto = registeredUsers.firstOrNull {
            it.phone.replace(" ", "").replace("-", "") == clean
        }?.photo
        if (!userPhoto.isNullOrBlank()) return userPhoto
        return null
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
        syncFriendsToContacts()
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
                    mutualFriendsCount = 0,
                    phone = PhoneUtils.maskNumber(u.phone),
                    isOnline = u.presence == UserPresence.ONLINE
                )
            }
    }

    // Conversations & Smart 3-Option Messaging (Internet, SMS, MMS)
    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    fun createGroupConversation(name: String, memberPhones: List<String>): ChatConversation {
        val groupId = "group_${UUID.randomUUID().toString().take(8)}"
        val newGroup = ChatConversation(
            id = groupId,
            contactName = name,
            phone = groupId,
            lastMessage = "Group created",
            lastTimestamp = "Just now",
            unreadCount = 0,
            isPinned = false,
            isOnline = true,
            isGroup = true,
            groupMembers = memberPhones,
            messages = listOf(
                ChatMessage(
                    id = "msg_grp_${UUID.randomUUID().toString().take(6)}",
                    text = "Welcome to $name group! Created with ${memberPhones.size} members.",
                    timestamp = "Just now",
                    isFromMe = true,
                    channel = MessageChannel.INTERNET,
                    status = MessageDeliveryStatus.SENT
                )
            )
        )
        _conversations.value = listOf(newGroup) + _conversations.value
        return newGroup
    }

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

        val me = _currentUser.value ?: registeredUsers.firstOrNull() ?: User(
            id = "user_me",
            name = "Me",
            email = "me@nillbook.app",
            phone = phone
        )
        val isRecipientOnline = registeredUsers.find { it.phone == phone }?.presence == UserPresence.ONLINE && _isOnline.value

        val newMsg: ChatMessage
        if (channel == MessageChannel.INTERNET) {
            // End-to-End Encryption (E2EE) with ECDH P-256 + AES-256-GCM
            val result = com.example.data.security.E2EEMessagingManager.sendE2EEMessage(
                sender = me,
                recipientPhone = phone,
                recipientName = contactName,
                plaintext = if (trimmed.isEmpty() && attachmentType == "image") "📷 Photo attachment" else if (trimmed.isEmpty() && attachmentType == "audio") "🎤 Voice message (${audioDuration ?: 4}s)" else trimmed,
                attachmentUrl = attachmentUrl,
                attachmentType = attachmentType,
                audioDuration = audioDuration,
                replyToText = replyToText,
                isRecipientOnline = isRecipientOnline
            )

            newMsg = when (result) {
                is com.example.data.security.E2EESendResult.DeliveredImmediately -> result.localChatMessage
                is com.example.data.security.E2EESendResult.QueuedOnServerForOfflineRecipient -> result.localChatMessage
                is com.example.data.security.E2EESendResult.Error -> {
                    ChatMessage(
                        id = "msg_${UUID.randomUUID().toString().take(8)}",
                        text = trimmed,
                        timestamp = "Just now",
                        isFromMe = true,
                        channel = channel,
                        status = MessageDeliveryStatus.FAILED,
                        isEncrypted = true,
                        encryptionAlgorithm = "AES-256-GCM / ECDH-P256"
                    )
                }
            }
        } else {
            // Carrier SIM SMS channel (Fallback when offline or user selected SMS)
            newMsg = ChatMessage(
                id = "msg_${UUID.randomUUID().toString().take(8)}",
                text = if (trimmed.isEmpty() && attachmentType == "image") "📷 Photo attachment" else if (trimmed.isEmpty() && attachmentType == "audio") "🎤 Voice message (${audioDuration ?: 4}s)" else trimmed,
                timestamp = "Just now",
                isFromMe = true,
                channel = channel,
                status = MessageDeliveryStatus.SENT,
                attachmentUrl = attachmentUrl,
                attachmentType = attachmentType,
                audioDurationSeconds = audioDuration,
                replyToText = replyToText,
                isEncrypted = false,
                encryptionAlgorithm = "None (Carrier SIM)"
            )
        }

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

    /**
     * Polls temporary server relay queue for any queued encrypted messages addressed to current user.
     * When messages arrive, they are decrypted on device, stored locally, and immediately deleted from server.
     */
    fun syncOfflineMessagesForCurrentUser(): Int {
        val me = _currentUser.value ?: return 0
        val decryptedMessages = com.example.data.security.E2EEMessagingManager.syncPendingOfflineMessages(me.phone)
        if (decryptedMessages.isEmpty()) return 0

        val current = _conversations.value.toMutableList()
        for (msg in decryptedMessages) {
            val senderPhone = msg.replyToSender ?: me.phone
            val idx = current.indexOfFirst { it.phone == senderPhone }
            if (idx != -1) {
                val conv = current[idx]
                current[idx] = conv.copy(
                    lastMessage = msg.text,
                    lastTimestamp = "Just now",
                    unreadCount = conv.unreadCount + 1,
                    messages = conv.messages + msg
                )
            }
        }
        _conversations.value = current
        return decryptedMessages.size
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
    private val _callLogs = MutableStateFlow<List<CallLogEntry>>(emptyList())
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
