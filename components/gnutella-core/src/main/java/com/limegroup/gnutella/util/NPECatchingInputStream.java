package com.limegroup.gnutella.util;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;

/**
 * InputStream that catches NPEs from BufferedInputStream
 * and rethrows them as IOExceptions.
 *
 * Prior to Java 1.5 (which finally was able to properly implement a completely
 * asynchronous BufferedInputStream), various internal methods could throw an NPE
 * if the connection was asynchronously closed).
 */
public class NPECatchingInputStream extends FilterInputStream { 
    
    public NPECatchingInputStream (final BufferedInputStream in) {
        super(in);
    }
    
    @Override
    public int read() throws IOException {
        try {
            return in.read();
        } catch(NullPointerException npe) {
            throw (IOException)new IOException().initCause(npe);
        }
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            return in.read(b, off, len);
        } catch(NullPointerException npe) {
            throw (IOException)new IOException().initCause(npe);
        }
    }
    
    @Override
    public long skip(long n) throws IOException {
        try {
            return in.skip(n);
        } catch(NullPointerException npe) {
            throw (IOException)new IOException().initCause(npe);
        }
    }
    
    @Override
    public void close() throws IOException {
        try {
            in.close();
        } catch(NullPointerException npe) {
            throw (IOException)new IOException().initCause(npe);
        }
    }
}
