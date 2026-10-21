# -*- coding: utf-8 -*-
"""
generate_scm_report.py
Generates the comprehensive university-level SCM project report in Word (.docx) format
for the Flower Detection System.
"""

import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

def create_report():
    doc = docx.Document()

    # Page Margins (1 inch)
    for section in doc.sections:
        section.top_margin = Inches(1.0)
        section.bottom_margin = Inches(1.0)
        section.left_margin = Inches(1.0)
        section.right_margin = Inches(1.0)

    # Palette
    COLOR_PRIMARY = RGBColor(0x1F, 0x49, 0x7D)     # Deep Navy
    COLOR_SECONDARY = RGBColor(0x59, 0x59, 0x59)   # Slate Grey
    COLOR_BODY = RGBColor(0x26, 0x26, 0x26)        # Charcoal / Off-black
    HEX_HEADER_BG = "1F497D"
    HEX_ALT_ROW = "F2F5F9"
    HEX_BORDER = "D3D3D3"

    # Style Helpers
    def apply_run_font(run, name="Calibri", size_pt=11, color=COLOR_BODY, bold=False, italic=False):
        run.font.name = name
        run.font.size = Pt(size_pt)
        run.font.color.rgb = color
        run.bold = bold
        run.italic = italic

    def add_p(doc, text="", style='Normal', space_before=0, space_after=6, line_spacing=1.15, align=WD_ALIGN_PARAGRAPH.LEFT):
        p = doc.add_paragraph(text, style=style)
        p.alignment = align
        p.paragraph_format.space_before = Pt(space_before)
        p.paragraph_format.space_after = Pt(space_after)
        p.paragraph_format.line_spacing = line_spacing
        return p

    def add_h1(doc, text):
        p = add_p(doc, space_before=16, space_after=6)
        r = p.add_run(text)
        apply_run_font(r, name="Calibri", size_pt=18, color=COLOR_PRIMARY, bold=True)
        return p

    def add_h2(doc, text):
        p = add_p(doc, space_before=12, space_after=4)
        r = p.add_run(text)
        apply_run_font(r, name="Calibri", size_pt=14, color=COLOR_PRIMARY, bold=True)
        return p

    def add_h3(doc, text):
        p = add_p(doc, space_before=8, space_after=2)
        r = p.add_run(text)
        apply_run_font(r, name="Calibri", size_pt=12, color=COLOR_SECONDARY, bold=True)
        return p

    def add_body(doc, text, bold_prefix=None, space_after=6):
        p = add_p(doc, space_before=0, space_after=space_after, line_spacing=1.15)
        if bold_prefix:
            r_pre = p.add_run(bold_prefix)
            apply_run_font(r_pre, name="Calibri", size_pt=11, color=COLOR_BODY, bold=True)
        r_text = p.add_run(text)
        apply_run_font(r_text, name="Calibri", size_pt=11, color=COLOR_BODY)
        return p

    def add_bullet(doc, text, bold_prefix=None):
        p = doc.add_paragraph(style='List Bullet')
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(3)
        p.paragraph_format.line_spacing = 1.15
        if bold_prefix:
            r_pre = p.add_run(bold_prefix)
            apply_run_font(r_pre, name="Calibri", size_pt=11, color=COLOR_BODY, bold=True)
        r_text = p.add_run(text)
        apply_run_font(r_text, name="Calibri", size_pt=11, color=COLOR_BODY)
        return p

    def add_caption(doc, text):
        p = add_p(doc, align=WD_ALIGN_PARAGRAPH.CENTER, space_before=4, space_after=12)
        r = p.add_run(text)
        apply_run_font(r, name="Calibri", size_pt=10, color=COLOR_SECONDARY, italic=True)
        return p

    def set_cell_shading(cell, color_hex):
        tcPr = cell._tc.get_or_add_tcPr()
        shd = OxmlElement('w:shd')
        shd.set(qn('w:val'), 'clear')
        shd.set(qn('w:color'), 'auto')
        shd.set(qn('w:fill'), color_hex)
        tcPr.append(shd)

    def format_table(table, col_widths, headers, data):
        table.alignment = WD_TABLE_ALIGNMENT.CENTER
        # Format Header
        hdr_row = table.rows[0]
        trPr = hdr_row._tr.get_or_add_trPr()
        trPr.append(OxmlElement('w:tblHeader'))

        for i, title in enumerate(headers):
            cell = hdr_row.cells[i]
            cell.width = col_widths[i]
            set_cell_shading(cell, HEX_HEADER_BG)
            p = cell.paragraphs[0]
            p.alignment = WD_ALIGN_PARAGRAPH.LEFT
            p.paragraph_format.space_before = Pt(4)
            p.paragraph_format.space_after = Pt(4)
            r = p.add_run(title)
            apply_run_font(r, name="Calibri", size_pt=10, color=RGBColor(255, 255, 255), bold=True)

        # Format Data Rows
        for r_idx, row_data in enumerate(data):
            row = table.rows[r_idx + 1]
            bg_color = HEX_ALT_ROW if (r_idx % 2 == 1) else "FFFFFF"
            for c_idx, val in enumerate(row_data):
                cell = row.cells[c_idx]
                cell.width = col_widths[c_idx]
                if bg_color != "FFFFFF":
                    set_cell_shading(cell, bg_color)
                p = cell.paragraphs[0]
                p.alignment = WD_ALIGN_PARAGRAPH.LEFT
                p.paragraph_format.space_before = Pt(3)
                p.paragraph_format.space_after = Pt(3)
                p.paragraph_format.line_spacing = 1.15
                r = p.add_run(str(val))
                apply_run_font(r, name="Calibri", size_pt=9.5, color=COLOR_BODY)

    def add_xml_field(p, field_str):
        run = p.add_run()
        fld1 = OxmlElement('w:fldChar')
        fld1.set(qn('w:fldCharType'), 'begin')
        instr = OxmlElement('w:instrText')
        instr.set(qn('xml:space'), 'preserve')
        instr.text = field_str
        fld2 = OxmlElement('w:fldChar')
        fld2.set(qn('w:fldCharType'), 'separate')
        fld3 = OxmlElement('w:fldChar')
        fld3.set(qn('w:fldCharType'), 'end')
        run._r.append(fld1)
        run._r.append(instr)
        run._r.append(fld2)
        run._r.append(fld3)

    # -------------------------------------------------------------
    # COVER PAGE
    # -------------------------------------------------------------
    p_pre = add_p(doc, space_before=40, space_after=10, align=WD_ALIGN_PARAGRAPH.CENTER)
    r_uni = p_pre.add_run("VELLORE INSTITUTE OF TECHNOLOGY\nDEPARTMENT OF SOFTWARE AND SYSTEMS ENGINEERING")
    apply_run_font(r_uni, name="Calibri", size_pt=14, color=COLOR_SECONDARY, bold=True)

    p_title = add_p(doc, space_before=60, space_after=15, align=WD_ALIGN_PARAGRAPH.CENTER)
    r_main_title = p_title.add_run("FLOWER DETECTION SYSTEM WITH INTEGRATED RUNTIME SCM GOVERNANCE\n")
    apply_run_font(r_main_title, name="Calibri", size_pt=24, color=COLOR_PRIMARY, bold=True)
    r_sub = p_title.add_run("An Edge-Inference Android Platform Powered by EfficientNet-B0 and an In-App Software Configuration Management Audit Engine")
    apply_run_font(r_sub, name="Calibri", size_pt=13, color=COLOR_SECONDARY, italic=True)

    p_meta = add_p(doc, space_before=120, space_after=8, align=WD_ALIGN_PARAGRAPH.CENTER)
    r_auth = p_meta.add_run("Candidate Name: Rishit\nRegistration Number: 24MIS0185\nCourse: Software Configuration Management\nFaculty Evaluator: [TO BE FILLED BY AUTHOR: Faculty Name & Designation]")
    apply_run_font(r_auth, name="Calibri", size_pt=12, color=COLOR_BODY, bold=True)

    p_date = add_p(doc, space_before=40, space_after=0, align=WD_ALIGN_PARAGRAPH.CENTER)
    r_date = p_date.add_run("October 2026")
    apply_run_font(r_date, name="Calibri", size_pt=11, color=COLOR_SECONDARY)

    doc.add_page_break()

    # Setup Header and Footer for Document Body
    body_section = doc.sections[0]
    body_footer = body_section.footer
    p_ft = body_footer.paragraphs[0]
    p_ft.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    r_ft_lbl = p_ft.add_run("Flower Detection System | Page ")
    apply_run_font(r_ft_lbl, name="Calibri", size_pt=9, color=COLOR_SECONDARY)
    add_xml_field(p_ft, "PAGE")
    r_ft_of = p_ft.add_run(" of ")
    apply_run_font(r_ft_of, name="Calibri", size_pt=9, color=COLOR_SECONDARY)
    add_xml_field(p_ft, "NUMPAGES")

    # -------------------------------------------------------------
    # TABLE OF CONTENTS & LIST OF FIGURES
    # -------------------------------------------------------------
    add_h1(doc, "Table of Contents")
    p_toc = add_p(doc, space_before=6, space_after=12)
    add_xml_field(p_toc, "TOC")
    add_body(doc, "(Note: Press F9 or right-click to update table fields in Microsoft Word / LibreOffice).", space_after=16)

    add_h2(doc, "List of Figures")
    add_bullet(doc, "Figure 1: Machine Learning Inference and Classification Pipeline (Page 5)")
    add_bullet(doc, "Figure 2: Liquid Glassmorphic Authentication Screen (screen_login.png) (Page 7)")
    add_bullet(doc, "Figure 3: Runtime Traceability Verification Card (scm_traceability.png) (Page 8)")
    add_bullet(doc, "Figure 4: Change Control & Pull Request Governance Card (scm_change_control.png) (Page 9)")
    add_bullet(doc, "Figure 5: Floating Commit Detail Modal & Inspection Capsule (scm_modal_capsule.jpg) (Page 10)")

    add_h2(doc, "List of Tables")
    add_bullet(doc, "Table 1: Oxford 102 Flowers Dataset Partition and Image Counts (Page 4)")
    add_bullet(doc, "Table 2: Training Epoch Metrics and Validation Accuracies (Page 5)")
    add_bullet(doc, "Table 3: Comprehensive Software Configuration Management Practice Matrix (Page 12)")
    add_bullet(doc, "Table 4: Comparative Analysis of GitHub Native Metrics vs. FloraScan In-App SCM Engine (Page 15)")

    doc.add_page_break()

    # -------------------------------------------------------------
    # 1. INTRODUCTION
    # -------------------------------------------------------------
    add_h1(doc, "1. Introduction")
    add_body(doc, "The Flower Detection System (FloraScan) is a native Android mobile application designed to bridge computer vision edge intelligence with rigorous, self-auditing Software Configuration Management (SCM) principles. The primary functional utility of the platform is the rapid, offline identification of flower species directly on modern handheld devices. By eliminating the necessity of constant cloud connectivity, users such as field botanists, agricultural workers, horticultural students, gardeners, and nature enthusiasts can instantly photograph any blooming plant and receive botanical classification within milliseconds.")
    
    add_body(doc, "The application is developed entirely in Kotlin utilizing modern declarative Jetpack Compose and Google Material Design 3 guidelines. To establish an aesthetic, immersive user experience, the interface incorporates hardware-accelerated liquid glassmorphism using the Haze graphics engine. Underneath the visual presentation layer, the application executes a highly optimized on-device deep neural network (PyTorch Mobile Lite) trained specifically on the Oxford 102 Flowers benchmark dataset.")

    add_body(doc, "Crucially, FloraScan is engineered not merely as an isolated end-user product, but as a dual-purpose software configuration management demonstration platform. Embedded directly into the client's Profile navigation graph is an automated, live SCM Governance and Quality Dashboard. This system continuously inspects its own GitHub source code repository (RISHITMENON288/Flower-Detection-System) over public HTTPS REST endpoints. It audits version drift between compiled binaries and Git branches, verifies commit traceability back to structured issues or pull requests, evaluates release integrity against semantic versioning standards, and validates whether mainline changes arrived through governed code reviews or direct developer pushes. Consequently, the project exemplifies how edge-deployed machine learning systems can maintain complete developmental transparency and continuous traceability throughout their product lifecycle.")

    # -------------------------------------------------------------
    # 2. PROBLEM STATEMENT & OBJECTIVES
    # -------------------------------------------------------------
    add_h1(doc, "2. Problem Statement")
    add_body(doc, "Manual flower identification presents substantial hurdles across scientific and recreational domains. Plant taxa encompass tens of thousands of visual phenotypes where morphological variations between disparate species can be remarkably subtle, while intra-species phenotypic variance (due to soil composition, sunlight exposure, hydration, and seasonal morphs) remains broad. Classical manual taxonomy relies on dense, multi-volume dichotomous floral keys that require formal botanical expertise and extensive floral dissection, rendering real-time field identification impractical for students, forest rangers, and hobbyists.")

    add_body(doc, "Concurrently, existing software solutions suffer from severe architectural and operational shortcomings:")
    add_bullet(doc, "Most commercial plant identification services rely entirely on centralized server farms. In remote nature reserves, agricultural zones, or deep forest trails, mobile cellular connectivity is frequently degraded or completely absent, rendering cloud-dependent applications completely unusable.", bold_prefix="Cloud Dependency & Connectivity Failures: ")
    add_bullet(doc, "Standard mobile deployments frequently obscure the provenance of the underlying machine learning models. Application stores routinely distribute binaries where developers have zero traceability linking the bundled model weights to the exact training commit, dataset split, or validation run.", bold_prefix="Traceability Black Box in Edge AI: ")
    add_bullet(doc, "Software configuration management audits in academic and enterprise contexts are customarily executed retrospectively using manual spreadsheets or decoupled continuous integration web portals. Developers and evaluators lack immediate, ambient verification showing whether the binary running on their physical smartphone actually matches the declared repository version and adheres to branch governance conventions.", bold_prefix="Decoupled Governance Auditing: ")

    add_h2(doc, "Project Objectives")
    add_body(doc, "To resolve these practical and methodological challenges, this project targets five key technical objectives:")
    add_bullet(doc, "Train a lightweight, high-accuracy convolutional neural network on the Oxford 102 Flowers benchmark that balances classification performance against strict mobile memory footprints.", bold_prefix="1. Edge Model Optimization: ")
    add_bullet(doc, "Quantize and compile the model into PyTorch Mobile Lite (`.ptl`) format, executing inference entirely on-device via native C++ JNI bindings without external API latency or bandwidth costs.", bold_prefix="2. Zero-Latency Native Inference: ")
    add_bullet(doc, "Deliver a responsive, liquid glassmorphic user interface using Jetpack Compose, offering real-time camera ingestion, an offline botanical encyclopedia (Explore screen), and historical scan logs.", bold_prefix="3. Modern Material UI/UX: ")
    add_bullet(doc, "Build an embedded, runtime SCM Governance Engine that polls GitHub REST APIs to verify version drift, semantic release tagging, asset completeness, commit traceability, and pull request change control.", bold_prefix="4. In-App SCM Governance: ")
    add_bullet(doc, "Enforce strict developmental hygiene through automated unit test suites (`ScmChecksTest`, `ScmHistoryTest`), eliminating build drift and maintaining absolute baseline reproducibility.", bold_prefix="5. Automated Quality Assurance: ")

    # -------------------------------------------------------------
    # 3. MODEL USED & TRAINING PIPELINE
    # -------------------------------------------------------------
    add_h1(doc, "3. Model Used & Training Pipeline")
    add_body(doc, "The classification backbone selected for FloraScan is EfficientNet-B0, developed by Tan and Le (2019). EfficientNet models leverage a principled compound scaling method that uniformly scales network depth, width, and image resolution with a fixed compound coefficient $\\phi$, rather than arbitrarily scaling a single dimension. EfficientNet-B0 was selected over heavier alternatives (such as ResNet-50 or DenseNet-121) because it achieves superior top-1 accuracy on ImageNet (77.1%) while utilizing only 5.3 million parameters. This minimal parameter footprint translates directly into an installed asset size of approximately 16.5 MB, making it exceptionally well-suited for edge deployment on low-to-mid-range Android smartphones without thermal throttling or out-of-memory (OOM) crashes.")

    add_h2(doc, "Dataset Partition & Structure")
    add_body(doc, "The model was trained on the Oxford 102 Category Flower Dataset (Nilsback & Zisserman, 2008). The dataset consists of 102 distinct flower species commonly appearing across the United Kingdom. Per the standard scientific benchmark protocol implemented by `torchvision.datasets.Flowers102`, the images are partitioned into pre-defined train, validation, and test splits:")

    # Table 1: Dataset Partition
    t1 = doc.add_table(rows=4, cols=3)
    format_table(
        t1,
        [Inches(2.0), Inches(1.8), Inches(2.7)],
        ["Dataset Split", "Image Count", "Role in Pipeline"],
        [
            ["Train Split", "1,020 images", "Gradient optimization (10 images per class)"],
            ["Validation Split", "1,020 images", "Hyperparameter tuning and checkpoint selection"],
            ["Test Split", "6,149 images", "Final unbiased generalization assessment"]
        ]
    )
    add_caption(doc, "Table 1: Oxford 102 Flowers Dataset Partition and Image Counts")

    add_h2(doc, "Training Setup & Hyperparameters")
    add_body(doc, "The training workflow is documented inside `train.ipynb`. Training was conducted utilizing PyTorch with CUDA acceleration. The pretrained EfficientNet-B0 weights (`IMAGENET1K_V1`) were imported, and the original 1000-class linear classification head was replaced with a custom linear projection: `nn.Linear(in_features=1280, out_features=102)`. Training was executed across two distinct phases to prevent catastrophic forgetting:")

    add_body(doc, "In this stage, all convolutional feature extraction blocks (`model.features`) were frozen (`param.requires_grad = False`). Only the newly initialized classification head was trained for 5 epochs using the Adam optimizer with a learning rate of $\\eta = 0.001$ and Cross-Entropy loss. This rapidly adjusted the final weights to floral feature representations.", bold_prefix="Phase 1 — Feature Extractor Warmup (5 Epochs): ")

    add_body(doc, "All convolutional layers across the network were unfrozen (`param.requires_grad = True`). The model was fine-tuned end-to-end for 10 epochs using Adam with a reduced learning rate of $\\eta = 0.0001$. Model checkpoints were evaluated at the end of each epoch against the validation split, and only weights yielding an improvement in validation accuracy were saved to disk as `flower_model.pth`.", bold_prefix="Phase 2 — End-to-End Fine-Tuning (10 Epochs): ")

    # Table 2: Training Epoch Metrics
    t2 = doc.add_table(rows=16, cols=5)
    format_table(
        t2,
        [Inches(1.1), Inches(1.2), Inches(1.3), Inches(1.3), Inches(1.6)],
        ["Phase", "Epoch", "Cross-Entropy Loss", "Train Accuracy (%)", "Validation Accuracy (%)"],
        [
            ["Warmup", "Epoch 1/5", "4.366", "12.1%", "53.1%"],
            ["Warmup", "Epoch 2/5", "3.238", "60.9%", "72.6%"],
            ["Warmup", "Epoch 3/5", "2.481", "74.7%", "77.7%"],
            ["Warmup", "Epoch 4/5", "1.933", "80.8%", "81.5%"],
            ["Warmup", "Epoch 5/5", "1.611", "84.9%", "82.7%"],
            ["Fine-Tune", "Epoch 1/10", "1.146", "87.4%", "87.0% (Checkpoint)"],
            ["Fine-Tune", "Epoch 2/10", "0.830", "88.8%", "88.3% (Checkpoint)"],
            ["Fine-Tune", "Epoch 3/10", "0.653", "90.4%", "90.0% (Checkpoint)"],
            ["Fine-Tune", "Epoch 4/10", "0.533", "92.9%", "91.5% (Checkpoint)"],
            ["Fine-Tune", "Epoch 5/10", "0.457", "94.3%", "91.6% (Checkpoint)"],
            ["Fine-Tune", "Epoch 6/10", "0.389", "94.7%", "92.2% (Checkpoint)"],
            ["Fine-Tune", "Epoch 7/10", "0.335", "96.3%", "91.8%"],
            ["Fine-Tune", "Epoch 8/10", "0.289", "96.5%", "92.3% (Checkpoint)"],
            ["Fine-Tune", "Epoch 9/10", "0.289", "95.9%", "93.1% (Best Checkpoint)"],
            ["Fine-Tune", "Epoch 10/10", "0.261", "95.9%", "92.7%"]
        ]
    )
    add_caption(doc, "Table 2: Training Epoch Metrics and Validation Accuracies from train.ipynb")

    add_body(doc, "As confirmed in the execution logs of `train.ipynb`, the fine-tuning phase converged to a peak validation accuracy of 93.1% at Epoch 9 with a training cross-entropy loss of 0.289.")

    add_h2(doc, "Model Export & Edge Execution")
    add_body(doc, "To transition the PyTorch model (`flower_model.pth`, 16.8 MB) to mobile execution, the weights were exported using PyTorch's mobile compilation pipeline into a TorchScript Lite bytecode module (`flower_model.ptl`, 17.3 MB). This file was placed in the Android source repository under `app/src/main/assets/flower_model.ptl`.")

    add_body(doc, "Within the Android codebase, the `FlowerClassifier` class (located in `com.example.myapplication.ml`) initializes the module using `LiteModuleLoader.load(assetFilePath(context, \"flower_model.ptl\"))`. When a user provides a photograph via CameraX preview or the Android photo picker, the image undergoes rigorous preprocessing in Kotlin to replicate `train.ipynb`'s validation transforms:")
    add_bullet(doc, "The incoming Android `Bitmap` is resized to 256 pixels along its shortest dimension while maintaining native aspect ratio.", bold_prefix="1. Aspect-Aware Resize: ")
    add_bullet(doc, "A 224x224 pixel crop is extracted from the geometric center of the resized bitmap.", bold_prefix="2. Center Crop: ")
    add_bullet(doc, "Pixel RGB channels [0, 255] are transformed into float tensors and normalized using ImageNet distribution statistics: $\\mu = [0.485, 0.456, 0.406]$ and $\\sigma = [0.229, 0.224, 0.225]$.", bold_prefix="3. ImageNet Normalization: ")
    add_bullet(doc, "Native execution occurs within a thread-synchronized lock (`inferenceLock`) to guard C++ JNI pointers. The resulting 102-dimensional logit vector is passed through a CPU Softmax function, sorting the predictions to extract the top-5 candidate species with exact percentage confidences.", bold_prefix="4. Native Inference & Softmax: ")

    # Figure 1 Diagram
    p_fig1_diag = add_p(doc, align=WD_ALIGN_PARAGRAPH.CENTER, space_before=12, space_after=6)
    r_diag = p_fig1_diag.add_run("[User Photo / Camera]  -->  [Resize 256 & CenterCrop 224]  -->  [Normalize ImageNet Stats]\n                             |\n                             v\n[Top-5 Predictions Card] <-- [Softmax Layer] <-- [EfficientNet-B0 (flower_model.ptl)]")
    apply_run_font(r_diag, name="Courier New", size_pt=9.5, color=COLOR_PRIMARY, bold=True)
    add_caption(doc, "Figure 1: Machine Learning Inference and Classification Pipeline")

    # -------------------------------------------------------------
    # 4. SYSTEM OVERVIEW & APP SCREENS
    # -------------------------------------------------------------
    add_h1(doc, "4. System Overview & Application Screens")
    add_body(doc, "FloraScan's mobile architecture is structured around four primary navigation destinations coordinated by Jetpack Compose Navigation (`NavHost`):")
    add_bullet(doc, "The core classification console featuring live CameraX capture preview, photo gallery picker, instant model inference trigger, and a dynamic results card presenting the predicted flower name, botanical taxonomy, and top-5 confidence bars.", bold_prefix="1. Home Screen: ")
    add_bullet(doc, "A searchable botanical encyclopedia cataloging all 102 recognizable species from the Oxford dataset, providing reference imagery, native blooming habitats, sunlight, and hydration guidelines.", bold_prefix="2. Explore Screen: ")
    add_bullet(doc, "A historical temporal timeline logging prior classifications, recorded confidences, and saved timestamps stored on-device.", bold_prefix="3. History Screen: ")
    add_bullet(doc, "User profile management and Google Authentication settings, combined with the embedded real-time Software Configuration Management (SCM) audit dashboard.", bold_prefix="4. Profile & SCM Dashboard: ")

    add_body(doc, "The following screenshots, captured from the active project workspace, illustrate the visual styling and live SCM governance features of the application:")

    # Screenshots
    def insert_screenshot(filename, caption_text, desc_text, is_portrait=False):
        path = os.path.join("screenshots", filename)
        if os.path.exists(path):
            p_img = add_p(doc, align=WD_ALIGN_PARAGRAPH.CENTER, space_before=12, space_after=4)
            img_width = Inches(3.2) if is_portrait else Inches(4.8)
            p_img.add_run().add_picture(path, width=img_width)
            add_caption(doc, caption_text)
            add_body(doc, desc_text, space_after=12)
        else:
            add_body(doc, f"[TO BE FILLED BY AUTHOR: Screenshot {filename} missing in screenshots/ directory]")

    insert_screenshot(
        "screen_login.png",
        "Figure 2: Liquid Glassmorphic Authentication Screen (screen_login.png)",
        "Figure 2 displays the application entrance interface designed using dynamic liquid glassmorphism. It features high-resolution moving floral backdrops overlaid with real-time frosted glass cards (powered by Chris Banes' Haze library) and integrates one-tap Google Identity Services via Firebase Authentication.",
        is_portrait=True
    )

    insert_screenshot(
        "scm_traceability.png",
        "Figure 3: Runtime Traceability Verification Card (scm_traceability.png)",
        "Figure 3 demonstrates the live Traceability Card within the SCM Dashboard. The card queries the GitHub repository over HTTPS, evaluates whether recent commits reference valid issue or PR identifiers (#number), and groups commits into interactive Traceable Changes and Untraced Changes glass capsules.",
        is_portrait=False
    )

    insert_screenshot(
        "scm_change_control.png",
        "Figure 4: Change Control & Pull Request Governance Card (scm_change_control.png)",
        "Figure 4 illustrates the Change Control governance card. It analyzes the commit directed acyclic graph (DAG) to determine whether changes reached the mainline branch through formal pull request code reviews or direct developer pushes, presenting metrics on merged PR counts and percentage compliance.",
        is_portrait=False
    )

    insert_screenshot(
        "scm_modal_capsule.jpg",
        "Figure 5: Floating Commit Detail Modal & Inspection Capsule (scm_modal_capsule.jpg)",
        "Figure 5 shows the floating inspection modal summoned when tapping a commit capsule. The modal renders complete commit information—including 7-character short SHAs, author names, commit subjects, and extended messages—with unconstrained, bounded vertical scrolling.",
        is_portrait=True
    )

    # -------------------------------------------------------------
    # 5. SCM TOOL USED & WORKFLOW
    # -------------------------------------------------------------
    add_h1(doc, "5. SCM Tool Used")
    add_body(doc, "The primary software configuration management tool utilized throughout the project lifecycle is Git for distributed version control, hosted on GitHub (Repository: `RISHITMENON288/Flower-Detection-System`). GitHub serves as the central remote repository, issue tracking system, and release distribution portal.")

    add_h2(doc, "Repository Organization & Directory Structure")
    add_body(doc, "The repository is partitioned into modular subsystems to maintain strict separation of concerns:")
    add_bullet(doc, "Houses the complete native Android codebase. Subdirectories follow standard Android Gradle conventions: `app/src/main/java/com/example/myapplication/` contains UI screens (dashboard, explore, history, home, profile), ML bindings (`FlowerClassifier.kt`), and GitHub API data layers (`GitHubApi.kt`, `ScmHistory.kt`, `ScmChecks.kt`).", bold_prefix="`app/`: ")
    add_bullet(doc, "Stores bundled application assets loaded at runtime via Android AssetManager, specifically the compiled PyTorch Mobile model `flower_model.ptl`.", bold_prefix="`app/src/main/assets/`: ")
    add_bullet(doc, "Contains comprehensive JVM unit test suites (`ScmChecksTest.kt`, `ScmHistoryTest.kt`) that validate semantic versioning, release audits, regex pattern matching, and commit DAG traversal.", bold_prefix="`app/src/test/`: ")
    add_bullet(doc, "Holds training notebooks (`train.ipynb`), full training checkpoints (`flower_model.pth`), dataset caches, design assets, and academic review documentation. This folder is deliberately excluded from git commits via `.gitignore` to prevent repository bloat.", bold_prefix="`project-resources/`: ")
    add_bullet(doc, "Modern Gradle Version Catalog (`libs.versions.toml`) centralizing dependency coordinates and versions for Jetpack Compose, PyTorch Lite, Firebase, and Kotlin compiler plugins.", bold_prefix="`gradle/`: ")

    add_h2(doc, "Development & Git Workflow")
    add_body(doc, "Development is structured around a trunk-based branching model with governed pull request integration. Feature enhancements and architectural refactors (such as the SCM dashboard, modal height clipping fixes, and traceability capsules) are developed on dedicated task branches or merged into `master` via pull requests. Pull requests carry semantic squash titles (e.g., `(#1)`, `(#7)`) that link code commits directly to documented changes, enabling automated tracking by both GitHub and the in-app SCM auditor.")

    # -------------------------------------------------------------
    # 6. SCM PRACTICES TARGETED
    # -------------------------------------------------------------
    add_h1(doc, "6. SCM Practices Targeted")
    add_body(doc, "Software Configuration Management encompasses the disciplines, processes, and tools required to establish and maintain integrity across software products throughout their evolution. Table 3 details the SCM practices implemented in FloraScan, documenting their formal definition, project evidence, and completion status.")

    # Table 3: SCM Practice Matrix
    t3 = doc.add_table(rows=12, cols=4)
    format_table(
        t3,
        [Inches(1.5), Inches(1.8), Inches(2.5), Inches(0.8)],
        ["SCM Practice", "Formal Definition", "Application & Concrete Evidence in Project", "Status"],
        [
            [
                "Version Control & Commit Conventions",
                "Tracking code history with descriptive messages detailing why and what changed.",
                "Git history with 8 structured commits; messages use imperative style with issue/PR references (e.g. commit 331932b: 'Add traceable and untraced commit capsules to Traceability card (#7)').",
                "Done"
            ],
            [
                "Branching Strategy",
                "Isolating parallel development lines to maintain an always-deployable main branch.",
                "Trunk-based development centered on master branch; features developed on branch lines and merged via Pull Requests.",
                "Done"
            ],
            [
                "Pull Requests & Code Review",
                "Formal mechanism for peer inspection and review before merging code into mainline.",
                "Commits reflect PR merges (#1 through #7); in-app Change Control analyzer tracks merged PRs and review percentages.",
                "Done"
            ],
            [
                "Issue & Change Tracking",
                "Tying every code alteration back to an identifiable requirement or defect ticket.",
                "Traceability regex engine ((?<![\\w&])#\\d+\\b) parses commit subjects; verified in ScmHistoryTest.kt and displayed in in-app Traceability card.",
                "Done"
            ],
            [
                "Baselines & Semantic Release Tagging",
                "Marking stable, immutable release points using standard semantic version syntax.",
                "Release tag '4' created on commit fc9082f; audited by ReleaseAudits in ScmChecks.kt for version consistency and release notes.",
                "Done"
            ],
            [
                "Build & CI Automation",
                "Automated scripting of compilation, testing, and artifact packaging.",
                "Gradle Kotlin DSL build automation (./gradlew assembleDebug, ./gradlew testDebugUnitTest); [TO BE FILLED BY AUTHOR: GitHub Actions CI workflow .github/workflows/ci.yml].",
                "Partly"
            ],
            [
                "Handling Large Model Files",
                "Managing heavy binary weights outside core git VCS history to avoid repository bloat.",
                "Heavy 600MB dataset and flower_model.pth stored in /project-resources/ (ignored in .gitignore); compiled flower_model.ptl (17MB) bundled as release asset.",
                "Done"
            ],
            [
                "Reproducible Environments",
                "Documenting exact runtime dependencies, versions, and random seeds.",
                "libs.versions.toml version catalog for Android dependencies; PyTorch transforms with explicit mean/std vectors in train.ipynb and FlowerClassifier.kt.",
                "Done"
            ],
            [
                "Configuration & Secrets Management",
                "Isolating environment configurations, keys, and credentials from version control.",
                ".gitignore excludes local.properties, *.iml, .gradle, and credentials; Firebase Google ID uses runtime Android Credential Manager.",
                "Done"
            ],
            [
                "Traceability (Code, Model & App)",
                "Establishing an unbroken provenance chain linking dataset, model binary, and app build.",
                "VersionCheckCard dynamically compares installed PackageInfo (v4.0, code 4) against source build.gradle.kts and latest GitHub release tag.",
                "Done"
            ],
            [
                "Backup & Disaster Recovery",
                "Maintaining secure, redundant, off-site replicas of project code and assets.",
                "Decentralized Git architecture replicated to remote GitHub repository; release binaries published as attached assets on GitHub releases.",
                "Done"
            ]
        ]
    )
    add_caption(doc, "Table 3: Comprehensive Software Configuration Management Practice Matrix")

    # -------------------------------------------------------------
    # 7. WHAT GITHUB DASHBOARD DOES NOT SHOW
    # -------------------------------------------------------------
    add_h1(doc, "7. What the GitHub Dashboard Does Not Show")
    add_body(doc, "GitHub is recognized as the global standard for source code hosting, version control, and team collaboration. Its native web portal includes diverse analytical dashboards such as Insights, Pulse, Network Graphs, Contributors, and GitHub Actions. However, these tools were fundamentally architected for generic text-based software engineering rather than Machine Learning (ML) edge applications or comprehensive SCM quality governance.")

    add_body(doc, "A critical academic examination of GitHub's native analytics reveals several key visibility gaps for edge AI projects:")
    add_bullet(doc, "GitHub tracks lines of code added or deleted in text files, but treats compiled machine learning models (`.ptl`, `.onnx`, `.tflite`) as opaque binary blobs. It provides zero visibility into model architecture, training hyperparameters, training loss trajectories, or validation accuracy benchmarks achieved by the model corresponding to a particular commit.", bold_prefix="1. Absence of Machine Learning Provenance: ")
    add_bullet(doc, "While GitHub records commits on branches, it does not evaluate whether commits adhere to traceability conventions (e.g., referencing a valid issue ticket `#number`). A team must inspect each commit manually or build external bot scripts to discover how many commits represent untraced, unverified modifications.", bold_prefix="2. Lack of Commit Traceability Auditing: ")
    add_bullet(doc, "GitHub allows repository administrators to enable branch protection rules, but it provides no ambient dashboard showing the percentage of mainline commits that actually bypassed peer review via direct developer pushes vs. those merged through verified pull requests.", bold_prefix="3. No Runtime Change Control Assessment: ")
    add_bullet(doc, "GitHub has no mechanism to compare the software currently installed on an end-user's physical device against the checked-in source code or the latest release tag. If a user runs a stale or drifting build, GitHub cannot identify the discrepancy.", bold_prefix="4. Inability to Detect Mobile Version Drift: ")
    add_bullet(doc, "GitHub releases display tags and uploaded assets, but do not automatically audit whether release tags follow uniform semantic versioning styles (e.g. `v4.0` vs `4`), whether release notes were authored, or whether mandatory installation files (`.apk`) are attached.", bold_prefix="5. Superficial Release Quality Audits: ")

    # -------------------------------------------------------------
    # 8. WHAT THIS PROJECT SHOWS EXTRA
    # -------------------------------------------------------------
    add_h1(doc, "8. What This Project Shows Extra")
    add_body(doc, "To resolve the analytical limitations of standard hosting platforms, FloraScan incorporates custom in-app SCM governance tooling that operates directly within the mobile application. Built using Kotlin coroutines and standard Android networking, this dashboard polls GitHub REST endpoints and parses repository artifacts in real-time. It delivers five custom analytical cards that have no direct native equivalent in GitHub:")

    add_h2(doc, "1. Live Version Drift Detector (`VersionCheckCard`)")
    add_body(doc, "The Version Drift module queries the local Android `PackageManager` for the installed `versionName` (\"4.0\") and `versionCode` (4). Simultaneously, it fetches `app/build.gradle.kts` directly from GitHub Raw CDN and parses the `versionName` string using regular expressions. It also queries the GitHub Releases API (`/releases/latest`) to retrieve the newest tag. The card evaluates two automated audit rules: (1) Does the installed phone binary match the source code? and (2) Does the source code match the published release? Any mismatch triggers an immediate visual warning.")

    add_h2(doc, "2. Commit Traceability Engine (`TraceabilityCard`)")
    add_body(doc, "Located in `ScmHistory.kt`, the Traceability analyzer inspects recent commits on the default branch using a negative lookbehind regex (`(?<![\\w&])#\\d+\\b`). It verifies whether each commit references an issue or pull request (e.g., `Fixes #3`, `(#7)`), while ignoring false matches like HTML entities (`&#39;`) or hex colors (`#fff`). The module generates a mathematical compliance percentage and separates commits into two interactive glass capsules: Traceable Changes and Untraced Changes, allowing reviewers to instantly audit untracked code.")

    add_h2(doc, "3. Branch Change Control & Mainline Walker (`ChangeControlCard`)")
    add_body(doc, "To determine whether changes reached production through governed reviews, the Change Control module performs a topological walk along the git commit directed acyclic graph. It follows the first parent of each commit from newest to oldest to establish the 'main line'. It then cross-references commit SHAs against merged Pull Request records (`/repos/{owner}/{repo}/pulls`). Commits exhibiting multiple parents (`parents.size > 1`), matching PR merge SHAs, or carrying squash suffixes (`(#PR)`) are classified as reviewed. All other commits are flagged as direct pushes. The results are presented in interactive capsules with visual health badges.")

    add_h2(doc, "4. Release Standard & Asset Auditor (`ReleaseAuditCard`)")
    add_body(doc, "The Release Audit module evaluates every published release tag against project quality conventions. It verifies whether the tag is a valid dotted number (via `Versions.parse()`), checks whether tag prefix styling matches the latest release (e.g., uniform `v` prefix usage), confirms whether an installable APK binary is attached to the release assets, and checks whether release notes were written.")

    add_h2(doc, "5. Dependency Inventory Inspector (`DependenciesCard`)")
    add_body(doc, "The Dependency Inventory module scrapes `gradle/libs.versions.toml` from the repository at runtime, parses its TOML sections, and resolves library aliases to their exact maven coordinates and version strings. This provides instant visibility into third-party libraries bundled inside the application.")

    # Table 4: Comparison Table
    t4 = doc.add_table(rows=7, cols=3)
    format_table(
        t4,
        [Inches(2.0), Inches(2.0), Inches(2.5)],
        ["Information Needed", "GitHub Native Capability", "FloraScan In-App SCM Engine"],
        [
            [
                "Installed App vs. Repository Version Drift",
                "None. GitHub has no awareness of installed mobile client state.",
                "Real-time comparison: Installed APK vs. build.gradle.kts vs. latest GitHub release tag."
            ],
            [
                "Commit Traceability to Issues / PRs",
                "Manual. Requires clicking each individual commit.",
                "Automated regex engine calculates compliance percentage; separates Traceable vs. Untraced into glass capsules."
            ],
            [
                "Change Control & Code Review Compliance",
                "Partial. Shows PR list, but no percentage breakdown of reviewed commits vs. direct pushes.",
                "Mainline graph walker classifies every commit; shows reviewed percentage and direct push capsules."
            ],
            [
                "Release Integrity & Asset Audit",
                "Static listing of release assets and markdown notes.",
                "Automated 4-point rule check: numeric tag, consistent v-prefix, APK attached, and release notes authored."
            ],
            [
                "Live Dependency Version Inventory",
                "Dependency Graph (web portal only, requires repository permissions).",
                "Parses libs.versions.toml at runtime over HTTPS; displays all libraries and versions directly in-app."
            ],
            [
                "Accessibility for Non-Technical Evaluators",
                "Requires GitHub account, repository access, and navigating multiple complex web pages.",
                "Zero authentication required; evaluators inspect full SCM health directly on the smartphone in 1 tap."
            ]
        ]
    )
    add_caption(doc, "Table 4: Comparative Analysis of GitHub Native Metrics vs. FloraScan In-App SCM Engine")

    # -------------------------------------------------------------
    # 9. CONCLUSION & FUTURE WORK
    # -------------------------------------------------------------
    add_h1(doc, "9. Conclusion & Future Work")
    add_body(doc, "The Flower Detection System (FloraScan) successfully demonstrates the practical convergence of edge-deployed computer vision and active Software Configuration Management governance. By utilizing an optimized EfficientNet-B0 backbone fine-tuned on the Oxford 102 Flowers dataset, the mobile client delivers 93.1% validation accuracy in an entirely offline, sub-20MB mobile footprint. Simultaneously, the application resolves the developmental opacity common in modern edge AI by embedding a native SCM audit engine that verifies version drift, traceability, release compliance, and change control in real time.")

    add_h2(doc, "Future Work")
    add_body(doc, "Future iterations of the platform will target three primary technical enhancements:")
    add_bullet(doc, "Automate end-to-end continuous integration pipelines using GitHub Actions (`.github/workflows/android.yml`) to automatically compile, test, and release APK binaries upon every tag push.", bold_prefix="1. Fully Automated CI/CD Pipelines: ")
    add_bullet(doc, "Integrate Data Version Control (DVC) paired with cloud object storage (AWS S3 or Google Cloud Storage) to track raw dataset versions, image augmentations, and full `.pth` checkpoints directly alongside Git commits.", bold_prefix="2. DVC Integration for ML Artifacts: ")
    add_bullet(doc, "Incorporate 8-bit dynamic integer quantization (INT8) using PyTorch Mobile Lite, reducing the model asset size from 17 MB to under 5 MB while decreasing CPU inference latency by an additional 40%.", bold_prefix="3. INT8 Quantization & Hardware Acceleration: ")

    # -------------------------------------------------------------
    # REFERENCES
    # -------------------------------------------------------------
    add_h1(doc, "References")
    add_body(doc, "1. Nilsback, M-E. and Zisserman, A. (2008). Automated Flower Classification over a Large Number of Classes. Proceedings of the Indian Conference on Computer Vision, Graphics and Image Processing, pp. 722–729.")
    add_body(doc, "2. Tan, M. and Le, Q. V. (2019). EfficientNet: Rethinking Model Scaling for Convolutional Neural Networks. Proceedings of the 36th International Conference on Machine Learning (ICML 2019), PMLR 97, pp. 6105–6114.")
    add_body(doc, "3. Banes, C. (2023). Haze: A glassmorphic blur library for Jetpack Compose. GitHub Repository: https://github.com/chrisbanes/haze")
    add_body(doc, "4. PyTorch Team (2022). PyTorch Mobile Lite Interpreter for Android. PyTorch Documentation, Linux Foundation. https://pytorch.org/mobile/android/")
    add_body(doc, "5. Android Open Source Project (2024). Jetpack Compose Design and Material 3 Architecture. Android Developers. https://developer.android.com/jetpack/compose")
    add_body(doc, "6. GitHub, Inc. (2024). GitHub REST API v3 Documentation: Commits, Pulls, and Releases. https://docs.github.com/en/rest")
    add_body(doc, "7. Chacon, S. and Straub, B. (2014). Pro Git: Everything You Need to Know About Git. 2nd Edition, Apress.")

    # Save output
    output_filename = "Flower_Detection_SCM_Report.docx"
    doc.save(output_filename)
    print(f"Document successfully created and saved as {output_filename}")

if __name__ == "__main__":
    create_report()
