package org.limewire.security;


public class StubSecureMessageCallback implements SecureMessageCallback {

    private SecureMessage sm;
    private boolean passed;
    private boolean replied = false;
    
    public synchronized void handleSecureMessage(SecureMessage sm, boolean passed) {
        this.sm = sm;
        this.passed = passed;
        replied = true;
        notify();
    }
    
    public synchronized void waitForReply() {
        if(!replied) {
            try {
                wait(10000); // only wait so long.
            } catch(InterruptedException ie) {
            }
        }
        
        if(!replied)
            throw new RuntimeException("didn't get a reply!");
    }
    
    public SecureMessage getSecureMessage() { return sm; }
    public boolean getPassed() { return passed; }


}
