package com.limegroup.gnutella;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceStage;
import org.limewire.util.Clock;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Maintains various session statistics, like uptime.  
 * Statistics are initialized the when the class is loaded; call
 * Statistics.instance() to guarantee initialization.  
 */
@Singleton
public class Statistics implements Service {
    
    private final Clock clock;
    
    /** The number of seconds in a day. */
    private static final int SECONDS_PER_DAY=24*60*60;
    /** Controls how much past is remembered in calculateFractionalUptime.
     *  Default: 7 days, which doesn't quite mean what you might think
     *  see calculateFractionalUptime. */
    private static final int WINDOW_MILLISECONDS=7*SECONDS_PER_DAY*1000;
    
    /** The time this was initialized. */
    private long startTime;
        
    @Inject
    Statistics(Clock clock) {
        this.clock = clock;
        startTime = clock.now();
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this).in(ServiceStage.LATE);
    }
    
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Session Statistics");
    }
    public void initialize() {
    }
    
    public void start() {
    }   

    /** 
     * Returns the amount of time this has been running.
     * @return the session uptime in milliseconds
     */        
    public long getUptime() {
        //TODO: clocks can go backwards from daylight savings, resulting in
        //negative values.
        return clock.now() - startTime;
    }

    /**
     * Calculates the average number of seconds this host runs per day, i.e.,
     * calculateFractionRunning*24*60*60.
     * @return uptime in seconds/day.
     * @see calculateFractionalUptime
     */
    public int calculateDailyUptime() {
        return (int)(calculateFractionalUptime()*SECONDS_PER_DAY);
    }

    /** 
     * Calculates the fraction of time this is running, a unitless quantity
     * between zero and 1.  Implemented using an exponential moving average
     * (EMA) that discounts the past.  Does not update the FRACTION_RUNNING
     * property; that should only be done once, on shutdown
     * @see calculateDailyUptime  
     */
    public float calculateFractionalUptime() { 
        //Let
        //     P = the last value returned by calculateFractionRunning stored
        //         in the SettingsMangager
        //     W = the window size in seconds.  (See note below.)
        //     t = the uptime for this session.  It is assumed that
        //         t<W; otherwise set t=W. 
        //     T = the elapsed time since the end of the previous session, i.e.,
        //         since P' was updated.  Note that t<T.  It is assumed that
        //         T<W; otherwise set T=W.
        //
        //The new fraction P' of the time this is running can be calculated as
        //a weighted average of the current session (t/T) and the past (P):
        //     P' = (T/W)*t/T + (1-T/W)*P
        //        =  t/W      + (W-T)/W*P
        //
        //W's name is misleading, because more than W seconds worth of history
        //are factored into the calculation.  More specifically, a session i
        //days ago contributes 1/W * ((W-1)/W)^i part of the average.  The
        //default value of W (7 days) means, for example, that the past 9 days
        //account for 75% of the calculation.
        
        final float W=WINDOW_MILLISECONDS;
        float T=Math.min(W, clock.now() - ApplicationSettings.LAST_SHUTDOWN_TIME.getValue());
        float t=Math.min(W, getUptime());
        float P=ApplicationSettings.FRACTIONAL_UPTIME.getValue();
        
        //Occasionally clocks can go backwards, e.g., if user adjusts them or
        //from daylight savings time.  In this case, ignore the current session
        //and just return P.
        if (t<0 || T<0 || t>T)
            return P;

        return t/W + (W-T)/W*P;
    }

    /**
     * Notifies this that LimeWire is shutting down, updating permanent
     * statistics in limewire.props if necessary.  
     */
    public void stop() {
        //Order matters, as calculateFractionalUptime() depends on the
        //LAST_SHUTDOWN_TIME property.
        ApplicationSettings.FRACTIONAL_UPTIME.setValue(calculateFractionalUptime());
        ApplicationSettings.LAST_SHUTDOWN_TIME.setValue(clock.now());
        int sessions = ApplicationSettings.SESSIONS.getValue();
        ApplicationSettings.SESSIONS.setValue( sessions + 1 );
    }


}
