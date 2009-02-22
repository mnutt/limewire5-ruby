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

package org.limewire.mojito.concurrent;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.OnewayExchanger;
import org.limewire.mojito.Context;
import org.limewire.mojito.exceptions.LockTimeoutException;

/**
 * A cancellable asynchronous DHT task. This class provides a base implementation 
 * of {@link DHTFuture}, with methods to start and cancel a DHT task, query to see 
 * if the task is complete, and retrieve the result of the task. The result can only 
 * be retrieved when the computation has completed; the <code>get</code> method 
 * will block if the task has not yet completed. Once the task has completed, 
 * the computation cannot be restarted or cancelled.
 * 
 * {@see java.util.concurrent.FutureTask}
 */
public class DHTFutureTask<T> implements Runnable, DHTFuture<T>, Cancellable {
    
    private static final Log LOG = LogFactory.getLog(DHTFutureTask.class);
    
    private final OnewayExchanger<T, ExecutionException> exchanger;
	
    private final Set<DHTFutureListener<T>> listeners 
        = Collections.synchronizedSet(new LinkedHashSet<DHTFutureListener<T>>());
    
    private final Context context;
    
    private final DHTTask<T> task;
    
    /**
     * true if starting or started.
     * LOCKING: exchanger
     */
    private boolean taskIsActive;
    
    private ScheduledFuture<?> watchdog = null;
    
    public DHTFutureTask(final Context context, DHTTask<T> task) {
        this.task = task;
        this.context = context;
        
        exchanger = new OnewayExchanger<T, ExecutionException>(true) {
            @Override
            public synchronized void setValue(T value) {
                if (!isDone()) {
                    super.setValue(value);
                    killWatchdog();
                    internalDone();
                }
            }

            @Override
            public synchronized void setException(ExecutionException exception) {
                if (!isDone()) {
                    super.setException(exception);
                    killWatchdog();
                    internalDone();
                }
            }

            @Override
            public synchronized boolean cancel() {
                if (super.cancel()) {
                    killWatchdog();
                    internalDone();
                    return true;
                }
                return false;
            }
        };
    }
    
    public void run() {
        try {
            synchronized(exchanger) {
                if (isDone()) {
                    return;
                }
                taskIsActive = true;
            }
            task.start(exchanger);
            
            synchronized (exchanger) {
                if (!isDone()) {
                    initWatchdog();
                }
            }
            
        } catch (Throwable t) {
            exchanger.setException(new ExecutionException(t));
        }
    }

    /**
     * Starts the Watchdog
     */
    private void initWatchdog() {
        Runnable r = new Runnable() {
            public void run() {
                boolean timeout = false;
                synchronized (exchanger) {
                    if (!isDone()) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Watchdog is canceling " + task);
                        }
                        
                        timeout = taskIsActive;
                        exchanger.setException(
                                new ExecutionException(
                                        new LockTimeoutException(task.toString())));
                    }
                }
                
                if (timeout) {
                    task.cancel();
                }
            }
        };
    
        watchdog = context.getDHTExecutorService().schedule(
                r, task.getWaitOnLockTimeout(), TimeUnit.MILLISECONDS);
    }
    
    /**
     * Stops the Watchdog
     */
    private void killWatchdog() {
        synchronized (exchanger) {
            if (watchdog != null) {
                watchdog.cancel(true);
                watchdog = null;
            }
        }
    }
    
    /**
     * Protected method invoked when this task transitions to state isDone 
     * (whether normally or via cancellation). The default implementation 
     * does nothing. Subclasses may override this method to invoke completion 
     * callbacks or perform bookkeeping. Note that you can query status 
     * inside the implementation of this method to determine whether this 
     * task has been cancelled.
     */
    protected void done() {
        
    }
    
    /**
     * Calls done() and notifies all DHTFutureListeners
     */
    private void internalDone() {
    	
    	Runnable task = new Runnable() {
            public void run() {
                
                done();
                
                // Notify the listeners on a different Thread
                try {
                    T value = exchanger.get();
                    fireFutureResult(value);
                } catch (ExecutionException e) {
                    fireExecutionException(e);
                } catch (CancellationException e) {
                    fireCancellationException(e);
                } catch (InterruptedException e) {
                    fireInterruptedException(e);
                }
            }
        };
    	
    	context.getDHTExecutorService().execute(task);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.concurrent.DHTFuture#addDHTFutureListener(org.limewire.mojito.concurrent.DHTFutureListener)
     */
    public void addDHTFutureListener(final DHTFutureListener<T> listener) {
        if (listener == null) {
            throw new NullPointerException("DHTFutureListener is null");
        }
        
        boolean done = false;
        synchronized (exchanger) {
            done = isDone();
            if (!done) {
                listeners.add(listener);
            }
        }
        
        if (done) {
            Runnable r = new Runnable() {
                public void run() {
                    try {
                        T value = exchanger.get();
                        listener.handleFutureSuccess(value);
                    } catch (ExecutionException e) {
                        listener.handleExecutionException(e);
                    } catch (CancellationException e) {
                        listener.handleCancellationException(e);
                    } catch (InterruptedException e) {
                        listener.handleInterruptedException(e);
                    }
                }
            };
            
            context.getDHTExecutorService().execute(r);
        }
    }

    /*
     * (non-Javadoc)
     * @see java.util.concurrent.Future#cancel(boolean)
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelTask = false;
        synchronized (exchanger) {
            if (!isDone()) {
                if (!taskIsActive || mayInterruptIfRunning) {
                    exchanger.cancel();
                    cancelTask = taskIsActive;
                }
            }
        }
        
        if (cancelTask) {
            task.cancel();
        }
        
        return cancelTask;
    }

    /*
     * (non-Javadoc)
     * @see java.util.concurrent.Future#isCancelled()
     */
    public final boolean isCancelled() {
        return exchanger.isCancelled();
    }

    /*
     * (non-Javadoc)
     * @see java.util.concurrent.Future#isDone()
     */
    public final boolean isDone() {
        return exchanger.isDone();
    }
    
    /*
     * (non-Javadoc)
     * @see java.util.concurrent.Future#get()
     */
    public T get() throws InterruptedException, ExecutionException {
        BlockingDHTFutureListener<T> listener = new BlockingDHTFutureListener<T>();
        addDHTFutureListener(listener);
        return listener.get();
    }

    /*
     * (non-Javadoc)
     * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
     */
    public T get(long timeout, TimeUnit unit) 
            throws InterruptedException, ExecutionException, TimeoutException {
        BlockingDHTFutureListener<T> listener = new BlockingDHTFutureListener<T>();
        addDHTFutureListener(listener);
        return listener.get(timeout, unit);
    }
    
    /**
     * Returns a copy of all listeners as an Array
     */
    @SuppressWarnings("unchecked")
    private DHTFutureListener<T>[] listeners() {
        synchronized (listeners) {
            return listeners.toArray(new DHTFutureListener[0]);
        }
    }
    
    /**
     * Fires a success event
     */
    protected void fireFutureResult(T value) {
        for (DHTFutureListener<T> l : listeners()) {
            l.handleFutureSuccess(value);
        }
    }
    
    /**
     * Fires an ExecutionException event
     */
    protected void fireExecutionException(ExecutionException e) {
        for (DHTFutureListener<T> l : listeners()) {
            l.handleExecutionException(e);
        }
    }
    
    /**
     * Fires an CancellationException event
     */
    protected void fireCancellationException(CancellationException e) {
        for (DHTFutureListener<T> l : listeners()) {
            l.handleCancellationException(e);
        }
    }
    
    /**
     * Fires an InterruptedException event
     */
    protected void fireInterruptedException(InterruptedException e) {
        for (DHTFutureListener<T> l : listeners) {
            l.handleInterruptedException(e);
        }
    }
}
