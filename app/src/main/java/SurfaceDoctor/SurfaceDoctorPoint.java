package SurfaceDoctor;

public class SurfaceDoctorPoint {
    private float[] accelerometerPhone;
    private float[] accelerometerEarth;
    private long tStart;
    private long tStop;


    public SurfaceDoctorPoint(float[] accelPhone, float[] accelEarth, long startTime, long stopTime) {
        accelerometerPhone = accelPhone;
        accelerometerEarth = accelEarth;
        tStart = startTime;
        tStop = stopTime;
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
