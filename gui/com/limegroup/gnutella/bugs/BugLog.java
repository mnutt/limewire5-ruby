package com.limegroup.gnutella.bugs;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Generates a buffered log. It buffers the input and dumps it out after 
 * buffer gets full. It dumps the logs to prespecified file.
 * @author  asingla
 */
public class BugLog {
    /**
     * The output stream to write to
     */
    private static PrintStream _out = null; 
    
    /**
     * Initial capacity for the buffer (no. of characters)
     */
    private static final int BUFFER_INITIAL_CAPACITY = 10000;
    
    /**
     * The internal buffer
     */
    private static StringBuilder _buffer 
        = new StringBuilder(BUFFER_INITIAL_CAPACITY);
    
    /**
     * The number of calls to print/append methods that are buffered, 
     * before actually
     * outputting the result to the out stream.
     */
    private static final int BUFFERING_LIMIT = 300;
    
    /**
     * Number of calls that have been made to print/append methods, after the 
     * buffer was last written out to the 'out' stream
     */
    private static int _count = 0;
    
    
    //static initializer
    static
    {
        try
        {
            _out = new PrintStream(
                new FileOutputStream("/home/logs/BugLog",true));
        }
        catch(IOException ioe)
        {
            _out = null;
            ioe.printStackTrace();
        }
    }
    
    
    /**
     * schedules the passed object for writing out to the stream
     */
    public static void println(Object o)
    {
        //append to the buffer
        _buffer.append(o);
        _buffer.append("\n");
        
        //increment the count
        _count++;
        
        //if count reached threshold
        if(_count >= BUFFERING_LIMIT)
        {
            //write the buffer out to the stream
            writeBufferOut();
            //reinitialize the count
            _count = 0;
        }
    }//end of fn append
    
    /**
     * Writes the buffer to the 'out' stream
     */
    private static void writeBufferOut()
    {
        //write out
        _out.print(_buffer);
        _out.flush();
        //reinitialize buffer
        _buffer = new StringBuilder(BUFFER_INITIAL_CAPACITY);
    }
    
    /**
     * Forces the internal buffer to be written out to the out stream
     */
    public static void flush()
    {
        writeBufferOut();
    }
    
}
