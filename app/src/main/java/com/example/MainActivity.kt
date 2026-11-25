package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.remote.NaradDeepLink
import com.example.data.remote.PhoneContactsHelper
import com.example.ui.NaradApp
import com.example.ui.theme.NaradTheme

class MainActivity : ComponentActivity() {
    private var pendingDeepLink by mutableStateOf<NaradDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleDeepLinkIntent(intent)
        setContent {
            NaradTheme {
                NaradApp(
                    pendingDeepLink = pendingDeepLink,
                    onConsumeDeepLink = { pendingDeepLink = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            PhoneContactsHelper.parseDeepLinkUri(intent.data)?.let { link ->
                pendingDeepLink = link
            }
        }
    }
}
