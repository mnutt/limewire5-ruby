package com.limegroup.gnutella.messages;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.collection.Buffer;
import org.limewire.inspection.Inspectable;
import org.limewire.util.ByteUtils;

/** 
 * Defines the interface for a Gnutella message (packet). See
 * <a href="http://rfc-gnutella.sourceforge.net/developer/testing/messageArchitecture.html">
 * Gnutella message architecture</a> for more information.
 *
 */
public interface Message extends Comparable<Message> {

    /** The network a message came from or will travel through. */
    public static enum Network {
        UNKNOWN, TCP, UDP, MULTICAST;        
    }
    
    // Functional IDs defined by Gnutella protocol.
    public static final byte F_PING = (byte) 0x0;

    public static final byte F_PING_REPLY = (byte) 0x1;

    public static final byte F_PUSH = (byte) 0x40;

    public static final byte F_QUERY = (byte) 0x80;

    public static final byte F_QUERY_REPLY = (byte) 0x81;

    public static final byte F_ROUTE_TABLE_UPDATE = (byte) 0x30;

    public static final byte F_VENDOR_MESSAGE = (byte) 0x31;

    public static final byte F_VENDOR_MESSAGE_STABLE = (byte) 0x32;

    public static final byte F_UDP_CONNECTION = (byte) 0x41;

    /**
     * Writes a message quickly, without using temporary buffers or crap.
     */
    public void writeQuickly(OutputStream out) throws IOException;

    /**
     * Writes a message out, using the buffer as the temporary header.
     */
    public void write(OutputStream out, byte[] buf) throws IOException;

    /**
     * @modifies out
     * @effects Writes an encoding of this to out.  Does NOT flush out.
     */
    public void write(OutputStream out) throws IOException;

    ////////////////////////////////////////////////////////////////////
    public Network getNetwork();

    public boolean isMulticast();

    public boolean isUDP();

    public boolean isTCP();

    public boolean isUnknownNetwork();

    public byte[] getGUID();

    public byte getFunc();

    public byte getTTL();

    /**
     * If Time To Live (TTL) is less than zero, throws IllegalArgumentException.  
     * Otherwise sets this TTL to the given value.  This is useful when you 
     * want certain messages to travel less than others.
     *    @modifies this' TTL
     */
    public void setTTL(byte ttl) throws IllegalArgumentException;

    /**
     * If the hops is less than zero, throws IllegalArgumentException.
     * Otherwise sets this hops to the given value.  This is useful when you
     * want certain messages to look as if they've travelled further.
     *   @modifies this' hops
     */
    public void setHops(byte hops) throws IllegalArgumentException;

    public byte getHops();

    /** Returns the length of this' payload, in bytes. */
    public int getLength();

    /** Returns the total length of this, in bytes. */
    public int getTotalLength();

    /** @modifies this
     *  @effects increments hops, decrements TTL if > 0, and returns the
     *   OLD value of TTL.
     */
    public byte hop();

    /** 
     * Returns the system time (i.e., the result of System.currentTimeMillis())
     * this was instantiated.
     */
    public long getCreationTime();

    /** Returns this user-defined priority.  Lower values are higher priority. */
    public int getPriority();

    /** Set this user-defined priority for flow-control purposes.  Lower values
     *  are higher priority. */
    public void setPriority(int priority);

    /**
     * Returns the class that message handlers for it should register upon, i.e.
     * the interface class that it implements. 
     */
    public Class<? extends Message> getHandlerClass();
    
    //*********************************
    /// Inspection-related code follows
    //*********************************
    
    /**
     * Counts messages by type and network they came on. 
     * Also counts size in bytes, hops, TTLs.
     */
    public static class MessageCounter implements Inspectable {
        
        private final Map<Class, EnumMap<Network, MessageTypeCounter>> counts = 
            new HashMap<Class, EnumMap<Network,MessageTypeCounter>>();
        
        private final int history;
        
        /**
         * @param history how many messages to record history for each type/network
         */
        public MessageCounter(int history) {
            this.history = history;
        }


        public synchronized void countMessage(Message msg) {
            EnumMap<Network, MessageTypeCounter> m = counts.get(msg.getClass());
            if (m == null) {
                m = new EnumMap<Network, MessageTypeCounter>(Network.class);
                counts.put(msg.getClass(),m);
            }
            MessageTypeCounter count = m.get(msg.getNetwork());
            if (count == null) {
                count = new MessageTypeCounter(msg.getClass(), msg.getNetwork(), history);
                m.put(msg.getNetwork(),count);
            }
            count.countMessage(msg);
        }

        @Override
        public synchronized Object inspect() {
            List<Map<String,Object>> ret = new ArrayList<Map<String,Object>>(counts.size());
            for (EnumMap<Network, MessageTypeCounter> e : counts.values()) {
                for (MessageTypeCounter mtc : e.values())
                    ret.add(mtc.inspect());
            }
            return ret;
        }

        /** 
         * Keeps track of traffic information about a specific type of message
         */
        private static class MessageTypeCounter {
            private final Class clazz;
            private final Network net;
            private long num, size;
            private Buffer<Long> timestamps;
            private Buffer<Integer> sizes;
            private Buffer<Byte> hops;
            private Buffer<Byte> ttls;
            private long[] totalTtls = new long[5];
            private long[] totalHops = new long[5];

            /**
             * @param clazz the message class this is counting
             * @param net the network this message travels on
             * @param history how many messages to record history for
             */
            MessageTypeCounter(Class<? extends Message> clazz, Network net, int history) {
                this.clazz = clazz;
                this.net = net;
                timestamps = new Buffer<Long>(history); // each entry 6 bytes on network
                sizes = new Buffer<Integer>(history); // each entry 2 bytes on network
                hops = new Buffer<Byte>(history);
                ttls = new Buffer<Byte>(history);
            }

            void countMessage(Message m) {
                num++;
                size += m.getLength();
                timestamps.add(System.currentTimeMillis());
                sizes.add(m.getLength());
                hops.add(m.getHops());
                ttls.add(m.getTTL());
                // the last element is "or more"
                totalTtls[Math.min(4,m.getTTL())]++;
                totalHops[Math.min(4,m.getHops())]++;
            }

            Map<String,Object> inspect() {
                Map<String,Object> ret = new HashMap<String,Object>();
                ret.put("class",clazz.toString());
                ret.put("net",net.toString());
                ret.put("num",num);
                ret.put("size",size);
                byte [] timesByte = new byte[timestamps.getSize() * 6]; // 6 bytes per timestamp
                byte [] sizesByte = new byte[sizes.getSize() * 2]; // 2 bytes per size
                byte [] hopsByte = new byte[hops.getSize()];
                byte [] ttlsByte = new byte[ttls.getSize()];

                for (int i = 0; i < timestamps.getSize(); i++) {
                    long timestamp = timestamps.get(i);
                    timesByte[i * 6] = (byte)((timestamp >> 40) & 0xFF);
                    timesByte[i * 6 + 1] = (byte)((timestamp >> 32) & 0xFF);
                    ByteUtils.int2beb((int)timestamp, timesByte, i * 6 + 2);
                }

                for (int i = 0; i < sizes.getSize(); i++) {
                    short size = (short) Math.min(0xFFFF,sizes.get(i));
                    ByteUtils.short2beb(size, sizesByte, i * 2);
                }

                for (int i = 0; i < hops.getSize(); i++) 
                    hopsByte[i] = hops.get(i);
                
                for (int i = 0; i < ttls.getSize(); i++) 
                    ttlsByte[i] = ttls.get(i);
                
                ret.put("times",timesByte);
                ret.put("sizes",sizesByte);
                ret.put("hops", hopsByte);
                ret.put("ttls", ttlsByte);
                ret.put("totalTttls",totalTtls);
                ret.put("totalHops",totalHops);
                return ret;
            }
        }
    }
}