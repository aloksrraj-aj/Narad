package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Channel
import com.example.data.model.ChannelBroadcast
import com.example.data.model.ChannelPrivacy
import com.example.data.model.UserSubscription
import com.example.data.remote.UiState
import com.example.ui.components.CitizenAvatar
import com.example.ui.components.SovereigntySealChip
import com.example.ui.components.formatNaradTimestamp
import com.example.ui.theme.AshokaIndigo
import com.example.ui.theme.SovereignEmerald

@Composable
fun ChannelsScreen(
    currentUserId: String,
    publicChannelsState: UiState<List<Channel>>,
    myChannelsState: UiState<List<Channel>>,
    subscriptionsState: UiState<List<UserSubscription>>,
    activeChannel: Channel?,
    broadcastsState: UiState<List<ChannelBroadcast>>,
    onSelectChannel: (Channel?) -> Unit,
    onCreateChannel: (name: String, handle: String, description: String, category: String, privacy: ChannelPrivacy) -> Unit,
    onToggleSubscription: (Channel, Boolean) -> Unit,
    onPublishBroadcast: (Channel, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val subscriptions = (subscriptionsState as? UiState.Success)?.data.orEmpty()
    val subscribedIds = remember(subscriptions) { subscriptions.map { it.channelId }.toSet() }

    val publicChannels = (publicChannelsState as? UiState.Success)?.data.orEmpty()
    val myChannels = (myChannelsState as? UiState.Success)?.data.orEmpty()
    val allChannels = remember(publicChannels, myChannels) {
        (myChannels + publicChannels).distinctBy { it.id }
    }
    val resolvedActiveChannel = remember(activeChannel, allChannels) {
        activeChannel?.let { target -> allChannels.firstOrNull { it.id == target.id } ?: target }
    }

    if (resolvedActiveChannel != null) {
        BackHandler { onSelectChannel(null) }
        ChannelDetailView(
            channel = resolvedActiveChannel,
            isOwner = resolvedActiveChannel.ownerUid == currentUserId,
            isSubscribed = resolvedActiveChannel.id in subscribedIds,
            broadcastsState = broadcastsState,
            onBack = { onSelectChannel(null) },
            onToggleSubscribe = {
                onToggleSubscription(resolvedActiveChannel, resolvedActiveChannel.id in subscribedIds)
            },
            onPublishBroadcast = { title, body -> onPublishBroadcast(resolvedActiveChannel, title, body) },
            modifier = modifier
        )
        return
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var filterTab by remember { mutableStateOf("ALL") }

    val displayed = remember(filterTab, allChannels, subscribedIds, currentUserId) {
        when (filterTab) {
            "SUBSCRIBED" -> allChannels.filter { it.id in subscribedIds }
            "MINE" -> allChannels.filter { it.ownerUid == currentUserId }
            else -> allChannels
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_channel_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create Channel")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Vani",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Editorial Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(116.dp)
                        .clip(MaterialTheme.shapes.large)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_channel_banner),
                        contentDescription = "Narad Vani Channels Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(AshokaIndigo.copy(alpha = 0.85f), Color.Transparent)
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Sovereign Channels",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Broadcasts • Zero tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filterTab == "ALL",
                        onClick = { filterTab = "ALL" },
                        label = { Text("Discover") }
                    )
                    FilterChip(
                        selected = filterTab == "SUBSCRIBED",
                        onClick = { filterTab = "SUBSCRIBED" },
                        label = { Text("Joined (${subscribedIds.size})") }
                    )
                    FilterChip(
                        selected = filterTab == "MINE",
                        onClick = { filterTab = "MINE" },
                        label = { Text("Mine") }
                    )
                }
            }

            if (publicChannelsState is UiState.Loading && allChannels.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(displayed, key = { it.id }) { channel ->
                    val isSub = channel.id in subscribedIds
                    ChannelRowCard(
                        channel = channel,
                        isSubscribed = isSub,
                        onOpen = { onSelectChannel(channel) },
                        onToggleSub = { onToggleSubscription(channel, isSub) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showCreateDialog) {
        CreateChannelDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, handle, desc, cat, priv ->
                onCreateChannel(name, handle, desc, cat, priv)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun ChannelRowCard(
    channel: Channel,
    isSubscribed: Boolean,
    onOpen: () -> Unit,
    onToggleSub: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onOpen)
            .testTag("channel_card_${channel.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CitizenAvatar(name = channel.name, size = 48.dp, verified = channel.verified)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (channel.verified) {
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = "Verified channel",
                            tint = SovereignEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = "${channel.handle} • ${channel.subscribersCount} subscribers • ${channel.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = channel.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (isSubscribed) {
                OutlinedButton(onClick = onToggleSub) {
                    Text("Joined")
                }
            } else {
                Button(onClick = onToggleSub) {
                    Text("Join")
                }
            }
        }
    }
}

@Composable
private fun ChannelDetailView(
    channel: Channel,
    isOwner: Boolean,
    isSubscribed: Boolean,
    broadcastsState: UiState<List<ChannelBroadcast>>,
    onBack: () -> Unit,
    onToggleSubscribe: () -> Unit,
    onPublishBroadcast: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBroadcastDialog by remember { mutableStateOf(false) }
    val broadcasts = (broadcastsState as? UiState.Success)?.data.orEmpty()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (isOwner) {
                FloatingActionButton(
                    onClick = { showBroadcastDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Campaign, contentDescription = "Broadcast update")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to channels")
                    }
                    CitizenAvatar(name = channel.name, size = 44.dp, verified = channel.verified)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(channel.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${channel.handle} • ${channel.subscribersCount} members",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = onToggleSubscribe) {
                        Icon(Icons.Filled.NotificationsActive, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSubscribed) "Joined" else "Subscribe")
                    }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SovereigntySealChip(
                            label = if (channel.channelPrivacy == ChannelPrivacy.INVITE_ONLY.name) "Invite-Only Channel"
                            else "Public Broadcast Channel",
                            isPrivate = channel.channelPrivacy == ChannelPrivacy.INVITE_ONLY.name
                        )
                        Text(channel.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                Text("Channel Broadcasts", style = MaterialTheme.typography.titleMedium)
            }

            if (broadcasts.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isOwner) "Tap the megaphone button to send your first broadcast."
                            else "No broadcasts published in this channel yet.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(broadcasts, key = { it.id }) { bc ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(bc.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(bc.body, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                formatNaradTimestamp(bc.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBroadcastDialog) {
        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showBroadcastDialog = false },
            title = { Text("Send Broadcast") },
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
                        label = { Text("Headline") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = {
                            body = it
                            error = null
                        },
                        label = { Text("Announcement") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!error.isNullOrBlank()) {
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isBlank() || body.isBlank()) {
                            error = "Enter headline and announcement"
                        } else {
                            onPublishBroadcast(title, body)
                            showBroadcastDialog = false
                        }
                    }
                ) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBroadcastDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CreateChannelDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, handle: String, description: String, category: String, privacy: ChannelPrivacy) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Technology") }
    var privacy by remember { mutableStateOf(ChannelPrivacy.PUBLIC) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Channel") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("Channel Name") },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("channel_name_input")
                )
                if (!error.isNullOrBlank()) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text("Handle (@mychannel)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Mission (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = privacy == ChannelPrivacy.PUBLIC,
                        onClick = { privacy = ChannelPrivacy.PUBLIC },
                        label = { Text("Public") },
                        leadingIcon = { Icon(Icons.Filled.Public, null, Modifier.size(14.dp)) }
                    )
                    FilterChip(
                        selected = privacy == ChannelPrivacy.INVITE_ONLY,
                        onClick = { privacy = ChannelPrivacy.INVITE_ONLY },
                        label = { Text("Invite-Only") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null, Modifier.size(14.dp)) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    if (cleanName.length < 2) {
                        error = "Channel name must be at least 2 characters"
                    } else {
                        val h = handle.trim().ifEmpty {
                            cleanName.lowercase().replace(Regex("[^a-z0-9]"), "_").ifEmpty { "channel" }
                        }
                        val finalDesc = desc.trim().ifEmpty { "Official Narad Vani channel" }
                        onCreate(cleanName, h, finalDesc, category.ifBlank { "General" }, privacy)
                    }
                },
                modifier = Modifier.testTag("create_channel_confirm_button")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
