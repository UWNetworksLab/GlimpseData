package edu.uwcse.netslab.videorecorder;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by syhan on 2014. 1. 19..
 */
public class LocationWriter implements LocationListener {

    LocationManager lm;

    public LocationWriter(LocationManager lm)
    {
        this.lm = lm;

    }

    private BufferedWriter writer;
    public void start(String filename)
    {
        try{
        writer = new BufferedWriter(new FileWriter(filename));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        this.lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        this.lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
    }

    public void stop()
    {
        this.lm.removeUpdates(this);
        try{
            writer.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            writer.write(location.getTime() + " " + location.getProvider() + " " + location.getAccuracy() + " " + location.getLatitude() + " " + location.getLongitude() + " " + location.getSpeed());
            writer.newLine();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
