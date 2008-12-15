
package com.limegroup.gnutella.gui;

import javax.swing.AbstractListModel;

import org.limewire.collection.Buffer;


/**
 *  Use the Buffer class to efficiently deal with adding to a 
 *  fixed sized ListModel.
 *
 * @author Greg Bildson
 */
public class BufferListModel<T> extends AbstractListModel
{

    private Buffer<T>              buffer;

	/**
	 *  Create list model with size capacity
	 */
    public BufferListModel(int size) 
    {
        buffer = new Buffer<T>(size);
    }

	/**
	 *  Implement the default value getter for ListModel
	 */
    public T getElementAt(int idx)
    {
        return buffer.get(idx);
    }

	/**
	 *  Implement the default size return for ListModel
	 */
    public int getSize()
    {
		if ( buffer == null )
			return 0;
        return buffer.getSize();
    }

	/**
	 *  Change the size of the fixed list while maintaining the content
	 */
    public void changeSize(int size)
    {
		if ( size == 0 )
		{
            int oldSize = 0;
			if ( buffer != null )
			    oldSize = buffer.getSize();
			buffer = null;
            fireContentsChanged(this, 0, oldSize);
			return;
		}

        Buffer<T> nbuffer = new Buffer<T>(size);
        for ( int i = 0; buffer != null && 
		      i < Math.min(buffer.getSize(), size); i++ )
        {
            nbuffer.addFirst(buffer.get(i));
        }
        buffer = nbuffer;
        //fireContentsChanged(this, 0, Math.max(buffer.getSize(),size));
    }

	/**
	 *  Clear the list
	 */
    public void removeAllElements()
    {
		if ( buffer == null )
			return;

        buffer.clear();
        fireContentsChanged(this, 0, buffer.getCapacity());
    }

	/**
	 *  Add to the top of the fixed-size list
	 */
    public void addFirst(T val)
    {
		if ( buffer == null )
			return;

        buffer.addFirst(val);
        fireContentsChanged(this, 0, buffer.getSize());
    }
}


