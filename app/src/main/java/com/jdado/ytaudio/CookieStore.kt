package com.jdado.ytaudio

import android.content.Context
import android.webkit.CookieManager
import java.io.File

/**
 * Bridges the Android WebView [CookieManager] (populated after the user signs in
 * to Google/YouTube) into a Netscape cookies.txt file that yt-dlp understands via
 * its --cookies option. This is how downloads of age-restricted / sign-in-required
 * videos are authorized.
 */
object CookieStore {

    private const val FILE_NAME = "cookies.txt"

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    fun isLoggedIn(context: Context): Boolean {
        val f = file(context)
        if (!f.exists()) return false
        val text = runCatching { f.readText() }.getOrDefault("")
        // These cookies are only present for an authenticated YouTube session.
        return text.contains("SAPISID") || text.contains("__Secure-3PSID") ||
            text.contains("LOGIN_INFO")
    }

    /**
     * Harvests cookies for the YouTube/Google domains from the WebView cookie jar
     * and writes them in Netscape format. Returns true if any auth cookie was found.
     */
    fun exportFromWebView(context: Context): Boolean {
        val cm = CookieManager.getInstance()
        cm.flush()
        val domains = listOf(
            "https://www.youtube.com" to ".youtube.com",
            "https://youtube.com" to ".youtube.com",
            "https://google.com" to ".google.com",
            "https://accounts.google.com" to ".google.com",
        )
        // Far-future expiry so yt-dlp treats them as valid session cookies.
        val expiry = (System.currentTimeMillis() / 1000L) + 60L * 60L * 24L * 365L

        val seen = HashSet<String>()
        val lines = StringBuilder("# Netscape HTTP Cookie File\n")
        var hasAuth = false

        for ((url, domain) in domains) {
            val raw = cm.getCookie(url) ?: continue
            for (pair in raw.split(";")) {
                val trimmed = pair.trim()
                val eq = trimmed.indexOf('=')
                if (eq <= 0) continue
                val name = trimmed.substring(0, eq)
                val value = trimmed.substring(eq + 1)
                val key = "$domain|$name"
                if (!seen.add(key)) continue
                if (name.contains("SAPISID") || name == "LOGIN_INFO" ||
                    name.contains("SID")
                ) hasAuth = true
                // domain  includeSubdomains  path  secure  expiry  name  value
                lines.append(domain).append('\t')
                    .append("TRUE").append('\t')
                    .append("/").append('\t')
                    .append("TRUE").append('\t')
                    .append(expiry).append('\t')
                    .append(name).append('\t')
                    .append(value).append('\n')
            }
        }

        return if (hasAuth) {
            file(context).writeText(lines.toString())
            true
        } else {
            false
        }
    }

    fun logout(context: Context) {
        runCatching { file(context).delete() }
        val cm = CookieManager.getInstance()
        cm.removeAllCookies(null)
        cm.flush()
    }
}
