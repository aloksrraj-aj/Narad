package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ConsentGrant
import com.example.data.model.ConsentScope
import com.example.data.model.MessagePolicy
import com.example.data.model.PostVisibility
import com.example.data.model.PrivacyAuditLog
import com.example.data.model.PrivacyVault
import com.example.data.model.SocialPost
import com.example.data.model.UserProfile
import com.example.data.model.safeEnumValue
import com.example.data.remote.PhoneContactsHelper
import com.example.data.remote.UiState
import com.example.ui.components.CitizenAvatar
import com.example.ui.components.WarrantCharterDialog
import com.example.ui.components.formatNaradTimestamp
import com.example.ui.theme.SovereignEmerald

@Composable
fun VaultProfileScreen(
    currentUserId: String,
    profileState: UiState<UserProfile?>,
    vaultState: UiState<PrivacyVault?>,
    consentGrantsState: UiState<List<ConsentGrant>>,
    auditLogsState: UiState<List<PrivacyAuditLog>>,
    myPostsState: UiState<List<SocialPost>>,
    contactsCount: Int,
    onOpenContactsSheet: (initialTab: Int) -> Unit,
    onUpdateProfile: (displayName: String, handle: String, bio: String, city: String, discoverable: Boolean) -> Unit,
    onUpdateVault: (
        defaultPostVisibility: PostVisibility,
        messagePolicy: MessagePolicy,
        readReceipts: Boolean,
        onlineStatus: Boolean,
        disappearingHours: Long,
        screenshotGuard: Boolean
    ) -> Unit,
    onAddConsentGrant: (name: String, handle: String, scope: ConsentScope) -> Unit,
    onToggleConsentGrant: (ConsentGrant) -> Unit,
    onDeleteConsentGrant: (String) -> Unit,
    onRunWarrantCheck: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile = (profileState as? UiState.Success)?.data
    val vault = (vaultState as? UiState.Success)?.data
    val grants = (consentGrantsState as? UiState.Success)?.data.orEmpty()
    val logs = (auditLogsState as? UiState.Success)?.data.orEmpty()
    val myPosts = (myPostsState as? UiState.Success)?.data.orEmpty()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddGrantDialog by remember { mutableStateOf(false) }
    var showWarrantDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("vault_lazy_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kavach",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sign Out")
                }
            }
        }

        // Citizen Identity Card
        item {
            if (profileState is UiState.Loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CitizenAvatar(
                                name = profile?.displayName ?: "Citizen",
                                size = 56.dp,
                                verified = profile?.verifiedCitizen ?: true
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile?.displayName ?: "Narad Citizen",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "${profile?.handle ?: "@citizen"} • ${profile?.city ?: "India"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { showEditProfileDialog = true },
                                modifier = Modifier.testTag("edit_profile_button")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit profile")
                            }
                        }
                        profile?.bio?.takeIf { it.isNotBlank() }?.let { bioText ->
                            Text(bioText, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatPill("${myPosts.size}", "Posts")
                            StatPill("${myPosts.count { it.visibility == PostVisibility.PRIVATE.name }}", "Vault")
                            StatPill("$contactsCount", "Contacts")
                        }
                    }
                }
            }
        }

        // Quick Connect: Add Contact, Invite Friend & Share Link
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onOpenContactsSheet(0) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_contacts_button")
                    ) {
                        Icon(Icons.Filled.Contacts, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Contact")
                    }
                    Button(
                        onClick = { onOpenContactsSheet(1) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_invite_button")
                    ) {
                        Icon(Icons.Filled.PersonAddAlt1, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Invite Friend")
                    }
                }
                OutlinedButton(
                    onClick = {
                        val code = PhoneContactsHelper.personalInviteCode(currentUserId)
                        val shareText = PhoneContactsHelper.buildConnectShareText(
                            senderName = profile?.displayName ?: "Narad Citizen",
                            senderHandle = profile?.handle ?: "@citizen",
                            inviteCode = code,
                            senderUid = currentUserId
                        )
                        PhoneContactsHelper.shareTextSafely(
                            context = context,
                            subject = "Connect with me on Narad",
                            text = shareText
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_share_connect_button")
                ) {
                    Icon(Icons.Filled.Share, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share My Narad Connect Link")
                }
            }
        }

        // Compact Lawful Due-Process Strip
        item {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.Gavel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Criminal Warrant Lock Only",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Region ${vault?.dataResidencyRegion ?: "IN-SOU-1"} • Zero trackers",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                    TextButton(onClick = { showWarrantDialog = true }) {
                        Text("Charter")
                    }
                }
            }
        }

        // Granular Privacy & Consent Matrix
        if (vault != null) {
            item {
                val defaultVis = safeEnumValue(vault.defaultPostVisibility, PostVisibility.PRIVATE)
                val msgPolicy = safeEnumValue(vault.messagePolicy, MessagePolicy.CONTACTS)

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Shield, null, tint = SovereignEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Privacy Controls", style = MaterialTheme.typography.titleMedium)
                        }

                        Text("Default Post Mode", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = defaultVis == PostVisibility.PRIVATE,
                                onClick = {
                                    onUpdateVault(
                                        PostVisibility.PRIVATE, msgPolicy, vault.readReceipts,
                                        vault.onlineStatus, vault.disappearingHours, vault.screenshotGuard
                                    )
                                },
                                label = { Text("Private") }
                            )
                            FilterChip(
                                selected = defaultVis == PostVisibility.PUBLIC,
                                onClick = {
                                    onUpdateVault(
                                        PostVisibility.PUBLIC, msgPolicy, vault.readReceipts,
                                        vault.onlineStatus, vault.disappearingHours, vault.screenshotGuard
                                    )
                                },
                                label = { Text("Public") }
                            )
                        }

                        Text("Direct Messages", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = msgPolicy == MessagePolicy.CONTACTS,
                                onClick = {
                                    onUpdateVault(
                                        defaultVis, MessagePolicy.CONTACTS, vault.readReceipts,
                                        vault.onlineStatus, vault.disappearingHours, vault.screenshotGuard
                                    )
                                },
                                label = { Text("Contacts") }
                            )
                            FilterChip(
                                selected = msgPolicy == MessagePolicy.EVERYONE,
                                onClick = {
                                    onUpdateVault(
                                        defaultVis, MessagePolicy.EVERYONE, vault.readReceipts,
                                        vault.onlineStatus, vault.disappearingHours, vault.screenshotGuard
                                    )
                                },
                                label = { Text("Anyone") }
                            )
                        }

                        HorizontalDivider()

                        PrivacyToggleRow(
                            title = "Screenshot Guard",
                            subtitle = "Block secret chat capture",
                            checked = vault.screenshotGuard,
                            onCheckedChange = {
                                onUpdateVault(
                                    defaultVis, msgPolicy, vault.readReceipts,
                                    vault.onlineStatus, vault.disappearingHours, it
                                )
                            }
                        )

                        PrivacyToggleRow(
                            title = "Hide Last Seen",
                            subtitle = "Mask active presence",
                            checked = !vault.onlineStatus,
                            onCheckedChange = { hide ->
                                onUpdateVault(
                                    defaultVis, msgPolicy, vault.readReceipts,
                                    !hide, vault.disappearingHours, vault.screenshotGuard
                                )
                            }
                        )

                        PrivacyToggleRow(
                            title = "Read Receipts",
                            subtitle = "Show checkmarks in chats",
                            checked = vault.readReceipts,
                            onCheckedChange = {
                                onUpdateVault(
                                    defaultVis, msgPolicy, it,
                                    vault.onlineStatus, vault.disappearingHours, vault.screenshotGuard
                                )
                            }
                        )
                    }
                }
            }
        }

        // Explicit Consent Grants Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Consent Grants", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = { showAddGrantDialog = true },
                    modifier = Modifier.testTag("add_consent_grant_button")
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Grant")
                }
            }
        }

        items(grants, key = { it.id }) { grant ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (grant.active) Icons.Filled.VerifiedUser else Icons.Filled.Lock,
                        contentDescription = null,
                        tint = if (grant.active) SovereignEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${grant.granteeName} (${grant.granteeHandle})", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${grant.scope} • ${if (grant.active) "ACTIVE" else "REVOKED"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (grant.active) SovereignEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = grant.active,
                        onCheckedChange = { onToggleConsentGrant(grant) }
                    )
                    IconButton(onClick = { onDeleteConsentGrant(grant.id) }) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = "Delete grant",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Audit Trail
        item {
            Text("Audit Log", style = MaterialTheme.typography.titleMedium)
        }

        items(logs, key = { it.id }) { log ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(log.action, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(log.detail, style = MaterialTheme.typography.bodySmall)
                        Text(
                            formatNaradTimestamp(log.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(64.dp)) }
    }

    if (showEditProfileDialog && profile != null) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, handle, bio, city, discoverable ->
                onUpdateProfile(name, handle, bio, city, discoverable)
                showEditProfileDialog = false
            }
        )
    }

    if (showAddGrantDialog) {
        AddConsentGrantDialog(
            onDismiss = { showAddGrantDialog = false },
            onGrant = { name, handle, scope ->
                onAddConsentGrant(name, handle, scope)
                showAddGrantDialog = false
            }
        )
    }

    if (showWarrantDialog) {
        WarrantCharterDialog(
            onDismiss = { showWarrantDialog = false },
            onRunAuditCheck = onRunWarrantCheck
        )
    }
}

@Composable
private fun StatPill(value: String, label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PrivacyToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(profile.displayName) }
    var handle by remember { mutableStateOf(profile.handle) }
    var bio by remember { mutableStateOf(profile.bio) }
    var city by remember { mutableStateOf(profile.city) }
    var discoverable by remember { mutableStateOf(profile.discoverable) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
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
                    label = { Text("Name") },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_profile_name_input")
                )
                OutlinedTextField(
                    value = handle,
                    onValueChange = {
                        handle = it
                        error = null
                    },
                    label = { Text("Handle") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_profile_handle_input")
                )
                if (!error.isNullOrBlank()) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Discoverable", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = discoverable, onCheckedChange = { discoverable = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    val cleanHandle = handle.trim().removePrefix("@")
                    if (cleanName.isEmpty() || cleanHandle.isEmpty()) {
                        error = "Name and handle are required"
                    } else {
                        onSave(cleanName, handle.trim(), bio, city, discoverable)
                    }
                },
                modifier = Modifier.testTag("edit_profile_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddConsentGrantDialog(
    onDismiss: () -> Unit,
    onGrant: (String, String, ConsentScope) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var scope by remember { mutableStateOf(ConsentScope.PRIVATE_POSTS) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Grant Access") },
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
                    label = { Text("Name") },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("consent_name_input")
                )
                OutlinedTextField(
                    value = handle,
                    onValueChange = {
                        handle = it
                        error = null
                    },
                    label = { Text("@handle") },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("consent_handle_input")
                )
                if (!error.isNullOrBlank()) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ConsentScope.entries.forEach { s ->
                        FilterChip(
                            selected = scope == s,
                            onClick = { scope = s },
                            label = { Text(s.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    val cleanHandle = handle.trim().removePrefix("@")
                    if (cleanName.isEmpty() || cleanHandle.isEmpty()) {
                        error = "Enter contact name and @handle"
                    } else {
                        onGrant(cleanName, handle.trim(), scope)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SovereignEmerald),
                modifier = Modifier.testTag("consent_grant_confirm_button")
            ) {
                Text("Grant")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
