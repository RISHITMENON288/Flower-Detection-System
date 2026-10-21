import matplotlib.pyplot as plt
import matplotlib.patches as patches
import os

def create_architecture_diagram(output_path):
    # Setup canvas: 18 x 12.5 inches at 300 DPI
    fig, ax = plt.subplots(figsize=(18, 12.5), dpi=300)
    ax.set_xlim(0, 18)
    ax.set_ylim(0, 12.5)
    ax.axis('off')

    # Background
    BG_COLOR = '#F8FAFC'  # Clean soft slate
    fig.patch.set_facecolor(BG_COLOR)
    ax.set_facecolor(BG_COLOR)

    # Color Palette
    PRIMARY = '#0F172A'       # Deep Slate
    BLUE_BORDER = '#0284C7'   # Sky Blue
    BLUE_BG = '#F0F9FF'
    GREEN_BORDER = '#059669'  # Emerald
    GREEN_BG = '#ECFDF5'
    AMBER_BORDER = '#D97706'  # Amber
    AMBER_BG = '#FFFBEB'
    PURPLE_BORDER = '#7C3AED' # Violet
    PURPLE_BG = '#F5F3FF'
    SLATE_BORDER = '#475569'
    SLATE_BG = '#F1F5F9'
    MUTED = '#64748B'

    # Title & Subtitle
    ax.text(9.0, 12.0, "FloraScan System Architecture & Dataflow Blueprint",
            ha='center', va='center', fontsize=22, fontweight='bold', color=PRIMARY)
    ax.text(9.0, 11.6, "Edge AI Flower Classification with Embedded SCM Governance & Audit Dashboard",
            ha='center', va='center', fontsize=12, fontweight='normal', color=MUTED)

    # Helper: draw container group with custom banner position
    def draw_group(x, y, w, h, title, border_color, banner_x_offset=0.25):
        bg = patches.FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.0,rounding_size=0.18",
                                    linewidth=1.4, edgecolor=border_color, facecolor='#FFFFFF', linestyle='--', zorder=1)
        ax.add_patch(bg)
        tw = len(title) * 0.125 + 0.45
        header = patches.FancyBboxPatch((x + banner_x_offset, y + h - 0.22), tw, 0.42, boxstyle="round,pad=0.0,rounding_size=0.08",
                                       facecolor=border_color, edgecolor='none', zorder=3)
        ax.add_patch(header)
        ax.text(x + banner_x_offset + tw/2, y + h - 0.01, title.upper(), ha='center', va='center', fontsize=8.2, fontweight='bold', color='#FFFFFF', zorder=4)

    # Helper: draw component card with safe badge positioning
    def draw_card(x, y, w, h, title, items, bg='#FFFFFF', border='#CBD5E1', title_color=PRIMARY, tag=None, tag_color=PRIMARY):
        card = patches.FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.0,rounding_size=0.12",
                                      linewidth=1.2, edgecolor=border, facecolor=bg, zorder=2)
        ax.add_patch(card)
        
        # Title (left-aligned)
        ax.text(x + 0.22, y + h - 0.28, title, ha='left', va='center', fontsize=9.6, fontweight='bold', color=title_color, zorder=4)
        
        # Pill tag on right
        if tag:
            tw = len(tag) * 0.095 + 0.30
            tbox = patches.FancyBboxPatch((x + w - tw - 0.18, y + h - 0.38), tw, 0.22, boxstyle="round,pad=0.0,rounding_size=0.06",
                                         facecolor=tag_color, edgecolor='none', zorder=4)
            ax.add_patch(tbox)
            ax.text(x + w - 0.18 - tw/2, y + h - 0.27, tag, ha='center', va='center', fontsize=6.8, fontweight='bold', color='#FFFFFF', zorder=5)

        # Divider line
        ax.plot([x + 0.2, x + w - 0.2], [y + h - 0.52, y + h - 0.52], color=border, linewidth=0.8, alpha=0.6, zorder=3)

        # Bullet items
        start_y = y + h - 0.78
        for i, item in enumerate(items):
            cur_y = start_y - (i * 0.25)
            ax.text(x + 0.22, cur_y, "•", ha='left', va='center', fontsize=8.5, color=border, fontweight='bold', zorder=4)
            ax.text(x + 0.42, cur_y, item, ha='left', va='center', fontsize=7.8, color='#334155', zorder=4)

    # -------------------------------------------------------------------------
    # 1. PRESENTATION LAYER (Top: y=8.6 to 11.2)
    # -------------------------------------------------------------------------
    draw_group(0.6, 8.6, 16.8, 2.6, "1. Presentation Layer (Android Jetpack Compose UI & Navigation)", BLUE_BORDER, banner_x_offset=0.3)

    draw_card(0.9, 8.85, 3.1, 2.0, "Auth & Login",
              ["Glassmorphic UI design", "Firebase Google & Email Auth", "Session state gatekeeper"],
              bg=BLUE_BG, border=BLUE_BORDER, tag="COMPOSE", tag_color=BLUE_BORDER)

    draw_card(4.25, 8.85, 3.1, 2.0, "SCM Dashboard",
              ["5 Auditing cards & telemetry", "Traceability commit capsules", "Live repo branch / tag audit"],
              bg=PURPLE_BG, border=PURPLE_BORDER, title_color=PURPLE_BORDER, tag="AUDIT", tag_color=PURPLE_BORDER)

    draw_card(7.65, 8.85, 3.1, 2.0, "Home Scanner",
              ["Live CameraX / Gallery picker", "Interactive scanning card", "Recent detections stream"],
              bg=BLUE_BG, border=BLUE_BORDER, tag="PRIMARY", tag_color=BLUE_BORDER)

    draw_card(11.05, 8.85, 3.0, 2.0, "Explore Catalog",
              ["Oxford 102 species catalog", "Dynamic search & filtering", "Botanical details & taxonomy"],
              bg=BLUE_BG, border=BLUE_BORDER, tag="CATALOG", tag_color=BLUE_BORDER)

    draw_card(14.35, 8.85, 2.75, 2.0, "History Archive",
              ["Local past inference logs", "Confidence score breakdown", "Direct item re-inspection"],
              bg=BLUE_BG, border=BLUE_BORDER, tag="STORAGE", tag_color=BLUE_BORDER)

    # -------------------------------------------------------------------------
    # 2. APPLICATION & LOGIC LAYER (Middle: y=5.0 to 7.6)
    # -------------------------------------------------------------------------
    draw_group(0.6, 5.0, 16.8, 2.6, "2. Domain & Application Logic (Kotlin Coroutines / MVVM Architecture)", GREEN_BORDER, banner_x_offset=0.3)

    draw_card(0.9, 5.25, 3.6, 2.0, "ViewModel & StateFlow",
              ["StateFlow & UI State hoisting", "Firebase Auth session state", "Screen lifecycle binding", "Reactive UI event dispatching"],
              bg=GREEN_BG, border=GREEN_BORDER, tag="MVVM", tag_color=GREEN_BORDER)

    draw_card(4.9, 5.25, 4.0, 2.0, "SCM Governance Engine",
              ["ScmHistory.kt & Traceability.kt", "Git commit message regex audit", "Semantic version diffing (GitHub)", "Untraced commit capsule isolation"],
              bg=PURPLE_BG, border=PURPLE_BORDER, title_color=PURPLE_BORDER, tag="GOVERNANCE", tag_color=PURPLE_BORDER)

    draw_card(9.3, 5.25, 4.5, 2.0, "ML Inference Controller",
              ["FlowerClassifier.kt asset bridge", "224x224 RGB image normalization", "PyTorch Lite native tensor pass", "Top-5 softmax probability extractor"],
              bg=GREEN_BG, border=GREEN_BORDER, tag="EDGE AI", tag_color=GREEN_BORDER)

    draw_card(14.2, 5.25, 2.9, 2.0, "Local Data Cache",
              ["Prediction history storage", "SharedPreferences / JSON cache", "Botanical metadata repository", "Offline recovery support"],
              bg=SLATE_BG, border=SLATE_BORDER, tag="CACHE", tag_color=SLATE_BORDER)

    # -------------------------------------------------------------------------
    # 3A. REMOTE CLOUD & VCS SERVICES (Bottom-Left: x=0.6 to 7.8, y=0.7 to 4.0)
    # -------------------------------------------------------------------------
    # Banner placed on top-center of container
    draw_group(0.6, 0.7, 7.3, 3.3, "3A. Remote Services & Cloud APIs", PRIMARY, banner_x_offset=1.5)

    # Side-by-side cards: Left is Firebase under ViewModel; Right is GitHub under SCM Engine!
    draw_card(0.9, 1.0, 3.3, 2.6, "Firebase Auth Platform",
              ["OAuth 2.0 Google Tokens", "Email/Password session gate", "Client token refresh", "Cloud authentication state", "Identity listener binding"],
              bg=SLATE_BG, border=PRIMARY, tag="AUTH", tag_color=PRIMARY)

    draw_card(4.45, 1.0, 3.2, 2.6, "GitHub REST API & CDN",
              ["api.github.com/repos/...", "Releases, Commits, PRs", "Repo compare endpoints", "raw.githubusercontent.com", "build.gradle.kts & toml"],
              bg=SLATE_BG, border=PRIMARY, tag="HTTPS REST", tag_color=PRIMARY)

    # -------------------------------------------------------------------------
    # 3B. ON-DEVICE EDGE ML PIPELINE (Bottom-Right: x=8.2 to 17.4, y=0.7 to 4.0)
    # -------------------------------------------------------------------------
    # Banner placed at right side of container so it doesn't overlap the left arrow!
    draw_group(8.2, 0.7, 9.2, 3.3, "3B. On-Device Edge AI Pipeline (PyTorch Mobile Lite)", AMBER_BORDER, banner_x_offset=2.8)

    draw_card(8.45, 1.0, 2.8, 2.6, "1. Preprocessing",
              ["Input: Camera/Gallery Bitmap", "Center-crop & resize: 224 x 224", "ImageNet Normalization:", "  mean = [0.485, 0.456, 0.406]", "  std  = [0.229, 0.224, 0.225]", "1x3x224x224 FloatTensor"],
              bg=AMBER_BG, border=AMBER_BORDER, tag="TENSOR", tag_color=AMBER_BORDER)

    draw_card(11.5, 1.0, 3.2, 2.6, "2. EfficientNet-B0 PTL",
              ["Asset: flower_model.ptl", "Size: 17.3 MB (TorchScript Lite)", "EfficientNet-B0 backbone", "Classifier: Linear(1280, 102)", "Trained on RTX 3050 (Oxford 102)", "Validation Accuracy: 93.1%"],
              bg='#FEF3C7', border=AMBER_BORDER, title_color='#92400E', tag="CORE ML", tag_color='#B45309')

    draw_card(14.95, 1.0, 2.2, 2.6, "3. Output",
              ["Softmax layer pass", "Sorted top-5 classes", "Confidence % mapping", "Species labels mapping", "Botanical descriptions"],
              bg=AMBER_BG, border=AMBER_BORDER, tag="OUTPUT", tag_color=AMBER_BORDER)

    # Internal pipeline horizontal arrows
    ax.annotate('', xy=(11.4, 2.3), xytext=(11.25, 2.3),
                arrowprops=dict(arrowstyle="->", color=AMBER_BORDER, lw=2.2), zorder=5)
    ax.annotate('', xy=(14.85, 2.3), xytext=(14.7, 2.3),
                arrowprops=dict(arrowstyle="->", color=AMBER_BORDER, lw=2.2), zorder=5)

    # -------------------------------------------------------------------------
    # INTER-LAYER ARROWS & DATAFLOW BADGES (Zero collision!)
    # -------------------------------------------------------------------------
    def draw_badge(x, y, text, color=PRIMARY):
        bbox = dict(boxstyle='round,pad=0.25', facecolor='#FFFFFF', edgecolor=color, linewidth=1.0)
        ax.text(x, y, text, ha='center', va='center', fontsize=7.2, fontweight='bold', color=color, bbox=bbox, zorder=6)

    # --- Vertical Connections Between Layer 1 (UI) and Layer 2 (Logic) ---
    # 1. Auth & Login <-> ViewModel (x=2.4)
    ax.annotate('', xy=(2.4, 7.3), xytext=(2.4, 8.8),
                arrowprops=dict(arrowstyle="<->", color=BLUE_BORDER, lw=1.8), zorder=5)
    draw_badge(2.4, 8.1, "User Auth Events", BLUE_BORDER)

    # 2. SCM Dashboard <-> SCM Governance Engine (x=5.8)
    ax.annotate('', xy=(5.8, 7.3), xytext=(5.8, 8.8),
                arrowprops=dict(arrowstyle="<->", color=PURPLE_BORDER, lw=1.8), zorder=5)
    draw_badge(5.8, 8.1, "SCM Telemetry Request", PURPLE_BORDER)

    # 3. Home Scanner -> ML Inference Controller (x=9.5)
    ax.annotate('', xy=(9.8, 7.3), xytext=(9.2, 8.8),
                arrowprops=dict(arrowstyle="->", color=GREEN_BORDER, lw=1.8), zorder=5)
    draw_badge(9.5, 8.1, "Captured Bitmap", GREEN_BORDER)

    # 4. Explore / History <-> Cache (x=15.2)
    ax.annotate('', xy=(15.2, 7.3), xytext=(15.2, 8.8),
                arrowprops=dict(arrowstyle="<->", color=SLATE_BORDER, lw=1.8), zorder=5)
    draw_badge(15.2, 8.1, "Archive & Catalog", SLATE_BORDER)

    # --- Vertical Connections Between Layer 2 (Logic) and Layer 3 (Infrastructure/ML) ---
    # 5. ViewModel <-> Firebase Identity Platform (Straight down on left, x=2.4)
    ax.annotate('', xy=(2.4, 3.65), xytext=(2.4, 5.2),
                arrowprops=dict(arrowstyle="<->", color=PRIMARY, lw=1.8), zorder=5)
    draw_badge(2.4, 4.5, "OAuth Session / Tokens", PRIMARY)

    # 6. SCM Governance Engine <-> GitHub REST API (Straight down on left-center, x=5.8)
    ax.annotate('', xy=(5.8, 3.65), xytext=(5.8, 5.2),
                arrowprops=dict(arrowstyle="<->", color=PURPLE_BORDER, lw=1.8), zorder=5)
    draw_badge(5.8, 4.5, "GitHub REST API Telemetry", PURPLE_BORDER)

    # 7. ML Controller -> Preprocessing (Down into Stage 1, x=9.8)
    ax.annotate('', xy=(9.8, 3.65), xytext=(10.5, 5.2),
                arrowprops=dict(arrowstyle="->", color=AMBER_BORDER, lw=1.8), zorder=5)
    draw_badge(10.1, 4.5, "Raw Bitmap Tensor Buffer", AMBER_BORDER)

    # 8. ML Output -> ML Controller (Up from Stage 3, x=15.5 to 12.8)
    ax.annotate('', xy=(12.8, 5.2), xytext=(15.5, 3.65),
                arrowprops=dict(arrowstyle="->", color=GREEN_BORDER, lw=1.8), zorder=5)
    draw_badge(14.2, 4.5, "Top-5 Predictions & Metadata", GREEN_BORDER)

    # Footer Caption
    ax.text(9.0, 0.25, "FloraScan Architectural Blueprint • Designed for VIT University SCM Review • Rishit (24MIS0185)",
            ha='center', va='center', fontsize=9.0, color=MUTED, fontweight='medium')

    plt.tight_layout()
    plt.savefig(output_path, dpi=300, facecolor=fig.get_facecolor(), edgecolor='none', bbox_inches='tight')
    plt.close()
    print(f"Generated pixel-perfect architecture diagram at {output_path}")

if __name__ == '__main__':
    out_dir = r"c:\Users\menon\OneDrive\Desktop\SCM Project\screenshots"
    os.makedirs(out_dir, exist_ok=True)
    out_file = os.path.join(out_dir, "architecture_diagram.png")
    create_architecture_diagram(out_file)

    root_file = r"c:\Users\menon\OneDrive\Desktop\SCM Project\architecture_diagram.png"
    create_architecture_diagram(root_file)

    artifact_dir = r"C:\Users\menon\.gemini\antigravity\brain\c825a4cf-24bb-4332-990d-a9514cd9f1f8"
    artifact_file = os.path.join(artifact_dir, "architecture_diagram.png")
    create_architecture_diagram(artifact_file)
