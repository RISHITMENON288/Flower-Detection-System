package com.example.myapplication.data.github

/** Version-number handling for tags like "4", "v4.0", "v2.0.1" and versionName like "4.0". */
object Versions {
    /** "v4.0" -> [4, 0]. Null if it is not a plain dotted number (e.g. "final-build"). */
    fun parse(raw: String?): List<Int>? {
        if (raw == null) return null
        val cleaned = raw.trim().removePrefix("v").removePrefix("V")
        if (cleaned.isEmpty()) return null
        val numbers = ArrayList<Int>()
        for (part in cleaned.split(".")) {
            if (part.isEmpty() || !part.all { it.isDigit() }) return null
            numbers += part.toIntOrNull() ?: return null
        }
        return numbers
    }

    /** True when both are version numbers and equal, ignoring a "v" prefix and trailing zeros ("4" == "4.0"). */
    fun same(a: String?, b: String?): Boolean {
        val pa = parse(a) ?: return false
        val pb = parse(b) ?: return false
        val length = maxOf(pa.size, pb.size)
        return (0 until length).all { pa.getOrElse(it) { 0 } == pb.getOrElse(it) { 0 } }
    }
}

/** Reads facts out of the project's own files as they are on GitHub. */
object SourceParsers {
    private val VERSION_NAME = Regex("versionName\\s*=\\s*\"([^\"]+)\"")
    private val QUOTED = Regex("\"([^\"]*)\"")

    /** versionName from app/build.gradle.kts, or null if not found. */
    fun versionName(gradleFile: String): String? =
        VERSION_NAME.find(gradleFile)?.groupValues?.get(1)

    /** Every library declared in a Gradle version catalog (gradle/libs.versions.toml). */
    fun dependencies(toml: String): List<Dependency> {
        // Walk the file once, tagging each "key = value" line with its [section].
        val entries = ArrayList<Triple<String, String, String>>()
        var section = ""
        for (rawLine in toml.lineSequence()) {
            val line = rawLine.substringBefore('#').trim()
            if (line.isEmpty()) continue
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.removePrefix("[").removeSuffix("]").trim()
                continue
            }
            val eq = line.indexOf('=')
            if (eq < 0) continue
            entries += Triple(section, line.substring(0, eq).trim(), line.substring(eq + 1).trim())
        }

        val versions = HashMap<String, String>()
        for ((sec, key, value) in entries) {
            if (sec == "versions") QUOTED.find(value)?.let { versions[key] = it.groupValues[1] }
        }

        val result = ArrayList<Dependency>()
        for ((sec, key, value) in entries) {
            if (sec == "libraries") parseLibrary(key, value, versions)?.let { result += it }
        }
        return result
    }

    private fun parseLibrary(alias: String, value: String, versions: Map<String, String>): Dependency? {
        // Short form: alias = "group:name:version"
        if (value.startsWith("\"")) {
            val parts = QUOTED.find(value)?.groupValues?.get(1)?.split(":") ?: return null
            if (parts.size < 2) return null
            return Dependency(alias, parts[0] + ":" + parts[1], parts.getOrNull(2))
        }
        val body = value.substringAfter("{", "").substringBeforeLast("}")
        val module = field(body, "module")
        val group = field(body, "group")
        val name = field(body, "name")
        val coordinate = module ?: if (group != null && name != null) "$group:$name" else return null
        val version = field(body, "version")
            ?: field(body, "version.ref")?.let { versions[it] ?: "?" }
        return Dependency(alias, coordinate, version)
    }

    private fun field(body: String, key: String): String? =
        Regex("(?:^|[\\s,{])" + Regex.escape(key) + "\\s*=\\s*\"([^\"]*)\"")
            .find(body)?.groupValues?.get(1)
}

object ReleaseAudits {
    /**
     * Checks one release against the project's release conventions.
     * [latestTagUsesV] is whether the newest release's tag starts with "v";
     * older tags should follow the same style.
     */
    fun audit(release: GitHubRelease, latestTagUsesV: Boolean): ReleaseAudit {
        val tagIsVersion = Versions.parse(release.tag) != null
        val usesV = release.tag.startsWith("v") || release.tag.startsWith("V")
        val hasApk = release.assets.any { it.name.endsWith(".apk", ignoreCase = true) }
        val hasNotes = release.notes.isNotBlank()

        return ReleaseAudit(
            release = release,
            checks = listOf(
                AuditCheck(
                    "Tag is a version number",
                    if (tagIsVersion) CheckState.PASS else CheckState.FAIL,
                    if (tagIsVersion) null else "\"${release.tag}\" is not like 4 or v4.0"
                ),
                AuditCheck(
                    "Tag style matches the latest release",
                    if (usesV == latestTagUsesV) CheckState.PASS else CheckState.FAIL,
                    if (usesV == latestTagUsesV) null
                    else if (latestTagUsesV) "latest release uses a v prefix" else "latest release has no v prefix"
                ),
                AuditCheck(
                    "APK attached",
                    if (hasApk) CheckState.PASS else CheckState.FAIL,
                    if (hasApk) null else "nothing to install from this release"
                ),
                AuditCheck(
                    "Release notes written",
                    if (hasNotes) CheckState.PASS else CheckState.FAIL
                )
            )
        )
    }
}

object ScmChecks {
    /** Version drift: does what is installed match the source, and does the source match the latest release? */
    fun versionChecks(installed: String, source: String?, latestTag: String?): List<AuditCheck> {
        val installedVsSource = when {
            source == null -> AuditCheck("Installed app matches the source", CheckState.UNKNOWN, "could not read the source version")
            Versions.same(installed, source) -> AuditCheck("Installed app matches the source", CheckState.PASS)
            else -> AuditCheck("Installed app matches the source", CheckState.FAIL, "installed $installed, source $source")
        }
        val sourceVsRelease = when {
            source == null || latestTag == null -> AuditCheck("Source matches the latest release", CheckState.UNKNOWN, "needs both the source version and a release")
            Versions.same(source, latestTag) -> AuditCheck("Source matches the latest release", CheckState.PASS)
            else -> AuditCheck("Source matches the latest release", CheckState.FAIL, "source $source, latest release $latestTag")
        }
        return listOf(installedVsSource, sourceVsRelease)
    }
}
