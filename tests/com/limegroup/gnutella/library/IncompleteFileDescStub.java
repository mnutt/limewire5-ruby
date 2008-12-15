
package com.limegroup.gnutella.library;

import java.io.IOException;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;

import com.limegroup.gnutella.URN;

/**
 * A stub that is identical to FileDescStub.  The code uses instanceof,
 * so we need this stub for Partial files.
 * 
 * It also stubs out some methods.  Feel free to stub out more methods as
 * need arises.
 */
@SuppressWarnings("unchecked")
public class IncompleteFileDescStub extends FileDescStub implements IncompleteFileDesc {
	public static final String urnString = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";
	private static final URN urn;
    
    private IntervalSet.ByteIntervals _ranges;
    
    public static final int size = 1126400;
    static {
        URN u;
        try {
            u = URN.createSHA1Urn(urnString);
        } catch(IOException iox) {
            throw new RuntimeException(iox);
        }
        urn = u;
    }
    
    public IncompleteFileDescStub() {
        this("abc.txt", urn, 0);
    }
    
    public IncompleteFileDescStub(String name, URN urn, int index) {
        super(name, urn, index);
    }
    
    @Override
    public boolean containsUrn(URN urn) {
        if(urn.equals(IncompleteFileDescStub.urn)) {
            return true;
        } else {
            return super.containsUrn(urn);
        }
    }
    
    @Override
    public long getFileSize() {
        return size;
    }
    
    @Override
	public IntervalSet.ByteIntervals getRangesAsByte() {
		return _ranges;
	}
	
	public void setRangesByte(IntervalSet.ByteIntervals what) {
		_ranges=what;
	}
    
    public void setRangesAsIntervals(Range... intervals) {
        IntervalSet set = new IntervalSet();
        for(Range intvl : intervals)
            set.add(intvl);
        _ranges = set.toBytes();
    }

    @Override
    public String getAvailableRanges() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Range getAvailableSubRange(long low, long high) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasUrnsAndPartialData() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isRangeSatisfiable(long low, long high) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isRangeSatisfiable(Range range) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean loadResponseRanges(IntervalSet dest) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String httpStringValue() {
        // TODO Auto-generated method stub
        return null;
    }
	
}
