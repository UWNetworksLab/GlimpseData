import sys
import os
import cv2

def detect(path):
    img = cv2.imread(path)
    cascade = cv2.CascadeClassifier("opencv_xmls/haarcascade_frontalface_alt.xml")
    rects = cascade.detectMultiScale(img, 1.3, 4, cv2.cv.CV_HAAR_SCALE_IMAGE, (20,20))

    if len(rects) == 0:
        return [], img
    rects[:, 2:] += rects[:, :2]
    return rects, img

def box(rects, img, path):
    for x1, y1, x2, y2 in rects:
        cv2.rectangle(img, (x1, y1), (x2, y2), (127, 255, 0), 2)
    cv2.imwrite(path, img);


target_dir = sys.argv[1]
for fname in os.listdir(target_dir):
    if(not fname.endswith(".jpg")): continue
    rects, img = detect(os.path.join(target_dir,fname))
    pout = fname + "\t" + str(len(rects))
    if(len(rects) > 0):
        for x1, y1, x2, y2 in rects:
            t = [x1,y1,x2,y2]
            pout += "\t" + ",".join(map(str,t))

    print(pout)
    #box(rects, img, os.path.join(target_dir, "d_"+fname))
