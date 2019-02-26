package SurfaceDoctor;

import android.location.Location;

public class SegmentHandler {

    private static boolean units;
    private static int maxDistance;
    private static int maxSpeed;
    private static int minSpeed;

    private static float[] accelerometer;

    private static float speed;
    private static float[] location;

    private static float segementDistance;


    public static void setSurfaceDoctorPreferences(boolean inputUnits, int inputSegmentDistance,
                                             int inputMaxSpeed, int inputMinSpeed) {
        inputUnits = units;
        inputSegmentDistance = maxDistance;
        inputMaxSpeed = maxSpeed;
        inputMinSpeed = minSpeed;
    }


    public static void setSurfaceDoctorAccelerometer(float[] inputAccelerometer) {
        inputAccelerometer = accelerometer;
    }


    public static void setSurfaceDoctorLocation(Location location) {}


    public static void setLocationParameters(float[] inputLocation, float inputSpeed) {
        inputLocation = location;
        inputSpeed = speed;
    }


    public static boolean isWithinSpeed() {

        boolean isWithinSpeed = false;

        if (minSpeed >= speed && speed <= maxSpeed) {
            isWithinSpeed = true;
        }

        return isWithinSpeed;
    }


    public static boolean isSegmentLogging() {
        return true;
    }


    public static boolean isSegmentEnd() {

        boolean isSegmentEnd = false;

        if (segementDistance >= maxDistance) {
            isSegmentEnd = true;
        }

        return isSegmentEnd;
    }

    public static void sendSegmentDetails() {

    }

}
