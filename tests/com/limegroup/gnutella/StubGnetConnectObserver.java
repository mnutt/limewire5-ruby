package com.limegroup.gnutella;

import com.limegroup.gnutella.connection.GnetConnectObserver;


public class StubGnetConnectObserver implements GnetConnectObserver {
    private volatile boolean noGOK;
    private volatile int code;
    private volatile String msg;
    private volatile boolean badHandshake;
    private volatile boolean connect;
    private volatile boolean shutdown;
    private volatile Thread finishedThread;
    
    public synchronized void handleNoGnutellaOk(int code, String msg) {
        this.noGOK = true;
        this.code = code;
        this.msg = msg;
        this.finishedThread = Thread.currentThread();
        notify();
    }

    public synchronized void handleBadHandshake() {
        this.badHandshake = true;
        this.finishedThread = Thread.currentThread();
        notify();
    }

    public synchronized void handleConnect() {
        this.connect = true;
        this.finishedThread = Thread.currentThread();        
        notify();
    }

    public synchronized void shutdown() {
        this.shutdown = true;
        this.finishedThread = Thread.currentThread();
        notify();
    }
    
    public synchronized void waitForResponse(long time) throws Exception {
        wait(time);
    }

    public boolean isBadHandshake() {
        return badHandshake;
    }

    public int getCode() {
        return code;
    }

    public boolean isConnect() {
        return connect;
    }

    public String getMsg() {
        return msg;
    }

    public boolean isNoGOK() {
        return noGOK;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public Thread getFinishedThread() {
        return finishedThread;
    }
    
    @Override
    public String toString() {
        return "ngok: " + noGOK + ", code: " + code + ", msg: " + msg + ", badHandshake: " + badHandshake + ", connect: " + connect + "shutdown: " + shutdown + ", finishedThread: " + finishedThread;
    }

}
