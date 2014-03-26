from wavUtil import *
import sys

wav = loadWavFileCore(sys.argv[1])
for f in wav:
    print(f)
