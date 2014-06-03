package edu.uwcse.netslab.videorecorder;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

public class RecorderService extends Service implements SurfaceHolder.Callback {

	private WindowManager windowManager;
    private SurfaceView surfaceView;
    private Camera mCamera = null;
    private final boolean useHID = true;
	
    public class LocalBinder extends Binder {
    	RecorderService getService() {
            return RecorderService.this;
        }
    }
    
    private static final String TAG = "RecorderService";
    private SensorManager mSensorManager;
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
  
    private final IBinder mBinder = new LocalBinder();
	private static final String PATH = Environment.getExternalStorageDirectory().toString() + "/glimpse";
    private HIDReader hid = new HIDReader();
    private LocationWriter lw;
    private SensorCollector sc;
    private WifiScanner ws;

    @Override
    public void onCreate() {
    	File dir = new File(PATH);
		if(!dir.exists()){
			Log.w(TAG, "video directory does not exist, creating");
			dir.mkdir();
		}
        // Display a notification about us starting.  We put an icon in the status bar.
        Notification notification = new Notification.Builder(this)
        .setContentTitle("Background Video Recorder")
        .setContentText("")
        .setSmallIcon(R.drawable.ic_launcher)
        .build();
        startForeground(1234, notification);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);

        surfaceView = new SurfaceView(this);
        LayoutParams layoutParams = new WindowManager.LayoutParams(
            1, 1,
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        windowManager.addView(surfaceView, layoutParams);
        surfaceView.getHolder().addCallback(this);

        lw = new LocationWriter((LocationManager) getSystemService(Context.LOCATION_SERVICE));
        ws = new WifiScanner((WifiManager) getSystemService(Context.WIFI_SERVICE));
        sc = new SensorCollector((SensorManager) getSystemService(Context.SENSOR_SERVICE));
    }

    @Override
	public IBinder onBind(Intent intent) {
    	return mBinder;
	}
    
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {}
	
	private long startTime;
	private Date startDate;
	public BufferedWriter writer;
    SurfaceHolder surfaceHolder;


    private static final int SECOND = 1000;
    private static final int MINUTE = 60*SECOND;
    private static final int max_duration = 20*MINUTE;
    private RehearsalAudioRecorder audioRecorder;

    private void startRecorder(Date startDate)
    {
        if(startDate == null) {
            startDate = new Date();
        }

        if(MainActivity.main != null)
        {
            MainActivity.main.update(startDate);
        }

        mCamera = Camera.open();

        Camera.Parameters parameters = mCamera.getParameters();
        for(Camera.Size s: parameters.getSupportedPreviewSizes())
        {
            Log.i(TAG, ""+s.width +" " +s.height);
        }

        final String pathWithDate =  PATH + "/" + DateFormat.format("yyyy-MM-dd_kk-mm-ss", startDate.getTime());
        File dir_pic = new File(pathWithDate+"/pic");
        dir_pic.mkdir();
        audioRecorder = new RehearsalAudioRecorder(false, MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        audioRecorder.setOutputFile(pathWithDate+"/audio.3gp");
        audioRecorder.prepare();
        if(writer != null)
        {
            try {
                long curTime = System.currentTimeMillis();
                writer.write("#start " + curTime);
                writer.newLine();

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        audioRecorder.start();

        parameters.setPreviewFpsRange(30000, 30000);
        parameters.setPreviewSize(640, 480);
        parameters.setRotation(90);
        mCamera.setParameters(parameters);
        final Camera.Size previewSize = parameters.getPreviewSize();

        int dataBufferSize=(int)(previewSize.height*previewSize.width*
                (ImageFormat.getBitsPerPixel(mCamera.getParameters().getPreviewFormat())/8.0));
        for(int i=0;i<4;i++) mCamera.addCallbackBuffer(new byte[dataBufferSize]);
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
            mCamera.release();
            mCamera = null;
            return;
        }
        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            private long timestamp=0;
            public synchronized void onPreviewFrame(byte[] data, Camera camera) {
                timestamp=System.currentTimeMillis();
                //do picture data process
                YuvImage image = new YuvImage(data, ImageFormat.NV21, previewSize.width, previewSize.height, null);

                File file = new File(pathWithDate+"/pic/"+timestamp+".jpg");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, baos);
                    byte[] rawImage = baos.toByteArray();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length);
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, previewSize.width, previewSize.height, matrix, false);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                camera.addCallbackBuffer(data);
                return;
            }
        });

        try {
            mCamera.startPreview();
        } catch (Exception e) {
            mCamera.release();
            mCamera = null;
            return;
        }
    }

	@Override
	public void surfaceCreated(SurfaceHolder surfaceHolder) {
		// TODO Auto-generated method stub

        startDate = new Date();
        final String pathWithDate =  PATH + "/" + DateFormat.format("yyyy-MM-dd_kk-mm-ss", startDate.getTime());
        File dir = new File(pathWithDate);
        dir.mkdir();
        try {
            writer = new BufferedWriter(new FileWriter(pathWithDate + "/sensor.txt"));
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        this.surfaceHolder = surfaceHolder;


        if(useHID) hid.start(pathWithDate+"/hid.txt");
        lw.start(pathWithDate+"/loc.txt");
        ws.Start(pathWithDate + "/wifi.txt", this);




        startRecorder(startDate);
        sc.RegisterSensors(writer);
	}

    private void stopRecorder()
    {
        if(mCamera != null) {
            mCamera.stopPreview();
            try {
                mCamera.setPreviewDisplay(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.release();
            mCamera = null;
        }

        //mediaRecorder.release();


    }
	
	@Override
    public void onDestroy() {
        Log.i(TAG, "on destroy");
        stopRecorder();
        sc.UnregisterSensors();
        if(useHID) hid.stop();
        lw.stop();
        ws.Stop();
        audioRecorder.stop();
        audioRecorder.release();
        windowManager.removeView(surfaceView);
        Log.i(TAG, "Service Destroyed");
        
        if(writer != null)
        {
        	try {
        		long curTime = System.currentTimeMillis();
        		writer.write("#stop " + curTime);
    			writer.newLine();
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	writer = null;
        }
        
    }

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}
}
