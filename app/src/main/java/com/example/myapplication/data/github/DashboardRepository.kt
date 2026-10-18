package com.example.myapplication.data.github

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Builds the SCM dashboard from GitHub: version drift, release audit,
 * change log between releases, dependency inventory, commit traceability
 * and change control. Each part
 * loads on its own, so one failing request only blanks that card.
 */
object DashboardRepository {
    // Each comparison is one API request; keep the total small (60/hour limit).
    private const val MAX_CHANGE_LOG_RELEASES = 5

    suspend fun load(installedName: String, installedCode: Long): DashboardData =
        withContext(Dispatchers.IO) {
            val branch = GitHubClient.fetchDefaultBranch()

            val releases: Section<List<GitHubRelease>> = section { GitHubClient.fetchReleases() }
            val sourceVersion: Section<String?> = section {
                SourceParsers.versionName(GitHubClient.fetchRawFile("app/build.gradle.kts", branch))
            }
            val dependencies: Section<List<Dependency>> = section {
                SourceParsers.dependencies(GitHubClient.fetchRawFile("gradle/libs.versions.toml", branch))
            }

            val commits: Section<List<RepoCommit>> = section { GitHubClient.fetchRecentCommits(branch) }
            val pulls: Section<List<PullRequestInfo>> = section { GitHubClient.fetchMergedPullRequests() }

            // 1. Version drift
            val latestTag = (releases as? Section.Ok)?.value?.firstOrNull()?.tag
            val sourceName = (sourceVersion as? Section.Ok)?.value
            val drift = VersionDrift(
                installedName = installedName,
                installedCode = installedCode,
                sourceVersion = sourceVersion,
                latestRelease = releases.map { it.firstOrNull()?.tag },
                checks = ScmChecks.versionChecks(installedName, sourceName, latestTag)
            )

            // 2. Release audit
            val audits: Section<List<ReleaseAudit>> = releases.map { list ->
                val latestUsesV = list.firstOrNull()?.tag?.let { it.startsWith("v") || it.startsWith("V") } ?: false
                list.map { ReleaseAudits.audit(it, latestUsesV) }
            }

            // 3. Change log: each release against the one before it
            val changeLog: Section<List<ChangeLogEntry>> = when (releases) {
                is Section.Failed -> releases
                is Section.Ok -> {
                    val list = releases.value
                    Section.Ok(
                        coroutineScope {
                            list.take(MAX_CHANGE_LOG_RELEASES).mapIndexed { index, release ->
                                val previous = list.getOrNull(index + 1)
                                async {
                                    ChangeLogEntry(
                                        release = release,
                                        previousTag = previous?.tag,
                                        comparison = previous?.let { p ->
                                            section { GitHubClient.fetchComparison(p.tag, release.tag) }
                                        }
                                    )
                                }
                            }.awaitAll()
                        }
                    )
                }
            }

            // 4. Traceability and 5. Change control, both from the commit history
            val traceability: Section<TraceReport> = commits.map { Traceability.analyze(it) }
            val changeControl: Section<ChangeControlReport> = when (commits) {
                is Section.Failed -> commits
                is Section.Ok -> when (pulls) {
                    is Section.Failed -> pulls
                    is Section.Ok -> Section.Ok(ChangeControl.analyze(commits.value, pulls.value))
                }
            }

            DashboardData(
                drift = drift,
                audits = audits,
                changeLog = changeLog,
                dependencies = dependencies,
                traceability = traceability,
                changeControl = changeControl,
                loadedAtMillis = System.currentTimeMillis()
            )
        }

    private inline fun <T> section(block: () -> T): Section<T> = try {
        Section.Ok(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: GitHubException) {
        Section.Failed(e.message ?: "GitHub request failed.")
    } catch (e: Exception) {
        Section.Failed("Unexpected response from GitHub.")
    }
}
