package org.limewire.promotion.containers;

import java.util.Arrays;
import java.util.Date;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.util.StringUtils;

/**
 * Provides an abstract generic message container to store arbitrary key/value
 * pairs without the GGEP exception handling overhead.
 */
public abstract class MapMessageContainer implements MessageContainer {
    private GGEP payload = new GGEP();

    /**
     * Stores the given key/value pair, overwriting any previous value if
     * present. Neither key or value may be null.
     */
    protected void put(String key, String value) {
        if (value == null || key == null)
            throw new NullPointerException("key and value must not be null.");
        payload.put(key, StringUtils.toUTF8Bytes(value));
    }

    /**
     * Stores the given key/value pair, overwriting any previous value if
     * present. Neither key or value may be null.
     */
    protected void put(String key, Date value) {
        if (value == null || key == null)
            throw new NullPointerException("key and value must not be null.");
        payload.put(key, value.getTime());
    }

    /**
     * Stores the given key/value pair, overwriting any previous value if
     * present. Neither key or value may be null.
     */
    protected void put(String key, byte[] value) {
        if (value == null || key == null)
            throw new NullPointerException("key and value must not be null.");
        payload.put(key, value);
    }

    /**
     * Stores the given key/value pair, overwriting any previous value if
     * present. Key may not be null.
     */
    protected void put(String key, long value) {
        if (key == null)
            throw new NullPointerException("key must not be null.");
        payload.put(key, value);
    }

    /**
     * @return the value corresponding to the given key, or null if the key is
     *         not set or there is a problem parsing.
     */
    protected String getString(String key) {
        if (key == null)
            throw new NullPointerException("key must not be null.");
        if (!payload.hasValueFor(key))
            return null;
        return StringUtils.toUTF8String(payload.get(key));
    }

    /**
     * @return the value corresponding to the given key, or null if the key is
     *         not set or there is a problem parsing.
     */
    protected byte[] getBytes(String key) {
        if (key == null)
            throw new NullPointerException("key must not be null.");
        if (!payload.hasValueFor(key))
            return null;
        try {
            return payload.getBytes(key);
        } catch (BadGGEPPropertyException ex) {
            return null;
        }
    }

    /**
     * @return the value corresponding to the given key, or null if the key is
     *         not set or there is a problem parsing.
     */
    protected Long getLong(String key) {
        if (key == null)
            throw new NullPointerException("key must not be null.");
        if (!payload.hasValueFor(key))
            return null;
        try {
            return payload.getLong(key);
        } catch (BadGGEPPropertyException ex) {
            return null;
        }
    }

    /**
     * @return the value corresponding to the given key, or null if the key is
     *         not set or there is a problem parsing.
     */
    protected Date getDate(String key) {
        if (key == null)
            throw new NullPointerException("key must not be null.");
        if (!payload.hasValueFor(key))
            return null;
        try {
            return new Date(payload.getLong(key));
        } catch (BadGGEPPropertyException ex) {
            return null;
        }
    }

    public byte[] encode() {
        payload.put(TYPE_KEY, getType());
        return payload.toByteArray();
    }

    public void decode(GGEP rawGGEP) throws BadGGEPBlockException {
        if (!Arrays.equals(getType(), rawGGEP.get(TYPE_KEY)))
            throw new BadGGEPBlockException("Incorrect type.");
        this.payload = rawGGEP;
    }

}
