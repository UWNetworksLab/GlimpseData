GlimpseData
===========

Data collection and visualization framework

# Glimpse Visualizer
This is a visualizer for data collected through GlimpseData. 

## Data Preparation
Collected data is stored /sdcard/monocleV/[DATE]

So, create a directory and copy data from the sdcard.

	mkdir MYDATA
	adb pull /sdcard/monocleV/[DATE]

First you need to convert data. in the MYDATA directory, run get_pic_list.py to get list of photos.

	python ../get_pic_list.py > pic_list.txt
	
Audio file is stored in .3gp, you need to change it into wav, and txt.

	ffmpeg -i audio.3gp audio.wav
	python ../convert_wav.py audio.wav > audio.txt

Finally, you need to create .json file to indicate which directory to use and starttime of the audio file. To get timestamp when the audio recording started, see sensor.txt and use timestamp after #start. See example.json for formatting.

	{"dir": "example", "media_start":1394527030147}
	
## Visualize
Now everything is prepared. It can run on any web server, if you don't have one, you can run through python simpleHTTPServer from the glimpse_viz directory.

	python -m SimpleHTTPServer

Open a webbrowser and enter URL like
 
	http://127.0.0.1/glimpse_viz.html?data=[JSON]
	
If you don't input data parameter, it uses an example to illustrate. 
