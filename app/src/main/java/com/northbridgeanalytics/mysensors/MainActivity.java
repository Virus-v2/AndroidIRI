package com.northbridgeanalytics.mysensors;

// Sources
// https://code.tutsplus.com/tutorials/using-the-accelerometer-on-android--mobile-22125
// https://google-developer-training.gitbooks.io/android-developer-advanced-course-practicals/content/unit-1-expand-the-user-experience/lesson-3-sensors/3-2-p-working-with-sensor-based-orientation/3-2-p-working-with-sensor-based-orientation.html
// https://stackoverflow.com/questions/5464847/transforming-accelerometers-data-from-devices-coordinates-to-real-world-coordi
// https://stackoverflow.com/questions/23701546/android-get-accelerometers-on-earth-coordinate-system
// https://stackoverflow.com/questions/11578636/acceleration-from-devices-coordinate-system-into-absolute-coordinate-system

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity
        implements SensorEventListener, LocationListener {

    public int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    // Default tag for Log
    public static final String TAG ="MyMessage";

    // System sensor manager instance.
    private SensorManager SensorManager;
    private LocationManager locationManager;

    // Accelerometer and magnetometer sensors, as retrieved from the
    // sensor manager.
    private Sensor SensorAccelerometer;
    private Sensor SensorMagnetometer;
    private Sensor SensorGravity;

    // TextViews to display current sensor values.
    private TextView TextSensorPhoneAccX;
    private TextView TextSensorPhoneAccY;
    private TextView TextSensorPhoneAccZ;

    private TextView TextSensorEarthAccX;
    private TextView TextSensorEarthAccY;
    private TextView TextSensorEarthAccZ;

    private TextView TextSensorPhoneAzimuth;
    private TextView TextSensorPhonePitch;
    private TextView TextSensorPhoneRoll;

    // Very small values for the accelerometer (on all three axes) should be interpreted as 0. This value is the amount
    // of acceptable non-zero drift.
    private static final float VALUE_DRIFT = 0.05f;

    // Variables to hold current sensor values.
    private float[] AccelerometerData = new float[3];
    private float[] MagnetometerData = new float[3];
    private float[] GravityData = new float[3];

    // Variables to hold current location values.
    private double currentLatitude;
    private double currentLongitude;


    //******************************************************************************************************************
    //                                            BEGIN METHODS
    //******************************************************************************************************************

    public double[] radiansToDegrees(float[] inputRadians) {

        double[] outputDegrees = new double[inputRadians.length];

        for (int i=0; i < inputRadians.length; i++) {
            outputDegrees[i] = inputRadians[i] * (180/Math.PI);
        }

        return outputDegrees;
    }


    public float[] phoneOrientation(float[] accelerometer, float[] magnetometer) {

        // Empty Float array to hold the rotation matrix.
        float[] rotationMatrix = new float[9];
        // Empty Float array to hold the azimuth, pitch, and roll.
        float orientationValues[] = new float[3];

        // Not sure exactly how this works, but populates the matrix with the input data. rotationOK returns true if the
        // .getRotationMatrix method is successful.
        // "You can transform any vector from the phone's coordinate system to the Earth's coordinate system by
        // multiplying it with the rotation matrix."
        boolean rotationOK = SensorManager.getRotationMatrix(rotationMatrix,
                null, accelerometer, magnetometer);

        // If the getRotationMatrix method is successful run the following code,
        // TODO Do I need this at all?.
        if (rotationOK) {

            SensorManager.getOrientation(rotationMatrix, orientationValues);

        }

        return orientationValues;
    }


    public float[] earthAccelerometer(float[] accelerometer, float[] magnetometer, float[] gravity) {
        float[] phoneAcceleration = new float[4];
        phoneAcceleration[0] = accelerometer[0];
        phoneAcceleration[1] = accelerometer[1];
        phoneAcceleration[2] = accelerometer[2];
        phoneAcceleration[3] = 0;

        // Change the device relative acceleration values to earth relative values
        // X axis -> East
        // Y axis -> North Pole
        // Z axis -> Sky

        float[] R = new float[16], I = new float[16], earthAcceleration = new float[16];

        SensorManager.getRotationMatrix(R, I, gravity, magnetometer);

        float[] inv = new float[16];

        android.opengl.Matrix.invertM(inv, 0, R, 0);
        android.opengl.Matrix.multiplyMV(earthAcceleration, 0, inv, 0, phoneAcceleration, 0);

        return earthAcceleration;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Lock the orientation to portrait (for now)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        TextSensorPhoneAccX = (TextView) findViewById(R.id.phone_acc_x);
        TextSensorPhoneAccY = (TextView) findViewById(R.id.phone_acc_y);
        TextSensorPhoneAccZ = (TextView) findViewById(R.id.phone_acc_z);
        TextSensorEarthAccX = (TextView) findViewById(R.id.earth_acc_x);
        TextSensorEarthAccY = (TextView) findViewById(R.id.earth_acc_y);
        TextSensorEarthAccZ = (TextView) findViewById(R.id.earth_acc_z);
        TextSensorPhoneAzimuth = (TextView) findViewById(R.id.phone_azimuth);
        TextSensorPhonePitch = (TextView) findViewById(R.id.phone_pitch);
        TextSensorPhoneRoll = (TextView) findViewById(R.id.phone_roll);


        // Get accelerometer and magnetometer sensors from the sensor manager.
        // The getDefaultSensor() method returns null if the sensor
        // is not available on the device.
        SensorManager = (SensorManager) getSystemService(
                Context.SENSOR_SERVICE);
        SensorAccelerometer = SensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);
        SensorMagnetometer = SensorManager.getDefaultSensor(
                Sensor.TYPE_MAGNETIC_FIELD);
        SensorGravity = SensorManager.getDefaultSensor(
                Sensor.TYPE_GRAVITY);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
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
        if (SensorAccelerometer != null) {
            SensorManager.registerListener(this, SensorAccelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (SensorMagnetometer != null) {
            SensorManager.registerListener(this, SensorMagnetometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (SensorManager != null) {
            SensorManager.registerListener( this, SensorGravity,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (locationManager != null) {
            // Register the listener with the Location Manager to receive location updates from the GPS only. The second
            // parameter controls minimum time interval between notifications and the third is the minimum change in
            // distance between notifications - setting both to zero requests location notifications as frequently as
            // possible.
            // See the following link for LocationManager methods:
            // https://developer.android.com/reference/android/location/LocationManager.html#removeUpdates(android.location.LocationListener)
            // TODO: Check for permission and ask the user for permissions if necessary.
            // TODO: Check if GPS enabled first?
            // TODO: Disable the listener if no GPS is detected?
            // TODO: Move this to

            if (ContextCompat.checkSelfPermission(this,
              Manifest.permission.ACCESS_FINE_LOCATION)
              != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                  Manifest.permission.ACCESS_FINE_LOCATION)) {}
                else {
                   ActivityCompat.requestPermissions(this,
                     new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                     MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                }

            } else {

                locationManager.requestLocationUpdates(
                  LocationManager.GPS_PROVIDER, 0, 0, this);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unregister all sensor listeners in this callback so they don't
        // continue to use resources when the app is stopped.
        SensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                AccelerometerData = sensorEvent.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                MagnetometerData = sensorEvent.values.clone();
                break;
            case Sensor.TYPE_GRAVITY:
                GravityData = sensorEvent.values.clone();
                break;
            default:
                return;
        }

        // Get the phone's accelerometer values in earth's coordinate system.
        //
        // X = East / West
        // Y = North / South
        // Z = Up / Down
        float[] earthAcc = earthAccelerometer(AccelerometerData, MagnetometerData, GravityData);

        // Get the phone's orientation - given in radians.
        float[] phoneOrientationValuesRadians = phoneOrientation(AccelerometerData, MagnetometerData);

        // Convert radians to degrees.
        double[] phoneOrientationValuesDegrees = radiansToDegrees(phoneOrientationValuesRadians);


        // Display the phone's accelerometer data in the view.
        TextSensorPhoneAccX.setText(getResources().getString(
                R.string.value_format, AccelerometerData[0]));
        TextSensorPhoneAccY.setText(getResources().getString(
                R.string.value_format, AccelerometerData[1]));
        TextSensorPhoneAccZ.setText(getResources().getString(
                R.string.value_format, AccelerometerData[2]));

        // Display the phone's accelerometer data in earth's coordinate system.
        TextSensorEarthAccX.setText(getResources().getString(
                R.string.value_format, earthAcc[0]));
        TextSensorEarthAccY.setText(getResources().getString(
                R.string.value_format, earthAcc[1]));
        TextSensorEarthAccZ.setText(getResources().getString(
                R.string.value_format, earthAcc[2]));

        // Display the phone's orientation data in the view.
        TextSensorPhoneAzimuth.setText(getResources().getString(
                R.string.value_format, phoneOrientationValuesDegrees[0]));
        TextSensorPhonePitch.setText(getResources().getString(
                R.string.value_format, phoneOrientationValuesDegrees[1]));
        TextSensorPhoneRoll.setText(getResources().getString(
                R.string.value_format, phoneOrientationValuesDegrees[2]));

    }


    // Called when the location has changed.
    @Override
    public void onLocationChanged(Location location) {
        // See link for a list of methods for the location object:
        // https://developer.android.com/reference/android/location/Location.html#getLatitude()

        // All locations generated by the LocationManager are guaranteed to have valid lat, long, and timestamp.
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();

        Log.i("Location", "Lat: " + currentLatitude + "Long: " + currentLongitude);
    }

    // Called when the provider status changes. This method is called when a provider is unable to fetch a location
    // or if the provider has recently become available after a period of unavailability.
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.i("Location", "onSatusChanged fired");


    }

    // Called when the provider is enabled by the user
    @Override
    public void onProviderEnabled(String provider) {
        // TODO: Remove message that the app won't work with the GPS disabled.
        Log.i("Location", "onProviderEnabled fired");
    }

    // Called when the prover is disabled by the user. If requestLocationUpdates is called on an already disabled
    // provider, this method is called immediately.
    @Override
    public void onProviderDisabled(String provider) {
        // TODO: Add message that the app won't work with GPS disabled.
        Log.i("Location", "onProviderDisabled fired");

    }


    /**
     * Must be implemented to satisfy the SensorEventListener interface;
     * unused in this app.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {


    }
}