package org.limewire.collection;

import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;

/**
 * Executes {@link Runnable Runnables} in a round-robin order per queue ID. 
 * 
 <pre>
    class Runner implements Runnable {
        final int item;
        
        Runner(int item) {
            this.item = item;
        }
        
        public void run() {
            try {
                Thread.sleep(150);
                System.out.println("Item: " + item);
            } catch (InterruptedException iex) {
                System.out.println(iex.toString());
            }
        }
    }       
    RRProcessingQueue rrpq = new RRProcessingQueue("sampleCodeRRProcessingQueue");
    
    rrpq.execute(new Runner(1), "Abby");
    rrpq.execute(new Runner(2), "Bob");
    rrpq.execute(new Runner(3), "Abby");
    rrpq.execute(new Runner(4), "Bob");
    rrpq.execute(new Runner(5), "Chris");
            
    try {
        Thread.sleep(1000);
    } catch (InterruptedException ignored) {}                

    Output:
        Item: 1
        Item: 2
        Item: 5
        Item: 3
        Item: 4
 </pre>
 */
/* TODO: Convert to using java.util.concurrent. */
public class RRProcessingQueue {
	
	private static final Log LOG = LogFactory.getLog(RRProcessingQueue.class);
    
    /** Factory to get new threads from. */
    private final ThreadFactory FACTORY;
    
    /** The thread doing the processing. */
    private Thread _runner = null;

    private final Map<Object, NamedQueue> queues = new HashMap<Object, NamedQueue>();
    private final RoundRobinQueue<NamedQueue> lists = new RoundRobinQueue<NamedQueue>();
	private int size;

	public RRProcessingQueue(String name) {
        FACTORY = ExecutorsHelper.daemonThreadFactory(name);
	}

	public synchronized void execute(Runnable runner, Object queueId) {
		NamedQueue queue = queues.get(queueId);
		if (queue == null) {
			queue = new NamedQueue(new LinkedList<Runnable>(), queueId);
			queues.put(queueId, queue);
			lists.enqueue(queue);
		}
		queue.list.add(runner);
		size++;
			
		notifyAndStart();
	}
    
    /** Notifies the waiting thread or starts a new one. */
    protected synchronized void notifyAndStart() {
        notify();
        if(_runner == null)
            startRunner();
    }
    
    /** Starts a new runner. */
    private synchronized void startRunner() {
        _runner = FACTORY.newThread(new Processor());
        _runner.setDaemon(true);
        _runner.start();
    }

    protected synchronized boolean moreTasks() {
        return size > 0;
    }
	
	protected synchronized Runnable next() {
		Runnable ret = null;
		while (lists.size() > 0) {
			NamedQueue next = lists.next();
			ret = next.next();
			if (ret == null || next.list.isEmpty()) {
				lists.removeAllOccurences(next);
				queues.remove(next.name);
			} 
			
			if (ret != null) {
				size--;
				return ret;
			}
		}
		return null;
	}
	
	public synchronized int size() {
		return size;
	}
	
	public synchronized void clear() {
		if (LOG.isDebugEnabled())
			LOG.debug("removing all "+size +" jobs from "+queues.size()+" queues");
		
		queues.clear();
		lists.clear();
		size = 0;
	}
	
	public synchronized void clear(Object name) {
		NamedQueue toRemove = queues.remove(name);
		if (toRemove == null)
			return;
		lists.removeAllOccurences(toRemove);
		
		if (LOG.isDebugEnabled())
			LOG.debug("removing "+toRemove.list.size()+" jobs out of "+size);
		
		size -= toRemove.list.size();
	}
	
	private class NamedQueue {
		final List<Runnable> list;
		final Object name;
		NamedQueue (List<Runnable> list, Object name) {
			this.list = list;
			this.name = name;
		}
		
		Runnable next() {
			return list.isEmpty() ? null : list.remove(0);
		}
	}
    
    /** The runnable that processes the queue. */
    private class Processor implements Runnable {
        public void run() {
            try {
                while(true) {
                    Runnable next = next();
                    if(next != null)
                        next.run();

                    // Ideally this would be in a finally clause -- but if it
                    // is then we can potentially ignore exceptions that were
                    // thrown.
                    synchronized(RRProcessingQueue.this) {
                        // If something was added before we grabbed the lock,
                        // process those items immediately instead of waiting
                        if(moreTasks())
                            continue;
                        
                        // Wait a little bit to see if something new is going
                        // to come in, so we don't needlessly kill/recreate
                        // threads.
                        try {
                            RRProcessingQueue.this.wait(5 * 1000);
                        } catch(InterruptedException ignored) {}
                        
                        // If something was added and notified us, process it
                        // instead of exiting.
                        if(moreTasks())
                            continue;
                        // Otherwise, exit
                        else
                            break;
                    }
                }
            } finally {
                // We must restart a new runner if something was added.
                // It's highly unlikely that something was added between
                // the try of one synchronized block & the finally of another,
                // but it technically is possible.
                // We cannot loop here because we'd lose any exceptions
                // that may have been thrown.
                synchronized(RRProcessingQueue.this) {
                    if(moreTasks())
                        startRunner();
                    else
                        _runner = null;
                }
            }
        }
    }

    
}
