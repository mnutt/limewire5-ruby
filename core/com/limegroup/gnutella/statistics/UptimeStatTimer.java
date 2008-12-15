package com.limegroup.gnutella.statistics;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.i18n.I18nMarker;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * This class handles the timer that updates uptime statistics.
 */
@Singleton
final class UptimeStatTimer implements Service {

    /** Current uptime in seconds. */
    @InspectablePrimitive("currentUptime")
    private static volatile long currentUptime = 0;

    /**
     * The interval in seconds at which to update the history of the last n
     * uptimes.
     */
    private static final int UPTIME_HISTORY_SNAPSHOT_INTERVAL = 60;

    /**
     * The number of uptimes to remember.
     */
    private static final int LAST_N_UPTIMES = 20;
    
    private volatile long lastUpdateTime = -1;
    private volatile ScheduledFuture<?> future;

    private final AtomicBoolean firstUtimeUpdate = new AtomicBoolean(true);    
    private final ScheduledExecutorService backgroundExecutor;
    
    @Inject UptimeStatTimer(@Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        this.backgroundExecutor = backgroundExecutor;
    }
    
    @Inject void register(ServiceRegistry registry) {
        registry.register(this);
    }
    
    @Override
    public String getServiceName() {
        return I18nMarker.marktr("Uptime Statistics");
    }
    @Override
    public void initialize() {
    }
    
    @Override
    public void start() {
        lastUpdateTime = System.currentTimeMillis();
        this.future = backgroundExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                refreshStats();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
    
    @Override
    public void stop() {
        Future future = this.future;
        if(future != null) {
            future.cancel(false);
            this.future = null;
        }
    }

    /**
     * Refreshes all of uptime statistics.
     */
    private void refreshStats() {
        long now = System.currentTimeMillis();
        long elapsed = (now - lastUpdateTime) / 1000;
        if(elapsed > 0) {
            currentUptime += elapsed;
    
            long totalUptime = ApplicationSettings.TOTAL_UPTIME.getValue() + elapsed;
            ApplicationSettings.TOTAL_UPTIME.setValue(totalUptime);
            ApplicationSettings.AVERAGE_UPTIME.setValue(totalUptime / ApplicationSettings.SESSIONS.getValue());
    
            updateUptimeHistory(currentUptime, UPTIME_HISTORY_SNAPSHOT_INTERVAL, LAST_N_UPTIMES);
        }
        
        lastUpdateTime = now;
    }

    /**
     * Updates the uptime history with the current uptime.
     * 
     * @param currentUptime the current uptime in seconds
     * @param interval the interval at which to update the history
     * @param historyLength the number of entries to remember in the history
     */
    void updateUptimeHistory(long currentUptime, int interval, int historyLength) {
        if (currentUptime == 0) {
            return;
        }
        // only update setting every so many seconds
        if (currentUptime % interval == 0) {
            String[] lastNUptimes = ApplicationSettings.LAST_N_UPTIMES.getValue();
            // first time this session
            if (firstUtimeUpdate.getAndSet(false)) {
                String[] copy;
                if (lastNUptimes.length < historyLength) {
                    copy = new String[lastNUptimes.length + 1];
                    System.arraycopy(lastNUptimes, 0, copy, 0, lastNUptimes.length);
                } else {
                    copy = new String[historyLength];
                    System.arraycopy(lastNUptimes, 1, copy, 0, copy.length - 1);
                }
                copy[copy.length - 1] = Long.toString(currentUptime);
                ApplicationSettings.LAST_N_UPTIMES.setValue(copy);
            } else {
                // very defensive, should never happen
                if (lastNUptimes.length == 0) {
                    ApplicationSettings.LAST_N_UPTIMES.setValue(new String[] { Long
                            .toString(currentUptime) });
                } else {
                    lastNUptimes[lastNUptimes.length - 1] = Long.toString(currentUptime);
                    ApplicationSettings.LAST_N_UPTIMES.setValue(lastNUptimes);
                }
            }
        }
    }
}
