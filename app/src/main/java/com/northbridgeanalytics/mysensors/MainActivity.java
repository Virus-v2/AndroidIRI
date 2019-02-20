package com.northbridgeanalytics.mysensors;

// Sources
// https://code.tutsplus.com/tutorials/using-the-accelerometer-on-android--mobile-22125
// https://google-developer-training.gitbooks.io/android-developer-advanced-course-practicals/content/unit-1-expand-the-user-experience/lesson-3-sensors/3-2-p-working-with-sensor-based-orientation/3-2-p-working-with-sensor-based-orientation.html
// https://stackoverflow.com/questions/5464847/transforming-accelerometers-data-from-devices-coordinates-to-real-world-coordi
// https://stackoverflow.com/questions/23701546/android-get-accelerometers-on-earth-coordinate-system
// https://stackoverflow.com/questions/11578636/acceleration-from-devices-coordinate-system-into-absolute-coordinate-system

import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import utils.Dialogs;
import utils.VectorAlgebra;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity
        implements SensorEventListener, LocationListener {

    // Default tag for Log
    private static final String TAG ="MyMessage";

    // Callback code for GPS permissions.
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    FragmentManager fm = getSupportFragmentManager();

    // Very small values for the accelerometer (on all three axes) should be interpreted as 0. This value is the amount
    // of acceptable non-zero drift.
    private static final float VALUE_DRIFT = 0.05f;

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

    // System sensor manager instance.
    private SensorManager SensorManager;
    private LocationManager locationManager;

    // Accelerometer and magnetometer sensors, as retrieved from the
    // sensor manager.
    private Sensor SensorAccelerometer;
    private Sensor SensorMagnetometer;
    private Sensor SensorGravity;

    // Variables to hold current sensor values.
    private float[] AccelerometerData = new float[3];
    private float[] MagnetometerData = new float[3];
    private float[] GravityData = new float[3];

    // Variables to hold current location values.
    private double currentLatitude;
    private double currentLongitude;

    // Button to toggle GPS logging.
    private Button toggleRecordingButton;
    private boolean isToggleRecordingButtonClicked = false;

    // On click listener for toggle GPS logging.
    private View.OnClickListener toggleRecordingListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            // TODO: User needs visual feedback of the current state of the button.
            // TODO: Booleans need to be moved to end of function, can they be a return of the function?
            if (!isToggleRecordingButtonClicked) {
                toggleRecordingClickedOn();
            } else {
                toggleRecordingClickedOff();
            }
        }
    };


    //******************************************************************************************************************
    //                                            BEGIN APP METHODS
    //******************************************************************************************************************

    // Stop logging when the user turns off GPS.
    private void toggleRecordingClickedOff() {
        // Turns off updates from LocationListener.
        locationManager.removeUpdates(this);
        isToggleRecordingButtonClicked = false;
    }


    // The user has turned on GPS logging.
    private void toggleRecordingClickedOn() {

        // Check if we have permission to use the GPS and request it if we don't.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
          != PackageManager.PERMISSION_GRANTED) {

            // Uh-oh we don't have permissions, better ask.
            ActivityCompat.requestPermissions(MainActivity.this,
              new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
              MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                // MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION is an integer constant that we will use to lookup the
                // result of this request in the onRequestPermissionsResult() callback.

        } else {
            // We already have permission, so let's enable the GPS.
            enableGPS();

        }
    }


    // After we get permission, enable the GPS.
    private void enableGPS() {
        // Lets see if the user has GPS enabled.
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            // We have permission, but the GPS isn't enabled, send the user to the settings screen.
            // TODO: In some cases, a matching Activity may not exist, so ensure we have a safeguard against this.
//            Intent onGPS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//            startActivity(onGPS);

            Dialogs gpsSettings = new Dialogs();
            gpsSettings.show(fm, "Alert Dialog");



            // The GPS was not enabled from the button press, so let's change it to false.
            isToggleRecordingButtonClicked = false;

        } else {

            // We have permission and GPS is enabled, let's start logging.
            // Register the listener with the Location Manager to receive location updates from the GPS only. The second
            // parameter controls minimum time interval between notifications and the third is the minimum change in
            // distance between notifications - setting both to zero requests location notifications as frequently as
            // possible.
            locationManager.requestLocationUpdates(
              LocationManager.GPS_PROVIDER, 0, 0, this);

            // Successfully started logging the GPS, set the button as clicked.
            isToggleRecordingButtonClicked = true;
        }
    }

    //******************************************************************************************************************
    //                                            BEGIN ANDROID CALLBACKS
    //******************************************************************************************************************


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Lock the orientation to portrait (for now)
        // TODO: Support screen rotation?
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Get the start recording button view.
        toggleRecordingButton = findViewById(R.id.toggleRecording);

        // Set the onClick listener for the start recording button view.
        toggleRecordingButton.setOnClickListener(toggleRecordingListener);

        // Get the TextViews that will show the sensor values.
        TextSensorPhoneAccX = (TextView) findViewById(R.id.phone_acc_x);
        TextSensorPhoneAccY = (TextView) findViewById(R.id.phone_acc_y);
        TextSensorPhoneAccZ = (TextView) findViewById(R.id.phone_acc_z);
        TextSensorEarthAccX = (TextView) findViewById(R.id.earth_acc_x);
        TextSensorEarthAccY = (TextView) findViewById(R.id.earth_acc_y);
        TextSensorEarthAccZ = (TextView) findViewById(R.id.earth_acc_z);
        TextSensorPhoneAzimuth = (TextView) findViewById(R.id.phone_azimuth);
        TextSensorPhonePitch = (TextView) findViewById(R.id.phone_pitch);
        TextSensorPhoneRoll = (TextView) findViewById(R.id.phone_roll);


        // Get accelerometer and magnetometer sensors from the sensor manager. The getDefaultSensor() method returns
        // null if the sensor is not available on the device.
        SensorManager = (SensorManager) getSystemService(
                Context.SENSOR_SERVICE);
        SensorAccelerometer = SensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);
        SensorMagnetometer = SensorManager.getDefaultSensor(
                Sensor.TYPE_MAGNETIC_FIELD);
        SensorGravity = SensorManager.getDefaultSensor(
                Sensor.TYPE_GRAVITY);

        // Get the LocationManager.
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
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unregister all sensor listeners in this callback so they don't
        // continue to use resources when the app is stopped.
        SensorManager.unregisterListener(this);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // We asked the user for permission and they said yes.
                    enableGPS();

                } else {
                    // Darn, they said no.

                    // Since nothing resulted from the button press, lets make it false.
                    isToggleRecordingButtonClicked = false;

                    // TODO: Something needs to happen if they deny permissions.
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
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
        float[] earthAcc = VectorAlgebra.earthAccelerometer(
          AccelerometerData, MagnetometerData,
          GravityData, SensorManager);

        // Get the phone's orientation - given in radians.
        float[] phoneOrientationValuesRadians = VectorAlgebra.phoneOrientation(
          AccelerometerData, MagnetometerData, SensorManager);

        // Phone's orientation is given in radians, lets convert that to degrees.
        double[] phoneOrientationValuesDegrees = VectorAlgebra.radiansToDegrees(phoneOrientationValuesRadians);


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
        // TODO: Add message that the app won't work with GPS disabled, then prompt to turn it on.

        // We have permission, but the GPS is disabled. Lets prompt the user to turn it on.
        enableGPS();

    }

    /**
     * Must be implemented to satisfy the SensorEventListener interface;
     * unused in this app.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {


    }
}