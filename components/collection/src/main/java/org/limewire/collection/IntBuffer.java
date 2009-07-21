package org.limewire.collection;

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

/** 
 * Provides a fixed-size double-ended queue, a circular buffer.
 * Use <code>IntBuffer</code> when you want to use a fixed amount of resources.
 * For a minimal amount of efficiency, the internal buffer is only
 * allocated on the first insertion or retrieval, allowing lots of
 * int buffers to be created that may not be used.
 * <p>
 * This class is not thread-safe.
<pre>
    IntBuffer ib = new IntBuffer(10);
    for(int i = 0; !ib.isFull(); i++)
        ib.add(i);
    for(int i = 0; i < ib.size() ; i++)
        System.out.println(ib.get(i));      

    Output:
        9
        8
        7
        6
        5
        4
        3
        2
        1
        0
</pre>
 */
public final class IntBuffer implements Cloneable, Iterable<Integer> {
    /**<pre>
     * The abstraction function is
     *   [ buf[head], buf[head+1], ..., buf[tail-1] ] if head<=tail
     * or
     *   [ buf[head], buf[head+1], ..., buf[size-1], 
     *     buf[0], buf[1], ..., buf[tail-1] ]         otherwise
     *
     * Note that buf[head] is the location of the head, and
     * buf[tail] is just past the location of the tail. This
     * means that there is always one unused element of the array.
     * See p. 202 of  _Introduction to Algorithms_ by Cormen, 
     * Leiserson, Rivest for details.
     *
     * Also note that size is really the MAX size of this+1, i.e., 
     * the capacity, not the current size.
     *
     * INVARIANT: buf.length=size
     *            0<=head, tail<size
     *            size>=2
     *</pre>
     */
    private final int size;
    private int buf[];
    private int head;
    private int tail;

    /** 
     * @requires size>=1. 
     * @effects creates a new, empty buffer that can hold 
     *  size elements.
     */
    public IntBuffer(int size) {
        assert (size >= 1);
        //one element of buf unused
        this.size = size+1;
        // lazily initialized to preserver memory.
        //buf = new int[size+1];
        head = 0;
        tail = 0;
    }

    /** "Copy constructor": constructs a new shallow copy of other. */
    public IntBuffer(IntBuffer other) {
        this.size=other.size;
        this.head=other.head;
        this.tail=other.tail;

        if(other.buf != null) {
            this.buf=new int[other.buf.length];
            System.arraycopy(other.buf, 0,
                             this.buf, 0,
                             other.buf.length);
        }
    }
    
    private void initialize() {
        if(buf == null)
            buf = new int[size + 1];
    }

	/*
	public int[] toArray() {
		int[] newBuf = new int[size];

		int index = tail-head;
		System.arraycopy(buf, head,
						 newBuf, 0,
						 index);

		System.arraycopy(buf, 0,
						 newBuf, index,
						 size-index);
	}
	*/

    /** Returns true if and only if this is empty. */
    public boolean isEmpty() {
        return head==tail;
    }

    /** Returns true if and only if this is full, e.g., adding another element 
     *  would force another out.
     */
    public boolean isFull() {
        return increment(tail)==head;
    }

    /** Same as getSize(). */
    public final int size() {
        return getSize();
    }

    /** Returns the number of elements in this.  Note that this never
     *  exceeds the value returned by getCapacity. */
    public int getSize() {
        if (head<=tail)
            //tail-1-head+1                  [see abstraction function]
            return tail-head;
        else
            //(size-1-head+1) + (tail-1+1)   [see abstraction function]
            return size-head+tail;
    }

    /** Returns the number of elements that this can hold, i.e., the
     *  max size that was passed to the constructor. */
    public int getCapacity() {
        return size-1;
    }

    private int decrement(int i) {
        if (i==0)
            return size-1;
        else
            return i-1;
    }

    private int increment(int i) {
        if (i==(size-1))
            return 0;
        else
            return i+1;
    }

    /** Returns the j such that buf[j]=this[i]. */
    private int index(int i) throws IndexOutOfBoundsException {        
        if (i<0 || i>=getSize())
            throw new IndexOutOfBoundsException("index: " + i + ", size: " + getSize());
        return (i+head) % size;
    }

    /** If i<0 or i>=getSize(), throws IndexOutOfBoundsException.
      * Else returns this[i] */
    public int get(int i) throws IndexOutOfBoundsException {
        initialize();
        return buf[index(i)];
    }

    /*
     * @modifies this[i].
     * @effects if i<0 or i>=getSize(), throws IndexOutOfBoundsException 
     *  and does not modify this.  Else this[i]=o.
     */
    public void set(int i, int value) throws IndexOutOfBoundsException {
        initialize();
        buf[index(i)] = value;
    }

    /** 
	 * Same as addFirst(x). 
	 */
    public int add(int x) {
        return addFirst(x);
    }

    /** 
     * @modifies this.
     * @effects adds x to the head of this, removing the tail 
     *  if necessary so that the number of elements in this is less than
     *  or equal to the maximum size. Returns the element removed, or null
     *  if none was removed.
     */
    public int addFirst(int x) {
        initialize();
		int ret = -1;
        if (isFull())
            ret=removeLast();
        head=decrement(head);
        buf[head]=x;
        return ret;
    }

    /** 
     * @modifies this.
     * @effects adds x to the tail of this, removing the head 
     *  if necessary so that the number of elements in this is less than
     *  or equal to the maximum size.  Returns the element removed, or null
     *  if none was removed.
     */
    public int addLast(int x) {
        initialize();
		int ret = -1;
        if (isFull())
            ret=removeFirst();
        buf[tail]=x;
        tail=increment(tail);
        return ret;
    }


    /**
     * Returns true if the input object x is in the buffer.
     */
    public boolean contains(int x) {
        IntBufferIterator iterator = iterator();
        while (iterator.hasNext())
            if (iterator.nextInt() == x)
                return true;
        return false;
    }


    /**
     * Returns the head of this, or throws NoSuchElementException if
     * this is empty.
     */
    public int first() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSuchElementException();
        return buf[head];
    }
    
    /**
     * Returns the tail of this, or throws NoSuchElementException if
     * this is empty.
     */
    public int last() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSuchElementException();
        return buf[decrement(tail)];
    }    

    /**
     * @modifies this.
     * @effects removes and returns the head of this, or throws 
     *   NoSuchElementException if this is empty.
     */
    public int removeFirst() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSuchElementException();
        int ret=buf[head];
        buf[head]=-1;     //optimization: don't retain removed values
        head=increment(head);
        return ret;
    }

    /**
     * @modifies this.
     * @effects removes and returns the tail of this, or throws 
     *   NoSuchElementException if this is empty.
     */
    public int removeLast() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSuchElementException();
        tail=decrement(tail);
        int ret=buf[tail];
        buf[tail]=-1;    //optimization: don't retain removed values
        return ret;
    }

    /**
     * @modifies this.
     * @effects removes and returns the i'th element of this, or
     *  throws IndexOutOfBoundsException if i is not a valid index
     *  of this. In the worst case, this runs in linear time with
     *  respect to size().
     */ 
    public int remove(int i) throws IndexOutOfBoundsException {
        int ret=get(i);
        //Shift all elements to left.  This could be micro-optimized.
        for (int j=index(i); j!=tail; j=increment(j)) {
            buf[j]=buf[increment(j)];
        }
        //Adjust tail pointer accordingly.
        tail=decrement(tail);
        return ret;
    }

    /**
     * @modifies this.
     * @effects removes the first occurrence of x in this,
     *  if any, as determined by .equals. Returns true if any 
     *  elements were removed. In the worst case, this runs in linear 
     *  time with respect to size().
     */
    public boolean removeValue(int x) {
        for (int i=0; i<getSize(); i++) {
            if (x == get(i)) {
                remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * @modifies this.
     * @effects removes all occurrences of x in this,
     *  if any, as determined by .equals. Returns true if any 
     *  elements were removed. In the worst case, this runs in linear 
     *  time with respect to size().
     */
    public boolean removeAll(int x) {
        boolean ret=false;
        for (int i=0; i<getSize(); i++) {
            if (x == get(i)) {
                remove(i);
                i--;
                ret=true;
            }
        }
        return ret;
    }


    /**
     * @modifies this.
     * @effects removes all elements of this.
     */
    public void clear() {
        head=0;
        tail=0;
    }

    /** 
     * @effects returns an iterator that yields the elements of this, in 
     *  order, from head to tail.
     * @requires this not modified will iterator in use.
     */
    public IntBufferIterator iterator() {
        return new IntBufferIterator();
    }

	/**
	 * Specialized <tt>Iterator</tt> for <tt>IntBuffer</tt> that iterates
	 * over an array of ints.
	 */
    private class IntBufferIterator extends UnmodifiableIterator<Integer> {
        /** The index of the next element to yield. */
        int index;	
        /** Defensive programming; detect modifications while
         *  iterator in use. */
        int oldHead;
        int oldTail;

        IntBufferIterator() {
            index=head;
            oldHead=head;
            oldTail=tail;
        }

        public boolean hasNext() {
            ensureNoModifications();
            return index!=tail;
        }

		public Integer next() throws NoSuchElementException {
			return nextInt();
		}

        public int nextInt() throws NoSuchElementException {
            ensureNoModifications();
            if (!hasNext()) 
                throw new NoSuchElementException();

            int ret=buf[index];
            index=increment(index);
            return ret;
        }

        private void ensureNoModifications() {
            if (oldHead!=head || oldTail!=tail)
                throw new ConcurrentModificationException();
        }
    }

    /** Returns a shallow copy of this, of type <tt>IntBuffer</tt>. */
    @Override
    public Object clone() {
        return new IntBuffer(this);        
    }

	// overrides Object.toString to return more information
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean isFirst=true;
        for (IntBufferIterator iter=iterator(); iter.hasNext(); ) {
            if (! isFirst) 
                sb.append(", ");
            else
                isFirst=false;
            sb.append(iter.nextInt());            
        }
        sb.append("]");
        return sb.toString();
    }
}
