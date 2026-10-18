package com.example.myapplication.data.github

/** One piece of the dashboard can load or fail without taking the others down. */
sealed interface Section<out T> {
    data class Ok<out T>(val value: T) : Section<T>
    data class Failed(val message: String) : Section<Nothing>
}

inline fun <T, R> Section<T>.map(transform: (T) -> R): Section<R> = when (this) {
    is Section.Ok -> Section.Ok(transform(value))
    is Section.Failed -> this
}

data class ReleaseAsset(val name: String, val sizeBytes: Long, val downloadCount: Int)

data class GitHubRelease(
    val tag: String,
    val title: String,
    val isPrerelease: Boolean,
    val publishedAt: String, // ISO 8601, e.g. 2026-10-08T12:00:00Z
    val notes: String,
    val url: String,
    val assets: List<ReleaseAsset>
)

data class ChangeCommit(val sha: String, val subject: String, val author: String)

/** What changed between two releases. */
data class ReleaseComparison(
    val totalCommits: Int,
    val filesChanged: Int,
    val additions: Int,
    val deletions: Int,
    val commits: List<ChangeCommit> // newest first
)

/** One library from gradle/libs.versions.toml. [version] is null when a BoM decides it. */
data class Dependency(val alias: String, val coordinate: String, val version: String?)

enum class CheckState { PASS, FAIL, UNKNOWN }

data class AuditCheck(val label: String, val state: CheckState, val detail: String? = null)

data class ReleaseAudit(val release: GitHubRelease, val checks: List<AuditCheck>) {
    val passed: Boolean get() = checks.all { it.state == CheckState.PASS }
}

data class VersionDrift(
    val installedName: String,
    val installedCode: Long,
    val sourceVersion: Section<String?>,
    val latestRelease: Section<String?>,
    val checks: List<AuditCheck>
)

data class ChangeLogEntry(
    val release: GitHubRelease,
    val previousTag: String?,                       // null for the oldest release
    val comparison: Section<ReleaseComparison>?     // null when there is no previous release
)

/** A commit on the default branch. [parents] are full SHAs; more than one means a merge commit. */
data class RepoCommit(
    val sha: String,
    val message: String,
    val author: String,
    val parents: List<String>
) {
    val subject: String get() = message.lineSequence().firstOrNull()?.trim().orEmpty()
    val shortSha: String get() = sha.take(7)
}

/** A pull request that was merged into the default branch. */
data class PullRequestInfo(val number: Int, val title: String, val mergeCommitSha: String?)

/** How many recent commits can be traced back to an issue or pull request (#123). */
data class TraceReport(
    val total: Int,
    val traced: Int,
    val untraced: List<RepoCommit>,
    val tracedCommits: List<RepoCommit> = emptyList()
) {
    val percent: Int get() = if (total == 0) 100 else traced * 100 / total
}

/** How recent commits reached the default branch: through a pull request or pushed directly. */
data class ChangeControlReport(
    val total: Int,
    val viaPullRequest: Int,
    val directPushes: List<RepoCommit>,
    val reviewedCommits: List<RepoCommit> = emptyList(),
    val mergedPullRequests: Int
) {
    val percentViaPullRequest: Int get() = if (total == 0) 100 else viaPullRequest * 100 / total
}

data class DashboardData(
    val drift: VersionDrift,
    val audits: Section<List<ReleaseAudit>>,
    val changeLog: Section<List<ChangeLogEntry>>,
    val dependencies: Section<List<Dependency>>,
    val traceability: Section<TraceReport>,
    val changeControl: Section<ChangeControlReport>,
    val loadedAtMillis: Long
)
