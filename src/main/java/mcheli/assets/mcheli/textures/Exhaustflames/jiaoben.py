import os
import shutil

# 创建并运行脚本
script_content = '''
import os
import shutil

def copy_and_rename():
    for i in range(24):
        original = f"radar{i}.png"
        new_name = f"radar{47-i}.png"
        
        if os.path.exists(original):
            shutil.copy2(original, new_name)
            print(f"已复制 {original} -> {new_name}")
        else:
            print(f"警告: 文件 {original} 不存在")

if __name__ == "__main__":
    copy_and_rename()
    print("完成！共复制了 24 张图片")
'''

# 保存为Python文件
with open("copy_radar_images.py", "w", encoding="utf-8") as f:
    f.write(script_content)

print("Python脚本已创建为 'copy_radar_images.py'")
print("请运行: python copy_radar_images.py")