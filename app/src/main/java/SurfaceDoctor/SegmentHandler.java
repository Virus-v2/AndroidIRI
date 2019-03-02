package SurfaceDoctor;

import android.location.Location;
import android.util.Log;

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
        minSpeed = inputMaxSpeed;
    }


    public static void setSurfaceDoctorAccelerometer(float[] inputAccelerometer) {

        // Once we know we've established a location, let's start summing our accelerometer data.
        if (hasLocationPairs) {
            lineAccelerometerX = inputAccelerometer[0];
            lineAccelerometerY = inputAccelerometer[1];
            lineAccelerometerZ = inputAccelerometer[2];

            totalAccelerometerX += lineAccelerometerX;
            totalAccelerometerY += lineAccelerometerY;
            totalAccelerometerZ += lineAccelerometerZ;
        }
    }


    public static void setSurfaceDoctorLocation(Location inputLocation) {

        // If we have a pair of location objects, let's run through our logic.
        if (currentLocation != null) {

            // Let's first tell our accelerometer sensors to start summing data.
            hasLocationPairs = true;

            // Now let's swap the current point to the old point, and update the newest location.
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

            Log.i("SEG", "Speed is: " + lineSpeed + " line distance is: " + lineDistance +
                    " total distance is: " + currentDistance);

        } else {
            // This is our first point, our logic depends on a comparison of two location objects, so let's do nothing
            // until we get that second location.
            currentLocation = inputLocation;
        }
    }


    public static void appendSegmentDistance() {
        lineDistance += currentDistance;
    }


    public static void appendSegmentAccelerometer() {
        //TODO: Best way to add a value to an array.
//         totalAccelerometerX += currentAccelerometer[0];
//         totalAccelerometerY += currentAccelerometer[1];
//         totalAccelerometerZ += currentAccelerometer[2];
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

    // TODO: We don't need this anymore, but may need a way to evaluate how long it's been since our last gps point.
    public static boolean hasGPS() {
        return true;
    }


    public static void resetSegment() {

        currentDistance = 0;
        totalAccelerometerX = 0;
        totalAccelerometerY = 0;
        totalAccelerometerZ = 0;

    }


    public static void finalizeSegment() {

        // Do something here

        // TODO: Need a way to ensure segment was logged before resetting.
        resetSegment();

    }



    public static void executeSurfaceDoctor() {

        Log.i("SEG", "isWithinSpee " + isWithinSpeed() +
                " isSegmentEnd " + isSegmentEnd());


        // We're within speed and haven't reached the end of a segment, let's log the line.
        if ( isWithinSpeed() && !isSegmentEnd() ) {

            currentDistance += lineDistance;
            totalAccelerometerZ += lineAccelerometerZ;


            Log.i("SEG", "LINE: x: " +
                    currentLong + " y: " + currentLat + " accelz: " +
                    totalAccelerometerZ + " distance: " + lineDistance);

            appendSegmentDistance();

        }
        // We're withing speed and reached the end of a segment, let's finalize the segment.
        else if ( isWithinSpeed() && isSegmentEnd() ) {

            finalizeSegment();

            Log.i("SEG", "Final distance is " + currentDistance);

        }
        // We've exceeded our speed threshold, we need to reset everything.
        else if ( !isWithinSpeed()) {

            resetSegment();


        }
        else {
            Log.i("SEG", "A condition was met that we didn't think about. ");
        }



    }
}
