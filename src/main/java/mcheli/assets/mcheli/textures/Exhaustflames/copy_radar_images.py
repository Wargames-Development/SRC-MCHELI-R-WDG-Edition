
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
