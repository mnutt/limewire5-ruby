package org.limewire.promotion.containers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.promotion.LatitudeLongitude;
import org.limewire.promotion.exceptions.PromotionException;
import org.limewire.util.ByteUtils;
import org.limewire.util.StringUtils;

/**
 * Instances of this class are messages that contain the keywords, target URL
 * and restrictions that define a promotion.
 */
public class PromotionMessageContainer implements MessageContainer, Serializable {
    /**
     * Since internal dates are stored in 4-bytes, seconds since Unix epoch,
     * this is the maximum value that can be encoded, which works out to be
     * January 18, 2038. Hello Y2K38.
     */
    public static final long MAX_DATE_IN_SECONDS = 2147483647L;

    private static final String KEY_HEADER = "H";

    private static final String KEY_TERRITORIES = "T";

    private static final String KEY_DESCRIPTION = "D";
    
    private static final String KEY_TITLE = "t";
    
    private static final String KEY_DISPLAY_URL = "u";

    private static final String KEY_URL = "U";

    private static final String KEY_KEYWORDS = "K";

    private static final String KEY_GEO_RESTRICT = "G";

    private static final String KEY_DATE_RANGE = "d";

    private static final String KEY_PROPERTIES = "P";
    
    private static final String KEY_IMPRESS_ONLY = "I";

    private GGEP payload = new GGEP();

    public byte[] getType() {
        return StringUtils.toUTF8Bytes("P");
    }

    /* Throws a RTE if we're missing any required fields. */
    public byte[] encode() {
        payload.put(TYPE_KEY, getType());
        if (!payload.hasKey(KEY_HEADER))
            throw new RuntimeException("Missing header");
        if (!payload.hasKey(KEY_TERRITORIES))
            throw new RuntimeException("Missing territories");
        if (!payload.hasKey(KEY_DESCRIPTION))
            throw new RuntimeException("Missing description");
        if (!payload.hasKey(KEY_URL))
            throw new RuntimeException("Missing URL");
        if (!payload.hasKey(KEY_KEYWORDS))
            throw new RuntimeException("Missing keywords");

        return payload.toByteArray();
    }

    /**
     * A long that specifies a globally unique ID for this promo.
     */
    public void setUniqueID(long id) {
        byte[] header = getHeader();
        ByteUtils.long2beb(id, header, 0);
        setHeader(header);
    }
    
    public long getUniqueID() {
        byte[] header = getHeader();
        return ByteUtils.beb2long(header, 0, 8);
    }

    /**
     * a goodness ratio, between 0 and 1. If greater than 1, sets to 1. If less
     * than 0, sets to 0. This is an approximation with only 8 bits of
     * resolution.
     */
    public void setProbability(float probability) {
        if (probability < 0)
            probability = 0;
        if (probability > 1)
            probability = 1;
        byte probByte = (byte) ((probability * 255) - 128);
        byte[] header = getHeader();
        header[9] = probByte;
        setHeader(header);
    }

    public float getProbability() {
        byte[] header = getHeader();
        byte probByte = header[9];
        return (probByte + 128) / 255.0F;
    }

    public PromotionMediaType getMediaType() {
        byte[] header = getHeader();
        byte typeByte = header[8];
        return PromotionMediaType.getInstance(typeByte);
    }

    public void setMediaType(PromotionMediaType type) {
        byte[] header = getHeader();
        header[8] = type.getValue();
        setHeader(header);
    }
    
    public boolean isImpressionOnly() {
        return payload.hasKey(KEY_IMPRESS_ONLY);
    }
    
    public void setImpressionOnly(boolean impressOnly) {
        if(impressOnly)
            payload.put(KEY_IMPRESS_ONLY);
        else
            payload.getHeaders().remove(KEY_IMPRESS_ONLY);
    }

    public void setOptions(PromotionOptions options) {
        byte[] header = getHeader();
        // How many bytes we have for the bitmask.
        int bitmaskLength = header.length - 10;
        byte[] mask = new byte[bitmaskLength];

        if (mask.length > 0) {
            // We have a byte to fill
            if (options.isMatchAllWords())
                mask[0] = (byte) (mask[0] | 1);
            if (options.isOpenInNewTab())
                mask[0] = (byte) (mask[0] | 2);
            if (options.isOpenInHomeTab())
                mask[0] = (byte) (mask[0] | 4);
            if (options.isOpenInStoreTab())
                mask[0] = (byte) (mask[0] | 8);
            if (options.isOpenInUnknownTab())
                mask[0] = (byte) (mask[0] | 16);
        }

        // Now we have the mask. Copy it back to the header.
        System.arraycopy(mask, 0, header, 10, bitmaskLength);
        setHeader(header);
    }

    public PromotionOptions getOptions() {
        byte[] header = getHeader();
        PromotionOptions options = new PromotionOptions();
        if (header.length > 10) {
            byte mask = header[10];
            options.setMatchAllWords((mask & 1) > 0);
            options.setOpenInNewTab((mask & 2) > 0);
            options.setOpenInHomeTab((mask & 4) > 0);
            options.setOpenInStoreTab((mask & 8) > 0);
            options.setOpenInUnknownTab((mask & 16) > 0);
        }
        return options;
    }

    /** Sets the payload with the given header. Should be 11 bytes or bigger. */
    private void setHeader(byte[] header) {
        if (header == null || header.length < 11)
            throw new IllegalArgumentException("header must be at least 11 bytes long.");
        payload.put(KEY_HEADER, header);
    }

    /**
     * Gets the header bytes from the payload, or creates them freshly with
     * defaults set if there is a problem parsing them.
     */
    private byte[] getHeader() {
        byte[] header = null;
            try {
                if (payload.hasKey(KEY_HEADER))
                header = payload.getBytes(KEY_HEADER);
            } catch (BadGGEPPropertyException ignored) {
            }
        if (header == null || header.length < 11)
            header = new byte[11];
        return header;
    }

    /**
     * Set the territory list. Takes the #getCountry() value from each locale,
     * discarding any other info.
     */
    public void setTerritories(Locale... locales) {
        StringBuilder countries = new StringBuilder();
        for (Locale locale : locales)
            countries.append(locale.getCountry());
        payload.put(KEY_TERRITORIES, StringUtils.toUTF8Bytes(countries.toString()));
    }

    /**
     * @return an array of 0 or more {@link Locale} instances with their country
     *         property set to a two-character ISO country code.
     */
    public Locale[] getTerritories() {
        List<Locale> territoryList = new ArrayList<Locale>();
        String territories;
        try {
            territories = StringUtils.toUTF8String(payload.getBytes(KEY_TERRITORIES));
        } catch (BadGGEPPropertyException ex) {
            throw new RuntimeException("GGEP exception parsing territories.", ex);
        }
        for (int i = 0; i < territories.length() - 1; i += 2)
            territoryList.add(new Locale("", territories.substring(i, i + 2)));
        return territoryList.toArray(new Locale[territoryList.size()]);
    }

    /**
     * The description to display to the user. Setting this to null sets it to
     * "".
     */
    public void setDescription(String description) {
        set(KEY_DESCRIPTION, description);
    }

    public String getDescription() {
        return get(KEY_DESCRIPTION);
    }
    
    public void setTitle(String title) {
        set(KEY_TITLE, title);
    }

    public String getTitle() {
        return get(KEY_TITLE);
    }
    
    public void setDisplayUrl(String displayUrl) {
        set(KEY_DISPLAY_URL, displayUrl);
    }
    
    public String getDisplayUrl() {
        return get(KEY_DISPLAY_URL);
    }

    /**
     * The keywords to search by, in encoded format (See the keyword parser
     * elsewhere for format). Setting this to null sets it to "".
     */
    public void setKeywords(String keywords) {
        set(KEY_KEYWORDS, keywords);
    }

    public String getKeywords() {
        return get(KEY_KEYWORDS);
    }

    /**
     * The url to direct the user to . Setting this to null sets it to "".
     */
    public void setURL(String url) {
        set(KEY_URL, url);
    }

    public String getURL() {
        return get(KEY_URL);
    }

    /**
     * Sets the properties for this promotion. OPTIONAL. Keys must obviously not
     * be null. Null values are ignored and not encoded into the final message.
     * Keys with trailing "." characters are trimmed to remove them.
     */
    public void setProperties(Map<String, String> properties) {
        StringBuilder builder = new StringBuilder();
        for (String key : properties.keySet()) {
            String value = properties.get(key);
            if (value == null)
                continue;
            builder.append(encodePropertyKey(key)).append('=').append(value).append('\t');
        }
        if (builder.length() == 0) {
            payload.put(KEY_PROPERTIES, new byte[0]);
            return;
        }
        // Put the string into the ggep, but strip the last character (which is
        // always a tab)
        byte[] encodedProperties = StringUtils.toUTF8Bytes(builder.substring(0,
                builder.length() - 1));
        payload.put(KEY_PROPERTIES, encodedProperties);
    }

    /**
     * if the key is recognized as an encodable key, returns a shortened
     * version. Otherwise returns the original key. Package visible for testing.
     */
    String encodePropertyKey(String key) {
        if (key != null && key.indexOf(".") != -1 && !key.equals("."))
            return encodeDottedPropertyKey(key);
        for (int i = 0; i < PROPERTY_ENCODING_ARRAY.length; i++)
            if (PROPERTY_ENCODING_ARRAY[i].equals(key))
                return new String(((char) (i + 128)) + "");
        return key;
    }

    /**
     * takes a key like "xxx.yyy.zzz" splits it and encodes each token
     * individually, then rejoins them.
     */
    private String encodeDottedPropertyKey(String key) {
        StringTokenizer tokens = new StringTokenizer(key, ".", true);
        StringBuilder builder = new StringBuilder();
        boolean lastTokenWasCompressed = false;
        while (tokens.hasMoreTokens()) {
            String token = encodePropertyKey(tokens.nextToken());
            if (!token.equals(".") || !lastTokenWasCompressed)
                builder.append(token);
            lastTokenWasCompressed = (token.length() == 1 && token.charAt(0) >= 128);
        }

        return trimTrailingPeriods(builder);
    }

    private String trimTrailingPeriods(StringBuilder builder) {
        if (builder.charAt(builder.length() - 1) == '.') {
            builder.deleteCharAt(builder.length() - 1);
            return trimTrailingPeriods(builder);
        }
        return builder.toString();
    }

    /** ONLY ADD TO THIS LIST, AND ADD AT THE END. ORDERING IS SUPER-IMPORTANT. */
    private static final String[] PROPERTY_ENCODING_ARRAY = new String[] { "artist", "album",
            "url", "genre", "license", "size", "creation_time", "vendor", "name", "audio", "video",
            "document" };

    /**
     * If the key is decodable, returns the lengthened version, otherwise
     * returns the original key. Package visible for testing.
     */
    String decodePropertyKey(String key) {
        if (key == null || key.length() == 0)
            return key;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            int index = key.charAt(i);
            if (index >= 128) {
                index -= 128;
                if (index < PROPERTY_ENCODING_ARRAY.length) {
                    builder.append(PROPERTY_ENCODING_ARRAY[index]);
                    builder.append(".");
                }
            } else {
                builder.append((char) index);
            }
        }
        if (builder.charAt(builder.length() - 1) == '.' && !key.endsWith("."))
            return builder.substring(0, builder.length() - 1);
        else
            return builder.toString();
    }

    /**
     * @return a map of properties, and an empty map if no properties have been
     *         set or the field has an encoding error. Never null.
     */
    public Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<String, String>();
        try {
            String encoded = StringUtils.toUTF8String(payload.getBytes(KEY_PROPERTIES));
            StringTokenizer tokens = new StringTokenizer(encoded, "\t");
            while (tokens.hasMoreTokens()) {
                String token = tokens.nextToken();
                if (token.indexOf('=') > 0) {
                    String key = token.substring(0, token.indexOf('='));
                    String value = token.substring(key.length() + 1);
                    properties.put(decodePropertyKey(key), value);
                }
            }
        } catch (BadGGEPPropertyException ignored) {
        }
        return properties;
    }

    /** Sets the {@link GeoRestriction} list for this message. Optional. */
    public void setGeoRestrictions(List<GeoRestriction> restrictions) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (GeoRestriction restriction : restrictions)
            try {
                out.write(restriction.toBytes());
            } catch (IOException ignored) {
                // This stream won't throw this
            }
        payload.put(KEY_GEO_RESTRICT, out.toByteArray());
    }

    /**
     * Gets a list of {@link GeoRestriction} entries for this message, or an
     * empty list if there are none.
     */
    public List<GeoRestriction> getGeoRestrictions() {
        List<GeoRestriction> list = new ArrayList<GeoRestriction>();
        if (payload.hasValueFor(KEY_GEO_RESTRICT)) {
            byte[] encoded = payload.get(KEY_GEO_RESTRICT);
            for (int i = 0; i < encoded.length - 6; i += 7) {
                byte[] geoBytes = new byte[7];
                System.arraycopy(encoded, i, geoBytes, 0, 7);
                try {
                    list.add(new GeoRestriction(geoBytes));
                } catch (PromotionException ex) {
                    // Only happens if we miscalculated the array size
                    throw new RuntimeException(
                            "PromotionException while parsing geo restrictions.", ex);
                }
            }
        }
        return list;
    }

    /**
     * Sets an individual validity range for this promotion, OPTIONAL (if not
     * set, promotion inherits validity range from its parent container).
     * 
     * @param start when this promo becomes valid. Cannot be past the parent
     *        container's end date, or will never be valid. If null, defaults to
     *        now.
     */
    public void setValidStart(Date start) {
        setValidRange(start, getValidEnd());
    }

    /**
     * Sets an individual validity range for this promotion, OPTIONAL (if not
     * set, promotion inherits validity range from its parent container).
     * 
     * @param end when this promo expires. If null, the promo will expire at the
     *        end of the parent container's validity period.
     */
    public void setValidEnd(Date end) {
        setValidRange(getValidStart(), end);
    }

    /**
     * @param start when this promo becomes valid. Cannot be past the parent
     *        container's end date, or will never be valid. If null, defaults to
     *        now.
     * @param end when this promo expires. If null, the promo will expire at the
     *        end of the parent container's validity period.
     */
    private void setValidRange(Date start, Date end) {
        if (start == null)
            start = new Date();
        if (end == null)
            end = new Date(MAX_DATE_IN_SECONDS * 1000);
        byte[] range = new byte[8];
        byte[] startBytes = ByteUtils.long2bytes(start.getTime() / 1000, 4);
        byte[] endBytes = ByteUtils.long2bytes(end.getTime() / 1000, 4);
        System.arraycopy(startBytes, 0, range, 0, 4);
        System.arraycopy(endBytes, 0, range, 4, 4);
        payload.put(KEY_DATE_RANGE, range);
    }

    /**
     * If not set or there is trouble parsing the field, returns the earliest
     * possible date, which will be overridden by the parent container's start date.
     */
    public Date getValidStart() {
        // Date is stored as a 4 byte long, seconds since UNIX epoch.
        try {
            byte range[] = payload.getBytes(KEY_DATE_RANGE);
            byte start[] = new byte[4];
            System.arraycopy(range, 0, start, 0, 4);
            long startLong = ByteUtils.beb2long(start, 0, 4);
            return new Date(startLong * 1000);
        } catch (BadGGEPPropertyException ex) {
            return new Date(0);
        }
    }

    /**
     * If not set or there is trouble parsing the field, returns a date far in
     * the future which will be overwritten by the parent container's expiration
     * date.
     */
    public Date getValidEnd() {
        // Date is stored as a 4 byte long, seconds since UNIX epoch.
        try {
            byte range[] = payload.getBytes(KEY_DATE_RANGE);
            byte end[] = new byte[4];
            System.arraycopy(range, 4, end, 0, 4);
            long endLong = ByteUtils.beb2long(end, 0, 4);
            return new Date(endLong * 1000);
        } catch (BadGGEPPropertyException ex) {
            return new Date(MAX_DATE_IN_SECONDS * 1000L);
        }
    }

    /** Parses out the given key, or returns "" if the key is not present. */
    private String get(String key) {
        try {
            if (!payload.hasValueFor(key)) {
                return "";
            } else {
                return StringUtils.toUTF8String(payload.getBytes(key));
            }
        } catch (BadGGEPPropertyException ex) {
            throw new RuntimeException("GGEP exception parsing value.", ex);
        }
    }

    /**
     * Set the given key to the given value encoded to UTF-8. Setting to null
     * sets the value to "".
     */
    private void set(String key, String value) {
        payload.put(key, StringUtils.toUTF8Bytes(value));
    }

    public void decode(GGEP rawGGEP) throws BadGGEPBlockException {
        if (!Arrays.equals(getType(), rawGGEP.get(TYPE_KEY)))
            throw new BadGGEPBlockException("Incorrect type.");
        if (!rawGGEP.hasKey(KEY_HEADER))
            throw new BadGGEPBlockException("Missing header");
        if (!rawGGEP.hasKey(KEY_TERRITORIES))
            throw new BadGGEPBlockException("Missing territories");
        if (!rawGGEP.hasKey(KEY_DESCRIPTION))
            throw new BadGGEPBlockException("Missing description");
        if (!rawGGEP.hasKey(KEY_URL))
            throw new BadGGEPBlockException("Missing URL");
        if (!rawGGEP.hasKey(KEY_KEYWORDS))
            throw new BadGGEPBlockException("Missing keywords");

        this.payload = rawGGEP;
    }

    /**
     * Checks the UID, validity dates, keywords and URL to decide equality.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PromotionMessageContainer))
            return false;
        PromotionMessageContainer compare = (PromotionMessageContainer) obj;
        if (!new Long(getUniqueID()).equals(compare.getUniqueID()))
            return false;
        if (!getKeywords().equals(compare.getKeywords()))
            return false;
        if (!getURL().equals(compare.getURL()))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return (int) getUniqueID();
    }

    public static enum PromotionMediaType {
        /** audio content. */
        AUDIO(1),
        /** video content. */
        VIDEO(2),
        /** LimeWire Store content. */
        STORE(3),
        /** LimeSpot content. */
        SPOT(4),
        /** A browser link of no special type. */
        GENERIC_LINK(5),
        /**
         * UNKNOWN is the default for if A) the type is not known for B) the
         * promotion specifies a media type that this client does not know how
         * to parse.
         */
        UNKNOWN(0);

        private byte value;

        /** The value this enum encodes to in the message. */
        public byte getValue() {
            return value;
        }

        private PromotionMediaType(int value) {
            this.value = (byte) value;
        }

        /**
         * An instance of this enum, or UNKNOWN if the value cannot be mapped to
         * an enum.
         */
        static PromotionMediaType getInstance(byte value) {
            for (PromotionMediaType type : PromotionMediaType.values()) {
                if (type.value == value)
                    return type;
            }
            return UNKNOWN;
        }
    }

    public static class GeoRestriction {
        private LatitudeLongitude center;

        private int radiusInMeters;

        /**
         * Constructs an instance with the given center point and radius.
         */
        public GeoRestriction(LatitudeLongitude center, int radiusInMeters) {
            this.center = center;
            this.radiusInMeters = radiusInMeters;
        }

        /**
         * Decodes a 7-byte array, first 3 bytes are latitude, second 3 are
         * longitude, and final byte encodes radius using the rules discussed in
         * {@link #getEncodedRadius()}. Package visible for unit testing, but
         * should only be called during promo message parsing.
         * 
         * @throws PromotionException if the array is not exactly 7 bytes long.
         */
        GeoRestriction(byte[] bytes) throws PromotionException {
            if (bytes == null || bytes.length != 7)
                throw new PromotionException("expected exactly 7 bytes for construction.");
            byte[] lat = new byte[3];
            byte[] lon = new byte[3];
            System.arraycopy(bytes, 0, lat, 0, 3);
            System.arraycopy(bytes, 3, lon, 0, 3);

            this.center = new LatitudeLongitude(lat, lon);
            this.radiusInMeters = decodeRadius(bytes[6]);
        }

        /**
         * @return true if point is within this restriction.
         */
        public boolean contains(LatitudeLongitude point) {
            return center.distanceFrom(point) <= (radiusInMeters / 1000.0);
        }

        /**
         * @return a 7-byte array, the first 3 bytes encode latitude, next 3
         *         encode longitude, and final byte encodes radius using the
         *         following formula (b is byte, 1-256): (b*13)^2 meters
         */
        public byte[] toBytes() {
            byte[] bytes = new byte[7];
            System.arraycopy(center.toBytes(), 0, bytes, 0, 6);
            bytes[6] = getEncodedRadius();
            return bytes;
        }

        /**
         * Encodes the radius to a single byte. The byte can be inflated by
         * multiplying it by 13 and then squaring the result. Package visible
         * for unit testing.
         */
        byte getEncodedRadius() {
            long value = (long) (Math.sqrt(radiusInMeters) / 13);
            return ByteUtils.long2bytes(value - 1, 1)[0];
        }

        /**
         * @return the radius in meters that the byte represents, as defined by
         *         {@link #getEncodedRadius()}.
         */
        static int decodeRadius(byte radiusByte) {
            long radius = ByteUtils.beb2long(new byte[] { radiusByte }, 0, 1) + 1;
            return (int) Math.pow(13 * radius, 2);
        }
    }

    /** Bean that represents the options bit mask. */
    public static class PromotionOptions {
        private boolean matchAllWords = false;

        private boolean openInNewTab = false;

        private boolean openInHomeTab = false;

        private boolean openInStoreTab = false;

        private boolean openInUnknownTab = false;

        /**
         * @return if true, query should match all words, otherwise any words.
         */
        public boolean isMatchAllWords() {
            return matchAllWords;
        }

        public void setMatchAllWords(boolean matchAllWords) {
            this.matchAllWords = matchAllWords;
        }

        /**
         * Not a settable property, goes to true if all the other browser
         * options are false.
         * 
         * @return if true, open the link in a new (external to LW) browser
         *         window.
         */
        public boolean isOpenInNewWindow() {
            return !(openInStoreTab || openInNewTab || openInHomeTab || openInUnknownTab);
        }

        /**
         * @return if true and LW supports tabs, open a new tab for this
         *         browser.
         */
        public boolean isOpenInNewTab() {
            return openInNewTab;
        }

        public void setOpenInNewTab(boolean openInNewTab) {
            this.openInNewTab = openInNewTab;
        }

        /**
         * @return if true and LW supports a "Store" tab, open this into that
         *         tab, creating it if it's not already open.
         */
        public boolean isOpenInStoreTab() {
            return openInStoreTab;
        }

        public void setOpenInStoreTab(boolean openInStoreTab) {
            this.openInStoreTab = openInStoreTab;
        }

        /**
         * @return if true and LW supports a "Spot" tab, open this into that
         *         tab, creating it if it's not already open.
         */
        public boolean isOpenInUnknownTab() {
            return openInUnknownTab;
        }

        public void setOpenInUnknownTab(boolean openInUnknownTab) {
            this.openInUnknownTab = openInUnknownTab;
        }

        /**
         * @return if true and LW supports a "Client" (browser) tab, open this
         *         into that tab, creating it if it's not already open.
         */
        public boolean isOpenInHomeTab() {
            return openInHomeTab;
        }

        public void setOpenInHomeTab(boolean openInHomeTab) {
            this.openInHomeTab = openInHomeTab;
        }
    }

}
