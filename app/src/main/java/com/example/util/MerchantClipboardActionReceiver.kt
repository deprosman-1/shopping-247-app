package com.example.util

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class MerchantClipboardActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_COPY_MERCHANT_DETAIL) return
        val label = intent.getStringExtra(EXTRA_COPY_LABEL).orEmpty().ifBlank { "ODIYA Payment" }
        val value = intent.getStringExtra(EXTRA_COPY_VALUE).orEmpty()
        val toastText = intent.getStringExtra(EXTRA_TOAST_MESSAGE).orEmpty()
            .ifBlank { "Copied $label: $value" }

        if (value.isNotBlank()) {
            runCatching {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText(label, value))
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
            } else {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        const val ACTION_COPY_MERCHANT_DETAIL = "com.example.action.COPY_MERCHANT_DETAIL"
        const val EXTRA_COPY_LABEL = "copy_label"
        const val EXTRA_COPY_VALUE = "copy_value"
        const val EXTRA_TOAST_MESSAGE = "toast_message"
    }
}
