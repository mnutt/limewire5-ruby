package org.limewire.promotion;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.ByteUtils;

public class LatitudeLongitudeTest extends BaseTestCase {
    public LatitudeLongitudeTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LatitudeLongitudeTest.class);
    }

    public void testCalculateDistance() {
        LatitudeLongitude pos1 = new LatitudeLongitude(15, -90);
        LatitudeLongitude pos2 = new LatitudeLongitude(45, 0);

        assertEquals(pos1.distanceFrom(pos2), pos2.distanceFrom(pos1));
        assertEquals(8835, Math.round(pos1.distanceFrom(pos2)));

        // Go around 360 degrees
        assertEquals(0.0, pos1.distanceFrom(new LatitudeLongitude(375, 270)));
    }

    public void testLongBytesToDegrees() {
        assertEquals(0.0, LatitudeLongitude.toRadians(new byte[] { 0 }));
        assertEquals(6, Math.round(LatitudeLongitude.toRadians(new byte[] { -1, -1, -1 })));
        assertEquals(3, Math
                .round(LatitudeLongitude.toRadians(new byte[] { 127, -1, -1 })));

        assertEquals(6, Math.round(LatitudeLongitude.toRadians(ByteUtils.long2bytes(
                16777215, 3))));

    }

    public void testConvertDegreesToBytes() {
        assertEquals(new byte[] { 0, 0, 0 }, LatitudeLongitude.convertDegreesToBytes(0, 3));
        assertEquals(new byte[] { -1, -1, -1 }, LatitudeLongitude.convertDegreesToBytes(
                359.9999999, 3));
        assertEquals(ByteUtils.long2bytes(8388608, 3), LatitudeLongitude.convertDegreesToBytes(
                180, 3));
    }

    public void testToBytes() {
        assertEquals(new byte[] { 0, 0, 0, 0, 0, 0 }, new LatitudeLongitude(0, 0).toBytes());
        assertEquals(new byte[] { 0, 0, 0, -128, 0, 0 }, new LatitudeLongitude(0, 180).toBytes());
        assertEquals(new byte[] { -128, 0, 0, -128, 0, 0 }, new LatitudeLongitude(180, 180)
                .toBytes());
    }
}
