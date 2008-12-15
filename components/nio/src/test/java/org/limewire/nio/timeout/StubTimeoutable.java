package org.limewire.nio.timeout;


public class StubTimeoutable implements Timeoutable {
    private boolean notified = false;
    private long now = -1;
    private long expired = -1;
    private long timeoutLength = -1;

    public void notifyTimeout(long now, long expired, long timeoutLength) {
        this.now = now;
        this.expired = expired;
        this.timeoutLength = timeoutLength;
        this.notified = true;
    }

    public long getExpired() {
        return expired;
    }

    public boolean isNotified() {
        return notified;
    }

    public long getNow() {
        return now;
    }

    public long getTimeoutLength() {
        return timeoutLength;
    }
}
