package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PostVisibility { PUBLIC, PRIVATE }
enum class MessagePolicy { CONTACTS, EVERYONE }
enum class ConsentScope { PRIVATE_POSTS, PROFILE_DETAILS, DIRECT_MEDIA }
enum class AuditCategory { CONSENT, ENCRYPTION, SECURITY, WARRANT_CHECK }
enum class ChatType { DIRECT, GROUP, SECRET }
enum class DeliveryState { SENT, DELIVERED, READ }
enum class PostKind { POST, STORY }
enum class MediaPreset { NONE, HERITAGE, SOVEREIGN, STUDIO }
enum class ChannelPrivacy { PUBLIC, INVITE_ONLY }
enum class VideoFormat { SHORT, LONG }
enum class ThumbnailPreset { HERITAGE, SOVEREIGN, STUDIO, BANNER }
enum class InviteStatus { SENT, JOINED }

inline fun <reified T : Enum<T>> safeEnumValue(raw: String?, default: T): T {
    if (raw.isNullOrBlank()) return default
    return enumValues<T>().firstOrNull { it.name == raw } ?: default
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val handle: String = "",
    val bio: String = "",
    val city: String = "",
    val avatarIndex: Long = 0L,
    val discoverable: Boolean = true,
    val verifiedCitizen: Boolean = true,
    val followersCount: Long = 0L,
    val postsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivacyVault(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val defaultPostVisibility: String = PostVisibility.PRIVATE.name,
    val messagePolicy: String = MessagePolicy.CONTACTS.name,
    val readReceipts: Boolean = true,
    val onlineStatus: Boolean = false,
    val disappearingHours: Long = 24L,
    val screenshotGuard: Boolean = true,
    val dataResidencyRegion: String = "IN-SOU-1",
    val warrantLockAccepted: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ConsentGrant(
    @DocumentId val id: String = "",
    val userId: String = "",
    val granteeName: String = "",
    val granteeHandle: String = "",
    val scope: String = ConsentScope.PRIVATE_POSTS.name,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivacyAuditLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val action: String = "",
    val detail: String = "",
    val category: String = AuditCategory.SECURITY.name,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserSubscription(
    @DocumentId val id: String = "",
    val userId: String = "",
    val channelId: String = "",
    val channelName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ContactItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val contactUid: String = "",
    val displayName: String = "",
    val handle: String = "",
    val note: String = "",
    val verified: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class InviteRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val recipientName: String = "",
    val inviteCode: String = "",
    val channel: String = "SHARE",
    val status: String = InviteStatus.SENT.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val participantUids: List<String> = emptyList(),
    val title: String = "",
    val chatType: String = ChatType.DIRECT.name,
    val lastMessage: String = "",
    val lastSenderUid: String = "",
    val disappearingHours: Long = 0L,
    val encryptedFingerprint: String = "",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val participantUids: List<String> = emptyList(),
    val text: String = "",
    val replyToText: String = "",
    val mediaTag: String = "",
    val reaction: String = "",
    val deliveryState: String = DeliveryState.SENT.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SocialPost(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val caption: String = "",
    val location: String = "",
    val postKind: String = PostKind.POST.name,
    val visibility: String = PostVisibility.PUBLIC.name,
    val mediaPreset: String = MediaPreset.NONE.name,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Channel(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val handle: String = "",
    val description: String = "",
    val category: String = "",
    val channelPrivacy: String = ChannelPrivacy.PUBLIC.name,
    val subscribersCount: Long = 0L,
    val verified: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChannelBroadcast(
    @DocumentId val id: String = "",
    val channelId: String = "",
    val channelOwnerUid: String = "",
    val channelPrivacy: String = ChannelPrivacy.PUBLIC.name,
    val authorUid: String = "",
    val title: String = "",
    val body: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VideoItem(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val channelId: String = "",
    val channelName: String = "",
    val title: String = "",
    val description: String = "",
    val videoFormat: String = VideoFormat.SHORT.name,
    val durationLabel: String = "0:45",
    val category: String = "Technology",
    val thumbnailPreset: String = ThumbnailPreset.SOVEREIGN.name,
    val visibility: String = PostVisibility.PUBLIC.name,
    val viewsCount: Long = 0L,
    val likesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommentItem(
    @DocumentId val id: String = "",
    val parentId: String = "",
    val parentVisibility: String = PostVisibility.PUBLIC.name,
    val parentOwnerUid: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
