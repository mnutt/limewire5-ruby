package org.limewire.ui.support;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ThreadExecutor;

/** Simple class to help monitor deadlocking. */
public class DeadlockSupport {
    
    private static Log LOG = LogFactory.getLog(DeadlockSupport.class);
    
    /** 
     * How often to check for deadlocks. 
     * 
     * This class doubles as a workaround for bug_id=6435126,
     * so it doesn't use a multiple of 10 for the sleep interval.
     */
    private static final int DEADLOCK_CHECK_INTERVAL = 3001;

    public static void startDeadlockMonitoring() {
        Thread t = ThreadExecutor.newManagedThread(new Runnable() {
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(DEADLOCK_CHECK_INTERVAL);
                    } catch (InterruptedException ignored) {}
                    LOG.trace("deadlock check start");
                    long [] ids = findDeadlockedThreads(ManagementFactory.getThreadMXBean());
                    
                    if (ids == null) {
                        LOG.trace("no deadlocks found");
                        continue;
                    }
                    
                    StringBuilder sb = new StringBuilder("Deadlock Report:\n");
                    StackTraceElement[] firstStackTrace = null;
                    ThreadInfo[] allThreadInfo = ManagementFactory.getThreadMXBean().getThreadInfo(ids, true, true);
                    for (ThreadInfo info : allThreadInfo) {
                        sb.append("\"" + info.getThreadName() + "\" (id=" + info.getThreadId() + ")");
                        sb.append(" " + info.getThreadState() + " on " + info.getLockName() + " owned by ");
                        sb.append("\"" + info.getLockOwnerName() + "\" (id=" + info.getLockOwnerId() + ")");
                        if (info.isSuspended())
                            sb.append(" (suspended)");
                        if (info.isInNative())
                            sb.append(" (in native)");
                        sb.append("\n");
                        StackTraceElement[] trace = info.getStackTrace();
                        if(firstStackTrace == null)
                            firstStackTrace = trace;
                        for(int i = 0; i < trace.length; i++) {
                            sb.append("\tat " + trace[i].toString() + "\n");
                            if(i == 0)
                                addLockInfo(info, sb);
                            addMonitorInfo(info, sb, i);
                        }
                        
                        addLockedSynchronizers(info, sb);
                        
                        sb.append("\n");
                    }
                    
                    DeadlockException deadlock = new DeadlockException();
                    // Redirect the stack trace to separate deadlock reports.
                    if(firstStackTrace != null)
                        deadlock.setStackTrace(firstStackTrace);
                    
                    DeadlockBugManager.handleDeadlock(deadlock, Thread.currentThread().getName(), sb.toString());
                    return;
                }
            }
        });
        t.setDaemon(true);
        t.setName("Deadlock Detection Thread");
        t.start();
    }

    private static long[] findDeadlockedThreads(ThreadMXBean threadMXBean) {
        if(threadMXBean.isSynchronizerUsageSupported()) {
            return threadMXBean.findDeadlockedThreads();
        } else {
            return threadMXBean.findMonitorDeadlockedThreads();
        }
    }

    /** Add locked synchronizers data. */
    private static void addLockedSynchronizers(ThreadInfo info, StringBuilder sb) {
        LockInfo[] lockInfo = info.getLockedSynchronizers();
        if(lockInfo.length > 0) {
            sb.append("\n\tNumber of locked synchronizers = " + lockInfo.length + "\n");
            for(int i = 0; i < lockInfo.length; i++) {
                sb.append("\t- " + lockInfo[i] + "\n");
            }
        }
    }
    
    /** Add more specific locking details. */
    private static void addMonitorInfo(ThreadInfo info, StringBuilder sb, int stackDepth) {
        MonitorInfo[] monitorInfos = info.getLockedMonitors();
        for(int i = 0; i < monitorInfos.length; i++) {
            MonitorInfo mi = monitorInfos[i];
            int depth = mi.getLockedStackDepth();
            if(depth == stackDepth) {
                sb.append("\t-  locked " + mi + "\n");
            }
        }
    }
    
    /** Add the LockInfo data to the report. */
    private static void addLockInfo(ThreadInfo info, StringBuilder sb) {
        Thread.State ts = info.getThreadState();
        switch (ts) {
            case BLOCKED: 
                sb.append("\t-  blocked on " + info.getLockInfo() + "\n");
                break;
            case WAITING:
            case TIMED_WAITING:
                sb.append("\t-  waiting on " + info.getLockInfo() + "\n");
                break;
            default:
        }
    }
}
