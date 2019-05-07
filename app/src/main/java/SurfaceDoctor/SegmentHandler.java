package SurfaceDoctor;

import android.content.Context;
import android.hardware.SensorEvent;
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
    private int maxDistance = 200;
    private int maxSpeed = 8000;
    private int minSpeed = 10;

    private Context context;

    private long accelerometerStartTime = 0;
    private long accelerometerStopTime = 0;
    private List<SurfaceDoctorPoint> surfaceDoctorPoints = new ArrayList<SurfaceDoctorPoint>();

    private float[] gravity = new float[3];

    private float alpha = 0.8f;

    private boolean hasLocationPairs = false;
    private Location endPoint;
    private ArrayList<String[]> segmentCoordinates = new ArrayList<>();

    private double totalAccumulatedDistance = 0.0;

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
    public SegmentHandler(Context context) {
        this.context = context;
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
        // TODO: Need to implement high-pass filter.
        // TODO: Do we need just the upwards acceleration?

        // We need time between accelerometer events to calculate the IRI.
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

            linearAcceleration[0] = inputAccelerometer[0] - adjustedGravity[0];
            linearAcceleration[1] = inputAccelerometer[1] - adjustedGravity[1];
            linearAcceleration[2] = inputAccelerometer[2] - adjustedGravity[2];

            // For each measurement of the accelerometer, create a SurfaceDoctorPoint object.
            SurfaceDoctorPoint surfaceDoctorPoint = new SurfaceDoctorPoint(
                    linearAcceleration[0],
                    linearAcceleration[1],
                    linearAcceleration[2],
                    accelerometerStartTime,
                    accelerometerStopTime);
            surfaceDoctorPoints.add(surfaceDoctorPoint);
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
            executeSurfaceDoctor(endPoint, startPoint );

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

        // Let's extract the required data from our Location object.
        double lineDistance = locationStart.distanceTo(locationEnd);
        double lineSpeed = locationEnd.getSpeed();
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

            // Make a new reference to the data.
            // TODO: Note sure if this is necessary.
            double finalDistance = totalAccumulatedDistance;
            ArrayList<String[]> finalCoordinantes = new ArrayList<>(segmentCoordinates);
            List<SurfaceDoctorPoint> finalPoints = new ArrayList<>(surfaceDoctorPoints);

            // This is the end of a segment, let's send it off to be finalized.
            finalizeSegment(finalDistance, finalCoordinantes, finalPoints);

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

                double[] currentVerticalDisplacement = new double[3];
                double[] previousVerticalDisplacement = new double[3];

                currentVerticalDisplacement[0] = measurements.get(i).getVertDissX();
                currentVerticalDisplacement[1] = measurements.get(i).getVertDissY();
                currentVerticalDisplacement[2] = measurements.get(i).getVertDissZ();
                previousVerticalDisplacement[0]  = measurements.get(previousIndex).getVertDissX();
                previousVerticalDisplacement[1]  = measurements.get(previousIndex).getVertDissY();
                previousVerticalDisplacement[2] = measurements.get(previousIndex).getVertDissZ();

                totalVerticalDisplacement[0] += Math.abs( currentVerticalDisplacement[0] - previousVerticalDisplacement[0] );
                totalVerticalDisplacement[1] += Math.abs( currentVerticalDisplacement[1] - previousVerticalDisplacement[1] );
                totalVerticalDisplacement[2] += Math.abs( currentVerticalDisplacement[2] - previousVerticalDisplacement[2] );
            }
        }

        // Now, IRI = total vertical displacement / segment distance.
        double totalIRIofX = totalVerticalDisplacement[0] / distance;
        double totalIRIofY = totalVerticalDisplacement[1] / distance;
        double totalIRIofZ = totalVerticalDisplacement[2] / distance;

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

        if ( isExternalStorageWritable() ) {
            // Add results to GeoJSON.

            String[][] test = new String[polyline.size()][polyline.size()];
            test = polyline.toArray(test);

            String output = "{\"type\": \"FeatureCollection\", \"features\": [ { \"type\": \"Feature\", \"geometry\":" +
                    "{ \"type\": \"LineString\", \"coordinates\":" + Arrays.deepToString(test) + "},\"properties\": { \"DISTANCE\":" +
                    distance + ", \"IRIofX\":" + IRIofX + ", \"IRIofY\":" + IRIofY + ", \"IRIofZ\":" + IRIofZ + "}}]}";


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






            // TODO: Save file as geoJSON or EsriJSON.
        } else {
            // TODO: We'll save to inter
        }

    }


    private boolean isWithinSpeed( double inputSpeed ) {
//        Log.i("SEG", "MIN: " + minSpeed + " SPEED " + lineSpeed + " MAX " + maxSpeed);
        return minSpeed <= inputSpeed && inputSpeed <= maxSpeed;
    }


    private boolean isSegmentEnd() {
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

        //
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

    private File getPrivateStorageDirectory(Context context, String fileName) {
        File file = new File(context.getExternalFilesDir("geoJson"), fileName);
        return file;
    }

}
