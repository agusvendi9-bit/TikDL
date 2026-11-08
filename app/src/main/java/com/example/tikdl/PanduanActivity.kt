package com.example.tikdl

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class PanduanActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#151515"))
            setPadding(dp(16), dp(24), dp(16), dp(24))
        }

        // Header (X & Judul)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(24))
        }
        header.addView(TextView(this).apply {
            text = "✕"
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(0, 0, dp(16), 0)
            setOnClickListener { finish() }
        })
        header.addView(TextView(this).apply {
            text = "Cara Mengunduh"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        })
        root.addView(header)

        // --- BAGIAN 1: VIA TAUTAN ---
        addSectionTitle(root, "Via Tautan", dp)
        addText(root, "1. Buka TikTok, klik ikon 'Bagikan'.", dp)
        addImage(root, R.drawable.panduan_share, dp)
        addText(root, "2. Pilih 'Salin Tautan' (Copy Link).", dp)
        addImage(root, R.drawable.panduan_copylink, dp)
        addText(root, "3. Buka TikDL, tempel tautan, lalu klik 'Unduh Video'.", dp)

        // --- BAGIAN 2: VIA BUBBLE ---
        addSectionTitle(root, "Via Floating Bubble", dp)
        addText(root, "1. Aktifkan Floating Bubble di menu samping aplikasi.", dp)
        addText(root, "2. Buka TikTok, salin tautan video.", dp)
        addText(root, "3. Tap ikon Bubble TikDL yang melayang di layar.", dp)
        addImage(root, R.drawable.panduan_bubble, dp)

        // --- TOMBOL BUKA TIKTOK ---
        val btnTiktok = Button(this).apply {
            text = "Buka Aplikasi TikTok"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            elevation = 0f
            stateListAnimator = null
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#FF0050"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54)).apply {
                topMargin = dp(32)
            }
            setOnClickListener {
                try {
                    // Coba buka aplikasi TikTok
                    val intent = packageManager.getLaunchIntentForPackage("com.zhiliaoapp.musically")
                    if (intent != null) {
                        startActivity(intent)
                    } else {
                        // Kalau nggak ada, buka web
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com")))
                    }
                } catch (e: Exception) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com")))
                }
            }
        }
        root.addView(btnTiktok)

        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
    }

    // Fungsi bantuan buat nampilin teks
    private fun addText(parent: LinearLayout, text: String, dp: (Int) -> Int) {
        parent.addView(TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(8), 0, dp(8))
        })
    }

    // Fungsi bantuan buat nampilin judul bagian
    private fun addSectionTitle(parent: LinearLayout, text: String, dp: (Int) -> Int) {
        parent.addView(TextView(this).apply {
            this.text = text
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, dp(24), 0, dp(8))
        })
    }

    // Fungsi bantuan buat nampilin gambar
    private fun addImage(parent: LinearLayout, resId: Int, dp: (Int) -> Int) {
        parent.addView(ImageView(this).apply {
            setImageResource(resId)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8); bottomMargin = dp(16) }
        })
    }
}
