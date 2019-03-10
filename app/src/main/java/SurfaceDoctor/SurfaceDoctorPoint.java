package SurfaceDoctor;

public class SurfaceDoctorPoint {
    public static float x;
    public static float y;
    public static float z;
    public static long tStart;
    public static long tStop;


    public SurfaceDoctorPoint(float accelerometerX, float accelerometerY, float accelerometerZ, long startTime, long stopTime) {
        x = accelerometerX;
        y = accelerometerY;
        z = accelerometerZ;
        tStart = startTime;
        tStop = stopTime;
    }


    public long getTimeDiff() {
        return tStop - tStart;
    }


    public float getIRIofX() {
        return x * getTimeDiff() * getTimeDiff();
    }


    public float getIRIofY() {
        return y * getTimeDiff() * getTimeDiff();
    }
    

    public float getIRIofZ() {
        return z * getTimeDiff() * getTimeDiff();
    }
}
