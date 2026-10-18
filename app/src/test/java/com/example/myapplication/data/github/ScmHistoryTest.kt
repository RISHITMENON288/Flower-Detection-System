package com.example.myapplication.data.github

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScmHistoryTest {
    private fun commit(sha: String, message: String, vararg parents: String) =
        RepoCommit(sha = sha, message = message, author = "a", parents = parents.toList())

    @Test
    fun issueReferencesAreFound() {
        assertTrue(Traceability.hasReference("Fixes #12"))
        assertTrue(Traceability.hasReference("Add login (#7)"))
        assertTrue(Traceability.hasReference("Add login\n\nCloses #3"))
        assertFalse(Traceability.hasReference("Initial commit"))
        assertFalse(Traceability.hasReference("Use color #fff"))
        assertFalse(Traceability.hasReference("It&#39;s fine"))
    }

    @Test
    fun traceabilityCountsTracedCommits() {
        val report = Traceability.analyze(
            listOf(commit("c3", "Fix crash #4", "c2"), commit("c2", "tweak", "c1"), commit("c1", "Initial commit"))
        )
        assertEquals(3, report.total)
        assertEquals(1, report.traced)
        assertEquals(33, report.percent)
        assertEquals(listOf("c2", "c1"), report.untraced.map { it.sha })
    }

    @Test
    fun emptyHistoryIsFullyTraceable() {
        assertEquals(100, Traceability.analyze(emptyList()).percent)
    }

    @Test
    fun directPushesAreFlagged() {
        val report = ChangeControl.analyze(
            listOf(commit("c2", "More work", "c1"), commit("c1", "Initial commit")),
            emptyList()
        )
        assertEquals(2, report.directPushes.size)
        assertEquals(0, report.viaPullRequest)
    }

    @Test
    fun mergeCommitAndItsBranchCommitsCountAsReviewed() {
        // main: m (merge) -> a -> root;  branch: b -> a
        val commits = listOf(
            commit("m", "Merge pull request #1 from x/feature", "a", "b"),
            commit("b", "Work on feature", "a"),
            commit("a", "Setup", "root"),
            commit("root", "Initial commit")
        )
        val report = ChangeControl.analyze(commits, listOf(PullRequestInfo(1, "Feature", "m")))
        assertEquals(listOf("a", "root"), report.directPushes.map { it.sha })
        assertEquals(2, report.viaPullRequest)
        assertEquals(1, report.mergedPullRequests)
    }

    @Test
    fun squashMergedPullRequestIsRecognised() {
        val byMarker = ChangeControl.analyze(listOf(commit("s", "Add profile (#5)", "root"), commit("root", "Initial commit")), emptyList())
        assertEquals(listOf("root"), byMarker.directPushes.map { it.sha })

        val byPullList = ChangeControl.analyze(
            listOf(commit("s", "Add profile", "root"), commit("root", "Initial commit")),
            listOf(PullRequestInfo(5, "Add profile", "s"))
        )
        assertEquals(listOf("root"), byPullList.directPushes.map { it.sha })
    }

    @Test
    fun emptyHistoryHasNothingDirect() {
        val report = ChangeControl.analyze(emptyList(), emptyList())
        assertEquals(100, report.percentViaPullRequest)
        assertTrue(report.directPushes.isEmpty())
    }
}
