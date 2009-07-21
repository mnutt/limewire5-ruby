package org.limewire.facebook.service;

import org.json.JSONException;
import org.json.JSONObject;

public class FacebookUtils {

    /**
     * Returns a random elemement from <code>array</code> using
     * {@link Math#random()}.
     */
    public static final <T> T getRandomElement(T[] array) {
        return array[(int)Math.floor(Math.random()*array.length)];
    }
    
    /**
     * Parses a string into a {@link JSONObject} catching any {@link JSONException}
     * and rethrowing it including the input text.
     */
    public static final JSONObject parse(String input) throws JSONException {
        try {
            return new JSONObject(input);
        } catch (JSONException e) {
            JSONException copy = new JSONException(input);
            copy.initCause(e);
            throw copy;
        }
    }
}
