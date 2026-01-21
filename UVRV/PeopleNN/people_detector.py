"""
Bus Occupancy Detector
Detects people in bus images and classifies occupancy as empty/semi_full/full.
"""

import torch
from torchvision.models.detection import fasterrcnn_resnet50_fpn_v2, FasterRCNN_ResNet50_FPN_V2_Weights
from torchvision.transforms import functional as F
from PIL import Image
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from pathlib import Path
from datetime import datetime
import colorsys

# Configuration
COCO_PERSON_CLASS = 1
CONFIDENCE_THRESHOLD = 0.7
SEMI_FULL_MAX = 8
MAX_IMAGE_DIM = 1024


def load_model(device=None):
    """Load Faster R-CNN model."""
    if device is None:
        device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    weights = FasterRCNN_ResNet50_FPN_V2_Weights.DEFAULT
    model = fasterrcnn_resnet50_fpn_v2(weights=weights)
    model.to(device)
    model.eval()
    return model, device


def detect_people(model, image_path, device):
    """Detect people in image. Returns count, boxes, scores, and resized image."""
    image = Image.open(image_path).convert("RGB")

    # Resize large images
    orig_w, orig_h = image.size
    if max(orig_w, orig_h) > MAX_IMAGE_DIM:
        scale = MAX_IMAGE_DIM / max(orig_w, orig_h)
        image = image.resize((int(orig_w * scale), int(orig_h * scale)), Image.LANCZOS)

    image_tensor = F.to_tensor(image).unsqueeze(0).to(device)

    with torch.no_grad():
        predictions = model(image_tensor)[0]

    labels = predictions['labels'].cpu().numpy()
    scores = predictions['scores'].cpu().numpy()
    boxes = predictions['boxes'].cpu().numpy()

    mask = (labels == COCO_PERSON_CLASS) & (scores >= CONFIDENCE_THRESHOLD)
    return len(boxes[mask]), boxes[mask], scores[mask], image


def classify(count):
    """Classify occupancy: empty (0), semi_full (1-8), full (>8)."""
    if count == 0:
        return "empty"
    elif count <= SEMI_FULL_MAX:
        return "semi_full"
    return "full"


def generate_colors(n):
    """Generate n distinct colors."""
    return [colorsys.hsv_to_rgb(i / max(n, 1), 0.8, 0.9) for i in range(n)]


def visualize(image, boxes, scores, count, classification, image_name, save_path=None):
    """Create side-by-side visualization."""
    fig, axes = plt.subplots(1, 2, figsize=(14, 6))

    axes[0].imshow(image)
    axes[0].set_title(f"Original: {image_name}")
    axes[0].axis('off')

    axes[1].imshow(image)
    axes[1].set_title(f"Detected: {count} people -> {classification.upper()}")

    colors = generate_colors(len(boxes))
    for i, (box, score) in enumerate(zip(boxes, scores)):
        x1, y1, x2, y2 = box
        rect = patches.Rectangle((x1, y1), x2 - x1, y2 - y1, linewidth=2, edgecolor=colors[i], facecolor='none')
        axes[1].add_patch(rect)
        axes[1].text(x1, y1 - 5, f"#{i+1} ({score:.2f})", color='white', fontsize=8, fontweight='bold',
                     bbox=dict(boxstyle='round', facecolor=colors[i], alpha=0.8))

    axes[1].axis('off')
    plt.tight_layout()

    if save_path:
        plt.savefig(save_path, dpi=150, bbox_inches='tight')
    plt.show()


def process_image(model, device, image_path, save_path=None):
    """Process single image and return result."""
    count, boxes, scores, image = detect_people(model, image_path, device)
    classification = classify(count)
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    if save_path:
        visualize(image, boxes, scores, count, classification, Path(image_path).name, save_path)

    return {
        'image': Path(image_path).name,
        'timestamp': timestamp,
        'classification': classification,
        'count': count
    }


def process_directory(model, device, directory, output_dir=None, show=False):
    """Process all images in directory."""
    directory = Path(directory)
    results = []

    if output_dir:
        output_dir = Path(output_dir)
        output_dir.mkdir(exist_ok=True)

    for image_path in sorted(directory.iterdir()):
        if image_path.suffix.lower() not in {'.jpg', '.jpeg', '.png', '.bmp'}:
            continue

        save_path = output_dir / f"{image_path.stem}_result.png" if output_dir else None
        count, boxes, scores, image = detect_people(model, image_path, device)
        classification = classify(count)
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        print(f"{image_path.name} | {timestamp} | {classification}")

        if show or save_path:
            visualize(image, boxes, scores, count, classification, image_path.name, save_path)

        results.append({
            'image': image_path.name,
            'timestamp': timestamp,
            'classification': classification,
            'count': count
        })

    return results


if __name__ == "__main__":
    model, device = load_model()
    base = Path(__file__).parent

    for dir_name in ['emptyBus', 'semiFull', 'fullBus']:
        dir_path = base / dir_name
        if dir_path.exists():
            print(f"\n--- {dir_name} ---")
            process_directory(model, device, dir_path, show=True)
