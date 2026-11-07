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
import org.json.JSONArray

class MainActivity : Activity() {

    private lateinit var linkInput: EditText
    private lateinit var statusText: TextView
    private lateinit var downloadBtn: Button
    private lateinit var historyContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        // ===== 1. FRAME LAYOUT SEBAGAI DASAR =====
        val mainFrame = FrameLayout(this)

        // ===== 2. BACKGROUND GAMBAR (PAKAI IMAGEVIEW BIAR NGGAK KEPOTONG) =====
        val bgImage = ImageView(this).apply {
            setImageResource(R.drawable.bg_minecraft)
            scaleType = ImageView.ScaleType.CENTER_CROP // Biar full layar & proporsional
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        mainFrame.addView(bgImage)

        // ===== 3. KONTEN (SCROLLVIEW) =====
        val scrollView = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(32), dp(20), dp(20))
        }

        // JUDUL
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

        // INPUT LINK
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

        // TOMBOL PASTE + DOWNLOAD
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
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(6)
        })

        downloadBtn = Button(this).apply {
            text = "⬇ Download"
            textSize = 13f
            setBackgroundColor(Color.parseColor("#FF0050"))
            setTextColor(Color.WHITE)
            setOnClickListener { startDownload() }
        }
        actionRow.addView(downloadBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(6)
        })
        root.addView(actionRow)

        // STATUS
        statusText = TextView(this).apply {
            text = "Siap menerima link"
            textSize = 13f
            setTextColor(Color.WHITE)
            setShadowLayer(6f, 2f, 2f, Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, dp(20))
        }
        root.addView(statusText)

        // ===== 4. SECTION RIWAYAT (Dibungkus biar rapi) =====
        val historyBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#CC000000")) // Hitam transparan
            }
            setPadding(0, 0, 0, dp(16)) 
        }

        historyBox.addView(TextView(this).apply {
            text = "Riwayat Download"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(8))
        })

        historyContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        historyBox.addView(historyContainer)
        root.addView(historyBox)

        // ===== 5. SECTION FLOATING BUBBLE =====
        val bubbleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#CC000000"))
            }
            setPadding(0, dp(16), 0, 0)
        }

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
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
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

        root.addView(bubbleLayout)

        // Masukin root ke ScrollView, lalu ScrollView ke FrameLayout
        scrollView.addView(root)
        mainFrame.addView(scrollView)

        setContentView(mainFrame)

        loadHistory()
    }

    private fun setStatus(msg: String, color: String) {
        statusText.text = msg
        statusText.setTextColor(Color.parseColor(color))
    }

    private fun startDownload() {
        val link = linkInput.text.toString().trim()
        if (link.isBlank()) {
            setStatus("Link masih kosong bro", "#F44336")
            return
        }
        if (!link.contains("tiktok.com", ignoreCase = true)) {
            setStatus("Itu bukan link TikTok", "#F44336")
            return
        }

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
        val historyString = prefs.getString("history_json", "[]") ?: "[]"
        
        try {
            val jsonArray = JSONArray(historyString)
            val newArray = JSONArray()
            newArray.put(fileName) 
            
            for (i in 0 until jsonArray.length()) {
                newArray.put(jsonArray.getString(i))
            }
            prefs.edit().putString("history_json", newArray.toString()).apply()
            loadHistory()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadHistory() {
        historyContainer.removeAllViews()
        val prefs = getSharedPreferences("TikDLHistory", Context.MODE_PRIVATE)
        val historyString = prefs.getString("history_json", "[]") ?: "[]"
        
        try {
            val jsonArray = JSONArray(historyString)
            if (jsonArray.length() == 0) {
                historyContainer.addView(TextView(this).apply {
                    text = "Belum ada riwayat download."
                    textSize = 12f
                    setTextColor(Color.LTGRAY)
                    setPadding(0, dp(4), 0, 0)
                })
            } else {
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getString(i)
                    historyContainer.addView(TextView(this).apply {
                        text = "${i + 1}. 🎬 $item"
                        textSize = 12f
                        setTextColor(Color.WHITE)
                        setPadding(0, dp(4), 0, dp(4))
                    })
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }
}
