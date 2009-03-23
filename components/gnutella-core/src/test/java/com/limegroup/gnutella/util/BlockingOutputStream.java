package com.limegroup.gnutella.util;

import java.io.IOException;
import java.io.OutputStream;

/** 
 * Blocks after writing more than n bytes. 
 */
public class BlockingOutputStream extends OutputStream {        
    private int _bytesBeforeBlock;
    private boolean _closed=false;
    private OutputStream _delegate;

    public BlockingOutputStream(OutputStream delegate, int n) {
        _bytesBeforeBlock=n;
        _delegate=delegate;
    }

    @Override
    public synchronized void write(int b) throws IOException {
        if (_bytesBeforeBlock<=0) {
            //Wait until closed
            while (true) {
                if (_closed)
                    throw new IOException();
                try {
                    wait();
                } catch (InterruptedException e) {                
                    throw new IOException();
                }
            }
        }

        _delegate.write(b);
        _bytesBeforeBlock--;        
    }

    @Override
    public synchronized void close() throws IOException {
        _closed=true;
        notifyAll();
        _delegate.close();
    }
}
