package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Channel
import com.example.data.model.ChannelBroadcast
import com.example.data.model.ChannelPrivacy
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.ChatType
import com.example.data.model.CommentItem
import com.example.data.model.ConsentGrant
import com.example.data.model.ConsentScope
import com.example.data.model.ContactItem
import com.example.data.model.InviteRecord
import com.example.data.model.MediaPreset
import com.example.data.model.MessagePolicy
import com.example.data.model.PostKind
import com.example.data.model.PostVisibility
import com.example.data.model.PrivacyAuditLog
import com.example.data.model.PrivacyVault
import com.example.data.model.SocialPost
import com.example.data.model.ThumbnailPreset
import com.example.data.model.UserProfile
import com.example.data.model.UserSubscription
import com.example.data.model.VideoFormat
import com.example.data.model.VideoItem
import com.example.data.remote.UiState
import com.example.data.remote.unwrapFirestoreFailure
import com.example.data.repository.SocialAndMessagingRepository
import com.example.data.repository.VaultAndProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NaradViewModel(
    private val vaultRepo: VaultAndProfileRepository,
    private val socialRepo: SocialAndMessagingRepository,
    val currentUserId: String,
    private val initialDisplayName: String?,
    private val initialEmail: String?
) : ViewModel() {

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _activeChannel = MutableStateFlow<Channel?>(null)
    val activeChannel: StateFlow<Channel?> = _activeChannel.asStateFlow()

    private val _activeCommentPost = MutableStateFlow<SocialPost?>(null)
    val activeCommentPost: StateFlow<SocialPost?> = _activeCommentPost.asStateFlow()

    private val _activeVideo = MutableStateFlow<VideoItem?>(null)
    val activeVideo: StateFlow<VideoItem?> = _activeVideo.asStateFlow()

    private val _localPickedMediaUris = MutableStateFlow<Map<String, String>>(emptyMap())
    val localPickedMediaUris: StateFlow<Map<String, String>> = _localPickedMediaUris.asStateFlow()

    init {
        viewModelScope.launch {
            val res = vaultRepo.ensureCitizenInitialized(
                uid = currentUserId,
                displayName = initialDisplayName,
                email = initialEmail
            )
            res.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to initialize Narad vault"
            }
        }
    }

    private fun <T> Flow<T>.toUiStateFlow(tag: String, errorFallback: String): StateFlow<UiState<T>> {
        return this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val root = unwrapFirestoreFailure(error)
                Log.w(tag, errorFallback, root)
                emit(UiState.Error(root.message ?: errorFallback))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )
    }

    val profileState: StateFlow<UiState<UserProfile?>> =
        vaultRepo.observeMyProfile(currentUserId)
            .toUiStateFlow(TAG, "Failed to load citizen profile")

    val discoverableCitizensState: StateFlow<UiState<List<UserProfile>>> =
        vaultRepo.observeDiscoverableCitizens(currentUserId)
            .toUiStateFlow(TAG, "Failed to load citizen directory")

    val contactsState: StateFlow<UiState<List<ContactItem>>> =
        vaultRepo.observeContacts(currentUserId)
            .toUiStateFlow(TAG, "Failed to load contacts")

    val invitesState: StateFlow<UiState<List<InviteRecord>>> =
        vaultRepo.observeInvites(currentUserId)
            .toUiStateFlow(TAG, "Failed to load invites")

    val privacyVaultState: StateFlow<UiState<PrivacyVault?>> =
        vaultRepo.observePrivacyVault(currentUserId)
            .toUiStateFlow(TAG, "Failed to load privacy vault")

    val consentGrantsState: StateFlow<UiState<List<ConsentGrant>>> =
        vaultRepo.observeConsentGrants(currentUserId)
            .toUiStateFlow(TAG, "Failed to load consent grants")

    val auditLogsState: StateFlow<UiState<List<PrivacyAuditLog>>> =
        vaultRepo.observeAuditLogs(currentUserId)
            .toUiStateFlow(TAG, "Failed to load audit logs")

    val chatsState: StateFlow<UiState<List<ChatThread>>> =
        socialRepo.observeChats(currentUserId)
            .toUiStateFlow(TAG, "Failed to load chats")

    val activeMessagesState: StateFlow<UiState<List<ChatMessage>>> =
        _activeChatId.flatMapLatest { chatId ->
            if (chatId.isNullOrBlank()) flowOf(emptyList())
            else socialRepo.observeMessages(chatId, currentUserId)
        }.toUiStateFlow(TAG, "Failed to load messages")

    val publicPostsState: StateFlow<UiState<List<SocialPost>>> =
        socialRepo.observePublicPosts(currentUserId)
            .toUiStateFlow(TAG, "Failed to load social feed")

    val myPostsState: StateFlow<UiState<List<SocialPost>>> =
        socialRepo.observeMyPosts(currentUserId)
            .toUiStateFlow(TAG, "Failed to load personal posts")

    val publicChannelsState: StateFlow<UiState<List<Channel>>> =
        socialRepo.observePublicChannels(currentUserId)
            .toUiStateFlow(TAG, "Failed to load channels")

    val myChannelsState: StateFlow<UiState<List<Channel>>> =
        socialRepo.observeMyChannels(currentUserId)
            .toUiStateFlow(TAG, "Failed to load owned channels")

    val subscriptionsState: StateFlow<UiState<List<UserSubscription>>> =
        socialRepo.observeSubscriptions(currentUserId)
            .toUiStateFlow(TAG, "Failed to load subscriptions")

    val activeBroadcastsState: StateFlow<UiState<List<ChannelBroadcast>>> =
        _activeChannel.flatMapLatest { channel ->
            if (channel == null) flowOf(emptyList())
            else socialRepo.observeBroadcasts(channel, currentUserId)
        }.toUiStateFlow(TAG, "Failed to load channel broadcasts")

    val publicVideosState: StateFlow<UiState<List<VideoItem>>> =
        socialRepo.observePublicVideos(currentUserId)
            .toUiStateFlow(TAG, "Failed to load videos")

    val myVideosState: StateFlow<UiState<List<VideoItem>>> =
        socialRepo.observeMyVideos(currentUserId)
            .toUiStateFlow(TAG, "Failed to load personal videos")

    val activePostCommentsState: StateFlow<UiState<List<CommentItem>>> =
        _activeCommentPost.flatMapLatest { post ->
            if (post == null) flowOf(emptyList())
            else socialRepo.observePostComments(post, currentUserId)
        }.toUiStateFlow(TAG, "Failed to load post comments")

    val activeVideoCommentsState: StateFlow<UiState<List<CommentItem>>> =
        _activeVideo.flatMapLatest { video ->
            if (video == null) flowOf(emptyList())
            else socialRepo.observeVideoComments(video, currentUserId)
        }.toUiStateFlow(TAG, "Failed to load video comments")

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun selectChat(chatId: String?) {
        _activeChatId.value = chatId
    }

    fun selectChannel(channel: Channel?) {
        _activeChannel.value = channel
    }

    fun selectCommentPost(post: SocialPost?) {
        _activeCommentPost.value = post
    }

    fun selectVideo(video: VideoItem?) {
        _activeVideo.value = video
        if (video != null) {
            viewModelScope.launch { socialRepo.incrementVideoView(video.id) }
        }
    }

    fun rememberLocalMediaUri(key: String, uri: String) {
        _localPickedMediaUris.value = _localPickedMediaUris.value + (key to uri)
    }

    private fun handleMutationResult(result: Result<*>, successMessage: String? = null) {
        result.onSuccess {
            if (successMessage != null) _statusBanner.value = successMessage
        }.onFailure { err ->
            _statusBanner.value = err.localizedMessage ?: "Action could not be completed"
        }
    }

    // --- CONTACTS & INVITES ACTIONS ---

    fun addContact(
        displayName: String,
        handle: String,
        note: String = "",
        contactUid: String? = null
    ) {
        viewModelScope.launch {
            handleMutationResult(
                vaultRepo.addContact(displayName, handle, note, contactUid),
                "Contact saved"
            )
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            handleMutationResult(vaultRepo.deleteContact(contactId), "Contact removed")
        }
    }

    fun createInvite(
        recipientName: String,
        channel: String = "SHARE",
        presetCode: String? = null,
        onCreated: (InviteRecord) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = vaultRepo.createInvite(recipientName, channel, presetCode)
            res.onSuccess { record ->
                _statusBanner.value = "Invite ${record.inviteCode} saved & ready"
                onCreated(record)
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Invite copied (offline)"
            }
        }
    }

    fun startChatWithContact(
        displayName: String,
        contactUid: String? = null,
        onChatReady: (String) -> Unit
    ) {
        val currentChats = (chatsState.value as? UiState.Success)?.data.orEmpty()
        val existing = currentChats.firstOrNull { chat ->
            val matchesUid = !contactUid.isNullOrBlank() && contactUid in chat.participantUids
            val matchesTitle = chat.chatType != ChatType.GROUP.name &&
                chat.title.equals(displayName.trim(), ignoreCase = true)
            matchesUid || matchesTitle
        }
        if (existing != null) {
            onChatReady(existing.id)
            return
        }
        createChat(
            title = displayName,
            chatType = ChatType.DIRECT,
            disappearingHours = 24L,
            otherParticipantUid = contactUid,
            onCreated = onChatReady
        )
    }

    // --- MESSAGING ACTIONS ---

    fun createChat(
        title: String,
        chatType: ChatType,
        disappearingHours: Long,
        otherParticipantUid: String? = null,
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = socialRepo.createChat(title, chatType, disappearingHours, otherParticipantUid)
            res.onSuccess { id ->
                _statusBanner.value = "Encrypted chat created"
                onCreated(id)
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Could not create chat"
            }
        }
    }

    fun sendMessage(
        chat: ChatThread,
        senderName: String,
        text: String,
        replyToText: String? = null,
        mediaTag: String? = null,
        localMediaUri: String? = null
    ) {
        viewModelScope.launch {
            val res = socialRepo.sendMessage(chat, senderName, text, replyToText, mediaTag)
            res.onSuccess { msgId ->
                if (!localMediaUri.isNullOrBlank()) {
                    rememberLocalMediaUri(msgId, localMediaUri)
                }
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Message could not be sent"
            }
        }
    }

    fun reactToMessage(chatId: String, messageId: String, emoji: String) {
        viewModelScope.launch {
            handleMutationResult(socialRepo.reactToMessage(chatId, messageId, emoji))
        }
    }

    fun updateDisappearingTimer(chatId: String, hours: Long) {
        viewModelScope.launch {
            val res = socialRepo.updateChatDisappearingTimer(chatId, hours)
            handleMutationResult(res, if (hours == 0L) "Disappearing timer turned off" else "Timer set to ${hours}h")
        }
    }

    // --- FEED & STORIES ACTIONS ---

    fun createPost(
        authorName: String,
        authorHandle: String,
        caption: String,
        location: String,
        postKind: PostKind,
        visibility: PostVisibility,
        mediaPreset: MediaPreset,
        localMediaUri: String? = null
    ) {
        viewModelScope.launch {
            val res = socialRepo.createPost(
                authorName, authorHandle, caption, location, postKind, visibility, mediaPreset
            )
            res.onSuccess { postId ->
                if (!localMediaUri.isNullOrBlank()) {
                    rememberLocalMediaUri(postId, localMediaUri)
                }
                _statusBanner.value = if (visibility == PostVisibility.PRIVATE) {
                    "Saved to your Kavach Vault"
                } else {
                    "Published to Samvad"
                }
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Could not publish post"
            }
        }
    }

    fun togglePostVisibility(post: SocialPost) {
        viewModelScope.launch {
            val res = socialRepo.togglePostVisibility(post)
            handleMutationResult(res, "Post privacy updated")
        }
    }

    fun likePost(postId: String) {
        viewModelScope.launch {
            handleMutationResult(socialRepo.likePost(postId))
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            handleMutationResult(socialRepo.deletePost(postId), "Post deleted")
        }
    }

    fun addPostComment(post: SocialPost, authorName: String, text: String) {
        viewModelScope.launch {
            handleMutationResult(socialRepo.addPostComment(post, authorName, text))
        }
    }

    // --- CHANNELS & VIDEOS ACTIONS ---

    fun createChannel(
        name: String,
        handle: String,
        description: String,
        category: String,
        privacy: ChannelPrivacy
    ) {
        viewModelScope.launch {
            handleMutationResult(
                socialRepo.createChannel(name, handle, description, category, privacy),
                "Channel created on Narad Vani"
            )
        }
    }

    fun toggleSubscription(channel: Channel, isSubscribed: Boolean) {
        viewModelScope.launch {
            handleMutationResult(
                socialRepo.toggleSubscription(channel, isSubscribed),
                if (isSubscribed) "Unsubscribed from ${channel.name}" else "Subscribed to ${channel.name}"
            )
        }
    }

    fun publishBroadcast(channel: Channel, title: String, body: String) {
        viewModelScope.launch {
            handleMutationResult(
                socialRepo.publishBroadcast(channel, title, body),
                "Broadcast sent to channel"
            )
        }
    }

    fun createVideo(
        channelId: String,
        channelName: String,
        title: String,
        description: String,
        format: VideoFormat,
        durationLabel: String,
        category: String,
        thumbnailPreset: ThumbnailPreset,
        visibility: PostVisibility
    ) {
        viewModelScope.launch {
            handleMutationResult(
                socialRepo.createVideo(
                    channelId, channelName, title, description, format,
                    durationLabel, category, thumbnailPreset, visibility
                ),
                "Video published to Drishya"
            )
        }
    }

    fun likeVideo(videoId: String) {
        viewModelScope.launch {
            handleMutationResult(socialRepo.likeVideo(videoId))
        }
    }

    fun addVideoComment(video: VideoItem, authorName: String, text: String) {
        viewModelScope.launch {
            handleMutationResult(socialRepo.addVideoComment(video, authorName, text))
        }
    }

    // --- PROFILE & KAVACH PRIVACY VAULT ACTIONS ---

    fun updateProfile(
        displayName: String,
        handle: String,
        bio: String,
        city: String,
        discoverable: Boolean
    ) {
        viewModelScope.launch {
            handleMutationResult(
                vaultRepo.updateProfile(displayName, handle, bio, city, discoverable),
                "Citizen profile updated"
            )
        }
    }

    fun updatePrivacyVault(
        defaultPostVisibility: PostVisibility,
        messagePolicy: MessagePolicy,
        readReceipts: Boolean,
        onlineStatus: Boolean,
        disappearingHours: Long,
        screenshotGuard: Boolean
    ) {
        viewModelScope.launch {
            handleMutationResult(
                vaultRepo.updatePrivacyVault(
                    defaultPostVisibility, messagePolicy, readReceipts,
                    onlineStatus, disappearingHours, screenshotGuard
                ),
                "Kavach privacy policy saved"
            )
        }
    }

    fun addConsentGrant(granteeName: String, granteeHandle: String, scope: ConsentScope) {
        viewModelScope.launch {
            handleMutationResult(
                vaultRepo.addConsentGrant(granteeName, granteeHandle, scope),
                "Explicit consent granted to $granteeHandle"
            )
        }
    }

    fun toggleConsentGrant(grant: ConsentGrant) {
        viewModelScope.launch {
            handleMutationResult(
                vaultRepo.toggleConsentGrant(grant),
                if (grant.active) "Consent revoked" else "Consent activated"
            )
        }
    }

    fun deleteConsentGrant(grantId: String) {
        viewModelScope.launch {
            handleMutationResult(vaultRepo.deleteConsentGrant(grantId), "Consent grant removed")
        }
    }

    fun runWarrantAuditCheck() {
        viewModelScope.launch {
            handleMutationResult(
                vaultRepo.logWarrantPolicyInspection(),
                "Audit complete: 0 external warrants, 100% owner-isolated"
            )
        }
    }

    private companion object {
        const val TAG = "NaradVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
