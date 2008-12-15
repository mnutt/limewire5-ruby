package org.limewire.nio.statemachine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;

public class StubIOState implements IOState {
    
    private boolean writing;
    private boolean reading;
    private boolean throwIOX;
    private boolean returnTrueOnProcess;
    private boolean processed;
    private byte[] dataToPutInBuffer;

    public void setDataToPutInBuffer(byte[] dataToPutInBuffer) {
        this.dataToPutInBuffer = dataToPutInBuffer;
    }

    public boolean isWriting() {
        return writing;
    }

    public boolean isReading() {
        return reading;
    }

    public boolean process(Channel channel, ByteBuffer buffer) throws IOException {
        processed = true;
        
        if(throwIOX)
            throw new IOException();
        
        if(dataToPutInBuffer != null)
            buffer.put(dataToPutInBuffer);
        
        return returnTrueOnProcess;
    }
    
    public long getAmountProcessed() {
        return -1;
    }

    public void setReading(boolean reading) {
        this.reading = reading;
    }

    public void setReturnTrueOnProcess(boolean returnTrueOnProcess) {
        this.returnTrueOnProcess = returnTrueOnProcess;
    }

    public void setThrowIOX(boolean throwIOX) {
        this.throwIOX = throwIOX;
    }

    public void setWriting(boolean writing) {
        this.writing = writing;
    }
    
    public boolean isProcessed() {
        return processed;
    }
    
    public void clear() {
        processed = false;
    }

}
