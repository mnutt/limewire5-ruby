package org.limewire.promotion;

public class SimplePromotionServicesImpl implements PromotionServices {

    private boolean isRunning = false;

    public void start() {
        isRunning = true;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void stop() {
        isRunning = false;
    }
}
