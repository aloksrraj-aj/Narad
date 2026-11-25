package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Channel
import com.example.data.model.ChannelBroadcast
import com.example.data.model.ChannelPrivacy
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.ChatType
import com.example.data.model.CommentItem
import com.example.data.model.DeliveryState
import com.example.data.model.MediaPreset
import com.example.data.model.PostKind
import com.example.data.model.PostVisibility
import com.example.data.model.SocialPost
import com.example.data.model.ThumbnailPreset
import com.example.data.model.UserSubscription
import com.example.data.model.VideoFormat
import com.example.data.model.VideoItem
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.data.remote.unwrapFirestoreFailure
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SocialAndMessagingRepository(
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

    // --- WHATSAPP & TELEGRAM CHATS ---

    fun observeChats(userId: String): Flow<List<ChatThread>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val query = db.collection("chats").whereArrayContains("participantUids", userId)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(
                        ChatThread::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val unwrapped = unwrapFirestoreFailure(error)
                    if (unwrapped is Exception && auth.currentUser?.uid == userId) {
                        handleFirestoreError(unwrapped, OperationType.LIST, "chats")
                    }
                    throw error
                }
        )
    }

    fun observeMessages(chatId: String, currentUserId: String): Flow<List<ChatMessage>> = flow {
        require(chatId.isNotBlank() && currentUserId.isNotBlank())
        val query = db.collection("chats").document(chatId).collection("messages")
            .whereArrayContains("participantUids", currentUserId)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val unwrapped = unwrapFirestoreFailure(error)
                    if (unwrapped is Exception && auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(unwrapped, OperationType.LIST, "chats/$chatId/messages")
                    }
                    throw error
                }
        )
    }

    suspend fun createChat(
        title: String,
        chatType: ChatType,
        disappearingHours: Long,
        otherParticipantUid: String? = null
    ): Result<String> {
        val path = "chats"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(80)
            require(cleanTitle.isNotEmpty()) { "Chat title cannot be empty" }
            val participants = buildList {
                add(uid)
                if (!otherParticipantUid.isNullOrBlank() && otherParticipantUid != uid) {
                    add(otherParticipantUid)
                }
            }.distinct().take(20)
            val validHours = if (disappearingHours in listOf(0L, 24L, 168L)) disappearingHours else 0L
            val fingerprint = "E2EE-" + UUID.randomUUID().toString().take(8).uppercase()
            val docRef = db.collection("chats").document()
            docRef.set(
                mapOf(
                    "participantUids" to participants,
                    "title" to cleanTitle,
                    "chatType" to chatType.name,
                    "lastMessage" to "Encrypted channel initialized.",
                    "lastSenderUid" to uid,
                    "disappearingHours" to validHours,
                    "encryptedFingerprint" to fingerprint,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        chat: ChatThread,
        senderName: String,
        text: String,
        replyToText: String? = null,
        mediaTag: String? = null
    ): Result<String> {
        val path = "chats/${chat.id}/messages"
        return try {
            val uid = requireUserId()
            require(chat.id.isNotBlank()) { "Chat ID cannot be blank" }
            val cleanText = text.trim().take(1000)
            require(cleanText.isNotEmpty()) { "Message cannot be empty" }
            val participants = chat.participantUids.ifEmpty { listOf(uid) }
            val msgRef = db.collection("chats").document(chat.id).collection("messages").document()
            val payload = buildMap<String, Any> {
                put("chatId", chat.id)
                put("senderUid", uid)
                put("senderName", senderName.trim().ifEmpty { "Citizen" }.take(60))
                put("participantUids", participants)
                put("text", cleanText)
                replyToText?.trim()?.takeIf { it.isNotEmpty() }?.take(200)?.let { put("replyToText", it) }
                mediaTag?.trim()?.takeIf { it.isNotEmpty() }?.take(80)?.let { put("mediaTag", it) }
                put("deliveryState", DeliveryState.READ.name)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            msgRef.set(payload).await()

            db.collection("chats").document(chat.id).update(
                mapOf(
                    "lastMessage" to cleanText.take(300),
                    "lastSenderUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(msgRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun reactToMessage(chatId: String, messageId: String, emoji: String): Result<Unit> {
        val path = "chats/$chatId/messages/$messageId"
        return try {
            requireUserId()
            require(chatId.isNotBlank() && messageId.isNotBlank())
            db.collection("chats").document(chatId).collection("messages").document(messageId)
                .update(
                    mapOf(
                        "reaction" to emoji.trim().take(16),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateChatDisappearingTimer(chatId: String, hours: Long): Result<Unit> {
        val path = "chats/$chatId"
        return try {
            requireUserId()
            val validHours = if (hours in listOf(0L, 24L, 168L)) hours else 0L
            db.collection("chats").document(chatId).update(
                mapOf(
                    "disappearingHours" to validHours,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- FACEBOOK / INSTAGRAM FEED & STORIES ---

    fun observePublicPosts(currentUserId: String): Flow<List<SocialPost>> = flow {
        require(currentUserId.isNotBlank())
        val query = db.collection("posts").whereEqualTo("visibility", PostVisibility.PUBLIC.name)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(
                        SocialPost::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val unwrapped = unwrapFirestoreFailure(error)
                    if (unwrapped is Exception && auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(unwrapped, OperationType.LIST, "posts")
                    }
                    throw error
                }
        )
    }

    fun observeMyPosts(userId: String): Flow<List<SocialPost>> = flow {
        require(userId.isNotBlank())
        val query = db.collection("posts").whereEqualTo("userId", userId)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(
                        SocialPost::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val unwrapped = unwrapFirestoreFailure(error)
                    if (unwrapped is Exception && auth.currentUser?.uid == userId) {
                        handleFirestoreError(unwrapped, OperationType.LIST, "posts")
                    }
                    throw error
                }
        )
    }

    suspend fun createPost(
        authorName: String,
        authorHandle: String,
        caption: String,
        location: String,
        postKind: PostKind,
        visibility: PostVisibility,
        mediaPreset: MediaPreset
    ): Result<String> {
        val path = "posts"
        return try {
            val uid = requireUserId()
            val cleanCaption = caption.trim().take(1000)
            require(cleanCaption.isNotEmpty()) { "Caption cannot be empty" }
            val normalizedHandle = authorHandle.trim()
                .let { if (it.startsWith("@")) it else "@$it" }
                .let { if (it.length < 2) "@citizen" else it }
                .take(40)
            val ref = db.collection("posts").document()
            val payload = buildMap<String, Any> {
                put("userId", uid)
                put("authorName", authorName.trim().ifEmpty { "Narad Citizen" }.take(60))
                put("authorHandle", normalizedHandle)
                put("caption", cleanCaption)
                location.trim().takeIf { it.isNotEmpty() }?.take(80)?.let { put("location", it) }
                put("postKind", postKind.name)
                put("visibility", visibility.name)
                put("mediaPreset", mediaPreset.name)
                put("likesCount", 0L)
                put("commentsCount", 0L)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun togglePostVisibility(post: SocialPost): Result<Unit> {
        val path = "posts/${post.id}"
        return try {
            val uid = requireUserId()
            check(post.userId == uid) { "Only the owner can change visibility" }
            val nextVis = if (post.visibility == PostVisibility.PUBLIC.name) {
                PostVisibility.PRIVATE.name
            } else {
                PostVisibility.PUBLIC.name
            }
            db.collection("posts").document(post.id).update(
                mapOf(
                    "visibility" to nextVis,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun likePost(postId: String): Result<Unit> {
        val path = "posts/$postId"
        return try {
            requireUserId()
            require(postId.isNotBlank())
            db.collection("posts").document(postId).update(
                mapOf(
                    "likesCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        val path = "posts/$postId"
        return try {
            requireUserId()
            require(postId.isNotBlank())
            db.collection("posts").document(postId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observePostComments(post: SocialPost, currentUserId: String): Flow<List<CommentItem>> = flow {
        require(post.id.isNotBlank() && currentUserId.isNotBlank())
        val base = db.collection("posts").document(post.id).collection("comments")
        val query = if (post.visibility == PostVisibility.PUBLIC.name) {
            base.whereEqualTo("parentVisibility", PostVisibility.PUBLIC.name)
        } else {
            base.whereEqualTo("parentOwnerUid", currentUserId)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(
                        CommentItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val unwrapped = unwrapFirestoreFailure(error)
                    if (unwrapped is Exception && auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(unwrapped, OperationType.LIST, "posts/${post.id}/comments")
                    }
                    throw error
                }
        )
    }

    suspend fun addPostComment(post: SocialPost, authorName: String, text: String): Result<Unit> {
        val path = "posts/${post.id}/comments"
        return try {
            val uid = requireUserId()
            val clean = text.trim().take(400)
            require(clean.isNotEmpty()) { "Comment cannot be empty" }
            db.collection("posts").document(post.id).collection("comments").document().set(
                mapOf(
                    "parentId" to post.id,
                    "parentVisibility" to post.visibility,
                    "parentOwnerUid" to post.userId,
                    "authorUid" to uid,
                    "authorName" to authorName.trim().ifEmpty { "Citizen" }.take(60),
                    "text" to clean,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("posts").document(post.id).update(
                mapOf(
                    "commentsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- CHANNELS (YOUTUBE + TELEGRAM) ---

    fun observePublicChannels(currentUserId: String): Flow<List<Channel>> = flow {
        require(currentUserId.isNotBlank())
        val query = db.collection("channels")
            .whereEqualTo("channelPrivacy", ChannelPrivacy.PUBLIC.name)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(
                        Channel::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.subscribersCount }
                }
                .catch { error ->
                    val unwrapped = unwrapFirestoreFailure(error)
                    if (unwrapped is Exception && auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(unwrapped, OperationType.LIST, "channels")
                    }
                    throw error
                }
        )
    }

    fun observeMyChannels(userId: String): Flow<List<Channel>> = flow {
        require(userId.isNotBlank())
        val query = db.collection("channels").whereEqualTo("ownerUid", userId)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(
                        Channel::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val unwrapped = unwrapFirestoreFailure(error)
                    if (unwrapped is Exception && auth.currentUser?.uid == userId) {
                        handleFirestoreError(unwrapped, OperationType.LIST, "channels")
                    }
                    throw error
                }
        )
    }

    fun observeSubscriptions(userId: String): Flow<List<UserSubscription>> = flow {
        require(userId.isNotBlank())
        val col = db.collection("users").document(userId).collection("subscriptions")
        emitAll(
            col.snapshots()
                .map { snap ->
                    snap.toObjects(
                        UserSubscription::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
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

    fun observeBroadcasts(channel: Channel, currentUserId: String): Flow<List<ChannelBroadcast>> = flow {
        require(channel.id.isNotBlank() && currentUserId.isNotBlank())
        val base = db.collection("channels").document(channel.id).collection("broadcasts")
        val query = if (channel.channelPrivacy == ChannelPrivacy.PUBLIC.name) {
            base.whereEqualTo("channelPrivacy", ChannelPrivacy.PUBLIC.name)
        } else {
            base.whereEqualTo("channelOwnerUid", currentUserId)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(
                        ChannelBroadcast::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val unwrapped = unwrapFirestoreFailure(error)
                    if (unwrapped is Exception && auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(unwrapped, OperationType.LIST, "channels/${channel.id}/broadcasts")
                    }
                    throw error
                }
        )
    }

    suspend fun createChannel(
        name: String,
        handle: String,
        description: String,
        category: String,
        privacy: ChannelPrivacy
    ): Result<Unit> {
        val path = "channels"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(60)
            val rawHandle = handle.trim().let { if (it.startsWith("@")) it else "@$it" }
            val cleanHandle = if (rawHandle.length < 2) {
                "@" + cleanName.lowercase().replace(Regex("[^a-z0-9]"), "").ifEmpty { "channel" }
            } else {
                rawHandle
            }.take(40)
            val cleanDesc = description.trim().ifEmpty { "Official Narad Vani channel" }.take(300)
            require(cleanName.length >= 2 && cleanHandle.length >= 2 && cleanDesc.isNotEmpty()) {
                "Channel name, handle, and description are required"
            }
            val ref = db.collection("channels").document()
            ref.set(
                mapOf(
                    "ownerUid" to uid,
                    "name" to cleanName,
                    "handle" to cleanHandle,
                    "description" to cleanDesc,
                    "category" to category.trim().ifEmpty { "General" }.take(40),
                    "channelPrivacy" to privacy.name,
                    "subscribersCount" to 1L,
                    "verified" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("users").document(uid).collection("subscriptions").document(ref.id).set(
                mapOf(
                    "userId" to uid,
                    "channelId" to ref.id,
                    "channelName" to cleanName,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleSubscription(channel: Channel, isSubscribed: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/subscriptions/${channel.id}"
        return try {
            val activeUid = requireUserId()
            val subRef = db.collection("users").document(activeUid)
                .collection("subscriptions").document(channel.id)
            val chanRef = db.collection("channels").document(channel.id)
            if (isSubscribed) {
                subRef.delete().await()
                if (channel.subscribersCount > 0L) {
                    chanRef.update(
                        mapOf(
                            "subscribersCount" to FieldValue.increment(-1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } else {
                subRef.set(
                    mapOf(
                        "userId" to activeUid,
                        "channelId" to channel.id,
                        "channelName" to channel.name.take(60),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                chanRef.update(
                    mapOf(
                        "subscribersCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun publishBroadcast(channel: Channel, title: String, body: String): Result<Unit> {
        val path = "channels/${channel.id}/broadcasts"
        return try {
            val uid = requireUserId()
            check(channel.ownerUid == uid) { "Only the channel owner can broadcast" }
            val cleanTitle = title.trim().take(100)
            val cleanBody = body.trim().take(1000)
            require(cleanTitle.isNotEmpty() && cleanBody.isNotEmpty()) {
                "Broadcast title and message are required"
            }
            db.collection("channels").document(channel.id).collection("broadcasts").document().set(
                mapOf(
                    "channelId" to channel.id,
                    "channelOwnerUid" to channel.ownerUid,
                    "channelPrivacy" to channel.channelPrivacy,
                    "authorUid" to uid,
                    "title" to cleanTitle,
                    "body" to cleanBody,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- VIDEOS (YOUTUBE SHORTS & LONG VIDEOS) ---

    fun observePublicVideos(currentUserId: String): Flow<List<VideoItem>> = flow {
        require(currentUserId.isNotBlank())
        val query = db.collection("videos").whereEqualTo("visibility", PostVisibility.PUBLIC.name)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(
                        VideoItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val unwrapped = unwrapFirestoreFailure(error)
                    if (unwrapped is Exception && auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(unwrapped, OperationType.LIST, "videos")
                    }
                    throw error
                }
        )
    }

    fun observeMyVideos(userId: String): Flow<List<VideoItem>> = flow {
        require(userId.isNotBlank())
        val query = db.collection("videos").whereEqualTo("ownerUid", userId)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(
                        VideoItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val unwrapped = unwrapFirestoreFailure(error)
                    if (unwrapped is Exception && auth.currentUser?.uid == userId) {
                        handleFirestoreError(unwrapped, OperationType.LIST, "videos")
                    }
                    throw error
                }
        )
    }

    suspend fun createVideo(
        channelId: String,
        channelName: String,
        title: String,
        description: String,
        format: VideoFormat,
        durationLabel: String,
        category: String,
        thumbnailPreset: ThumbnailPreset,
        visibility: PostVisibility
    ): Result<Unit> {
        val path = "videos"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.length >= 2) { "Video title must be at least 2 characters" }
            db.collection("videos").document().set(
                mapOf(
                    "ownerUid" to uid,
                    "channelId" to channelId.ifBlank { "personal_$uid" }.take(128),
                    "channelName" to channelName.trim().ifEmpty { "My Narad Studio" }.take(60),
                    "title" to cleanTitle,
                    "description" to description.trim().take(500),
                    "videoFormat" to format.name,
                    "durationLabel" to durationLabel.trim().ifEmpty { if (format == VideoFormat.SHORT) "0:45" else "10:15" }.take(16),
                    "category" to category.trim().ifEmpty { "Creator" }.take(40),
                    "thumbnailPreset" to thumbnailPreset.name,
                    "visibility" to visibility.name,
                    "viewsCount" to 1L,
                    "likesCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun likeVideo(videoId: String): Result<Unit> {
        val path = "videos/$videoId"
        return try {
            requireUserId()
            db.collection("videos").document(videoId).update(
                mapOf(
                    "likesCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun incrementVideoView(videoId: String): Result<Unit> {
        val path = "videos/$videoId"
        return try {
            requireUserId()
            db.collection("videos").document(videoId).update(
                mapOf(
                    "viewsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeVideoComments(video: VideoItem, currentUserId: String): Flow<List<CommentItem>> = flow {
        require(video.id.isNotBlank() && currentUserId.isNotBlank())
        val base = db.collection("videos").document(video.id).collection("comments")
        val query = if (video.visibility == PostVisibility.PUBLIC.name) {
            base.whereEqualTo("parentVisibility", PostVisibility.PUBLIC.name)
        } else {
            base.whereEqualTo("parentOwnerUid", currentUserId)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(
                        CommentItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val unwrapped = unwrapFirestoreFailure(error)
                    if (unwrapped is Exception && auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(unwrapped, OperationType.LIST, "videos/${video.id}/comments")
                    }
                    throw error
                }
        )
    }

    suspend fun addVideoComment(video: VideoItem, authorName: String, text: String): Result<Unit> {
        val path = "videos/${video.id}/comments"
        return try {
            val uid = requireUserId()
            val clean = text.trim().take(400)
            require(clean.isNotEmpty()) { "Comment cannot be empty" }
            db.collection("videos").document(video.id).collection("comments").document().set(
                mapOf(
                    "parentId" to video.id,
                    "parentVisibility" to video.visibility,
                    "parentOwnerUid" to video.ownerUid,
                    "authorUid" to uid,
                    "authorName" to authorName.trim().ifEmpty { "Citizen" }.take(60),
                    "text" to clean,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
