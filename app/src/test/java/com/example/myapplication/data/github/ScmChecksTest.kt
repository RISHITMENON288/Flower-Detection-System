package com.example.myapplication.data.github

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScmChecksTest {

    @Test
    fun parse_readsVersionsAndRejectsOtherText() {
        assertEquals(listOf(2, 0), Versions.parse("v2.0"))
        assertEquals(listOf(4), Versions.parse("4"))
        assertNull(Versions.parse("final-build"))
        assertNull(Versions.parse(""))
        assertNull(Versions.parse(null))
    }

    @Test
    fun same_ignoresVPrefixAndTrailingZeros() {
        assertTrue(Versions.same("v4.0", "4"))
        assertTrue(Versions.same("4.0", "4"))
        assertFalse(Versions.same("4.1", "4"))
        assertFalse(Versions.same("4", null))
    }

    @Test
    fun versionName_isReadFromGradleFile() {
        val gradle = "defaultConfig {\n    versionCode = 4\n    versionName = \"4.0\"\n}"
        assertEquals("4.0", SourceParsers.versionName(gradle))
        assertNull(SourceParsers.versionName("nothing here"))
    }

    @Test
    fun dependencies_resolveVersionRefsAndBomLibraries() {
        val toml = """
            [versions]
            haze = "1.6.0"
            agp = "9.4.1"

            [libraries]
            haze = { group = "dev.chrisbanes.haze", name = "haze", version.ref = "haze" }
            androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
            junit = { group = "junit", name = "junit", version = "4.13.2" }

            [plugins]
            android-application = { id = "com.android.application", version.ref = "agp" }
        """.trimIndent()

        val deps = SourceParsers.dependencies(toml)
        assertEquals(3, deps.size)
        assertEquals("dev.chrisbanes.haze:haze", deps[0].coordinate)
        assertEquals("1.6.0", deps[0].version)
        assertNull(deps[1].version)
        assertEquals("4.13.2", deps[2].version)
    }

    @Test
    fun audit_flagsMissingApkAndNotes() {
        val bare = GitHubRelease(
            tag = "4", title = "Version 4", isPrerelease = false,
            publishedAt = "2026-10-08T12:00:00Z", notes = "", url = "", assets = emptyList()
        )
        val audit = ReleaseAudits.audit(bare, latestTagUsesV = false)
        assertFalse(audit.passed)
        assertEquals(CheckState.FAIL, audit.checks.first { it.label.contains("APK") }.state)
        assertEquals(CheckState.FAIL, audit.checks.first { it.label.contains("notes") }.state)
        assertEquals(CheckState.PASS, audit.checks.first { it.label.contains("version number") }.state)
    }

    @Test
    fun audit_flagsMixedTagStyle() {
        val release = GitHubRelease(
            tag = "v2.0", title = "v2.0", isPrerelease = false,
            publishedAt = "2026-10-06T12:00:00Z", notes = "ok", url = "",
            assets = listOf(ReleaseAsset("app-debug.apk", 1000, 0))
        )
        val audit = ReleaseAudits.audit(release, latestTagUsesV = false)
        assertEquals(CheckState.FAIL, audit.checks.first { it.label.contains("style") }.state)
    }

    @Test
    fun versionChecks_reportMismatchAndUnknown() {
        val ok = ScmChecks.versionChecks("4.0", "4.0", "4")
        assertTrue(ok.all { it.state == CheckState.PASS })

        val drift = ScmChecks.versionChecks("4.0", "4.1", "4")
        assertEquals(CheckState.FAIL, drift[0].state)
        assertEquals(CheckState.FAIL, drift[1].state)

        val unknown = ScmChecks.versionChecks("4.0", null, "4")
        assertEquals(CheckState.UNKNOWN, unknown[0].state)
    }
}
