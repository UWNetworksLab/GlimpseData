package edu.uwcse.netslab.videorecorder;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by syhan on 2014. 3. 4..
 */
public class SensorCollector implements SensorEventListener {

    private static final String TAG = "SensorCollector";

    private SensorManager mSensorManager;
    public SensorCollector(SensorManager sensormanager)
    {
        mSensorManager = sensormanager;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
        long curtime = System.currentTimeMillis();
        try {
            writer.write(curtime + " " + sensor.getType() + " *" + accuracy);
            writer.newLine();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // TODO Auto-generated method stub

        long curtime = System.currentTimeMillis();
        try {
            writer.write(curtime + " " + event.sensor.getType() + " " + Arrays.toString(event.values));
            writer.newLine();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void UnregisterSensors() {
        mSensorManager.unregisterListener(this);
    }

    private BufferedWriter writer;
    public void RegisterSensors(BufferedWriter writer) {
        this.writer = writer;
        Sensor lightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        Sensor gravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        Sensor accelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor gyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor linearSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor magneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor rotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        Sensor orientationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        Sensor ambient_temperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        Sensor proximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        Sensor pressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        if(lightSensor != null){
            Log.i(TAG, "lightSensor is registered");
            mSensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        if(gravitySensor != null){
            Log.i(TAG, "Gravity is registered");
            mSensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
        if(accelSensor != null){
            mSensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
        if(gyroSensor != null){
            mSensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        if(linearSensor != null){
            Log.i(TAG, "Linear sensor is registered");
            mSensorManager.registerListener(this, linearSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
        if(magneticSensor != null){
            Log.i(TAG, "Magnetic sensor is registered");
            mSensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
        if(rotationVector != null){
            mSensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_FASTEST);
        }
        if(orientationVector != null){
            Log.i(TAG, "Orientation sensor is registered");
            mSensorManager.registerListener(this, orientationVector, SensorManager.SENSOR_DELAY_FASTEST);
        }
        if(ambient_temperature != null) {
            Log.i(TAG, "Ambient temp is registered");
            mSensorManager.registerListener(this, ambient_temperature, SensorManager.SENSOR_DELAY_FASTEST);
        }
        if(proximity != null)
        {
            Log.i(TAG, "proximity is registered");
            mSensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_FASTEST);
        }
        if(pressure != null)
        {
            Log.i(TAG, "pressure is registered");
            mSensorManager.registerListener(this, pressure, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }
}
