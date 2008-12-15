package com.limegroup.bittorrent.tracking;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.HttpConnectionParams;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.StringUtils;
import org.limewire.util.URIUtils;

import com.google.inject.Provider;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentContext;
import com.limegroup.bittorrent.bencoding.Token;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Keeps track of a torrent tracker's information.
 * Also connects to the tracker to retrieve information.
 */
public class TrackerImpl implements Tracker {
	private static final Log LOG = LogFactory.getLog(Tracker.class);

	/* 25 seconds */
	
	private static final int HTTP_TRACKER_TIMEOUT = 25 * 1000;

	static final String QUESTION_MARK = "?";

	static final String EQUALS = "=";

	static final String AND = "&";
	
	private final URI uri;
	
	private final TorrentContext context;
	
	private final ManagedTorrent torrent;
    private final Provider<LimeHttpClient> clientProvider;

    private int failures;
    
    /** The key, as required by some trackers */
    private final String key;
    
    private final NetworkManager networkManager;
    private final ApplicationServices applicationServices;
        
    TrackerImpl(URI uri, TorrentContext context, ManagedTorrent torrent,
            NetworkManager networkManager,
            ApplicationServices applicationServices, Provider<LimeHttpClient> clientProvider) {
	    this.networkManager = networkManager;
        this.applicationServices = applicationServices;
		this.uri = uri;
		this.context = context;
		this.torrent = torrent;
        this.clientProvider = clientProvider;
        String k = Integer.toHexString((int)(Math.random() * Integer.MAX_VALUE));
        while(k.length() < 8) // make sure length is 8 bytes
            k = k+"0";
        key = k;
            
	}

	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.tracking.Tracker#recordFailure()
     */
	public void recordFailure() {
		failures++;
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.tracking.Tracker#recordSuccess()
     */
	public void recordSuccess() {
		failures = 0;
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.tracking.Tracker#getFailures()
     */
	public int getFailures() {
		return failures;
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.tracking.Tracker#request(com.limegroup.bittorrent.tracking.TrackerImpl.Event)
     */
	public TrackerResponse request(Event event) {
		String queryStr = createQueryString(event);
		return connectHTTP(uri, queryStr);
	}

	/**
	 * helper method creating the query string for a HTTP tracker request
	 * 
	 * @param event
	 *            the event code to send
	 * @return string, the HTTP GET query string we send to the tracker
	 */
	private String createQueryString(Event event) {
        StringBuilder buf = new StringBuilder();
		try {
			String infoHash = URLEncoder.encode(
					StringUtils.getASCIIString(context.getMetaInfo().getInfoHash()),
					org.limewire.util.Constants.ASCII_ENCODING);
			addGetField(buf, "info_hash", infoHash);

			String peerId = URLEncoder
					.encode(StringUtils.getASCIIString(applicationServices.getMyBTGUID()),
							org.limewire.util.Constants.ASCII_ENCODING);
			addGetField(buf, "peer_id", peerId);

			addGetField(buf, "key", key);

		} catch (UnsupportedEncodingException uee) {
            ErrorService.error(uee);
        }

        addGetField(buf, "ip", NetworkUtils.ip2string(networkManager.getAddress()));

        addGetField(buf, "port", String.valueOf(networkManager.getPort()));

		addGetField(buf, "downloaded", 
				String.valueOf(torrent.getTotalDownloaded()));

		addGetField(buf, "uploaded", 
				String.valueOf(torrent.getTotalUploaded()));

		addGetField(buf, "left", String.valueOf(context.getFileSystem().getTotalSize()
				- context.getDiskManager().getBlockSize()));

		addGetField(buf, "compact", "1");

		addEventFields(event, buf);

		if (LOG.isDebugEnabled())
			LOG.debug("tracker query " + buf.toString());
		return buf.toString();
	}

	/**
	 * connects to a tracker via HTTP
	 * 
	 * @param uri
	 *            the URL for the tracker
	 * @param query
	 *            the HTTP GET query string, sent to the tracker
	 * @return TrackerResponse or null if there was a problem connecting
	 */
	private TrackerResponse connectHTTP(URI uri, String query) {
        HttpResponse response = null;
        HttpGet get = null;
        LimeHttpClient client = clientProvider.get();
        try {
            URI requestURI = URIUtils.toURI(uri.toASCIIString() + query);
            get = new HttpGet(requestURI);
            get.addHeader("User-Agent", LimeWireUtils.getHttpServer());
            get.addHeader("Cache-Control", "no-cache");
            get.addHeader(HTTPHeaderName.CONNECTION.httpStringValue(),
                    "close");
            
            HttpConnectionParams.setConnectionTimeout(client.getParams(), HTTP_TRACKER_TIMEOUT);
            HttpConnectionParams.setSoTimeout(client.getParams(), HTTP_TRACKER_TIMEOUT);

            response = client.execute(get);
            // response too long
            if (response.getEntity() != null) {
                if (response.getEntity().getContentLength() > 32768) {
                    return null;
                }

                byte[] body = IOUtils.readFully(response.getEntity().getContent());

                if (body.length == 0)
                    return null;

                if (LOG.isDebugEnabled())
                    LOG.debug(new String(body));
                return new TrackerResponse(Token.parse(body));
            }
            return null;
        } catch (IOException e) {
            return null;
        } catch (URISyntaxException e) {
            return null;
        } finally {
            client.releaseConnection(response);
        }
    }

	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.tracking.Tracker#toString()
     */
	@Override
    public String toString() {
		return "Tracker " + uri +" failures "+failures;
	}

    /**
     * helper method adding a field to the HTTP GET query string
     * 
     * @param buf
     *            the StringBuffer containing the query string so far
     * @param key
     *            the key for the entry in the query string
     * @param value
     *            the value of the entry in the query string
     * @return StringBuffer containing the modified query string (not really
     *         necessary because the StringBuffer given as argument will be
     *         modified)
     */
    private static StringBuilder addGetField(StringBuilder buf, String key,
            String value) {
        if (buf.length() == 0)
            buf.append(TrackerImpl.QUESTION_MARK);
        else
            buf.append(TrackerImpl.AND);
        buf.append(key);
        buf.append(TrackerImpl.EQUALS);
        buf.append(value);
        return buf;
    }
    
    private void addEventFields(Event event, StringBuilder buf) {
        if (event.getDescription() != null)            
            addGetField(buf,"event",event.getDescription());
        if (event.getNumWant() != null)
            addGetField(buf,"numwant",event.getNumWant());
    }
}
