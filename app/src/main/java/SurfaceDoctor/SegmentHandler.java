package SurfaceDoctor;

import android.hardware.SensorEvent;
import android.location.Location;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static java.lang.Math.abs;

public class SegmentHandler {

    // Default user input parameters.
    private static boolean units = true;
    private static int maxDistance = 500;
    private static int maxSpeed = 20000;
    private static int minSpeed = 5;

    private static long accelerometerStartTime = 0;
    private static long accelerometerStopTime = 0;
    private static ArrayList<SurfaceDoctorPoint> surfaceDoctorPoints = new ArrayList<>();

    private static boolean hasLocationPairs = false;
    private static Location currentLocation;
    private static ArrayList<double[]> segmentCoordinates = new ArrayList<>();

    private static double totalAccumulatedDistance = 0.0;

    // Required to create an Event.
    private SurfaceDoctorInterface listener;
    public void setSomeEventListener (SurfaceDoctorInterface listener) {
        this.listener = listener;
    }


    /**
     * Method for setting SurfaceDoctor settings that were defined by the user.
     *
     * @param inputUnits
     * @param inputSegmentDistance
     * @param inputMaxSpeed
     * @param inputMinSpeed
     */
    public static void setSurfaceDoctorPreferences(boolean inputUnits, int inputSegmentDistance,
                                             int inputMaxSpeed, int inputMinSpeed) {
        // TODO: This needs implementing.
        units = inputUnits;
        maxDistance = inputSegmentDistance;
        maxSpeed = inputMaxSpeed;
        minSpeed = inputMinSpeed;
    }


    /**
     * Method for receiving accelerometer data from sensors.
     *
     * @param sensorEvent
     */
    public void setSurfaceDoctorAccelerometer(SensorEvent sensorEvent) {
        // TODO: Need to implement high-pass filter.
        // TODO: Do we need just the upwards acceleration?

        // We need time between accelerometer events to calculate the IRI.
        accelerometerStartTime = accelerometerStopTime;
        accelerometerStopTime = sensorEvent.timestamp;

        // Check if we have location pairs and start & stop times. If so, we start collecting SurfaceDoctorPoint objects.
        // hasLocationPairs is set to true by the setSurfaceDoctorLocation method when to location event's have been
        // recorded.
        if ( hasLocationPairs && accelerometerStartTime > 0 ) {

            // The sensorEvent.values contains an array of acceleromter values:
            // 0: X
            // 1: Y
            // 2: Z
            float[] inputAccelerometer = sensorEvent.values;

            // For each measurement of the accelerometer, create a SurfaceDoctorPoint object.
            // TODO: Does this need to be a class?
            surfaceDoctorPoints.add( new SurfaceDoctorPoint(
                    inputAccelerometer[0],
                    inputAccelerometer[1],
                    inputAccelerometer[2],
                    accelerometerStartTime,
                    accelerometerStopTime));
        }
    }


    /** Method for receiving Location data from the GPS sensor.
     *
     * @param inputLocation
     */
    public void setSurfaceDoctorLocation(Location inputLocation) {

        // If we have a pair of location objects, let's run through our logic.
        if (currentLocation != null) {

            // Let's first tell our accelerometer sensors to start summing data.
            hasLocationPairs = true;

            // Now let's make the current point the old point, and update the current point with the new point. We'll
            // use these location pairs to extract data later.
            Location lastLocation = currentLocation;
            currentLocation = inputLocation;

            // We're logging, let's process the data.
            executeSurfaceDoctor( currentLocation, lastLocation );

            // TODO: We'll need to create an event that lets MainActivity know if we're within speed, logging, etc. This could also be handled on MainActivity side.
            double lineBearing = inputLocation.getBearing();

            // This creates an event with the sensor values, that will fire in the MainActivity. It was used for learning
            // purposes and currently does nothing.
//            if (listener != null) {
//                SurfaceDoctorEvent e = new SurfaceDoctorEvent();
//                e.type = "TYPE_LOCATION";
//                e.speed = lineSpeed;
//                e.heading = lineBearing;
//
//                listener.onSurfaceDoctorEvent(e);
//            }

        } else {
            // This is our first point, our logic depends on a comparison of two location objects, so let's do nothing
            // until we get that second location.
            currentLocation = inputLocation;
        }
    }


    /**
     *  This is the main logic handler of the SegmentHandler.
     *
     *  This is fired at every GPS location callback from Android.
     */
    private void executeSurfaceDoctor(Location locationStart, Location locationEnd) {

        // Let's extract the required data from our Location object.
        double lineDistance = locationStart.distanceTo(locationEnd);
        double lineSpeed = locationEnd.getSpeed();
        double[] coordinates = new double[]{locationStart.getLongitude(), locationStart.getLatitude()};
        double[] coordinatesLast = new double[]{locationEnd.getLongitude(), locationEnd.getLatitude()};

        // We're within speed and haven't reached the end of a segment, let's add the distance between the coordinate
        // pairs to the total distance of the segment.
        if ( isWithinSpeed( lineSpeed ) && !isSegmentEnd() ) {
            // Append the distance between the coordinate points to the total distance.
            totalAccumulatedDistance += lineDistance;
            // Append the new coordinates to the ArrayList so we can create a polyline later.
            segmentCoordinates.add(coordinates);

            Log.i("IRI", "Distance: " + totalAccumulatedDistance);
        }
        // We're withing speed and reached the end of a segment, let's finalize the segment.
        else if ( isWithinSpeed( lineSpeed ) && isSegmentEnd() ) {
            // Append the distance between the coordinate points to the total distance.
            totalAccumulatedDistance += lineDistance;
            // Append the new coordinates to the ArrayList so we can create a polyline later.
            segmentCoordinates.add(coordinates);
            // The segement is done, add the last coordinate.
            segmentCoordinates.add(coordinatesLast);

            finalizeSegment(totalAccumulatedDistance, segmentCoordinates, surfaceDoctorPoints);

            resetSegment(false);
        }
        // We've exceeded our speed threshold, we need to do a hard reset.
        // TODO: Would if we lose coordinate pairs or accelerometer data. 
        else if ( !isWithinSpeed( lineSpeed )) {
            // The hard rest will clear out our location pairs, as well.
            resetSegment(true );
        }
        else {
            Log.i("SEG", "A condition was met that we didn't think about. ");
        }
    }


    /**
     *  Finalizes a road segment
     *
     *  Executes when the segment distance threshold has been met.      *
     */
    private void finalizeSegment(double distance, ArrayList<double[]> polyline, ArrayList<SurfaceDoctorPoint> measurements) {
        double totalVerticalDisplacementX = 0.0;
        double totalVerticalDisplacementY = 0.0;
        double totalVerticalDisplacementZ = 0.0;

        // First get the total vertical displacement of the segment.
        for (int i = 0; i < measurements.size(); i++ ) {
            int previousIndex = i - 1;

            // Vertical Displacements equals the absolute value of the longitudinal offset minus the previous offset.
            if ( previousIndex >= 0 ) {

                double currentX = measurements.get(i).getVertDissX();
                double currentY = measurements.get(i).getVertDissY();
                double currentZ = measurements.get(i).getVertDissZ();
                double previousX = measurements.get(previousIndex).getVertDissX();
                double previousY = measurements.get(previousIndex).getVertDissY();
                double previousZ = measurements.get(previousIndex).getVertDissZ();

                totalVerticalDisplacementX += Math.abs(currentX - previousX);
                totalVerticalDisplacementY += Math.abs(currentY - previousY);
                totalVerticalDisplacementZ += Math.abs(currentZ - previousZ);
            }
        }

        // Now, IRI = total vertical displacement / segment distance.
        double totalIRIofX = totalVerticalDisplacementX / distance;
        double totalIRIofY = totalVerticalDisplacementY / distance;
        double totalIRIofZ = totalVerticalDisplacementZ / distance;

        Log.i("IRI", "X " + totalIRIofX + " Y " + totalIRIofY + " Z " + totalIRIofZ);

        // Let's pass the segment data to the SurfaceDoctorEvent so it can be used in the main activity.
        // The SurfaceDoctorEvent will be triggered in the MainActivity.
        if (listener != null) {
            SurfaceDoctorEvent e = new SurfaceDoctorEvent();
            e.type = "TYPE_SEGMENT_IRI";
            e.x = totalVerticalDisplacementX;
            e.y = totalIRIofY;
            e.z = totalIRIofZ;
            e.distance = distance;
            // TODO: Assign output to SurfaceDoctorEvent class.
            listener.onSurfaceDoctorEvent(e);
        }
    }


    private static void saveResults(double distance, double IRIofX, double IRIofY, double IRIofZ, ArrayList<double[]> polyline ) {

        // Add results to GeoJSON.
        try {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("type", "Feature");

            JSONObject geometryJSON = new JSONObject();
            geometryJSON.put("type", "LineString");
            geometryJSON.put("coordinates", polyline);
            jsonObj.put("geometry", geometryJSON);

            JSONObject propertiesJSON = new JSONObject();
            propertiesJSON.put("DISTANCE", distance);
            propertiesJSON.put("IRI_X", IRIofX);
            propertiesJSON.put("IRI_Y", IRIofY);
            propertiesJSON.put("IRI_Z", IRIofZ);
            jsonObj.put("properties", propertiesJSON);
        } catch (JSONException e) {
            Log.e("JSON", "Unexpected JSON exception", e);
        }

        // TODO: Save file as geoJSON or EsriJSON.

    }


    private static boolean isWithinSpeed( double inputSpeed ) {
//        Log.i("SEG", "MIN: " + minSpeed + " SPEED " + lineSpeed + " MAX " + maxSpeed);
        return minSpeed <= inputSpeed && inputSpeed <= maxSpeed;
    }


    private static boolean isSegmentEnd() {
        return totalAccumulatedDistance >= maxDistance;
    }


    /**
     * Resets all the parameters.
     *
     * Used to restart the logging of a road segment.
     *
     * @param hardReset boolean
     */
    private static void resetSegment(boolean hardReset) {

        // Reset the segment distance to zero.
        totalAccumulatedDistance = 0;

        // Clear all accelerometer measurements from the ArrayList.
        surfaceDoctorPoints.clear();

        // Clear the list of coordinates that make the polyline. 
        segmentCoordinates.clear();

        //
        if ( hardReset ) {
            hasLocationPairs = false;
            currentLocation = null;
            accelerometerStopTime = 0;
        }
    }
}
