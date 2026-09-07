import os
from PIL import Image, ImageDraw

def generate_icons(source_path):
    if not os.path.exists(source_path):
        print(f"Error: Source image '{source_path}' not found.")
        return

    sizes = {
        'mdpi': 48,
        'hdpi': 72,
        'xhdpi': 96,
        'xxhdpi': 144,
        'xxxhdpi': 192
    }

    base_res_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), 'app', 'src', 'main', 'res')

    try:
        img = Image.open(source_path)
        img = img.convert("RGBA")

        min_dim = min(img.size)
        left = (img.width - min_dim) / 2
        top = (img.height - min_dim) / 2
        right = (img.width + min_dim) / 2
        bottom = (img.height + min_dim) / 2
        img = img.crop((left, top, right, bottom))

        for density, size in sizes.items():
            res_dir = os.path.join(base_res_dir, f'mipmap-{density}')
            os.makedirs(res_dir, exist_ok=True)

            resized_img = img.resize((size, size), Image.Resampling.LANCZOS)
            standard_path = os.path.join(res_dir, 'ic_launcher.png')
            resized_img.save(standard_path, 'PNG')

            mask = Image.new('L', (size, size), 0)
            draw = ImageDraw.Draw(mask)
            draw.ellipse((0, 0, size, size), fill=255)

            round_img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
            round_img.paste(resized_img, (0, 0), mask)

            round_path = os.path.join(res_dir, 'ic_launcher_round.png')
            round_img.save(round_path, 'PNG')

    except Exception as e:
        print(f"Error generating icons: {e}")

if __name__ == "__main__":
    script_dir = os.path.dirname(os.path.abspath(__file__))
    source_image = os.path.join(script_dir, "source.jpg")
    generate_icons(source_image)
