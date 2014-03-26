import struct, Filter
# check format for the XBox Data
def to_printable(c):
    if 33 <= ord(c) <= 125:
        return c
    return "." 

def hexdump(blob):
    bytes = map(lambda c : "%02X" % ord(c), blob)
    bytes.extend(["  "] * 0x10)

    line = []
    for offset in range(0, len(bytes)/0x10):
        s = offset * 0x10
        e = s + 0x10

        col_hex = " ".join(bytes[s:e])
        col_str = "".join(map(to_printable, blob[s:e]))

        if col_str != "": 
            line.append("%08X: %s %s" % (s, col_hex, col_str))

    return "\n".join(line)


def loadWavFileCore(filename):
    with open(filename, "rb") as f:
        fileContent = f.read()
        dataSize = struct.unpack("<l", fileContent[76:80])[0]

        #chunkSize = struct.unpack("h", fileContent[16:18])
        #position = 40+chunkSize[0]-16
        if(dataSize != len(fileContent)-80):
            print("error processing " + filename)
            exit(0)
        wav = struct.unpack(str(dataSize/2)+"h", fileContent[80:])
        wav = map(lambda x:float(x)/32768, wav) 
        return wav

class FileLengthError(Exception):
    def __str__(self):
        return repr('FileLengthError')

def bandFilters(wav, skipFirst):
    rangeend = len(wav)/480
    frames = []
    if(skipFirst): startI = 1
    else: startI = 0
    for i in range(rangeend):
        try:
            res = Filter.apply_filter(wav[i*480:(i+1)*480])[startI:]
        except Exception as e:
            print(e)
            raise Exception('LogSumError')
        frames.append(res)
    return frames


def loadWavIntoFrames(filename, strip=False, earlyclip=False, skipFirst=True): 
    wav = loadWavFileCore(filename)
    if(len(wav) == 0): return [] 
    if(earlyclip):
        wav = removeEarlyClip(wav)
    if(strip):
        wav = stripSilence(wav)
    return bandFilters(wav, skipFirst)

import os
def getFileList(filename):
    with open(filename, "r") as f:
        files = f.readlines()
        files = map(lambda x:x.strip(), files)
        files = map(lambda x:os.sep.join(x.split("\\")), files)
        return files
import math
def loadFrames(filename, skipFirst = True):
    with open(filename) as f:
        lines = f.readlines()
        frames = []
        for line in lines:
            data = map(float, line.strip().split(","))
            if(sum(map(math.isnan, data)) > 0): 
                print("NAN skipping!!", filename)
                return []
            if(skipFirst): 
                frames.append( data[1:] )
            else:  
                frames.append( data )
        return frames

def removeEarlyClip(wav):
    for i in range(len(wav)):
        if(wav[i] >=0):
            return wav[i:]
import numpy as np
def stripSilence(wav):
    abs_wav = map(abs, wav) 
    start = 0 
    len_idx = len(wav)/480
    for i in range(len_idx):
        avg_val = np.average(abs_wav[i*480:(i+1)*480])
        if(avg_val > 0.01): 
            start = i
            break
    end = len_idx
    for i in range(len_idx):
        avg_val = np.average(abs_wav[(len_idx-i-1)*480:(len_idx-i)*480])
        if(avg_val > 0.01): 
            end = len_idx - i
            break
    return wav[start*480:end*480]
    
def addFramesIntoMatrix(X, frames, unit=3):
    for i in range(len(frames)-unit+1):
        t = []
        for j in range(unit):
            t += frames[i+j]
        X.append(t)
    return len(frames)-unit+1
