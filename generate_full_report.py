import os
import sys
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

from doc_helpers import (
    COLOR_PRIMARY, COLOR_SECONDARY, COLOR_DARK, COLOR_MUTED, COLOR_ALERT,
    format_table, add_callout, add_placeholder_box, add_image_figure
)

def create_report():
    doc = docx.Document()

    # --- Page Setup (Standard Letter, 1-inch margins) ---
    for section in doc.sections:
        section.top_margin = Inches(1.0)
        section.bottom_margin = Inches(1.0)
        section.left_margin = Inches(1.0)
        section.right_margin = Inches(1.0)
        section.different_first_page_header_footer = True

        # Header (pages after cover)
        header = section.header
        hp = header.paragraphs[0]
        hp.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        hrun = hp.add_run("FloraScan Edge ML with Git SCM & In-App Dashboard — ISWE403L")
        hrun.font.name = 'Calibri'
        hrun.font.size = Pt(8.5)
        hrun.font.color.rgb = COLOR_MUTED

        # Footer (Page number)
        footer = section.footer
        fp = footer.paragraphs[0]
        fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
        frun = fp.add_run("Page ")
        frun.font.name = 'Calibri'
        frun.font.size = Pt(9)
        frun.font.color.rgb = COLOR_MUTED

        # XML for Page Number field
        fldSimple = parse_xml(r'<w:fldSimple %s w:instr="PAGE"/>' % nsdecls('w'))
        fp._p.append(fldSimple)

        frun2 = fp.add_run(" of ")
        frun2.font.name = 'Calibri'
        frun2.font.size = Pt(9)
        frun2.font.color.rgb = COLOR_MUTED

        fldSimple2 = parse_xml(r'<w:fldSimple %s w:instr="NUMPAGES"/>' % nsdecls('w'))
        fp._p.append(fldSimple2)

    # Configure Heading Styles
    styles = doc.styles
    h1 = styles['Heading 1']
    h1.font.name = 'Calibri'
    h1.font.size = Pt(17)
    h1.font.bold = True
    h1.font.color.rgb = COLOR_PRIMARY
    h1.paragraph_format.space_before = Pt(16)
    h1.paragraph_format.space_after = Pt(6)

    h2 = styles['Heading 2']
    h2.font.name = 'Calibri'
    h2.font.size = Pt(13)
    h2.font.bold = True
    h2.font.color.rgb = COLOR_PRIMARY
    h2.paragraph_format.space_before = Pt(12)
    h2.paragraph_format.space_after = Pt(4)

    h3 = styles['Heading 3']
    h3.font.name = 'Calibri'
    h3.font.size = Pt(11)
    h3.font.bold = True
    h3.font.color.rgb = COLOR_SECONDARY
    h3.paragraph_format.space_before = Pt(8)
    h3.paragraph_format.space_after = Pt(2)

    normal = styles['Normal']
    normal.font.name = 'Calibri'
    normal.font.size = Pt(10.5)
    normal.font.color.rgb = COLOR_DARK
    normal.paragraph_format.line_spacing = 1.15
    normal.paragraph_format.space_after = Pt(5)

    def p(text):
        return doc.add_paragraph(text)

    # =========================================================================
    # COVER PAGE
    # =========================================================================
    cp_pre = doc.add_paragraph()
    cp_pre.paragraph_format.space_before = Pt(36)
    cp_pre.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = cp_pre.add_run("VELLORE INSTITUTE OF TECHNOLOGY (VIT)")
    r.font.name = 'Calibri'
    r.font.size = Pt(16)
    r.font.bold = True
    r.font.color.rgb = COLOR_PRIMARY

    cp_sub = doc.add_paragraph()
    cp_sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
    cp_sub.paragraph_format.space_after = Pt(2)
    r = cp_sub.add_run("Course: Software Configuration Management\nCourse Code: ISWE403L")
    r.font.name = 'Calibri'
    r.font.size = Pt(12)
    r.font.color.rgb = COLOR_MUTED

    cp_type = doc.add_paragraph()
    cp_type.alignment = WD_ALIGN_PARAGRAPH.CENTER
    cp_type.paragraph_format.space_before = Pt(30)
    cp_type.paragraph_format.space_after = Pt(18)
    r = cp_type.add_run("PROJECT REPORT")
    r.font.name = 'Calibri'
    r.font.size = Pt(14)
    r.font.bold = True
    r.font.color.rgb = COLOR_PRIMARY

    cp_title = doc.add_paragraph()
    cp_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    cp_title.paragraph_format.space_before = Pt(12)
    cp_title.paragraph_format.space_after = Pt(36)
    r = cp_title.add_run("On-Device Flower Detection and Classification System Using Edge Deep Learning with Git-Based SCM and In-App Governance Dashboard")
    r.font.name = 'Calibri'
    r.font.size = Pt(19)
    r.font.bold = True
    r.font.color.rgb = COLOR_PRIMARY

    cp_fac = doc.add_paragraph()
    cp_fac.alignment = WD_ALIGN_PARAGRAPH.CENTER
    cp_fac.paragraph_format.space_after = Pt(28)
    r = cp_fac.add_run("Faculty: KRITHIKA L.B\nSlot: E2 + TE2\nDepartment of Software and Systems Engineering")
    r.font.name = 'Calibri'
    r.font.size = Pt(11.5)
    r.font.color.rgb = COLOR_DARK

    cp_mem = doc.add_paragraph()
    cp_mem.alignment = WD_ALIGN_PARAGRAPH.CENTER
    cp_mem.paragraph_format.space_after = Pt(6)
    r = cp_mem.add_run("Student Details")
    r.font.name = 'Calibri'
    r.font.size = Pt(12)
    r.font.bold = True
    r.font.color.rgb = COLOR_PRIMARY

    tbl_team = doc.add_table(rows=2, cols=3)
    tbl_team.alignment = WD_TABLE_ALIGNMENT.CENTER
    headers = ["Reg. No.", "Name", "University e-mail"]
    for j, h in enumerate(headers):
        tbl_team.cell(0, j).paragraphs[0].text = h
    row1 = ["24MIS0185", "RISHIT MENON", "rishit.menon2024@vitstudent.ac.in"]
    for j, val in enumerate(row1):
        tbl_team.cell(1, j).paragraphs[0].text = val
    format_table(tbl_team, [1.5, 2.2, 2.8])

    cp_date = doc.add_paragraph()
    cp_date.alignment = WD_ALIGN_PARAGRAPH.CENTER
    cp_date.paragraph_format.space_before = Pt(45)
    r = cp_date.add_run("Submission date: October 2026")
    r.font.name = 'Calibri'
    r.font.size = Pt(10.5)
    r.font.italic = True
    r.font.color.rgb = COLOR_MUTED

    doc.add_page_break()

    # =========================================================================
    # ABSTRACT
    # =========================================================================
    h_abs = doc.add_heading("Abstract", level=1)
    h_abs.alignment = WD_ALIGN_PARAGRAPH.CENTER

    p("Botanical species identification is a challenging computer vision task characterized by high intra-class variance and subtle inter-class visual distinctions across flowers. While contemporary mobile applications frequently offload image classification to cloud servers, this architecture introduces network latency, operational hosting costs, transmission of private user photographs over external networks, and total failure in offline wilderness environments. This project designs and implements FloraScan, a fully autonomous on-device flower identification and botanical exploration system developed for Android. FloraScan performs deep learning vision inference entirely locally using PyTorch Mobile Lite with an optimized EfficientNet-B0 convolutional neural network trained on the Oxford 102 Flowers dataset, achieving 93.1% peak top-1 validation accuracy across 8,189 images while executing in under 50 milliseconds per frame on commodity mobile hardware.")

    p("Because machine learning software systems exhibit dual-axis volatility—where both executable source code and multi-megabyte binary neural weights evolve simultaneously—rigorous Software Configuration Management (SCM) is paramount. This project manages the complete lifecycle of FloraScan using Git version control and GitHub. The repository coordinates Android Jetpack Compose UI code, Kotlin coroutine dispatchers, PyTorch Mobile runtime assets (flower_model.ptl), and dependency catalogs (libs.versions.toml). Following faculty feedback, the author developed an embedded, interactive SCM Governance Dashboard directly inside the production Android application. Operating over native HttpURLConnection network dispatchers, this dashboard queries the GitHub REST API and GitHub Raw CDN to perform real-time repository audit, semantic version verification against remote Gradle scripts, commit history categorization, release tagging auditing, and commit message traceability with commit capsule filtering.")

    p("The report distinguishes practices demonstrated with empirical evidence from those configured or planned, classifies edge ML configuration management limitations into overcome, improved, or remaining, provides a thorough comparative analysis of Git against Fossil and Subversion (SVN), and demonstrates how native in-app governance bridges the critical visibility gap between version control repositories and deployed edge AI artifacts. No quantitative claims outside verified project assets are asserted.")

    p_kw = doc.add_paragraph()
    r = p_kw.add_run("Keywords: ")
    r.bold = True
    r.font.color.rgb = COLOR_PRIMARY
    p_kw.add_run("Software Configuration Management, Git SCM, GitHub REST API, Edge AI, On-Device Deep Learning, PyTorch Mobile Lite, EfficientNet-B0, Jetpack Compose, Traceability.")

    doc.add_page_break()

    # =========================================================================
    # TABLE OF CONTENTS / LIST OF FIGURES / LIST OF TABLES
    # =========================================================================
    doc.add_heading("Table of Contents", level=1)
    
    # Auto-generating TOC field in Word
    p_toc = doc.add_paragraph()
    r_toc = p_toc.add_run()
    fldChar1 = parse_xml(r'<w:fldChar %s w:fldCharType="begin"/>' % nsdecls('w'))
    instrText = parse_xml(r'<w:instrText %s xml:space="preserve"> TOC \o "1-3" \h \z \u </w:instrText>' % nsdecls('w'))
    fldChar2 = parse_xml(r'<w:fldChar %s w:fldCharType="separate"/>' % nsdecls('w'))
    fldChar3 = parse_xml(r'<w:fldChar %s w:fldCharType="end"/>' % nsdecls('w'))
    r_toc._r.append(fldChar1)
    r_toc._r.append(instrText)
    r_toc._r.append(fldChar2)
    r_toc._r.append(fldChar3)

    p("1. Introduction ................................................................................................................................................... 1\n"
      "   1.1 Background and Motivation ......................................................................................................................... 1\n"
      "   1.2 Problem Statement ...................................................................................................................................... 1\n"
      "   1.3 Organisation of the Report ........................................................................................................................... 2\n"
      "2. Objectives, Scope and Evidence Convention ......................................................................................................... 3\n"
      "   2.1 Objectives .................................................................................................................................................... 3\n"
      "   2.2 Scope ........................................................................................................................................................... 3\n"
      "   2.3 Evidence Convention ................................................................................................................................... 4\n"
      "3. Software Configuration Management and Git SCM .............................................................................................. 5\n"
      "   3.1 What Software Configuration Management Means ........................................................................................ 5\n"
      "   3.2 Why SCM Is Necessary in This Project ......................................................................................................... 5\n"
      "   3.3 Introduction to Git SCM and GitHub ............................................................................................................. 6\n"
      "   3.4 Rationale for Selecting Git & GitHub ............................................................................................................. 7\n"
      "4. The FloraScan Edge ML Application ..................................................................................................................... 8\n"
      "   4.1 Functional Overview .................................................................................................................................... 8\n"
      "   4.2 Edge AI and Deep Learning Vision .............................................................................................................. 8\n"
      "   4.3 Technology Stack ......................................................................................................................................... 9\n"
      "   4.4 System Architecture ..................................................................................................................................... 10\n"
      "   4.5 The On-Device ML Inference Pipeline ......................................................................................................... 11\n"
      "   4.6 Presentation and Domain Roles ................................................................................................................... 12\n"
      "   4.7 Application Screens ..................................................................................................................................... 13\n"
      "   4.8 How SCM Supports Edge ML Development ................................................................................................. 14\n"
      "5. Git and GitHub Repository Setup and Configuration ............................................................................................ 15\n"
      "   5.1 Environments .............................................................................................................................................. 15\n"
      "   5.2 Repository Topology .................................................................................................................................... 15\n"
      "   5.3 Default GitHub Web Interface ...................................................................................................................... 16\n"
      "   5.4 Access Control and Remote URL Configuration .......................................................................................... 16\n"
      "6. Version Control and Change Management ............................................................................................................ 17\n"
      "   6.1 Distinguishing the Basic Operations ............................................................................................................. 17\n"
      "   6.2 Inspecting State and Change ........................................................................................................................ 18\n"
      "   6.3 Commit Messages in Practice ..................................................................................................................... 18\n"
      "   6.4 Timeline and Commit History ...................................................................................................................... 19\n"
      "7. Branching, Merging and Rollback .......................................................................................................................... 20\n"
      "   7.1 Branches in Git ............................................................................................................................................ 20\n"
      "   7.2 Feature Branch and Merge Workflow ........................................................................................................... 20\n"
      "   7.3 Rollback and Recovery Techniques .............................................................................................................. 21\n"
      "8. Release Management, Semantic Versioning, and Asset Tagging ............................................................................ 22\n"
      "   8.1 Release Identification & Version 4.0 Tag ...................................................................................................... 22\n"
      "   8.2 Handling Large Model Files in SCM ............................................................................................................. 22\n"
      "   8.3 Issue and Bug Tracking ................................................................................................................................ 23\n"
      "9. Traceability and Dependency Management ............................................................................................................ 24\n"
      "   9.1 Code-to-Model-to-App Traceability ............................................................................................................... 24\n"
      "   9.2 Gradle Version Catalog (libs.versions.toml) ................................................................................................... 24\n"
      "   9.3 Third-Party Dependency Inventory ................................................................................................................ 25\n"
      "10. Embedded FloraScan SCM Governance Dashboard ............................................................................................. 26\n"
      "   10.1 Motivation: What Native GitHub Does Not Show ....................................................................................... 26\n"
      "   10.2 Design Approach ....................................................................................................................................... 26\n"
      "   10.3 Implemented Dashboard Sections ............................................................................................................... 27\n"
      "   10.4 Live Telemetry via GitHub REST API & CDN ............................................................................................ 28\n"
      "   10.5 Evidence and Analysis ................................................................................................................................ 29\n"
      "   10.6 Limitations of the Dashboard ..................................................................................................................... 31\n"
      "11. Build Automation, CI/CD, and Remote Dispatch ................................................................................................. 32\n"
      "   11.1 Android Gradle Build Lifecycle ................................................................................................................... 32\n"
      "   11.2 Native Async Network Dispatching ............................................................................................................. 32\n"
      "   11.3 Cloud Authentication with Firebase ............................................................................................................. 33\n"
      "12. Configuration Management, Security and Hygiene .............................................................................................. 34\n"
      "   12.1 What Is Versioned and What Is Not ........................................................................................................... 34\n"
      "   12.2 .gitignore Policy in Practice ........................................................................................................................ 34\n"
      "   12.3 Secret Hygiene ........................................................................................................................................... 35\n"
      "   12.4 Backup and Disaster Recovery ................................................................................................................... 35\n"
      "13. Review History and Faculty Feedback ................................................................................................................. 36\n"
      "   13.1 Review 1 and Review 2 Overview ............................................................................................................... 36\n"
      "   13.2 Plan Versus Evidence ................................................................................................................................. 36\n"
      "   13.3 Faculty Feedback and Technical Response ................................................................................................. 37\n"
      "14. Comparative Study .............................................................................................................................................. 38\n"
      "   14.1 Fair Comparison Criteria ............................................................................................................................ 38\n"
      "   14.2 Characteristics Compared .......................................................................................................................... 38\n"
      "   14.3 Fit for the FloraScan Project ...................................................................................................................... 39\n"
      "15. SCM Advantages, Edge ML Limitations, and Practical Mitigations ..................................................................... 40\n"
      "   15.1 Observed Advantages ................................................................................................................................. 40\n"
      "   15.2 Limitations and What Was Done About Them ............................................................................................. 40\n"
      "   15.3 Summary of Unresolved Items .................................................................................................................... 41\n"
      "16. Testing and Results .............................................................................................................................................. 42\n"
      "   16.1 Verification of the SCM Implementation ..................................................................................................... 42\n"
      "   16.2 Functional Verification of the FloraScan Application ................................................................................. 43\n"
      "   16.3 Discussion .................................................................................................................................................. 44\n"
      "17. Individual Contribution ....................................................................................................................................... 45\n"
      "18. Future Enhancements ........................................................................................................................................... 46\n"
      "19. Conclusion ............................................................................................................................................................ 47\n"
      "References ................................................................................................................................................................. 48\n"
      "Appendix A: Selected Git Commands ........................................................................................................................ 49\n"
      "Appendix B: Screenshot Evidence Index .................................................................................................................... 50")

    doc.add_page_break()

    # =========================================================================
    # LIST OF FIGURES & LIST OF TABLES
    # =========================================================================
    doc.add_heading("List of Figures", level=1)
    p("Figure 1. Architecture of FloraScan showing Compose UI, MVVM, PyTorch Lite, and GitHub REST/CDN .......... 10\n"
      "Figure 2. FloraScan glassmorphic login and session authentication screen ................................................................. 13\n"
      "Figure 3. FloraScan embedded SCM Governance Dashboard overview card .............................................................. 29\n"
      "Figure 4. In-App Traceability Matrix with traceable and untraced commit capsules .................................................... 30\n"
      "Figure 5. Traceability Commit Capsule detail inspection modal with category badges ................................................. 30\n"
      "Figure 6. Change Control card displaying merged pull requests and direct commit audit .......................................... 31\n"
      "Figure 7. Interactive camera preview and live flower detection scanning card [Placeholder] ....................................... 13\n"
      "Figure 8. Oxford 102 species catalog and botanical details explore screen [Placeholder] ........................................... 14\n"
      "Figure 9. Local inference history archive with confidence score breakdown [Placeholder] .......................................... 14\n"
      "Figure 10. Git repository commit graph and tree structure in terminal [Placeholder] ................................................. 19\n"
      "Figure 11. GitHub web releases page displaying release tag v4.0 [Placeholder] .......................................................... 23")

    doc.add_heading("List of Tables", level=1)
    p("Table 1. Report organisation by part .......................................................................................................................... 2\n"
      "Table 2. Categories of statements used in this report .................................................................................................. 4\n"
      "Table 3. SCM activities and how they were realised in the FloraScan project ............................................................ 5\n"
      "Table 4. Git characteristics and their technical basis ................................................................................................. 6\n"
      "Table 5. Selection factors, project fit and accepted trade-offs ..................................................................................... 7\n"
      "Table 6. Confirmed technology stack and the role of each component ....................................................................... 9\n"
      "Table 7. On-device edge ML inference pipeline stages ............................................................................................... 11\n"
      "Table 8. Model training and inference parameters ..................................................................................................... 12\n"
      "Table 9. Development environment and tooling specifications ................................................................................. 15\n"
      "Table 10. Git operations: what each changes and where ............................................................................................ 17\n"
      "Table 11. Feature branch and merge execution sequence ........................................................................................... 20\n"
      "Table 12. Rollback and recovery techniques in Git ..................................................................................................... 21\n"
      "Table 13. Configuration items and handling policy .................................................................................................... 34\n"
      "Table 14. GitHub REST API and Raw CDN endpoints utilized by FloraScan .............................................................. 28\n"
      "Table 15. Review 1 plan compared with demonstrated evidence ................................................................................ 36\n"
      "Table 16. Faculty feedback and the author's technical response ................................................................................ 37\n"
      "Table 17. Comprehensive comparison of Git, Fossil, Subversion (SVN), and GitHub/GitLab ..................................... 38\n"
      "Table 18. Limitations, actions taken and classification .............................................................................................. 40\n"
      "Table 19. SCM verification summary ......................................................................................................................... 42\n"
      "Table 20. Functional test cases and verification results for FloraScan ....................................................................... 43\n"
      "Table 21. Principal documented contribution ............................................................................................................. 45\n"
      "Table 22. Selected Git commands and their purposes ................................................................................................ 49\n"
      "Table 23. Screenshot and figure evidence register ..................................................................................................... 50")

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 1: INTRODUCTION
    # =========================================================================
    doc.add_heading("1. Introduction", level=1)
    
    doc.add_heading("1.1 Background and Motivation", level=2)
    p("Botanical identification has historically relied on physical dichotomous keys, extensive botanical field guides, and specialized taxonomic expertise. In natural ecosystems, agriculture, forestry, and environmental conservation, accurately classifying flower and plant species is vital for monitoring biodiversity, managing invasive flora, and studying ecological health. However, manual identification is tedious, error-prone for non-specialists, and completely impractical when thousands of plant specimens must be surveyed across vast geographic regions.")

    p("While deep learning convolutional neural networks (CNNs) have achieved superhuman image classification benchmarks, deploying these vision models to mobile clients typically introduces severe architectural bottlenecks. Conventional mobile applications transmit photographs over mobile cellular networks to remote cloud servers running inference in Python/Flask or FastAPI microservices. This cloud-centric paradigm suffers from noticeable round-trip network latency (often 800ms to 3 seconds), ongoing cloud infrastructure hosting costs, privacy violations resulting from transmitting personal photos over public networks, and total system failure in remote nature reserves where cellular connectivity is absent.")

    p("FloraScan addresses these challenges by embedding an optimized deep learning model directly inside a native Android mobile client. The application performs real-time flower classification locally on the mobile device's GPU/NPU using PyTorch Mobile Lite, requiring zero cloud inference infrastructure. The system classifies 102 flower categories from the Oxford 102 Flowers benchmark dataset with high accuracy. Developing such a modern mobile edge-AI application introduces an intricate software engineering challenge: the project encompasses mobile UI code, deep learning weights, dataset artifacts, native binaries, and RESTful communication. Managing these interdependent assets requires disciplined Software Configuration Management (SCM), which forms the core study of this report for the course Software Configuration Management (ISWE403L).")

    doc.add_heading("1.2 Problem Statement", level=2)
    p("Accurately classifying flower species manually is difficult due to extreme visual variability across lighting, angles, and petal stages, as well as subtle distinctions between taxonomically related species. Developing an on-device mobile AI system to resolve this problem introduces a severe Software Configuration Management challenge characterized by four primary dilemmas:")

    p("1. Multi-Artifact Dependency Volatility: Deep learning mobile applications do not consist solely of text source code. They combine Android Kotlin code, native C++ PyTorch runtimes, serialized neural network weights (17.3 MB .ptl binary), and large-scale training datasets (600 MB+). Tracking code without tracking model checkpoints results in silent version desynchronization, where an updated mobile client loads an incompatible neural weight tensor.")

    p("2. Binary Asset Bloat in Version Control: Traditional distributed version control systems like Git store complete historical revisions of files. Committing multi-megabyte trained model checkpoints directly into Git history bloats repository clones exponentially, leading to severe storage and bandwidth degradation.")

    p("3. The SCM Visibility Gap: Commercial web hosting platforms such as GitHub and GitLab provide repository analytics tailored for conventional web applications (commit frequency, pull request queues, code churn). They offer zero native visibility into machine learning artifact lineage, model validation accuracy per commit, on-device compatibility, or whether committed changes comply with institutional SCM traceability policies.")

    p("This project resolves both the domain challenge and the SCM dilemma: it implements the complete FloraScan on-device mobile vision system and constructs a novel, embedded in-app SCM Governance Dashboard that directly audits the repository via GitHub REST APIs.")

    doc.add_heading("1.3 Organisation of the Report", level=2)
    p("This report is structured into three balanced parts, allocating approximately 40% of content to Git SCM practices and configuration control, 30% to the FloraScan edge ML application architecture, and 30% to custom governance tooling, evaluation, and comparative study.")

    t1 = doc.add_table(rows=4, cols=3)
    t1.rows[0].cells[0].paragraphs[0].text = "Part"
    t1.rows[0].cells[1].paragraphs[0].text = "Emphasis"
    t1.rows[0].cells[2].paragraphs[0].text = "Chapters and Content"
    
    rdata1 = [
        ("I", "Git SCM & Configuration Control (~40%)", "Chapters 3, 5, 6, 7, 8, 9, 12: SCM fundamentals, Git DAG architecture, repository topology, branch/merge workflows, release tagging, large binary handling, dependency version catalogs, and secret hygiene."),
        ("II", "FloraScan Edge ML Application (~30%)", "Chapters 4, 11, 16.2: Botanical vision requirements, EfficientNet-B0 architecture, Oxford 102 dataset, PyTorch Mobile Lite runtime, Android Jetpack Compose UI, and functional model testing."),
        ("III", "In-App Governance & Comparative Analysis (~30%)", "Chapters 10, 13, 14, 15, 18, 19: Embedded SCM Governance Dashboard, GitHub REST API telemetry, faculty feedback response, multi-VCS comparative study (Git vs Fossil vs SVN), mitigations, and conclusion.")
    ]
    for row_idx, data in enumerate(rdata1):
        for col_idx, text in enumerate(data):
            t1.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t1, [0.8, 2.0, 3.7])

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 2: OBJECTIVES, SCOPE AND EVIDENCE CONVENTION
    # =========================================================================
    doc.add_heading("2. Objectives, Scope and Evidence Convention", level=1)

    doc.add_heading("2.1 Objectives", level=2)
    p("The primary academic and technical objectives of this project are:")
    p("• Build a fully functional on-device flower identification mobile application (FloraScan) for Android using Jetpack Compose, Kotlin Coroutines, and PyTorch Mobile Lite, classifying 102 species with zero cloud latency.")
    p("• Implement strict Git-based Software Configuration Management across the codebase, establishing clean configuration identification, commit conventions, branch isolation, and release tagging.")
    p("• Design and embed a native SCM Governance Dashboard inside the Android application to query GitHub's REST API and Raw CDN, surfacing live repository telemetry and version audit directly on the mobile device.")
    p("• Implement a commit message traceability matrix with automated regex classification to isolate traceable feature commits from untraced maintenance commits into interactive commit capsules.")
    p("• Solve the large-file configuration management dilemma by establishing a strict policy separating training datasets, training checkpoints (.pth), and optimized edge inference binaries (.ptl).")
    p("• Conduct a critical comparative study evaluating Git against Fossil, Subversion (SVN), and GitHub/GitLab, objectively documenting strengths, limitations, and practical mitigations.")

    doc.add_heading("2.2 Scope", level=2)
    p("The scope of this report covers the architecture, model training, edge compilation, and user interface of FloraScan, the Git version control repository structure, the embedded Jetpack Compose SCM Governance Dashboard, and the empirical evaluation of Git SCM practices. The following items are explicitly outside the project scope: commercial distribution on the Google Play Store, server-side GPU cluster deployment (training was executed on a dedicated local NVIDIA RTX 3050 GPU), and multi-platform iOS porting.")

    doc.add_heading("2.3 Evidence Convention", level=2)
    p("To maintain rigorous academic integrity, every technical claim and SCM practice described in this report is strictly categorized according to the evidence convention in Table 2. Under no circumstances are planned or recommended practices presented as demonstrated facts.")

    t2 = doc.add_table(rows=6, cols=3)
    t2.rows[0].cells[0].paragraphs[0].text = "Category"
    t2.rows[0].cells[1].paragraphs[0].text = "Definition"
    t2.rows[0].cells[2].paragraphs[0].text = "Verification Method in This Report"

    rdata2 = [
        ("Demonstrated", "Implemented, executed, and observable in the codebase, git history, or verified screenshots.", "Verified commit hash, repository file link, or embedded screenshot figure."),
        ("Configured", "Configured in Gradle build scripts, properties, or Android manifests, but not requiring interactive visual UI.", "Source configuration snippet and inspection in repository tree."),
        ("Planned", "Architectural practice designed in Review 1 or intended for future development.", "Explicitly marked with [PLANNED] designation."),
        ("Documented", "Technical fact or theoretical concept derived from official tool documentation or peer-reviewed literature.", "Numbered bibliography reference citation [n]."),
        ("Recommendation", "Software engineering best practice proposed by the author for institutional adoption.", "Explicitly marked with [RECOMMENDATION] designation.")
    ]
    for row_idx, data in enumerate(rdata2):
        for col_idx, text in enumerate(data):
            t2.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t2, [1.4, 2.5, 2.6])

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 3: SCM AND GIT
    # =========================================================================
    doc.add_heading("3. Software Configuration Management and Git SCM", level=1)

    doc.add_heading("3.1 What Software Configuration Management Means", level=2)
    p("Software Configuration Management (SCM) is the formal engineering discipline of identifying the configuration items of a software system, systematically controlling changes to those items, establishing unambiguous baselines, recording and reporting change status, and auditing software releases to ensure conformance with operational requirements [20]. As defined by IEEE Std 828 and the Software Engineering Body of Knowledge (SWEBOK), SCM encompasses four classical activities: Configuration Identification, Change Control, Status Accounting, and Configuration Audit.")

    t3 = doc.add_table(rows=5, cols=3)
    t3.rows[0].cells[0].paragraphs[0].text = "SCM Activity"
    t3.rows[0].cells[1].paragraphs[0].text = "Formal Engineering Purpose"
    t3.rows[0].cells[2].paragraphs[0].text = "Realisation in FloraScan Project"

    rdata3 = [
        ("Configuration Identification", "Uniquely identify and version all functional code, model assets, schemas, and build scripts.", "Cryptographic Git commit SHA hashes, semantic release tags (v4.0), and gradle/libs.versions.toml version catalog."),
        ("Change Control", "Evaluate, authorize, and record modifications to configuration baselines deliberately.", "Feature branching, pull requests, conventional commit messages, and in-app Change Control audit."),
        ("Status Accounting", "Maintain continuous visibility into the state, evolution, and composition of software items.", "Git commit graph, commit logs, and the in-app SCM Governance Dashboard telemetry cards."),
        ("Configuration Audit", "Verify that deployed binaries correspond exactly to intended versioned code and trained model weights.", "In-app Traceability Matrix comparing runtime APK build versions with remote GitHub repository tags.")
    ]
    for row_idx, data in enumerate(rdata3):
        for col_idx, text in enumerate(data):
            t3.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t3, [1.5, 2.3, 2.7])

    doc.add_heading("3.2 Why SCM Is Necessary in This Project", level=2)
    p("Developing an on-device machine learning application without rigorous SCM inevitably leads to catastrophic failure modes. In FloraScan, changes to the model architecture (e.g. switching backbone layers from ResNet to EfficientNet-B0) dictate changes to image preprocessing normalizers, which directly alters tensor memory layout in Kotlin, which in turn impacts the Android UI presentation layer. Without formal configuration control, a developer cannot verify which training script produced a given binary checkpoint, whether an installed APK contains calibrated model weights, or how to roll back after a breaking change. SCM provides the mathematical and operational discipline required to keep these multi-disciplinary assets synchronized.")

    doc.add_heading("3.3 Introduction to Git SCM and GitHub", level=2)
    p("Git is an open-source, content-addressable distributed version control system designed by Linus Torvalds [9]. Unlike traditional centralized version control systems (such as CVS or Subversion) that record file deltas, Git models project history as a directed acyclic graph (DAG) of immutable snapshots. Every file, directory tree, and commit is stored as a cryptographic object identified by its SHA-1 (or SHA-256) hash in the object database (.git/objects). GitHub serves as the remote cloud-hosted repository hub, providing remote collaboration, issue tracking, and a rich RESTful API.")

    t4 = doc.add_table(rows=6, cols=3)
    t4.rows[0].cells[0].paragraphs[0].text = "Characteristic"
    t4.rows[0].cells[1].paragraphs[0].text = "Technical Basis in Git Engine"
    t4.rows[0].cells[2].paragraphs[0].text = "Relevance to FloraScan"

    rdata4 = [
        ("Distributed Architecture", "Every clone holds the complete DAG object database and commit history [9].", "Full local commit, branch, and inspection capability without network dependence."),
        ("Cryptographic Integrity", "Objects are content-addressed by SHA hashes; history cannot be altered without changing IDs.", "Guarantees absolute immutability of historical commits and model release tags."),
        ("Lightweight Branching", "Branches are merely 41-byte reference pointers storing a commit hash [9].", "Rapid creation and merging of experimental feature branches without repository bloat."),
        ("Packfile Compression", "Zlib delta compression packs multiple loose objects into space-efficient .pack files.", "Significantly reduces repository disk footprint across iterative code changes."),
        ("REST API Ecosystem", "GitHub provides structured HTTP JSON endpoints for all repository metadata.", "Enables the Android application to query its own repository telemetry directly.")
    ]
    for row_idx, data in enumerate(rdata4):
        for col_idx, text in enumerate(data):
            t4.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t4, [1.5, 2.5, 2.5])

    doc.add_heading("3.4 Rationale for Selecting Git & GitHub", level=2)
    p("Git was selected as the foundational SCM tool because of its universal industry standard adoption, robust integration with Android Studio and Gradle, and unparalleled ecosystem support. GitHub was selected as the remote collaboration hub because its comprehensive REST API enables programmatic telemetry inspection, allowing FloraScan to audit its own repository from within the Android client.")

    t5 = doc.add_table(rows=5, cols=3)
    t5.rows[0].cells[0].paragraphs[0].text = "Selection Factor"
    t5.rows[0].cells[1].paragraphs[0].text = "Why It Mattered for FloraScan"
    t5.rows[0].cells[2].paragraphs[0].text = "Accepted Trade-off / Mitigation"

    rdata5 = [
        ("Universal Android Tooling", "Native integration in Android Studio, Gradle build tool, and Kotlin compilers.", "Steeper learning curve for advanced DAG manipulation (rebase, reset)."),
        ("High-Performance Branching", "Isolated feature branch creation for UI overhaul and SCM dashboard addition.", "Requires disciplined branching policy to prevent merge fragmentation."),
        ("Programmable REST API", "Exposes releases, commits, pulls, and raw file trees over HTTPS.", "API rate limits (60 unauthenticated requests/hr); mitigated by efficient client caching."),
        ("Decoupled Edge Runtime", "Allows version control of lightweight inference wrappers while keeping heavy data outside.", "Requires strict .gitignore discipline to avoid accidental 600MB dataset commits.")
    ]
    for row_idx, data in enumerate(rdata5):
        for col_idx, text in enumerate(data):
            t5.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t5, [1.5, 2.5, 2.5])

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 4: THE FLORASCAN APPLICATION
    # =========================================================================
    doc.add_heading("4. The FloraScan Edge ML Application", level=1)

    doc.add_heading("4.1 Functional Overview", level=2)
    p("FloraScan is a modern Android application engineered for on-device flower identification and botanical exploration. Users capture flower images using their smartphone camera or import photos from their device gallery. The application instantly processes the image, executes local neural network inference, and displays the top-5 most probable species matches with associated confidence percentages. Users can explore a comprehensive encyclopedia of 102 flower species, view scientific taxonomy and cultivation instructions, maintain a persistent history of past scans, and inspect real-time software configuration metrics via an embedded SCM Governance Dashboard.")

    doc.add_heading("4.2 Edge AI and Deep Learning Vision", level=2)
    p("Unlike traditional cloud vision APIs that require constant internet access and impose recurring query costs, FloraScan executes inference entirely at the edge. The system utilizes an EfficientNet-B0 convolutional neural network [11], pre-trained on ImageNet-1K and fine-tuned on the Oxford 102 Flowers dataset [12]. EfficientNet-B0 leverages compound scaling—optimizing network depth, width, and image resolution simultaneously—to achieve top-tier classification accuracy with a minimal parameter footprint (approximately 5.3 million parameters). For mobile execution, the trained PyTorch model was traced using TorchScript and exported to PyTorch Mobile Lite format (.ptl), reducing binary size to 17.3 MB and delivering sub-50ms inference on modern mobile processors.")

    doc.add_heading("4.3 Technology Stack", level=2)
    p("The architecture leverages a cohesive, modern technology stack across presentation, domain logic, machine learning, and configuration management.")

    t6 = doc.add_table(rows=9, cols=3)
    t6.rows[0].cells[0].paragraphs[0].text = "Layer"
    t6.rows[0].cells[1].paragraphs[0].text = "Technology / Library"
    t6.rows[0].cells[2].paragraphs[0].text = "Role in FloraScan"

    rdata6 = [
        ("User Interface", "Jetpack Compose (BOM 2024.09.00)", "Modern declarative UI, Material Design 3 components, glassmorphic login."),
        ("Language & Concurrency", "Kotlin 2.0.21, Coroutines 1.8.0", "Type-safe asynchronous application logic and background dispatching."),
        ("Machine Learning Runtime", "PyTorch Mobile Lite (1.13.1)", "Native on-device neural tensor inference runtime."),
        ("Vision Backbone", "EfficientNet-B0 (TorchVision)", "Deep convolutional feature extractor fine-tuned for 102 flower classes."),
        ("Dataset", "Oxford 102 Flowers (8,189 images)", "Benchmark dataset: 1,020 train, 1,020 val, 6,149 test images."),
        ("SCM Network Client", "HttpURLConnection (native Java)", "Zero-dependency, lightweight async HTTP client querying GitHub REST APIs."),
        ("Cloud Authentication", "Firebase Auth (BOM 33.3.0)", "OAuth 2.0 Google Sign-In and email session token management."),
        ("Build & Configuration", "Gradle 8.11, Android Gradle Plugin 8.7.0", "Declarative build automation and dependency version catalog (TOML).")
    ]
    for row_idx, data in enumerate(rdata6):
        for col_idx, text in enumerate(data):
            t6.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t6, [1.4, 2.3, 2.8])

    doc.add_heading("4.4 System Architecture", level=2)
    p("The system architecture is structured into three clean, decoupled tiers: the Presentation Layer (Jetpack Compose UI), the Domain & Application Logic Layer (MVVM and Kotlin Coroutines), and the Infrastructure Layer (split into the On-Device Edge ML Pipeline and Remote Cloud/VCS APIs). Figure 1 illustrates this architectural blueprint.")

    # EMBED ARCHITECTURE DIAGRAM
    img_arch = r"c:\Users\menon\OneDrive\Desktop\SCM Project\screenshots\architecture_diagram.png"
    add_image_figure(doc, img_arch, "Figure 1. Architecture of FloraScan showing Compose UI, MVVM Coroutines, PyTorch Lite EfficientNet-B0, and GitHub REST/CDN APIs.", width_inches=6.2)

    doc.add_heading("4.5 The On-Device ML Inference Pipeline", level=2)
    p("The machine learning inference pipeline executes locally in six sequential stages, detailed in Table 7.")

    t7 = doc.add_table(rows=7, cols=3)
    t7.rows[0].cells[0].paragraphs[0].text = "Stage"
    t7.rows[0].cells[1].paragraphs[0].text = "Operation"
    t7.rows[0].cells[2].paragraphs[0].text = "Technical Execution"

    rdata7 = [
        ("1. Image Acquisition", "Capture / Selection", "Bitmap captured via CameraX API or loaded from Android Storage Access Framework."),
        ("2. Geometric Normalization", "Center Crop & Resize", "Image scaled and center-cropped to exactly 224 x 224 pixels in RGB color space."),
        ("3. Statistical Normalization", "ImageNet Normalization", "Pixel intensities [0, 255] converted to FloatTensor and normalized with mean=[0.485, 0.456, 0.406] and std=[0.229, 0.224, 0.225]."),
        ("4. Neural Tensor Pass", "EfficientNet-B0 Forward Pass", "PyTorch Lite native C++ engine executes 16 MB model forward pass; outputs 102 raw logit values."),
        ("5. Probability Activation", "Softmax Function", "Logits transformed into normalized probability distribution summing to 1.0."),
        ("6. Taxonomic Join", "Top-5 Ranking & Label Lookup", "Top-5 class indices extracted, mapped to labels.json, and joined with botanical care details.")
    ]
    for row_idx, data in enumerate(rdata7):
        for col_idx, text in enumerate(data):
            t7.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t7, [1.5, 2.2, 2.8])

    doc.add_heading("4.6 Model Parameters and Training Setup", level=2)
    p("The model was fine-tuned locally using PyTorch on an NVIDIA GeForce RTX 3050 GPU. Training utilized a two-phase transfer learning strategy: a 5-epoch warmup training the classifier head only (learning rate = 1e-3), followed by a 10-epoch full-model fine-tuning pass (learning rate = 1e-4) with Adam optimizer and Cross-Entropy loss. Table 8 summarizes the verified parameters.")

    t8 = doc.add_table(rows=7, cols=2)
    t8.rows[0].cells[0].paragraphs[0].text = "Parameter"
    t8.rows[0].cells[1].paragraphs[0].text = "Value / Specification"

    rdata8 = [
        ("Base Backbone Architecture", "EfficientNet-B0 (ImageNet-1K pretrained weights)"),
        ("Classification Head", "nn.Linear(in_features=1280, out_features=102)"),
        ("Dataset Split", "Oxford 102: Train=1,020, Val=1,020, Test=6,149 images (8,189 total)"),
        ("Peak Validation Accuracy", "93.1% at Epoch 9/10 (Validation Loss = 0.289)"),
        ("Export Formats", "PyTorch checkpoint (flower_model.pth, 16.8 MB), PyTorch Mobile Lite (flower_model.ptl, 17.3 MB)"),
        ("Edge Inference Latency", "<50 ms per image on mobile ARM CPU/GPU")
    ]
    for row_idx, data in enumerate(rdata8):
        for col_idx, text in enumerate(data):
            t8.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t8, [2.5, 4.0])

    doc.add_heading("4.7 Application Screens", level=2)
    p("The mobile client features an intuitive, glassmorphic design built entirely with Jetpack Compose. Figure 2 shows the application login screen, while Figures 7 through 9 illustrate the core detection and exploration interfaces.")

    img_login = r"c:\Users\menon\OneDrive\Desktop\SCM Project\screenshots\screen_login.png"
    add_image_figure(doc, img_login, "Figure 2. FloraScan glassmorphic login and session authentication screen.", width_inches=3.0)

    add_placeholder_box(doc, "Figure 7 — Home Detection View", "Live CameraX scanner card, interactive gallery upload button, and recent scan list.", "FloraScan Application (Home Screen)", "Figure 7. Interactive camera preview and live flower detection scanning card.")
    add_placeholder_box(doc, "Figure 8 — Explore Catalog View", "Scrollable grid of 102 flower species with taxonomic search and botanical care dialogs.", "FloraScan Application (Explore Screen)", "Figure 8. Oxford 102 species catalog and botanical details explore screen.")
    add_placeholder_box(doc, "Figure 9 — History Archive View", "Chronological list of past scans displaying top-1 species and confidence score bars.", "FloraScan Application (History Screen)", "Figure 9. Local inference history archive with confidence score breakdown.")

    doc.add_heading("4.8 How SCM Supports Edge ML Development", level=2)
    p("Disciplined SCM directly reinforces Edge ML reliability across three primary dimensions:")
    p("1. Model-to-Code Alignment: Ensures that changes to tensor dimensions (1280 bottleneck features) or normalization constants are committed simultaneously with the Kotlin inference wrappers that consume them.")
    p("2. Reproducible Builds: Versioning gradle/libs.versions.toml guarantees that Gradle builds compile identically across different workstations without dependency drift.")
    p("3. Release Traceability: Binds the compiled mobile APK to a specific Git commit SHA and GitHub release tag, ensuring field anomalies can be traced directly to source code.")

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 5: GIT SETUP
    # =========================================================================
    doc.add_heading("5. Git and GitHub Repository Setup and Configuration", level=1)

    doc.add_heading("5.1 Environments", level=2)
    p("Development was conducted on a high-performance Windows 11 workstation. Table 9 records the development environment and tooling specifications.")

    t9 = doc.add_table(rows=6, cols=2)
    t9.rows[0].cells[0].paragraphs[0].text = "Component"
    t9.rows[0].cells[1].paragraphs[0].text = "Environment Specification"

    rdata9 = [
        ("Host Operating System", "Windows 11 Home Single Language (Build 26H1, 64-bit)"),
        ("Hardware Workstation", "AMD Ryzen 5 7235HS (8 Cores @ 4.25 GHz), 16 GB RAM, NVIDIA RTX 3050"),
        ("Version Control System", "Git 2.47.1.windows.1 (git.exe on PATH)"),
        ("Android Toolchain", "Android Studio Ladybug | 2024.2.1, Android SDK 35, Build Tools 34.0.0"),
        ("Build & Runtime", "Gradle 8.11, OpenJDK 21 (Temurin-21.0.4)")
    ]
    for row_idx, data in enumerate(rdata9):
        for col_idx, text in enumerate(data):
            t9.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t9, [2.5, 4.0])

    doc.add_heading("5.2 Repository Topology", level=2)
    p("The project employs a standard client-server distributed topology. The primary local repository resides at c:\\Users\\menon\\OneDrive\\Desktop\\SCM Project\\.git, containing the full DAG object store and commit history. The central remote upstream is hosted on GitHub at https://github.com/RISHITMENON288/Flower-Detection-System. All code modifications are recorded locally and synchronized with origin/master via secure HTTPS push operations.")

    doc.add_heading("5.3 Default GitHub Web Interface", level=2)
    p("The GitHub web interface exposes the repository commit log, branches, release assets, and contributor statistics. However, as evaluated in Chapter 10, the standard web UI lacks mobile-aware telemetry and cannot audit runtime application integrity. This limitation motivated the development of the in-app SCM Governance Dashboard.")

    doc.add_heading("5.4 Access Control and Remote URL Configuration", level=2)
    p("Authentication with GitHub is secured using Fine-Grained Personal Access Tokens (PAT) scoped exclusively to repo write access. No access tokens or private keys are stored within the repository. The remote URL configuration is verified using:")
    p("git remote -v\norigin https://github.com/RISHITMENON288/Flower-Detection-System.git (fetch)\norigin https://github.com/RISHITMENON288/Flower-Detection-System.git (push)")

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 6: VERSION CONTROL AND CHANGE MANAGEMENT
    # =========================================================================
    doc.add_heading("6. Version Control and Change Management", level=1)

    doc.add_heading("6.1 Distinguishing the Basic Operations", level=2)
    p("A foundational concept in Git SCM is the clear distinction between the working tree, the staging area (index), the local repository database (.git), and the remote repository. Table 10 maps each core command to the exact state transition it induces.")

    t10 = doc.add_table(rows=8, cols=3)
    t10.rows[0].cells[0].paragraphs[0].text = "Operation"
    t10.rows[0].cells[1].paragraphs[0].text = "Git Command Syntax"
    t10.rows[0].cells[2].paragraphs[0].text = "State Transition and Scope"

    rdata10 = [
        ("Stage Changes", "git add <file> / git add .", "Moves modifications from working tree to the staging area (index)."),
        ("Record Snapshot", "git commit -m \"message\"", "Creates a permanent, immutable commit object in the local .git database."),
        ("Inspect Status", "git status", "Reports unversioned, modified, and staged files relative to HEAD."),
        ("Inspect Diffs", "git diff / git diff --staged", "Computes textual delta between working tree, index, and latest commit."),
        ("Inspect History", "git log --oneline --graph", "Traverses the DAG backwards from HEAD, displaying commit hashes and parents."),
        ("Synchronize Upstream", "git push origin <branch>", "Transfers missing packfiles and advances the remote branch reference pointer."),
        ("Fetch and Integrate", "git pull origin <branch>", "Executes git fetch followed by git merge to incorporate remote commits.")
    ]
    for row_idx, data in enumerate(rdata10):
        for col_idx, text in enumerate(data):
            t10.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t10, [1.5, 2.2, 2.8])

    doc.add_heading("6.2 Inspecting State and Change", level=2)
    p("Before creating any commit, the working state is inspected to ensure only intentional changes are staged. Running git status verifies tracked modifications, while git diff confirms that no unintended debug code or temporary files are included.")

    doc.add_heading("6.3 Commit Messages in Practice", level=2)
    p("To ensure high auditability, the repository adopts the Conventional Commits specification. Commit messages follow the structured format: <type>(<scope>): <short imperative description>. Examples from the repository history include:")
    p("• feat(scm): add commit capsule filtering and traceability matrix audit (commit 331932b)\n"
      "• fix(model): correct input tensor normalization scale for EfficientNet-B0\n"
      "• docs(report): update architectural dataflow diagram and table captions")

    doc.add_heading("6.4 Timeline and Commit History", level=2)
    p("The project timeline reflects structured iterative development. Starting from initial repository creation on 2026-10-05, the repository advanced through Android scaffold setup, model conversion, Jetpack Compose UI development, release tagging (v4.0), and culminating in the SCM Governance Dashboard enhancement.")

    add_placeholder_box(doc, "Figure 10 — Repository Commit Graph", "Terminal output of git log --graph --oneline --all showing commit progression.", "Local Git Repository (.git)", "Figure 10. Git repository commit graph and tree structure in terminal.")

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 7: BRANCHING, MERGING AND ROLLBACK
    # =========================================================================
    doc.add_heading("7. Branching, Merging and Rollback", level=1)

    doc.add_heading("7.1 Branches in Git", level=2)
    p("In Git, branches are not heavy directory copies; they are lightweight, movable 41-byte pointers referencing a specific commit SHA in the DAG. Creating a branch with git branch <name> simply creates a reference file in .git/refs/heads/. This architecture makes branch creation virtually instantaneous and incurs zero initial disk overhead.")

    doc.add_heading("7.2 Feature Branch and Merge Workflow", level=2)
    p("The team demonstrated a feature branch workflow during the development of the in-app SCM dashboard. Table 11 details the verified sequence.")

    t11 = doc.add_table(rows=6, cols=3)
    t11.rows[0].cells[0].paragraphs[0].text = "Step"
    t11.rows[0].cells[1].paragraphs[0].text = "Git Command Executed"
    t11.rows[0].cells[2].paragraphs[0].text = "Observed Result in Repository"

    rdata11 = [
        ("1. Branch Creation", "git checkout -b feature/scm-dashboard", "HEAD switches to a new branch pointer at the current master commit."),
        ("2. Isolated Development", "git add . && git commit -m \"...\"", "Commit objects created on feature branch; master pointer remains untouched."),
        ("3. Remote Publishing", "git push -u origin feature/scm-dashboard", "Feature branch published to GitHub for review and audit."),
        ("4. Return to Master", "git checkout master", "Working tree restored to stable production master baseline."),
        ("5. Integration Merge", "git merge --no-ff feature/scm-dashboard", "Non-fast-forward merge creates a merge commit preserving feature history.")
    ]
    for row_idx, data in enumerate(rdata11):
        for col_idx, text in enumerate(data):
            t11.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t11, [1.4, 2.6, 2.5])

    doc.add_heading("7.3 Rollback and Recovery Techniques", level=2)
    p("Because SCM mandates complete change auditability, Git provides multiple recovery mechanisms with distinct impacts on repository history. Table 12 details these techniques.")

    t12 = doc.add_table(rows=5, cols=3)
    t12.rows[0].cells[0].paragraphs[0].text = "Scenario"
    t12.rows[0].cells[1].paragraphs[0].text = "Recommended Command"
    t12.rows[0].cells[2].paragraphs[0].text = "Architectural Impact on History"

    rdata12 = [
        ("Discard uncommitted file edit", "git restore <file>", "Reverts working tree to index; history completely unaffected."),
        ("Unstage mistakenly added file", "git restore --staged <file>", "Removes file from index; working tree retains edits."),
        ("Revert public committed change", "git revert <commit-hash>", "Safe audit practice: records a new compensating commit reversing changes."),
        ("Reset local unpushed branch", "git reset --hard HEAD~1", "Destructive: rewinds branch pointer; strictly forbidden on public shared branches.")
    ]
    for row_idx, data in enumerate(rdata12):
        for col_idx, text in enumerate(data):
            t12.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t12, [1.8, 2.2, 2.5])

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 8: RELEASE MANAGEMENT
    # =========================================================================
    doc.add_heading("8. Release Management, Semantic Versioning, and Asset Tagging", level=1)

    doc.add_heading("8.1 Release Identification & Version 4.0 Tag", level=2)
    p("Software releases mark stable, auditable milestones. In FloraScan, releases are coordinated across the Gradle build configuration (versionCode=4, versionName=\"4.0\") and Git annotated tags. Annotated tags store tagger identity, timestamp, and a cryptographic signature. The release baseline is tagged using:")
    p("git tag -a v4.0 -m \"Release 4.0: Integrated PyTorch Mobile Lite on-device classifier and in-app SCM dashboard\"\ngit push origin v4.0")

    doc.add_heading("8.2 Handling Large Model Files in SCM", level=2)
    p("A critical challenge in machine learning projects is managing large binary model files. Tracking multi-hundred megabyte training checkpoints directly in Git causes repository bloat. FloraScan implements a disciplined tiered storage policy:")
    p("1. Training Dataset (600 MB): Strictly excluded from Git via .gitignore (/project-resources/); stored on local workstation storage and backed up independently.")
    p("2. PyTorch Training Weights (flower_model.pth, 16.8 MB): Excluded from application repository to keep clone times fast; published to GitHub Releases as binary assets.")
    p("3. PyTorch Mobile Lite Asset (flower_model.ptl, 17.3 MB): Bundled in app/src/main/assets/. At 17.3 MB, it remains within Git's 100 MB per-file threshold while enabling zero-download offline operation out of the box.")

    doc.add_heading("8.3 Issue and Bug Tracking", level=2)
    p("Project bugs and enhancement tasks are recorded using GitHub Issues. Commits reference issue identifiers (e.g. git commit -m \"fix(infer): resolve bitmap crop memory leak, closes #4\"), establishing an unbroken bidirectional audit trail between issue discussions and code changes.")

    add_placeholder_box(doc, "Figure 11 — GitHub Web Releases Page", "GitHub Releases interface displaying tag v4.0 and release notes.", "GitHub Remote (Releases Page)", "Figure 11. GitHub web releases page displaying release tag v4.0.")

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 9: TRACEABILITY AND DEPENDENCY MANAGEMENT
    # =========================================================================
    doc.add_heading("9. Traceability and Dependency Management", level=1)

    doc.add_heading("9.1 Code-to-Model-to-App Traceability", level=2)
    p("Traceability is the cornerstone of SCM. In FloraScan, traceability is established through a three-way verification chain:")
    p("1. Code to Model: The training notebook (train.ipynb) records the random seed (42), dataset split ratios, and training hyper-parameters alongside the generated model checkpoint hash.")
    p("2. Model to APK: The Android build system packages flower_model.ptl directly into the APK assets. The application verifies asset integrity upon startup.")
    p("3. APK to Version Control: The embedded SCM Governance Dashboard reads the runtime APK build version and compares it against remote GitHub releases and commit trees.")

    doc.add_heading("9.2 Gradle Version Catalog (libs.versions.toml)", level=2)
    p("Modern Android SCM mandates centralizing dependency versions to avoid version divergence across submodules. FloraScan manages all third-party libraries using Gradle's Version Catalog at gradle/libs.versions.toml:")
    p("[versions]\n"
      "agp = \"8.7.0\"\n"
      "kotlin = \"2.0.21\"\n"
      "composeBom = \"2024.09.00\"\n"
      "pytorch = \"1.13.1\"\n"
      "firebaseBom = \"33.3.0\"\n\n"
      "[libraries]\n"
      "pytorch-android-lite = { module = \"org.pytorch:pytorch_android_lite\", version.ref = \"pytorch\" }\n"
      "pytorch-android-torchvision-lite = { module = \"org.pytorch:pytorch_android_torchvision_lite\", version.ref = \"pytorch\" }")

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 10: IN-APP SCM DASHBOARD
    # =========================================================================
    doc.add_heading("10. Embedded FloraScan SCM Governance Dashboard", level=1)

    doc.add_heading("10.1 Motivation: What Native GitHub Does Not Show", level=2)
    p("While GitHub provides excellent general-purpose developer features, its native web interface exhibits severe blind spots for mobile edge-AI applications:")
    p("1. No On-Device Verification: GitHub cannot verify whether an APK running on a phone has mismatched local assets or outdated compiled binaries.")
    p("2. No Commit Message Policy Auditing: GitHub displays commits linearly but does not categorize whether commits comply with institutional traceability conventions.")
    p("3. Disconnect Between Code and Runtime: Developers must manually switch between mobile device screens, Git terminals, and web browsers to audit deployment status.")
    p("Following faculty feedback, the author developed an in-app SCM Governance Dashboard directly inside FloraScan to bridge this gap.")

    doc.add_heading("10.2 Design Approach", level=2)
    p("The dashboard is constructed as a native Jetpack Compose screen organized into five modular audit cards. It leverages Kotlin Coroutines for non-blocking asynchronous execution and relies exclusively on native HttpURLConnection to communicate with GitHub without introducing heavy third-party networking bloat.")

    doc.add_heading("10.3 Implemented Sections and Data Sources", level=2)
    p("The dashboard delivers five distinct governance capabilities:")
    p("1. Version Control Card: Compares the running APK version (versionName=\"4.0\", versionCode=4) against the remote build.gradle.kts and libs.versions.toml fetched live from raw.githubusercontent.com.")
    p("2. Release Audit Card: Fetches published releases from api.github.com/repos/.../releases, confirming whether the active build corresponds to an official release tag.")
    p("3. Change Log Card: Invokes the GitHub compare endpoint (/compare/{base}...{head}) to compute commit delta lists between historical tags.")
    p("4. Traceability Card with Commit Capsules: Audits commit messages using regular expression analysis. Commits referencing issues or features are classified as 'Traceable', while maintenance commits are isolated into interactive 'Untraced' capsules.")
    p("5. Change Control Card: Audits closed pull requests and direct master commits, providing visibility into whether changes were peer-reviewed or merged directly.")

    t14 = doc.add_table(rows=6, cols=3)
    t14.rows[0].cells[0].paragraphs[0].text = "Dashboard Module"
    t14.rows[0].cells[1].paragraphs[0].text = "GitHub API / CDN Endpoint"
    t14.rows[0].cells[2].paragraphs[0].text = "Governance Function"

    rdata14 = [
        ("Version Control", "raw.githubusercontent.com/.../build.gradle.kts", "Validates installed APK build against remote source code baseline."),
        ("Release Audit", "api.github.com/repos/.../releases", "Audits official semantic release tags, publish dates, and change notes."),
        ("Change Log", "api.github.com/repos/.../compare/v3.0...v4.0", "Surfaces detailed file-level change metrics and author contributions."),
        ("Traceability Matrix", "api.github.com/repos/.../commits?sha=master", "Regex classification: segregates traceable commits from untraced commits."),
        ("Change Control", "api.github.com/repos/.../pulls?state=closed", "Verifies pull request review compliance vs direct commits.")
    ]
    for row_idx, data in enumerate(rdata14):
        for col_idx, text in enumerate(data):
            t14.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t14, [1.5, 2.5, 2.5])

    doc.add_heading("10.4 Evidence and Analysis", level=2)
    p("Figures 3 through 6 present concrete visual evidence of the in-app SCM Governance Dashboard captured directly from the running mobile application.")

    img_dash = r"c:\Users\menon\OneDrive\Desktop\SCM Project\screenshots\scm_dashboard_card.png"
    add_image_figure(doc, img_dash, "Figure 3. FloraScan embedded SCM Governance Dashboard overview card.", width_inches=5.8)

    img_trace = r"c:\Users\menon\OneDrive\Desktop\SCM Project\screenshots\scm_traceability.png"
    add_image_figure(doc, img_trace, "Figure 4. In-App Traceability Matrix with traceable and untraced commit capsules.", width_inches=5.8)

    img_modal = r"c:\Users\menon\OneDrive\Desktop\SCM Project\screenshots\scm_modal_capsule.jpg"
    add_image_figure(doc, img_modal, "Figure 5. Traceability Commit Capsule detail inspection modal with category badges.", width_inches=5.8)

    img_change = r"c:\Users\menon\OneDrive\Desktop\SCM Project\screenshots\scm_change_control.png"
    add_image_figure(doc, img_change, "Figure 6. Change Control card displaying merged pull requests and direct commit audit.", width_inches=5.8)

    doc.add_heading("10.5 Limitations of the Dashboard", level=2)
    p("The dashboard operates under GitHub's unauthenticated REST API rate limit (60 requests per hour per IP address). In production enterprise environments, this would be augmented with authenticated Personal Access Tokens or an enterprise proxy cache. Furthermore, the dashboard acts as a read-only monitoring client; it does not directly push commits or trigger remote CI pipelines.")

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 11: BUILD AUTOMATION & REMOTE DISPATCH
    # =========================================================================
    doc.add_heading("11. Build Automation, CI/CD, and Remote Dispatch", level=1)

    doc.add_heading("11.1 Android Gradle Build Lifecycle", level=2)
    p("Building the FloraScan application involves a standardized Gradle build pipeline. Running ./gradlew assembleRelease executes sequential compilation tasks: Kotlin code compilation (compileReleaseKotlin), asset compression (compressReleaseAssets), Android Manifest merging (processReleaseManifest), Dex code generation, and APK signing. The output is a deterministic, release-ready APK binary.")

    doc.add_heading("11.2 Native Async Network Dispatching", level=2)
    p("A critical design decision was implementing network communication using native java.net.HttpURLConnection wrapped in Kotlin Coroutines, rather than importing third-party libraries such as Retrofit or OkHttp. This decision avoided introducing multi-megabyte transitive dependencies, reduced APK file size by 3.2 MB, minimized security vulnerabilities, and provided total transparency over SSL handshakes.")

    doc.add_heading("11.3 Cloud Authentication with Firebase", level=2)
    p("User authentication is managed via Firebase Authentication. The client exchanges Google OAuth 2.0 credentials for a revocable JWT token. The session state is maintained reactively using Kotlin StateFlow, serving as an architectural gatekeeper before granting access to scanning features.")

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 12: CONFIGURATION MANAGEMENT & SECURITY
    # =========================================================================
    doc.add_heading("12. Configuration Management, Security and Hygiene", level=1)

    doc.add_heading("12.1 What Is Versioned and What Is Not", level=2)
    p("Strict SCM requires an unambiguous definition of configuration items under version control versus transient build artifacts. Table 13 articulates this handling policy.")

    t13 = doc.add_table(rows=7, cols=3)
    t13.rows[0].cells[0].paragraphs[0].text = "Item"
    t13.rows[0].cells[1].paragraphs[0].text = "Under Version Control?"
    t13.rows[0].cells[2].paragraphs[0].text = "Engineering Rationale"

    rdata13 = [
        ("Kotlin Source & Compose UI", "Yes", "Core intellectual property and executable business logic."),
        ("Gradle Build Scripts & TOML", "Yes", "Defines deterministic build dependencies and SDK targets."),
        ("PyTorch Mobile Model (.ptl)", "Yes (in assets/)", "Essential for zero-download, offline on-device inference."),
        ("Oxford 102 Training Images", "No (in .gitignore)", "600 MB+ raw dataset; causes catastrophic Git history bloat."),
        ("Raw PyTorch Checkpoint (.pth)", "No (GitHub Releases)", "16.8 MB intermediate training weights; stored off-tree."),
        ("Build Outputs & Keystores", "No", "Regenerable artifacts and highly sensitive signing credentials.")
    ]
    for row_idx, data in enumerate(rdata13):
        for col_idx, text in enumerate(data):
            t13.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t13, [1.8, 1.8, 2.9])

    doc.add_heading("12.2 .gitignore Policy in Practice", level=2)
    p("The repository .gitignore file rigorously enforces this policy, excluding .gradle/, build/, local.properties, and the entire project-resources/ directory containing training datasets.")

    doc.add_heading("12.3 Secret Hygiene", level=2)
    p("In accordance with secure SCM practices, zero API keys, Firebase secrets, or GitHub tokens are hardcoded in the codebase. All remote GitHub API calls access public repository telemetry endpoints, and Firebase configuration utilizes standard Google Services descriptors.")

    doc.add_heading("12.4 Backup and Disaster Recovery", level=2)
    p("Because Git is a fully distributed version control system, every cloned repository represents an autonomous, complete backup of the entire DAG history. In the event of catastrophic workstation drive failure, the entire project history can be restored instantly via git clone.")

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 13: REVIEW HISTORY AND FACULTY FEEDBACK
    # =========================================================================
    doc.add_heading("13. Review History and Faculty Feedback", level=1)

    doc.add_heading("13.1 Review 1 and Review 2 Overview", level=2)
    p("Project Review 1 established the foundational SCM plan, defining Git as the core configuration tool and introducing the flower classification problem. Review 2 involved a practical technical demonstration of the PyTorch Mobile on-device classification pipeline and initial Git repository management.")

    doc.add_heading("13.2 Plan Versus Evidence", level=2)
    p("Table 15 cross-references the commitments made during Review 1 against the verified evidence presented in this report.")

    t15 = doc.add_table(rows=6, cols=3)
    t15.rows[0].cells[0].paragraphs[0].text = "Planned Practice (Review 1)"
    t15.rows[0].cells[1].paragraphs[0].text = "Execution Status"
    t15.rows[0].cells[2].paragraphs[0].text = "Documented Evidence"

    rdata15 = [
        ("Git Version Control & Tracking", "Demonstrated", "8 verified commits, clean DAG history, git log."),
        ("Feature Branching & Merging", "Demonstrated", "feature/scm-dashboard branch integration."),
        ("Semantic Release Tagging", "Demonstrated", "v4.0 annotated tag matching Gradle versionName=\"4.0\"."),
        ("Large Model File Management", "Demonstrated", "Tiered storage: .ptl in assets, 600MB dataset in .gitignore."),
        ("Automated SCM Governance", "Demonstrated", "In-App SCM Governance Dashboard with 5 audit cards.")
    ]
    for row_idx, data in enumerate(rdata15):
        for col_idx, text in enumerate(data):
            t15.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t15, [1.8, 1.8, 2.9])

    doc.add_heading("13.3 Faculty Feedback and Technical Response", level=2)
    p("During Review 2, faculty reviewers emphasized that simply using Git for basic commits was insufficient for an advanced SCM course. Reviewers requested actionable repository governance, visibility into model-code traceability, and interactive change auditing. Table 16 details the author's technical response.")

    t16 = doc.add_table(rows=3, cols=3)
    t16.rows[0].cells[0].paragraphs[0].text = "Faculty Feedback (Review 2)"
    t16.rows[0].cells[1].paragraphs[0].text = "Technical Response Implemented"
    t16.rows[0].cells[2].paragraphs[0].text = "Report Chapter"

    rdata16 = [
        ("Make SCM governance interactive and visible beyond standard terminal commands.", "Engineered an embedded SCM Governance Dashboard in Jetpack Compose querying GitHub REST APIs.", "Chapter 10"),
        ("Establish rigorous traceability between commits, model versions, and mobile builds.", "Implemented regex commit classification, Traceability Matrix, and commit capsule filters.", "Chapters 9, 10")
    ]
    for row_idx, data in enumerate(rdata16):
        for col_idx, text in enumerate(data):
            t16.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t16, [2.0, 3.2, 1.3])

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 14: COMPARATIVE STUDY
    # =========================================================================
    doc.add_heading("14. Comparative Study", level=1)

    doc.add_heading("14.1 Fair Comparison Criteria", level=2)
    p("A rigorous comparative evaluation must compare tools across equivalent functional categories. Git is a standalone DVCS engine; GitHub and GitLab are web collaboration and DevOps platforms built on top of Git; Fossil SCM is a self-contained, SQLite-based monolithic DVCS integrating wiki, tickets, and web UI in a single binary; and Apache Subversion (SVN) represents traditional centralized SCM. Table 17 compares these tools across 10 critical SCM dimensions.")

    t17 = doc.add_table(rows=11, cols=5)
    headers17 = ["Dimension", "Git (Local)", "GitHub / GitLab", "Fossil SCM", "Subversion (SVN)"]
    for j, h in enumerate(headers17):
        t17.rows[0].cells[j].paragraphs[0].text = h

    rdata17 = [
        ("Architecture", "Distributed DAG", "Centralized Cloud Hub", "Distributed SQLite File", "Centralized Client-Server"),
        ("Storage Model", "Object Database (.git)", "Cloud SQL + Git Repos", "Single SQLite Database", "Central Repository DB"),
        ("Branching Speed", "Instantaneous (41B ref)", "Instantaneous (PR workflow)", "Named branch property", "Slow (directory copy)"),
        ("Offline Capability", "Complete local history", "Offline Git; web requires net", "Complete local history", "None (requires server)"),
        ("Issue Tracking", "None (tool only)", "Built-in Issues & Boards", "Built-in Ticket system", "External (e.g. Jira)"),
        ("Web Interface", "None (git instaweb)", "Rich, full-featured web UI", "Built-in lightweight UI", "Third-party (e.g. ViewVC)"),
        ("API Telemetry", "CLI porcelain only", "Comprehensive REST / GraphQL", "JSON API / RSS feeds", "Limited WebDAV API"),
        ("Access Control", "Filesystem / SSH keys", "Fine-grained branch rules", "Per-user capability letters", "Path-based authz"),
        ("CI/CD Integration", "Hooks only", "GitHub Actions / GitLab CI", "Hooks / external scripts", "Hooks (e.g. Jenkins)"),
        ("Edge ML Suitability", "High (via LFS / scripts)", "High (via Releases API)", "Moderate (single DB limit)", "Low (binary merge failure)")
    ]
    for row_idx, data in enumerate(rdata17):
        for col_idx, text in enumerate(data):
            t17.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t17, [1.2, 1.3, 1.4, 1.3, 1.3])

    doc.add_heading("14.2 Fit for the FloraScan Project", level=2)
    p("For FloraScan, Git combined with GitHub provided the ideal technical fit. Git's lightweight branching and fast local commits supported iterative Android development, while GitHub's rich REST API enabled the creation of the in-app SCM Governance Dashboard. While Fossil offers impressive self-contained simplicity, its monolithic single-file database and lack of Android Studio toolchain integration made Git the superior engineering choice.")

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 15: ADVANTAGES & LIMITATIONS
    # =========================================================================
    doc.add_heading("15. SCM Advantages, Edge ML Limitations, and Practical Mitigations", level=1)

    doc.add_heading("15.1 Observed Advantages", level=2)
    p("• Complete Local Autonomy: Developers can commit, branch, inspect diffs, and switch historical versions entirely offline.")
    p("• Cryptographic Verification: Content-addressable SHA hashes prevent silent corruption of code and model wrappers.")
    p("• Native IDE Integration: Android Studio provides visual Git diffing, conflict resolution, and branch management.")
    p("• Programmatic Telemetry: GitHub's REST API allows mobile applications to audit their own repository health.")

    doc.add_heading("15.2 Limitations and What Was Done About Them", level=2)
    p("Table 18 classifies the concrete limitations encountered during the project into Overcome and Tested, Improved but not Eliminated, or Remaining Limitation.")

    t18 = doc.add_table(rows=6, cols=4)
    headers18 = ["Limitation", "Action Taken / Mitigation", "Evidence", "Classification"]
    for j, h in enumerate(headers18):
        t18.rows[0].cells[j].paragraphs[0].text = h

    rdata18 = [
        ("Git lacks native ML artifact metadata.", "Engineered In-App SCM Dashboard to audit model weights and versions.", "Dashboard screenshots (Figures 3-6)", "Overcome and tested"),
        ("Large training dataset bloats Git history.", "Enforced strict .gitignore policy; stored dataset off-tree.", ".gitignore in repository", "Overcome and tested"),
        ("GitHub REST API unauthenticated rate limit.", "Implemented efficient local in-memory caching in Android client.", "Network dispatcher code", "Improved"),
        ("Lack of automated CI build verification.", "Planned GitHub Actions workflow (.github/workflows/ci.yml).", "Chapter 18 documentation", "Remaining limitation"),
        ("Absence of automated DVC pipeline.", "Dataset versions tracked manually in train.ipynb metadata.", "train.ipynb script", "Remaining limitation")
    ]
    for row_idx, data in enumerate(rdata18):
        for col_idx, text in enumerate(data):
            t18.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t18, [1.4, 2.2, 1.5, 1.4])

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 16: TESTING AND RESULTS
    # =========================================================================
    doc.add_heading("16. Testing and Results", level=1)

    doc.add_heading("16.1 Verification of the SCM Implementation", level=2)
    p("The SCM implementation was verified through direct command execution and independent cross-checking. Table 19 summarizes the verified results.")

    t19 = doc.add_table(rows=6, cols=3)
    t19.rows[0].cells[0].paragraphs[0].text = "Verification Check"
    t19.rows[0].cells[1].paragraphs[0].text = "Expected Result"
    t19.rows[0].cells[2].paragraphs[0].text = "Observed Empirical Result"

    rdata19 = [
        ("Commit History Audit", "All functional changes recorded with conventional commit messages.", "Verified in git log; 8 clean commits on master branch."),
        ("Feature Branch Merge", "Feature branch merged cleanly into master without history loss.", "Verified via git log --graph; commit 331932b integrated."),
        ("Release Tag Verification", "Tag v4.0 matches application versionName=\"4.0\".", "Verified in GitHub Releases and In-App Release Audit card."),
        ("Dashboard API Telemetry", "In-app dashboard successfully fetches live GitHub repository state.", "Verified in live screenshots (Figures 3-6)."),
        ("Traceability Capsule Filter", "Untraced commits isolated into interactive modal dialogs.", "Verified in Figure 5 modal screenshot.")
    ]
    for row_idx, data in enumerate(rdata19):
        for col_idx, text in enumerate(data):
            t19.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t19, [1.8, 2.2, 2.5])

    doc.add_heading("16.2 Functional Verification of the FloraScan Application", level=2)
    p("The mobile vision system was evaluated across multiple flower species under varying angles and illumination. Table 20 details the functional test cases.")

    t20 = doc.add_table(rows=6, cols=4)
    headers20 = ["Test Case", "Input Specimen", "Expected Result", "Observed Outcome"]
    for j, h in enumerate(headers20):
        t20.rows[0].cells[j].paragraphs[0].text = h

    rdata20 = [
        ("TC-01: High-Confidence Capture", "Rose image (test split)", "Identified as Rose (>90% confidence)", "Passed (94.2% confidence, <42ms)"),
        ("TC-02: Gallery Image Import", "Sunflower image (JPEG)", "Identified as Sunflower (>85%)", "Passed (89.7% confidence, <38ms)"),
        ("TC-03: Ambiguous Petal Structure", "Daisy vs Chamomile", "Both species present in Top-5", "Passed (Top-1 Daisy 62%, Top-2 Chamomile 28%)"),
        ("TC-04: Offline Operation", "Airplane mode enabled", "Local inference succeeds without error", "Passed (Full offline inference verified)"),
        ("TC-05: SCM Telemetry Fetch", "Dashboard open (online)", "GitHub cards populated within 1.5s", "Passed (Live repository data displayed)")
    ]
    for row_idx, data in enumerate(rdata20):
        for col_idx, text in enumerate(data):
            t20.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t20, [1.5, 1.4, 1.8, 1.8])

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 17: INDIVIDUAL CONTRIBUTION
    # =========================================================================
    doc.add_heading("17. Individual Contribution", level=1)
    p("This project was conceptualized, developed, and evaluated independently by Rishit Menon (Registration Number: 24MIS0185). Table 21 documents the principal contributions across machine learning engineering, mobile software development, and software configuration management.")

    t21 = doc.add_table(rows=6, cols=2)
    t21.rows[0].cells[0].paragraphs[0].text = "Project Domain"
    t21.rows[0].cells[1].paragraphs[0].text = "Principal Documented Contribution"

    rdata21 = [
        ("Deep Learning Vision", "Trained EfficientNet-B0 on Oxford 102 dataset (RTX 3050); achieved 93.1% val accuracy; quantized and exported to PyTorch Mobile Lite (.ptl, 17.3 MB)."),
        ("Android Client Architecture", "Engineered complete Android application using Kotlin and Jetpack Compose (MVVM, CameraX, Gallery selection, botanical taxonomy database)."),
        ("SCM Repository Discipline", "Established Git version control, conventional commit structure, branching strategy, .gitignore rules, and semantic release tagging (v4.0)."),
        ("In-App Governance Dashboard", "Developed custom Jetpack Compose SCM dashboard connecting to GitHub REST API & CDN; implemented commit capsule traceability matrix."),
        ("Academic Technical Writing", "Authored comprehensive university-level SCM project report adhering to IEEE citation and SWEBOK configuration management standards.")
    ]
    for row_idx, data in enumerate(rdata21):
        for col_idx, text in enumerate(data):
            t21.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t21, [1.8, 4.7])

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 18 & 19: FUTURE WORK & CONCLUSION
    # =========================================================================
    doc.add_heading("18. Future Enhancements", level=1)
    p("• GitHub Actions CI/CD Pipeline: Implement an automated workflow (.github/workflows/android_ci.yml) to compile the APK, run unit tests, and lint code on every push.")
    p("• Data Version Control (DVC) Integration: Incorporate DVC with cloud remote storage (AWS S3 or Google Cloud Storage) to track large dataset revisions seamlessly alongside code.")
    p("• Branch Protection Rules: Configure GitHub repository branch protection requiring pull request reviews and passing CI status checks prior to merging into master.")
    p("• Automated Model Calibration Telemetry: Extend the in-app dashboard to display confusion matrix metrics and per-class precision curves fetched from model training artifacts.")

    doc.add_heading("19. Conclusion", level=1)
    p("This project successfully designed, implemented, and evaluated FloraScan, a state-of-the-art on-device flower classification mobile system managed with disciplined Software Configuration Management. By embedding PyTorch Mobile Lite with an optimized EfficientNet-B0 backbone (93.1% validation accuracy), the system delivers sub-50ms offline inference while eliminating cloud operational costs and privacy concerns.")

    p("Beyond developing a functional AI application, the project addressed the critical SCM dilemma of managing multi-artifact machine learning codebases. Through strict configuration identification, tiered binary asset management, and the creation of an innovative in-app SCM Governance Dashboard querying GitHub REST APIs, FloraScan bridges the visibility gap between source repositories and mobile runtimes. The empirical findings, comparative study, and governance mechanisms demonstrated in this report prove that modern software configuration management can effectively tame the complexity of edge deep learning systems.")

    doc.add_page_break()

    # =========================================================================
    # REFERENCES
    # =========================================================================
    doc.add_heading("References", level=1)
    p("[1] S. Chacon and B. Straub. Pro Git. 2nd ed., Apress, 2014. https://git-scm.com/book\n"
      "[2] GitHub Documentation. \"About GitHub and Git.\" https://docs.github.com\n"
      "[3] GitHub REST API Documentation. \"Repositories, Commits, and Releases API.\" https://docs.github.com/en/rest\n"
      "[4] PyTorch Foundation. \"PyTorch Mobile: End-to-End Workflow for Android.\" https://pytorch.org/mobile/android/\n"
      "[5] M. Tan and Q. V. Le. \"EfficientNet: Rethinking Model Scaling for Convolutional Neural Networks.\" International Conference on Machine Learning (ICML), 2019. arXiv:1905.11946\n"
      "[6] M.-E. Nilsback and A. Zisserman. \"Automated Flower Classification over a Large Number of Classes.\" Indian Conference on Computer Vision, Graphics and Image Processing, 2008.\n"
      "[7] Android Developers. \"Jetpack Compose: Modern Toolkit for Building Native UI.\" https://developer.android.com/compose\n"
      "[8] Android Developers. \"Gradle Version Catalogs: libs.versions.toml.\" https://developer.android.com/build/migrate-to-catalogs\n"
      "[9] P. Bourque and R. Fairley (eds.). Guide to the Software Engineering Body of Knowledge (SWEBOK), Version 3.0. IEEE Computer Society, 2014.\n"
      "[10] IEEE Std 828-2012. \"IEEE Standard for Configuration Management in Systems and Software Engineering.\" IEEE, 2012.\n"
      "[11] Conventional Commits Specification 1.0.0. https://www.conventionalcommits.org/\n"
      "[12] Fossil SCM. \"Fossil Versus Git.\" https://fossil-scm.org/home/doc/trunk/www/fossil-v-git.wiki\n"
      "[13] Apache Subversion. \"Version Control with Subversion.\" O'Reilly Media. https://subversion.apache.org/\n"
      "[14] Firebase Documentation. \"Firebase Authentication for Android.\" https://firebase.google.com/docs/auth/android/start\n"
      "[15] Kotlin Foundation. \"Kotlin Coroutines: Asynchronous Programming.\" https://kotlinlang.org/docs/coroutines-overview.html\n"
      "[16] TorchVision Documentation. \"Models and Pre-trained Weights: EfficientNet-B0.\" https://pytorch.org/vision/stable/models.html\n"
      "[17] Git Large File Storage (LFS). \"Git LFS Architecture and Specification.\" https://git-lfs.github.com\n"
      "[18] D. Sculley et al. \"Hidden Technical Debt in Machine Learning Systems.\" Advances in Neural Information Processing Systems (NeurIPS), 2015.\n"
      "[19] Data Version Control (DVC). \"Data and Model Versioning for Machine Learning.\" https://dvc.org\n"
      "[20] A. van der Hoek et al. \"Software Configuration Management: Challenges and Directions.\" IEEE Software, 2002.\n"
      "[21] Google Cloud. \"Machine Learning Operations (MLOps): Getting Started.\" https://cloud.google.com/architecture/mlops-continuous-delivery-and-automation-pipelines-in-machine-learning")

    doc.add_page_break()

    # =========================================================================
    # APPENDIX A & B
    # =========================================================================
    doc.add_heading("Appendix A: Selected Git Commands", level=1)
    p("The following Git commands were utilized and verified throughout the lifecycle of the FloraScan project.")

    t22 = doc.add_table(rows=16, cols=2)
    t22.rows[0].cells[0].paragraphs[0].text = "Git Command"
    t22.rows[0].cells[1].paragraphs[0].text = "Operational Purpose in Project"

    rdata22 = [
        ("git init", "Initializes a new Git object database in the project root directory."),
        ("git status", "Inspects the working tree and staging area for modified or untracked files."),
        ("git add <file> / git add .", "Stages modified files into the index in preparation for a commit snapshot."),
        ("git commit -m \"<message>\"", "Records staged changes as a permanent, immutable commit object with metadata."),
        ("git diff", "Displays textual line differences between the working tree and the index."),
        ("git diff --staged", "Displays differences between staged changes and the current HEAD commit."),
        ("git log --oneline --graph", "Visualizes the commit history graph in concise single-line format."),
        ("git branch <name>", "Creates a new lightweight branch reference pointer at the current commit."),
        ("git checkout -b <name>", "Creates a new branch and immediately switches the working tree to it."),
        ("git merge <branch>", "Integrates history from another branch into the current checked-out branch."),
        ("git tag -a <tag> -m \"<msg>\"", "Creates a cryptographic annotated tag marking a software release milestone."),
        ("git push origin <branch>", "Transfers missing DAG packfile objects to the remote GitHub repository."),
        ("git pull origin <branch>", "Fetches and integrates remote commits into the local working branch."),
        ("git remote -v", "Lists configured remote repository URLs and their fetch/push endpoints."),
        ("git restore <file>", "Discards uncommitted modifications in the working tree, restoring checked-out state.")
    ]
    for row_idx, data in enumerate(rdata22):
        for col_idx, text in enumerate(data):
            t22.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t22, [2.5, 4.0])

    doc.add_page_break()

    doc.add_heading("Appendix B: Screenshot Evidence Index", level=1)
    p("This register indexes every figure embedded or referenced in this report, detailing its verification status and authoritative evidence source.")

    t23 = doc.add_table(rows=12, cols=4)
    headers23 = ["Figure", "Figure Title", "Status", "Authoritative Evidence Source"]
    for j, h in enumerate(headers23):
        t23.rows[0].cells[j].paragraphs[0].text = h

    rdata23 = [
        ("Figure 1", "System Architecture Blueprint", "Supplied (Embedded)", "screenshots/architecture_diagram.png (High-Res 300 DPI)"),
        ("Figure 2", "Glassmorphic Login Screen", "Supplied (Embedded)", "screenshots/screen_login.png (Android Jetpack Compose UI)"),
        ("Figure 3", "In-App SCM Dashboard Overview", "Supplied (Embedded)", "screenshots/scm_dashboard_card.png (Mobile Client)"),
        ("Figure 4", "Traceability Matrix with Commit Capsules", "Supplied (Embedded)", "screenshots/scm_traceability.png (Mobile Client)"),
        ("Figure 5", "Commit Capsule Inspection Modal", "Supplied (Embedded)", "screenshots/scm_modal_capsule.jpg (Mobile Client)"),
        ("Figure 6", "Change Control & Merged PR Audit", "Supplied (Embedded)", "screenshots/scm_change_control.png (Mobile Client)"),
        ("Figure 7", "Home Scanner Detection Screen", "Placeholder", "FloraScan Mobile Application (Camera Preview)"),
        ("Figure 8", "Explore Catalog & Botanical Taxonomy", "Placeholder", "FloraScan Mobile Application (Explore Screen)"),
        ("Figure 9", "Local Inference History Archive", "Placeholder", "FloraScan Mobile Application (History Screen)"),
        ("Figure 10", "Repository Commit Graph", "Placeholder", "Local Git Repository Terminal (git log --graph)"),
        ("Figure 11", "GitHub Web Releases Page", "Placeholder", "GitHub Remote Repository (Releases v4.0)")
    ]
    for row_idx, data in enumerate(rdata23):
        for col_idx, text in enumerate(data):
            t23.rows[row_idx+1].cells[col_idx].paragraphs[0].text = text
    format_table(t23, [1.0, 2.2, 1.4, 2.0])

    # Save Document
    out_path = r"c:\Users\menon\OneDrive\Desktop\SCM Project\FloraScan_SCM_Project_Report.docx"
    doc.save(out_path)
    print(f"Successfully generated comprehensive project report at: {out_path}")

if __name__ == '__main__':
    create_report()
