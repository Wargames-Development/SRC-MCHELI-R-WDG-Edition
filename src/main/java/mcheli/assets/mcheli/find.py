import os

SEARCH_STRING = "1200s"

# Directory where THIS script is located
ROOT_DIR = os.path.dirname(os.path.abspath(__file__))

for root, dirs, files in os.walk(ROOT_DIR):
    for filename in files:
        file_path = os.path.join(root, filename)

        try:
            with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                for line_num, line in enumerate(f, start=1):
                    if SEARCH_STRING in line:
                        print(f"FILE: {filename}")
                        print(f"PATH: {file_path}")
                        print(f"LINE {line_num}: {line.strip()}")
                        print("=" * 60)
                        break  # stop after first hit in this file
        except:
            pass

print("SEARCH COMPLETE")
