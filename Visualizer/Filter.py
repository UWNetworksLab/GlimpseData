from scipy import signal
# Currently this filter uses numbers for boundaries defined in Oliver's matlab code.
# Shoaib pointed out that it should be 13 bands instead of 11 for original MFCC bands. 
# Need to remove .62* later.
period16=16000
period8=8000
period4=4000
period2=2000
#why do we use different period value?
#periods = [period2] * 4 + [period4] * 2 + [period8] * 4 + [period16]
#mul = [8] * 4 + [4] * 2 + [2] * 4 + [1]
periods = [period16] * 11 
mul = [1] * 16
coefficients = [150, 517, 781, 1104, 1496, 1973, 2554, 3262, 4123, 5171]
filter_ba = []
for i in range(len(coefficients)-1):
    #print( (coefficients[i], coefficients[i+1]) )
    t = signal.butter(3, \
        [coefficients[i]*0.62/(periods[i]/2.0), \
        coefficients[i+1]*0.62/(periods[i]/2.0)], 'bandpass' )
    filter_ba.append(t)
filter_ba.append( signal.butter(3, 5171*0.62/(periods[9]/2), 'highpass') )
filter_ba.append( signal.butter(4, 4500./(periods[10]/2), 'highpass') )
import numpy as np
import matplotlib.pyplot as plt
#currently working on apply_filter function
def apply_filter(data):
    global filter_ba
    global periods
    res = []
    cnt = 0 
    for i in filter_ba:
        b, a = i
        filtered = np.array(signal.lfilter(b, a, data))
        res.append(sum(filtered*filtered)*mul[cnt])
        cnt += 1
    if(sum(res) == 0): raise Exception('result sum is zero')
    totalEnergy = np.log(sum(res))
    return [totalEnergy] + list(np.log(res)-totalEnergy)
    #plt.plot(np.arange(480), data, 'r')
    #plt.plot(np.arange(480), res[0], 'b')
    #plt.plot(np.arange(480), res[1], 'g')
        #print(np.log(sum(filtered*filtered)))
    #plt.show()
