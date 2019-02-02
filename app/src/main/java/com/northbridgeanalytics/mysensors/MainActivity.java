package com.northbridgeanalytics.mysensors;

// https://code.tutsplus.com/tutorials/using-the-accelerometer-on-android--mobile-22125
// https://google-developer-training.gitbooks.io/android-developer-advanced-course-practicals/content/unit-1-expand-the-user-experience/lesson-3-sensors/3-2-p-working-with-sensor-based-orientation/3-2-p-working-with-sensor-based-orientation.html
// https://stackoverflow.com/questions/5464847/transforming-accelerometers-data-from-devices-coordinates-to-real-world-coordi
// https://stackoverflow.com/questions/23701546/android-get-accelerometers-on-earth-coordinate-system
// https://stackoverflow.com/questions/11578636/acceleration-from-devices-coordinate-system-into-absolute-coordinate-system

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity
        implements SensorEventListener {

    // Default tag for our Log
    public static final String TAG ="MyMessage";

    // System sensor manager instance.
    private SensorManager mSensorManager;

    // Accelerometer and magnetometer sensors, as retrieved from the
    // sensor manager.
    private Sensor mSensorAccelerometer;
    private Sensor mSensorMagnetometer;
    private Sensor mSensorGravity;

    // TextViews to display current sensor values.
    private TextView mTextSensorPhoneAccX;
    private TextView mTextSensorPhoneAccY;
    private TextView mTextSensorPhoneAccZ;
    private TextView mTextSensorEarthAccX;
    private TextView mTextSensorEarthAccY;
    private TextView mTextSensorEarthAccZ;
    private TextView mTextSensorPhoneAzimuth;
    private TextView mTextSensorPhonePitch;
    private TextView mTextSensorPhoneRoll;

    // Very small values for the accelerometer (on all three axes) should
    // be interpreted as 0. This value is the amount of acceptable
    // non-zero drift.
    private static final float VALUE_DRIFT = 0.05f;

    private float[] mAccelerometerData = new float[3];
    private float[] mMagnetometerData = new float[3];
    private float[] mGravityData = new float[3];

    public double[] radiansToDegrees(float[] inputRadians) {
        double[] outputDegrees = new double[inputRadians.length];

        for (int i=0; i < inputRadians.length; i++) {
            outputDegrees[i] = inputRadians[i] * (180/Math.PI);
        }

        return outputDegrees;
    }


    public float[] phoneOrientation(float[] accelorometer, float[] magnetometer) {
        float[] rotationMatrix = new float[9];

        // Not sure exactly what this does, but populates the matrix with the input data. rotationOK returns true if the
        // .getRotationMatrix method is successful.
        // "You can transform any vector from the phone's coordinate system to the Earth's coordinate system by
        // multiplying it with the rotation matrix."
        boolean rotationOK = SensorManager.getRotationMatrix(rotationMatrix,
                null, accelorometer, magnetometer);

        // Empty Float array to hold the azimuth, pitch, and roll.
        float orientationValues[] = new float[3];

        // If the getRotationMatrix method is successfull run the following code.
        if (rotationOK) {

//            Log.i(TAG, Arrays.toString(rotationMatrix));

            SensorManager.getOrientation(rotationMatrix, orientationValues);

            // Azimuth, pitch, and roll in radians, taken from  the .getOrientation method.
//            float azimuth = orientationValues[0];
//            float pitch = orientationValues[1];
//            float roll = orientationValues[2];

            // Azimuth, pitch, and roll are given in radians. Here we convert them to degrees.
//            double azimuthDeg = orientationValues[0] * (180 / Math.PI);
//            double pitchDeg = orientationValues[1] * (180 / Math.PI);
//            double rollDeg = orientationValues[2] * (180 / Math.PI);
        }

        return orientationValues;
    }


    public float[] earthAccelorometer(float[] accelerometer, float[] magnetometer, float[] gravity) {
        float[] deviceRelativeAcceleration = new float[4];
        deviceRelativeAcceleration[0] = accelerometer[0];
        deviceRelativeAcceleration[1] = accelerometer[1];
        deviceRelativeAcceleration[2] = accelerometer[2];
        deviceRelativeAcceleration[3] = 0;

        // Change the device relative acceleration values to earth relative values
        // X axis -> East
        // Y axis -> North Pole
        // Z axis -> Sky

        float[] R = new float[16], I = new float[16], earthAcc = new float[16];

        SensorManager.getRotationMatrix(R, I, gravity, magnetometer);

        float[] inv = new float[16];

        android.opengl.Matrix.invertM(inv, 0, R, 0);
        android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, deviceRelativeAcceleration, 0);
        Log.d("Earth","x" + earthAcc[0] + " y " + earthAcc[1] + " z " + earthAcc[2]);

        return earthAcc;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Lock the orientation to portrait (for now)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mTextSensorPhoneAccX = (TextView) findViewById(R.id.phone_acc_x);
        mTextSensorPhoneAccY = (TextView) findViewById(R.id.phone_acc_y);
        mTextSensorPhoneAccZ = (TextView) findViewById(R.id.phone_acc_z);
        mTextSensorEarthAccX = (TextView) findViewById(R.id.earth_acc_x);
        mTextSensorEarthAccY = (TextView) findViewById(R.id.earth_acc_y);
        mTextSensorEarthAccZ = (TextView) findViewById(R.id.earth_acc_z);
        mTextSensorPhoneAzimuth = (TextView) findViewById(R.id.phone_azimuth);
        mTextSensorPhonePitch = (TextView) findViewById(R.id.phone_pitch);
        mTextSensorPhoneRoll = (TextView) findViewById(R.id.phone_roll);


        // Get accelerometer and magnetometer sensors from the sensor manager.
        // The getDefaultSensor() method returns null if the sensor
        // is not available on the device.
        mSensorManager = (SensorManager) getSystemService(
                Context.SENSOR_SERVICE);
        mSensorAccelerometer = mSensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);
        mSensorMagnetometer = mSensorManager.getDefaultSensor(
                Sensor.TYPE_MAGNETIC_FIELD);
        mSensorGravity = mSensorManager.getDefaultSensor(
                Sensor.TYPE_GRAVITY);
    }

    /**
     * Listeners for the sensors are registered in this callback so that
     * they can be unregistered in onStop().
     */
    @Override
    protected void onStart() {
        super.onStart();

        // Listeners for the sensors are registered in this callback and
        // can be unregistered in onStop().
        //
        // Check to ensure sensors are available before registering listeners.
        // Both listeners are registered with a "normal" amount of delay
        // (SENSOR_DELAY_NORMAL).
        if (mSensorAccelerometer != null) {
            mSensorManager.registerListener(this, mSensorAccelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mSensorMagnetometer != null) {
            mSensorManager.registerListener(this, mSensorMagnetometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mSensorManager != null) {
            mSensorManager.registerListener( this, mSensorGravity,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unregister all sensor listeners in this callback so they don't
        // continue to use resources when the app is stopped.
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                mAccelerometerData = sensorEvent.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mMagnetometerData = sensorEvent.values.clone();
                break;
            case Sensor.TYPE_GRAVITY:
                mGravityData = sensorEvent.values.clone();
                break;
            default:
                return;
        }

//        Log.i("Original", "x " + mAccelerometerData[0] + " y " + mAccelerometerData[1] + " z " + mAccelerometerData[2]);
        float[] earthAcc = earthAccelorometer(mAccelerometerData, mMagnetometerData, mGravityData);

        float[] phoneOrientationValuesRadians = phoneOrientation(mAccelerometerData, mMagnetometerData);

        double[] phoneOrientationValuesDegrees = radiansToDegrees(phoneOrientationValuesRadians);

        // TODO: Set the phone's orientation to a  view.

        // Display the phone's accelerometer data in the view.
        mTextSensorPhoneAccX.setText(getResources().getString(
                R.string.value_format, mAccelerometerData[0]));
        mTextSensorPhoneAccY.setText(getResources().getString(
                R.string.value_format, mAccelerometerData[1]));
        mTextSensorPhoneAccZ.setText(getResources().getString(
                R.string.value_format, mAccelerometerData[2]));

        // Display the phone's accelerometer data in earth's coordinate system.
        mTextSensorEarthAccX.setText(getResources().getString(
                R.string.value_format, earthAcc[0]));
        mTextSensorEarthAccY.setText(getResources().getString(
                R.string.value_format, earthAcc[1]));
        mTextSensorEarthAccZ.setText(getResources().getString(
                R.string.value_format, earthAcc[2]));

        // Log the phone orientation
//        Log.i("Orientation",
//                "azimuth " + phoneOrientationValuesDegrees[0] +
//                " pithch " + phoneOrientationValuesDegrees[1] +
//                " roll " + phoneOrientationValuesDegrees[2]);
        mTextSensorPhoneAzimuth.setText(getResources().getString(R.string.value_format, phoneOrientationValuesDegrees[0]));
        mTextSensorPhonePitch.setText(getResources().getString(R.string.value_format, phoneOrientationValuesDegrees[1]));
        mTextSensorPhoneRoll.setText(getResources().getString(R.string.value_format, phoneOrientationValuesDegrees[2]));

    }

    /**
     * Must be implemented to satisfy the SensorEventListener interface;
     * unused in this app.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {


    }
}