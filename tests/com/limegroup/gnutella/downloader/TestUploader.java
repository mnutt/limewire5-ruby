package com.limegroup.gnutella.downloader;

import static junit.framework.Assert.assertEquals;
import static org.limewire.util.AssertComparisons.assertGreaterThan;
import static org.limewire.util.AssertComparisons.assertLessThan;
import static org.limewire.util.AssertComparisons.assertLessThanOrEquals;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Function;
import org.limewire.collection.IntPair;
import org.limewire.collection.RoundRobinQueue;
import org.limewire.concurrent.ManagedThread;
import org.limewire.io.BandwidthThrottle;
import org.limewire.io.IP;
import org.limewire.nio.ssl.SSLUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.DebugRunnable;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AltLocUtils;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.filters.IPList;
import com.limegroup.gnutella.http.FeaturesWriter;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPHeaderValueCollection;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.http.HttpTestUtils;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.tigertree.HashTreeWriteHandler;
import com.limegroup.gnutella.tigertree.HashTreeWriteHandlerFactory;
import com.limegroup.gnutella.tigertree.SimpleHashTreeNodeManager;
import com.limegroup.gnutella.tigertree.dime.TigerWriteHandlerFactoryImpl;

// NOT A SINGLETON!!
public class TestUploader {    
    
    private static final Log LOG = LogFactory.getLog(TestUploader.class);
    
    /** My name, for debugging */
    private volatile String name;

    /** Number of bytes uploaded */
    private volatile int fullRequestsUploaded;
    /** The number of connections received */
    private int connects=0;
    /** The maximum number of connect attempts */
    private int maxConnects = Integer.MAX_VALUE;
    /** The last request sent, e.g., "GET /get/0/file.txt HTTP/1.0" */
    private String request=null;

    /** The throttle rate in kilobytes/sec */
    private volatile float rate;    
    /**The number of bytes this uploader uploads before dying*/
    private volatile int stopAfter;
    /** This is stopped. */
    private boolean stopped;
    
    private final Object stoppedLock = new Object();
    
    /** switch to send incorrect bytes to simulate a bad uploader*/
    private boolean sendCorrupt;
    /** the boundary between stop/start to send corrupt bytes */
    private float corruptPercentage;

	private AlternateLocationCollection storedGoodLocs,storedBadLocs;
	private  List<AlternateLocation> incomingGoodAltLocs, incomingBadAltLocs;
	private URN                         _sha1;
    private boolean http11 = true;
    private ServerSocket server;
    private Socket socket;
    private boolean busy = false;
    private int retryAfter = -1;
    private int timesBusy = Integer.MAX_VALUE;
    //Note about queue testing: This is how the queuing simulation works: If
    //queue is set, the uploader sends the X-Queue header etc in handleRequest
    //method, and sets values for minPollTime and maxPollTime. In the loop
    //method, if queue was set, when handleRequest returns, queue is set to
    //false and handleRequest is called one more time. This time handleRequest
    //will upload the file normally, but it will also check that the downloader
    //1. Kept the socket open 2. Responsed within the given time range.
    //3. completed the download after coming out of the queue.
    private boolean queue = false;
    private long minPollTime = -1;
    private long maxPollTime = -1;
    public final int MIN_POLL = 45000;
    public final int MAX_POLL = 120000;
    private int partial = 0;
    private Long creationTime = null;
    
    private boolean unqueue = true;
    private volatile int queuePos = 1;

    private boolean killedByDownloader = false;
    
    private int start,stop;
    /**
     * The offset for the low chunk.
     */
    private int lowChunkOffset = 0;
    
    /**
     * The offset for the high chunk.
     */
    private int highChunkOffset = 0;
        
    /**
     * The number of requests this uploader has received.
     */
    private int requestsReceived = 0;
    
    /**
     * Whether or not this uploader should respond with HTTP/1.1
     */
    private boolean respondWithHTTP11 = true;
    
    /**
     * Whether or not we'll include the THEX-Tree header in our response.
     */
    private boolean sendThexTreeHeader = false;
    
    /**
     * Whether or not we'll include the THEX-Tree in our response.
     */
    private boolean sendThexTree = false;    
    
    /**
     * Whether or not thex was requested.
     */
    private boolean thexWasRequested = false;
    
    /**
     * Whether or not to send the content length in the response.
     */
    private boolean sendContentLength = true;
    
    /**
     * If we should queue when thex is requested.
     */
    private boolean queueOnThex = false;
    
    /**
     * Whether or not we should use a bad THEX response header.
     */
    private boolean useBadThexResponseHeader = false;
    
    /**
     * whether or not we are interested in receiving push locs
     */
    private boolean interestedInFalts = false;
    
    /**
     * whether we are firewalled
     */
    private boolean isFirewalled = false;
    
    /**
     * whether we stall while writing headers
     */
    private boolean stallHeaders = false;
    
    /**
     * if not -1 X-FWTP is written out with the value
     */
    private int FWTPort = -1;
    
    /**
     * Use this to throttle sending our data
     */
    private BandwidthThrottle throttle;
    
    /**
     * a callback to notify every time we start an http11 request
     */
    private HTTP11Listener _httpListener;

    /**
     * The sum of the number of bytes we need to upload across all requests.  If
     * this value is less than totalUploaded and the uploader encountered an
     * IOException in handle request it means the downloader killed the
     * connection.
     */
    private int totalAmountToUpload;

    /**
     * <tt>IPFilter</tt> for only allowing local connections.
     */
    private static final IPList local = new IPList();
    static {
        local.add("127.0.0.1");
    }
    
    
    /**
     * String to send if we are writing the X-Push-Proxies header
     */
    private String _proxiesString;

    private final AlternateLocationFactory alternateLocationFactory;

    private final NetworkManager networkManager;
    
    /**
     * If the network manager we got is a stub, keep a ref here 
     * so it can be customized.
     * INVARIANT: networkManagerStub == networkManager
     */
    private final NetworkManagerStub networkManagerStub;
    
    @Inject
    public TestUploader(Injector injector) {
        this(injector.getInstance(AlternateLocationFactory.class),injector.getInstance(NetworkManager.class));
    }
    
    public TestUploader(AlternateLocationFactory alternateLocationFactory, NetworkManager networkManager) {
        this.alternateLocationFactory = alternateLocationFactory;
        this.networkManager = networkManager;
        this.networkManagerStub = networkManager instanceof NetworkManagerStub ? 
                (NetworkManagerStub) networkManager : null;
    }

    /** 
     * Starts the TestUploader listening on the given port.  Will upload a
     * special test file to any requesters via HTTP.  Non-blocking; starts
     * another thread to do the listening. 
     */
    public void start(String name, final int port, boolean tls) {
        this.name=name;
        reset();
        
        try {
            if(!tls)
                server = new ServerSocket();
            else {
                SSLContext context = SSLUtils.getTLSContext();
                SSLServerSocket sslServer = (SSLServerSocket)context.getServerSocketFactory().createServerSocket();
                sslServer.setEnabledCipherSuites(new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" } );
                sslServer.setNeedClientAuth(false);
                sslServer.setWantClientAuth(false);
                server = sslServer;
            }
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            LOG.debug("Couldn't bind socket to port "+port+"\n");
            throw new RuntimeException(e);
        }
        
        //spawn loop();
        Thread t = new ManagedThread(new DebugRunnable(new Runnable() {
            @Override
            public void run() {
                loop(port);
            }
        }));
        t.setDaemon(true);
        t.start();        
    }
    
    void start(String name) throws IOException {
        this.name=name;
        reset();
        LOG.debug("starting to handle request with direct socket given");
        
        Thread t = new ManagedThread(name) {
            public void run() {
                synchronized(TestUploader.this) {
                    try{
                        while(socket==null) {
                            LOG.debug("socket is null");
                            TestUploader.this.wait();
                        }
                    } catch(InterruptedException hmm) {
                        throw new RuntimeException(hmm);
                    }
                }
                Runnable r = new SocketHandler(socket);
                r.run();
            }
        };
        t.start();

    }
    
    public synchronized void setSocket(Socket s) {
        LOG.debug("setting socket");
        socket=s;
        notify();
    }

    public void stopThread() {
        LOG.debug("stopping thread");
        try {
            if ( server != null )
                server.close();
        } catch (IOException e) {}
    }

    /** 
     * Resets the rate, amount uploaded, stop byte, etc.
     */
    public void reset() {
        LOG.debug("resetting uploader "+name);
	    storedGoodLocs   = null;//new AlternateLocationCollection();
	    storedBadLocs = null;
	    incomingGoodAltLocs = null;//new AlternateLocationCollection();
        fullRequestsUploaded = 0;
        stopAfter = -1;
        rate = 10000;
        stopped = false;
        sendCorrupt = false;
        corruptPercentage = 0;
        busy = false;
        retryAfter = -1;
        timesBusy = Integer.MAX_VALUE;
        queue = false;
        partial = 0;
        minPollTime = -1;
        maxPollTime = -1;
        unqueue = true;
        queuePos=1;
        killedByDownloader = false;
        totalAmountToUpload = 0;
        requestsReceived = 0;
        connects = 0;
        maxConnects = Integer.MAX_VALUE;
        lowChunkOffset = 0;
        highChunkOffset = 0;
        respondWithHTTP11 = true;
        sendThexTreeHeader = false;
        sendThexTree = false;
        thexWasRequested = false;
        sendContentLength = true;
        queueOnThex = false;
        useBadThexResponseHeader = false;
        _httpListener = null;
        incomingBadAltLocs = new ArrayList<AlternateLocation>();
        incomingGoodAltLocs = new ArrayList<AlternateLocation>();
        FWTPort = -1;
    }

    public int fullRequestsUploaded() {
        return fullRequestsUploaded;
    }
    
    public int getAmountUploaded() {
        return fullRequestsUploaded+amountThisRequest;
    }
    
    /**
     * If not -1 port is written out in X-FWTP header
     */
    public void setFWTPort(int port) {
        FWTPort = port;
    }
    
    /** Sets the upload throttle rate 
     * Note: Even if the rate is set to zero the send method will send atleast 
     * one byte per second, in order to detect socket closes. 
     * @param rate kilobytes/sec. 
     */   
    public void setRate(float rate) {
        this.rate=rate;
    }
    
    /**
     * Determine how many times this test uploader should respond
     * with a busy status.
     */
    public void setTimesBusy(int nTimes) {
        this.timesBusy = nTimes;
    }
    
    /**
     * Determines what should be sent in the Retry-After header
     */
    public void setRetryAfter(int seconds) {
        this.retryAfter = seconds;
    }
    
    /**
     * Sets this test uploader to be busy.
     * Use setTimesBusy to set the number of times this
     * uploader should respond as busy.
     * By default it will respond Integer.MAX_VALUE times.
     */
    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public void setQueue(boolean q) { 
        this.queue = q;
    }

    public void setCreationTime(Long l) {
        this.creationTime = l;
    }

    public void setPartial(boolean part) {
        if(part)
            this.partial = 1;
        else
            this.partial = 0;//reset
    }

    /** Sets whether this should send bad data. */
    public void setCorruption(boolean corrupt) {
        this.sendCorrupt = corrupt;
    }
    
    /**
     * Sets the corrupt percentage of the corrupted bytes.
     */
    public void setCorruptPercentage(float num) {
        this.corruptPercentage = num;
    }

    /** 
     * Sets the number of  bytes that this should send.  This lets the user
     * simulate a broken upload.
     * @param n the number of  bytes to send, or -1 if no limit 
     */
    public void stopAfter(int n) {
        this.stopAfter = n;
    }

    /** 
     * Store the alternate locations that this uploader knows about.
     * @param alts the alternate locations
     */
    public void setGoodAlternateLocations(AlternateLocationCollection alts) {
        storedGoodLocs = alts;
    }
    
    public void setBadAlternateLocations(AlternateLocationCollection alts) {
    	storedBadLocs = alts;
    }
    /**
     * Sets the offset for the low chunk.
     */
    public void setLowChunkOffset(int offset) {
        lowChunkOffset = offset;
    }
    
    /**
     * Sets the offset for the high chunk.
     */
    public void setHighChunkOffset(int offset) {
        highChunkOffset = offset;
    }
    
    /**
     * Sets whether or not this uploader will respond with HTTP/1.1
     */
    public void setHTTP11(boolean yes) {
        respondWithHTTP11 = yes;
    }
    
    public void setHTTPListener(HTTP11Listener listener) {
    	_httpListener = listener;
    }
    
    /**
     * Sets whether or not we'll send the thex tree header in our response.
     */
    public void setSendThexTreeHeader(boolean yes) {
        sendThexTreeHeader = yes;
    }    
    
    /**
     * Sets whether or not we'll send the thex tree in our response.
     */
    public void setSendThexTree(boolean yes) {
        sendThexTree = yes;
    }
    
    /**
     * Determiens whether or not thex was requested this time.
     */
    public boolean thexWasRequested() {
        return thexWasRequested;
    }
    
    /**
     * Sets whether or not to send the content length in the response.
     */
    public void setSendContentLength(boolean yes) {
        sendContentLength = yes;
    }
    
    /**
     * Sets whether or not to queue on the thex request.
     */
    public void setQueueOnThex(boolean yes) {
        queueOnThex = yes;
    }
    
    /**
     * Sets whether or not the uploader should receive falts
     */
    public void setInterestedInFalts(boolean yes) {
        interestedInFalts=yes;
    }
    
    /**
     * sets whether the uploader is firewalled, which affects headers written
     */
    public void setFirewalled(boolean yes) {
        if (networkManagerStub != null)
            networkManagerStub.setAcceptedIncomingConnection(!yes);
        isFirewalled=yes;
    }
    
    /**
     * sets which proxies we should write in the proxies header
     */
    public void setProxiesString(String str) {
        _proxiesString=str;
    }
    
    /**
     * Sets whether or not we'll use a bad thex response header.
     */
    public void setUseBadThexResponseHeader(boolean yes ) {
        useBadThexResponseHeader = yes;
    }
    
    public void setStallHeaders(boolean yes) {
        stallHeaders = yes;
    }
    
    /** 
     * Get the alternate locations that this uploader has read from headers
     */
    public URN getReportedSHA1() {
        return _sha1;
    }
    
    /** Returns the number of connections this accepted. */
    public int getConnections() {
        return connects;
    }
    
    /**
     * Sets the maximum amount of connects allowed.
     */
    public void setMaxConnects(int max) {
        maxConnects = max;
    }
    
    /**
     * Returns the number of requests this received.
     */
    public int getRequestsReceived() {
        return requestsReceived;
    }
        
    /** Returns the last request sent or null if none. 
     *  @return a request like "GET /get/0/file.txt HTTP/1.1" */
    public String getRequest() {
        return request;
    }

    /**
     * Repeatedly accepts connections and handles them.
     */
    private void loop(int port) {
        Socket socket = null;
        while(true) {
            try {
                socket = server.accept();
                connects++;
                if(connects > maxConnects) {
                    LOG.debug("over-connected");
                    socket.close();
                    continue;
                }
                    

                // make sure it's from us
				InetAddress address = socket.getInetAddress();
                if (isBannedIP(address.getHostAddress())) {
                    LOG.debug("Banned address -- closing");
                    socket.close();
                    continue;
                }
                LOG.debug("Uploader accepted connection");
                //spawn thread to handle request
                final Socket mySocket = socket;
                Thread runner=new ManagedThread(new SocketHandler(mySocket),name);
                runner.start();
            } catch (IOException e) {
                LOG.debug("exception in accept", e);
                try { server.close(); } catch (IOException ignore) { }
                return;  //server socket closed.
            }
            //handling next request
        }
    }

    /**
     * Returns whether <tt>ip</tt> is a banned address.
     * @param ip an address in resolved dotted-quad format, e.g., 18.239.0.144
     * @return true iff ip is a banned address.
     */
    public boolean isBannedIP(String ip) {        
        return !local.contains(new IP(ip));
    }
    
    
    private void handleRequest(Socket socket) throws IOException {
        LOG.debug("Handling a request on socket: " + socket);
        
        //Find the region of the file to upload.  If a Range request is present,
        //use that.  Otherwise, send the whole file.  Skip all other headers.
        //TODO2: Later we should also check the validity of the requests
        BufferedReader input = 
            new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        OutputStream output = new BufferedOutputStream(socket.getOutputStream());
        if(rate > 0)
            throttle = new BandwidthThrottle(rate*1024);
        else
            throttle = new BandwidthThrottle(Float.MAX_VALUE);
        start = 0;
        stop = TestFile.length();
        boolean firstLine=true;
        boolean thexReq = false;
        
        while (true) {
            String line=input.readLine();
            LOG.debug("read "+line);
            if (firstLine) {
                if(line != null && !line.equals("")) {
                    requestsReceived++;
                }
                request=line;
                firstLine=false;
            }
            if (line==null)
                throw new IOException("Unexpected close");
            if (line.equals(""))
                break;

			if(HTTPHeaderName.GNUTELLA_CONTENT_URN.matchesStartOfString(line)) {
				_sha1 = readContentUrn(line);
			}
            
            if(HTTPHeaderName.NALTS.matchesStartOfString(line) ||
                    HTTPHeaderName.BFALT_LOCATION.matchesStartOfString(line))
                readAlternateLocations(line,false);
			if(HTTPHeaderName.ALT_LOCATION.matchesStartOfString(line) ||
			        HTTPHeaderName.FALT_LOCATION.matchesStartOfString(line))
			    readAlternateLocations(line,true);
			    

            int i=line.indexOf("Range:");
            assertLessThanOrEquals("Range should be at the beginning or not at all", 0, i);
            if (i==0) {
                IntPair p = null;
                try {
                    p=parseRange(line);
                } catch (Exception e) { 
                    throw new RuntimeException("Bad Range request: \""+line+"\"", e);
                }
                start = Math.max(0, p.a + lowChunkOffset);
                stop = Math.min(TestFile.length(),
                               Math.max(1, p.b + highChunkOffset));

            }
            
            i = line.indexOf("GET");
            if(i==0) {
                http11 = line.indexOf("1.1") > 0;
                thexReq = line.indexOf(TestFile.tree().getThexURI()) > 0;
                thexWasRequested |= thexReq;
            }
		}

        if(thexReq && queueOnThex) {
            queueOnThex = false;
            queue = true;
            sendThexTree = true;
        }
        
        if(thexReq && useBadThexResponseHeader) {
            sendThexTree = true;
        }
        
        if(thexReq && sendThexTree && !queue) {
        	if (_httpListener != null)
        		_httpListener.thexRequestStarted();
            LOG.debug("sending thex tree.");
            sendThexTree = false;
            sendThexTree(output);
            output.flush();
            LOG.debug("done sending thex tree.");
        } else {    
            //Send the data.
        	if (_httpListener != null)
        		_httpListener.requestStarted(this);
            send(output, start, stop);
        }
        
        if (_httpListener != null) {
            if (thexReq)
                _httpListener.thexRequestHandled();
            else
                _httpListener.requestHandled();
        }
    }
    
    private void sendThexTree(OutputStream out) throws IOException {
        HashTreeWriteHandlerFactory tigerWriteHandlerFactory = new TigerWriteHandlerFactoryImpl(
                new SimpleHashTreeNodeManager());
        HashTreeWriteHandler tigerWriteHandler = tigerWriteHandlerFactory
                .createTigerWriteHandler(TestFile.tree());
        
        if(!useBadThexResponseHeader) {
            String str = "HTTP/1.1 200 OK\r\n" +
                         "ugly-header: ugly-value\r\n" + 
                         "hot diggity doo\r\n" +
                         "Content-Length: " + tigerWriteHandler.getOutputLength() + "\r\n" + 
                         "\r\n";
            out.write(str.getBytes());
            tigerWriteHandler.write(out);
        } else {
            String body = "You have failed miserably in your attempts.";
            String str = "HTTP/1.1 9000 Failed Miserably\r\n" +
                         "Content-Length: " + body.length() + "\r\n" +
                         "\r\n";
            out.write(str.getBytes());
            out.write(body.getBytes());
        }
    }

    private void send(OutputStream out, int start, int stop) 
        throws IOException {
        LOG.debug("starting to send data "+start+"-"+stop);
        totalAmountToUpload += stop - start;
        //Write header, stolen from NormalUploadState.writeHeader()
        long t0 = System.currentTimeMillis();
        if(minPollTime > 0) 
            assertGreaterThan("queued downloader responded too quick by "+
                    (minPollTime-t0)+" mS", minPollTime, t0);
                        
        if(maxPollTime > 0)
            assertLessThan("queued downloader responded too late, by "+
                    (t0-maxPollTime) +" mS", maxPollTime, t0);
                                

        String httpValue = respondWithHTTP11 ? "HTTP/1.1" : "HTTP/1.0";
		String str = httpValue + " " +
            (busy || queue || partial==1 ?
            "503 Service Unavailable\r\n" :
            "200 OK \r\n");
		out.write(str.getBytes());
		
		if(busy && retryAfter != -1) {
		    str = "Retry-After: " + retryAfter + "\r\n";
		    out.write(str.getBytes());
		}

        if(queue) {
            LOG.debug("Upload Queued");
            str = "X-Queue: position="+queuePos+
                ", pollMin=" + MIN_POLL/1000 + 
                ", pollMax=" + MAX_POLL/1000 +
                "\r\n";
            out.write(str.getBytes());
            str = "\r\n";
            out.write(str.getBytes());
            out.flush();//don't close socket
            long t = System.currentTimeMillis();
            minPollTime = t+MIN_POLL;
            maxPollTime = t+MAX_POLL;
            return;
        }

        if(partial > 0) {
            switch(partial) {
            case 1:
                str="X-Available-Ranges: ByTes 50000-400000, 500000-600000,800000-900000\r\n";
                break;
            case 2:
                str="X-Available-Ranges: bytes 50000-900000\r\n";
                break;
            default:
                str="X-Available-Ranges: bytes 50000-150000\r\n";
            }
            out.write(str.getBytes());
            out.flush();
            partial++;
            if(partial==2) {//was 1 until last statement
                str="\r\n";
                out.write(str.getBytes());
                out.flush();
                return;
            }
        }
        if(sendContentLength) {
		    str = "Content-Length:"+ (stop - start) + "\r\n";
		    out.write(str.getBytes());	   
        }
        
		if (start != 0 || (stop - start != TestFile.length())) {
            //Note that HTTP stop values are INCLUSIVE.  Our internal values
            //are EXCLUSIVE.  Hence the -1.
			str = "Content-range: bytes " + start  +
			"-" + (stop-1) + "/" + TestFile.length() + "\r\n"; 
			out.write(str.getBytes());
		}
		if(storedGoodLocs != null && storedGoodLocs.hasAlternateLocations()) {

            LOG.debug("Writing alternate location header:\n"+storedGoodLocs+"\n");      
            TestUploader.writeHeader(HTTPHeaderName.ALT_LOCATION,
                                  storedGoodLocs, out);
        } else {
            LOG.debug("Did not write alt locs:\n"+storedGoodLocs+"\n");
        }
		if(storedBadLocs != null && storedBadLocs.hasAlternateLocations()) {

            LOG.debug("Writing alternate location header:\n"+storedBadLocs+"\n");      
            TestUploader.writeHeader(HTTPHeaderName.NALTS,
                                  storedBadLocs, out);
        } else {
            LOG.debug("Did not write alt locs:\n"+storedBadLocs+"\n");
        }
        if(creationTime != null) {
            LOG.debug("Writing out Creation Time.");
            TestUploader.writeHeader(HTTPHeaderName.CREATION_TIME, ""+creationTime,
                                  out);
        }
        if(sendThexTreeHeader) {
            TestUploader.writeHeader(HTTPHeaderName.THEX_URI,
                                  TestFile.tree(),
                                  out);
        }
        if(interestedInFalts) {
            FeaturesWriter featuresWriter = new FeaturesWriter(networkManager);
            Set<HTTPHeaderValue> features = featuresWriter.getFeaturesValue();
            // Write X-Features header.
            if (features.size() > 0) {
                TestUploader.writeHeader(HTTPHeaderName.FEATURES, new HTTPHeaderValueCollection(
                        features), out);
            }
        }
        

        if (isFirewalled && _proxiesString!=null) {
            TestUploader.writeHeader(HTTPHeaderName.PROXIES,_proxiesString,out);
            if (FWTPort != -1) {
                TestUploader.writeHeader(HTTPHeaderName.FWTPORT, FWTPort, out);
            }
        }
        
        
        out.flush();
        if (stallHeaders) {
            LOG.debug("stalling as requested");
            try {
                Thread.sleep(100000000);
            } catch (InterruptedException end) {
                return;
            }
        }
        
        str = "\r\n";
		out.write(str.getBytes());
        out.flush();
        if (busy) {
            //turn busy off for the next time if necessary
            if( connects >= timesBusy )
                busy = false;
            out.close();
            return;
        }
        
        int length = stop-start;
        int okLength = length - (int)(length * corruptPercentage);
        
        amountThisRequest = 0;
        boolean sentCorrupt = false;
        for (int i=start; i<stop; ) {
            
            //1 second write cycle
            if (stopAfter > -1){
                assertLessThanOrEquals(
                        "total "+fullRequestsUploaded+" this "+amountThisRequest+" stop "+stopAfter,
                        stopAfter, fullRequestsUploaded + amountThisRequest);
                if (fullRequestsUploaded + amountThisRequest == stopAfter) {
                    
                    stopped=true;
                    synchronized (stoppedLock) {
                        stoppedLock.notifyAll();
                    }
                    out.flush();
                    LOG.debug(name+" stopped at "+(fullRequestsUploaded + amountThisRequest));
                    throw new IOException();
                }
            }
            throttle.request(1);
            if(sendCorrupt 
                    && (i-start) >= okLength) {
                sentCorrupt = true;
                out.write(TestFile.getByte(i)+(byte)1);
            }
            else
                out.write(TestFile.getByte(i));
            
            amountThisRequest++;
            i++;
            
        }
        
        if (sentCorrupt)
            LOG.debug("corrupt data sent");
        out.flush();
        // only if the flush didn't throw do we add to fullRequestsUploaded
        fullRequestsUploaded += amountThisRequest;
        amountThisRequest = 0;
    }

    /**
     * Returns start (inclusive) and stop range (exclusive) from the given Range
     * header.  In other words, "Range; 2-5" will return [2, 6). Extracted from
     * HTTPUploader.
     *
     * @exception IndexOutOfBoundsException parse error
     * @exception NumberFormatException parse error
     * @exception IOException socket closed
     */
    private IntPair parseRange(String str) 
            throws IndexOutOfBoundsException, 
                   NumberFormatException, IOException {
        int start=0;
        int stop=TestFile.length();
        //Set 'sub' to the value after the "bytes=" or "bytes ".  Note
        //that we don't validate the data between "Range:" and the
        //bytes.
        String sub;
        String second;
        try {
            int i=str.indexOf("bytes");    //TODO: use constant
            if (i<0)
                throw new IOException();
            i+=6;                          //TODO: use constant
            sub = str.substring(i);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException();
        }
        // remove the white space
        sub = sub.trim();   
        char c;
        // get the first character
        try {
            c = sub.charAt(0);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException();
        }
        // - n  
        if (c == '-') {  
            // String second;
            try {
                second = sub.substring(1);
            } catch (IndexOutOfBoundsException e) {
                throw new IOException();
            }
            second = second.trim();
            try {
                //A range request for "-3" means return the last 3 bytes
                //of the file.  (LW used to incorrectly return bytes
                //0-3.)  
                start = Math.max(0, TestFile.length()-Integer.parseInt(second));
            } catch (NumberFormatException e) {
                throw new IOException();
            }
        }
        else {                
            // m - n or 0 -
            int dash = sub.indexOf("-");
            String first;
            try {
                first = sub.substring(0, dash);
            } catch (IndexOutOfBoundsException e) {
                throw new IOException();
            }
            first = first.trim();
            try {
                start = java.lang.Integer.parseInt(first);
            } catch (NumberFormatException e) {
                throw new IOException();
            }
            try {
                second = sub.substring(dash+1);
            } catch (IndexOutOfBoundsException e) {
                throw new IOException();
            }
            second = second.trim();
            if (!second.equals("")) 
                try {
                    stop = java.lang.Integer.parseInt(second)+1; //sic
                } catch (NumberFormatException e) {
                    throw new IOException();
                }
        }
        //check that the downloader made a correct range request if we are
        //partial and this is the second iteration. 
        if(partial==3) {//check this before checking partial2 -- we set it there
            assertEquals("downloader picked incorrect start range in iteration",
                         150010, start);
            assertEquals("downloader pick incorrect stop range in iteration",
                         250020, stop);
        }
        if(partial==2) {
            assertEquals("invalid start range", 50000, start);
            assertEquals("invalid stop range", 150010, stop);
        }                
        return new IntPair(start, stop);
    }
	/**
	 * Reads alternate location header.  The header can contain only one
	 * alternate location, or it can contain many in the same header.
	 * This method adds them all to the <tt>FileDesc</tt> for this
	 * uploader.  This will not allow more than 20 alternate locations
	 * for a single file.
	 *
	 * @param altHeader the full alternate locations header
	 * @param alc the <tt>AlternateLocationCollector</tt> that read alternate
	 *  locations should be added to
	 */
	private void readAlternateLocations (String altHeader, final boolean good) {
        String alternateLocations=HttpTestUtils.extractHeaderValue(altHeader);
        AltLocUtils.parseAlternateLocations(_sha1, alternateLocations, true, alternateLocationFactory, new Function<AlternateLocation, Void>() {
            public Void apply(AlternateLocation location) {
                if(location instanceof PushAltLoc)
                    ((PushAltLoc)location).updateProxies(good);
                
                if (good) 
                    incomingGoodAltLocs.add(location);
                else 
                    incomingBadAltLocs.add(location);
                
                return null;
            }
        }, true);
	}

	/**
	 * This method parses the "X-Gnutella-Content-URN" header, as specified
	 * in HUGE v0.93.  This assigns the requested urn value for this 
	 * upload, which otherwise remains null.
	 *
	 * @param contentUrnStr the string containing the header
	 * @return a new <tt>URN</tt> instance for the request line, or 
	 *  <tt>null</tt> if there was any problem creating it
	 */
	private static URN readContentUrn(final String contentUrnStr) {
		String urnStr = HttpTestUtils.extractHeaderValue(contentUrnStr);
		
		// return null if the header value could not be extracted
		if(urnStr == null) return null;
		try {
			return URN.createSHA1Urn(urnStr);
		} catch(IOException e) {
			// this will be thrown if the URN string was invalid for any
			// reason -- just return null
			return null;
		}		
	}
    
    public void waitForUploaderToStop() {
        synchronized (stoppedLock) {
            while (!stopped) {
                try {
                    stoppedLock.wait();
                }
                catch (InterruptedException ie) {
                }
            }
        }
    }
	
	private static final RoundRobinQueue rr = new RoundRobinQueue();

    private int amountThisRequest;
	private class SocketHandler implements Runnable {
	    private final Socket mySocket;
	    public SocketHandler(Socket s) {
	        mySocket=s;
	    }
	    @SuppressWarnings("unchecked")
        public void run() {  
	        LOG.debug(name+" starting to upload.. ");
            try {
                while(http11 && !stopped) {

                    handleRequest(mySocket);
                    if (queue) { 
                        mySocket.setSoTimeout(MAX_POLL);
                        if(unqueue) // second time give slot
                            queue = false;
                        handleRequest(mySocket);
                    }
                    mySocket.setSoTimeout(8000);
                }
            } catch (IOException e) {
                if(fullRequestsUploaded < totalAmountToUpload)
                    killedByDownloader = true;
                LOG.debug("Exception in uploader (" + name + ")", e);
            } catch(Throwable t) {
                ErrorService.error(t);
            } finally {
                
                synchronized(rr) {
                    rr.remove(this);
                    if (rr.size() > 0){
                        Object next = rr.next();
                        synchronized(next){
                            next.notify();
                        }
                    }
                }
                try {
                    mySocket.close();
                } catch (IOException e) {
                    return;
                }
            }//end of finally
        }
	}
	
	
    public void setUnqueue(boolean unqueue) {
        this.unqueue = unqueue;
    }


    public boolean getKilledByDownloader() {
        return killedByDownloader;
    }


    public List<AlternateLocation> getIncomingGoodAltLocs() {
        return incomingGoodAltLocs;
    }
    
    public List<AlternateLocation> getIncomingBadAltLocs() {
        return incomingBadAltLocs;
    }


    public void setQueuePos(int queuePos) {
        this.queuePos = queuePos;
    }

    /**
     * Create a single http header String with the specified header name 
     * and the specified header value.
     *
     * @param name the <tt>HTTPHeaderName</tt> instance containing the
     *  header name 
     * @param valueStr the value of the header, generally the httpStringValue
     *  or a HttpHeaderValue, or just a String.
     */
    public static String createHeader(HTTPHeaderName name, String valueStr) {
        return HTTPUtils.createHeader(name.httpStringValue(), valueStr);
    }

    /**
     * Utility method for writing a header with an integer value.  This removes
     * the burden to the caller of converting integer HTTP values to strings.
     * 
     * @param name the <tt>HTTPHeaderName</tt> of the header to write
     * @param value the int value of the header
     * @param stream the <tt>OutputStream</tt> instance to write the header to
     * @throws IOException if an IO error occurs during the write
     */
    public static void writeHeader(HTTPHeaderName name, int value, OutputStream stream) throws IOException {
        writeHeader(name, String.valueOf(value), stream);
    }

    /**
     * Writes an single http header to the specified 
     * <tt>OutputStream</tt> instance, with the specified header name 
     * and the specified header value.
     *
     * @param name the <tt>HTTPHeaderName</tt> instance containing the
     *  header name to write to the stream
     * @param name the <tt>HTTPHeaderValue</tt> instance containing the
     *  header value to write to the stream
     * @param os the <tt>OutputStream</tt> instance to write to
     */
    public static void writeHeader(HTTPHeaderName name, HTTPHeaderValue value, OutputStream os) 
      throws IOException {
    	if(name == null) {
    		throw new NullPointerException("null name in writing http header");
    	} else if(value == null) {
    		throw new NullPointerException("null value in writing http header: "+
    									   name);
    	} else if(os == null) {
    		throw new NullPointerException("null os in writing http header: "+
    									   name);
    	}
    	String header = TestUploader.createHeader(name, value.httpStringValue());
    	os.write(header.getBytes());
    }

    /**
     * Writes an single http header to the specified 
     * <tt>OutputStream</tt> instance, with the specified header name 
     * and the specified header value.
     *
     * @param name the <tt>HTTPHeaderName</tt> instance containing the
     *  header name to write to the stream
     * @param value the <tt>String</tt> instance containing the
     *  header value to write to the stream
     * @param os the <tt>OutputStream</tt> instance to write to
     */
    public static void writeHeader(HTTPHeaderName name, String value, 
    							   OutputStream os) 
    	throws IOException {
    	if(name == null) {
    		throw new NullPointerException("null name in writing http header");
    	} else if(value == null) {
    		throw new NullPointerException("null value in writing http header: "+
    									   name);
    	} else if(os == null) {
    		throw new NullPointerException("null os in writing http header: "+
    									   name);
    	}
    	String header = TestUploader.createHeader(name, value);
    	os.write(header.getBytes());
    }
    
}
