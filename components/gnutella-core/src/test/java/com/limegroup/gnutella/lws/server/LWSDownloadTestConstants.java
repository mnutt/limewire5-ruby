package com.limegroup.gnutella.lws.server;

/**
 * Some reused values in the download tests.
 */
final class LWSDownloadTestConstants {
    
    /** Name of the file we're downloading. */
    final String FILE = "test.mp3";
    
    /** Relative URL of the file we're downloading. */
    final String URL = "/" + FILE;
    
    /** The length of the file */
    final long LENGTH = 400000;
    
    /** Dummy ID of the progress bar we're updating, passed to the <code>Download</code> message. */
    final String ID = "123456";
    
    /** Host from where we're downloading. */
    final String HOST = "127.0.0.1";
    
    /** Port on which the {@link SimpleWebServer} will be running. */
    final int PORT = 8011;
    
    /** The amount we wait for a download.  This should be much more than needed. */
    final long DOWNLOAD_WAIT_TIME = 1000 * 20 * 1;
}
