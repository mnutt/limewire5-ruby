package org.limewire.collection;

/**
 * Provides an XOR view. <code>XorView</code> gets and finds the next set 
 * (equal to 1) and clear (equal to 0) bit starting at a specific location.
 * <p>
 * See <a href="http://en.wikipedia.org/wiki/Exclusive_or">exclusive or</a> 
 * for more information.
<pre>
    void sampleCodeXorView(){
        
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
                       
        XorView xov = new XorView(bf1, bf2);
        printBitField(xov, "xov");
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
        xov: 01100
</pre>

 */
public class XorView extends BooleanFunction {

	public XorView(BitField first, BitField... more) {
		super(first, more);
	}

	public boolean get(int i) {
		boolean ret = fields[0].get(i);
		for (int j = 1;j < fields.length;j++) {
			ret ^= fields[j].get(i);
        }
		return ret;
	}

	public int nextClearBit(int startIndex) {
		// not very efficient
		for (int i = startIndex; i < maxSize(); i++) {
			if (!get(i))
				return i;
		}
		return -1;
	}

	public int nextSetBit(int startIndex) {
		// not very efficient
		for (int i = startIndex; i < maxSize(); i++) {
			if (get(i))
				return i;
		}
		return -1;
	}

}
