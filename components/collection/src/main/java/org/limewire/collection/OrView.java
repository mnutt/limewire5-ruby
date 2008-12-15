package org.limewire.collection;

/**
 * Gives an Or view over one to many {@link BitField BitFields}. 
 * <code>OrView</code> returns the Or value and next clear (equal to 0) and 
 * set (equal to 1) bit of the <code>BitField</code>s.
 * <p>
 * See <a href="http://en.wikipedia.org/wiki/OR">Or</a> for more information on
 * the logical disjunction.
 * <pre>
    void sampleCodeOrView(){
        BitSet bs1 = new BitSet();
        bs1.set(0);
        bs1.set(1);
        
        BitSet bs2 = new BitSet();
        bs2.set(0);
        bs2.set(2);
                
        BitField bf1 = new BitFieldSet(bs1, 5);
        BitField bf2 = new BitFieldSet(bs2, 5);
               
        printBitField(bf1, "bf1");
        printBitField(bf2, "bf2");
        
        OrView ov = new OrView(bf1, bf2);
        printBitField(ov, " ov");

    }
    
    void printBitField(BitField bf, String bfName){
        System.out.print(bfName + ": ");
        for(int i = 0; i < bf.maxSize(); i++){
            int j = 0;
            if(bf.get(i))
                j = 1;          
            System.out.print(j);
        }
        System.out.println(""); 
    }

    Output:
        bf1: 11000
        bf2: 10100
         ov: 11100
 * </pre>
 *
 */
public class OrView extends BooleanFunction {

	public OrView(BitField first, BitField... more) {
		super(first, more);
	}

	public boolean get(int i) {
		for (BitField bf : fields) {
			if (bf.get(i))
				return true;
		}
		return false;
	}

	public int nextClearBit(int startIndex) {
		int currentIndex = startIndex;
		while(currentIndex < maxSize()) {
			boolean allSame = true;
			int largest = -1;
			int current = -1;
			for (int i = 0; i < fields.length; i++) {
				current = fields[i].nextClearBit(currentIndex);
				if (current == -1)
					return -1; // shortcut
				if (i == 0)
					largest = current;
				else if (current != largest) {
					allSame = false;
					largest = Math.max(largest,current);
				}
			}
			if (allSame)
				return largest;
			currentIndex = largest;
		}
		return -1;
	}

	public int nextSetBit(int startIndex) {
		long smallest = Long.MAX_VALUE;
        for (BitField field : fields) {
            int current = field.nextSetBit(startIndex);
            if (current == -1)
                continue;
            smallest = Math.min(current, smallest);
        }
		return smallest == Long.MAX_VALUE ? -1 : (int)smallest;
	}

}
