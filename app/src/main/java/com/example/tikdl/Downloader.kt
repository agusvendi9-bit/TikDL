package com.example.tikdl

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Downloader {

    private const val API = "https://www.tikwm.com/api/"
    private const val UA =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    /** Mengunduh video tanpa watermark ke galeri (Movies/TikDL). Mengembalikan nama file. */
    fun download(ctx: Context, tiktokUrl: String): String {
        // 1. Minta link video ke API
        val c = URL(API).openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.doOutput = true
        c.connectTimeout = 15000
        c.readTimeout = 30000
        c.setRequestProperty("User-Agent", UA)
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        c.outputStream.use {
            it.write(("url=" + URLEncoder.encode(tiktokUrl, "UTF-8") + "&hd=1").toByteArray())
        }
        val body = c.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        if (json.optInt("code", -1) != 0) {
            throw Exception(json.optString("msg", "API gagal"))
        }
        val data = json.getJSONObject("data")
        var play = data.optString("hdplay")
        if (play.isBlank()) play = data.optString("play")
        if (play.isBlank()) throw Exception("Tidak ada video (mungkin postingan foto)")
        if (play.startsWith("/")) play = "https://www.tikwm.com$play"

        // 2. Siapkan file di galeri
        val name = "tiktok_${data.optString("id", System.currentTimeMillis().toString())}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/TikDL")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = ctx.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw Exception("Gagal membuat file")

        // 3. Unduh videonya
        try {
            val v = URL(play).openConnection() as HttpURLConnection
            v.setRequestProperty("User-Agent", UA)
            v.connectTimeout = 15000
            v.readTimeout = 60000
            v.inputStream.use { input ->
                resolver.openOutputStream(uri)!!.use { out -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return name
    }
}
