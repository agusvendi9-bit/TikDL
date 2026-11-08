package com.example.tikdl

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONArray

class MainActivity : Activity() {

    private lateinit var linkInput: EditText
    private lateinit var statusText: TextView
    private lateinit var downloadBtn: Button
    private lateinit var historyContainer: LinearLayout
    private lateinit var overlaySwitch: Switch

    private lateinit var containerBeranda: LinearLayout
    private lateinit var containerRiwayat: ScrollView
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        // ===== 1. ROOT DRAWER LAYOUT =====
        drawerLayout = DrawerLayout(this).apply { setBackgroundColor(Color.BLACK) }

        val bgImage = ImageView(this).apply {
            setImageResource(R.drawable.bg_minecraft)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = DrawerLayout.LayoutParams(
                DrawerLayout.LayoutParams.MATCH_PARENT, DrawerLayout.LayoutParams.MATCH_PARENT
            )
        }
        drawerLayout.addView(bgImage)

        // ===== 2. LAYOUT UTAMA =====
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = DrawerLayout.LayoutParams(
                DrawerLayout.LayoutParams.MATCH_PARENT, DrawerLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Header (Tombol Menu & Judul)
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(8))
        }

        headerLayout.addView(TextView(this).apply {
            text = "☰"
            textSize = 28f
            setTextColor(Color.WHITE)
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
            setPadding(0, 0, dp(16), 0)
            setOnClickListener { drawerLayout.openDrawer(Gravity.START) }
        })

        headerLayout.addView(TextView(this).apply {
            text = "TikDL"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        })
        mainLayout.addView(headerLayout)

        // Area Konten
        val contentArea = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        // --- HALAMAN BERANDA ---
        val scrollBeranda = ScrollView(this)
        containerBeranda = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(16))
        }
        setupBerandaUI(containerBeranda, ::dp)
        scrollBeranda.addView(containerBeranda)
        contentArea.addView(scrollBeranda)

        // --- HALAMAN RIWAYAT ---
        containerRiwayat = ScrollView(this).apply { visibility = ScrollView.GONE }
        val riwayatLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        riwayatLayout.addView(TextView(this).apply {
            text = "Riwayat Download"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
            setPadding(0, 0, 0, dp(16))
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

        // --- BOTTOM NAVIGATION (Ikon Rumah) ---
        val bottomNav = BottomNavigationView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(Color.parseColor("#111111"))
            menu.add(0, 1, 0, "Beranda").setIcon(R.drawable.ic_home)
            menu.add(0, 2, 1, "Riwayat").setIcon(android.R.drawable.ic_menu_recent_history)
            itemIconTintList = ColorStateList.valueOf(Color.WHITE)
            itemTextColor = ColorStateList.valueOf(Color.WHITE)
            setOnItemSelectedListener { item ->
                when (item.itemId) {
                    1 -> { scrollBeranda.visibility = ScrollView.VISIBLE; containerRiwayat.visibility = ScrollView.GONE }
                    2 -> { scrollBeranda.visibility = ScrollView.GONE; containerRiwayat.visibility = ScrollView.VISIBLE; loadHistory() }
                }
                true
            }
        }
        mainLayout.addView(bottomNav)
        drawerLayout.addView(mainLayout)

        // ===== 3. SIDEBAR MENU (AhaTok Style) =====
        setupDrawerMenu(::dp)

        setContentView(drawerLayout)
        loadHistory()
    }

    private fun setupBerandaUI(root: LinearLayout, dp: (Int) -> Int) {
        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10).toFloat()
                setColor(Color.parseColor("#CC222222"))
            }
            setPadding(dp(12), dp(6), dp(12), dp(6))
            gravity = Gravity.CENTER_VERTICAL
        }

        linkInput = EditText(this).apply {
            hint = "Masukkan tautan TikTok..."
            setHintTextColor(Color.GRAY)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            textSize = 14f
            setTextColor(Color.WHITE)
            setSingleLine()
            background = null
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        inputRow.addView(linkInput)

        inputRow.addView(TextView(this).apply {
            text = "❐"
            textSize = 26f
            setTextColor(Color.WHITE)
            setPadding(dp(8), 0, 0, 0)
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = cm.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    linkInput.setText(clip.getItemAt(0).text?.toString() ?: "")
                }
            }
        })
        root.addView(inputRow)

        downloadBtn = Button(this).apply {
            text = "Unduh Video"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            elevation = 0f
            stateListAnimator = null
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10).toFloat()
                setColor(Color.parseColor("#FF0050"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50)
            ).apply { topMargin = dp(12) }
            
            // --- ANIMASI TEKAN TOMBOL (SCALE BOUNCE) ---
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }
                }
                false // return false biar onClickListener tetep jalan
            }
            // -------------------------------------------
            
            setOnClickListener { startDownload() }
        }
        root.addView(downloadBtn)

        statusText = TextView(this).apply {
            text = "Siap menerima link"
            textSize = 12f
            setTextColor(Color.WHITE)
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, dp(16))
        }
        root.addView(statusText)
    }

    private fun setupDrawerMenu(dp: (Int) -> Int) {
        val drawerView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#151515"))
            layoutParams = DrawerLayout.LayoutParams(dp(280), DrawerLayout.LayoutParams.MATCH_PARENT).apply { gravity = Gravity.START }
            setPadding(dp(16), dp(32), dp(16), dp(16))
        }

        drawerView.addView(TextView(this).apply {
            text = "Pengaturan"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(16))
        })

        // --- SWITCH OVERLAY ---
        overlaySwitch = Switch(this)
        overlaySwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    startForegroundService(Intent(this@MainActivity, FloatingService::class.java))
                } else {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                    overlaySwitch.isChecked = false
                }
            } else {
                stopService(Intent(this@MainActivity, FloatingService::class.java))
            }
        }

        val switchRow = createMenuItem(drawerView, R.drawable.ic_home, "Floating Bubble", dp) {
            overlaySwitch.isChecked = !overlaySwitch.isChecked
        }
        switchRow.addView(overlaySwitch)
        
        addDivider(drawerView)

        createMenuItem(drawerView, R.drawable.ic_language, "Bahasa", dp) {}
        addDivider(drawerView)
        
        createMenuItem(drawerView, R.drawable.ic_help, "Cara Mengunduh", dp) {}
        addDivider(drawerView)
        
        createMenuItem(drawerView, R.drawable.ic_feedback, "Umpan Balik & Saran", dp) {}

        drawerLayout.addView(drawerView)
    }

    private fun createMenuItem(parent: LinearLayout, iconRes: Int, text: String, dp: (Int) -> Int, onClick: () -> Unit): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(16), dp(12), dp(16))
            
            // Mask Persegi buat Ripple (Biar nggak jadi bola)
            val mask = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
            }
            val rippleColor = ColorStateList.valueOf(Color.parseColor("#33FFFFFF"))
            background = RippleDrawable(rippleColor, null, mask)
            
            setOnClickListener { onClick() }
        }

        val icon = ImageView(this).apply {
            setImageResource(iconRes)
            layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(16) }
        }
        row.addView(icon)

        row.addView(TextView(this).apply {
            setText(text)
            textSize = 14f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        parent.addView(row)
        return row
    }

    private fun addDivider(parent: LinearLayout) {
        parent.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { topMargin = 8; bottomMargin = 8 }
            setBackgroundColor(Color.parseColor("#33FFFFFF"))
        })
    }

    private fun startDownload() {
        val link = linkInput.text.toString().trim()
        if (link.isBlank() || !link.contains("tiktok.com", true)) {
            statusText.text = "Link tidak valid"
            statusText.setTextColor(Color.RED)
            return
        }
        downloadBtn.isEnabled = false
        statusText.text = "⏳ Mengunduh..."
        statusText.setTextColor(Color.parseColor("#FF9800"))

        Thread {
            try {
                val name = Downloader.download(this, link)
                runOnUiThread {
                    statusText.text = "✅ Tersimpan: $name"
                    statusText.setTextColor(Color.parseColor("#4CAF50"))
                    linkInput.setText("")
                    downloadBtn.isEnabled = true
                    saveToHistory(name)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "❌ Gagal: ${e.message}"
                    statusText.setTextColor(Color.RED)
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
