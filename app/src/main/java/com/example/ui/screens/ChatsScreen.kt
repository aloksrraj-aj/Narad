package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.ChatType
import com.example.data.model.ContactItem
import com.example.data.model.UserProfile
import com.example.data.model.safeEnumValue
import com.example.data.remote.UiState
import com.example.ui.components.CitizenAvatar
import com.example.ui.components.formatNaradTimeShort
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.SovereignEmerald

@Composable
fun ChatsScreen(
    currentUserId: String,
    currentUserName: String,
    chatsState: UiState<List<ChatThread>>,
    contactsState: UiState<List<ContactItem>>,
    discoverableCitizensState: UiState<List<UserProfile>>,
    activeChatId: String?,
    messagesState: UiState<List<ChatMessage>>,
    localPickedMediaUris: Map<String, String>,
    onSelectChat: (String?) -> Unit,
    onCreateChat: (title: String, type: ChatType, disappearingHours: Long, otherUid: String?) -> Unit,
    onStartChatWithContact: (displayName: String, contactUid: String?) -> Unit,
    onOpenContactsSheet: (initialTab: Int) -> Unit,
    onSendMessage: (chat: ChatThread, text: String, replyTo: String?, mediaTag: String?, localMediaUri: String?) -> Unit,
    onReactToMessage: (chatId: String, messageId: String, emoji: String) -> Unit,
    onUpdateDisappearingTimer: (chatId: String, hours: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val chats = (chatsState as? UiState.Success)?.data.orEmpty()
    val contacts = (contactsState as? UiState.Success)?.data.orEmpty()
    val activeChat = chats.firstOrNull { it.id == activeChatId }

    if (activeChat != null) {
        BackHandler { onSelectChat(null) }
        ChatDetailView(
            chat = activeChat,
            currentUserId = currentUserId,
            messagesState = messagesState,
            localPickedMediaUris = localPickedMediaUris,
            onBack = { onSelectChat(null) },
            onSendMessage = { text, replyTo, mediaTag, localUri ->
                onSendMessage(activeChat, text, replyTo, mediaTag, localUri)
            },
            onReact = { messageId, emoji ->
                onReactToMessage(activeChat.id, messageId, emoji)
            },
            onUpdateDisappearingTimer = { hours ->
                onUpdateDisappearingTimer(activeChat.id, hours)
            },
            modifier = modifier
        )
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showNewChatDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChatDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("new_chat_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New chat")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chats",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { onOpenContactsSheet(0) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("open_contacts_button")
                    ) {
                        Icon(Icons.Filled.Contacts, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Contact", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = { onOpenContactsSheet(1) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("open_invite_button")
                    ) {
                        Icon(Icons.Filled.PersonAddAlt1, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Invite Friend", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Contacts & Invite Strip
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                item {
                    QuickActionAvatarItem(
                        label = "Invite Friend",
                        containerColor = SaffronPrimary,
                        icon = Icons.Filled.Share,
                        onClick = { onOpenContactsSheet(1) }
                    )
                }
                items(contacts, key = { it.id }) { contact ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(62.dp)
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                onStartChatWithContact(
                                    contact.displayName,
                                    contact.contactUid.takeIf { it.isNotBlank() }
                                )
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        CitizenAvatar(
                            name = contact.displayName,
                            size = 48.dp,
                            verified = contact.verified
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = contact.displayName.split(" ").first(),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                item {
                    QuickActionAvatarItem(
                        label = "Add Contact",
                        containerColor = SovereignEmerald,
                        icon = Icons.Filled.Add,
                        onClick = { onOpenContactsSheet(0) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf(
                    "ALL" to "All",
                    ChatType.DIRECT.name to "Direct",
                    ChatType.GROUP.name to "Groups",
                    ChatType.SECRET.name to "Secret"
                )
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (chatsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = chatsState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    val filtered = chats.filter { chat ->
                        val matchesType = selectedFilter == "ALL" || chat.chatType == selectedFilter
                        val matchesQuery = searchQuery.isBlank() ||
                            chat.title.contains(searchQuery, ignoreCase = true) ||
                            chat.lastMessage.contains(searchQuery, ignoreCase = true)
                        matchesType && matchesQuery
                    }

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "No chats found",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                TextButton(onClick = { onOpenContactsSheet(0) }) {
                                    Text("Add or pick a contact")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filtered, key = { it.id }) { chat ->
                                ChatRowCard(
                                    chat = chat,
                                    onClick = { onSelectChat(chat.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        val citizens = (discoverableCitizensState as? UiState.Success)?.data
            ?.filter { it.userId != currentUserId }
            .orEmpty()
        NewChatDialog(
            contacts = contacts,
            discoverableCitizens = citizens,
            onDismiss = { showNewChatDialog = false },
            onCreate = { title, type, hours, otherUid ->
                onCreateChat(title, type, hours, otherUid)
                showNewChatDialog = false
            }
        )
    }
}

@Composable
private fun QuickActionAvatarItem(
    label: String,
    containerColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(62.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    ) {
        Surface(
            color = containerColor.copy(alpha = 0.16f),
            shape = CircleShape,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = containerColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = containerColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun ChatRowCard(
    chat: ChatThread,
    onClick: () -> Unit
) {
    val type = safeEnumValue(chat.chatType, ChatType.DIRECT)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("chat_item_${chat.id}"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CitizenAvatar(
                name = chat.title,
                size = 48.dp,
                verified = type == ChatType.SECRET
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = chat.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (type == ChatType.GROUP) {
                            Icon(
                                Icons.Filled.Groups,
                                contentDescription = "Group",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        } else if (type == ChatType.SECRET) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = "Secret",
                                modifier = Modifier.size(15.dp),
                                tint = SovereignEmerald
                            )
                        }
                    }
                    Text(
                        text = formatNaradTimeShort(chat.updatedAt ?: chat.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DoneAll,
                        contentDescription = "Delivered",
                        tint = SovereignEmerald,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = chat.lastMessage.ifBlank { "Tap to chat" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatDetailView(
    chat: ChatThread,
    currentUserId: String,
    messagesState: UiState<List<ChatMessage>>,
    localPickedMediaUris: Map<String, String>,
    onBack: () -> Unit,
    onSendMessage: (text: String, replyTo: String?, mediaTag: String?, localMediaUri: String?) -> Unit,
    onReact: (messageId: String, emoji: String) -> Unit,
    onUpdateDisappearingTimer: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    var replyQuote by remember { mutableStateOf<String?>(null) }
    var pickedMediaTag by remember { mutableStateOf<String?>(null) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var showTimerMenu by remember { mutableStateOf(false) }
    var showE2eeInfo by remember { mutableStateOf(false) }
    var selectedMessageId by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedUri = uri
            pickedMediaTag = "Photo (${uri.lastPathSegment?.take(14) ?: "image"})"
        }
    }

    val messages = (messagesState as? UiState.Success)?.data.orEmpty()
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("chat_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                CitizenAvatar(name = chat.title, size = 38.dp, verified = true)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chat.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (chat.disappearingHours > 0) "E2EE • ${chat.disappearingHours}h" else "E2EE Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = SovereignEmerald,
                        maxLines = 1
                    )
                }
                IconButton(onClick = { showE2eeInfo = true }) {
                    Icon(
                        Icons.Filled.Shield,
                        contentDescription = "Encryption info",
                        tint = SovereignEmerald
                    )
                }
                Box {
                    IconButton(onClick = { showTimerMenu = true }) {
                        Icon(
                            Icons.Filled.Timer,
                            contentDescription = "Disappearing timer",
                            tint = if (chat.disappearingHours > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showTimerMenu,
                        onDismissRequest = { showTimerMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Timer: Off") },
                            onClick = {
                                onUpdateDisappearingTimer(0L)
                                showTimerMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Timer: 24h") },
                            onClick = {
                                onUpdateDisappearingTimer(24L)
                                showTimerMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Timer: 7d") },
                            onClick = {
                                onUpdateDisappearingTimer(168L)
                                showTimerMenu = false
                            }
                        )
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMine = msg.senderUid == currentUserId
                val showActions = selectedMessageId == msg.id
                MessageBubble(
                    message = msg,
                    localPhotoUri = localPickedMediaUris[msg.id],
                    isMine = isMine,
                    showActions = showActions,
                    onToggleActions = {
                        selectedMessageId = if (showActions) null else msg.id
                    },
                    onReact = { emoji ->
                        onReact(msg.id, emoji)
                        selectedMessageId = null
                    },
                    onReply = {
                        replyQuote = msg.text.take(100)
                        selectedMessageId = null
                    }
                )
            }
        }

        if (replyQuote != null || pickedMediaTag != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = pickedMediaTag ?: replyQuote ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            replyQuote = null
                            pickedMediaTag = null
                            pickedUri = null
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Composer Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(Icons.Filled.AttachFile, contentDescription = "Attach photo")
                }
                IconButton(
                    onClick = { pickedMediaTag = "Voice note (0:08)" }
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Attach voice note")
                }
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Message...") },
                    singleLine = false,
                    maxLines = 4,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank() || pickedMediaTag != null) {
                                val textToSend = messageText.ifBlank { pickedMediaTag ?: "Attachment" }
                                onSendMessage(textToSend, replyQuote, pickedMediaTag, pickedUri?.toString())
                                messageText = ""
                                replyQuote = null
                                pickedMediaTag = null
                                pickedUri = null
                            }
                        },
                        modifier = Modifier.testTag("chat_send_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }

    if (showE2eeInfo) {
        AlertDialog(
            onDismissRequest = { showE2eeInfo = false },
            icon = { Icon(Icons.Filled.Shield, null, tint = SovereignEmerald) },
            title = { Text("Kavach E2EE") },
            text = {
                Text(
                    "Key: ${chat.encryptedFingerprint}\nOnly thread members can read these messages.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showE2eeInfo = false }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    localPhotoUri: String?,
    isMine: Boolean,
    showActions: Boolean,
    onToggleActions: () -> Unit,
    onReact: (String) -> Unit,
    onReply: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Box {
            Surface(
                color = if (isMine) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isMine) 18.dp else 4.dp,
                    bottomEnd = if (isMine) 4.dp else 18.dp
                ),
                tonalElevation = 1.dp,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clickable(onClick = onToggleActions)
                    .testTag("msg_bubble_${message.id}")
            ) {
                Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)) {
                    if (!isMine) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (message.replyToText.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = message.replyToText,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (!localPhotoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = Uri.parse(localPhotoUri),
                            contentDescription = message.text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else if (message.mediaTag.isNotBlank()) {
                        Surface(
                            color = SovereignEmerald.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = message.mediaTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = SovereignEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (message.reaction.isNotBlank()) {
                            Text(
                                text = message.reaction,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Text(
                            text = formatNaradTimeShort(message.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isMine) {
                            Icon(
                                imageVector = Icons.Filled.DoneAll,
                                contentDescription = "Read",
                                tint = SovereignEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = showActions) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(50),
                tonalElevation = 4.dp,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("❤️", "👍", "🙏", "🔥").forEach { emoji ->
                        Text(
                            text = emoji,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onReact(emoji) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("msg_react_$emoji")
                        )
                    }
                    TextButton(
                        onClick = onReply,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.testTag("msg_reply_button")
                    ) {
                        Text("Reply", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun NewChatDialog(
    contacts: List<ContactItem>,
    discoverableCitizens: List<UserProfile>,
    onDismiss: () -> Unit,
    onCreate: (title: String, type: ChatType, disappearingHours: Long, otherUid: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ChatType.DIRECT) }
    var timerHours by remember { mutableLongStateOf(24L) }
    var selectedUid by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Chat") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        error = null
                    },
                    label = { Text("Chat Name") },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_chat_name_input")
                )
                if (!error.isNullOrBlank()) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChatType.entries.forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t.name) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0L to "Off", 24L to "24h", 168L to "7d").forEach { (hrs, lbl) ->
                        FilterChip(
                            selected = timerHours == hrs,
                            onClick = { timerHours = hrs },
                            label = { Text(lbl) }
                        )
                    }
                }
                if (contacts.isNotEmpty() || discoverableCitizens.isNotEmpty()) {
                    Text("Pick Contact", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(contacts, key = { "c_${it.id}" }) { contact ->
                            FilterChip(
                                selected = title == contact.displayName,
                                onClick = {
                                    title = contact.displayName
                                    selectedUid = contact.contactUid.takeIf { it.isNotBlank() }
                                    error = null
                                },
                                label = { Text(contact.displayName) }
                            )
                        }
                        items(discoverableCitizens, key = { "u_${it.userId}" }) { citizen ->
                            FilterChip(
                                selected = selectedUid == citizen.userId,
                                onClick = {
                                    selectedUid = citizen.userId
                                    title = citizen.displayName
                                    error = null
                                },
                                label = { Text(citizen.displayName) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.trim()
                    if (finalTitle.isEmpty()) {
                        error = "Enter a chat name or pick a contact"
                    } else {
                        onCreate(finalTitle, selectedType, timerHours, selectedUid)
                    }
                },
                modifier = Modifier.testTag("new_chat_start_button")
            ) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
