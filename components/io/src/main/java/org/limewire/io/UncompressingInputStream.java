package org.limewire.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Extends <code>InflaterInputStream</code> solely to catch a 
 * <code>NullPointerException</code> that can occur during the native 
 * <code>inflateBytes</code> method call if you concurrently closed the stream.
 */
public final class UncompressingInputStream extends InflaterInputStream {
    
    public UncompressingInputStream (final InputStream in, final Inflater flate) {
      super(in, flate);
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            return super.read(b, off, len);
        } catch(NullPointerException e) {
            //This will happen if 'end' was called on the inflate
            //while we were inflating.
            throw new IOException("inflater was ended");
        } catch(ArrayIndexOutOfBoundsException aioobe) {
            //This will happen occasionally on Windows machines
            //when the underlying socket was closed/disconnected
            //while the read reached the native socketRead0
            throw new IOException(aioobe.getMessage());
        } catch(OutOfMemoryError oome) {
            throw new IOException(oome.getMessage());
        }
    }
} // class
