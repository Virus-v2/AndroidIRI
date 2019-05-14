package SurfaceDoctor;

public class SurfaceDoctorPoint {
    private float[] accelerometerPhone;
    private long tStart;
    private long tStop;


    public SurfaceDoctorPoint(float[] accelPhone, long startTime, long stopTime) {
        accelerometerPhone = accelPhone;
        tStart = startTime;
        tStop = stopTime;
    }

    public double getVertDissX() {
        double timeDiff = (tStop - tStart) / 1000000000.0;
        return accelerometerPhone[0] * timeDiff * timeDiff;
    }

    public double getVertDissY() {
        double timeDiff = (tStop - tStart) / 1000000000.0;
        return accelerometerPhone[1] * timeDiff * timeDiff;
    }


    public double getVertDissZ() {
        double timeDiff = (tStop - tStart) / 1000000000.0;
        return accelerometerPhone[2] * timeDiff * timeDiff;
    }
}
