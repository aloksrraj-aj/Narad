package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.ChatType
import com.example.data.model.PostVisibility
import com.example.data.model.safeEnumValue
import com.example.data.remote.NaradDeepLink
import com.example.data.remote.UiState
import com.example.data.remote.signOutUser
import com.example.data.repository.SocialAndMessagingRepository
import com.example.data.repository.VaultAndProfileRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChannelsScreen
import com.example.ui.screens.ChatsScreen
import com.example.ui.screens.ContactsInviteSheet
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.VaultProfileScreen
import com.example.ui.screens.VideosScreen
import com.example.viewmodel.NaradViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class NaradTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    CHATS("Chats", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, "nav_chats"),
    FEED("Samvad", Icons.Filled.DynamicFeed, Icons.Outlined.DynamicFeed, "nav_feed"),
    VIDEOS("Drishya", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircleOutline, "nav_videos"),
    CHANNELS("Vani", Icons.Filled.Campaign, Icons.Outlined.Campaign, "nav_channels"),
    VAULT("Kavach", Icons.Filled.Shield, Icons.Outlined.Shield, "nav_vault")
}

@Composable
fun NaradApp(
    pendingDeepLink: NaradDeepLink? = null,
    onConsumeDeepLink: () -> Unit = {}
) {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        key(user.uid) {
            NaradAuthenticatedShell(
                currentUserId = user.uid,
                initialDisplayName = user.displayName,
                initialEmail = user.email,
                pendingDeepLink = pendingDeepLink,
                onConsumeDeepLink = onConsumeDeepLink
            )
        }
    }
}

@Composable
private fun NaradAuthenticatedShell(
    currentUserId: String,
    initialDisplayName: String?,
    initialEmail: String?,
    pendingDeepLink: NaradDeepLink?,
    onConsumeDeepLink: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: NaradViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                NaradViewModel(
                    vaultRepo = VaultAndProfileRepository(db),
                    socialRepo = SocialAndMessagingRepository(db),
                    currentUserId = currentUserId,
                    initialDisplayName = initialDisplayName,
                    initialEmail = initialEmail
                )
            }
        }
    )

    var selectedTab by rememberSaveable { mutableStateOf(NaradTab.CHATS) }
    var contactsSheetTab by rememberSaveable { mutableStateOf<Int?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val discoverableCitizensState by viewModel.discoverableCitizensState.collectAsStateWithLifecycle()
    val contactsState by viewModel.contactsState.collectAsStateWithLifecycle()
    val invitesState by viewModel.invitesState.collectAsStateWithLifecycle()
    val vaultState by viewModel.privacyVaultState.collectAsStateWithLifecycle()
    val consentGrantsState by viewModel.consentGrantsState.collectAsStateWithLifecycle()
    val auditLogsState by viewModel.auditLogsState.collectAsStateWithLifecycle()
    val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
    val activeChatId by viewModel.activeChatId.collectAsStateWithLifecycle()
    val messagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val publicPostsState by viewModel.publicPostsState.collectAsStateWithLifecycle()
    val myPostsState by viewModel.myPostsState.collectAsStateWithLifecycle()
    val activeCommentPost by viewModel.activeCommentPost.collectAsStateWithLifecycle()
    val postCommentsState by viewModel.activePostCommentsState.collectAsStateWithLifecycle()
    val publicChannelsState by viewModel.publicChannelsState.collectAsStateWithLifecycle()
    val myChannelsState by viewModel.myChannelsState.collectAsStateWithLifecycle()
    val subscriptionsState by viewModel.subscriptionsState.collectAsStateWithLifecycle()
    val activeChannel by viewModel.activeChannel.collectAsStateWithLifecycle()
    val broadcastsState by viewModel.activeBroadcastsState.collectAsStateWithLifecycle()
    val publicVideosState by viewModel.publicVideosState.collectAsStateWithLifecycle()
    val myVideosState by viewModel.myVideosState.collectAsStateWithLifecycle()
    val activeVideo by viewModel.activeVideo.collectAsStateWithLifecycle()
    val videoCommentsState by viewModel.activeVideoCommentsState.collectAsStateWithLifecycle()
    val localPickedMediaUris by viewModel.localPickedMediaUris.collectAsStateWithLifecycle()

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    if (selectedTab != NaradTab.CHATS &&
        activeChatId == null &&
        activeChannel == null &&
        activeVideo == null
    ) {
        BackHandler { selectedTab = NaradTab.CHATS }
    }

    val profile = (profileState as? UiState.Success)?.data
    val vault = (vaultState as? UiState.Success)?.data
    val currentName = profile?.displayName?.ifBlank { initialDisplayName ?: "Narad Citizen" }
        ?: initialDisplayName ?: "Narad Citizen"
    val currentHandle = profile?.handle?.ifBlank { "@citizen" } ?: "@citizen"
    val defaultPostVisibility = safeEnumValue(vault?.defaultPostVisibility, PostVisibility.PRIVATE)
    val myChannels = (myChannelsState as? UiState.Success)?.data.orEmpty()
    val contactsCount = (contactsState as? UiState.Success)?.data?.size ?: 0

    val isInDetailView = (selectedTab == NaradTab.CHATS && activeChatId != null) ||
        (selectedTab == NaradTab.CHANNELS && activeChannel != null) ||
        (selectedTab == NaradTab.VIDEOS && activeVideo != null)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 640.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isWideScreen && !isInDetailView) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NaradTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NaradTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (selectedTab) {
                        NaradTab.CHATS -> ChatsScreen(
                            currentUserId = currentUserId,
                            currentUserName = currentName,
                            chatsState = chatsState,
                            contactsState = contactsState,
                            discoverableCitizensState = discoverableCitizensState,
                            activeChatId = activeChatId,
                            messagesState = messagesState,
                            localPickedMediaUris = localPickedMediaUris,
                            onSelectChat = viewModel::selectChat,
                            onCreateChat = { title, type, hours, otherUid ->
                                viewModel.createChat(title, type, hours, otherUid) { newId ->
                                    viewModel.selectChat(newId)
                                }
                            },
                            onStartChatWithContact = { displayName, contactUid ->
                                viewModel.startChatWithContact(displayName, contactUid) { chatId ->
                                    viewModel.selectChat(chatId)
                                }
                            },
                            onOpenContactsSheet = { tab -> contactsSheetTab = tab },
                            onSendMessage = { chat, text, replyTo, mediaTag, localUri ->
                                viewModel.sendMessage(chat, currentName, text, replyTo, mediaTag, localUri)
                            },
                            onReactToMessage = viewModel::reactToMessage,
                            onUpdateDisappearingTimer = viewModel::updateDisappearingTimer
                        )

                        NaradTab.FEED -> FeedScreen(
                            currentUserId = currentUserId,
                            currentUserName = currentName,
                            currentUserHandle = currentHandle,
                            defaultPostVisibility = defaultPostVisibility,
                            publicPostsState = publicPostsState,
                            myPostsState = myPostsState,
                            localPickedMediaUris = localPickedMediaUris,
                            activeCommentPost = activeCommentPost,
                            commentsState = postCommentsState,
                            onCreatePost = { caption, loc, kind, vis, preset, localUri ->
                                viewModel.createPost(currentName, currentHandle, caption, loc, kind, vis, preset, localUri)
                            },
                            onLikePost = viewModel::likePost,
                            onToggleVisibility = viewModel::togglePostVisibility,
                            onDeletePost = viewModel::deletePost,
                            onOpenComments = viewModel::selectCommentPost,
                            onAddComment = { post, text ->
                                viewModel.addPostComment(post, currentName, text)
                            },
                            onShareToChats = { post ->
                                viewModel.createChat(
                                    title = "Shared: ${post.authorName}",
                                    chatType = ChatType.DIRECT,
                                    disappearingHours = 24L
                                ) { newChatId ->
                                    selectedTab = NaradTab.CHATS
                                    viewModel.selectChat(newChatId)
                                }
                            }
                        )

                        NaradTab.VIDEOS -> VideosScreen(
                            currentUserId = currentUserId,
                            publicVideosState = publicVideosState,
                            myVideosState = myVideosState,
                            myChannels = myChannels,
                            activeVideo = activeVideo,
                            videoCommentsState = videoCommentsState,
                            onSelectVideo = viewModel::selectVideo,
                            onCreateVideo = viewModel::createVideo,
                            onLikeVideo = viewModel::likeVideo,
                            onAddVideoComment = { video, text ->
                                viewModel.addVideoComment(video, currentName, text)
                            }
                        )

                        NaradTab.CHANNELS -> ChannelsScreen(
                            currentUserId = currentUserId,
                            publicChannelsState = publicChannelsState,
                            myChannelsState = myChannelsState,
                            subscriptionsState = subscriptionsState,
                            activeChannel = activeChannel,
                            broadcastsState = broadcastsState,
                            onSelectChannel = viewModel::selectChannel,
                            onCreateChannel = viewModel::createChannel,
                            onToggleSubscription = viewModel::toggleSubscription,
                            onPublishBroadcast = viewModel::publishBroadcast
                        )

                        NaradTab.VAULT -> VaultProfileScreen(
                            currentUserId = currentUserId,
                            profileState = profileState,
                            vaultState = vaultState,
                            consentGrantsState = consentGrantsState,
                            auditLogsState = auditLogsState,
                            myPostsState = myPostsState,
                            contactsCount = contactsCount,
                            onOpenContactsSheet = { tab -> contactsSheetTab = tab },
                            onUpdateProfile = viewModel::updateProfile,
                            onUpdateVault = viewModel::updatePrivacyVault,
                            onAddConsentGrant = viewModel::addConsentGrant,
                            onToggleConsentGrant = viewModel::toggleConsentGrant,
                            onDeleteConsentGrant = viewModel::deleteConsentGrant,
                            onRunWarrantCheck = viewModel::runWarrantAuditCheck,
                            onSignOut = {
                                signOutUser(
                                    credentialManager = credentialManager,
                                    scope = scope
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    val sheetTab = contactsSheetTab
    if (sheetTab != null) {
        ContactsInviteSheet(
            initialTab = sheetTab,
            currentUserId = currentUserId,
            currentUserName = currentName,
            currentUserHandle = currentHandle,
            contactsState = contactsState,
            invitesState = invitesState,
            discoverableCitizensState = discoverableCitizensState,
            onDismiss = { contactsSheetTab = null },
            onAddContact = viewModel::addContact,
            onDeleteContact = viewModel::deleteContact,
            onStartChat = { name, uid ->
                viewModel.startChatWithContact(name, uid) { chatId ->
                    selectedTab = NaradTab.CHATS
                    viewModel.selectChat(chatId)
                }
            },
            onCreateInvite = viewModel::createInvite
        )
    }

    if (pendingDeepLink != null) {
        val targetName = pendingDeepLink.senderName.ifBlank {
            pendingDeepLink.senderHandle.removePrefix("@").ifBlank { "Narad Citizen" }
        }
        val targetHandle = pendingDeepLink.senderHandle.ifBlank {
            "@${pendingDeepLink.code.ifBlank { "narad" }.lowercase()}"
        }
        val targetUid = pendingDeepLink.senderUid.takeIf { it.isNotBlank() && it != currentUserId }
        AlertDialog(
            onDismissRequest = onConsumeDeepLink,
            title = { Text("Narad Invite Link") },
            text = {
                Text(
                    buildString {
                        append("Connect with $targetName ($targetHandle)")
                        if (pendingDeepLink.code.isNotBlank()) {
                            append(" using code ${pendingDeepLink.code}.")
                        }
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addContact(
                            displayName = targetName,
                            handle = targetHandle,
                            note = "Via ${pendingDeepLink.code.ifBlank { "Narad link" }}",
                            contactUid = targetUid
                        )
                        viewModel.startChatWithContact(targetName, targetUid) { chatId ->
                            selectedTab = NaradTab.CHATS
                            viewModel.selectChat(chatId)
                        }
                        onConsumeDeepLink()
                    }
                ) {
                    Text("Add & Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = onConsumeDeepLink) {
                    Text("Close")
                }
            }
        )
    }
}
