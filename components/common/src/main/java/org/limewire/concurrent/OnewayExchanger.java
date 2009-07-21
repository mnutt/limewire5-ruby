/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.limewire.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The OnewayExchanger is an one-way synchronization point
 * for Threads. One or more Threads can wait for the arrival
 * of a value by calling the get() method which will block
 * and suspend the Threads until an another Thread sets a
 * return value or an Exception which will be thrown by the
 * get() method.
 * <p>
 * The main differences between OnewayExchanger and 
 * java.util.concurrent.Exchanger are:
 * 
 * <ol>
 * <li> Multiple Threads can wait for a result from a single Thread.
 * <li> It's a one-way exchange.
 * <li> The setter Thread may set an exception causing this exception
 * to be thrown on the getter side.
 * <li> The OnewayExchanger is cancellable.
 * <li> The OnewayExchanger can be configured for a single shot. That
 * means once a return value or an exception has been set they cannot
 * be changed anymore.
 * </ol>
 */
public class OnewayExchanger<V, E extends Throwable> {
    
    /** Flag for whether or not we're done */
    private boolean done = false;
    
    /** Flag for whether or not the exchanger was cancelled */
    private boolean cancelled = false;
    
    /** Flag for whether or not this is an one-shot exchanger */
    private final boolean oneShot;
    
    /** The value we're going to return */
    private V value;
    
    /** The Exception we're going to throw */
    private E exception;
    
    /**
     * Creates an OnewayExchanger with the default configuration.
     */
    public OnewayExchanger() {
        this(false);
    }
    
    /**
     * Creates an OnewayExchanger that is either configured
     * for a single shot which means the return value or the
     * Exception cannot be changed after they've been set.
     * <p>
     * Default is false.
     */
    public OnewayExchanger(boolean oneShot) {
        this.oneShot = oneShot;
    }
    
    /**
     * Waits for another Thread for a value or an Exception
     * unless they're already set in which case this method
     * will return immediately.
     */
    public synchronized V get() throws InterruptedException, E {
        try {
            return get(0L, TimeUnit.MILLISECONDS);
        } catch (TimeoutException cannotHappen) {
            throw new Error(cannotHappen);
        }
    }
    
    /**
     * Waits for another Thread for the given time for a value 
     * or an Exception unless they're already set in which case 
     * this method will return immediately.
     */
    public synchronized V get(long timeout, TimeUnit unit) 
            throws InterruptedException, TimeoutException, E {
        
        if (!done) {
            if (timeout == 0L) {
                wait();
            } else {
                unit.timedWait(this, timeout);
            }
            
            // Not done? Must be a timeout!
            if (!done) {
                throw new TimeoutException();
            }
        }
        
        if (cancelled) {
            throw new CancellationException();
        }
        
        // Prioritize Exceptions!
        if (exception != null) {
            throw exception;
        }
        
        return value;
    }
    
    /**
     * Tries to get the value without blocking.
     */
    public synchronized V tryGet() throws InterruptedException, E {
        if (done) {
            return get();
        } else {
            return null;
        }
    }
    
    /**
     * Tries to cancel the OnewayExchanger and returns true
     * on success.
     */
    public synchronized boolean cancel() {
        if (done) {
            return cancelled;
        }
        
        done = true;
        cancelled = true;
        notifyAll();
        return true;
    }
    
    /**
     * Returns true if the OnewayExchanger is cancelled.
     */
    public synchronized boolean isCancelled() {
        return cancelled;
    }
    
    /**
     * Returns true if the get() method will return immediately
     * by throwing an Exception or returning a value.
     */
    public synchronized boolean isDone() {
        return done;
    }
    
    /**
     * Returns true if calling the get() method will
     * throw an Exception.
     */
    public synchronized boolean throwsException() {
        return cancelled || exception != null;
    }
    
    /**
     * Returns true if this is an one-shot OnewayExchanger.
     */
    public boolean isOneShot() {
        return oneShot;
    }
    
    /**
     * Sets the value that will be returned by the get() method.
     */
    public synchronized void setValue(V value) {
        if (cancelled) {
            return;
        }
        
        if (done && oneShot) {
            throw new IllegalStateException("The OnewayExchanger is configured for a single shot");
        }
        
        done = true;
        this.value = value;
        notifyAll();
    }
    
    /**
     * Sets the Exception that will be thrown by the get() method.
     */
    public synchronized void setException(E exception) {
        if (exception == null) {
            throw new NullPointerException();
        }
        
        if (cancelled) {
            return;
        }
        
        if (done && oneShot) {
            throw new IllegalStateException("The OnewayExchanger is configured for a single shot");
        }
        
        done = true;
        this.exception = exception;
        notifyAll();
    }
    
    /**
     * Resets the OnewayExchanger so that it can be
     * reused unless it's configured for a single shot.
     */
    public synchronized void reset() {
        if (oneShot) {
            throw new IllegalStateException("The OnewayExchanger is configured for a single shot");
        }
        
        done = false;
        cancelled = false;
        value = null;
        exception = null;
    }
}
