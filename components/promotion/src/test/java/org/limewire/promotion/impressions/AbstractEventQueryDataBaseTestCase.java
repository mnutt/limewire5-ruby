package org.limewire.promotion.impressions;

import java.util.Date;

import org.limewire.promotion.containers.PromotionMessageContainer;
import org.limewire.util.BaseTestCase;
import org.limewire.util.ByteUtils;

abstract class AbstractEventQueryDataBaseTestCase extends BaseTestCase {

    public AbstractEventQueryDataBaseTestCase(String name) {
        super(name);
    }

    private final String A_QUERY = "the query";

    private final long DATE_IN_MILLIS = 123123123123L;

    private final Date A_DATE = new Date(DATE_IN_MILLIS);

    protected final UserQueryEventData getImpressions(int n) {

        UserQueryEvent e = getUserQueryEvent(n);
        UserQueryEventData data = new UserQueryEventData(e);
        /*
         *   milliseconds since start of the day : 4 bytes 
         *   original query time                 : 8 bytes 
         *   number of impressions               : 1 byte 
         *   List of n=length of
         *     n=binder name length    : 1 byte 
         *     binder Name             : n bytes 
         *     promo ID                : 8 bytes
         *     impression time         : 8 bytes
         */
        int length = 4 + 8 + 1;
        for (Impression imp : e.getImpressions()) {
            length += 1;
            length += imp.getBinderUniqueName().length();
            length += 8;
            length += 8;
        }        
        final byte[] expected = new byte[length];
        int cur = 0;
        expected[cur++] = (byte) e.getImpressions().size();
        cur = transfer(data.getMillisSinceToday(), expected, cur);
        cur = transfer(A_DATE, expected, cur);
        for (Impression imp : e.getImpressions()) {
            cur = transfer(imp, expected, cur);
        }

        assertEquals(data.getQuery(), A_QUERY);
        assertEquals(expected, data.getData());

        return data;
    }

    protected final UserQueryEvent getUserQueryEvent(int n) {

        Impression[] imps = new Impression[n];
        for (int i = 0; i < n; i++) {
            imps[i] = new Impression(new PromotionMessageContainer(), ""
                    + (Math.random() * Long.MAX_VALUE), new Date(DATE_IN_MILLIS + i));
        }

        UserQueryEvent e = new UserQueryEvent(A_QUERY, A_DATE);
        for (Impression imp : imps) {
            e.addImpression(imp);
        }

        return e;
    }

    private int transfer(Date d, byte[] in, int start) {
        System.arraycopy(date2bytes(d), 0, in, start, 8);
        return start + 8;
    }

    private int transfer(long i, byte[] in, int start) {
        System.arraycopy(ByteUtils.long2bytes(i, 4), 0, in, start, 4);
        return start + 4;
    } 

    private int transfer(Impression i, byte[] in, int start) {
        int cur = start;
        int len = i.getBinderUniqueName().getBytes().length;
        arraycopy(ByteUtils.long2bytes(i.getBinderUniqueName().length(), 1), 0, in, cur, 1);     cur += 1;
        arraycopy(i.getBinderUniqueName().getBytes(), 0, in, cur, len);                             cur += len;
        arraycopy(ByteUtils.long2bytes(i.getPromoUniqueID(), 8), 0, in, cur, 8);           cur += 8;
        arraycopy(date2bytes(i.getTimeShown()), 0, in, cur, 8);                                     cur += 8;
        return cur;
    }

    private void arraycopy(byte[] src, int i, byte[] in, int start, int j) {
        System.arraycopy(src, i, in, start, j);
    }

    private byte[] date2bytes(Date d) {
        return ByteUtils.long2bytes(d.getTime(), 8);
    }
}
