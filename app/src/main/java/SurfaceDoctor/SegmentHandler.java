package SurfaceDoctor;

import android.location.Location;
import android.util.Log;

public class SegmentHandler {

    private static boolean units = true;
    private static int maxDistance = 1000;
    private static int maxSpeed = 20;
    private static int minSpeed = 80;

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


    public static void setSurfaceDoctorPreferences(boolean inputUnits, int inputSegmentDistance,
                                             int inputMaxSpeed, int inputMinSpeed) {
        units = inputUnits;
        maxDistance = inputSegmentDistance;
        maxSpeed = inputMaxSpeed;
        minSpeed = inputMaxSpeed;
    }


    public static void setSurfaceDoctorAccelerometer(float[] inputAccelerometer) {

        lineAccelerometerX = inputAccelerometer[0];
        lineAccelerometerY = inputAccelerometer[1];
        lineAccelerometerZ = inputAccelerometer[2];

        Log.i("SEG", "Got accell");

    }


    public static void setSurfaceDoctorLocation(Location inputLocation) {

        // If we get a new location and a location already exists, set the new to old.
        if (currentLocation != null) {

            lastLocation = currentLocation;
            currentLocation = inputLocation;

            lineBearing = inputLocation.getBearing();
            lineDistance = lastLocation.distanceTo(inputLocation);
            lineSpeed = inputLocation.getSpeed();
            currentLong = inputLocation.getLongitude();
            currentLat = inputLocation.getLatitude();

            // We're logging, let's process the data.
            executeSurfaceDoctor();

            Log.i("SEG", "Got location, speed is: " + lineSpeed + " distance is: " + lineDistance);

        } else {
            // This is our first point, let's do nothing this round.
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
        return minSpeed >= lineSpeed && lineSpeed <= maxSpeed;
    }


    public static boolean isSegmentEnd() {
        return currentDistance >= maxDistance;
    }


    public static boolean isPartialSegment() {
        return currentDistance > 0;
    }

    // TODO: We don't need this anymore, but may need a way to evaluate how long it's been since our last gps point.
    public static boolean hasGPS() {
        return true;
    }


    public static void resetLineParameters() {

        lineAccelerometerX = 0;
        lineAccelerometerY = 0;
        lineAccelerometerZ = 0;

    }


    public static void finalizeSegment() {

    }

    public static void resetSegment() {

    }


    public static void executeSurfaceDoctor() {

        // We're at the beginning of a segment.
        if ( isWithinSpeed() && !isPartialSegment() ) {
            Log.i("SEG", "Stating segment");
        }
        // We're within speed and haven't reached the end of a segment, let's log the line.
        else if ( isWithinSpeed() && !isSegmentEnd() ) {

            currentDistance += lineDistance;
            totalAccelerometerZ += lineAccelerometerZ;


            Log.i("SEG", "LINE: x: " +
                    currentLong + " y: " + currentLat + " accelz: " +
                    totalAccelerometerZ + " distance: " + lineDistance);

            appendSegmentDistance();
            resetLineParameters();

        }
        // We're withing speed and reached the end of a segment, let's finalize the segment.
        else if ( isWithinSpeed() && isSegmentEnd() ) {
            Log.i("SEG", "Final distance is " + currentDistance);

        }
        // We've exceeded our speed threshold, we need to throw out this segment, so let's reset everything.
        else if ( !isWithinSpeed() && isPartialSegment()) {
            Log.i("SEG", "Speed threshold exceeded.");
        }
        else {
            Log.i("SEG", "A condition was met that we didn't think about. ");
        }



    }
}
