package SurfaceDoctor;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SegmentHandler {

    // Default user input parameters.
    private boolean units = true;
    private int maxDistance = 100;
    private int maxSpeed = 80;
    private int minSpeed = 20;

    private Context context;
    private SensorManager sensorManager;

    private long accelerometerStartTime = 0;
    private long accelerometerStopTime = 0;
    private List<SurfaceDoctorPoint> surfaceDoctorPoints = new ArrayList<SurfaceDoctorPoint>();

    private float[] gravity = new float[3];
    private float[] magnetometer = new float[3];

    private float alpha = 0.8f;

    private boolean hasLocationPairs = false;
    private Location endPoint;
    private ArrayList<String[]> segmentCoordinates = new ArrayList<>();

    private float totalAccumulatedDistance = 0.0f;

    // Required to create an Event.
    private SurfaceDoctorInterface listener;
    public void setSomeEventListener (SurfaceDoctorInterface listener) {
        this.listener = listener;
    }

    /**
     * Constructors
     *
     * @param context
     */
    public SegmentHandler(Context context, SensorManager sm) {

        this.context = context;
        sensorManager = sm;
    }


    /**
     * Method for setting SurfaceDoctor settings that were defined by the user.
     *
     * @param inputUnits
     * @param inputSegmentDistance
     * @param inputMaxSpeed
     * @param inputMinSpeed
     */
    public void setSurfaceDoctorPreferences(boolean inputUnits, int inputSegmentDistance,
                                             int inputMaxSpeed, int inputMinSpeed) {
        // TODO: This needs implementing.
        units = inputUnits;
        maxDistance = inputSegmentDistance;
        maxSpeed = inputMaxSpeed;
        minSpeed = inputMinSpeed;
    }


    /**
     * Method for receiving accelerometer data from sensors.
     *
     * @param sensorEvent
     */
    public void setSurfaceDoctorAccelerometer(SensorEvent sensorEvent) {

        // We need time between accelerometer events to calculate the IRI.
        // TODO: Verify what time this is.
        accelerometerStartTime = accelerometerStopTime;
        accelerometerStopTime = sensorEvent.timestamp;

        // Check if we have location pairs and start & stop times. If so, we start collecting SurfaceDoctorPoint objects.
        // hasLocationPairs is set to true by the setSurfaceDoctorLocation method when to location event's have been
        // recorded.
        if ( hasLocationPairs && accelerometerStartTime > 0 ) {

            float[] inputAccelerometer = sensorEvent.values.clone();

            // Low-pass filter for isolating gravity.
            float[] adjustedGravity = new float[3];

            adjustedGravity[0] = alpha * gravity[0] + (1 - alpha) * inputAccelerometer[0];
            adjustedGravity[1] = alpha * gravity[1] + (1 - alpha) * inputAccelerometer[1];
            adjustedGravity[2] = alpha * gravity[2] + (1 - alpha) * inputAccelerometer[2];

            // High-pass filter for removing gravity.
            float[] linearAcceleration = new float[3];

            linearAccelerationPhone[0] = inputAccelerometer[0] - adjustedGravity[0];
            linearAccelerationPhone[1] = inputAccelerometer[1] - adjustedGravity[1];
            linearAccelerationPhone[2] = inputAccelerometer[2] - adjustedGravity[2];

            // Convert Accelerometer from phone coordinate system to earth coordinate system.
            float[] linearAccelerationEarth = VectorAlgebra.earthAccelerometer(
                    linearAccelerationPhone, magnetometer,
                    gravity, sensorManager);

            // For each measurement of the accelerometer, create a SurfaceDoctorPoint object.
            SurfaceDoctorPoint surfaceDoctorPoint = new SurfaceDoctorPoint(
                    linearAccelerationPhone,
                    accelerometerStartTime,
                    accelerometerStopTime);
            surfaceDoctorPoints.add(surfaceDoctorPoint);

//            Log.i("POINT", "X: " + surfaceDoctorPoint.getVertDissX() +
//                    " Y: " + surfaceDoctorPoint.getVertDissY() +
//                    " Z: " + surfaceDoctorPoint.getVertDissZ());
        }
    }


    /**
     * Receives Gravity from Android Sensor Callback
     *
     * @param sensorEvent
     */
    public void setSurfaceDoctorGravity(SensorEvent sensorEvent ) {
        gravity = sensorEvent.values.clone();
    }

    public void setSurfaceDoctorMagnetometer(SensorEvent sensorEvent) { magnetometer = sensorEvent.values.clone(); }


    /** Method for receiving Location data from the GPS sensor.
     *
     * @param inputLocation
     */
    public void setSurfaceDoctorLocation(Location inputLocation) {

        // If we have a pair of location objects, let's run through our logic.
        if (endPoint != null) {

            // Let's first tell our accelerometer sensors to start summing data.
            hasLocationPairs = true;

            // Now let's make the current point the old point, and update the current point with the new point. We'll
            // use these location pairs to extract data later.
            Location startPoint = endPoint;
            endPoint = inputLocation;

            // We're logging, let's process the data.
            executeSurfaceDoctor(startPoint, endPoint );

            // TODO: We'll need to create an event that lets MainActivity know if we're within speed, logging, etc. This could also be handled on MainActivity side.
            double lineBearing = inputLocation.getBearing();

            // This creates an event with the sensor values, that will fire in the MainActivity. It was used for learning
            // purposes and currently does nothing.
//            if (listener != null) {
//                SurfaceDoctorEvent e = new SurfaceDoctorEvent();
//                e.type = "TYPE_LOCATION";
//                e.speed = lineSpeed;
//                e.heading = lineBearing;
//
//                listener.onSurfaceDoctorEvent(e);
//            }

        } else {
            // This is our first point, our logic depends on a comparison of two location objects, so let's do nothing
            // until we get that second location.
            endPoint = inputLocation;
        }
    }


    /**
     *  This is the main logic handler of the SegmentHandler.
     *
     *  This is fired at every GPS location callback from Android.
     */
    private void executeSurfaceDoctor(Location locationStart, Location locationEnd) {

        // The approximate distance in meters.
        float lineDistance = locationStart.distanceTo(locationEnd);

        // The speed in m/s
        float lineSpeed = locationEnd.getSpeed();

        // Long / Lat of the Location pairs.
        DecimalFormat coordinatesFormat = new DecimalFormat("#.######");
        String[] coordinatesStart = new String[]{
                coordinatesFormat.format(locationStart.getLongitude()),
                coordinatesFormat.format(locationStart.getLatitude())};
        String[] coordinatesLast = new String[]{
                coordinatesFormat.format(locationEnd.getLongitude()),
                coordinatesFormat.format(locationEnd.getLatitude())};

        // We're within speed and haven't reached the end of a segment, let's add the distance between the coordinate
        // pairs to the total distance of the segment.
        if ( isWithinSpeed( lineSpeed ) && !isSegmentEnd() ) {
            // Append the distance between the coordinate points to the total distance.
            totalAccumulatedDistance += lineDistance;
            // Append the new coordinates to the ArrayList so we can create a polyline later.
            segmentCoordinates.add(coordinatesStart);

            Log.i("IRI", "Distance: " + totalAccumulatedDistance);
        }
        // We're withing speed and reached the end of a segment, let's finalize the segment.
        else if ( isWithinSpeed( lineSpeed ) && isSegmentEnd() ) {
            // Append the distance between the coordinate points to the total distance.
            totalAccumulatedDistance += lineDistance;
            // Append the new coordinates to the ArrayList so we can create a polyline later.
            segmentCoordinates.add(coordinatesStart);
            // The segement is done, add the last coordinate.
            segmentCoordinates.add(coordinatesLast);

            // This is the end of a segment, let's send it off to be finalized.
            finalizeSegment(totalAccumulatedDistance, segmentCoordinates, surfaceDoctorPoints);

            // Let's reset for the next segment.
            // TODO: Is this safe here?
            resetSegment(false);
        }
        // We've exceeded our speed threshold, we need to do a hard reset.
        // TODO: Would if we lose coordinate pairs or accelerometer data. 
        else if ( !isWithinSpeed( lineSpeed )) {
            // The hard rest will clear out our location pairs, as well.
            resetSegment(true );
        }
        else {
            Log.i("SEG", "A condition was met that we didn't think about. ");
        }
    }


    /**
     *  Finalizes a road segment
     *
     *  Executes when the segment distance threshold has been met.      *
     */
    private void finalizeSegment(double distance, ArrayList<String[]> polyline, List<SurfaceDoctorPoint> measurements) {

        double[] totalVerticalDisplacement = new double[3];

        // First get the total vertical displacement of the segment.
        for (int i = 0; i < measurements.size(); i++ ) {
            int previousIndex = i - 1;

            // Vertical Displacements equals the absolute value of the current longitudinal offset minus the previous
            // longitudinal offset.
            if ( previousIndex >= 0 ) {

                totalVerticalDisplacement[0] += Math.abs( measurements.get(i).getVertDissX() - measurements.get(previousIndex).getVertDissX() );
                totalVerticalDisplacement[1] += Math.abs( measurements.get(i).getVertDissY() - measurements.get(previousIndex).getVertDissY() );
                totalVerticalDisplacement[2] += Math.abs( measurements.get(i).getVertDissZ() - measurements.get(previousIndex).getVertDissZ() );
            }
        }

        // IRI(mm/m) = (total vertical displacement * 1000) / segment distance.
        // TODO: Need to allow the user to output IRI in mm/m or m/km.
        double totalIRIofX = (totalVerticalDisplacement[0] * 1000) / distance;
        double totalIRIofY = (totalVerticalDisplacement[1] * 1000) / distance;
        double totalIRIofZ = (totalVerticalDisplacement[2] * 1000) / distance;

        Log.i("IRI", "X " + totalIRIofX + " Y " + totalIRIofY + " Z " + totalIRIofZ);

        // Let's pass the segment data to the SurfaceDoctorEvent so it can be used in the main activity.
        // The SurfaceDoctorEvent will be triggered in the MainActivity.
        if (listener != null) {
            SurfaceDoctorEvent e = new SurfaceDoctorEvent();
            e.type = "TYPE_SEGMENT_IRI";
            e.x = totalIRIofX;
            e.y = totalIRIofY;
            e.z = totalIRIofZ;
            e.distance = distance;
            // TODO: Assign output to SurfaceDoctorEvent class.
            listener.onSurfaceDoctorEvent(e);
        }

        saveResults(distance, totalIRIofX, totalIRIofY, totalIRIofZ, polyline);
    }


    private void saveResults(double distance, double IRIofX, double IRIofY, double IRIofZ, ArrayList<String[]> polyline ) {
        // TODO: Instead of one file per segment, append multiple segments to one file.

        // Check if we have access to external storage?
        if ( isExternalStorageWritable() ) {
            // TODO: Save file as geoJSON or EsriJSON.

            // Add results to GeoJSON.

            // Convert the ArrayList to a string array.
            String[][] polylineArray = new String[polyline.size()][polyline.size()];
            polylineArray = polyline.toArray(polylineArray);

            // Create geoJASON string.
            // Tried using JASONobject and JASONArray, but couldn't git rid of quotes around coordinates. Didn't have
            // access to GSON library due to network permissions.
            String output = "{\"type\": \"FeatureCollection\", \"features\": [ { \"type\": \"Feature\", \"geometry\":" +
                    "{ \"type\": \"LineString\", \"coordinates\":" + Arrays.deepToString(polylineArray) + "}," +
                    "\"properties\": { \"DISTANCE\":" + distance + ", \"IRIofX\":" + IRIofX + ", \"IRIofY\":" + IRIofY +
                    ", \"IRIofZ\":" + IRIofZ + "}}]}";

            // Let's save the geoJASON string.
            byte[] outputBytes = output.getBytes();
            File file = getPrivateStorageDirectory(context, String.valueOf(accelerometerStartTime) + ".geojson");
            try {
                FileOutputStream fos = new FileOutputStream(file);
                try {
                    fos.write(outputBytes);
                    fos.close();
                } catch (IOException e) {
                    Log.e("ERROR", "IO Exception");
                }
            } catch (FileNotFoundException e) {
                Log.e("ERROR", "File not found");
            }

        } else {
            // TODO: If no access external storage, let's save to internal until we do get access.
        }
    }


    private boolean isWithinSpeed( float inputSpeed ) {
        // TODO: Need to handle which units the user selected.
        float mph = inputSpeed * 2.23694f;
        return minSpeed <= mph && mph <= maxSpeed;
    }


    private boolean isSegmentEnd() {
        // TODO: Need to handle user's input units.
        return totalAccumulatedDistance >= maxDistance;
    }


    /**
     * Resets all the parameters.
     *
     * Used to restart the logging of a road segment.
     *
     * @param hardReset boolean
     */
    private void resetSegment(boolean hardReset) {

        // Reset the segment distance to zero.
        totalAccumulatedDistance = 0;

        // Clear all accelerometer measurements from the ArrayList.
        surfaceDoctorPoints.clear();

        // Clear the list of coordinates that make the polyline. 
        segmentCoordinates.clear();

        // Hard resets are used when a sensor loses connectivity or passes a threshold.
        if ( hardReset ) {
            hasLocationPairs = false;
            endPoint = null;
            accelerometerStopTime = 0;
        }
    }


    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /** Creates an empty file that can be written to.
     *
     * @param context
     * @param fileName
     * @return
     */
    private File getPrivateStorageDirectory(Context context, String fileName) {
        File file = new File(context.getExternalFilesDir("geoJson"), fileName);
        return file;
    }

}
