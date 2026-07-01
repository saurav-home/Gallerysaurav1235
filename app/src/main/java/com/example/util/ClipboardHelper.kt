package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast

object ClipboardHelper {

    /**
     * Injects an image/video binary directly into the Android Clipboard as a Content URI.
     * This allows instant paste into chats like WhatsApp, Telegram, or Slack without opening share sheets.
     */
    fun copyUriToClipboard(context: Context, uri: Uri, mimeType: String, label: String = "Prism Media") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            
            // Create ClipData with appropriate mime type and URI
            val clipData = ClipData.newUri(context.contentResolver, label, uri)
            
            // Set primary clip
            clipboard.setPrimaryClip(clipData)
            
            // Provide immediate user feedback
            Toast.makeText(
                context,
                "Copied to clipboard! Ready to paste directly in chats.",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Clipboard copy failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Clears the system clipboard to prevent leakage of private or sensitive media,
     * fulfilling the secure "Revoke Clipboard Access when App Pauses" feature.
     */
    fun clearClipboard(context: Context) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboard.clearPrimaryClip()
            } else {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        } catch (e: Exception) {
            // Silent fallback
        }
    }
}
