package com.limegroup.gnutella.gui;

import com.limegroup.gnutella.bugs.BugManager;
import com.limegroup.gnutella.gui.notify.NotifyUserProxy;

/**
 * This class provides the "shutdown" method that should be
 * the only method for closing the application for production
 * (non_testing) code.  This method makes sure that all of
 * the necessary classes are notified that the virtual machine
 * is about to be exited.
 */
final class Finalizer {
    
    /** Stores the connection status before a shutdown
     * operation is initiated.
     */    
    private static boolean _wasConnected;
    
    /** Stores whether a shutdown operation has been
     * initiated.
     */    
    private static boolean _shutdownImminent;
    
    /** Indicates whether file uploads are complete.
     */    
    private static boolean _uploadsComplete;
    
    /** Indicates whether file downloads are complete.
     */    
    private static boolean _downloadsComplete;    
    
    /**
     * An update command to execute upon shutdown, if any.
     */
    private static volatile String _updateCommand;

	/**
	 * Suppress the default constructor to ensure that this class can never
	 * be constructed.
	 */
	private Finalizer() {}
    
    /** Indicates whether the application is waiting to
     * shutdown.
     * @return true if the application is waiting to
     * shutdown, false otherwise
     */    
    static boolean isShutdownImminent() {
        return _shutdownImminent;
    }
    
    /**
     * Exits the virtual machine, making calls to save
     * any necessary settings and to perform any
     * necessary cleanups.
     * @param toExecute a string to try to execute after shutting down.
     */
    static void shutdown() {
        GUIMediator.applyWindowSettings();
        
        GUIMediator.setAppVisible(false);
        ShutdownWindow window = new ShutdownWindow();
        GUIUtils.centerOnScreen(window);
        window.setVisible(true);
        
        // remove any user notification icons
        NotifyUserProxy.instance().hideTrayIcon();
        
        // Do shutdown stuff in another thread.
        // We don't want to lockup the event thread
        // (which this was called on).
        final String toExecute = _updateCommand;
        Thread shutdown = new Thread("Shutdown Thread") {
            @Override
            public void run() {
                try {
                    BugManager.instance().shutdown();
                    GuiCoreMediator.getLifecycleManager().shutdown(toExecute);                    
                    System.exit(0);
                } catch(Throwable t) {
                    t.printStackTrace();
                    System.exit(0);
                }
            }
        };
        shutdown.start();
    }
    
    static void flagUpdate(String toExecute) {
        _updateCommand = toExecute;
    }
    
    /** Exits the virtual machine, making calls to save
     * any necessary settings and to perform any
     * necessary cleanups, after all incoming and
     * outgoing transfers are complete.
     */    
    static void shutdownAfterTransfers() {
        if (isShutdownImminent())
            return;
        
        _shutdownImminent = true;
        
        _wasConnected = GuiCoreMediator.getConnectionServices().isConnected();
		
        if (_wasConnected)
            GuiCoreMediator.getConnectionServices().disconnect();
        
        if (transfersComplete())
            GUIMediator.shutdown();
    }
    
    /** Cancels a pending shutdown operation.
     */    
    public static void cancelShutdown() {
        _shutdownImminent = false;
        _uploadsComplete = false;
        _downloadsComplete = false;
        
        if (_wasConnected) {
            GuiCoreMediator.getConnectionServices().connect();
		}
		
    }
    
    /** Notifies the <tt>Finalizer</tt> that all
     * downloads have been completed.
     */    
    static void setDownloadsComplete() {
        _downloadsComplete = true;
        checkForShutdown();
    }
    
    /** Notifies the <tt>Finalizer</tt> that all uploads
     * have been completed.
     */    
    static void setUploadsComplete() {
        _uploadsComplete = true;
        checkForShutdown();
    }
    
    /** Indicates whether all incoming and outgoing
     * transfers have completed at the time this method
     * is called.
     * @return true if all transfers have been
     * completed, false otherwise.
     */    
    private static boolean transfersComplete() {        
        if (GuiCoreMediator.getDownloadServices().getNumDownloads() == 0)
            _downloadsComplete = true;
        if (GuiCoreMediator.getUploadServices().getNumUploads() == 0)
            _uploadsComplete = true;
        
        return _uploadsComplete & _downloadsComplete;
    }
    
    /** Attempts to shutdown the application.  This
     * method does nothing if all file transfers are
     * not yet complete.
     */    
    private static void checkForShutdown() {
        if(_shutdownImminent && _uploadsComplete && _downloadsComplete) {
            GUIMediator.shutdown();
        }
    }    
}
