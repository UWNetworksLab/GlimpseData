package edu.uwcse.netslab.videorecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by syhan on 2014. 1. 20..
 */
public class WifiScanner extends BroadcastReceiver {

    WifiManager wifi;
    public WifiScanner(WifiManager wifi)
    {
        this.wifi = wifi;
    }

    private BufferedWriter writer;
    private Context ct;
    private Timer timer;
    public void Start(String filename, Context c)
    {
        ct = c;
        c.registerReceiver(this, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        //Log.i("WifiM", "Scan started" + System.currentTimeMillis());
        try{
            writer = new BufferedWriter(new FileWriter(filename));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                wifi.startScan();
            }
        }, 0, 10);
    }

    public void Stop()
    {
        ct.unregisterReceiver(this);
        try{
            writer.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        timer.cancel();
        timer.purge();
        timer = null;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("WifiM", "Scan done" + System.currentTimeMillis());
        long curtime = System.currentTimeMillis();
        try{
            writer.write(curtime + "|");
            for(ScanResult sr: this.wifi.getScanResults())
            {
                writer.write(sr.BSSID + " " + sr.level + ",");

            }
            writer.newLine();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
