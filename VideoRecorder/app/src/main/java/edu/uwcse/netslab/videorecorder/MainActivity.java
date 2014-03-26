package edu.uwcse.netslab.videorecorder;

import android.content.Context;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import java.util.List;

public class MainActivity extends Activity {
	private boolean startedRecord = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(savedInstanceState != null)
			startedRecord = savedInstanceState.getBoolean("startedRecord");
		
		setContentView(R.layout.activity_main);
		final Button button = (Button) findViewById(R.id.button1);
		if(startedRecord)
		{
			button.setText("Stop");
		}
		else
		{
			button.setText("Start");
		}
        button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                 // Perform action on click   
            	 if(startedRecord){
         			Log.w("video", "Stop service");
         			stopService(new Intent(MainActivity.this, RecorderService.class));
         			button.setText("Start");
         		}else{
         			Intent i = new Intent(MainActivity.this, RecorderService.class);
         			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         			startService(i);
         			button.setText("Stop");
         		}
         		
         		startedRecord = !startedRecord;
             }
        });

        final Button beep = (Button) findViewById(R.id.button_beep);
        beep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    long curTime = System.currentTimeMillis();
                    Log.i("Beep Time", ""+ curTime);
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();
                    curTime = System.currentTimeMillis();
                    Log.i("Beep Time end", ""+ curTime);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState){
		super.onSaveInstanceState(savedInstanceState);
		
		savedInstanceState.putBoolean("startedRecord", startedRecord);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		
		startedRecord = savedInstanceState.getBoolean("startedRecord");
	}
}
