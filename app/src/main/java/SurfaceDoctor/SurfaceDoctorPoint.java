package SurfaceDoctor;

public class SurfaceDoctorPoint {
    private static float x;
    private static float y;
    private static float z;
    private static long tStart;
    private static long tStop;


    public SurfaceDoctorPoint(float accelerometerX, float accelerometerY, float accelerometerZ, long startTime, long stopTime) {
        x = accelerometerX;
        y = accelerometerY;
        z = accelerometerZ;
        tStart = startTime;
        tStop = stopTime;
    }

    public double getVertDissX() {
        double timeDiff = (tStop - tStart) / 1000000000.0;
        return x * timeDiff * timeDiff;
    }

    public double getVertDissY() {
        double timeDiff = (tStop - tStart) / 1000000000.0;
        return y * timeDiff * timeDiff;
    }


    public double getVertDissZ() {
        double timeDiff = (tStop - tStart) / 1000000000.0;
        return z * timeDiff * timeDiff;
    }
}
