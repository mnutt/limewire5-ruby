package org.limewire.promotion.containers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import junit.framework.Test;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.promotion.LatitudeLongitude;
import org.limewire.promotion.containers.PromotionMessageContainer.GeoRestriction;
import org.limewire.promotion.containers.PromotionMessageContainer.PromotionMediaType;
import org.limewire.promotion.containers.PromotionMessageContainer.PromotionOptions;
import org.limewire.promotion.exceptions.PromotionException;
import org.limewire.util.BaseTestCase;

public class PromotionMessageContainerTest extends BaseTestCase {
    public PromotionMessageContainerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PromotionMessageContainerTest.class);
    }

    public void testDescriptionNullSetGetCycle() {
        PromotionMessageContainer message = new PromotionMessageContainer();
        message.setDescription(null);
        assertEquals("", message.getDescription());
    }

    public void testDescriptionSetGetCycle() {
        PromotionMessageContainer message = new PromotionMessageContainer();
        message.setDescription("description");
        assertEquals("description", message.getDescription());
    }

    public void testURLSetGetCycle() {
        PromotionMessageContainer message = new PromotionMessageContainer();
        message.setURL("url");
        assertEquals("url", message.getURL());
    }

    public void testKeywordsSetGetCycle() {
        PromotionMessageContainer message = new PromotionMessageContainer();
        message.setKeywords("keywords");
        assertEquals("keywords", message.getKeywords());
    }

    public void testTerritoryEmptySetGetCycle() throws BadGGEPPropertyException {
        PromotionMessageContainer message = new PromotionMessageContainer();
        message.setTerritories();

        assertNotNull(message.getTerritories());
        assertEquals(0, message.getTerritories().length);
    }

    public void testTerritoryWWSetGetCycle() throws BadGGEPPropertyException {
        PromotionMessageContainer message = new PromotionMessageContainer();
        message.setTerritories(new Locale("", "WW"));

        assertNotNull(message.getTerritories());
        assertEquals(1, message.getTerritories().length);
        assertEquals("WW", message.getTerritories()[0].getCountry());
    }

    public void testTerritoryIrregularCountrySetGetCycle() throws BadGGEPPropertyException {
        PromotionMessageContainer message = new PromotionMessageContainer();
        message.setTerritories(new Locale("", "WW"), new Locale("", "X"));

        // We expect that last 1 char territory to go away, it's wrong.
        assertNotNull(message.getTerritories());
        assertEquals(1, message.getTerritories().length);
        assertEquals("WW", message.getTerritories()[0].getCountry());
    }

    public void testDateRangeStart() {
        PromotionMessageContainer message = new PromotionMessageContainer();
        assertEquals(0, message.getValidStart().getTime());
        assertEquals(new Date(PromotionMessageContainer.MAX_DATE_IN_SECONDS * 1000), message
                .getValidEnd());

        Date start = new Date(System.currentTimeMillis() + 65000);
        message.setValidStart(start);
        assertEquals(start.getTime() / 1000, message.getValidStart().getTime() / 1000);
        assertEquals(new Date(PromotionMessageContainer.MAX_DATE_IN_SECONDS * 1000), message
                .getValidEnd());

        message.setValidStart(null);
        assertEquals(System.currentTimeMillis() / 1000, message.getValidStart().getTime() / 1000);
    }

    public void testDateRangeEnd() {
        PromotionMessageContainer message = new PromotionMessageContainer();
        assertEquals(0, message.getValidStart().getTime());
        assertEquals(new Date(PromotionMessageContainer.MAX_DATE_IN_SECONDS * 1000), message
                .getValidEnd());

        Date start = new Date(System.currentTimeMillis());
        Date end = new Date(System.currentTimeMillis() + 65000);
        message.setValidStart(start);
        message.setValidEnd(end);
        assertEquals(end.getTime() / 1000, message.getValidEnd().getTime() / 1000);

        message.setValidEnd(null);
        assertEquals(new Date(PromotionMessageContainer.MAX_DATE_IN_SECONDS * 1000), message
                .getValidEnd());
        assertEquals(start.getTime() / 1000, message.getValidStart().getTime() / 1000);
    }

    public void testPropertiesCycle() {
        PromotionMessageContainer message = new PromotionMessageContainer();
        Map<String, String> props = new HashMap<String, String>();
        props.put("foo", "bar");
        message.setProperties(props);

        props = message.getProperties();
        assertEquals("bar", props.get("foo"));
    }

    public void testPropertiesEmpty() {
        // Test putting in empty values
        PromotionMessageContainer message = new PromotionMessageContainer();
        Map<String, String> props = new HashMap<String, String>();

        props.put("empty", "");

        message.setProperties(props);
        Map<String, String> props2 = message.getProperties();

        assertEquals("", props2.get("empty"));
    }

    public void testPropertiesNull() {
        // Test putting in empty values, null values
        PromotionMessageContainer message = new PromotionMessageContainer();
        Map<String, String> props = new HashMap<String, String>();

        props.put("null", null);

        message.setProperties(props);
        Map<String, String> props2 = message.getProperties();

        assertNull(props2.get("null"));
    }

    public void testPropertiesFull() {
        // Test putting in empty values, null values
        PromotionMessageContainer message = new PromotionMessageContainer();
        Map<String, String> props = new HashMap<String, String>();

        props.put("empty", "");
        props.put("empty2", "");
        props.put("foo1", "bar1");

        message.setProperties(props);
        Map<String, String> props2 = message.getProperties();

        assertEquals("", props2.get("empty"));
        assertEquals("", props2.get("empty2"));
        assertEquals("bar1", props2.get("foo1"));
    }

    public void testPropertyKeyCodecCycle() {
        PromotionMessageContainer message = new PromotionMessageContainer();

        assertEquals(((char) 128) + "", message.encodePropertyKey("artist"));
        assertEquals(((char) 129) + "", message.encodePropertyKey("album"));
        assertEquals(((char) 130) + "", message.encodePropertyKey("url"));
        assertEquals(((char) 131) + "", message.encodePropertyKey("genre"));
        assertEquals(((char) 132) + "", message.encodePropertyKey("license"));
        assertEquals(((char) 133) + "", message.encodePropertyKey("size"));
        assertEquals(((char) 134) + "", message.encodePropertyKey("creation_time"));
        assertEquals(((char) 135) + "", message.encodePropertyKey("vendor"));
        assertEquals(((char) 136) + "", message.encodePropertyKey("name"));

        assertEquals("artist", message.decodePropertyKey(((char) 128) + ""));
        assertEquals("album", message.decodePropertyKey(((char) 129) + ""));
        assertEquals("url", message.decodePropertyKey(((char) 130) + ""));
        assertEquals("genre", message.decodePropertyKey(((char) 131) + ""));
        assertEquals("license", message.decodePropertyKey(((char) 132) + ""));
        assertEquals("size", message.decodePropertyKey(((char) 133) + ""));
        assertEquals("creation_time", message.decodePropertyKey(((char) 134) + ""));
        assertEquals("vendor", message.decodePropertyKey(((char) 135) + ""));
        assertEquals("name", message.decodePropertyKey(((char) 136) + ""));
    }

    public void testPropertyKeyDottedEncode() {
        PromotionMessageContainer message = new PromotionMessageContainer();

        assertEquals("foo.bar", message.encodePropertyKey("foo.bar"));
        assertEquals("foo", message.encodePropertyKey("foo."));
        assertEquals(".bar", message.encodePropertyKey(".bar"));

        assertEquals("foo..jar", message.encodePropertyKey("foo..jar"));
        assertEquals("foo.bar", message.encodePropertyKey("foo.bar."));
        assertEquals("foo.bar", message.encodePropertyKey("foo.bar.."));
        assertEquals("foo.bar", message.encodePropertyKey("foo.bar..........."));
        assertEquals(".foo.bar", message.encodePropertyKey(".foo.bar"));

        assertEquals("f.b", message.encodePropertyKey("f.b"));
        assertEquals("f", message.encodePropertyKey("f."));
        assertEquals(".b", message.encodePropertyKey(".b"));

        assertEquals(((char) 128) + "" + ((char) 131), message.encodePropertyKey("artist.genre"));
        assertEquals(((char) 128) + "" + ((char) 131), message.encodePropertyKey("artist.genre."));
    }

    public void testPropertyKeyDottedDecode() {
        PromotionMessageContainer message = new PromotionMessageContainer();

        assertEquals("foo.bar", message.decodePropertyKey("foo.bar"));
        assertEquals("foo.", message.decodePropertyKey("foo."));
        assertEquals(".bar", message.decodePropertyKey(".bar"));

        assertEquals("foo.bar.jar", message.decodePropertyKey("foo.bar.jar"));
        assertEquals(".foo.bar", message.decodePropertyKey(".foo.bar"));

        assertEquals("f.b", message.decodePropertyKey("f.b"));
        assertEquals(".b", message.decodePropertyKey(".b"));

        assertEquals("artist.genre", message.decodePropertyKey(((char) 128) + "" + ((char) 131)));
    }

    public void testPropertyKeyDottedCycle() {
        dottedPropertyTest("foo.bar");
        dottedPropertyTest("foo..bar");
        dottedPropertyTest("foo...bar");
        dottedPropertyTest(".foo.bar");
        dottedPropertyTest("..foo.bar");
        dottedPropertyTest("...foo.bar");

        dottedPropertyTest("artist.genre");
        dottedPropertyTest(".artist.genre");
        dottedPropertyTest("..artist.genre");
        dottedPropertyTest("...artist.genre");
        dottedPropertyTest("...artist..genre");
        dottedPropertyTest("...artist...genre");
    }

    private void dottedPropertyTest(String key) {
        PromotionMessageContainer message = new PromotionMessageContainer();
        assertEquals(key, message.decodePropertyKey(message.encodePropertyKey(key)));
    }

    public void testUniqueID() {
        PromotionMessageContainer message = new PromotionMessageContainer();
        assertEquals(0, message.getUniqueID());
        message.setUniqueID(1);
        assertEquals(1, message.getUniqueID());
        message.setUniqueID(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, message.getUniqueID());
        message.setUniqueID(Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, message.getUniqueID());
    }

    public void testProbability() {
        PromotionMessageContainer message = new PromotionMessageContainer();
        message.setProbability(0);
        assertEquals(0.0F, message.getProbability());
        message.setProbability(1);
        assertEquals(1.0F, message.getProbability());
        message.setProbability(2);
        assertEquals(1.0F, message.getProbability());
        message.setProbability(-1);
        assertEquals(0.0F, message.getProbability());
        message.setProbability(.5F);
        assertLessThan(0.003, Math.abs(0.5 - message.getProbability()));

        for (float i = 0.0f; i <= 1.0f; i += .01) {
            message.setProbability(i);
            assertLessThan("Probability accuracy issue? i=" + i, 0.005, Math.abs(i
                    - message.getProbability()));
        }
    }

    public void testMediaType() {
        PromotionMessageContainer message = new PromotionMessageContainer();
        assertEquals(PromotionMediaType.UNKNOWN, message.getMediaType());

        for (PromotionMediaType type : PromotionMediaType.values()) {
            message.setMediaType(type);
            assertEquals(type, message.getMediaType());
        }
    }

    public void testOptionsBitmask() {
        PromotionMessageContainer message = new PromotionMessageContainer();
        assertTrue(message.getOptions().isOpenInNewWindow());
        assertFalse(message.getOptions().isMatchAllWords());

        PromotionOptions options = new PromotionOptions();
        options.setMatchAllWords(true);
        message.setOptions(options);
        assertTrue(message.getOptions().isOpenInNewWindow());
        assertTrue(message.getOptions().isMatchAllWords());

        options.setOpenInNewTab(true);
        message.setOptions(options);
        assertFalse(message.getOptions().isOpenInNewWindow());
        assertTrue(message.getOptions().isOpenInNewTab());

    }

    public void testEncodeFailsIfMissingFields() throws BadGGEPBlockException {
        PromotionMessageContainer message = new PromotionMessageContainer();
        try {
            message.encode();
            fail("Shouldn't have been able to getEncoded since we're missing required fields.");
        } catch (RuntimeException expected) {
            message.setOptions(new PromotionMessageContainer.PromotionOptions());
            message.setDescription("");
            message.setURL("http://foo");
            message.setKeywords("");
        }

        // This should work now, since we've set every field we think we need.
        message.decode(new GGEP(message.encode(), 0));
    }

    public void testGeoRestrictionWithin() {
        LatitudeLongitude latlon = new LatitudeLongitude(45, 45);
        PromotionMessageContainer.GeoRestriction gr = new GeoRestriction(latlon, 1000);
        assertTrue(gr.contains(latlon));
        assertFalse(gr.contains(new LatitudeLongitude(1, 1)));
    }

    public void testGeoRestrictionDecodeRadius() {
        assertEquals(169, PromotionMessageContainer.GeoRestriction.decodeRadius((byte) 0));
        assertEquals(676, PromotionMessageContainer.GeoRestriction.decodeRadius((byte) 1));
        assertEquals(11075584, PromotionMessageContainer.GeoRestriction.decodeRadius((byte) -1));
    }

    public void testGeoRestrictionEncodeRadius() {
        LatitudeLongitude latlon = new LatitudeLongitude(45, 45);
        PromotionMessageContainer.GeoRestriction gr = new GeoRestriction(latlon, 169);
        assertEquals(0, gr.getEncodedRadius());

        gr = new GeoRestriction(latlon, 676);
        assertEquals(1, gr.getEncodedRadius());
        gr = new GeoRestriction(latlon, 1520);
        assertEquals(1, gr.getEncodedRadius());

        gr = new GeoRestriction(latlon, 1521);
        assertEquals(2, gr.getEncodedRadius());

        gr = new GeoRestriction(latlon, 11075584);
        assertEquals(-1, gr.getEncodedRadius());
    }

    public void testGeoRestrictionByteConstruction() throws PromotionException {
        PromotionMessageContainer.GeoRestriction geo = new PromotionMessageContainer.GeoRestriction(
                new byte[] { 0, 0, 0, -128, 0, 0, 1 });
        assertTrue(geo.contains(new LatitudeLongitude(0, 180)));
        assertFalse(geo.contains(new LatitudeLongitude(0, 181)));
    }

    public void testGeoRestrictionsCycle() {
        PromotionMessageContainer message = new PromotionMessageContainer();
        List<GeoRestriction> list = new ArrayList<GeoRestriction>();
        list.add(new GeoRestriction(new LatitudeLongitude(180, 0), 2000));
        list.add(new GeoRestriction(new LatitudeLongitude(180, 180), 20000));
        list.add(new GeoRestriction(new LatitudeLongitude(0, 180), 2000000));
        message.setGeoRestrictions(list);

        List<GeoRestriction> list2 = message.getGeoRestrictions();
        assertEquals(3, list2.size());
        // Test the first entry
        assertTrue(list2.get(0).contains(new LatitudeLongitude(180, 0)));
        assertTrue(list2.get(0).contains(new LatitudeLongitude(180.013, 0)));
        assertFalse(list2.get(0).contains(new LatitudeLongitude(180.014, 0)));
        // Test the second entry
        assertTrue(list2.get(1).contains(new LatitudeLongitude(180, 180)));
        assertTrue(list2.get(1).contains(new LatitudeLongitude(180.152, 180)));
        assertFalse(list2.get(1).contains(new LatitudeLongitude(180.153, 180)));
        // Test the third entry
        assertTrue(list2.get(2).contains(new LatitudeLongitude(0, 180)));
        assertTrue(list2.get(2).contains(new LatitudeLongitude(0, 197.727)));
        assertFalse(list2.get(2).contains(new LatitudeLongitude(0, 197.728)));
    }
}
