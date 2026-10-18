package com.example.myapplication.data.github

/**
 * Traceability: can a change be tied back to the reason it was made?
 * A commit counts as traced when its message mentions an issue or pull
 * request number such as "#12" ("Fixes #12", "Add login (#7)").
 */
object Traceability {
    // "#12" but not "&#39;" or "abc#1": the character before '#' must not be a word char or '&'.
    private val reference = Regex("""(?<![\w&])#\d+\b""")

    fun hasReference(message: String): Boolean = reference.containsMatchIn(message)

    fun analyze(commits: List<RepoCommit>): TraceReport {
        val untraced = commits.filterNot { hasReference(it.message) }
        return TraceReport(total = commits.size, traced = commits.size - untraced.size, untraced = untraced)
    }
}

/**
 * Change control: did changes reach the default branch through review
 * (a pull request or merge) or were they pushed straight onto it?
 *
 * Walks the "main line" (first parent of each commit, starting from the
 * newest). A main-line commit counts as controlled when it is a merge commit,
 * is the merge result of a merged pull request, or carries a squash-merge
 * "(#12)" suffix. Commits off the main line arrived inside a merge, so they
 * were reviewed along with it. Everything else is a direct push.
 *
 * Limit: a pull request merged with "rebase and merge" leaves no marker on
 * most of its commits, so those can show up as direct pushes.
 */
object ChangeControl {
    private val squashSuffix = Regex("""\(#\d+\)\s*$""")

    fun analyze(commits: List<RepoCommit>, pulls: List<PullRequestInfo>): ChangeControlReport {
        val bySha = commits.associateBy { it.sha }
        val mergedShas = pulls.mapNotNull { it.mergeCommitSha }.toSet()

        val mainLine = HashSet<String>()
        var current = commits.firstOrNull()
        while (current != null && mainLine.add(current.sha)) {
            current = current.parents.firstOrNull()?.let { bySha[it] }
        }

        val direct = ArrayList<RepoCommit>()
        val reviewedList = ArrayList<RepoCommit>()
        var controlled = 0
        for (commit in commits) {
            val reviewed = commit.sha !in mainLine ||
                commit.parents.size > 1 ||
                commit.sha in mergedShas ||
                commit.subject.startsWith("Merge pull request") ||
                squashSuffix.containsMatchIn(commit.subject)
            if (reviewed) {
                controlled++
                reviewedList += commit
            } else {
                direct += commit
            }
        }
        return ChangeControlReport(
            total = commits.size,
            viaPullRequest = controlled,
            directPushes = direct,
            reviewedCommits = reviewedList,
            mergedPullRequests = pulls.size
        )
    }
}
