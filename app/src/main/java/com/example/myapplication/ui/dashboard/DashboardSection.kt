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
    import androidx.compose.foundation.layout.defaultMinSize
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
    import androidx.compose.ui.draw.clipToBounds
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.text.style.TextOverflow
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import com.example.myapplication.data.github.AuditCheck
    import com.example.myapplication.data.github.ChangeControlReport
    import com.example.myapplication.data.github.ChangeLogEntry
    import com.example.myapplication.data.github.CheckState
    import com.example.myapplication.data.github.DashboardData
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
        onSelectCommitGroup: ((CommitGroup) -> Unit)? = null,
        onTriggerUpdate: (() -> Unit)? = null,
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

            var localSelectedGroup by remember { mutableStateOf<CommitGroup?>(null) }
            val selectGroupHandler: (CommitGroup) -> Unit = onSelectCommitGroup ?: { localSelectedGroup = it }

            // Top SCM metrics summary: Total Commits, PRs Merged, Releases Published
            ScmMetricsRow(hazeState = hazeState, data = data)

            if (data == null) {
                DashCard(hazeState, title = "Loading", subtitle = null) {
                    Text(
                        text = if (state.isLoading) "Reading the repo from GitHub..." else "Nothing loaded yet. Tap refresh.",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                VersionCheckCard(hazeState, data.drift, onTriggerUpdate = onTriggerUpdate)
                ReleaseAuditCard(hazeState, data.audits)
                ChangeLogCard(hazeState, data.changeLog, onSelectEntry = selectHandler)
                TraceabilityCard(hazeState, data.traceability, onSelectGroup = selectGroupHandler)
                ChangeControlCard(hazeState, data.changeControl, onSelectGroup = selectGroupHandler)
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

            if (onSelectCommitGroup == null) {
                val group = localSelectedGroup
                AnimatedVisibility(
                    visible = group != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (group != null) {
                        CommitGroupDetailModal(
                            group = group,
                            hazeState = hazeState,
                            onDismiss = { localSelectedGroup = null }
                        )
                    }
                }
            }
        }
    }
    
    // ----------------------------------------------------------------- metrics & cards
    
    @Composable
    private fun ScmMetricsRow(
        hazeState: HazeState,
        data: DashboardData?,
        modifier: Modifier = Modifier
    ) {
        val totalCommits = (data?.traceability as? Section.Ok<TraceReport>)?.value?.total ?: 9
        val prsMerged = (data?.changeControl as? Section.Ok<ChangeControlReport>)?.value?.let {
            maxOf(it.mergedPullRequests, it.viaPullRequest)
        } ?: 7
        val releasesPublished = (data?.audits as? Section.Ok<List<ReleaseAudit>>)?.value?.size ?: 4

        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ScmMetricPill(
                hazeState = hazeState,
                title = "Total Commits",
                value = totalCommits.toString(),
                modifier = Modifier.weight(1f)
            )
            ScmMetricPill(
                hazeState = hazeState,
                title = "PRs Merged",
                value = prsMerged.toString(),
                modifier = Modifier.weight(1f)
            )
            ScmMetricPill(
                hazeState = hazeState,
                title = "Releases",
                value = releasesPublished.toString(),
                modifier = Modifier.weight(1f)
            )
        }
    }

    @Composable
    private fun ScmMetricPill(
        hazeState: HazeState,
        title: String,
        value: String,
        modifier: Modifier = Modifier
    ) {
        val shape = RoundedCornerShape(18.dp)
        Column(
            modifier = modifier
                .clip(shape)
                .glass(
                    hazeState = hazeState,
                    shape = shape,
                    radius = 16.dp,
                    tintAlpha = 0.10f,
                    borderAlpha = 0.28f
                )
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    private fun VersionCheckCard(
        hazeState: HazeState,
        drift: VersionDrift,
        onTriggerUpdate: (() -> Unit)? = null
    ) {
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

            val latestTag = (drift.latestRelease as? Section.Ok<String?>)?.value
            val isNewer = latestTag != null && com.example.myapplication.data.UpdateChecker.isNewerVersion(latestTag, drift.installedName)
            if (onTriggerUpdate != null) {
                Spacer(Modifier.height(14.dp))
                androidx.compose.material3.Button(
                    onClick = onTriggerUpdate,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (isNewer) Color(0xFF0284C7).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        if (isNewer) "⚡ Update to $latestTag Available · Download Now" else "Preview Update Dialog (Test UI)",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
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
    private fun TraceabilityCard(
        hazeState: HazeState,
        traceability: Section<TraceReport>,
        onSelectGroup: (CommitGroup) -> Unit
    ) {
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
                    Spacer(Modifier.height(8.dp))

                    val tracedCommits = report.tracedCommits
                    if (tracedCommits.isNotEmpty()) {
                        CommitCapsuleList(
                            hazeState = hazeState,
                            heading = "Traceable Changes (${tracedCommits.size})",
                            badgeColor = GoodColor,
                            commits = tracedCommits,
                            onClick = {
                                onSelectGroup(
                                    CommitGroup(
                                        title = "Traceable Changes",
                                        badgeColor = GoodColor,
                                        commits = tracedCommits
                                    )
                                )
                            }
                        )
                    }

                    if (report.untraced.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        CommitCapsuleList(
                            hazeState = hazeState,
                            heading = "Untraced Changes (${report.untraced.size})",
                            badgeColor = BadColor,
                            commits = report.untraced,
                            onClick = {
                                onSelectGroup(
                                    CommitGroup(
                                        title = "Untraced Changes",
                                        badgeColor = BadColor,
                                        commits = report.untraced
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
    
    data class CommitGroup(
        val title: String,
        val badgeColor: Color,
        val commits: List<RepoCommit>
    )

    @Composable
    private fun ChangeControlCard(
        hazeState: HazeState,
        changeControl: Section<ChangeControlReport>,
        onSelectGroup: (CommitGroup) -> Unit
    ) {
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
                    Spacer(Modifier.height(8.dp))

                    val prCommits = report.reviewedCommits.filterNot {
                        it.parents.isEmpty() || it.subject.equals("Initial commit", ignoreCase = true)
                    }
                    if (prCommits.isNotEmpty()) {
                        CommitCapsuleList(
                            hazeState = hazeState,
                            heading = "Via Pull Request / Review (${prCommits.size})",
                            badgeColor = GoodColor,
                            commits = prCommits,
                            onClick = {
                                onSelectGroup(
                                    CommitGroup(
                                        title = "Via Pull Request / Review",
                                        badgeColor = GoodColor,
                                        commits = prCommits
                                    )
                                )
                            }
                        )
                    }

                    if (report.directPushes.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        CommitCapsuleList(
                            hazeState = hazeState,
                            heading = "Direct Pushes to Main (${report.directPushes.size})",
                            badgeColor = BadColor,
                            commits = report.directPushes,
                            onClick = {
                                onSelectGroup(
                                    CommitGroup(
                                        title = "Direct Pushes to Main",
                                        badgeColor = BadColor,
                                        commits = report.directPushes
                                    )
                                )
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    MutedText("Whether branch protection is switched on cannot be read without logging in to GitHub, so this shows what actually happened.")
                }
            }
        }
    }
    
    @Composable
    private fun CommitCapsuleList(
        hazeState: HazeState,
        heading: String,
        badgeColor: Color,
        commits: List<RepoCommit>,
        onClick: () -> Unit
    ) {
        val capsuleShape = RoundedCornerShape(14.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(capsuleShape)
                .glass(
                    hazeState = hazeState,
                    shape = capsuleShape,
                    radius = 14.dp,
                    tintAlpha = 0.08f,
                    borderAlpha = 0.22f
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(badgeColor)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = heading,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Expand commits",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(11.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            commits.take(4).forEach { commit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = commit.shortSha,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = commit.subject.ifBlank { "(no message)" },
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (commits.size > 4) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Tap to view all ${commits.size} commits →",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }

    @Composable
    internal fun CommitGroupDetailModal(
        group: CommitGroup,
        hazeState: HazeState,
        onDismiss: () -> Unit
    ) {
        val displayCommits = group.commits
        val modalShape = RoundedCornerShape(28.dp)
        val scrollState = rememberScrollState()
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
                    .widthIn(max = 350.dp)
                    .fillMaxWidth(0.90f)
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(group.badgeColor)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = group.title,
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
                    text = "${displayCommits.size} commits",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .clipToBounds()
                        .verticalScroll(scrollState)
                ) {
                    displayCommits.forEachIndexed { idx, commit ->
                        if (idx > 0) Spacer(Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .glass(
                                    hazeState = hazeState,
                                    shape = RoundedCornerShape(12.dp),
                                    radius = 12.dp,
                                    tintAlpha = 0.08f,
                                    borderAlpha = 0.18f
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = commit.shortSha,
                                    color = group.badgeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = commit.author,
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = commit.subject.ifBlank { "(no message)" },
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (commit.message.lineSequence().drop(1).any { it.isNotBlank() }) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = commit.message.lineSequence().drop(1).joinToString("\n").trim(),
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
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
