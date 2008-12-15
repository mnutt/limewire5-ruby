package com.limegroup.gnutella.browser;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.i18n.I18nMarker;
import org.limewire.io.ByteReader;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;
import org.limewire.service.MessageService;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.DownloadServices;
import com.limegroup.gnutella.URN;

@Singleton
public class ExternalControl {
    
    private static final Log LOG = LogFactory.getLog(ExternalControl.class);

    private boolean initialized = false;
    private volatile String  enqueuedRequest = null;
    
    private final DownloadServices downloadServices;
    private final Provider<ActivityCallback> activityCallback;
    
    @Inject
    public ExternalControl(DownloadServices downloadServices,
            Provider<ActivityCallback> activityCallback) {
        this.downloadServices = downloadServices;
        this.activityCallback = activityCallback;
    }

    public static String preprocessArgs(String args[]) {
        StringBuilder arg = new StringBuilder();
		for (int i = 0; i < args.length; i++) {
			arg.append(args[i]);
		}
		return arg.toString();
	}

	public boolean isInitialized() {
		return initialized;
	}
	public void enqueueControlRequest(String arg) {
		enqueuedRequest = arg;
	}

	public void runQueuedControlRequest() {
		initialized = true;
	    if ( enqueuedRequest != null ) {
			String request   = enqueuedRequest;
			enqueuedRequest = null;
			if (isTorrentRequest(request))
				handleTorrentRequest(request);
			else
				handleMagnetRequest(request);
		}
	}
	
	/**
	 * @return true if this is a torrent request.  
	 */
	public static boolean isTorrentRequest(String arg) {
		if (arg == null) 
			return false;
		arg = arg.trim().toLowerCase(Locale.US);
		// magnets pointing to .torrent files are just magnets for now
		return arg.endsWith(".torrent") && !arg.startsWith("magnet:");
	}
	
	//refactored the download logic into a separate method
	public void handleMagnetRequest(String arg) {
	    LOG.trace("enter handleMagnetRequest");

	    ActivityCallback callback = restoreApplication();
	    MagnetOptions options[] = MagnetOptions.parseMagnet(arg);

		if (options.length == 0) {
		    if(LOG.isWarnEnabled())
		        LOG.warn("Invalid magnet, ignoring: " + arg);
			return;
        }
		
		// ask callback if it wants to handle the magnets itself
		if (!callback.handleMagnets(options)) {
		    downloadMagnet(options);
		}
	}
	
	private ActivityCallback restoreApplication() {
		activityCallback.get().restoreApplication();
		activityCallback.get().showDownloads();
		return activityCallback.get();
	}
	
	private void handleTorrentRequest(String arg) {
		LOG.trace("enter handleTorrentRequest");
		ActivityCallback callback = restoreApplication();
		File torrentFile = new File(arg.trim());
		callback.handleTorrent(torrentFile);
	}
	
	/**
	 * performs the actual magnet download.  This way it is possible to 
	 * parse and download the magnet separately (which is what I intend to do in the gui) --zab
	 * @param options the magnet options returned from parseMagnet
	 */
	public void downloadMagnet(MagnetOptions[] options) {
		
		if(LOG.isDebugEnabled()) {
            for(int i = 0; i < options.length; i++) {
                LOG.debug("Kicking off downloader for option " + i +
                          " " + options[i]);
            }
        }                 

		for ( int i = 0; i < options.length; i++ ) {

			final MagnetOptions curOpt = options[i];
			
		    if (LOG.isDebugEnabled()) {
				URN urn = curOpt.getSHA1Urn();
		        LOG.debug("Processing magnet with params:\n" +
		                  "urn [" + urn + "]\n" +
		                  "options [" + curOpt + "]");
            }

			String msg = curOpt.getErrorMessage();
			
            // Validate that we have something to go with from magnet
            // If not, report an error.
            if (!curOpt.isDownloadable()) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("Invalid magnet: " + curOpt);
                }
				msg = msg != null ? msg : curOpt.toString();
                MessageService.showFormattedError(I18nMarker.marktr("Could not process bad MAGNET link {0}"), msg);
                return;	
            }
            
            // Warn the user that the link was slightly invalid
            if( msg != null )
                MessageService.showError(I18nMarker.marktr("One or more URLs in the MAGNET link were invalid. Your file may not download correctly."));
            
            try {
            	downloadServices.download(curOpt, false);
            }
            catch ( IllegalArgumentException il ) { 
			    ErrorService.error(il);
			}
			catch (SaveLocationException sle) {
			    activityCallback.get().handleSaveLocationException(new DownloadAction() {
			        @Override
			        public void download(File saveFile, boolean overwrite)
			                throws SaveLocationException {
			            downloadServices.download(curOpt, overwrite, saveFile.getParentFile(), saveFile.getName());
			            
			        }
			    }, sle, true);
			}
		}
	}
	
	/**
	 *  Handle a Magnet request via a socket (for TCP handling).
	 *  Deiconify the application, fire MAGNET request
	 *  and return true as a sign that LimeWire is running.
	 */
	public void fireControlThread(Socket socket, boolean magnet) {
	    LOG.trace("enter fireControl");
	    
        Thread.currentThread().setName("IncomingControlThread");
		try {
			// Only allow control from localhost
			if (!NetworkUtils.isLocalHost(socket)) {
                if(LOG.isWarnEnabled())
				    LOG.warn("Invalid control request from: " + socket.getInetAddress().getHostAddress());
				return;
            }

			// First read extra parameter
			socket.setSoTimeout(Constants.TIMEOUT);
			ByteReader br = new ByteReader(socket.getInputStream());
            // read the first line. if null, throw an exception
            String line = br.readLine();
			socket.setSoTimeout(0);

			BufferedOutputStream out =
			  new BufferedOutputStream(socket.getOutputStream());
			String s = CommonUtils.getUserName() + "\r\n";
			byte[] bytes=s.getBytes();
			out.write(bytes);
			out.flush();
			if (magnet)
				handleMagnetRequest(line);
			else
				handleTorrentRequest(line);
		} catch (IOException e) {
		    LOG.warn("Exception while responding to control request", e);
		} finally {
		    IOUtils.close(socket);
        }
	}
}
