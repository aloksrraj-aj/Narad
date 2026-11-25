package com.example.data.remote

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class PhoneContactEntry(
    val id: String,
    val displayName: String,
    val phoneNumber: String,
    val suggestedHandle: String
)

data class NaradDeepLink(
    val action: String, // "join" or "connect"
    val code: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val senderHandle: String = ""
)

data class ShareDispatchResult(
    val chooserLaunched: Boolean,
    val copiedToClipboard: Boolean,
    val sharedApkFile: Boolean = false
)

object PhoneContactsHelper {
    private const val PREFS_NAME = "narad_share_prefs"
    private const val KEY_CUSTOM_DOWNLOAD_URL = "custom_download_url"

    suspend fun fetchPhoneContacts(context: Context): List<PhoneContactEntry> = withContext(Dispatchers.IO) {
        val results = LinkedHashMap<String, PhoneContactEntry>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext() && results.size < 150) {
                    val rawName = if (nameIdx >= 0) cursor.getString(nameIdx)?.trim().orEmpty() else ""
                    val rawPhone = if (phoneIdx >= 0) cursor.getString(phoneIdx)?.trim().orEmpty() else ""
                    if (rawName.isNotEmpty() && rawPhone.isNotEmpty()) {
                        val normalizedKey = rawPhone.replace(Regex("[^0-9+]"), "").ifEmpty { rawName.lowercase() }
                        if (!results.containsKey(normalizedKey)) {
                            val rowId = if (idIdx >= 0) cursor.getString(idIdx) ?: normalizedKey else normalizedKey
                            val slug = rawName.lowercase().replace(Regex("[^a-z0-9]"), "").take(14).ifEmpty { "friend" }
                            val last4 = rawPhone.replace(Regex("[^0-9]"), "").takeLast(4)
                            val handle = if (last4.isNotEmpty()) "@${slug}_$last4" else "@$slug"
                            results[normalizedKey] = PhoneContactEntry(
                                id = rowId,
                                displayName = rawName.take(60),
                                phoneNumber = rawPhone.take(24),
                                suggestedHandle = handle.take(40)
                            )
                        }
                    }
                }
            }
        }
        results.values.toList()
    }

    fun generateInviteCode(): String =
        "NRD-" + UUID.randomUUID().toString().replace("-", "").take(4).uppercase()

    fun personalInviteCode(userId: String): String {
        val clean = userId.replace(Regex("[^a-zA-Z0-9]"), "").take(4).uppercase().padEnd(4, 'X')
        return "NRD-$clean"
    }

    fun getCustomDownloadUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_DOWNLOAD_URL, "").orEmpty().trim()
    }

    fun saveCustomDownloadUrl(context: Context, url: String) {
        val clean = url.trim().take(300)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_DOWNLOAD_URL, clean)
            .apply()
    }

    fun buildAppInviteLink(
        inviteCode: String,
        senderUid: String = "",
        senderName: String = "",
        senderHandle: String = ""
    ): String {
        val builder = Uri.Builder()
            .scheme("narad")
            .authority("join")
            .appendQueryParameter("code", inviteCode.trim())
        if (senderUid.isNotBlank()) builder.appendQueryParameter("uid", senderUid.trim())
        if (senderName.isNotBlank()) builder.appendQueryParameter("name", senderName.trim())
        if (senderHandle.isNotBlank()) builder.appendQueryParameter("handle", senderHandle.trim())
        return builder.build().toString()
    }

    fun buildConnectLink(
        handle: String,
        inviteCode: String,
        senderUid: String = "",
        senderName: String = ""
    ): String {
        val cleanHandle = handle.trim().let { if (it.startsWith("@")) it else "@$it" }.ifEmpty { "@citizen" }
        val builder = Uri.Builder()
            .scheme("narad")
            .authority("connect")
            .appendQueryParameter("handle", cleanHandle)
            .appendQueryParameter("code", inviteCode.trim())
        if (senderUid.isNotBlank()) builder.appendQueryParameter("uid", senderUid.trim())
        if (senderName.isNotBlank()) builder.appendQueryParameter("name", senderName.trim())
        return builder.build().toString()
    }

    fun buildInviteShareText(
        senderName: String,
        recipientName: String?,
        inviteCode: String,
        senderUid: String = "",
        senderHandle: String = "",
        customDownloadUrl: String = ""
    ): String {
        val appLink = buildAppInviteLink(inviteCode, senderUid, senderName, senderHandle)
        val greeting = if (!recipientName.isNullOrBlank()) "Namaste ${recipientName.trim()}!" else "Namaste!"
        val webUrl = customDownloadUrl.trim().takeIf {
            it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)
        }
        return if (webUrl != null) {
            "$greeting $senderName invited you to Narad (private chat, social & videos).\nDownload Narad: $webUrl\nInvite Code: $inviteCode\nConnect inside Narad: $appLink"
        } else {
            "$greeting $senderName invited you to Narad (private chat, social & videos).\nInvite Code: $inviteCode\nAlready installed? Paste this in Narad -> Invite Friend: $appLink"
        }
    }

    fun buildConnectShareText(
        senderName: String,
        senderHandle: String,
        inviteCode: String,
        senderUid: String = ""
    ): String {
        val link = buildConnectLink(senderHandle, inviteCode, senderUid, senderName)
        return "Connect with $senderName ($senderHandle) on Narad. Paste in Narad -> Invite Friend -> Connect:\n$link\nInvite Code: $inviteCode"
    }

    fun normalizePhoneForWhatsApp(rawPhone: String): String {
        val digits = rawPhone.replace(Regex("[^0-9]"), "")
        return when {
            digits.length == 10 -> "91$digits"
            digits.length == 11 && digits.startsWith("0") -> "91${digits.drop(1)}"
            else -> digits
        }
    }

    fun parseDeepLinkUri(uri: Uri?): NaradDeepLink? {
        if (uri == null) return null
        val scheme = uri.scheme?.lowercase().orEmpty()
        val host = uri.host?.lowercase().orEmpty()
        val path = uri.path.orEmpty()

        val action = when {
            scheme == "narad" && (host == "join" || host == "connect") -> host
            (scheme == "https" || scheme == "http") && path.contains("/connect") -> "connect"
            (scheme == "https" || scheme == "http") && path.contains("/join") -> "join"
            else -> return null
        }

        val code = uri.getQueryParameter("code").orEmpty().trim()
        val uid = uri.getQueryParameter("uid").orEmpty().trim()
        val name = uri.getQueryParameter("name").orEmpty().trim()
        val handle = uri.getQueryParameter("handle").orEmpty().trim()

        return NaradDeepLink(
            action = action,
            code = code,
            senderUid = uid,
            senderName = name,
            senderHandle = handle
        )
    }

    fun parseDeepLinkText(rawInput: String?): NaradDeepLink? {
        val text = rawInput?.trim().orEmpty()
        if (text.isEmpty()) return null

        val urlPattern = Regex("""((?:narad|https?)://[^\s|]+)""", RegexOption.IGNORE_CASE)
        urlPattern.findAll(text).firstNotNullOfOrNull { match ->
            parseDeepLinkUri(Uri.parse(match.value))
        }?.let { return it }

        val codeMatch = Regex("""\bNRD-[A-Z0-9]{3,12}\b""", RegexOption.IGNORE_CASE).find(text)
        if (codeMatch != null) {
            return NaradDeepLink(
                action = "join",
                code = codeMatch.value.uppercase()
            )
        }
        return null
    }

    fun copyToClipboard(context: Context, label: String, text: String): Boolean {
        return runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false
            cm.setPrimaryClip(ClipData.newPlainText(label, text))
            true
        }.getOrDefault(false)
    }

    fun shareTextSafely(context: Context, subject: String, text: String): ShareDispatchResult {
        val copied = copyToClipboard(context, subject, text)
        val launched = runCatching {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(sendIntent, subject).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(chooser)
            true
        }.getOrDefault(false)
        return ShareDispatchResult(chooserLaunched = launched, copiedToClipboard = copied)
    }

    fun openDirectSmsInvite(
        context: Context,
        phoneNumber: String,
        message: String
    ): ShareDispatchResult {
        val copied = copyToClipboard(context, "Narad SMS Invite", message)
        val cleanPhone = phoneNumber.trim()
        val launched = if (cleanPhone.isNotEmpty()) {
            runCatching {
                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(cleanPhone)}")).apply {
                    putExtra("sms_body", message)
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(smsIntent)
                true
            }.getOrDefault(false)
        } else {
            false
        }
        return if (launched) {
            ShareDispatchResult(chooserLaunched = true, copiedToClipboard = copied)
        } else {
            shareTextSafely(context, "Invite via SMS", message)
        }
    }

    fun openWhatsAppInvite(
        context: Context,
        phoneNumber: String,
        message: String
    ): ShareDispatchResult {
        val copied = copyToClipboard(context, "Narad WhatsApp Invite", message)
        val digits = normalizePhoneForWhatsApp(phoneNumber)
        val launched = if (digits.length >= 10) {
            runCatching {
                val waUrl = "https://wa.me/$digits?text=${Uri.encode(message)}"
                val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)).apply {
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(waIntent)
                true
            }.getOrDefault(false)
        } else {
            false
        }
        return if (launched) {
            ShareDispatchResult(chooserLaunched = true, copiedToClipboard = copied)
        } else {
            shareTextSafely(context, "Invite on WhatsApp", message)
        }
    }

    private fun prepareSharedApkFile(context: Context): File? {
        return runCatching {
            val sourcePath = context.applicationInfo?.sourceDir ?: return null
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists() || !sourceFile.canRead() || sourceFile.length() <= 0L) {
                return null
            }
            val dir = File(context.cacheDir, "shared_apks").apply { mkdirs() }
            val dest = File(dir, "Narad.apk")
            if (!dest.exists() || dest.length() != sourceFile.length()) {
                sourceFile.inputStream().use { input ->
                    dest.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            dest
        }.getOrNull()
    }

    fun shareInstalledApkSafely(context: Context, captionText: String): ShareDispatchResult {
        val copied = copyToClipboard(context, "Narad Invite", captionText)
        val apkFile = prepareSharedApkFile(context)
        if (apkFile != null) {
            val launchedApk = runCatching {
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.android.package-archive"
                    putExtra(Intent.EXTRA_STREAM, apkUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Install Narad App")
                    putExtra(Intent.EXTRA_TEXT, captionText)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(sendIntent, "Send Narad APK").apply {
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(chooser)
                true
            }.getOrDefault(false)
            if (launchedApk) {
                return ShareDispatchResult(
                    chooserLaunched = true,
                    copiedToClipboard = copied,
                    sharedApkFile = true
                )
            }
        }
        return shareTextSafely(context, "Join Narad", captionText)
    }
}
