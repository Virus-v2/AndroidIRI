package SurfaceDoctor;

import android.hardware.SensorEvent;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeMap;

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

    private static double currentDistance = 0.0;
    private static double lineDistance = 0;
    private static double lineBearing = 0.0;
    private static double lineSpeed = 0.0;

    private static boolean hasLocationPairs = false;

    //TODO: I think things are delayed now, we don't want to do anything until we know we're within speed. Right now, we're doing things then check if the speed is OK.


    public static void setSurfaceDoctorPreferences(boolean inputUnits, int inputSegmentDistance,
                                             int inputMaxSpeed, int inputMinSpeed) {
        units = inputUnits;
        maxDistance = inputSegmentDistance;
        maxSpeed = inputMaxSpeed;
        minSpeed = inputMinSpeed;
    }


    public void setSurfaceDoctorAccelerometer(SensorEvent sensorEvent) {


        accelerometerStopTime = accelerometerStartTime;
        accelerometerStartTime = sensorEvent.timestamp;

        // Once we know we've established a location, let's start summing our accelerometer data.
        if ( hasLocationPairs && accelerometerStopTime > 0 ) {

            // The sensorEvent.values contains an array of acceleromter values:
            // 0: X
            // 1: Y
            // 2: Z
            float[] inputAccelerometer = sensorEvent.values;

            // For each measurement of the accelerometer, create a SurfaceDoctorPoint object.
            surfaceDoctorPoints.add( new SurfaceDoctorPoint(
                    inputAccelerometer[0],
                    inputAccelerometer[1],
                    inputAccelerometer[2],
                    accelerometerStartTime,
                    accelerometerStopTime));

        } 
    }


    public void setSurfaceDoctorLocation(Location inputLocation) {

        // If we have a pair of location objects, let's run through our logic.
        if (currentLocation != null) {

            // Let's first tell our accelerometer sensors to start summing data.
            hasLocationPairs = true;

            // Now let's make the current point the old point, and update the current point with the new point.
            lastLocation = currentLocation;
            currentLocation = inputLocation;

            // Let's get all our location based data.
            lineBearing = inputLocation.getBearing();
            lineDistance = lastLocation.distanceTo(inputLocation);
            lineSpeed = inputLocation.getSpeed();
            currentLong = inputLocation.getLongitude();
            currentLat = inputLocation.getLatitude();

            // We're logging, let's process the data.
            executeSurfaceDoctor();

//            Log.i("SEG", "Speed is: " + lineSpeed + " line distance is: " + lineDistance +
//                    " total distance is: " + currentDistance);
            Log.i("HASH", "Distance: " + currentDistance);

        } else {
            // This is our first point, our logic depends on a comparison of two location objects, so let's do nothing
            // until we get that second location.
            currentLocation = inputLocation;
        }
    }


    public static void appendSegmentDistance() {
        lineDistance += currentDistance;
    }


    // TODO: Use in settings callback to set segement options.
    public static void setSurfaceDoctorSettings() {

    }


    public static boolean isWithinSpeed() {
//        Log.i("SEG", "MIN: " + minSpeed + " SPEED " + lineSpeed + " MAX " + maxSpeed);
        return minSpeed <= lineSpeed && lineSpeed <= maxSpeed;
    }


    public static boolean isSegmentEnd() {
        return currentDistance >= maxDistance;
    }


    public static void resetSegment(boolean hardReset) {

        // Reset the segment distance to zero.
        currentDistance = 0;

        // Clear all accelerometer measurements from the ArrayList.
        surfaceDoctorPoints.clear();

        //
        if ( hardReset ) {
            hasLocationPairs = false;
            currentLocation = null;
            lastLocation = null;
            accelerometerStopTime = 0;
        }

    }


    public static void finalizeSegment() {

        // TODO: Get the total distance traveled
        // TODO: Get the total accelerometer data.
        // TODO: Get the array of coordinate pairs.

        // TODO: Reset the total distance traveled.
        // TODO: Reset the total accelerometer data.
        // TODO: Reset the array of coordinate pairs.

        // TODO: Save file as EsriJSON.

        // TODO: Need a way to ensure segment was logged before resetting.
        // We will not rest the location pairs to allow for seemeless transition to the next segment. 
        resetSegment(false);

    }


    private void executeSurfaceDoctor() {

//        Log.i("SEG", "isWithinSpee " + isWithinSpeed() +
//                " isSegmentEnd " + isSegmentEnd());


        // We're within speed and haven't reached the end of a segment, let's log the line.
        if ( isWithinSpeed() && !isSegmentEnd() ) {

            currentDistance += lineDistance;

            //TODO: Log the coordinates to an array to generate the polyline.

//            Log.i("SEG", "LINE: x: " +
//                    currentLong + " y: " + currentLat + " accelz: " +
//                    totalAccelerometerZ + " distance: " + lineDistance);


        }
        // We're withing speed and reached the end of a segment, let's finalize the segment.
        else if ( isWithinSpeed() && isSegmentEnd() ) {

            finalizeSegment();

//            Log.i("SEG", "Final distance is " + currentDistance);

        }
        // We've exceeded our speed threshold, we need to reset everything.
        else if ( !isWithinSpeed()) {

            // The hard rest will clear out our location pairs, as well.
            resetSegment(true );

        }
        else {
            Log.i("SEG", "A condition was met that we didn't think about. ");
        }



    }
}
