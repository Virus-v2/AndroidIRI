package com.northbridgeanalytics.mysensors;

//https://code.tutsplus.com/tutorials/using-the-accelerometer-on-android--mobile-22125

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG="MyMessage";
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor rotation;
    private long accelLastUpdate;
    private long rotationLastUpdate;
    private final float[] mRotationMatrix = new float[16];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get an instance to the accelerometer.
        this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        this.accelerometer = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.rotation = this.sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(this, accelerometer , SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, rotation , SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;
        mRotationMatrix[ 0] = 1;
        mRotationMatrix[ 4] = 1;
        mRotationMatrix[ 8] = 1;
        mRotationMatrix[12] = 1;

        Log.i(TAG, "array: " + Arrays.toString(mRotationMatrix));

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - accelLastUpdate) > 1000) {
                long diffTime = (curTime - accelLastUpdate);
                accelLastUpdate = curTime;

//                Log.i(TAG, "ax: " + x + " ay: " + y + " az: " + z);

            }
        }

        if (mySensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            SensorManager.getRotationMatrixFromVector(
                    mRotationMatrix , sensorEvent.values);


            long curTime = System.currentTimeMillis();

            if ((curTime - rotationLastUpdate) > 5000) {
                long diffTime = (curTime - rotationLastUpdate);
                rotationLastUpdate = curTime;

                Log.i(TAG, "matrix: " + Arrays.toString(mRotationMatrix));
//                Log.i(TAG, "rx: " + x + " ry: " + y + " rz: " + z);

            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }



}
