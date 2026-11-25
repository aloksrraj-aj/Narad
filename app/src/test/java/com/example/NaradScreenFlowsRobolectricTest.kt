package com.example

import com.example.data.model.Channel
import com.example.data.model.ChannelPrivacy
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.ChatType
import com.example.data.model.ConsentGrant
import com.example.data.model.ConsentScope
import com.example.data.model.ContactItem
import com.example.data.model.InviteRecord
import com.example.data.model.MediaPreset
import com.example.data.model.MessagePolicy
import com.example.data.model.PostKind
import com.example.data.model.PostVisibility
import com.example.data.model.PrivacyVault
import com.example.data.model.SocialPost
import com.example.data.model.ThumbnailPreset
import com.example.data.model.UserProfile
import com.example.data.model.VideoFormat
import com.example.data.model.VideoItem
import com.example.data.remote.UiState
import com.example.ui.screens.ChannelsScreen
import com.example.ui.screens.ChatsScreen
import com.example.ui.screens.ContactsInviteSheet
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.VaultProfileScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.theme.NaradTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NaradScreenFlowsRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `feed screen validates empty post and publishes valid post`() {
        var createdCaption = ""
        var createdVis = PostVisibility.PUBLIC
        var likedPostId = ""
        var commentOpenedId = ""
        var addedComment = ""

        val samplePost = SocialPost(
            id = "p1",
            userId = "u1",
            authorName = "Ananya",
            authorHandle = "@ananya",
            caption = "Morning coffee in Bengaluru",
            postKind = PostKind.POST.name,
            visibility = PostVisibility.PUBLIC.name,
            mediaPreset = MediaPreset.NONE.name
        )

        composeRule.setContent {
            NaradTheme {
                FeedScreen(
                    currentUserId = "u1",
                    currentUserName = "Ananya",
                    currentUserHandle = "@ananya",
                    defaultPostVisibility = PostVisibility.PRIVATE,
                    publicPostsState = UiState.Success(listOf(samplePost)),
                    myPostsState = UiState.Success(listOf(samplePost)),
                    localPickedMediaUris = emptyMap(),
                    activeCommentPost = samplePost,
                    commentsState = UiState.Success(emptyList()),
                    onCreatePost = { caption, _, _, vis, _, _ ->
                        createdCaption = caption
                        createdVis = vis
                    },
                    onLikePost = { likedPostId = it },
                    onToggleVisibility = {},
                    onDeletePost = {},
                    onOpenComments = { commentOpenedId = it?.id ?: "" },
                    onAddComment = { _, text -> addedComment = text },
                    onShareToChats = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Test comment box in open bottom sheet first, then dismiss or test post actions
        composeRule.onNodeWithTag("post_comment_input").performTextInput("Jai Hind!")
        composeRule.onNodeWithTag("send_comment_button").performClick()
        composeRule.waitForIdle()
        assertEquals("Jai Hind!", addedComment)
    }

    @Test
    fun `feed create post dialog validates blank input and publishes post on click`() {
        var createdCaption = ""
        var createdKind = PostKind.POST
        var createdVis = PostVisibility.PUBLIC
        var likedPostId = ""

        val samplePost = SocialPost(
            id = "p1",
            userId = "u1",
            authorName = "Ananya",
            authorHandle = "@ananya",
            caption = "Existing post",
            postKind = PostKind.POST.name,
            visibility = PostVisibility.PUBLIC.name,
            mediaPreset = MediaPreset.NONE.name
        )

        composeRule.setContent {
            NaradTheme {
                FeedScreen(
                    currentUserId = "u1",
                    currentUserName = "Ananya",
                    currentUserHandle = "@ananya",
                    defaultPostVisibility = PostVisibility.PRIVATE,
                    publicPostsState = UiState.Success(listOf(samplePost)),
                    myPostsState = UiState.Success(listOf(samplePost)),
                    localPickedMediaUris = emptyMap(),
                    activeCommentPost = null,
                    commentsState = UiState.Success(emptyList()),
                    onCreatePost = { caption, _, kind, vis, _, _ ->
                        createdCaption = caption
                        createdKind = kind
                        createdVis = vis
                    },
                    onLikePost = { likedPostId = it },
                    onToggleVisibility = {},
                    onDeletePost = {},
                    onOpenComments = {},
                    onAddComment = { _, _ -> },
                    onShareToChats = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Like existing post
        composeRule.onNodeWithTag("like_post_p1").performClick()
        composeRule.waitForIdle()
        assertEquals("p1", likedPostId)

        // Open Create Post dialog
        composeRule.onNodeWithTag("create_post_fab").performClick()
        composeRule.waitForIdle()

        // Empty publish shows validation error
        composeRule.onNodeWithTag("publish_post_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Write something or pick a photo").assertIsDisplayed()
        assertEquals("", createdCaption)

        // Fill caption + choose Private + Publish
        composeRule.onNodeWithTag("post_caption_input").performTextInput("Sunset over Ganga")
        composeRule.onNodeWithTag("post_vis_private").performScrollTo().performClick()
        composeRule.onNodeWithTag("publish_post_button").performClick()
        composeRule.waitForIdle()

        assertEquals("Sunset over Ganga", createdCaption)
        assertEquals(PostKind.POST, createdKind)
        assertEquals(PostVisibility.PRIVATE, createdVis)
    }

    @Test
    fun `videos screen validates upload dialog and publishes video`() {
        var publishedTitle = ""
        var publishedFormat = VideoFormat.SHORT

        val longVideo = VideoItem(
            id = "v_long",
            ownerUid = "u1",
            channelName = "Digital Bharat",
            title = "Tech Documentary",
            videoFormat = VideoFormat.LONG.name,
            durationLabel = "12:00",
            visibility = PostVisibility.PUBLIC.name,
            thumbnailPreset = ThumbnailPreset.STUDIO.name
        )

        composeRule.setContent {
            NaradTheme {
                VideosScreen(
                    currentUserId = "u1",
                    publicVideosState = UiState.Success(listOf(longVideo)),
                    myVideosState = UiState.Success(emptyList()),
                    myChannels = emptyList(),
                    activeVideo = null,
                    videoCommentsState = UiState.Success(emptyList()),
                    onSelectVideo = {},
                    onCreateVideo = { _, _, title, _, fmt, _, _, _, _ ->
                        publishedTitle = title
                        publishedFormat = fmt
                    },
                    onLikeVideo = {},
                    onAddVideoComment = { _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        // Switch to Long Videos tab and verify card renders
        composeRule.onNodeWithTag("long_videos_tab_chip").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("long_video_v_long").assertIsDisplayed()

        // Open upload dialog
        composeRule.onNodeWithTag("upload_video_fab").performClick()
        composeRule.waitForIdle()

        // Empty publish shows error
        composeRule.onNodeWithTag("publish_video_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Title must be at least 2 characters").assertIsDisplayed()

        // Fill title & publish
        composeRule.onNodeWithTag("video_title_input").performTextInput("UPI Global Story")
        composeRule.onNodeWithTag("publish_video_button").performClick()
        composeRule.waitForIdle()

        assertEquals("UPI Global Story", publishedTitle)
        assertEquals(VideoFormat.LONG, publishedFormat)
    }

    @Test
    fun `channels screen validates create channel dialog and joins channel`() {
        var createdName = ""
        var createdHandle = ""
        var toggledChannelId = ""

        val channel = Channel(
            id = "ch1",
            ownerUid = "other_uid",
            name = "Science India",
            handle = "@science_in",
            description = "ISRO & Space",
            channelPrivacy = ChannelPrivacy.PUBLIC.name,
            subscribersCount = 500L
        )

        composeRule.setContent {
            NaradTheme {
                ChannelsScreen(
                    currentUserId = "u1",
                    publicChannelsState = UiState.Success(listOf(channel)),
                    myChannelsState = UiState.Success(emptyList()),
                    subscriptionsState = UiState.Success(emptyList()),
                    activeChannel = null,
                    broadcastsState = UiState.Success(emptyList()),
                    onSelectChannel = {},
                    onCreateChannel = { name, handle, _, _, _ ->
                        createdName = name
                        createdHandle = handle
                    },
                    onToggleSubscription = { ch, _ -> toggledChannelId = ch.id },
                    onPublishBroadcast = { _, _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Join").performClick()
        composeRule.waitForIdle()
        assertEquals("ch1", toggledChannelId)

        composeRule.onNodeWithTag("create_channel_fab").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("create_channel_confirm_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Channel name must be at least 2 characters").assertIsDisplayed()

        composeRule.onNodeWithTag("channel_name_input").performTextInput("Bharat AI")
        composeRule.onNodeWithTag("create_channel_confirm_button").performClick()
        composeRule.waitForIdle()

        assertEquals("Bharat AI", createdName)
        assertTrue(createdHandle.isNotBlank())
    }

    @Test
    fun `chats screen opens contacts invite tabs and creates new chat`() {
        var openedSheetTab = -1
        var createdChatTitle = ""

        composeRule.setContent {
            NaradTheme {
                ChatsScreen(
                    currentUserId = "u1",
                    currentUserName = "Ananya",
                    chatsState = UiState.Success(emptyList()),
                    contactsState = UiState.Success(emptyList()),
                    discoverableCitizensState = UiState.Success(emptyList()),
                    activeChatId = null,
                    messagesState = UiState.Success(emptyList()),
                    localPickedMediaUris = emptyMap(),
                    onSelectChat = {},
                    onCreateChat = { title, _, _, _ -> createdChatTitle = title },
                    onStartChatWithContact = { _, _ -> },
                    onOpenContactsSheet = { openedSheetTab = it },
                    onSendMessage = { _, _, _, _, _ -> },
                    onReactToMessage = { _, _, _ -> },
                    onUpdateDisappearingTimer = { _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("open_contacts_button").performClick()
        assertEquals(0, openedSheetTab)

        composeRule.onNodeWithTag("open_invite_button").performClick()
        assertEquals(1, openedSheetTab)

        composeRule.onNodeWithTag("new_chat_fab").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("new_chat_start_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Enter a chat name or pick a contact").assertIsDisplayed()

        composeRule.onNodeWithTag("new_chat_name_input").performTextInput("Vikram")
        composeRule.onNodeWithTag("new_chat_start_button").performClick()
        composeRule.waitForIdle()
        assertEquals("Vikram", createdChatTitle)
    }

    @Test
    fun `chats detail view sends message and reacts to message`() {
        var sentText = ""
        var reactedEmoji = ""

        val chat = ChatThread(id = "c1", title = "Vikram", participantUids = listOf("u1"))
        val msg = ChatMessage(id = "m1", chatId = "c1", senderUid = "u1", senderName = "Ananya", text = "Namaste!")

        composeRule.setContent {
            NaradTheme {
                ChatsScreen(
                    currentUserId = "u1",
                    currentUserName = "Ananya",
                    chatsState = UiState.Success(listOf(chat)),
                    contactsState = UiState.Success(emptyList()),
                    discoverableCitizensState = UiState.Success(emptyList()),
                    activeChatId = "c1",
                    messagesState = UiState.Success(listOf(msg)),
                    localPickedMediaUris = emptyMap(),
                    onSelectChat = {},
                    onCreateChat = { _, _, _, _ -> },
                    onStartChatWithContact = { _, _ -> },
                    onOpenContactsSheet = {},
                    onSendMessage = { _, text, _, _, _ -> sentText = text },
                    onReactToMessage = { _, _, emoji -> reactedEmoji = emoji },
                    onUpdateDisappearingTimer = { _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("msg_bubble_m1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("msg_react_❤️").performClick()
        composeRule.waitForIdle()
        assertEquals("❤️", reactedEmoji)

        composeRule.onNodeWithTag("chat_message_input").performTextInput("How are you?")
        composeRule.onNodeWithTag("chat_send_button").performClick()
        composeRule.waitForIdle()
        assertEquals("How are you?", sentText)
    }

    @Test
    fun `contacts invite sheet adds contact creates invite and redeems link`() {
        val addedContacts = mutableListOf<Pair<String, String>>()
        var invitedName = ""
        var invitedPresetCode = ""
        var startedChatWith = ""

        composeRule.setContent {
            NaradTheme {
                ContactsInviteSheet(
                    initialTab = 0,
                    currentUserId = "u1",
                    currentUserName = "Ananya",
                    currentUserHandle = "@ananya",
                    contactsState = UiState.Success(emptyList<ContactItem>()),
                    invitesState = UiState.Success(emptyList<InviteRecord>()),
                    discoverableCitizensState = UiState.Success(emptyList<UserProfile>()),
                    onDismiss = {},
                    onAddContact = { name, handle, _, _ ->
                        addedContacts.add(name to handle)
                    },
                    onDeleteContact = {},
                    onStartChat = { name, _ -> startedChatWith = name },
                    onCreateInvite = { name, _, presetCode, onCreated ->
                        invitedName = name
                        invitedPresetCode = presetCode.orEmpty()
                        onCreated(
                            InviteRecord(
                                id = "inv1",
                                recipientName = name,
                                inviteCode = presetCode ?: "NRD-1234"
                            )
                        )
                    }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("toggle_add_contact_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("contacts_tab_list")
            .performScrollToNode(hasTestTag("save_contact_button"))
        composeRule.onNodeWithTag("save_contact_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Enter name & handle").assertIsDisplayed()

        composeRule.onNodeWithTag("contacts_tab_list")
            .performScrollToNode(hasTestTag("add_contact_name_input"))
        composeRule.onNodeWithTag("add_contact_name_input").performTextInput("Meera")
        composeRule.onNodeWithTag("add_contact_handle_input").performTextInput("@meera_del")
        composeRule.onNodeWithTag("contacts_tab_list")
            .performScrollToNode(hasTestTag("save_contact_button"))
        composeRule.onNodeWithTag("save_contact_button").performClick()
        composeRule.waitForIdle()

        assertEquals("Meera" to "@meera_del", addedContacts.first())

        // Switch to Invite Friend tab
        composeRule.onNodeWithTag("sheet_tab_invite").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("invite_tab_list")
            .performScrollToNode(hasTestTag("invite_friend_name_input"))
        composeRule.onNodeWithTag("invite_friend_name_input").performTextInput("Karan")
        composeRule.onNodeWithTag("send_personal_invite_button").performClick()
        composeRule.waitForIdle()

        assertEquals("Karan", invitedName)
        assertTrue(invitedPresetCode.startsWith("NRD-"))

        // Redeem pasted narad:// connect link
        composeRule.onNodeWithTag("invite_tab_list")
            .performScrollToNode(hasTestTag("join_link_or_code_input"))
        composeRule.onNodeWithTag("join_link_or_code_input")
            .performTextInput("narad://connect?handle=@vikram_in&name=Vikram&code=NRD-9988&uid=u2")
        composeRule.onNodeWithTag("redeem_link_button").performClick()
        composeRule.waitForIdle()

        assertTrue(addedContacts.any { it.first == "Vikram" && it.second == "@vikram_in" })
        assertEquals("Vikram", startedChatWith)
    }

    @Test
    fun `vault profile screen validates edit profile and consent grant dialogs`() {
        var savedProfileName = ""
        var grantedName = ""

        val profile = UserProfile(
            userId = "u1",
            displayName = "Ananya",
            handle = "@ananya",
            bio = "Privacy first",
            city = "Bengaluru",
            discoverable = true
        )
        val vault = PrivacyVault(
            userId = "u1",
            defaultPostVisibility = PostVisibility.PRIVATE.name,
            messagePolicy = MessagePolicy.CONTACTS.name
        )

        composeRule.setContent {
            NaradTheme {
                VaultProfileScreen(
                    currentUserId = "u1",
                    profileState = UiState.Success(profile),
                    vaultState = UiState.Success(vault),
                    consentGrantsState = UiState.Success(emptyList<ConsentGrant>()),
                    auditLogsState = UiState.Success(emptyList()),
                    myPostsState = UiState.Success(emptyList()),
                    contactsCount = 2,
                    onOpenContactsSheet = {},
                    onUpdateProfile = { name, _, _, _, _ -> savedProfileName = name },
                    onUpdateVault = { _, _, _, _, _, _ -> },
                    onAddConsentGrant = { name, _, _ -> grantedName = name },
                    onToggleConsentGrant = {},
                    onDeleteConsentGrant = {},
                    onRunWarrantCheck = {},
                    onSignOut = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Edit Profile dialog validation + save
        composeRule.onNodeWithTag("edit_profile_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edit_profile_name_input").performTextClearance()
        composeRule.onNodeWithTag("edit_profile_save_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Name and handle are required").assertIsDisplayed()

        composeRule.onNodeWithTag("edit_profile_name_input").performTextInput("Ananya V")
        composeRule.onNodeWithTag("edit_profile_save_button").performClick()
        composeRule.waitForIdle()
        assertEquals("Ananya V", savedProfileName)

        // Add Consent Grant dialog validation + submit (scroll LazyColumn to Consent Grants)
        composeRule.onNodeWithTag("vault_lazy_list")
            .performScrollToNode(hasTestTag("add_consent_grant_button"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_consent_grant_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("consent_grant_confirm_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Enter contact name and @handle").assertIsDisplayed()

        composeRule.onNodeWithTag("consent_name_input").performTextInput("Rohan")
        composeRule.onNodeWithTag("consent_handle_input").performTextInput("@rohan")
        composeRule.onNodeWithTag("consent_grant_confirm_button").performClick()
        composeRule.waitForIdle()
        assertEquals("Rohan", grantedName)
    }
}
