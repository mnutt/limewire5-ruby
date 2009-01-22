package org.limewire.ui.swing;

import java.util.ArrayList;
import java.util.List;

import org.limewire.service.ErrorService;

import com.limegroup.gnutella.util.LimeWireUtils;

public class UncaughtExceptionHandlerImpl implements Thread.UncaughtExceptionHandler {
    private final List<StackTraceElement> filters;

    public UncaughtExceptionHandlerImpl() {
        filters = new ArrayList<StackTraceElement>();
        filters.add(new StackTraceElement("javax.jmdns.DNSRecord", "suppressedBy", null, -1));
        // add more filters here.
    }


    public void uncaughtException(Thread thread, Throwable throwable) {
        if (LimeWireUtils.isTestingVersion()) {
            StackTraceElement[] stackTraceElements = throwable.getStackTrace();
            for (StackTraceElement stackTraceElement : stackTraceElements) {
                if (matches(stackTraceElement)) {
                    throwable.printStackTrace();
                    return;
                }
            }
            ErrorService.error(throwable, "Uncaught thread error: " + thread.getName());
        }
    }

    /**
     * Checks to see if the give stack trace matches any of the filters.
     */
    private boolean matches(StackTraceElement stackTraceElement) {
        for (StackTraceElement filter : filters) {
            if (matches(filter, stackTraceElement)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks to see if a given stack trace element matches against a given
     * filter. For a match to be successful, either the ClassName methodName and
     * line number must match. Or the class name method name can match and the
     * filter line number can be a wild card by having a negative value.
     */
    private boolean matches(StackTraceElement filter, StackTraceElement element) {
        return filter.getClassName().equals(element.getClassName())
                && filter.getMethodName().equals(element.getMethodName())
                && (filter.getLineNumber() < 0 || filter.getLineNumber() == element.getLineNumber());
    }
}
