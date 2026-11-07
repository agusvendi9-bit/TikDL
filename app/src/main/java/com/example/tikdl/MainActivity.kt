package com.example.tikdl

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONArray

class MainActivity : Activity() {

    private lateinit var linkInput: EditText
    private lateinit var statusText: TextView
    private lateinit var downloadBtn: Button
    private lateinit var historyContainer: LinearLayout

    // Kontainer untuk pindah halaman
    private lateinit var containerBeranda: LinearLayout
    private lateinit var containerRiwayat: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        // ===== FRAME UTAMA =====
        val mainFrame = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

        // Background Minecraft
        val bgImage = ImageView(this).apply {
            setImageResource(R.drawable.bg_minecraft)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        mainFrame.addView(bgImage)

        // ===== LAYOUT UTAMA (Dibagi jadi Konten & Bottom Nav) =====
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // ===== AREA KONTEN (Tempat pindah halaman) =====
        val contentArea = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f // Weight 1 biar penuh ke atas
            )
        }

        // ---------- 1. HALAMAN BERANDA ----------
        val scrollBeranda = ScrollView(this)
        containerBeranda = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(32), dp(20), dp(20))
        }
        
        setupBerandaUI(containerBeranda, ::dp)
        scrollBeranda.addView(containerBeranda)
        contentArea.addView(scrollBeranda)

        // ---------- 2. HALAMAN RIWAYAT ----------
        containerRiwayat = ScrollView(this).apply { visibility = ScrollView.GONE } // Disembunyiin dulu
        val riwayatLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(32), dp(20), dp(20))
        }

        riwayatLayout.addView(TextView(this).apply {
            text = "Riwayat Download"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setShadowLayer(6f, 2f, 2f, Color.BLACK)
            setPadding(0, 0, 0, dp(20))
        })

        historyContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#CC000000"))
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        riwayatLayout.addView(historyContainer)
        
        containerRiwayat.addView(riwayatLayout)
        contentArea.addView(containerRiwayat)

        mainLayout.addView(contentArea)

        // ===== BOTTOM NAVIGATION BAR =====
        val bottomNav = BottomNavigationView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(Color.parseColor("#111111")) // Warna gelap
            
            // Bikin menu Beranda & Riwayat
            menu.add(0, 1, 0, "Beranda").setIcon(android.R.drawable.ic_menu_compass)
            menu.add(0, 2, 1, "Riwayat").setIcon(android.R.drawable.ic_menu_recent_history)
            
            // Warna teks & ikon
            itemIconTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            itemTextColor = android.content.res.ColorStateList.valueOf(Color.WHITE)
            setOnItemSelectedListener { item ->
                when (item.itemId) {
                    1 -> { // Beranda dipilih
                        scrollBeranda.visibility = ScrollView.VISIBLE
                        containerRiwayat.visibility = ScrollView.GONE
                    }
                    2 -> { // Riwayat dipilih
                        scrollBeranda.visibility = ScrollView.GONE
                        containerRiwayat.visibility = ScrollView.VISIBLE
                        loadHistory() // Refresh list tiap buka riwayat
                    }
                }
                true
            }
        }
        
        mainLayout.addView(bottomNav)
        mainFrame.addView(mainLayout)
        setContentView(mainFrame)

        loadHistory()
    }

    // ==========================================
    // FUNGSI BIKIN UI BERANDA (Biar kode rapi)
    // ==========================================
    private fun setupBerandaUI(root: LinearLayout, dp: (Int) -> Int) {
        root.addView(TextView(this).apply {
            text = "🎵 TikDL"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setShadowLayer(6f, 2f, 2f, Color.BLACK)
            gravity = Gravity.CENTER
        })

        root.addView(TextView(this).apply {
            text = "Downloader TikTok tanpa watermark"
            textSize = 13f
            setTextColor(Color.WHITE)
            setShadowLayer(6f, 2f, 2f, Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(24))
        })

        root.addView(TextView(this).apply {
            text = "Link TikTok"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setShadowLayer(6f, 2f, 2f, Color.BLACK)
            setPadding(0, 0, 0, dp(6))
        })

        linkInput = EditText(this).apply {
            hint = "https://vt.tiktok.com/..."
            setHintTextColor(Color.GRAY)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            textSize = 14f
            setSingleLine()
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(8).toFloat()
                setColor(Color.parseColor("#EEFFFFFF")) 
            }
        }
        root.addView(linkInput)

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }

        actionRow.addView(Button(this).apply {
            text = "📋 Paste"
            textSize = 13f
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = cm.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val txt = clip.getItemAt(0).text?.toString() ?: ""
                    linkInput.setText(txt)
                    setStatus("Link di-paste", "#2196F3")
                } else {
                    Toast.makeText(this@MainActivity, "Clipboard kosong", Toast.LENGTH_SHORT).show()
                }
            }
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) })

        downloadBtn = Button(this).apply {
            text = "⬇ Download"
            textSize = 13f
            setBackgroundColor(Color.parseColor("#FF0050"))
            setTextColor(Color.WHITE)
            setOnClickListener { startDownload() }
        }
        actionRow.addView(downloadBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(6) })
        root.addView(actionRow)

        statusText = TextView(this).apply {
            text = "Siap menerima link"
            textSize = 13f
            setTextColor(Color.WHITE)
            setShadowLayer(6f, 2f, 2f, Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, dp(14))
        }
        root.addView(statusText)

        val bubbleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#CC000000"))
            }
        }
        root.addView(bubbleLayout)

        bubbleLayout.addView(TextView(this).apply {
            text = "Floating Bubble"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(8))
        })

        bubbleLayout.addView(Button(this).apply {
            text = "1. Izinkan overlay"
            textSize = 13f
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        })
        bubbleLayout.addView(Button(this).apply {
            text = "2. Mulai bubble"
            textSize = 13f
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    startForegroundService(Intent(this@MainActivity, FloatingService::class.java))
                    setStatus("Bubble aktif", "#4CAF50")
                    moveTaskToBack(true)
                } else {
                    setStatus("Izin overlay belum aktif", "#F44336")
                }
            }
        })
        bubbleLayout.addView(Button(this).apply {
            text = "Hentikan bubble"
            textSize = 13f
            setOnClickListener {
                stopService(Intent(this@MainActivity, FloatingService::class.java))
                setStatus("Bubble dihentikan", "#666666")
            }
        })
    }

    private fun setStatus(msg: String, color: String) {
        statusText.text = msg
        statusText.setTextColor(Color.parseColor(color))
    }

    private fun startDownload() {
        val link = linkInput.text.toString().trim()
        if (link.isBlank()) { setStatus("Link masih kosong bro", "#F44336"); return }
        if (!link.contains("tiktok.com", ignoreCase = true)) { setStatus("Itu bukan link TikTok", "#F44336"); return }

        downloadBtn.isEnabled = false
        setStatus("⏳ Mengunduh...", "#FF9800")

        Thread {
            try {
                val name = Downloader.download(this, link)
                runOnUiThread {
                    setStatus("✅ Tersimpan: $name", "#4CAF50")
                    linkInput.setText("")
                    downloadBtn.isEnabled = true
                    saveToHistory(name)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    setStatus("❌ Gagal: ${e.message}", "#F44336")
                    downloadBtn.isEnabled = true
                }
            }
        }.start()
    }

    private fun saveToHistory(fileName: String) {
        val prefs = getSharedPreferences("TikDLHistory", Context.MODE_PRIVATE)
        val jsonArray = JSONArray(prefs.getString("history_json", "[]"))
        val newArray = JSONArray()
        newArray.put(fileName) 
        for (i in 0 until jsonArray.length()) { newArray.put(jsonArray.getString(i)) }
        prefs.edit().putString("history_json", newArray.toString()).apply()
        loadHistory()
    }

    private fun loadHistory() {
        historyContainer.removeAllViews()
        val prefs = getSharedPreferences("TikDLHistory", Context.MODE_PRIVATE)
        val jsonArray = JSONArray(prefs.getString("history_json", "[]"))
        
        if (jsonArray.length() == 0) {
            historyContainer.addView(TextView(this).apply {
                text = "Belum ada riwayat download."
                textSize = 14f
                setTextColor(Color.LTGRAY)
            })
        } else {
            for (i in 0 until jsonArray.length()) {
                historyContainer.addView(TextView(this).apply {
                    text = "${i + 1}. 🎬 ${jsonArray.getString(i)}"
                    textSize = 14f
                    setTextColor(Color.WHITE)
                    setPadding(0, 8, 0, 8)
                })
            }
        }
    }
}
