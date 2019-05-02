package SurfaceDoctor;

import android.hardware.SensorEvent;
import android.location.Location;
import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class SegmentHandler {

    private static boolean units = true;
    private static int maxDistance = 1000;
    private static int maxSpeed = 200;
    private static int minSpeed = 10;

    private static float lineAccelerometerX;
    private static float lineAccelerometerY;
    private static float lineAccelerometerZ;
    private static float totalAccelerometerX;
    private static float totalAccelerometerY;
    private static float totalAccelerometerZ;
    private static long accelerometerStartTime = 0;
    private static long accelerometerStopTime = 0;
    private static List<SurfaceDoctorPoint> surfaceDoctorPoints = new ArrayList<>();

    private static Location currentLocation;
    private static Location lastLocation;
    private static double currentLat;
    private static double currentLong;
    private static ArrayList<double[]> segmentCoordinates = new ArrayList<>();

    private static double totalAccumulatedDistance = 0.0;
    private static double lineDistance = 0;
    private static double lineBearing = 0.0;
    private static double lineSpeed = 0.0;

    private static boolean hasLocationPairs = false;

    private SurfaceDoctorInterface listener;


    // Required to create an Event.
    public void setSomeEventListener (SurfaceDoctorInterface listener) {
        this.listener = listener;
    }


    /**
     * Method for setting SurfaceDoctor settings that were defined by the user.
     *
     * @param inputUnits
     * @param inputSegmentDistance
     * @param inputMaxSpeed
     * @param inputMinSpeed
     */
    public static void setSurfaceDoctorPreferences(boolean inputUnits, int inputSegmentDistance,
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

        // This creates an event with the sensor values, that will fire in the MainActivity. It was used for learning
        // purposes and currently does nothing.
//        if (listener != null) {
//            SurfaceDoctorEvent e = new SurfaceDoctorEvent();
//            e.type = "TYPE_ACCELEROMETER_PHONE";
//            e.accelerometerPhone = sensorEvent.values;
//
//            listener.onSurfaceDoctorEvent(e);
//        }

        // When need time between accelerometer events to calculate the IRI.
        accelerometerStopTime = accelerometerStartTime;
        accelerometerStartTime = sensorEvent.timestamp;

        // Check if we have location pairs and start & stop times. If so, we start collecting SurfaceDoctorPoint objects.
        // hasLocationPairs is set to true by the setSurfaceDoctorLocation method when to location event's have been
        // recorded.
        if ( hasLocationPairs && accelerometerStopTime > 0 ) {

            // The sensorEvent.values contains an array of acceleromter values:
            // 0: X
            // 1: Y
            // 2: Z
            float[] inputAccelerometer = sensorEvent.values;

            // For each measurement of the accelerometer, create a SurfaceDoctorPoint object.
            // TODO: Does this need to be a class?
            surfaceDoctorPoints.add( new SurfaceDoctorPoint(
                    inputAccelerometer[0],
                    inputAccelerometer[1],
                    inputAccelerometer[2],
                    accelerometerStartTime,
                    accelerometerStopTime));

        }
    }


    /** Method for receiving Location data from the GPS sensor.
     *
     * @param inputLocation
     */
    public void setSurfaceDoctorLocation(Location inputLocation) {

        // If we have a pair of location objects, let's run through our logic.
        if (currentLocation != null) {

            // Let's first tell our accelerometer sensors to start summing data.
            hasLocationPairs = true;

            // Now let's make the current point the old point, and update the current point with the new point.
            lastLocation = currentLocation;
            currentLocation = inputLocation;

            // Let's assign all of our Location data to variables.
            lineBearing = inputLocation.getBearing();
            lineDistance = lastLocation.distanceTo(inputLocation);
            lineSpeed = inputLocation.getSpeed();
            currentLong = inputLocation.getLongitude();
            currentLat = inputLocation.getLatitude();

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

            // We're logging, let's process the data.
            executeSurfaceDoctor();

        } else {
            // This is our first point, our logic depends on a comparison of two location objects, so let's do nothing
            // until we get that second location.
            currentLocation = inputLocation;
        }
    }


    /**
     *  This is the main logic handler of the SegmentHandler.
     */
    private void executeSurfaceDoctor() {

        // We're within speed and haven't reached the end of a segment, let's add the distance between the coordinate
        // pairs to the total distance of the segment.
        if ( isWithinSpeed() && !isSegmentEnd() ) {
            // Append the distance between the coordinate points to the total distance.
            totalAccumulatedDistance += lineDistance;
            // Append the new coordinates to the ArrayList so we can create a polyline later.
            double[] coordinates = new double[]{currentLat, currentLong};
            segmentCoordinates.add(coordinates);
        }
        // We're withing speed and reached the end of a segment, let's finalize the segment.
        else if ( isWithinSpeed() && isSegmentEnd() ) {
            // TODO: Do we need to append the line distance and coordinates one last time. I think we do.
            finalizeSegment();
        }
        // We've exceeded our speed threshold, we need to do a soft reset.
        else if ( !isWithinSpeed()) {
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
    private void finalizeSegment() {

        // TODO: Get the total distance traveled
        Log.i("FINAL DISTANCE", "Final distance is " + totalAccumulatedDistance);
        Log.i("FINAL COORDINATES", "Coordinates " + segmentCoordinates);
        // TODO: Get the total accelerometer data.
        // TODO: Get the array of coordinate pairs.

        // TODO: Reset the total distance traveled.
        // TODO: Reset the total accelerometer data.
        // TODO: Reset the array of coordinate pairs.

        // TODO: Save file as EsriJSON.

        // TODO: Need a way to ensure segment was logged before resetting.
        // We will not rest the location pairs to allow for seemeless transition to the next segment.

        // Let's pass the segment data to the SurfaceDoctorEvent so it can be used in the main activity.
        if (listener != null) {
            SurfaceDoctorEvent e = new SurfaceDoctorEvent();
            e.type = "TYPE_SEGMENT_IRI";
            // TODO: Assign output to SurfaceDoctorEvent class.
            listener.onSurfaceDoctorEvent(e);
        }
        resetSegment(false);
    }


    private static boolean isWithinSpeed() {
//        Log.i("SEG", "MIN: " + minSpeed + " SPEED " + lineSpeed + " MAX " + maxSpeed);
        return minSpeed <= lineSpeed && lineSpeed <= maxSpeed;
    }


    private static boolean isSegmentEnd() {
        return totalAccumulatedDistance >= maxDistance;
    }


    /**
     * Resets all the parameters.
     *
     * Used to restart the logging of a road segment.
     *
     * @param hardReset boolean
     */
    private static void resetSegment(boolean hardReset) {

        // Reset the segment distance to zero.
        totalAccumulatedDistance = 0;

        // Clear all accelerometer measurements from the ArrayList.
        surfaceDoctorPoints.clear();

        // Clear the list of coordinates that make the polyline. 
        segmentCoordinates.clear();

        //
        if ( hardReset ) {
            hasLocationPairs = false;
            currentLocation = null;
            lastLocation = null;
            accelerometerStopTime = 0;
        }
    }
}
