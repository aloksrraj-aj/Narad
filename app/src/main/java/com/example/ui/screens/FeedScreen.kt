package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.CommentItem
import com.example.data.model.MediaPreset
import com.example.data.model.PostKind
import com.example.data.model.PostVisibility
import com.example.data.model.SocialPost
import com.example.data.remote.UiState
import com.example.ui.components.CitizenAvatar
import com.example.ui.components.PresetMediaArtwork
import com.example.ui.components.VisibilityBadge
import com.example.ui.components.formatNaradTimestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    currentUserId: String,
    currentUserName: String,
    currentUserHandle: String,
    defaultPostVisibility: PostVisibility,
    publicPostsState: UiState<List<SocialPost>>,
    myPostsState: UiState<List<SocialPost>>,
    localPickedMediaUris: Map<String, String>,
    activeCommentPost: SocialPost?,
    commentsState: UiState<List<CommentItem>>,
    onCreatePost: (
        caption: String,
        location: String,
        kind: PostKind,
        visibility: PostVisibility,
        preset: MediaPreset,
        localMediaUri: String?
    ) -> Unit,
    onLikePost: (String) -> Unit,
    onToggleVisibility: (SocialPost) -> Unit,
    onDeletePost: (String) -> Unit,
    onOpenComments: (SocialPost?) -> Unit,
    onAddComment: (SocialPost, String) -> Unit,
    onShareToChats: (SocialPost) -> Unit,
    modifier: Modifier = Modifier
) {
    var feedScope by remember { mutableStateOf("COMMUNITY") } // COMMUNITY vs MY_VAULT
    var showComposer by remember { mutableStateOf(false) }
    var composerDefaultKind by remember { mutableStateOf(PostKind.POST) }
    var viewingStory by remember { mutableStateOf<SocialPost?>(null) }

    val publicPosts = (publicPostsState as? UiState.Success)?.data.orEmpty()
    val myPosts = (myPostsState as? UiState.Success)?.data.orEmpty()
    val stories = remember(publicPosts, myPosts) {
        (myPosts + publicPosts).distinctBy { it.id }.filter { it.postKind == PostKind.STORY.name }
    }
    val displayedPosts = remember(feedScope, publicPosts, myPosts) {
        val source = if (feedScope == "MY_VAULT") myPosts else publicPosts
        source.filter { it.postKind == PostKind.POST.name }
    }
    val resolvedCommentPost = remember(activeCommentPost, publicPosts, myPosts) {
        activeCommentPost?.let { target ->
            (myPosts + publicPosts).firstOrNull { it.id == target.id } ?: target
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    composerDefaultKind = PostKind.POST
                    showComposer = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_post_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Post daily activity")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Samvad",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = {
                            composerDefaultKind = PostKind.STORY
                            showComposer = true
                        },
                        modifier = Modifier.testTag("create_story_button")
                    ) {
                        Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Story")
                    }
                }
            }

            // Stories strip
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    composerDefaultKind = PostKind.STORY
                                    showComposer = true
                                }
                                .padding(4.dp)
                        ) {
                            CitizenAvatar(name = "+", size = 56.dp, hasStoryRing = true)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your Story",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }
                    items(stories, key = { it.id }) { story ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable { viewingStory = story }
                                .padding(4.dp)
                        ) {
                            CitizenAvatar(
                                name = story.authorName,
                                size = 56.dp,
                                verified = true,
                                hasStoryRing = true
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = story.authorName.split(" ").first(),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Scope Switcher
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = feedScope == "COMMUNITY",
                        onClick = { feedScope = "COMMUNITY" },
                        label = { Text("Feed") },
                        leadingIcon = { Icon(Icons.Filled.Public, null, Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("feed_scope_public")
                    )
                    FilterChip(
                        selected = feedScope == "MY_VAULT",
                        onClick = { feedScope = "MY_VAULT" },
                        label = { Text("My Vault") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null, Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("feed_scope_vault")
                    )
                }
            }

            if (publicPostsState is UiState.Loading && feedScope == "COMMUNITY") {
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
            } else if (displayedPosts.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Nothing here yet", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Tap + to share a moment.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(displayedPosts, key = { it.id }) { post ->
                    SocialPostCard(
                        post = post,
                        localPhotoUri = localPickedMediaUris[post.id],
                        isOwner = post.userId == currentUserId,
                        onLike = { onLikePost(post.id) },
                        onComment = { onOpenComments(post) },
                        onToggleVisibility = { onToggleVisibility(post) },
                        onDelete = { onDeletePost(post.id) },
                        onShare = { onShareToChats(post) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showComposer) {
        val initialVisibility = if (feedScope == "MY_VAULT") defaultPostVisibility else PostVisibility.PUBLIC
        CreatePostDialog(
            initialKind = composerDefaultKind,
            defaultVisibility = initialVisibility,
            onDismiss = { showComposer = false },
            onPublish = { caption, location, kind, visibility, preset, localUri ->
                onCreatePost(caption, location, kind, visibility, preset, localUri)
                if (kind == PostKind.POST) {
                    feedScope = if (visibility == PostVisibility.PRIVATE) "MY_VAULT" else "COMMUNITY"
                }
                showComposer = false
            }
        )
    }

    viewingStory?.let { story ->
        val localUri = localPickedMediaUris[story.id]
        AlertDialog(
            onDismissRequest = { viewingStory = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CitizenAvatar(name = story.authorName, size = 36.dp, verified = true)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(story.authorName, style = MaterialTheme.typography.titleMedium)
                        Text(story.authorHandle, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!localUri.isNullOrBlank()) {
                        AsyncImage(
                            model = Uri.parse(localUri),
                            contentDescription = story.caption,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )
                    } else if (story.mediaPreset != MediaPreset.NONE.name) {
                        PresetMediaArtwork(
                            presetName = story.mediaPreset,
                            contentDescription = story.caption,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(MaterialTheme.shapes.medium)
                        )
                    }
                    Text(story.caption, style = MaterialTheme.typography.bodyLarge)
                    VisibilityBadge(story.visibility)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingStory = null }) { Text("Close") }
            }
        )
    }

    if (resolvedCommentPost != null) {
        ModalBottomSheet(onDismissRequest = { onOpenComments(null) }) {
            PostCommentsSheetContent(
                post = resolvedCommentPost,
                commentsState = commentsState,
                onAddComment = { text -> onAddComment(resolvedCommentPost, text) }
            )
        }
    }
}

@Composable
private fun SocialPostCard(
    post: SocialPost,
    localPhotoUri: String?,
    isOwner: Boolean,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CitizenAvatar(name = post.authorName, size = 42.dp, verified = true)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = post.authorHandle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (post.location.isNotBlank()) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = post.location,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                VisibilityBadge(post.visibility)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = post.caption,
                style = MaterialTheme.typography.bodyLarge
            )

            if (!localPhotoUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = Uri.parse(localPhotoUri),
                    contentDescription = post.caption,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            } else if (post.mediaPreset != MediaPreset.NONE.name) {
                Spacer(modifier = Modifier.height(12.dp))
                PresetMediaArtwork(
                    presetName = post.mediaPreset,
                    contentDescription = post.caption,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = formatNaradTimestamp(post.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = onLike,
                        modifier = Modifier.testTag("like_post_${post.id}")
                    ) {
                        Icon(
                            imageVector = if (post.likesCount > 0) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.likesCount > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${post.likesCount}")
                    }
                    TextButton(
                        onClick = onComment,
                        modifier = Modifier.testTag("comment_post_${post.id}")
                    ) {
                        Icon(
                            Icons.Filled.ChatBubbleOutline,
                            contentDescription = "Comments",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${post.commentsCount}")
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Filled.Share, contentDescription = "Share to chat", modifier = Modifier.size(18.dp))
                    }
                }

                if (isOwner) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onToggleVisibility) {
                            Text(
                                if (post.visibility == PostVisibility.PRIVATE.name) "Make Public"
                                else "Lock Private"
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = "Delete post",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostCommentsSheetContent(
    post: SocialPost,
    commentsState: UiState<List<CommentItem>>,
    onAddComment: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    val comments = (commentsState as? UiState.Success)?.data.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Comments (${comments.size})", style = MaterialTheme.typography.titleMedium)
        if (comments.isEmpty()) {
            Text(
                "Be the first to comment.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.height(220.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(comments, key = { it.id }) { item ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(item.authorName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(item.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                placeholder = { Text("Write a comment...") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("post_comment_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (commentText.isNotBlank()) {
                        onAddComment(commentText)
                        commentText = ""
                    }
                },
                modifier = Modifier.testTag("send_comment_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Post comment")
            }
        }
    }
}

@Composable
private fun CreatePostDialog(
    initialKind: PostKind,
    defaultVisibility: PostVisibility,
    onDismiss: () -> Unit,
    onPublish: (
        caption: String,
        location: String,
        kind: PostKind,
        visibility: PostVisibility,
        preset: MediaPreset,
        localMediaUri: String?
    ) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Bengaluru, IN") }
    var kind by remember { mutableStateOf(initialKind) }
    var visibility by remember { mutableStateOf(defaultVisibility) }
    var preset by remember { mutableStateOf(MediaPreset.STUDIO) }
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedPhotoUri = uri
            validationError = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (kind == PostKind.STORY) "New Story" else "New Post") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kind == PostKind.POST,
                        onClick = { kind = PostKind.POST },
                        label = { Text("Post") }
                    )
                    FilterChip(
                        selected = kind == PostKind.STORY,
                        onClick = { kind = PostKind.STORY },
                        label = { Text("Story") }
                    )
                }
                OutlinedTextField(
                    value = caption,
                    onValueChange = {
                        caption = it
                        validationError = null
                    },
                    label = { Text("What's new?") },
                    minLines = 2,
                    maxLines = 4,
                    isError = validationError != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_caption_input")
                )
                if (!validationError.isNullOrBlank()) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Text("Visibility", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = visibility == PostVisibility.PUBLIC,
                        onClick = { visibility = PostVisibility.PUBLIC },
                        label = { Text("Public") },
                        leadingIcon = { Icon(Icons.Filled.Public, null, Modifier.size(14.dp)) },
                        modifier = Modifier.testTag("post_vis_public")
                    )
                    FilterChip(
                        selected = visibility == PostVisibility.PRIVATE,
                        onClick = { visibility = PostVisibility.PRIVATE },
                        label = { Text("Private") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null, Modifier.size(14.dp)) },
                        modifier = Modifier.testTag("post_vis_private")
                    )
                }
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_photo_button")
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (pickedPhotoUri != null) "Photo Attached" else "Pick Photo")
                }
                if (pickedPhotoUri != null) {
                    AsyncImage(
                        model = pickedPhotoUri,
                        contentDescription = "Selected photo preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop
                    )
                }
                Text("Backdrop", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(MediaPreset.entries) { p ->
                        FilterChip(
                            selected = preset == p,
                            onClick = { preset = p },
                            label = { Text(p.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalCaption = caption.trim().ifEmpty {
                        if (pickedPhotoUri != null) "Shared a photo" else ""
                    }
                    if (finalCaption.isEmpty()) {
                        validationError = "Write something or pick a photo"
                    } else {
                        onPublish(
                            finalCaption,
                            location,
                            kind,
                            visibility,
                            preset,
                            pickedPhotoUri?.toString()
                        )
                    }
                },
                modifier = Modifier.testTag("publish_post_button")
            ) {
                Text("Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
