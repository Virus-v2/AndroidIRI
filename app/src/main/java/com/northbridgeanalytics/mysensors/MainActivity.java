package com.northbridgeanalytics.mysensors;

// Sources
// https://code.tutsplus.com/tutorials/using-the-accelerometer-on-android--mobile-22125
// https://google-developer-training.gitbooks.io/android-developer-advanced-course-practicals/content/unit-1-expand-the-user-experience/lesson-3-sensors/3-2-p-working-with-sensor-based-orientation/3-2-p-working-with-sensor-based-orientation.html
// https://stackoverflow.com/questions/5464847/transforming-accelerometers-data-from-devices-coordinates-to-real-world-coordi
// https://stackoverflow.com/questions/23701546/android-get-accelerometers-on-earth-coordinate-system
// https://stackoverflow.com/questions/11578636/acceleration-from-devices-coordinate-system-into-absolute-coordinate-system

import SurfaceDoctor.SegmentHandler;
import SurfaceDoctor.SurfaceDoctorEvent;
import SurfaceDoctor.SurfaceDoctorInterface;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import myAlerts.AlertDialogGPS;
import SurfaceDoctor.VectorAlgebra;
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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;

// TODO: https://blog.fossasia.org/comparing-different-graph-view-libraries-and-integrating-them-in-pslab-android-application/


public class MainActivity extends AppCompatActivity
        implements SensorEventListener, LocationListener, SurfaceDoctorInterface {

    // Callback code for GPS permissions.
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private FragmentManager fm = getSupportFragmentManager();

    private float[] adjustedGravity = new float[3];
    private float[] linear_acceleration = new float[3];

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
    private SegmentHandler segmentHandler;

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
    //                                            BEGIN ACTIVITY LIFECYCLE
    //******************************************************************************************************************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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

        Log.i("Activity", "OnCreate has fired");
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
        // TODO: Need a dialog saying sensors aren't available.
        if (SensorAccelerometer != null) {
            SensorManager.registerListener(this, SensorAccelerometer,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        if (SensorMagnetometer != null) {
            SensorManager.registerListener(this, SensorMagnetometer,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        if (SensorManager != null) {
            SensorManager.registerListener(this, SensorGravity,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }

        Log.i("Activity", "OnStart has fired");
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i("Activity", "onResume has fired");

    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.i("Activity", "onPause has fired");
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unregister all sensor listeners in this callback so they don't
        // continue to use resources when the app is stopped.
        SensorManager.unregisterListener(this);

        Log.i("Activity", "OnStop has fired");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.i("Activity", "onDestroy has fired");
    }


    //******************************************************************************************************************
    //                                            PERMISSIONS AND SETTINGS
    //******************************************************************************************************************

    // Stop logging when the user turns off GPS.
    private void toggleRecordingClickedOff() {
        // Turns off updates from LocationListener.
        locationManager.removeUpdates(this);
        isToggleRecordingButtonClicked = false;

        // The user no longer wants to record IRI, so let's delete it.
        segmentHandler = null;

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


    /**
     * Android callback for response to permissions requests.
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
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


    // After we get permission, enable the GPS.
    private void enableGPS() {
        // Lets see if the user has GPS enabled.
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            // We have permission, but the GPS isn't enabled, ask the user if they would like to go to their location
            // settings.
            AlertDialogGPS gpsSettings = new AlertDialogGPS();
            gpsSettings.show(fm, "Alert Dialog");

            // The GPS was not enabled from the button press, so let's make sure it's still false.
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

            // We're ready to start logging, let's create a new SegmentHandler object.
            segmentHandler = new SegmentHandler(this, SensorManager);
            segmentHandler.setSomeEventListener(this);
        }
    }


    //******************************************************************************************************************
    //                                                BEGIN APP BAR
    //******************************************************************************************************************

    /**
     * Adds entries to the action bar.
     *
     * Adds all the entries, such as settings, to the action bar dropdown.
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_toolbar_menu, menu);
        return true;
    }

    /**
     * App bar items callback.
     *
     * This method is called when the user selects one of the app bar items, and passes a MenuItem object to indicate
     * which item was clicked. The ID returned from MenutItem.getItemId() matches the id you declared for the app bar
     * item in res/menu/<-menu.xml->
     *
     * @param item MenuItem callback object to indicate which item was clicked. Use MenuItem.getItemId() to get value.
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the Settings item, show the app settings UI.

                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.preferenceFragment, new SettingsFragment())
                        .addToBackStack(null)
                        .commit();

                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }



    // TODO: What is this? This is how you get the preferences that were in the settings fragment somehow.
    private void getLoggingSettings() {

        SharedPreferences settings = getDefaultSharedPreferences(this);

        // Get settings settings about file type.
        boolean isEsriJASON = settings.getBoolean("preference_filename_json", false);
        String loggingFilePrefix = settings.getString("preference_filename_prefix", "androidIRI");

        // Get settings about logging variables.
        boolean loggingUnits = settings.getBoolean("preference_logging_units", true);
        int maxLoggingDistance = Integer.parseInt(
                settings.getString("preference_logging_distance", "1000"));
        int maxLoggingSpeed = Integer.parseInt(
                settings.getString("preference_logging_max_Speed", "80"));
        int minLoggingSpeed = Integer.parseInt(
                settings.getString("preference_logging_min_speed", "20"));

    }

    //******************************************************************************************************************
    //                                            BEGIN SENSOR CALLBACKS
    //******************************************************************************************************************

    //*********************************************   Accelerometer  ***************************************************

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:

                // TODO: Data should be removed from this method ASAP.
                AccelerometerData = sensorEvent.values.clone();

                float alpha = 0.8f;

                // Low-pass filter for isolating gravity.
                adjustedGravity[0] = alpha * GravityData[0] + (1 - alpha) *  AccelerometerData[0];
                adjustedGravity[1] = alpha * GravityData[1] + (1 - alpha) *  AccelerometerData[1];
                adjustedGravity[2] = alpha * GravityData[2] + (1 - alpha) *  AccelerometerData[2];

                // High-pass filter for removing gravity.
                linear_acceleration[0] =  AccelerometerData[0] - adjustedGravity[0];
                linear_acceleration[1] =  AccelerometerData[1] - adjustedGravity[1];
                linear_acceleration[2] =  AccelerometerData[2] - adjustedGravity[2];


                // The segmentHandler object is created in the enableGPS() method when the user presses the start logging
                // button. If the segmentHandler object exists, it means we have location permissions, the GPS is
                // enabled, and we need to pass the accelerometer SensorEvent to the SegmentHandler.
                if (segmentHandler != null) {
                    segmentHandler.setSurfaceDoctorAccelerometer(sensorEvent);
                }
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                MagnetometerData = sensorEvent.values.clone();

                if ( segmentHandler != null ) {
                    segmentHandler.setSurfaceDoctorMagnetometer(sensorEvent);
                }
                break;
            case Sensor.TYPE_GRAVITY:
                GravityData = sensorEvent.values.clone();

                if ( segmentHandler != null ) {
                    segmentHandler.setSurfaceDoctorGravity(sensorEvent);
                }

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
                linear_acceleration, MagnetometerData,
                GravityData, SensorManager);

        // TODO: We also need acceleromter data in user coordinate systmer where y is straight ahead. 

        // Get the phone's orientation - given in radians.
        float[] phoneOrientationValuesRadians = VectorAlgebra.phoneOrientation(
                AccelerometerData, MagnetometerData, SensorManager);

        // Phone's orientation is given in radians, lets convert that to degrees.
        double[] phoneOrientationValuesDegrees = VectorAlgebra.radiansToDegrees(phoneOrientationValuesRadians);


        // Display the phone's accelerometer data in the view.
        TextSensorPhoneAccX.setText(getResources().getString(
                R.string.value_format, linear_acceleration[0]));
        TextSensorPhoneAccY.setText(getResources().getString(
                R.string.value_format, linear_acceleration[1]));
        TextSensorPhoneAccZ.setText(getResources().getString(
                R.string.value_format, linear_acceleration[2]));

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


    /**
     * Android Callback for Accelerometer Accuracy Change.
     * <p>
     * Must be implemented to satisfy the SensorEventListener interface;
     * unused in this app.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {


    }


    //*********************************************   location   *******************************************************

    /**
     * Android Callback for GPS Location
     *
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {

        // The segmentHandler object is created in the enableGPS() method when the user presses the start logging
        // button. If the segmentHandler object exists, it means we have location permissions, the GPS is
        // enabled, and we need to pass the GPS Location to the SegmentHandler.
        if (segmentHandler != null) {
            segmentHandler.setSurfaceDoctorLocation(location);
        }
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


    //******************************************************************************************************************
    //                                            Surface Doctor
    //******************************************************************************************************************


    /**
     * Event from SegmentHandler.
     *
     * @param surfaceDoctorEvent
     */
    @Override
    public void onSurfaceDoctorEvent(SurfaceDoctorEvent surfaceDoctorEvent) {
        String surfaceDoctorEventType = surfaceDoctorEvent.getType();

        switch (surfaceDoctorEventType) {
            case "TYPE_SEGMENT_IRI":
                TextView x = findViewById(R.id.last_IRI_x);
                TextView y = findViewById(R.id.last_IRI_y);
                TextView z = findViewById(R.id.last_IRI_z);

                x.setText(Double.toString(surfaceDoctorEvent.x));
                y.setText(Double.toString(surfaceDoctorEvent.y));
                z.setText(Double.toString(surfaceDoctorEvent.z));
        }
    }

}