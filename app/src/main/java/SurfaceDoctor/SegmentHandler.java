package SurfaceDoctor;

import android.location.Location;

public class SegmentHandler {

    private static boolean units;
    private static int maxDistance;
    private static int maxSpeed;
    private static int minSpeed;

    private static float[] currentAccelerometer;
    private static float[] totalAccelerometerX;
    private static float[] totalAccelerometerY;
    private static float[] totalAccelerometerZ;

    private static Location currentLocation;
    private static Location lastLocation;
    private static float[] currentCoordinates;

    private static float currentDistance;
    private static float lineDistance;
    private static float lineBearing;
    private static float lineSpeed;


    public static void setSurfaceDoctorPreferences(boolean inputUnits, int inputSegmentDistance,
                                             int inputMaxSpeed, int inputMinSpeed) {
        units = inputUnits;
        maxDistance = inputSegmentDistance;
        maxSpeed = inputMaxSpeed;
        minSpeed = inputMaxSpeed;
    }


    public static void setSurfaceDoctorAccelerometer(float[] inputAccelerometer) {

        currentAccelerometer = inputAccelerometer;
    }


    public static void setSurfaceDoctorLocation(Location inputLocation) {
        currentLocation = lastLocation;
        currentLocation = inputLocation;
    }


    public static void setLocationParameters() {

        // get the distance between this measurement and the last measurement, so we can add it to the total distance.
        lineDistance = currentLocation.distanceTo(lastLocation);
        lineBearing = currentLocation.getBearing();
        lineSpeed = currentLocation.getSpeed();
    }


    public static void appendSegmentDistance() {
        currentDistance += lineDistance;
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


    public static boolean isSegmentLogging() {
        return true;
    }


    public static boolean isSegmentEnd() {
        return currentDistance >= maxDistance;
    }


    public static boolean isPartialSegment() {
        return currentDistance > 0;
    }

    public static boolean hasGPS() {
        return true;
    }


    public static void finalizeSegment() {

    }

    public static void resetSegment() {

    }


    public static void executeSurfaceDoctorLogic() {

    }
}
