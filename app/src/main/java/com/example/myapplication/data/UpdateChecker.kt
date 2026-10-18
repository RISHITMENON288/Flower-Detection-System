package com.example.myapplication.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** A GitHub release newer than what's currently installed. */
data class ReleaseInfo(val versionTag: String, val releaseUrl: String)

/**
 * Checks the project's public GitHub repo for a newer release by hitting
 * GitHub's public Releases API directly. No auth token needed (the repo
 * is public), and no new HTTP dependency: HttpURLConnection + org.json
 * are both already part of the Android SDK.
 *
 * This does blocking network I/O — every call into this object must run
 * on Dispatchers.IO. See UpdateViewModel, the only caller.
 */
object UpdateChecker {
    // Not private: the dashboard (data/github) reads the same repo.
    const val OWNER = "RISHITMENON288"
    const val REPO = "Flower-Detection-System"

    private const val TIMEOUT_MS = 8000

    /**
     * Returns the latest published release, or null on any failure — no
     * network, no releases published yet, a malformed response, GitHub
     * rate-limiting, etc. A failed check never surfaces as an error to
     * the user; it just means no update banner shows this session.
     */
    fun fetchLatestRelease(): ReleaseInfo? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("https://api.github.com/repos/$OWNER/$REPO/releases/latest")
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            ReleaseInfo(
                versionTag = json.getString("tag_name"),
                releaseUrl = json.getString("html_url")
            )
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * True if [latestTag] (e.g. "v1.2.0" or "1.2.0") names a different
     * version than [currentVersionName] (e.g. "1.0", read from the
     * installed package at runtime). Releases are published by hand for
     * this project, so "different from what's installed" is treated as
     * "newer" rather than doing a full semver comparison — in practice
     * there's never a published release tag older than what's running.
     */
    fun isNewerVersion(latestTag: String, currentVersionName: String): Boolean {
        val normalizedLatest = latestTag.removePrefix("v").trim()
        val normalizedCurrent = currentVersionName.removePrefix("v").trim()
        return normalizedLatest.isNotEmpty() && normalizedLatest != normalizedCurrent
    }
}
