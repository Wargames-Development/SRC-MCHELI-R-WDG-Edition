import os
import codecs

def replace_gravity_in_txt_files():
    # 获取当前目录下所有txt文件
    txt_files = [f for f in os.listdir('.') if f.endswith('.txt')]
    
    if not txt_files:
        print("当前文件夹中没有找到txt文件")
        return
    
    for filename in txt_files:
        try:
            # 以二进制方式读取文件，检测编码
            with open(filename, 'rb') as f:
                content_bytes = f.read()
            
            # 尝试用ANSI编码读取（Windows下ANSI通常指GBK）
            try:
                content = content_bytes.decode('gbk')
            except UnicodeDecodeError:
                # 如果不是ANSI，尝试用utf-8读取
                try:
                    content = content_bytes.decode('utf-8')
                except UnicodeDecodeError:
                    print(f"文件 {filename} 编码无法识别，跳过处理")
                    continue
            
            # 检查是否需要修改
            if "Gravity = -0.01" in content:
                # 替换参数
                new_content = content.replace("Gravity = -0.01", "Gravity = -0.006")
                
                # 以ANSI编码保存
                with codecs.open(filename, 'w', encoding='gbk') as f:
                    f.write(new_content)
                print(f"已修改文件: {filename}")
            else:
                print(f"文件 {filename} 中未找到目标参数，无需修改")
                
        except Exception as e:
            print(f"处理文件 {filename} 时出错: {str(e)}")

if __name__ == "__main__":
    print("开始处理txt文件...")
    replace_gravity_in_txt_files()
    print("处理完成！")
    input("按Enter键退出...")