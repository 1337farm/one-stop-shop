import os
import sys
from PIL import Image, ImageDraw

# Android icon sizes
SIZES = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192
}

def make_round_icon(img, size):
    # Resize first
    img_resized = img.resize((size, size), Image.Resampling.LANCZOS)

    # Create mask for round icon
    mask = Image.new('L', (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size, size), fill=255)

    # Apply mask
    round_img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    round_img.paste(img_resized, (0, 0), mask=mask)

    return round_img

def main():
    if len(sys.argv) < 2:
        print("Usage: python generate_icons.py <path_to_image>")
        sys.exit(1)

    img_path = sys.argv[1]

    try:
        base_img = Image.open(img_path).convert('RGBA')
    except Exception as e:
        print(f"Error opening image: {e}")
        sys.exit(1)

    # App resource directory
    res_dir = os.path.join(os.path.dirname(__file__), '..', '..', 'app', 'src', 'main', 'res')

    for density, size in SIZES.items():
        mipmap_dir = os.path.join(res_dir, f'mipmap-{density}')
        os.makedirs(mipmap_dir, exist_ok=True)

        # Standard icon
        standard_img = base_img.resize((size, size), Image.Resampling.LANCZOS)
        standard_path = os.path.join(mipmap_dir, 'ic_launcher.png')
        standard_img.save(standard_path, format='PNG')
        print(f"Saved {standard_path}")

        # Round icon
        round_img = make_round_icon(base_img, size)
        round_path = os.path.join(mipmap_dir, 'ic_launcher_round.png')
        round_img.save(round_path, format='PNG')
        print(f"Saved {round_path}")

if __name__ == '__main__':
    main()
