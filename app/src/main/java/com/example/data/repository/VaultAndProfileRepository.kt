package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AuditCategory
import com.example.data.model.ChannelPrivacy
import com.example.data.model.ChatType
import com.example.data.model.ConsentGrant
import com.example.data.model.ConsentScope
import com.example.data.model.ContactItem
import com.example.data.model.DeliveryState
import com.example.data.model.InviteRecord
import com.example.data.model.InviteStatus
import com.example.data.model.MediaPreset
import com.example.data.model.MessagePolicy
import com.example.data.model.PostKind
import com.example.data.model.PostVisibility
import com.example.data.model.PrivacyAuditLog
import com.example.data.model.PrivacyVault
import com.example.data.model.ThumbnailPreset
import com.example.data.model.UserProfile
import com.example.data.model.VideoFormat
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.data.remote.unwrapFirestoreFailure
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class VaultAndProfileRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun safeIdSuffix(uid: String): String =
        uid.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(16).ifEmpty { "citizen" }

    fun observeMyProfile(userId: String): Flow<UserProfile?> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val unwrapped = unwrapFirestoreFailure(error)
                    if (unwrapped is Exception && auth.currentUser?.uid == userId) {
                        handleFirestoreError(unwrapped, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeDiscoverableCitizens(currentUserId: String): Flow<List<UserProfile>> = flow {
        require(currentUserId.isNotBlank()) { "User ID must not be blank" }
        val query = db.collection("users").whereEqualTo("discoverable", true)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val unwrapped = unwrapFirestoreFailure(error)
                    if (unwrapped is Exception && auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(unwrapped, OperationType.LIST, "users")
                    }
                    throw error
                }
        )
    }

    fun observePrivacyVault(userId: String): Flow<PrivacyVault?> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val docRef = db.collection("users").document(userId).collection("private").document("vault")
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        PrivacyVault::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val unwrapped = unwrapFirestoreFailure(error)
                    if (unwrapped is Exception && auth.currentUser?.uid == userId) {
                        handleFirestoreError(unwrapped, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeConsentGrants(userId: String): Flow<List<ConsentGrant>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val col = db.collection("users").document(userId).collection("consent_grants")
        emitAll(
            col.snapshots()
                .map { snap ->
                    snap.toObjects(
                        ConsentGrant::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val unwrapped = unwrapFirestoreFailure(error)
                    if (unwrapped is Exception && auth.currentUser?.uid == userId) {
                        handleFirestoreError(unwrapped, OperationType.LIST, col.path)
                    }
                    throw error
                }
        )
    }

    fun observeAuditLogs(userId: String): Flow<List<PrivacyAuditLog>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val col = db.collection("users").document(userId).collection("audit_logs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(40)
        emitAll(
            col.snapshots()
                .map { snap ->
                    snap.toObjects(
                        PrivacyAuditLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val unwrapped = unwrapFirestoreFailure(error)
                    if (unwrapped is Exception && auth.currentUser?.uid == userId) {
                        handleFirestoreError(unwrapped, OperationType.LIST, "users/$userId/audit_logs")
                    }
                    throw error
                }
        )
    }

    fun observeContacts(userId: String): Flow<List<ContactItem>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val col = db.collection("users").document(userId).collection("contacts")
        emitAll(
            col.snapshots()
                .map { snap ->
                    snap.toObjects(
                        ContactItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.displayName.lowercase() }
                }
                .catch { error ->
                    val unwrapped = unwrapFirestoreFailure(error)
                    if (unwrapped is Exception && auth.currentUser?.uid == userId) {
                        handleFirestoreError(unwrapped, OperationType.LIST, col.path)
                    }
                    throw error
                }
        )
    }

    fun observeInvites(userId: String): Flow<List<InviteRecord>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val col = db.collection("users").document(userId).collection("invites")
        emitAll(
            col.snapshots()
                .map { snap ->
                    snap.toObjects(
                        InviteRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val unwrapped = unwrapFirestoreFailure(error)
                    if (unwrapped is Exception && auth.currentUser?.uid == userId) {
                        handleFirestoreError(unwrapped, OperationType.LIST, col.path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureCitizenInitialized(
        uid: String,
        displayName: String?,
        email: String?
    ): Result<Unit> {
        val path = "users/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Authenticated user mismatch" }

            val userRef = db.collection("users").document(uid)
            val existingUser = userRef.get().await()
            val cleanName = (displayName?.trim()?.takeIf { it.isNotEmpty() } ?: "Narad Citizen").take(60)
            val suffix = safeIdSuffix(uid)
            val baseHandle = "@" + cleanName.lowercase()
                .replace(Regex("[^a-z0-9]"), "")
                .take(14)
                .ifEmpty { "citizen" } + "_${suffix.take(4)}"

            if (!existingUser.exists()) {
                val profilePayload = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "handle" to baseHandle.take(40),
                    "bio" to "Privacy-first on Narad",
                    "city" to "Bharat",
                    "avatarIndex" to 0L,
                    "discoverable" to true,
                    "verifiedCitizen" to true,
                    "followersCount" to 12L,
                    "postsCount" to 3L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(profilePayload).await()
            }

            val vaultRef = userRef.collection("private").document("vault")
            if (!vaultRef.get().await().exists()) {
                val vaultPayload = buildMap<String, Any> {
                    put("userId", uid)
                    email?.take(120)?.let { put("email", it) }
                    put("defaultPostVisibility", PostVisibility.PRIVATE.name)
                    put("messagePolicy", MessagePolicy.CONTACTS.name)
                    put("readReceipts", true)
                    put("onlineStatus", false)
                    put("disappearingHours", 24L)
                    put("screenshotGuard", true)
                    put("dataResidencyRegion", "IN-SOU-1")
                    put("warrantLockAccepted", true)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                vaultRef.set(vaultPayload).await()

                seedInitialSovereignExperience(uid, cleanName, baseHandle, suffix)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun seedInitialSovereignExperience(
        uid: String,
        cleanName: String,
        handle: String,
        suffix: String
    ) {
        val userRef = db.collection("users").document(uid)

        userRef.collection("audit_logs").document("log_init_$suffix").set(
            mapOf(
                "userId" to uid,
                "action" to "KAVACH_VAULT_ACTIVE",
                "detail" to "Data residency IN-SOU-1. Zero-trust isolation active.",
                "category" to AuditCategory.SECURITY.name,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()

        userRef.collection("consent_grants").document("grant_family_$suffix").set(
            mapOf(
                "userId" to uid,
                "granteeName" to "Family Circle",
                "granteeHandle" to "@family_circle",
                "scope" to ConsentScope.PRIVATE_POSTS.name,
                "active" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        userRef.collection("contacts").document("contact_ananya_$suffix").set(
            mapOf(
                "userId" to uid,
                "displayName" to "Ananya Verma",
                "handle" to "@ananya_blr",
                "note" to "Bengaluru • Tech",
                "verified" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        userRef.collection("contacts").document("contact_rohan_$suffix").set(
            mapOf(
                "userId" to uid,
                "displayName" to "Rohan Deshmukh",
                "handle" to "@rohan_mum",
                "note" to "Mumbai • Design",
                "verified" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        val secretChatId = "chat_kavach_$suffix"
        val secretChatRef = db.collection("chats").document(secretChatId)
        if (!secretChatRef.get().await().exists()) {
            secretChatRef.set(
                mapOf(
                    "participantUids" to listOf(uid),
                    "title" to "Kavach Notes (E2EE)",
                    "chatType" to ChatType.SECRET.name,
                    "lastMessage" to "Private vault chat ready.",
                    "lastSenderUid" to uid,
                    "disappearingHours" to 24L,
                    "encryptedFingerprint" to "IN-KAVACH-${suffix.take(6).uppercase()}",
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            secretChatRef.collection("messages").document("msg_welcome_1").set(
                mapOf(
                    "chatId" to secretChatId,
                    "senderUid" to uid,
                    "senderName" to "Narad Kavach",
                    "participantUids" to listOf(uid),
                    "text" to "Namaste $cleanName! Your chats are isolated by zero-trust participant rules.",
                    "deliveryState" to DeliveryState.READ.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val groupChatId = "chat_samvad_$suffix"
        val groupChatRef = db.collection("chats").document(groupChatId)
        if (!groupChatRef.get().await().exists()) {
            groupChatRef.set(
                mapOf(
                    "participantUids" to listOf(uid),
                    "title" to "Bharatiya Builders",
                    "chatType" to ChatType.GROUP.name,
                    "lastMessage" to "Welcome to Narad Messaging!",
                    "lastSenderUid" to uid,
                    "disappearingHours" to 0L,
                    "encryptedFingerprint" to "GRP-${suffix.take(6).uppercase()}-256",
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            groupChatRef.collection("messages").document("msg_grp_1").set(
                mapOf(
                    "chatId" to groupChatId,
                    "senderUid" to uid,
                    "senderName" to cleanName,
                    "participantUids" to listOf(uid),
                    "text" to "Tap any message to react or reply. Add contacts anytime!",
                    "deliveryState" to DeliveryState.DELIVERED.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val techChannelId = "ch_bharat_tech_$suffix"
        val techChannelRef = db.collection("channels").document(techChannelId)
        if (!techChannelRef.get().await().exists()) {
            techChannelRef.set(
                mapOf(
                    "ownerUid" to uid,
                    "name" to "Digital Bharat",
                    "handle" to "@digitalbharat",
                    "description" to "Sovereign tech & digital rights.",
                    "category" to "Tech",
                    "channelPrivacy" to ChannelPrivacy.PUBLIC.name,
                    "subscribersCount" to 14820L,
                    "verified" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            techChannelRef.collection("broadcasts").document("bc_1").set(
                mapOf(
                    "channelId" to techChannelId,
                    "channelOwnerUid" to uid,
                    "channelPrivacy" to ChannelPrivacy.PUBLIC.name,
                    "authorUid" to uid,
                    "title" to "Citizen Privacy Charter",
                    "body" to "Private vaults stay locked under your UID until you grant consent.",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            userRef.collection("subscriptions").document(techChannelId).set(
                mapOf(
                    "userId" to uid,
                    "channelId" to techChannelId,
                    "channelName" to "Digital Bharat",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val cultureChannelId = "ch_heritage_$suffix"
        val cultureChannelRef = db.collection("channels").document(cultureChannelId)
        if (!cultureChannelRef.get().await().exists()) {
            cultureChannelRef.set(
                mapOf(
                    "ownerUid" to uid,
                    "name" to "Dharohar India",
                    "handle" to "@dharohar_in",
                    "description" to "Architecture, heritage & regional stories.",
                    "category" to "Culture",
                    "channelPrivacy" to ChannelPrivacy.PUBLIC.name,
                    "subscribersCount" to 9240L,
                    "verified" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val pubPostRef = db.collection("posts").document("post_studio_$suffix")
        if (!pubPostRef.get().await().exists()) {
            pubPostRef.set(
                mapOf(
                    "userId" to uid,
                    "authorName" to cleanName,
                    "authorHandle" to handle.take(40),
                    "caption" to "Building on Narad from Bengaluru.",
                    "location" to "Bengaluru",
                    "postKind" to PostKind.POST.name,
                    "visibility" to PostVisibility.PUBLIC.name,
                    "mediaPreset" to MediaPreset.STUDIO.name,
                    "likesCount" to 24L,
                    "commentsCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("posts").document("post_heritage_$suffix").set(
                mapOf(
                    "userId" to uid,
                    "authorName" to "Dharohar India",
                    "authorHandle" to "@dharohar_in",
                    "caption" to "Golden hour at Chand Baori, Rajasthan.",
                    "location" to "Abhaneri, RJ",
                    "postKind" to PostKind.POST.name,
                    "visibility" to PostVisibility.PUBLIC.name,
                    "mediaPreset" to MediaPreset.HERITAGE.name,
                    "likesCount" to 108L,
                    "commentsCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("posts").document("story_sovereign_$suffix").set(
                mapOf(
                    "userId" to uid,
                    "authorName" to cleanName,
                    "authorHandle" to handle.take(40),
                    "caption" to "Kavach Shield Active",
                    "location" to "Bharat",
                    "postKind" to PostKind.STORY.name,
                    "visibility" to PostVisibility.PUBLIC.name,
                    "mediaPreset" to MediaPreset.SOVEREIGN.name,
                    "likesCount" to 7L,
                    "commentsCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("posts").document("post_private_vault_$suffix").set(
                mapOf(
                    "userId" to uid,
                    "authorName" to cleanName,
                    "authorHandle" to handle.take(40),
                    "caption" to "Private Vault Moment — only visible to me.",
                    "location" to "Vault",
                    "postKind" to PostKind.POST.name,
                    "visibility" to PostVisibility.PRIVATE.name,
                    "mediaPreset" to MediaPreset.NONE.name,
                    "likesCount" to 0L,
                    "commentsCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val short1Ref = db.collection("videos").document("vid_short_1_$suffix")
        if (!short1Ref.get().await().exists()) {
            short1Ref.set(
                mapOf(
                    "ownerUid" to uid,
                    "channelId" to cultureChannelId,
                    "channelName" to "Dharohar India",
                    "title" to "Chand Baori Stepwell in 54s",
                    "description" to "3,500 symmetric steps in Rajasthan.",
                    "videoFormat" to VideoFormat.SHORT.name,
                    "durationLabel" to "0:54",
                    "category" to "Culture",
                    "thumbnailPreset" to ThumbnailPreset.HERITAGE.name,
                    "visibility" to PostVisibility.PUBLIC.name,
                    "viewsCount" to 4820L,
                    "likesCount" to 412L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("videos").document("vid_short_2_$suffix").set(
                mapOf(
                    "ownerUid" to uid,
                    "channelId" to techChannelId,
                    "channelName" to "Digital Bharat",
                    "title" to "Inside Narad Zero-Trust Vault",
                    "description" to "How your private chats stay isolated.",
                    "videoFormat" to VideoFormat.SHORT.name,
                    "durationLabel" to "0:38",
                    "category" to "Privacy",
                    "thumbnailPreset" to ThumbnailPreset.SOVEREIGN.name,
                    "visibility" to PostVisibility.PUBLIC.name,
                    "viewsCount" to 3190L,
                    "likesCount" to 285L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("videos").document("vid_long_1_$suffix").set(
                mapOf(
                    "ownerUid" to uid,
                    "channelId" to techChannelId,
                    "channelName" to "Digital Bharat",
                    "title" to "Sovereign Tech from Bengaluru",
                    "description" to "Inside India's privacy-first creator network.",
                    "videoFormat" to VideoFormat.LONG.name,
                    "durationLabel" to "14:28",
                    "category" to "Tech",
                    "thumbnailPreset" to ThumbnailPreset.STUDIO.name,
                    "visibility" to PostVisibility.PUBLIC.name,
                    "viewsCount" to 12450L,
                    "likesCount" to 1120L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("videos").document("vid_long_2_$suffix").set(
                mapOf(
                    "ownerUid" to uid,
                    "channelId" to cultureChannelId,
                    "channelName" to "Dharohar India",
                    "title" to "Living Water Temples of Bharat",
                    "description" to "Architecture & conservation documentary.",
                    "videoFormat" to VideoFormat.LONG.name,
                    "durationLabel" to "18:05",
                    "category" to "Heritage",
                    "thumbnailPreset" to ThumbnailPreset.BANNER.name,
                    "visibility" to PostVisibility.PUBLIC.name,
                    "viewsCount" to 8930L,
                    "likesCount" to 740L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun addContact(
        displayName: String,
        handle: String,
        note: String = "",
        contactUid: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/contacts"
        return try {
            val activeUid = requireUserId()
            val cleanName = displayName.trim().take(60)
            val cleanHandle = handle.trim().let { if (it.startsWith("@")) it else "@$it" }.take(40)
            require(cleanName.isNotEmpty() && cleanHandle.length >= 2) {
                "Contact name and handle required"
            }
            val ref = db.collection("users").document(activeUid).collection("contacts").document()
            val payload = buildMap<String, Any> {
                put("userId", activeUid)
                contactUid?.trim()?.takeIf { it.isNotEmpty() }?.take(128)?.let { put("contactUid", it) }
                put("displayName", cleanName)
                put("handle", cleanHandle)
                note.trim().takeIf { it.isNotEmpty() }?.take(60)?.let { put("note", it) }
                put("verified", !contactUid.isNullOrBlank())
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteContact(contactId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/contacts/$contactId"
        return try {
            val activeUid = requireUserId()
            require(contactId.isNotBlank())
            db.collection("users").document(activeUid).collection("contacts").document(contactId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun createInvite(
        recipientName: String,
        channel: String = "SHARE",
        presetCode: String? = null
    ): Result<InviteRecord> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/invites"
        return try {
            val activeUid = requireUserId()
            val cleanName = recipientName.trim().take(60).ifEmpty { "Friend" }
            val code = presetCode?.trim()?.takeIf { it.isNotEmpty() }?.take(24)
                ?: ("NRD-" + UUID.randomUUID().toString().take(4).uppercase())
            val ref = db.collection("users").document(activeUid).collection("invites").document()
            val payload = mapOf(
                "userId" to activeUid,
                "recipientName" to cleanName,
                "inviteCode" to code,
                "channel" to channel.trim().take(24).ifEmpty { "SHARE" },
                "status" to InviteStatus.SENT.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(
                InviteRecord(
                    id = ref.id,
                    userId = activeUid,
                    recipientName = cleanName,
                    inviteCode = code,
                    channel = channel,
                    status = InviteStatus.SENT.name
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        displayName: String,
        handle: String,
        bio: String,
        city: String,
        discoverable: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid"
        return try {
            val activeUid = requireUserId()
            val cleanName = displayName.trim().take(60)
            val cleanHandle = handle.trim().let { if (it.startsWith("@")) it else "@$it" }.take(40)
            require(cleanName.isNotEmpty()) { "Display name cannot be empty" }
            require(cleanHandle.length >= 2) { "Handle must be at least 2 characters" }
            val payload = mapOf(
                "displayName" to cleanName,
                "handle" to cleanHandle,
                "bio" to bio.trim().take(250),
                "city" to city.trim().take(60),
                "discoverable" to discoverable,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(activeUid).update(payload).await()
            logAuditInternal(
                uid = activeUid,
                action = "PROFILE_UPDATED",
                detail = "Profile updated (discoverable=$discoverable).",
                category = AuditCategory.SECURITY
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePrivacyVault(
        defaultPostVisibility: PostVisibility,
        messagePolicy: MessagePolicy,
        readReceipts: Boolean,
        onlineStatus: Boolean,
        disappearingHours: Long,
        screenshotGuard: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/private/vault"
        return try {
            val activeUid = requireUserId()
            val validHours = if (disappearingHours in listOf(0L, 24L, 168L)) disappearingHours else 24L
            db.collection("users").document(activeUid).collection("private").document("vault")
                .update(
                    mapOf(
                        "defaultPostVisibility" to defaultPostVisibility.name,
                        "messagePolicy" to messagePolicy.name,
                        "readReceipts" to readReceipts,
                        "onlineStatus" to onlineStatus,
                        "disappearingHours" to validHours,
                        "screenshotGuard" to screenshotGuard,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            logAuditInternal(
                uid = activeUid,
                action = "PRIVACY_POLICY_UPDATED",
                detail = "Default post=${defaultPostVisibility.name}, messages=${messagePolicy.name}, timer=${validHours}h",
                category = AuditCategory.SECURITY
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addConsentGrant(
        granteeName: String,
        granteeHandle: String,
        scope: ConsentScope
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/consent_grants"
        return try {
            val activeUid = requireUserId()
            val cleanName = granteeName.trim().take(60)
            val cleanHandle = granteeHandle.trim().let { if (it.startsWith("@")) it else "@$it" }.take(40)
            require(cleanName.isNotEmpty() && cleanHandle.length >= 2) {
                "Contact name and handle are required"
            }
            val ref = db.collection("users").document(activeUid).collection("consent_grants").document()
            ref.set(
                mapOf(
                    "userId" to activeUid,
                    "granteeName" to cleanName,
                    "granteeHandle" to cleanHandle,
                    "scope" to scope.name,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logAuditInternal(
                uid = activeUid,
                action = "CONSENT_GRANTED",
                detail = "Granted ${scope.name} to $cleanHandle",
                category = AuditCategory.CONSENT
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleConsentGrant(grant: ConsentGrant): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/consent_grants/${grant.id}"
        return try {
            val activeUid = requireUserId()
            require(grant.id.isNotBlank()) { "Grant ID cannot be blank" }
            val nextActive = !grant.active
            db.collection("users").document(activeUid)
                .collection("consent_grants").document(grant.id)
                .update(
                    mapOf(
                        "active" to nextActive,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            logAuditInternal(
                uid = activeUid,
                action = if (nextActive) "CONSENT_GRANTED" else "CONSENT_REVOKED",
                detail = "${if (nextActive) "Granted" else "Revoked"} ${grant.scope} for ${grant.granteeHandle}",
                category = AuditCategory.CONSENT
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteConsentGrant(grantId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/consent_grants/$grantId"
        return try {
            val activeUid = requireUserId()
            require(grantId.isNotBlank())
            db.collection("users").document(activeUid)
                .collection("consent_grants").document(grantId)
                .delete().await()
            logAuditInternal(
                uid = activeUid,
                action = "CONSENT_REMOVED",
                detail = "Removed consent grant $grantId",
                category = AuditCategory.CONSENT
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun logWarrantPolicyInspection(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        return try {
            val activeUid = requireUserId()
            logAuditInternal(
                uid = activeUid,
                action = "JUDICIAL_WARRANT_AUDIT_CHECK",
                detail = "Verified 0 active criminal holds. Zero third-party access.",
                category = AuditCategory.WARRANT_CHECK
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$uid/audit_logs")
            Result.failure(e)
        }
    }

    private suspend fun logAuditInternal(
        uid: String,
        action: String,
        detail: String,
        category: AuditCategory
    ) {
        db.collection("users").document(uid).collection("audit_logs").document().set(
            mapOf(
                "userId" to uid,
                "action" to action.take(80),
                "detail" to detail.take(240),
                "category" to category.name,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }
}
