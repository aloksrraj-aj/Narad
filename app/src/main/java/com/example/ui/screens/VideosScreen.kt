package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Channel
import com.example.data.model.CommentItem
import com.example.data.model.PostVisibility
import com.example.data.model.ThumbnailPreset
import com.example.data.model.VideoFormat
import com.example.data.model.VideoItem
import com.example.data.remote.PhoneContactsHelper
import com.example.data.remote.UiState
import com.example.ui.components.CitizenAvatar
import com.example.ui.components.PresetMediaArtwork
import com.example.ui.components.VisibilityBadge
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.SovereignEmerald
import kotlinx.coroutines.delay

@Composable
fun VideosScreen(
    currentUserId: String,
    publicVideosState: UiState<List<VideoItem>>,
    myVideosState: UiState<List<VideoItem>>,
    myChannels: List<Channel>,
    activeVideo: VideoItem?,
    videoCommentsState: UiState<List<CommentItem>>,
    onSelectVideo: (VideoItem?) -> Unit,
    onCreateVideo: (
        channelId: String,
        channelName: String,
        title: String,
        description: String,
        format: VideoFormat,
        durationLabel: String,
        category: String,
        preset: ThumbnailPreset,
        visibility: PostVisibility
    ) -> Unit,
    onLikeVideo: (String) -> Unit,
    onAddVideoComment: (VideoItem, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val publicVideos = (publicVideosState as? UiState.Success)?.data.orEmpty()
    val myVideos = (myVideosState as? UiState.Success)?.data.orEmpty()
    val allVideos = remember(publicVideos, myVideos) {
        (myVideos + publicVideos).distinctBy { it.id }
    }
    val resolvedActiveVideo = remember(activeVideo, allVideos) {
        activeVideo?.let { target -> allVideos.firstOrNull { it.id == target.id } ?: target }
    }

    if (resolvedActiveVideo != null) {
        BackHandler { onSelectVideo(null) }
        LongVideoWatchScreen(
            video = resolvedActiveVideo,
            commentsState = videoCommentsState,
            onBack = { onSelectVideo(null) },
            onLike = { onLikeVideo(resolvedActiveVideo.id) },
            onAddComment = { text -> onAddVideoComment(resolvedActiveVideo, text) },
            modifier = modifier
        )
        return
    }

    var selectedFormat by remember { mutableStateOf(VideoFormat.SHORT) }
    var showUploadDialog by remember { mutableStateOf(false) }

    val shortsList = remember(allVideos) {
        allVideos.filter { it.videoFormat == VideoFormat.SHORT.name }
    }
    val longList = remember(allVideos) {
        allVideos.filter { it.videoFormat == VideoFormat.LONG.name }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showUploadDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("upload_video_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Publish Short or Long Video")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header + Format Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Drishya",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedFormat == VideoFormat.SHORT,
                        onClick = { selectedFormat = VideoFormat.SHORT },
                        label = { Text("Shorts") },
                        modifier = Modifier.testTag("shorts_tab_chip")
                    )
                    FilterChip(
                        selected = selectedFormat == VideoFormat.LONG,
                        onClick = { selectedFormat = VideoFormat.LONG },
                        label = { Text("Videos") },
                        modifier = Modifier.testTag("long_videos_tab_chip")
                    )
                }
            }

            if (publicVideosState is UiState.Loading && allVideos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (selectedFormat == VideoFormat.SHORT) {
                if (shortsList.isEmpty()) {
                    EmptyVideoState("Tap + to post a Short.")
                } else {
                    ShortsVerticalPager(
                        shorts = shortsList,
                        onLike = onLikeVideo,
                        onOpenDetail = onSelectVideo
                    )
                }
            } else {
                if (longList.isEmpty()) {
                    EmptyVideoState("Tap + to post a Video.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(longList, key = { it.id }) { video ->
                            LongVideoCard(
                                video = video,
                                onClick = { onSelectVideo(video) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showUploadDialog) {
        UploadVideoDialog(
            initialFormat = selectedFormat,
            myChannels = myChannels,
            onDismiss = { showUploadDialog = false },
            onPublish = { chId, chName, title, desc, fmt, dur, cat, preset, vis ->
                onCreateVideo(chId, chName, title, desc, fmt, dur, cat, preset, vis)
                selectedFormat = fmt
                showUploadDialog = false
            }
        )
    }
}

@Composable
private fun EmptyVideoState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.VideoLibrary,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShortsVerticalPager(
    shorts: List<VideoItem>,
    onLike: (String) -> Unit,
    onOpenDetail: (VideoItem) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { shorts.size })
    VerticalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) { page ->
        val item = shorts[page]
        var isPlaying by remember(item.id) { mutableStateOf(true) }
        var progress by remember(item.id) { mutableFloatStateOf(0.15f) }

        LaunchedEffect(item.id, isPlaying) {
            while (isPlaying) {
                delay(200L)
                progress = if (progress >= 1f) 0f else progress + 0.025f
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.extraLarge)
                .clickable { isPlaying = !isPlaying }
        ) {
            PresetMediaArtwork(
                presetName = item.thumbnailPreset,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Play / Pause state indicator
            if (!isPlaying) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(68.dp)
                        .align(Alignment.Center)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Play Short",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
            }

            // Top badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = SaffronPrimary,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "SHORT • ${item.durationLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                VisibilityBadge(item.visibility)
            }

            // Right action rail (YouTube Shorts style)
            val context = LocalContext.current
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = CircleShape
                    ) {
                        IconButton(
                            onClick = { onLike(item.id) },
                            modifier = Modifier.testTag("like_short_${item.id}")
                        ) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Like Short", tint = SaffronPrimary)
                        }
                    }
                    Text("${item.likesCount}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = CircleShape
                    ) {
                        IconButton(
                            onClick = { onOpenDetail(item) },
                            modifier = Modifier.testTag("discuss_short_${item.id}")
                        ) {
                            Icon(Icons.Filled.ChatBubbleOutline, contentDescription = "Comments & Watch", tint = Color.White)
                        }
                    }
                    Text("Discuss", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = CircleShape
                    ) {
                        IconButton(
                            onClick = {
                                PhoneContactsHelper.shareTextSafely(
                                    context = context,
                                    subject = item.title,
                                    text = "Watch \"${item.title}\" by ${item.channelName} on Narad Drishya"
                                )
                            },
                            modifier = Modifier.testTag("share_short_${item.id}")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share Short", tint = Color.White)
                        }
                    }
                    Text("Share", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }

            // Bottom metadata + playback bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.78f)
                    .padding(start = 16.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CitizenAvatar(name = item.channelName, size = 34.dp, verified = true)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.channelName,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                LinearProgressIndicator(
                    progress = { progress },
                    color = SaffronPrimary,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}

@Composable
private fun LongVideoCard(
    video: VideoItem,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("long_video_${video.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(195.dp)
            ) {
                PresetMediaArtwork(
                    presetName = video.thumbnailPreset,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize()
                )
                Icon(
                    imageVector = Icons.Filled.PlayCircleFilled,
                    contentDescription = "Play video",
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier
                        .size(54.dp)
                        .align(Alignment.Center)
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = video.durationLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                CitizenAvatar(name = video.channelName, size = 40.dp, verified = true)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${video.channelName} • ${video.viewsCount} views • ${video.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LongVideoWatchScreen(
    video: VideoItem,
    commentsState: UiState<List<CommentItem>>,
    onBack: () -> Unit,
    onLike: () -> Unit,
    onAddComment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0.1f) }
    var speedLabel by remember { mutableStateOf("1.0x") }
    var commentText by remember { mutableStateOf("") }
    val comments = (commentsState as? UiState.Success)?.data.orEmpty()

    LaunchedEffect(video.id, isPlaying, speedLabel) {
        val step = when (speedLabel) {
            "1.5x" -> 0.015f
            "2.0x" -> 0.02f
            else -> 0.01f
        }
        while (isPlaying) {
            delay(250L)
            progress = if (progress >= 1f) 0f else progress + step
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Player Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(235.dp)
                .background(Color.Black)
                .clickable { isPlaying = !isPlaying }
        ) {
            PresetMediaArtwork(
                presetName = video.thumbnailPreset,
                contentDescription = video.title,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = CircleShape,
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.Center)
            ) {
                IconButton(onClick = { isPlaying = !isPlaying }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPlaying) "Playing • ${video.durationLabel}" else "Paused • ${video.durationLabel}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                    TextButton(
                        onClick = {
                            speedLabel = when (speedLabel) {
                                "1.0x" -> "1.5x"
                                "1.5x" -> "2.0x"
                                else -> "1.0x"
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(speedLabel, color = Color.White)
                    }
                }
                Slider(
                    value = progress,
                    onValueChange = { progress = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(video.title, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${video.viewsCount} views • ${video.category} • ${video.channelName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val watchContext = LocalContext.current
                    Button(
                        onClick = onLike,
                        modifier = Modifier.testTag("watch_like_button")
                    ) {
                        Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Like (${video.likesCount})")
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            PhoneContactsHelper.shareTextSafely(
                                context = watchContext,
                                subject = video.title,
                                text = "Watch \"${video.title}\" on Narad Drishya"
                            )
                        },
                        modifier = Modifier.testTag("watch_share_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                    VisibilityBadge(video.visibility)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = video.description.ifBlank { "Published on Narad Drishya sovereign video network." },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            item {
                Text(
                    text = "Discussion (${comments.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(comments, key = { it.id }) { comment ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(comment.authorName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Add a public comment...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onAddComment(commentText)
                            commentText = ""
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Post comment")
                }
            }
        }
    }
}

@Composable
private fun UploadVideoDialog(
    initialFormat: VideoFormat,
    myChannels: List<Channel>,
    onDismiss: () -> Unit,
    onPublish: (
        channelId: String,
        channelName: String,
        title: String,
        description: String,
        format: VideoFormat,
        durationLabel: String,
        category: String,
        preset: ThumbnailPreset,
        visibility: PostVisibility
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var format by remember { mutableStateOf(initialFormat) }
    var duration by remember { mutableStateOf(if (initialFormat == VideoFormat.SHORT) "0:48" else "11:20") }
    var category by remember { mutableStateOf("Technology") }
    var preset by remember { mutableStateOf(ThumbnailPreset.SOVEREIGN) }
    var visibility by remember { mutableStateOf(PostVisibility.PUBLIC) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val selectedChannel = myChannels.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publish Video") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = format == VideoFormat.SHORT,
                        onClick = {
                            format = VideoFormat.SHORT
                            duration = "0:48"
                        },
                        label = { Text("Short (<60s)") }
                    )
                    FilterChip(
                        selected = format == VideoFormat.LONG,
                        onClick = {
                            format = VideoFormat.LONG
                            duration = "12:15"
                        },
                        label = { Text("Long Video") }
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationError = null
                    },
                    label = { Text("Video Title") },
                    singleLine = true,
                    isError = validationError != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_title_input")
                )
                if (!validationError.isNullOrBlank()) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Duration") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ThumbnailPreset.entries) { tp ->
                        FilterChip(
                            selected = preset == tp,
                            onClick = { preset = tp },
                            label = { Text(tp.name) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = visibility == PostVisibility.PUBLIC,
                        onClick = { visibility = PostVisibility.PUBLIC },
                        label = { Text("Public") },
                        leadingIcon = { Icon(Icons.Filled.Public, null, Modifier.size(14.dp)) }
                    )
                    FilterChip(
                        selected = visibility == PostVisibility.PRIVATE,
                        onClick = { visibility = PostVisibility.PRIVATE },
                        label = { Text("Private") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null, Modifier.size(14.dp)) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.trim().length < 2) {
                        validationError = "Title must be at least 2 characters"
                    } else {
                        onPublish(
                            selectedChannel?.id ?: "personal_studio",
                            selectedChannel?.name ?: "My Narad Studio",
                            title.trim(),
                            description.trim(),
                            format,
                            duration.ifBlank { if (format == VideoFormat.SHORT) "0:48" else "11:20" },
                            category.ifBlank { "Creator" },
                            preset,
                            visibility
                        )
                    }
                },
                modifier = Modifier.testTag("publish_video_button")
            ) {
                Text("Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
