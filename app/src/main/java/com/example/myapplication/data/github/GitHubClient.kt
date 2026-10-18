package com.example.myapplication.data.github

import com.example.myapplication.data.UpdateChecker
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GitHubException(message: String) : Exception(message)

/**
 * Reads the project's public GitHub repo through GitHub's public API. No
 * login or token (the repo is public), so GitHub allows about 60 requests
 * per hour; the dashboard stays well under that. Same approach as
 * UpdateChecker: HttpURLConnection + org.json, no extra libraries.
 *
 * Blocking network calls: run them on Dispatchers.IO.
 */
object GitHubClient {
    private const val OWNER = UpdateChecker.OWNER
    private const val REPO = UpdateChecker.REPO
    private const val API = "https://api.github.com/repos/$OWNER/$REPO"
    private const val RAW = "https://raw.githubusercontent.com/$OWNER/$REPO"
    private const val TIMEOUT_MS = 10_000

    /** The repo's default branch, falling back to "master" if it cannot be read. */
    fun fetchDefaultBranch(): String = try {
        JSONObject(get(API, "application/vnd.github+json")).optString("default_branch").ifBlank { "master" }
    } catch (e: Exception) {
        "master"
    }

    /** Published releases (drafts are not visible without login), newest first. */
    fun fetchReleases(): List<GitHubRelease> {
        val array = JSONArray(get("$API/releases?per_page=30", "application/vnd.github+json"))
        val releases = ArrayList<GitHubRelease>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            if (o.optBoolean("draft")) continue

            val assets = ArrayList<ReleaseAsset>()
            val assetArray = o.optJSONArray("assets")
            if (assetArray != null) {
                for (j in 0 until assetArray.length()) {
                    val a = assetArray.getJSONObject(j)
                    assets += ReleaseAsset(
                        name = a.stringOrEmpty("name"),
                        sizeBytes = a.optLong("size"),
                        downloadCount = a.optInt("download_count")
                    )
                }
            }
            val tag = o.stringOrEmpty("tag_name")
            releases += GitHubRelease(
                tag = tag,
                title = o.stringOrEmpty("name").ifBlank { tag },
                isPrerelease = o.optBoolean("prerelease"),
                publishedAt = o.stringOrEmpty("published_at").ifBlank { o.stringOrEmpty("created_at") },
                notes = o.stringOrEmpty("body"),
                url = o.stringOrEmpty("html_url"),
                assets = assets
            )
        }
        return releases.sortedByDescending { it.publishedAt }
    }

    /** Commits and file changes going from [baseTag] to [headTag]. */
    fun fetchComparison(baseTag: String, headTag: String): ReleaseComparison {
        val json = JSONObject(
            get("$API/compare/${encode(baseTag)}...${encode(headTag)}", "application/vnd.github+json")
        )

        val commits = ArrayList<ChangeCommit>()
        val commitArray = json.optJSONArray("commits")
        if (commitArray != null) {
            for (i in 0 until commitArray.length()) {
                val c = commitArray.getJSONObject(i)
                val commit = c.optJSONObject("commit")
                val message = commit?.stringOrEmpty("message") ?: ""
                val author = commit?.optJSONObject("author")?.stringOrEmpty("name")?.ifBlank { null }
                    ?: c.optJSONObject("author")?.stringOrEmpty("login")?.ifBlank { null }
                    ?: "unknown"
                commits += ChangeCommit(
                    sha = c.stringOrEmpty("sha").take(7),
                    subject = message.lineSequence().firstOrNull()?.trim().orEmpty(),
                    author = author
                )
            }
        }

        var additions = 0
        var deletions = 0
        var filesChanged = 0
        val fileArray = json.optJSONArray("files")
        if (fileArray != null) {
            filesChanged = fileArray.length()
            for (i in 0 until fileArray.length()) {
                val f = fileArray.getJSONObject(i)
                additions += f.optInt("additions")
                deletions += f.optInt("deletions")
            }
        }

        return ReleaseComparison(
            totalCommits = json.optInt("total_commits", commits.size),
            filesChanged = filesChanged,
            additions = additions,
            deletions = deletions,
            commits = commits.reversed() // GitHub lists oldest first
        )
    }

    /** The latest [limit] commits on [branch], newest first. */
    fun fetchRecentCommits(branch: String, limit: Int = 30): List<RepoCommit> {
        val array = JSONArray(
            get("$API/commits?sha=${encode(branch)}&per_page=$limit", "application/vnd.github+json")
        )
        val commits = ArrayList<RepoCommit>()
        for (i in 0 until array.length()) {
            val c = array.getJSONObject(i)
            val commit = c.optJSONObject("commit")
            val author = commit?.optJSONObject("author")?.stringOrEmpty("name")?.ifBlank { null }
                ?: c.optJSONObject("author")?.stringOrEmpty("login")?.ifBlank { null }
                ?: "unknown"
            val parents = ArrayList<String>()
            val parentArray = c.optJSONArray("parents")
            if (parentArray != null) {
                for (j in 0 until parentArray.length()) {
                    parents += parentArray.getJSONObject(j).stringOrEmpty("sha")
                }
            }
            commits += RepoCommit(
                sha = c.stringOrEmpty("sha"),
                message = commit?.stringOrEmpty("message") ?: "",
                author = author,
                parents = parents
            )
        }
        return commits
    }

    /** Recently closed pull requests that were actually merged (not just closed). */
    fun fetchMergedPullRequests(limit: Int = 30): List<PullRequestInfo> {
        val array = JSONArray(
            get("$API/pulls?state=closed&sort=updated&direction=desc&per_page=$limit", "application/vnd.github+json")
        )
        val pulls = ArrayList<PullRequestInfo>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            if (o.isNull("merged_at")) continue
            pulls += PullRequestInfo(
                number = o.optInt("number"),
                title = o.stringOrEmpty("title"),
                mergeCommitSha = o.stringOrEmpty("merge_commit_sha").ifBlank { null }
            )
        }
        return pulls
    }

    /** A file from the repo's default branch as plain text. */
    fun fetchRawFile(path: String, branch: String): String =
        get("$RAW/${encode(branch)}/$path", "text/plain")

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun get(url: String, accept: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", accept)
            connection.setRequestProperty("User-Agent", "FlowerDetectionApp")
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                return connection.inputStream.bufferedReader().use { it.readText() }
            }
            throw GitHubException(
                when (code) {
                    403, 429 -> "GitHub rate limit reached (60 requests an hour without login). Try again later."
                    404 -> "Not found on GitHub. Has it been pushed to the repo?"
                    else -> "GitHub returned error $code."
                }
            )
        } catch (e: GitHubException) {
            throw e
        } catch (e: IOException) {
            throw GitHubException("Could not reach GitHub. Check your internet connection.")
        } finally {
            connection.disconnect()
        }
    }

    // org.json turns a JSON null into the text "null"; treat it as empty instead.
    private fun JSONObject.stringOrEmpty(key: String): String =
        if (isNull(key)) "" else optString(key, "")
}
