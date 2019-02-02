# AndroidIRI
Android app for collecting a phone's accelerometer data for use in calculating the 
International Roughness Index (IRI). The phone's accelerometer values are transformed into
earth's coordinate system, allowing values to be recorded independent of the phone's 
orientation.

![example](http://www.markbuie.com/img/github/phone_orientation.gif)

### Device Coordinate System

The coordinate-system is defined relative to the screen of the phone in its default 
orientation. The axes are not swapped when the device's screen orientation changes.

The X axis is horizontal and points to the right, the Y axis is vertical and points 
up and the Z axis points towards the outside of the front face of the screen. In this 
system, coordinates behind the screen have negative Z values.

### Earth Coordinate System

The X axis roughly points East (West when X is a negative number). Y is tangential to the ground at the device's current location and points towards 
magnetic north. Z points towards the sky and is perpendicular to the ground.

### Heading Based Coordinate System
In development. Check back soon. 