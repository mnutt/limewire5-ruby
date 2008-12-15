package org.limewire.ui.swing;

import org.limewire.service.ErrorService;

import com.limegroup.gnutella.util.LimeWireUtils;

public class UncaughtExceptionHandlerImpl implements Thread.UncaughtExceptionHandler{
    public void uncaughtException(Thread thread, Throwable throwable) {
        if(LimeWireUtils.isTestingVersion()) {
            ErrorService.error(throwable, "Uncaught thread error: " +thread.getName());
        }
    }
}
