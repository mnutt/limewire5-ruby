package com.limegroup.gnutella.downloader;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.limewire.util.AssertComparisons;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Downloader.DownloadState;

public class DownloadTestUtils {
    
    private DownloadTestUtils() {}
    
    /** Waits 1 second for the given state. */
    public static void waitForState(Downloader downloader, DownloadState state) throws Exception {
        waitForState(downloader, state, 2, TimeUnit.SECONDS);
    }
    
    /** Waits the duration for the given state. */
    public static void waitForState(Downloader downloader, DownloadState state, long timeout, TimeUnit unit) throws Exception {
        timeout = unit.toMillis(timeout);
        while(timeout > 0 && downloader.getState() != state) {
            long now = System.currentTimeMillis();
            Thread.sleep(50);
            timeout -= System.currentTimeMillis() - now;
        }
        AssertComparisons.assertEquals(state, downloader.getState());
    }
    
    /** Waits for the given state, only allowing the allowed states while waiting. */
    public static void strictWaitForState(Downloader downloader, DownloadState state, DownloadState... allowed) throws Exception {
        strictWaitForState(downloader, state, 2, TimeUnit.SECONDS, allowed);
    }
    
    /** Waits the duration for the given state, only allowing the allowed states while waiting. */
    public static void strictWaitForState(Downloader downloader, DownloadState state, long timeout, TimeUnit unit, DownloadState... allowed) throws Exception {
        timeout = unit.toMillis(timeout);
        while(timeout > 0 && downloader.getState() != state) {
            DownloadState current = downloader.getState();
            if(!Arrays.asList(allowed).contains(current)) {
                AssertComparisons.fail("Current state: " + current + ", not in allowed states: " + Arrays.asList(allowed)); 
            }
            
            long now = System.currentTimeMillis();
            Thread.sleep(200);
            timeout -= System.currentTimeMillis() - now;
        }
        AssertComparisons.assertEquals(state, downloader.getState());
    }
    
    public static void pumpThroughStates(Downloader downloader, Runnable pump, DownloadState startState, DownloadState endState, DownloadState... middleStates) throws Exception {
        pumpThroughStates(downloader, pump, startState, endState, 2, TimeUnit.SECONDS, middleStates);
    }
    
    public static void pumpThroughStates(Downloader downloader, Runnable pump, DownloadState startState, DownloadState endState, long timeout, TimeUnit unit, DownloadState... middleStates) throws Exception {
        AssertComparisons.assertEquals(startState, downloader.getState());
        timeout = unit.toMillis(timeout);
        // Jump out of the start state immediately.
        timeout = waitForStatesToEnd(downloader, pump, timeout, startState);
        if(downloader.getState() == startState) {
            // This is an additional assertion incase start & end are the same,
            // in which case we want to be extra certain that some middle states are involved.
            AssertComparisons.fail("Still in start state: " + startState);
        }
        
        // Then loop through the middle states.
        // (We do the first one separate to ensure that we can cycle from start -> end even if start & end are the same)
        while(timeout > 0 && downloader.getState() != endState) {
            timeout = waitForStatesToEnd(downloader, pump, timeout, middleStates);
        } 
        
        if(downloader.getState() != endState) {
            AssertComparisons.fail("Invalid end state: " + downloader.getState() + ", required: " + endState);
        }
    }
        
    private static long waitForStatesToEnd(Downloader downloader, Runnable pump, long timeout, DownloadState... possibleStates) throws Exception {
        while(timeout > 0 && Arrays.asList(possibleStates).contains(downloader.getState())) {
            long now = System.currentTimeMillis();
            Thread.sleep(200);
            timeout -= System.currentTimeMillis() - now;
            pump.run();
        }
        return timeout;
    }

}
