package org.limewire.nio.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.limewire.collection.Buffer;
import org.limewire.inspection.Inspectable;
import org.limewire.nio.observer.WriteObserver;
import org.limewire.util.ByteUtils;

/** A simple writer that maintains statistics about how much was written. */
public class StatisticGatheringWriter extends AbstractChannelInterestWriter implements Inspectable {

    private final static long NANO_START = System.nanoTime();
    private static final int HISTORY = 50;

    private final Buffer<Long> handleWrites = new Buffer<Long>(HISTORY);
    private final Buffer<Long> interestWrites = new Buffer<Long>(HISTORY);
    private final Buffer<Boolean> interestWritesStatus = new Buffer<Boolean>(HISTORY);
    private final Buffer<Long> writeTimes = new Buffer<Long>(HISTORY);
    private final Buffer<Long> writeAmounts = new Buffer<Long>(HISTORY);
    private final long msStart = System.currentTimeMillis();
    
    private long amountWrote, totalHandleWrite, totalInterestWrite, positiveInterestWrite;
    
    @Override
    public int write(ByteBuffer src) throws IOException {
        writeTimes.add(System.nanoTime() - NANO_START);
        int wrote = super.write(src);
        amountWrote += wrote;
        writeAmounts.add((long)wrote);
        return wrote;
    }
    
    @Override
    public boolean handleWrite() throws IOException {
        handleWrites.add(System.nanoTime() - NANO_START);
        totalHandleWrite++;
        return super.handleWrite();
    }

    @Override
    public void interestWrite(WriteObserver observer, boolean status) {
        interestWrites.add(System.nanoTime() - NANO_START);
        interestWritesStatus.add(status);
        totalInterestWrite++;
        if (status)
            positiveInterestWrite++;
        super.interestWrite(observer, status);
    }
    
    @Override
    public Object inspect() {
        Map<String,Object> ret = new HashMap<String,Object>();
        ret.put("ver",1);
        ret.put("ms",msStart);
        ret.put("hw",getPacked(handleWrites));
        ret.put("iw",getPacked(interestWrites));
        ret.put("iws", getPackedBool(interestWritesStatus));
        ret.put("wt",getPacked(writeTimes));
        ret.put("wa",getPacked(writeAmounts));
        ret.put("totalWrote", amountWrote);
        ret.put("totalHW", totalHandleWrite);
        ret.put("totalIW", totalInterestWrite);
        ret.put("totalIWP", positiveInterestWrite);
        return ret;
    }
    
    private byte [] getPacked(Buffer<Long> b) {
        byte [] ret = new byte[b.getSize() * 8];
        for (int i = 0; i < b.getSize(); i++)
            ByteUtils.long2beb(b.get(i), ret, i*8);
        return ret;
    }
    
    private byte [] getPackedBool(Buffer<Boolean> b) {
        byte [] ret = new byte[b.getSize()];
        for (int i = 0; i < b.getSize(); i++)
            ret[i] = (byte)(b.get(i) ? 1 : 0);
        return ret;
    }

}
