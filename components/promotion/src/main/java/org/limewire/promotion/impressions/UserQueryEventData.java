package org.limewire.promotion.impressions;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.util.ByteUtils;
import org.limewire.util.StringUtils;

/**
 * This is the data sent to the server. It includes a String query, and binary
 * data for the time and impressions.
 */
public class UserQueryEventData {

    private final UserQueryEvent event;

    private final long millisSinceToday;

    public UserQueryEventData(UserQueryEvent event, long millisSinceToday) {
        this.event = event;
        this.millisSinceToday = millisSinceToday;
    }

    public UserQueryEventData(UserQueryEvent event) {
        this(event, millisecondsSinceTheStartOfToday());
    }

    public String getQuery() {
        return event.getOriginalQuery();
    }

    // TODO: Improve to not include all the array creation
    public byte[] getData() {
        List<Impression> impressions = event.getImpressions();
        /*
         * Format:
         *  
         *   number of impressions               : 1 byte
         *   milliseconds since start of the day : 4 bytes 
         *   original query time                 : 8 bytes  
         *   List of n=length of
         *     n=binder name length     : 1 byte 
         *     binder name              : n bytes 
         *     promo ID                 : 8 bytes
         *     impression time          : 8 bytes
         */
        int length = 1 + 4 + 8;
        for (Impression imp : impressions) {
            length += 1;
            length += StringUtils.toAsciiBytes(imp.getBinderUniqueName()).length;
            length += 8;
            length += 8;
        }
        byte[] bytes = new byte[length];
        AtomicInteger inc = new AtomicInteger(0);
        bytes[inc.getAndAdd(1)] = (byte) (0xff & impressions.size());
        System.arraycopy(ByteUtils.long2bytes(millisSinceToday, 4), 0, bytes, inc.getAndAdd(4), 4);
        System.arraycopy(ByteUtils.long2bytes(event.getOriginalQueryTime().getTime(), 8), 0,
                bytes, inc.getAndAdd(8), 8);
        for (int i = 0; i < impressions.size(); i++) {
            Impression imp = impressions.get(i);
            byte[] binderName = StringUtils.toAsciiBytes(imp.getBinderUniqueName());
            bytes[inc.getAndAdd(1)] = (byte) (0xff & binderName.length);
            System.arraycopy(binderName, 0, bytes, inc.getAndAdd(binderName.length),
                    binderName.length);
            System.arraycopy(ByteUtils.long2bytes(imp.getPromoUniqueID(), 8), 0, bytes,
                    inc.getAndAdd(8), 8);
            System.arraycopy(ByteUtils.long2bytes(imp.getTimeShown().getTime(), 8), 0, bytes,
                    inc.getAndAdd(8), 8);
        }
        return bytes;
    }

    /**
     * Returns the number of milliseconds since the start of the today. This is
     * exposed, so we can get it during testing.
     * 
     * @return the number of milliseconds since the start of the today.
     */
    long getMillisSinceToday() {
        return millisSinceToday;
    }

    /**
     * Returns the milliseconds since the start of today.
     * 
     * @return
     */
    static int millisecondsSinceTheStartOfToday() {
        return (int) (System.currentTimeMillis() - getFirstMillisecond());
    }

    static long getFirstMillisecond() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        Calendar cal = new GregorianCalendar(year, 0, 1, 0, 0, 0);
        return cal.getTime().getTime();
    }

}
