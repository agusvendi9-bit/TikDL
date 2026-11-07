package com.example.tikdl

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "TikDL\n\n1. Izinkan tampil di atas aplikasi lain\n2. Mulai bubble\n3. Salin link TikTok, lalu tap bubble"
            textSize = 16f
            setPadding(0, 0, 0, pad)
        })

        root.addView(Button(this).apply {
            text = "1. Izinkan overlay"
            setOnClickListener {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        })

        root.addView(Button(this).apply {
            text = "2. Mulai bubble"
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    startForegroundService(Intent(this@MainActivity, FloatingService::class.java))
                    moveTaskToBack(true)
                } else {
                    text = "Izin overlay belum aktif, tap tombol 1 dulu"
                }
            }
        })

        root.addView(Button(this).apply {
            text = "Hentikan bubble"
            setOnClickListener {
                stopService(Intent(this@MainActivity, FloatingService::class.java))
            }
        })

        setContentView(root)
    }
}
