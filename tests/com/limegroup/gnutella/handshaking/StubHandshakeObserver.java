package com.limegroup.gnutella.handshaking;

public class StubHandshakeObserver implements HandshakeObserver {
    private boolean noGOK;
    private int code;
    private String msg;
    private boolean badHandshake;
    private boolean handshakeFinished;
    private Handshaker shaker;
    private boolean shutdown;
    
    public void clear() {
        noGOK = false;
        code = 0;
        msg = null;
        badHandshake = false;
        handshakeFinished = false;
        shaker = null;
        shutdown = false;
    }

    public void handleNoGnutellaOk(int code, String msg) {
        this.noGOK = true;
        this.code = code;
        this.msg = msg;
    }

    public void handleBadHandshake() {
        this.badHandshake = true;
    }

    public void handleHandshakeFinished(Handshaker shaker) {
        this.handshakeFinished = true;
        this.shaker = shaker;
    }

    public void shutdown() {
        this.shutdown = true;
    }

    public boolean isBadHandshake() {
        return badHandshake;
    }

    public int getCode() {
        return code;
    }

    public boolean isHandshakeFinished() {
        return handshakeFinished;
    }

    public String getMsg() {
        return msg;
    }

    public boolean isNoGOK() {
        return noGOK;
    }

    public Handshaker getShaker() {
        return shaker;
    }

    public boolean isShutdown() {
        return shutdown;
    }

}
