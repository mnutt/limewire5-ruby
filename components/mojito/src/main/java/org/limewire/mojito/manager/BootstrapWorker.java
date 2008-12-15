package org.limewire.mojito.manager;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.OnewayExchanger;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.handler.response.FindNodeResponseHandler;
import org.limewire.mojito.result.FindNodeResult;

/**
 * A worker thread that continuously refreshes buckets provided by the BootstrapProcess.
 */
class BootstrapWorker implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(BootstrapWorker.class);
    
    private final BootstrapProcess process;
    private final Context context;
    
    private volatile OnewayExchanger<FindNodeResult, ExecutionException> exchanger;
    
    private final AtomicBoolean shutdown = new AtomicBoolean();
    
    BootstrapWorker(Context context, BootstrapProcess process){
        this.process = process;
        this.context = context;
    }
    
    public void run() {
        LOG.debug("starting worker");
        while(!shutdown.get()) {
            KUID nextBucket = process.getNextBucket();
            if (LOG.isDebugEnabled())
                LOG.debug(this+" will refresh "+nextBucket);
            if (nextBucket == null)
                return;
            refreshBucket(nextBucket);
        } 
    }
    
    private void refreshBucket(KUID randomId) {
        OnewayExchanger<FindNodeResult, ExecutionException> c 
            = new OnewayExchanger<FindNodeResult, ExecutionException>(true);

        FindNodeResponseHandler handler 
            = new FindNodeResponseHandler(context, randomId);

        handler.start(c);
        exchanger = c;
        FindNodeResult value = null;
        try {
            value = c.get();
        } catch (InterruptedException ignore) {}
        catch (CancellationException cancelled) {
            if (!shutdown.get())
                throw new IllegalStateException(cancelled);
        }
        catch (ExecutionException ee) {
            LOG.info("ExecutionException", ee);
            process.handleExecutionException(ee);
        }
        if (value == null)
            return;

        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished Bucket refresh: " + value);
        }
        process.refreshDone(value.getRouteTableFailureCount(), !value.getPath().isEmpty());
    }
    
    public void shutdown() {
        if (shutdown.getAndSet(true))
            return;
        
        OnewayExchanger<FindNodeResult, ExecutionException> e = exchanger;
        if (e != null)
            e.cancel();
    }
}
