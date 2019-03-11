package SurfaceDoctor;

public class SurfaceDoctorEvent {
    // Type = TYPE_ACCELEROMETER_PHONE
    // Accelerometer data original

    // TYPE_ACCELEROMETER_HEADING
    // Accelerometer data heading

    // TYPE_LOCATION
    // current speed
    // current heading

    // TYPE_SEGMENT_STATUS
    // is logging
    // is within speed

    // TYPE_SEGMENT_IRI
    // iri of last segment
    // distance of last segment
    // array of coordinates making up the line of the segment.

    public static String type;

    public static float[] accelerometerPhone;
    public static float[] accelerometerHeading;

    public static double speed;
    public static double heading;

    public static boolean isLogging;
    public static boolean withinSpeed;

    public static float iri;
    public static float distance;
    public static float[] coordinates;

    public static String getType() { return type;}
}
