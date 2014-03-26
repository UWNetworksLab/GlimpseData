import os

files = os.listdir("pic")
files.sort()
for i in files:
    print(i.split(".")[0])
