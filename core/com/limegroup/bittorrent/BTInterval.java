package com.limegroup.bittorrent;

import org.limewire.collection.LongInterval;
import org.limewire.collection.Range;

/**
 * An interval within a block. 
 */
public class BTInterval extends LongInterval {
	private static final long serialVersionUID = 6565199693843714608L;

	private final int blockId;
	
	private int hashCode;

	public BTInterval(long low, long high, int id) {
		super(low, high);
		if (id < 0)
			throw new IllegalArgumentException("negative id");
		blockId = id;
	}
	
	public BTInterval(Range other, int id) {
		this(other.getLow(), other.getHigh(), id);
	}

	public BTInterval(long singleton, int id) {
		super(singleton);
		blockId = id;
	}
	
	public int getId() {
		return blockId;
	}
	
	@Override
    public boolean equals(Object other) {
		if (! (other instanceof BTInterval))
			return false;
		
		BTInterval o = (BTInterval) other;
		if (getId() != o.getId())
			return false;
		
		return super.equals(other);
	}
	
	@Override
    public int hashCode() {
		if (hashCode == 0) {
			hashCode = 17 * getId();
			hashCode *= 37 + getLow();
			hashCode *= 37 + getHigh();
		}
		return hashCode; 
	}
	
	public int getBlockId() {
		return blockId;
	}
	
	@Override
    public String toString() {
		return getId()+":"+super.toString();
	}
    
    
    public int get32BitLow() {
        return get32Bit(getLow());
    }
    
    public int get32BitHigh() {
        return get32Bit(getHigh());
    }
    
    public int get32BitLength() {
        long length = getHigh() - getLow() + 1;
        return get32Bit(length);
    }
    
    private static int get32Bit(long l) {
        // overflow intentional
        if (l > Integer.MAX_VALUE)
            return Integer.MAX_VALUE + (int)(l - Integer.MAX_VALUE);
        return (int)l;
    }
}
