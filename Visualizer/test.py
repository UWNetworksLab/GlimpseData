import time, datetime

while True:
    t = datetime.datetime.now()
    print(str(t) + " " + str(t.time()))
    time.sleep(0.2)
