package SurfaceDoctor;

public class SurfaceDoctorPoint {
    public String id;

    private float[] accelerometerPhone;
    private float[] accelerometerEarth;

    public float[] gravity;
    public float[] magnetometer;

    public long timeCreated;

    private long tStart;
    private long tStop;


    public SurfaceDoctorPoint(String inId, float[] accelPhone, float[] accelEarth, float[] inGravity, float[] inMagnetometer, long startTime, long stopTime) {
        id = inId;
        accelerometerPhone = accelPhone;
        accelerometerEarth = accelEarth;
        gravity = inGravity;
        magnetometer = inMagnetometer;
        tStart = startTime;
        tStop = stopTime;

        timeCreated = System.currentTimeMillis();
    }

    public double getVertDissX(boolean returnEarthCoordinateSystem) {
        double timeDiff = (tStop - tStart) / 1000000000.0;

        if (returnEarthCoordinateSystem) {
            return accelerometerEarth[0] * timeDiff * timeDiff;
        } else {
            return accelerometerPhone[0] * timeDiff * timeDiff;
        }
    }

    public double getVertDissY(boolean returnEarthCoordinateSystem) {
        double timeDiff = (tStop - tStart) / 1000000000.0;

        if (returnEarthCoordinateSystem) {
            return accelerometerEarth[1] * timeDiff * timeDiff;
        } else {
            return accelerometerPhone[1] * timeDiff * timeDiff;
        }
    }


    public double getVertDissZ(boolean returnEarthCoordinateSystem) {
        double timeDiff = (tStop - tStart) / 1000000000.0;

        if (returnEarthCoordinateSystem) {
            return accelerometerEarth[2] * timeDiff * timeDiff;
        } else {
            return accelerometerPhone[2] * timeDiff * timeDiff;
        }
    }
}
