package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.core.content.ContextCompat
import com.example.data.model.ContactItem
import com.example.data.model.InviteRecord
import com.example.data.model.UserProfile
import com.example.data.remote.PhoneContactEntry
import com.example.data.remote.PhoneContactsHelper
import com.example.data.remote.UiState
import com.example.ui.components.CitizenAvatar
import com.example.ui.theme.AshokaIndigo
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.SovereignEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsInviteSheet(
    initialTab: Int,
    currentUserId: String,
    currentUserName: String,
    currentUserHandle: String,
    contactsState: UiState<List<ContactItem>>,
    invitesState: UiState<List<InviteRecord>>,
    discoverableCitizensState: UiState<List<UserProfile>>,
    onDismiss: () -> Unit,
    onAddContact: (displayName: String, handle: String, note: String, contactUid: String?) -> Unit,
    onDeleteContact: (String) -> Unit,
    onStartChat: (displayName: String, contactUid: String?) -> Unit,
    onCreateInvite: (recipientName: String, channel: String, presetCode: String?, onCreated: (InviteRecord) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var phoneContacts by remember { mutableStateOf<List<PhoneContactEntry>>(emptyList()) }
    var loadingPhoneContacts by remember { mutableStateOf(false) }
    var permissionDeniedOnce by remember { mutableStateOf(false) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasContactsPermission = granted
        if (!granted) permissionDeniedOnce = true
    }

    LaunchedEffect(hasContactsPermission) {
        if (hasContactsPermission) {
            loadingPhoneContacts = true
            phoneContacts = PhoneContactsHelper.fetchPhoneContacts(context)
            loadingPhoneContacts = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Add Contact") },
                    leadingIcon = { Icon(Icons.Filled.Contacts, null, Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("sheet_tab_contacts")
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("Invite Friend") },
                    leadingIcon = { Icon(Icons.Filled.PersonAddAlt1, null, Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("sheet_tab_invite")
                )
            }

            if (selectedTab == 0) {
                ContactsTabContent(
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    currentUserHandle = currentUserHandle,
                    contactsState = contactsState,
                    discoverableCitizensState = discoverableCitizensState,
                    phoneContacts = phoneContacts,
                    hasContactsPermission = hasContactsPermission,
                    loadingPhoneContacts = loadingPhoneContacts,
                    permissionDeniedOnce = permissionDeniedOnce,
                    onRequestPhoneContacts = {
                        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    },
                    onAddContact = onAddContact,
                    onDeleteContact = onDeleteContact,
                    onStartChat = { name, uid ->
                        onStartChat(name, uid)
                        onDismiss()
                    },
                    onSwitchToInvite = { selectedTab = 1 }
                )
            } else {
                InviteTabContent(
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    currentUserHandle = currentUserHandle,
                    invitesState = invitesState,
                    discoverableCitizensState = discoverableCitizensState,
                    phoneContacts = phoneContacts,
                    hasContactsPermission = hasContactsPermission,
                    loadingPhoneContacts = loadingPhoneContacts,
                    permissionDeniedOnce = permissionDeniedOnce,
                    onRequestPhoneContacts = {
                        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    },
                    onAddContact = onAddContact,
                    onStartChat = { name, uid ->
                        onStartChat(name, uid)
                        onDismiss()
                    },
                    onCreateInvite = onCreateInvite
                )
            }
        }
    }
}

private fun dispatchShareSafely(context: Context, subject: String, text: String): String {
    val result = PhoneContactsHelper.shareTextSafely(context, subject, text)
    return if (result.chooserLaunched) {
        "Share sheet opened & link copied"
    } else {
        "Invite link copied to clipboard"
    }
}

@Composable
private fun ContactsTabContent(
    currentUserId: String,
    currentUserName: String,
    currentUserHandle: String,
    contactsState: UiState<List<ContactItem>>,
    discoverableCitizensState: UiState<List<UserProfile>>,
    phoneContacts: List<PhoneContactEntry>,
    hasContactsPermission: Boolean,
    loadingPhoneContacts: Boolean,
    permissionDeniedOnce: Boolean,
    onRequestPhoneContacts: () -> Unit,
    onAddContact: (String, String, String, String?) -> Unit,
    onDeleteContact: (String) -> Unit,
    onStartChat: (String, String?) -> Unit,
    onSwitchToInvite: () -> Unit
) {
    val context = LocalContext.current
    val contacts = (contactsState as? UiState.Success)?.data.orEmpty()
    val citizens = (discoverableCitizensState as? UiState.Success)?.data
        ?.filter { it.userId != currentUserId }
        .orEmpty()
    val savedHandles = remember(contacts) { contacts.map { it.handle.lowercase() }.toSet() }
    val personalCode = remember(currentUserId) { PhoneContactsHelper.personalInviteCode(currentUserId) }

    var showAddForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("contacts_tab_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        item(key = "contacts_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Contacts (${contacts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = onSwitchToInvite) {
                        Text("Invite Friend")
                    }
                    FilledTonalButton(
                        onClick = { showAddForm = !showAddForm },
                        modifier = Modifier.testTag("toggle_add_contact_button")
                    ) {
                        Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (showAddForm) "Hide" else "Add")
                    }
                }
            }
        }

        if (showAddForm) {
            item(key = "add_contact_form") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    formError = null
                                },
                                label = { Text("Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_contact_name_input")
                            )
                            OutlinedTextField(
                                value = handle,
                                onValueChange = {
                                    handle = it
                                    formError = null
                                },
                                label = { Text("@handle or phone") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_contact_handle_input")
                            )
                        }
                        if (!formError.isNullOrBlank()) {
                            Text(
                                text = formError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                label = { Text("Note (optional)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val cleanName = name.trim()
                                    val cleanHandle = handle.trim()
                                    if (cleanName.isEmpty() || cleanHandle.removePrefix("@").isEmpty()) {
                                        formError = "Enter name & handle"
                                    } else {
                                        onAddContact(cleanName, cleanHandle, note, null)
                                        name = ""
                                        handle = ""
                                        note = ""
                                        formError = null
                                        showAddForm = false
                                    }
                                },
                                modifier = Modifier.testTag("save_contact_button")
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }

        if (citizens.isNotEmpty()) {
            item(key = "existing_citizens_strip") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Existing Narad Users",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                dispatchShareSafely(
                                    context,
                                    "Connect on Narad",
                                    PhoneContactsHelper.buildConnectShareText(
                                        currentUserName,
                                        currentUserHandle,
                                        personalCode,
                                        currentUserId
                                    )
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Filled.Link, null, Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share My Link")
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(citizens, key = { it.userId }) { citizen ->
                            val alreadySaved = citizen.handle.lowercase() in savedHandles
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.medium,
                                tonalElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CitizenAvatar(name = citizen.displayName, size = 34.dp, verified = citizen.verifiedCitizen)
                                    Column {
                                        Text(
                                            citizen.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            citizen.handle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (alreadySaved) {
                                        FilledTonalButton(
                                            onClick = { onStartChat(citizen.displayName, citizen.userId) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Chat")
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                onAddContact(citizen.displayName, citizen.handle, citizen.city, citizen.userId)
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("+ Add")
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            dispatchShareSafely(
                                                context,
                                                "Connect with ${citizen.displayName}",
                                                PhoneContactsHelper.buildConnectShareText(
                                                    currentUserName,
                                                    currentUserHandle,
                                                    personalCode,
                                                    currentUserId
                                                )
                                            )
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = "Share connect link", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!hasContactsPermission) {
            item(key = "contacts_permission_card") {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (permissionDeniedOnce) "Allow Contacts to import from phone" else "Import contacts from phone",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            onClick = onRequestPhoneContacts,
                            modifier = Modifier.testTag("fetch_phone_contacts_button")
                        ) {
                            Text("Import")
                        }
                    }
                }
            }
        } else if (loadingPhoneContacts) {
            item(key = "contacts_loading_phone") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Reading phone contacts...", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

            items(contacts, key = { it.id }) { contact ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CitizenAvatar(name = contact.displayName, size = 42.dp, verified = contact.verified)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = contact.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (contact.verified) {
                                    Icon(
                                        Icons.Filled.Verified,
                                        contentDescription = "Verified",
                                        tint = SovereignEmerald,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = listOf(contact.handle, contact.note)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                onStartChat(
                                    contact.displayName,
                                    contact.contactUid.takeIf { it.isNotBlank() }
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("chat_contact_${contact.id}")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, null, Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Chat")
                        }
                        IconButton(onClick = { onDeleteContact(contact.id) }) {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = "Remove contact",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (phoneContacts.isNotEmpty()) {
                item {
                    Text(
                        text = "Phone Contacts (${phoneContacts.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(phoneContacts.take(30), key = { "phone_${it.id}" }) { entry ->
                    val alreadySaved = entry.suggestedHandle.lowercase() in savedHandles ||
                        contacts.any { it.displayName.equals(entry.displayName, ignoreCase = true) }
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CitizenAvatar(name = entry.displayName, size = 38.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                Text(
                                    entry.phoneNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (alreadySaved) {
                                TextButton(onClick = { onStartChat(entry.displayName, null) }) {
                                    Text("Chat")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        onAddContact(entry.displayName, entry.suggestedHandle, entry.phoneNumber, null)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("+ Add")
                                }
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun InviteTabContent(
    currentUserId: String,
    currentUserName: String,
    currentUserHandle: String,
    invitesState: UiState<List<InviteRecord>>,
    discoverableCitizensState: UiState<List<UserProfile>>,
    phoneContacts: List<PhoneContactEntry>,
    hasContactsPermission: Boolean,
    loadingPhoneContacts: Boolean,
    permissionDeniedOnce: Boolean,
    onRequestPhoneContacts: () -> Unit,
    onAddContact: (String, String, String, String?) -> Unit,
    onStartChat: (String, String?) -> Unit,
    onCreateInvite: (recipientName: String, channel: String, presetCode: String?, onCreated: (InviteRecord) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val invites = (invitesState as? UiState.Success)?.data.orEmpty()
    val citizens = (discoverableCitizensState as? UiState.Success)?.data
        ?.filter { it.userId != currentUserId }
        .orEmpty()
    val personalCode = remember(currentUserId) { PhoneContactsHelper.personalInviteCode(currentUserId) }
    var customDownloadUrl by remember { mutableStateOf(PhoneContactsHelper.getCustomDownloadUrl(context)) }
    var showDownloadUrlEditor by remember { mutableStateOf(false) }
    var downloadUrlDraft by remember(customDownloadUrl) { mutableStateOf(customDownloadUrl) }

    val appInviteLink = remember(personalCode, currentUserId, currentUserName, currentUserHandle) {
        PhoneContactsHelper.buildAppInviteLink(personalCode, currentUserId, currentUserName, currentUserHandle)
    }
    var friendName by remember { mutableStateOf("") }
    var redeemInput by remember { mutableStateOf("") }
    var redeemFeedback by remember { mutableStateOf<String?>(null) }
    var shareStatus by remember { mutableStateOf<String?>(null) }
    var phoneFilter by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }

    val filteredPhoneContacts = remember(phoneContacts, phoneFilter) {
        if (phoneFilter.isBlank()) phoneContacts
        else phoneContacts.filter {
            it.displayName.contains(phoneFilter, ignoreCase = true) ||
                it.phoneNumber.contains(phoneFilter, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("invite_tab_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item(key = "invite_hero_card") {
            // Gradient Invite Card with App Link + Optional Free APK Link
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(Brush.linearGradient(listOf(SaffronPrimary, AshokaIndigo)))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Invite & Connect",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = customDownloadUrl.ifBlank { "Send Narad.apk directly, or attach a free GitHub/Drive URL" },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.88f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = personalCode,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val shareText = PhoneContactsHelper.buildInviteShareText(
                                    senderName = currentUserName,
                                    recipientName = null,
                                    inviteCode = personalCode,
                                    senderUid = currentUserId,
                                    senderHandle = currentUserHandle,
                                    customDownloadUrl = customDownloadUrl
                                )
                                val result = PhoneContactsHelper.shareInstalledApkSafely(context, shareText)
                                shareStatus = if (result.sharedApkFile) {
                                    "Sending Narad.apk file & invite code"
                                } else {
                                    "Share sheet opened & invite copied"
                                }
                                copied = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = AshokaIndigo
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("send_apk_button")
                        ) {
                            Icon(Icons.Filled.Android, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Send APK", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                val shareText = PhoneContactsHelper.buildInviteShareText(
                                    senderName = currentUserName,
                                    recipientName = null,
                                    inviteCode = personalCode,
                                    senderUid = currentUserId,
                                    senderHandle = currentUserHandle,
                                    customDownloadUrl = customDownloadUrl
                                )
                                shareStatus = dispatchShareSafely(context, "Join Narad", shareText)
                                copied = true
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("share_invite_button")
                        ) {
                            Icon(Icons.Filled.Share, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                val shareText = PhoneContactsHelper.buildInviteShareText(
                                    senderName = currentUserName,
                                    recipientName = null,
                                    inviteCode = personalCode,
                                    senderUid = currentUserId,
                                    senderHandle = currentUserHandle,
                                    customDownloadUrl = customDownloadUrl
                                )
                                PhoneContactsHelper.copyToClipboard(context, "Narad Link", shareText)
                                copied = true
                                shareStatus = "Invite copied to clipboard"
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                null,
                                Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (copied) "Copied" else "Copy", maxLines = 1)
                        }
                    }
                    TextButton(
                        onClick = { showDownloadUrlEditor = !showDownloadUrlEditor },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.testTag("edit_download_link_button")
                    ) {
                        Icon(Icons.Filled.Link, null, Modifier.size(14.dp), tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (customDownloadUrl.isBlank()) "Add Free APK Link (GitHub / Drive)" else "Edit APK Download Link",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }

        if (!shareStatus.isNullOrBlank()) {
            item(key = "share_status_banner") {
                Surface(
                    color = SovereignEmerald.copy(alpha = 0.14f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = shareStatus ?: "",
                        style = MaterialTheme.typography.labelMedium,
                        color = SovereignEmerald,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        if (showDownloadUrlEditor) {
            item(key = "download_url_editor") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = downloadUrlDraft,
                            onValueChange = { downloadUrlDraft = it },
                            label = { Text("Free APK / Store Link (optional)") },
                            placeholder = { Text("https://github.com/.../narad.apk") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("download_url_input")
                        )
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { showDownloadUrlEditor = false }) { Text("Cancel") }
                            Button(
                                onClick = {
                                    val cleanUrl = downloadUrlDraft.trim()
                                    PhoneContactsHelper.saveCustomDownloadUrl(context, cleanUrl)
                                    customDownloadUrl = cleanUrl
                                    showDownloadUrlEditor = false
                                    shareStatus = if (cleanUrl.isBlank()) "APK link cleared" else "APK link attached to invites"
                                },
                                modifier = Modifier.testTag("save_download_link_button")
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }

        item(key = "manual_friend_invite_row") {
            // Quick manual friend invite (fires share chooser + clipboard immediately, saves to Firestore in background)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = friendName,
                    onValueChange = { friendName = it },
                    placeholder = { Text("Friend's name") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("invite_friend_name_input")
                )
                Button(
                    onClick = {
                        val clean = friendName.trim().ifEmpty { "Friend" }
                        val code = PhoneContactsHelper.generateInviteCode()
                        val shareText = PhoneContactsHelper.buildInviteShareText(
                            senderName = currentUserName,
                            recipientName = clean,
                            inviteCode = code,
                            senderUid = currentUserId,
                            senderHandle = currentUserHandle,
                            customDownloadUrl = customDownloadUrl
                        )
                        friendName = ""
                        shareStatus = dispatchShareSafely(context, "Invite $clean to Narad", shareText)
                        onCreateInvite(clean, "SHARE", code) {}
                    },
                    modifier = Modifier.testTag("send_personal_invite_button")
                ) {
                    Text("Invite")
                }
            }
        }

        item(key = "redeem_link_row") {
            // Accept / Redeem pasted Narad invite link or code
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = redeemInput,
                    onValueChange = {
                        redeemInput = it
                        redeemFeedback = null
                    },
                    placeholder = { Text("Paste narad:// link or NRD-code") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("join_link_or_code_input")
                )
                FilledTonalButton(
                    onClick = {
                        val parsed = PhoneContactsHelper.parseDeepLinkText(redeemInput)
                        if (parsed == null) {
                            redeemFeedback = "Paste a valid Narad link or NRD-XXXX code"
                        } else {
                            val contactName = parsed.senderName.ifBlank {
                                parsed.senderHandle.removePrefix("@").ifBlank { "Invited Friend (${parsed.code})" }
                            }
                            val contactHandle = parsed.senderHandle.ifBlank {
                                "@${parsed.code.lowercase().replace("-", "_")}"
                            }
                            val targetUid = parsed.senderUid.takeIf { it.isNotBlank() && it != currentUserId }
                            onAddContact(contactName, contactHandle, "Joined via ${parsed.code}", targetUid)
                            redeemInput = ""
                            redeemFeedback = "Connected with $contactName"
                            if (targetUid != null || parsed.senderName.isNotBlank()) {
                                onStartChat(contactName, targetUid)
                            }
                        }
                    },
                    modifier = Modifier.testTag("redeem_link_button")
                ) {
                    Text("Connect")
                }
            }
        }

        if (!redeemFeedback.isNullOrBlank()) {
            item(key = "redeem_feedback_row") {
                Text(
                    text = redeemFeedback ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = SovereignEmerald
                )
            }
        }

        item(key = "phone_contacts_permission_or_filter") {
            // Phone Contacts Fetch / List inside Invite Tab
            if (!hasContactsPermission) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Fetch Phone Contacts",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (permissionDeniedOnce) "Grant Contacts permission in prompt or settings" else "Send app link directly to saved contacts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Button(
                            onClick = onRequestPhoneContacts,
                            modifier = Modifier.testTag("invite_fetch_contacts_button")
                        ) {
                            Text("Load")
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = phoneFilter,
                    onValueChange = { phoneFilter = it },
                    placeholder = { Text("Filter phone contacts...") },
                    leadingIcon = { Icon(Icons.Filled.Search, null, Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
            if (loadingPhoneContacts) {
                item {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Loading saved contacts...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (filteredPhoneContacts.isNotEmpty()) {
                item {
                    Text(
                        text = "From Phone (${filteredPhoneContacts.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(filteredPhoneContacts, key = { "inv_ph_${it.id}" }) { contact ->
                    val existingUser = citizens.firstOrNull {
                        it.displayName.equals(contact.displayName, ignoreCase = true)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CitizenAvatar(
                                name = contact.displayName,
                                size = 40.dp,
                                verified = existingUser != null
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (existingUser != null) "On Narad • ${existingUser.handle}" else contact.phoneNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (existingUser != null) SovereignEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            if (existingUser != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilledTonalButton(
                                        onClick = { onStartChat(existingUser.displayName, existingUser.userId) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Chat")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            shareStatus = dispatchShareSafely(
                                                context,
                                                "Connect on Narad",
                                                PhoneContactsHelper.buildConnectShareText(
                                                    currentUserName,
                                                    currentUserHandle,
                                                    personalCode,
                                                    currentUserId
                                                )
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Share")
                                    }
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val code = PhoneContactsHelper.generateInviteCode()
                                            val shareText = PhoneContactsHelper.buildInviteShareText(
                                                senderName = currentUserName,
                                                recipientName = contact.displayName,
                                                inviteCode = code,
                                                senderUid = currentUserId,
                                                senderHandle = currentUserHandle,
                                                customDownloadUrl = customDownloadUrl
                                            )
                                            PhoneContactsHelper.openDirectSmsInvite(
                                                context = context,
                                                phoneNumber = contact.phoneNumber,
                                                message = shareText
                                            )
                                            shareStatus = "SMS opened for ${contact.displayName} & copied"
                                            onAddContact(
                                                contact.displayName,
                                                contact.suggestedHandle,
                                                contact.phoneNumber,
                                                null
                                            )
                                            onCreateInvite(contact.displayName, "SMS", code) {}
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Filled.Sms, null, Modifier.size(14.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("SMS")
                                    }
                                    Button(
                                        onClick = {
                                            val code = PhoneContactsHelper.generateInviteCode()
                                            val shareText = PhoneContactsHelper.buildInviteShareText(
                                                senderName = currentUserName,
                                                recipientName = contact.displayName,
                                                inviteCode = code,
                                                senderUid = currentUserId,
                                                senderHandle = currentUserHandle,
                                                customDownloadUrl = customDownloadUrl
                                            )
                                            PhoneContactsHelper.openWhatsAppInvite(
                                                context = context,
                                                phoneNumber = contact.phoneNumber,
                                                message = shareText
                                            )
                                            shareStatus = "Opening WhatsApp for ${contact.displayName}"
                                            onAddContact(
                                                contact.displayName,
                                                contact.suggestedHandle,
                                                contact.phoneNumber,
                                                null
                                            )
                                            onCreateInvite(contact.displayName, "WHATSAPP", code) {}
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("WA")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (citizens.isNotEmpty()) {
                item {
                    Text(
                        text = "Share With Existing Narad Users",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(citizens, key = { "inv_cit_${it.userId}" }) { citizen ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CitizenAvatar(name = citizen.displayName, size = 38.dp, verified = true)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(citizen.displayName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    citizen.handle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = { onStartChat(citizen.displayName, citizen.userId) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Chat")
                            }
                            Spacer(Modifier.width(6.dp))
                            OutlinedButton(
                                onClick = {
                                    shareStatus = dispatchShareSafely(
                                        context,
                                        "Connect on Narad",
                                        PhoneContactsHelper.buildConnectShareText(
                                            currentUserName,
                                            currentUserHandle,
                                            personalCode,
                                            currentUserId
                                        )
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Share")
                            }
                        }
                    }
                }
            }

            if (invites.isNotEmpty()) {
                item {
                    Text(
                        text = "Sent Invites (${invites.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(invites, key = { it.id }) { invite ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(invite.recipientName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    invite.inviteCode,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = SovereignEmerald.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = invite.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SovereignEmerald,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
        }
    }
}
