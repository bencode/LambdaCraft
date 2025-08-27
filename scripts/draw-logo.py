import os
import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    print("错误: 需要安装PIL库。请运行: pip install Pillow")
    sys.exit(1)

def create_ascii_art():
    script_dir = Path(__file__).parent
    logo_path = script_dir.parent / "images" / "logo.png"
    
    if not logo_path.exists():
        print(f"错误: 找不到logo文件: {logo_path}")
        sys.exit(1)
    
    chars = " .:-=+*#%@"  # 从亮到暗的字符表
    
    try:
        img = Image.open(logo_path).convert("L")
        img = img.resize((80, 40))
        
        pixels = img.getdata()
        ascii_str = "".join([chars[min(pixel // 26, len(chars) - 1)] for pixel in pixels])
        ascii_img = "\n".join([ascii_str[i:i+80] for i in range(0, len(ascii_str), 80)])
        
        return ascii_img
    except Exception as e:
        print(f"错误: 处理图片时出错: {e}")
        sys.exit(1)

if __name__ == "__main__":
    print(create_ascii_art())

