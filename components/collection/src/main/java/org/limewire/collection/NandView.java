package org.limewire.collection;

/**
 * 
 * <pre>
 * Output: bf1: 1100 
 *         bf2: 1010 
 *         av:  0111
 * </pre>
 */
public class NandView extends BooleanFunction {

    public NandView(BitField first, BitField... more) {
        super(first, more);
    }

    public boolean get(int i) {

        for (BitField bf : fields) {
            if (!bf.get(i)) {
                return true;
            }
        }
        return false;
    }

    public int nextClearBit(int startIndex) {
        for (int i = startIndex; i < maxSize(); i++) {
            boolean set = get(i);
            if (!set) {
                return i;
            }
        }

        return -1;
    }

    public int nextSetBit(int startIndex) {
        for (int i = startIndex; i < maxSize(); i++) {
            boolean set = get(i);
            if (set) {
                return i;
            }
        }
        return -1;
    }

}
