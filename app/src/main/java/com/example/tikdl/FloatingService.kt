package com.example.tikdl

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.hypot

class FloatingService : Service() {

    companion object {
        private const val CHANNEL = "tikdl"
        private const val IDLE_DELAY = 2500L
        private const val IDLE_ALPHA = 0.4f
    }

    private lateinit var wm: WindowManager
    private val main = Handler(Looper.getMainLooper())

    // bubble
    private var bubble: FrameLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var icon: ImageView? = null
    private var badge: TextView? = null
    private var pulse: ObjectAnimator? = null
    private var snapAnim: ValueAnimator? = null

    // tombol X
    private var closeView: TextView? = null
    private var closeCx = 0
    private var closeCy = 0
    private var overClose = false

    // baca clipboard
    private var grabbing = false
    private val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
        if (hasFocus && grabbing) {
            main.postDelayed({ if (grabbing) finishGrab() }, 80)
        }
    }

    // antrean unduhan (satu per satu)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val pending = AtomicInteger(0)
    private val pendingLinks = HashSet<String>()
    private var stopWhenIdle = false

    private val fadeRunnable = Runnable {
        if (pending.get() == 0 && !grabbing) {
            bubble?.animate()?.alpha(IDLE_ALPHA)?.setDuration(500)?.start()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        addBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        main.postDelayed({ snapToEdge(false) }, 300)
    }

    override fun onDestroy() {
        pulse?.cancel()
        snapAnim?.cancel()
        hideClose()
        bubble?.let { runCatching { wm.removeView(it) } }
        bubble = null
        executor.shutdown()
        main.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "TikDL", NotificationManager.IMPORTANCE_LOW)
        )
        val notif = Notification.Builder(this, CHANNEL)
            .setContentTitle("TikDL aktif")
            .setContentText("Salin link TikTok, lalu tap bubble")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notif)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    @Suppress("DEPRECATION")
    private fun screenSize(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= 30) {
            val b = wm.currentWindowMetrics.bounds
            Pair(b.width(), b.height())
        } else {
            val dm = DisplayMetrics()
            wm.defaultDisplay.getRealMetrics(dm)
            Pair(dm.widthPixels, dm.heightPixels)
        }
    }

    // -------- bubble --------
    private fun addBubble() {
        val s = dp(46)

        val container = FrameLayout(this)

        val iv = ImageView(this)
        iv.setBackgroundResource(R.drawable.bg_bubble)
        iv.setImageResource(R.drawable.ic_download)
        iv.setPadding(dp(9), dp(9), dp(9), dp(9))
        container.addView(
            iv,
            FrameLayout.LayoutParams(dp(40), dp(40), Gravity.BOTTOM or Gravity.START)
        )

        val bd = TextView(this)
        bd.textSize = 10f
        bd.setTextColor(0xFFFE2C55.toInt())
        bd.typeface = Typeface.DEFAULT_BOLD
        bd.gravity = Gravity.CENTER
        bd.minWidth = dp(18)
        bd.setPadding(dp(3), 0, dp(3), 0)
        bd.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(9).toFloat()
            setColor(Color.WHITE)
            setStroke(dp(1), 0xFFFE2C55.toInt())
        }
        bd.visibility = View.GONE
        container.addView(
            bd,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(18), Gravity.TOP or Gravity.END
            )
        )

        val lp = WindowManager.LayoutParams(
            s, s,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        val (sw, sh) = screenSize()
        lp.x = sw - s
        lp.y = (sh * 0.4f).toInt()

        ).apply {
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        container.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    snapAnim?.cancel()
                    showSolid()
                    startX = lp.x
                    startY = lp.y
                    touchX = e.rawX
                    touchY = e.rawY
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - touchX
                    val dy = e.rawY - touchY
                    if (!moved && (abs(dx) > dp(8) || abs(dy) > dp(8))) {
                        moved = true
                        showClose()
                    }
                    if (moved) dragTo(startX + dx.toInt(), startY + dy.toInt())
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (moved) {
                        val drop = overClose && e.action == MotionEvent.ACTION_UP
                        hideClose()
                        if (drop) {
                            closeBubble()
                        } else {
                            snapToEdge(true)
                            scheduleFade()
                        }
                    } else if (e.action == MotionEvent.ACTION_UP) {
                        startGrab()
                        scheduleFade()
                    }
                }
            }
            true
        }

        wm.addView(container, lp)
        bubble = container
        params = lp
        icon = iv
        badge = bd
        scheduleFade()
    }

    private fun dragTo(x: Int, y: Int) {
        val lp = params ?: return
        val v = bubble ?: return
        val s = dp(46)
        lp.x = x
        lp.y = y
        val cx = x + s / 2f
        val cy = y + s / 2f
        val near = closeView != null && hypot(cx - closeCx, cy - closeCy) < dp(80)
        if (near) {
            lp.x = (closeCx - s / 2f).toInt()
            lp.y = (closeCy - s / 2f).toInt()
        }
        if (near != overClose) {
            overClose = near
            val scale = if (near) 1.3f else 1f
            closeView?.animate()?.scaleX(scale)?.scaleY(scale)?.setDuration(120)?.start()
        }
        wm.updateViewLayout(v, lp)
    }

    /** Bubble hanya boleh menempel di tepi kiri atau kanan. */
    private fun snapToEdge(animate: Boolean) {
        val v = bubble ?: return
        val lp = params ?: return
        val (sw, sh) = screenSize()
        val s = dp(46)
        val targetX = if (lp.x + s / 2 < sw / 2) 0 else sw - s
        val targetY = lp.y.coerceIn(dp(24), sh - s - dp(48))
        if (!animate) {
            lp.x = targetX
            lp.y = targetY
            wm.updateViewLayout(v, lp)
            return
        }
        val fromX = lp.x
        val fromY = lp.y
        snapAnim?.cancel()
        snapAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener { a ->
                val t = a.animatedFraction
                lp.x = (fromX + (targetX - fromX) * t).toInt()
                lp.y = (fromY + (targetY - fromY) * t).toInt()
                if (bubble != null) runCatching { wm.updateViewLayout(v, lp) }
            }
            start()
        }
    }

    // -------- transparan saat diam --------
    private fun showSolid() {
        main.removeCallbacks(fadeRunnable)
        bubble?.animate()?.cancel()
        bubble?.alpha = 1f
    }

    private fun scheduleFade() {
        main.removeCallbacks(fadeRunnable)
        main.postDelayed(fadeRunnable, IDLE_DELAY)
    }

    // -------- tombol X --------
    private fun showClose() {
        if (closeView != null) return
        val size = dp(56)
        val (sw, sh) = screenSize()
        val tv = TextView(this)
        tv.text = "X"
        tv.textSize = 22f
        tv.setTextColor(Color.WHITE)
        tv.gravity = Gravity.CENTER
        tv.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xCC222222.toInt())
        }
        tv.alpha = 0f
        val lp = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = sw / 2 - size / 2
        lp.y = sh - dp(200) // <--- Posisi X udah dinaikin biar nyaman
        wm.addView(tv, lp)
        tv.animate().alpha(1f).setDuration(150).start()
        closeView = tv
        closeCx = lp.x + size / 2
        closeCy = lp.y + size / 2
        overClose = false
    }

    private fun hideClose() {
        closeView?.let { runCatching { wm.removeView(it) } }
        closeView = null
        overClose = false
    }

    /** Bubble dimatikan lewat X. Unduhan yang sedang berjalan diselesaikan dulu. */
    private fun closeBubble() {
        pulse?.cancel()
        snapAnim?.cancel()
        bubble?.let { runCatching { wm.removeView(it) } }
        bubble = null
        badge = null
        icon = null
        if (pending.get() == 0) stopSelf() else stopWhenIdle = true
    }

    // -------- baca clipboard tanpa membuka layar --------
    private fun startGrab() {
        val v = bubble ?: return
        val lp = params ?: return
        if (grabbing) return
        grabbing = true
        v.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        wm.updateViewLayout(v, lp)
        main.postDelayed({ if (grabbing) finishGrab() }, 600)
    }

    private fun finishGrab() {
        val v = bubble ?: return
        val lp = params ?: return
        grabbing = false
        v.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)

        val cm = getSystemService(ClipboardManager::class.java)
        val text = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        val link = Regex("https?://\\S*tiktok\\.com\\S*").find(text)?.value

        lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        wm.updateViewLayout(v, lp)

        if (link == null) toast("Tidak ada link TikTok di clipboard") else enqueue(link)
    }

    // -------- antrean --------
    private fun updateBadge() {
        val b = badge ?: return
        val n = pending.get()
        if (n > 0) {
            b.text = if (n > 9) "9+" else n.toString()
            b.visibility = View.VISIBLE
            showSolid()
            if (pulse?.isRunning != true) 
        } else {
            b.visibility = View.GONE
            pulse?.cancel()
            icon?.scaleX = 1f
            icon?.scaleY = 1f
            scheduleFade()
        }
    }

    private fun enqueue(link: String) {
        if (!pendingLinks.add(link)) {
            toast("Link ini sudah ada di antrean")
            return
        }
        val wasIdle = pending.getAndIncrement() == 0
        updateBadge()
        toast(if (wasIdle) "Mengunduh..." else "Masuk antrean")

        executor.execute {
            try {
                // Proses download
                Downloader.download(applicationContext, link)
                // Notifikasi teks "Tersimpan di Movies" sudah DIHAPUS biar bersih
            } catch (e: Exception) {
                toast("Gagal: ${e.message}")
            }
            pending.decrementAndGet()
            main.post {
                pendingLinks.remove(link)
                updateBadge()
                if (stopWhenIdle && pending.get() == 0) stopSelf()
            }
            try {
                Thread.sleep(1100)
            } catch (e: InterruptedException) {
            }
        }
    }

    private fun toast(msg: String) {
        main.post { Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show() }
    }
}
