package com.example.tikdl

import android.app.Activity
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast

/**
 * Activity transparan yang muncul sesaat. Android 10+ hanya mengizinkan baca
 * clipboard saat aplikasi sedang fokus, jadi pembacaan dilakukan di sini.
 */
class ClipActivity : Activity() {

    private var handled = false

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || handled) return
        handled = true

        val cm = getSystemService(ClipboardManager::class.java)
        val text = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        val link = Regex("https?://\\S*tiktok\\.com\\S*").find(text)?.value

        if (link == null) {
            Toast.makeText(this, "Tidak ada link TikTok di clipboard", Toast.LENGTH_SHORT).show()
        } else {
            startService(
                Intent(this, FloatingService::class.java)
                    .putExtra(FloatingService.EXTRA_URL, link)
            )
        }
        finish()
    }
}
