package com.limegroup.gnutella;

/**
 * This class handles distributing internationalized strings to the back end.
 * The method to set the callback must be called immediately to allow
 * the backend to use this service during construction time.
 */
public final class MessageResourceService {	

	/**
	 * The <tt>MessageResourceCallback</tt> instance that callbacks are sent to.
	 * We use the <tt>ShellMessageResourceCallback</tt> as the default in case
	 * no other callback is set.
	 */
	private static MessageResourceCallback _callback = 
		new ShellMessageResourceCallback();

	/**
	 * Private constructor to ensure this class cannot be instantiated.
	 */
	private MessageResourceService() {}

	/**
	 * Sets the <tt>ErrorCallback</tt> class to use.
	 */
	public static void setCallback(MessageResourceCallback callback) {
		_callback = callback;
	}
	
    public static String getHTMLPageTitle() {
        return _callback.getHTMLPageTitle();
    }
    public static String getHTMLPageListingHeader() {
        return _callback.getHTMLPageListingHeader();
    }
    public static String getHTMLPageMagnetHeader() {
        return _callback.getHTMLPageMagnetHeader();
    }

	/**
	 * Helper class that simply outputs English.
	 */
	private static class ShellMessageResourceCallback 
        implements MessageResourceCallback {

        public String getHTMLPageTitle() {
            return "Download Page";
        }
        public String getHTMLPageListingHeader() {
            return "File Listing for ";
        }
        public String getHTMLPageMagnetHeader() {
            return "Magnet Links for Fast Downloads (if you have LimeWire installed)";
        }

	}
}
