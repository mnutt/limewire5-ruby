package org.limewire.lws.server;

import java.net.InetAddress;
import java.net.URLDecoder;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;

/**
 * Utility methods for this package.
 */
public final class LWSServerUtil {

    private final static String ERROR_START = "ERROR:";
    
    final static Map<String, String> EMPTY_MAP_OF_STRING_X_STRING = new HashMap<String, String>();

    private LWSServerUtil() {
        // nothing
    }

    /**
     * Returns <code>true</code> if <code>s</code> is <code>null</code> or
     * <code>""</code>, otherwise <code>false</code>.
     * 
     * @param s {@link String} in question
     * @return <code>true</code> if <code>s</code> is <code>null</code> or
     *         <code>""</code>, otherwise <code>false</code>.
     */
    public static boolean isEmpty(final String s) {
        return s == null || s.equals("");
    }

    /**
     * Parses and returns CGI arguments.
     * 
     * @param rest the string to parse
     * @return CGI arguments from <tt>rest</tt>
     */
    public static Map<String, String> parseArgs(final String rest) {
        if (isEmpty(rest))
            return Collections.emptyMap();
        final Map<String, String> res = new HashMap<String, String>(3);
        for (StringTokenizer st = new StringTokenizer(rest, "&", false); st
                .hasMoreTokens();) {
            final String pair = st.nextToken();
            final int ieq = pair.indexOf('=');
            String key, val;
            if (ieq == -1) {
                key = pair;
                val = null;
            } else {
                key = pair.substring(0, ieq);
                val = pair.substring(ieq + 1);
            }
            res.put(key.trim(), val == null ? val : val.trim());
        }
        return res;
    }

    /**
     * Returns a string suitable for the date for a cookie.
     * 
     * @return a string suitable for the date for a cookie
     */
    public static String createCookieDate() {
        // Wdy, DD-Mon-YYYY HH:MM:SS GMT
        Format f = new SimpleDateFormat("E, dd-MM-yyyy kk:mm:ss");
        Date date = Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime();
        return f.format(date) + " GMT";
    }

    /**
     * Returns the simple name for the class <tt>c</tt>.
     * 
     * @param c the class in question
     * @return the simple name for the class <tt>c</tt>
     */
    static String simpleName(final Class c) {
        String s = c.getName();
        int ilastDot = s.lastIndexOf(".");
        return ilastDot == -1 ? s : s.substring(ilastDot + 1);
    }

    

    /**
     * Returns the error code found in {@link ErrorCodes} or <tt>null</tt> if it's
     * not an error.
     * 
     * @param line {@link String} containing an error to unwrap
     * @return error code found in {@link ErrorCodes} or <tt>null</tt> if it's
     *         not an error
     */
    public static String unwrapError(final String line) {
        if (line.startsWith(ERROR_START)) {
            return line.substring(ERROR_START.length()).trim();
        } else {
            return null;
        }
    }

    /**
     * Returns the appropriate string for identifying an error.
     * 
     * @param error the error message
     * @return the appropriate string for identifying an error
     */
    public static String wrapError(final String error) {
        return ERROR_START + error;
    }

    /**
     * Will remove the method application to <tt>res</tt>.
     * 
     */
    public static String removeCallback(final String res) {
        if (res == null) return null;
        String start = "(" + LWSDispatcherSupport.Constants.CALLBACK_QUOTE_STRING;
        String end = LWSDispatcherSupport.Constants.CALLBACK_QUOTE_STRING + ")";
        int istart = res.indexOf(start);
        if (istart == -1) return res;
        int iend = res.lastIndexOf(end);
        if (iend == -1) return res;
        return res.substring(istart + start.length(), iend);
    }

    private static boolean isValidKey(final String key) {
        if (key == null || key.equals("")) return false;
        //
        // No periods, this is an indication of an error
        //
        for (int i=0, I=key.length(); i<I; i++) {
            if (key.charAt(i) == '.') return false;
        }
        return true;
    }

    /**
     * Returns <tt>true</tt> if <tt>key</tt> is a valid public key,
     * <tt>false</tt> otherwise.
     * 
     * @param key key in question
     * @return <tt>true</tt> if <tt>key</tt> is a valid public key,
     *         <tt>false</tt> otherwise
     */
    public static boolean isValidPublicKey(final String key) {
        return isValidKey(key);
    }

    /**
     * Returns <tt>true</tt> if <tt>key</tt> is a valid shared key,
     * <tt>false</tt> otherwise.
     * 
     * @param key key in question
     * @return <tt>true</tt> if <tt>key</tt> is a valid shared key,
     *         <tt>false</tt> otherwise
     */
    public static boolean isValidSharedKey(final String key) {
        return isValidKey(key);
    }    

    /**
     * Returns <tt>true</tt> if <tt>key</tt> is a valid private key,
     * <tt>false</tt> otherwise.
     * 
     * @param key key in question
     * @return <tt>true</tt> if <tt>key</tt> is a valid private key,
     *         <tt>false</tt> otherwise
     */
    public static boolean isValidPrivateKey(final String key) {
        return isValidKey(key);
    }

    /**
     * Returns the string IP address for the given address.
     * 
     * @param addr address in question
     * @return the string IP address for the given address
     */
    public static String getIPAddress(final InetAddress addr) {
        if (addr == null)
            return null;
        byte[] bs = addr.getAddress();
        return NetworkUtils.ip2string(bs);
    }

    /**
     * Generates a public or private key. <br>
     * INVARIANT: {@link #isValidPublicKey(String)}({@link #generateKey()})
     * <tt> == true</tt> and {@link #isValidPrivateKey(String)}({@link #generateKey()})
     * <tt> == true</tt>
     * 
     * @return a public or private key
     */
    public static String generateKey() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LWSDispatcherSupport.Constants.KEY_LENGTH;) {
            final int r = 'A' + (int) (Math.random() * ('Z' - 'A'));
            final char c = (char) r;
            if (c == ';')
                continue;
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    /**
     * Returns a string with the urlencoded arguments removed with these
     * arguments separated by {@link Constants#ARGUMENT_SEPARATOR}. This method
     * has the side-affect of putting these urlencoded arguments into
     * <tt>args</tt>. Arguments with no equals sign will have
     * <code>null</code> values, arguments with nothing after the equal sign
     * will have an empty value, non-empty arguments will be as expected. Here
     * are a couple examples:
     * <ul>
     * <li>one=1 &rarr; <code>{one=1}</code></li>
     * <li>one= &rarr; <code>{one=}</code></li>
     * <li>one &rarr; <code>{one=null}</code></li>
     * </ul>
     * 
     * @param cmd original command, this can be <code>null</code>
     * @param args original arguments (SIDEEFFECT: these are updated)
     * @return a string with the urlencoded arguments removed with these
     *         arguments separated by {@link Constants#ARGUMENT_SEPARATOR}
     */
    public static String addURLEncodedArguments(final String cmd,
            final Map<String, String> args) {
        if (cmd == null) return null;

        int ihuh = cmd.indexOf("?");
        if (ihuh == -1) return cmd;

        final String newCmd = cmd.substring(0, ihuh);
        //
        // We have to decode the rest because the commands are identified using
        // CGI parameters, but the value of the arguments are often parameters,
        // and get encoded.
        //
        String tmpRest = cmd.substring(ihuh + 1);
        try {
            tmpRest = URLDecoder.decode(tmpRest, "UTF-8");
        } catch (Exception e) { ErrorService.error(e); }
        final String rest = tmpRest;
        for (StringTokenizer st = new StringTokenizer(rest, "&", false); 
             st.hasMoreTokens();) {
            final String tok = st.nextToken();
            int ieq = tok.indexOf('=');
            String key, val;
            if (ieq == -1) {
                key = tok;
                val = null;
            } else {
                key = tok.substring(0, ieq);
                val = tok.substring(ieq + 1);
            }
            args.put(key, val);

        }
        return newCmd;

    }

    /**
     * Returns <code>true</code> is <code>res</code> isn't <code>null</code>
     * and starts with {@link #ERROR_START}.
     * 
     * @param res the {@link String} in question
     * @return <code>true</code> is <code>res</code> isn't <code>null</code>
     *         and starts with {@link #ERROR_START}
     */
    public static boolean isError(String res) {
        return res != null && res.startsWith(ERROR_START);
    }
}
