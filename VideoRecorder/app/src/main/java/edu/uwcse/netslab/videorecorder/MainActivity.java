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
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

public class MainActivity extends Activity {
	private boolean startedRecord = false;
	public static MainActivity main = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        main = this;
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
        final TextView tv = (TextView) findViewById(R.id.tv_date);
        final TextView tv_status = (TextView) findViewById(R.id.tv_status);
        button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                 // Perform action on click   
            	 if(startedRecord){
         			Log.w("video", "Stop service");
         			stopService(new Intent(MainActivity.this, RecorderService.class));
         			button.setText("Start");
                    tv_status.setText("Not Recording");
                    tv.setText("N/A");

         		}else{
         			Intent i = new Intent(MainActivity.this, RecorderService.class);
         			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         			startService(i);
         			button.setText("Stop");
         		}
         		
         		startedRecord = !startedRecord;
             }
        });
	}

    public void update(Date date)
    {
        final TextView tv = (TextView) findViewById(R.id.tv_date);
        final TextView tv_status = (TextView) findViewById(R.id.tv_status);
        final Date dt = date;

        runOnUiThread(new Runnable(){
            public void run() {

                tv.setText(DateFormat.format("MM/dd kk:mm:ss", dt.getTime()));
                tv_status.setText("Recording");
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
