package com.limegroup.gnutella.dht.db;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.result.FindValueResult;
import com.limegroup.gnutella.dht.DHTManager;

/**
 * An abstract implementation of DHTFutureAdapter to handle AltLocValues
 * and PushAltLocValues.
 * 
 * Iterates over entity keys and retrieves their values from the node and
 * calls {@link #handleDHTValueEntity(DHTValueEntity)} for them.
 */
abstract class AbstractResultHandler extends DHTFutureAdapter<FindValueResult> {
    
    private static final Log LOG = LogFactory.getLog(AbstractResultHandler.class);
    
    /**
     * Result type to denote different results of {@link AbstractResultHandler#handleDHTValueEntity(DHTValueEntity)}.
     */
    enum Result {
        /** An alternate location has been found */
        FOUND(true),
        /** An alternate location has not been found and will not be found */
        NOT_FOUND(false),
        /** An alternate location has not yet been found but could still be found */
        NOT_YET_FOUND(false) {
            @Override
            public boolean isFound() {
                throw new UnsupportedOperationException("Should not have been called on " + this);
            }  
        };
        
        private final boolean value;
        
        private Result(boolean value) {
            this.value = value;
        }
        
        public boolean isFound() {
            return value;
        }
    };
    
    protected final DHTManager dhtManager;
    
    protected final KUID key;
    
    private final SearchListener listener;
    
    protected final DHTValueType valueType;
    
    AbstractResultHandler(DHTManager dhtManager, KUID key, SearchListener listener, 
            DHTValueType valueType) {
        this.dhtManager = dhtManager;
        this.key = key;
        this.listener = listener;
        this.valueType = valueType;
        if (listener == null) {
            throw new NullPointerException("listener should not be null");
        }
    }
    
    @Override
    public void handleFutureSuccess(FindValueResult result) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("result: " + result);
        }
        
        Result outcome = Result.NOT_FOUND;
        
        if (result.isSuccess()) {
            for (DHTValueEntity entity : result.getEntities()) {
                outcome = updateResult(outcome, handleDHTValueEntity(entity)); 
            }
            
            for (EntityKey entityKey : result.getEntityKeys()) {
                if (!entityKey.getDHTValueType().equals(valueType)) {
                    continue;
                }
                DHTFuture<FindValueResult> future = dhtManager.get(entityKey);
                if(future != null) {
                    try {                        
                        // TODO make this a non-blocking call
                        FindValueResult resultFromKey = future.get();
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("result from second lookup: " + resultFromKey);
                        }
                        if (resultFromKey.isSuccess()) {
                            for (DHTValueEntity entity : resultFromKey.getEntities()) {
                                outcome = updateResult(outcome, handleDHTValueEntity(entity));
                            }
                        }
                    } catch (ExecutionException e) {
                        LOG.error("ExecutionException", e);
                    } catch (InterruptedException e) {
                        LOG.error("InterruptedException", e);
                    } 
                }
            }
        }
        
        // only notify if we haven't found anything
        if (outcome == Result.NOT_FOUND) {
            LOG.debug("Not found");
            listener.searchFailed();
        }
    }
    
    /**
     * Updates the result from the old value to the new value if the new value
     * doesn't already say the value was found. 
     *
     * FOUND => goes nowhere, the value has been found
     * NOT_YET_FOUND  => can go to FOUND
     * NOT_FOUND => can go to NOT_YET_FOUND, or FOUND, or stay with FOUND
     */
    private Result updateResult(Result oldValue, Result possibleValue) {
        if (oldValue == Result.FOUND || possibleValue == Result.FOUND) {
            return Result.FOUND;
        } else if (oldValue == Result.NOT_YET_FOUND || possibleValue == Result.NOT_YET_FOUND) {
            return Result.NOT_YET_FOUND;
        } else {
            return possibleValue;
        }
    }
    
    /**
     * Handles a DHTValueEntity (turns it into some Gnutella Object)
     * and returns true on success
     */
    protected abstract Result handleDHTValueEntity(DHTValueEntity entity);
    
    @Override
    public void handleCancellationException(CancellationException e) {
        LOG.error("CancellationException", e);
        listener.searchFailed();
    }

    @Override
    public void handleExecutionException(ExecutionException e) {
        LOG.error("ExecutionException", e);
        listener.searchFailed();
    }

    @Override
    public void handleInterruptedException(InterruptedException e) {
        LOG.error("InterruptedException", e);
        listener.searchFailed();
    }
    
    @Override
    public int hashCode() {
        return key.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof AbstractResultHandler)) {
            return false;
        }
        
        AbstractResultHandler other = (AbstractResultHandler)o;
        return key.equals(other.key)
                && valueType.equals(other.valueType);
    }
}