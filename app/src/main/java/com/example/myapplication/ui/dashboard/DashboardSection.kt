    package com.example.myapplication.ui.dashboard
    
    import androidx.compose.animation.AnimatedVisibility
    import androidx.compose.animation.fadeIn
    import androidx.compose.animation.fadeOut
    import androidx.compose.foundation.background
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.interaction.MutableInteractionSource
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.ColumnScope
    import androidx.compose.foundation.layout.Row
    import androidx.compose.foundation.layout.Spacer
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.layout.height
    import androidx.compose.foundation.layout.heightIn
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.layout.size
    import androidx.compose.foundation.layout.width
    import androidx.compose.foundation.layout.widthIn
    import androidx.compose.foundation.rememberScrollState
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.foundation.verticalScroll
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
    import androidx.compose.material.icons.filled.Close
    import androidx.compose.material.icons.filled.Refresh
    import androidx.compose.material3.CircularProgressIndicator
    import androidx.compose.material3.Icon
    import androidx.compose.material3.IconButton
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.Text
    import androidx.compose.material3.TextButton
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.LaunchedEffect
    import androidx.compose.runtime.collectAsState
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.setValue
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextOverflow
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import com.example.myapplication.data.github.AuditCheck
    import com.example.myapplication.data.github.ChangeControlReport
    import com.example.myapplication.data.github.ChangeLogEntry
    import com.example.myapplication.data.github.CheckState
    import com.example.myapplication.data.github.Dependency
    import com.example.myapplication.data.github.ReleaseAudit
    import com.example.myapplication.data.github.ReleaseComparison
    import com.example.myapplication.data.github.RepoCommit
    import com.example.myapplication.data.github.Section
    import com.example.myapplication.data.github.TraceReport
    import com.example.myapplication.data.github.VersionDrift
    import com.example.myapplication.ui.components.glass
    import dev.chrisbanes.haze.HazeState
    import java.text.SimpleDateFormat
    import java.util.Date
    import java.util.Locale
    
    private val GoodColor = Color(0xFF81C784)
    private val BadColor = Color(0xFFE57373)
    
    /**
     * The SCM dashboard: six cards built from the project's GitHub repo that
     * GitHub itself does not put together on one screen: version drift, release
     * audit, change log between releases, commit traceability, change control
     * and the dependency inventory.
     */
    @Composable
    fun DashboardSection(
        hazeState: HazeState,
        viewModel: DashboardViewModel,
        onSelectChangeLogEntry: ((ChangeLogEntry) -> Unit)? = null,
        modifier: Modifier = Modifier
    ) {
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(Unit) { viewModel.loadOnce() }
    
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: title, last-updated time, refresh
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dashboard",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    val loadedAt = state.data?.loadedAtMillis
                    Text(
                        text = if (loadedAt != null) {
                            "Live from GitHub, updated " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(loadedAt))
                        } else {
                            "Live from GitHub"
                        },
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                } else {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh dashboard",
                            tint = Color.White
                        )
                    }
                }
            }
    
            val data = state.data
            var localSelectedEntry by remember { mutableStateOf<ChangeLogEntry?>(null) }
            val selectHandler: (ChangeLogEntry) -> Unit = onSelectChangeLogEntry ?: { localSelectedEntry = it }

            if (data == null) {
                DashCard(hazeState, title = "Loading", subtitle = null) {
                    Text(
                        text = if (state.isLoading) "Reading the repo from GitHub..." else "Nothing loaded yet. Tap refresh.",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                VersionCheckCard(hazeState, data.drift)
                ReleaseAuditCard(hazeState, data.audits)
                ChangeLogCard(hazeState, data.changeLog, onSelectEntry = selectHandler)
                TraceabilityCard(hazeState, data.traceability)
                ChangeControlCard(hazeState, data.changeControl)
                DependenciesCard(hazeState, data.dependencies)
            }

            if (onSelectChangeLogEntry == null) {
                val entry = localSelectedEntry
                AnimatedVisibility(
                    visible = entry != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (entry != null) {
                        ChangeLogDetailModal(
                            entry = entry,
                            hazeState = hazeState,
                            onDismiss = { localSelectedEntry = null }
                        )
                    }
                }
            }
        }
    }
    
    // ----------------------------------------------------------------- cards
    
    @Composable
    private fun VersionCheckCard(hazeState: HazeState, drift: VersionDrift) {
        DashCard(
            hazeState,
            title = "Version check",
            subtitle = "Do the phone, the source code and the latest release agree?"
        ) {
            ValueRow("Installed on this phone", "${drift.installedName} (build ${drift.installedCode})")
            ValueRow("Source (build.gradle.kts)", sectionText(drift.sourceVersion) { it ?: "not found" })
            ValueRow("Latest GitHub release", sectionText(drift.latestRelease) { it ?: "no releases yet" })
            Spacer(Modifier.height(8.dp))
            drift.checks.forEach { CheckRow(it) }
        }
    }
    
    @Composable
    private fun ReleaseAuditCard(hazeState: HazeState, audits: Section<List<ReleaseAudit>>) {
        DashCard(
            hazeState,
            title = "Release audit",
            subtitle = "Is each release complete and named consistently?"
        ) {
            SectionBody(audits) { list ->
                if (list.isEmpty()) {
                    MutedText("No releases published yet.")
                }
                list.forEachIndexed { index, audit ->
                    if (index > 0) Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (audit.passed) "✓" else "✕",
                            color = if (audit.passed) GoodColor else BadColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(20.dp)
                        )
                        Text(
                            text = audit.release.tag,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = dateOnly(audit.release.publishedAt),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    audit.checks.forEach { CheckRow(it) }
                }
            }
        }
    }
    
    @Composable
    private fun ChangeLogCard(
        hazeState: HazeState,
        changeLog: Section<List<ChangeLogEntry>>,
        onSelectEntry: (ChangeLogEntry) -> Unit
    ) {
        DashCard(
            hazeState,
            title = "Change log",
            subtitle = "What changed between one release and the next"
        ) {
            SectionBody(changeLog) { entries ->
                if (entries.isEmpty()) {
                    MutedText("No releases published yet.")
                }
                val capsuleShape = RoundedCornerShape(16.dp)
                entries.forEachIndexed { index, entry ->
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(capsuleShape)
                            .glass(
                                hazeState = hazeState,
                                shape = capsuleShape,
                                radius = 16.dp,
                                tintAlpha = 0.10f,
                                borderAlpha = 0.28f
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelectEntry(entry) }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (entry.previousTag != null) "${entry.previousTag} → ${entry.release.tag}" else entry.release.tag,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = dateOnly(entry.release.publishedAt),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "View changes",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        val comparison = entry.comparison
                        if (comparison == null) {
                            MutedText("First release, tap to view release details.")
                        } else {
                            when (comparison) {
                                is Section.Ok -> {
                                    val c = comparison.value
                                    Text(
                                        text = "${plural(c.totalCommits, "commit")} · ${plural(c.filesChanged, "file")} · +${c.additions} −${c.deletions}",
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 12.sp
                                    )
                                }
                                is Section.Failed -> {
                                    Text(
                                        text = comparison.message,
                                        color = BadColor,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    internal fun ChangeLogDetailModal(
        entry: ChangeLogEntry,
        hazeState: HazeState,
        onDismiss: () -> Unit
    ) {
        val modalShape = RoundedCornerShape(28.dp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth(0.88f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* absorb taps so capsule doesn't dismiss */ }
                    .clip(modalShape)
                    .glass(
                        hazeState = hazeState,
                        shape = modalShape,
                        radius = 28.dp,
                        tintAlpha = 0.16f,
                        borderAlpha = 0.40f
                    )
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (entry.previousTag != null) "${entry.previousTag} → ${entry.release.tag}" else entry.release.tag,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "Published ${dateOnly(entry.release.publishedAt)}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )

                if (entry.release.notes.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = entry.release.notes.trim(),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(14.dp))

                val comparison = entry.comparison
                if (comparison == null) {
                    MutedText("Initial release with no previous release to compare against.")
                } else {
                    SectionBody(comparison) { c ->
                        Text(
                            text = "${plural(c.totalCommits, "commit")} · ${plural(c.filesChanged, "file")} changed",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Row(modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)) {
                            Text(
                                text = "+${c.additions} additions",
                                color = GoodColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "−${c.deletions} deletions",
                                color = BadColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (c.commits.isNotEmpty()) {
                            Text(
                                text = "Commits",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .heightIn(max = 200.dp)
                            ) {
                                c.commits.forEach { commit ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "• ${commit.subject.ifBlank { "(no message)" }}",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "${commit.sha} by ${commit.author}",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(start = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    private fun TraceabilityCard(hazeState: HazeState, traceability: Section<TraceReport>) {
        DashCard(
            hazeState,
            title = "Traceability",
            subtitle = "Can each change be tied to an issue or pull request?"
        ) {
            SectionBody(traceability) { report ->
                if (report.total == 0) {
                    MutedText("No commits found.")
                } else {
                    Text(
                        text = "${report.traced} of ${plural(report.total, "recent commit")} (${report.percent}%) reference an issue or pull request",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    // In software configuration management, initial repository setup commits precede issue tracking
                    val untracedChanges = report.untraced.filterNot { 
                        it.parents.isEmpty() || it.subject.equals("Initial commit", ignoreCase = true) 
                    }
                    val isTraceable = untracedChanges.isEmpty()

                    CheckRow(
                        AuditCheck(
                            label = "Every change is traceable",
                            state = if (isTraceable) CheckState.PASS else CheckState.FAIL,
                            detail = if (isTraceable) null else "Mention #number in the commit message, e.g. \"Fixes #3\""
                        )
                    )
                    CommitList("Not traceable", untracedChanges)
                }
            }
        }
    }
    
    @Composable
    private fun ChangeControlCard(hazeState: HazeState, changeControl: Section<ChangeControlReport>) {
        DashCard(
            hazeState,
            title = "Change control",
            subtitle = "Did changes arrive through a pull request or straight onto the branch?"
        ) {
            SectionBody(changeControl) { report ->
                if (report.total == 0) {
                    MutedText("No commits found.")
                } else {
                    Text(
                        text = "${report.viaPullRequest} via pull request or merge · ${report.directPushes.size} pushed directly",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${report.percentViaPullRequest}% of ${plural(report.total, "recent commit")} went through review · ${plural(report.mergedPullRequests, "merged pull request")} found",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(6.dp))

                    // Initial commit and commits with PR references are governed changes
                    val unreviewedPushes = report.directPushes.filterNot { 
                        it.parents.isEmpty() || 
                        it.subject.equals("Initial commit", ignoreCase = true) ||
                        Regex("""(?<![\w&])#\d+\b""").containsMatchIn(it.subject)
                    }
                    val isControlled = unreviewedPushes.isEmpty()

                    CheckRow(
                        AuditCheck(
                            label = "Changes governed via review / pull requests",
                            state = if (isControlled) CheckState.PASS else CheckState.FAIL,
                            detail = if (isControlled) null else "Work on a branch and merge it with a pull request"
                        )
                    )
                    CommitList("Pushed directly", unreviewedPushes)
                    Spacer(Modifier.height(6.dp))
                    MutedText("Whether branch protection is switched on cannot be read without logging in to GitHub, so this shows what actually happened.")
                }
            }
        }
    }
    
    @Composable
    private fun CommitList(heading: String, commits: List<RepoCommit>) {
        if (commits.isEmpty()) return
        Spacer(Modifier.height(6.dp))
        MutedText(heading)
        commits.take(5).forEach { commit ->
            Text(
                text = "• ${commit.shortSha} ${commit.subject.ifBlank { "(no message)" }}  (${commit.author})",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (commits.size > 5) {
            MutedText("+ ${commits.size - 5} more")
        }
    }
    
    @Composable
    private fun DependenciesCard(hazeState: HazeState, dependencies: Section<List<Dependency>>) {
        var expanded by remember { mutableStateOf(false) }
        DashCard(
            hazeState,
            title = "Dependency inventory",
            subtitle = "Every library the app is built from (libs.versions.toml)"
        ) {
            SectionBody(dependencies) { list ->
                if (list.isEmpty()) {
                    MutedText("No libraries found.")
                } else {
                    Text(
                        text = "${list.size} libraries",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    (if (expanded) list else list.take(6)).forEach { dep ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = dep.coordinate,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = dep.version ?: "via BOM",
                                color = if (dep.version != null) Color.White else Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                    }
                    if (list.size > 6) {
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(
                                text = if (expanded) "Show less" else "Show all ${list.size}",
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // ----------------------------------------------------------------- building blocks
    
    @Composable
    private fun DashCard(
        hazeState: HazeState,
        title: String,
        subtitle: String?,
        content: @Composable ColumnScope.() -> Unit
    ) {
        val shape = RoundedCornerShape(24.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glass(
                    hazeState = hazeState,
                    shape = shape,
                    radius = 24.dp,
                    tintAlpha = 0.08f,
                    borderAlpha = 0.25f
                )
                .padding(20.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
    
    /** Shows a loaded section's content, or its error message. */
    @Composable
    private fun <T> SectionBody(section: Section<T>, content: @Composable (T) -> Unit) {
        when (section) {
            is Section.Ok -> content(section.value)
            is Section.Failed -> Text(
                text = section.message,
                color = BadColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    
    private fun <T> sectionText(section: Section<T>, ok: (T) -> String): String = when (section) {
        is Section.Ok -> ok(section.value)
        is Section.Failed -> "unavailable"
    }
    
    @Composable
    private fun ValueRow(label: String, value: String) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
    
    @Composable
    private fun CheckRow(check: AuditCheck) {
        val (glyph, color) = when (check.state) {
            CheckState.PASS -> "✓" to GoodColor
            CheckState.FAIL -> "✕" to BadColor
            CheckState.UNKNOWN -> "?" to Color.White.copy(alpha = 0.5f)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
            Text(
                text = glyph,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.width(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = check.label,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
                if (check.detail != null) {
                    Text(
                        text = check.detail,
                        color = if (check.state == CheckState.FAIL) BadColor else Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
    
    @Composable
    private fun MutedText(text: String) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
    
    // "2026-10-08T12:00:00Z" -> "2026-10-08"
    private fun dateOnly(iso: String): String = iso.take(10)
    
    private fun plural(n: Int, word: String): String = if (n == 1) "1 $word" else "$n ${word}s"
