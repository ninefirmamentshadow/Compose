package com.drafts.compose.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast

/**
 * The clipboard is the only way text leaves this app. There is no share sheet, no
 * export, no upload — copy, then paste it where it is going.
 */
object Clip {

    fun copy(context: Context, label: String, text: String) {
        if (text.isBlank()) return
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        manager.setPrimaryClip(ClipData.newPlainText(label, text))
        // Android 13+ shows its own copy confirmation; a second one is just noise.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
        }
    }
}
