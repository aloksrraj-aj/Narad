package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MediaPreset
import com.example.data.model.PostVisibility
import com.example.data.model.ThumbnailPreset
import com.example.ui.theme.AshokaIndigo
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.SovereignEmerald
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CitizenAvatar(
    name: String,
    size: Dp = 44.dp,
    verified: Boolean = false,
    hasStoryRing: Boolean = false
) {
    val initials = name.trim()
        .split(Regex("\\s+"))
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "N" }

    val ringModifier = if (hasStoryRing) {
        Modifier.border(
            width = 2.5.dp,
            brush = Brush.linearGradient(listOf(SaffronPrimary, SovereignEmerald)),
            shape = CircleShape
        ).padding(3.dp)
    } else {
        Modifier
    }

    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(size)
                .then(ringModifier)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            SaffronPrimary.copy(alpha = 0.92f),
                            AshokaIndigo
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        if (verified) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = "Verified citizen",
                    tint = SovereignEmerald,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun PresetMediaArtwork(
    presetName: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val resId = when (presetName) {
        MediaPreset.HERITAGE.name, ThumbnailPreset.HERITAGE.name -> R.drawable.img_thumb_heritage
        MediaPreset.SOVEREIGN.name, ThumbnailPreset.SOVEREIGN.name -> R.drawable.img_thumb_sovereignty
        MediaPreset.STUDIO.name, ThumbnailPreset.STUDIO.name -> R.drawable.img_thumb_sovereignty
        ThumbnailPreset.BANNER.name -> R.drawable.img_channel_banner
        else -> R.drawable.img_narad_sovereign_hero
    }
    Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

@Composable
fun SovereigntySealChip(
    label: String,
    isPrivate: Boolean = true,
    modifier: Modifier = Modifier
) {
    val container = if (isPrivate) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val content = if (isPrivate) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        modifier = modifier,
        color = container,
        shape = RoundedCornerShape(50),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (isPrivate) Icons.Filled.Lock else Icons.Filled.Public,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VisibilityBadge(visibility: String) {
    val isPriv = visibility == PostVisibility.PRIVATE.name
    SovereigntySealChip(
        label = if (isPriv) "Private Vault" else "Public",
        isPrivate = isPriv
    )
}

fun formatNaradTimestamp(timestamp: Timestamp?): String {
    val date = (timestamp ?: Timestamp.now()).toDate()
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(date)
}

fun formatNaradTimeShort(timestamp: Timestamp?): String {
    val date = (timestamp ?: Timestamp.now()).toDate()
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(date)
}

@Composable
fun WarrantCharterDialog(
    onDismiss: () -> Unit,
    onRunAuditCheck: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Gavel,
                contentDescription = "Judicial warrant lock",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.privacy_charter_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.privacy_charter_body),
                    style = MaterialTheme.typography.bodyMedium
                )
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Active Lawful Holds: 0\nData Residency: India (IN-SOU-1)\nThird-Party Trackers: Blocked",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onRunAuditCheck()
                onDismiss()
            }) {
                Text("Log Integrity Check")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
