package com.limegroup.gnutella.downloader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/** Returns pre-determined pseudorandom numbers. */
@SuppressWarnings("unchecked")
public class TestPredeterminedRandom extends Random {
    /** Holds float values for nextFloat */
    public Iterator /* of Float */floatIterator;

    /** Holds int values for nextInt */
    public Iterator /* of Integer */intIterator;

    /** Holds long values for nextLong */
    public Iterator /* of Long */longIterator;

    /**
     * Care must be taken not to attempt to take too many values out of
     * floatIterator. This is just a test stub, so it's not a big deal that the
     * method is very fragile.
     * 
     * @return a float from floatIterator,
     */
    @Override
    public synchronized float nextFloat() {
        Float ret = (Float) floatIterator.next();
        return ret.floatValue();
    }

    /**
     * Care must be taken not to attempt to take too many values out of
     * floatIterator. This is just a test stub, so it's not a big deal that the
     * method is very fragile.
     * 
     * @return a float from floatIterator,
     */
    @Override
    public synchronized int nextInt() {
        Integer ret = (Integer) intIterator.next();
        return ret.intValue();
    }

    /**
     * Care must be taken not to attempt to take too many values out of
     * longIterator. This is just a test stub, so it's not a big deal that the
     * method is very fragile.
     * 
     * @return a long from longIterator
     */
    @Override
    public synchronized long nextLong() {
        Long ret = (Long) longIterator.next();
        return ret.longValue();
    }

    // /////// Mutators /////////////////////////
    /**
     * Sets the sequence of floats to be returned by nextFloat().
     */
    public synchronized void setFloats(float[] floats) {
        ArrayList /* of Float */floatList = new ArrayList();
        for (int i = 0; i < floats.length; i++) {
            // Check for conformity to the Random specification
            if (floats[i] < 0.0f || floats[i] >= 1.0f) {
                throw new IllegalArgumentException(
                        "Attempt to set float outside the legal "+
                        "range [0.0f 1.0f) :"+floats[i]);
            }
            floatList.add(new Float(floats[i]));
        }
        floatIterator = floatList.iterator();
    }

    /**
     * Sets the sequence of floats to be returned by nextFloat() to be only one
     * float.
     */
    public void setFloat(float f) {
        float[] fArray = new float[1];
        fArray[0] = f;
        setFloats(fArray);
    }

    /**
     * Sets the sequence of ints to be returned by nextInt().
     */
    public synchronized void setInts(int[] ints) {
        ArrayList /* of Integer */intList = new ArrayList();
        for (int i = 0; i < ints.length; i++)
            intList.add(new Integer(ints[i]));
        intIterator = intList.iterator();
    }

    /**
     * Sets the sequence of ints to be returned by nextInt() to include only one
     * int.
     */
    public void setInt(int i) {
        int[] iArray = new int[1];
        iArray[0] = i;
        setInts(iArray);
    }

    /**
     * Sets the sequence of longs to be returned by nextLong().
     */
    public synchronized void setLongs(long[] longs) {
        ArrayList /* of Long */longList = new ArrayList();
        for (int i = 0; i < longs.length; i++)
            longList.add(new Long(longs[i]));
        longIterator = longList.iterator();
    }

    /** Sets the sequence of longs to be returned by
     * nextLong() to be a single long.
     */
    public void setLong(long l) {
        long[] lArray = new long[1];
        lArray[0] = l;
        setLongs(lArray);
    }
}
