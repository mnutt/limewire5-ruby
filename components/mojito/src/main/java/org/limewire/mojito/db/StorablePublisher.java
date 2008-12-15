package org.limewire.mojito.db;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureListener;
import org.limewire.mojito.exceptions.DHTException;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.DatabaseSettings;
import org.limewire.mojito.statistics.DatabaseStatisticContainer;
import org.limewire.service.ErrorService;

/**
 * Publishes {@link Storable} values in the DHT.
 */
public class StorablePublisher implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(StorablePublisher.class);
    
    private final Context context;
    
    private final DatabaseStatisticContainer databaseStats;
    
    private ScheduledFuture future;
    
    private final PublishTask publishTask = new PublishTask();
    
    public StorablePublisher(Context context) {
        this.context = context;
        
        databaseStats = context.getDatabaseStats();
    }
    
    /**
     * Starts the <code>DHTValuePublisher</code>.
     */
    public void start() {
        synchronized (publishTask) {
            if (future == null) {
                long delay = DatabaseSettings.STORABLE_PUBLISHER_PERIOD.getValue();
                long initialDelay = delay;
                
                future = context.getDHTExecutorService()
                    .scheduleWithFixedDelay(this, initialDelay, delay, TimeUnit.MILLISECONDS);
            }
        }
    }
    
    /**
     * Stops the <code>DHTValuePublisher</code>.
     */
    public void stop() {
        synchronized (publishTask) {
            if (future != null) {
                future.cancel(true);
                future = null;
                
                publishTask.stop();
            }
        }
    }
    
    public void run() {
        
        // Do not publish values if we're not bootstrapped!
        if (context.isBootstrapped() 
                && !context.isBootstrapping()) {
            if (publishTask.isDone()) {
                
                if (LOG.isInfoEnabled()) {
                    LOG.info(context.getName() + " begins with publishing");
                }
                
                publishTask.start();
            }
        } else {
            
            if (LOG.isInfoEnabled()) {
                LOG.info(context.getName() + " is not bootstrapped");
            }
            
            publishTask.stop();
        }
    }
    
    /**
     * Publishes DHTValue(s) one-by-one by going
     * through a List of DHTValues. Every time a store finishes, it
     * continues with the next DHTValue until all DHTValues have
     * been republished
     */
    private class PublishTask {
        
        private Iterator<Storable> values = null;
        
        private DHTFuture<StoreResult> future = null;
        
        /**
         * Stops the PublishTask
         */
        public synchronized void stop() {
            if (future != null) {
                future.cancel(true);
                future = null;
            }
            
            values = null;
        }
        
        /**
         * Returns whether or not the PublishTask is done
         */
        public synchronized boolean isDone() {
            return values == null || !values.hasNext();
        }
        
        /**
         * Starts the PublishTask.
         */
        public synchronized void start() {
            assert (isDone());
            
            StorableModelManager modelManager = context.getStorableModelManager();
            Collection<Storable> valuesToPublish = modelManager.getStorables();
            if (valuesToPublish == null) {
                valuesToPublish = Collections.emptyList();
            }
            
            if (LOG.isInfoEnabled()) {
                LOG.info(context.getName() + " has " 
                        + valuesToPublish.size() + " DHTValues to process");
            }
            
            values = valuesToPublish.iterator();
            next();
        }
        
        /**
         * Publishes the next <code>DHTValue</code>.
         */
        private synchronized boolean next() {
            if (isDone()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(context.getName() + " is done with publishing");
                }
                return false;
            }
            
            while(values.hasNext()) {
                Storable storable = values.next();
                if (publish(storable)) {
                    return true;
                }
            }
            
            return false;
        }
        
        /**
         * Publishes or expires the given <code>DHTValue</code>.
         */
        private boolean publish(Storable storable) {
            
            // Check if value is still in DB because we're
            // working with a copy of the Collection.
            databaseStats.REPUBLISHED_VALUES.incrementStat();
            
            future = context.store(DHTValueEntity.createFromStorable(context, storable));
            future.addDHTFutureListener(new StoreResultHandler(storable));
            return true;
        }
    }
    
    private class StoreResultHandler implements DHTFutureListener<StoreResult> {
        
        private final Storable storable;
        
        private StoreResultHandler(Storable storable) {
            this.storable = storable;
        }
        
        public void handleFutureSuccess(final StoreResult result) {
            
            if (LOG.isInfoEnabled()) {
                Collection<? extends Contact> locations = result.getLocations();
                if (!locations.isEmpty()) {
                    LOG.info(result);
                } else {
                    LOG.info("Failed to store " + result.getValues());
                }
            }
            
            storable.handleStoreResult(result);
            
            context.getDHTExecutorService().execute(new Runnable() {
                public void run() {
                    context.getStorableModelManager().handleStoreResult(storable, result);
                }
            });
            
            if (!publishTask.next()) {
                publishTask.stop();
            }
        }
        
        public void handleExecutionException(ExecutionException e) {
            LOG.error("ExecutionException", e);

            if (!(e.getCause() instanceof DHTException)) {
                ErrorService.error(e);
            }
            
            if (!publishTask.next()) {
                publishTask.stop();
            }
        }

        public void handleCancellationException(CancellationException e) {
            LOG.debug("CancellationException", e);
            publishTask.stop();
        }
        
        public void handleInterruptedException(InterruptedException e) {
            LOG.debug("InterruptedException", e);
            publishTask.stop();
        }
    }
}
