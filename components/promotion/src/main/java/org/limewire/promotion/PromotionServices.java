package org.limewire.promotion;

/**
 * This defines an interface to the basic services needed by and provided for
 * the Promotion system.
 */
public interface PromotionServices {

    /**
     * Initialize all the needed components.
     */
    void start();

    /**
     * This is called to disable all promotion things, afterwards
     * {@link isRunning()} will return false.
     */
    void stop();

    /**
     * Returns <code>true</code> after a successful {@link #start()} and false
     * after {@link #stop()} is called. Before doing any query using the
     * promotion services, you should check that this returns <code>true</code>.
     * Only after returning <code>true</code> can you been assured that we
     * have a database connection and everything is ready to go.
     * 
     * @return <code>true</code> after a successful {@link #start()} and false
     *         after {@link #stop()} is called.
     */
    boolean isRunning();
}
