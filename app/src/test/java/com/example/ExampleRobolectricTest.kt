package com.example

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.PhoneContactsHelper
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboCursor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Narad", appName)
    }

    @Test
    fun `invite and connect deep links format and parse cleanly`() {
        val code = PhoneContactsHelper.personalInviteCode("uid_abc123")
        assertTrue(code.startsWith("NRD-"))

        val inviteText = PhoneContactsHelper.buildInviteShareText(
            senderName = "Ananya",
            recipientName = "Rohan",
            inviteCode = code,
            senderUid = "uid_abc123",
            senderHandle = "@ananya_blr",
            customDownloadUrl = "https://github.com/ananya/narad/releases/latest/download/narad.apk"
        )
        assertTrue(inviteText.contains("narad://join?"))
        assertTrue(inviteText.contains("code=$code"))
        assertTrue(inviteText.contains("https://github.com/ananya/narad/releases/latest/download/narad.apk"))

        val parsedFromShare = PhoneContactsHelper.parseDeepLinkText(inviteText)
        assertNotNull(parsedFromShare)
        assertEquals("join", parsedFromShare?.action)
        assertEquals(code, parsedFromShare?.code)
        assertEquals("uid_abc123", parsedFromShare?.senderUid)
        assertEquals("Ananya", parsedFromShare?.senderName)
        assertEquals("@ananya_blr", parsedFromShare?.senderHandle)

        val connectLink = PhoneContactsHelper.buildConnectLink(
            handle = "@ananya_blr",
            inviteCode = code,
            senderUid = "uid_abc123",
            senderName = "Ananya"
        )
        val parsedConnect = PhoneContactsHelper.parseDeepLinkUri(Uri.parse(connectLink))
        assertNotNull(parsedConnect)
        assertEquals("connect", parsedConnect?.action)
        assertEquals("@ananya_blr", parsedConnect?.senderHandle)
        assertEquals(code, parsedConnect?.code)

        val parsedCodeOnly = PhoneContactsHelper.parseDeepLinkText("Use my code $code")
        assertNotNull(parsedCodeOnly)
        assertEquals(code, parsedCodeOnly?.code)
    }

    @Test
    fun `custom download url saves and restores from SharedPreferences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val url = "https://github.com/narad/releases/narad.apk"
        PhoneContactsHelper.saveCustomDownloadUrl(context, url)
        assertEquals(url, PhoneContactsHelper.getCustomDownloadUrl(context))
    }

    @Test
    fun `phone normalization and safe sms or apk dispatch work without crashing`() {
        assertEquals("919876543210", PhoneContactsHelper.normalizePhoneForWhatsApp("+91 98765-43210"))
        assertEquals("919876543210", PhoneContactsHelper.normalizePhoneForWhatsApp("9876543210"))
        assertEquals("919876543210", PhoneContactsHelper.normalizePhoneForWhatsApp("09876543210"))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val smsRes = PhoneContactsHelper.openDirectSmsInvite(context, "+919876543210", "Join Narad Code: NRD-1234")
        assertTrue(smsRes.copiedToClipboard)

        val apkRes = PhoneContactsHelper.shareInstalledApkSafely(context, "Install Narad")
        assertTrue(apkRes.copiedToClipboard)
    }

    @Test
    fun `fetchPhoneContacts queries and deduplicates phone contacts`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cursor = RoboCursor().apply {
            setColumnNames(
                listOf(
                    ContactsContract.CommonDataKinds.Phone._ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
            )
            setResults(
                arrayOf(
                    arrayOf<Any>("1", "Rohan Sharma", "+91 98765 43210"),
                    arrayOf<Any>("2", "Rohan Duplicate", "+919876543210"),
                    arrayOf<Any>("3", "Priya Nair", "9123456789")
                )
            )
        }
        shadowOf(context.contentResolver).setCursor(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            cursor
        )

        val entries = PhoneContactsHelper.fetchPhoneContacts(context)
        assertEquals(2, entries.size)
        assertEquals("Rohan Sharma", entries[0].displayName)
        assertEquals("@rohansharma_3210", entries[0].suggestedHandle)
        assertEquals("Priya Nair", entries[1].displayName)
    }
}
