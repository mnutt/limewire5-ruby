package org.limewire.promotion;

public class LatitudeLongitude {
    private final static double AVERAGE_EARTH_RADIUS_KM = 6371.0;

    private final byte[] latitude;

    private final byte[] longitude;

    /**
     * Construct an instance using the given byte-encoded integer values using
     * the following decoding:
     * <ul>
     * <li>convert the array to a long (1-8 bytes are acceptable)
     * <li>Calculate the conversion rate byte taking (2^(array.length*8))/360
     * (for instance, 3 bytes would be (16,777,216/360.0)
     * <li>Divide the converted long by the conversion rate to get the degrees,
     * and pass that to the degree constructor
     * </ul>
     * 
     * @param latitude the Latitude
     * @param longitude the Longitude
     */
    public LatitudeLongitude(byte[] latitude, byte[] longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Construct an instance with the given latitude and longitude converted to
     * be storable in 3 bytes each.
     * 
     * @param latitudeDegrees North > 0, South < 0
     * @param longitudeDegrees East > 0, West < 0
     */
    public LatitudeLongitude(double latitudeDegrees, double longitudeDegrees) {
        this(convertDegreesToBytes(latitudeDegrees, 3), convertDegreesToBytes(longitudeDegrees, 3));
    }

    /**
     * Converts a degree value to a normal value between 0 and 359 to a byte
     * array with the given byte count (1-8 bytes). The array will represent a
     * long between 0 and 2^(8 * byteCount), where the highest value scales to
     * 360. Package visible for unit testing.
     * <p>
     * Examples:
     * <ul>
     * <li>(360,3) -> [-1,-1,-1]
     * <li>(0,3) -> [0,0,0]
     * <li>(180,3) -> [127,-1,-1]
     */
    static byte[] convertDegreesToBytes(double degrees, int byteCount) {
        double scale = Math.pow(2, 8 * byteCount) / 360.0;
        long value = (long) (normalizeDegrees(degrees) * scale);
        return org.limewire.util.ByteUtils.long2bytes(value, byteCount);
    }

    /**
     * Takes a degree measure and returns a value between 0 and 359. -90 becomes
     * 270, 361 becomes 1, etc.
     */
    private static double normalizeDegrees(double degrees) {
        while (degrees < 0)
            degrees += 360;
        return degrees % 360;
    }

    /**
     * Converts a B.E. byte array into a long, then converts the long to a
     * double between 0 and 360 based on the maximum size of the long, then
     * converts to radians. Package-visible for unit testing.
     */
    static double toRadians(byte[] bytes) {
        long value = org.limewire.util.ByteUtils.beb2long(bytes, 0, bytes.length);
        return Math.toRadians(value / (Math.pow(2, 8 * bytes.length) / 360.0));
    }

    private double getLatitudeRadians() {
        return toRadians(latitude);
    }

    private double getLongitudeRadians() {
        return toRadians(longitude);
    }

    /** @return the distance from the other point, in kilometers. */
    public double distanceFrom(LatitudeLongitude otherPoint) {
        double lat = getLatitudeRadians();
        double lon = getLongitudeRadians();
        double otherlat = otherPoint.getLatitudeRadians();
        double otherlon = otherPoint.getLongitudeRadians();

        double p1 = Math.cos(lat) * Math.cos(lon) * Math.cos(otherlat) * Math.cos(otherlon);
        double p2 = Math.cos(lat) * Math.sin(lon) * Math.cos(otherlat) * Math.sin(otherlon);
        double p3 = Math.sin(lat) * Math.sin(otherlat);

        return (Math.acos(p1 + p2 + p3) * AVERAGE_EARTH_RADIUS_KM);
    }

    /**
     * If this instance was created using
     * {@link LatitudeLongitude#LatitudeLongitude(double, double)} or using
     * {@link LatitudeLongitude#LatitudeLongitude(byte[], byte[])} using a pair
     * of 3-byte arrays, returns an exact representation for that value.
     * Otherwise a new approximation is created (probably accurate enough to
     * never matter since we're only accurate to 2 meters anyway) and returned.
     * 
     * @return a 6-byte array, the first 3 bytes encoding latitude, the second 3
     *         encoding longitude.
     */
    public byte[] toBytes() {
        byte[] array = new byte[6];
        System.arraycopy(to3Bytes(latitude), 0, array, 0, 3);
        System.arraycopy(to3Bytes(longitude), 0, array, 3, 3);
        return array;
    }

    /**
     * if the array is 3 bytes exactly, returns the original array. Otherwise,
     * converts the array into a degree representation and then back into a byte
     * array of the correct size
     */
    private byte[] to3Bytes(byte[] in) {
        if (in.length == 3)
            return in;
        double value = toRadians(in);
        return convertDegreesToBytes(Math.toDegrees(value), 3);
    }

}
