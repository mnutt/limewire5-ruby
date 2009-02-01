package org.limewire.swarm.http;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.ConsumingNHttpEntityTemplate;
import org.apache.http.nio.protocol.AsyncNHttpClientHandler;
import org.apache.http.nio.protocol.NHttpRequestExecutionHandler;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.http.reactor.LimeConnectingIOReactor;
import org.limewire.http.reactor.LimeConnectingIOReactorFactory;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmFile;
import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceDownloader;
import org.limewire.swarm.http.listener.ResponseContentListener;
import org.limewire.swarm.http.listener.SwarmHttpContentListener;
import org.limewire.util.Objects;

/**
 * The SwarmHttpSource handler is responsible for processing http source
 * connections.
 * 
 * It will attempt to lease a range of bytes to download from the given
 * SwarmCoordinator, then it will attempt to download those bytes by submitting
 * a Partial Content http request to the connections server. Upon a successful
 * download it will write the bytes in batch asynchronously as they come in on
 * the reading connection.
 * 
 * When finished writing the bytes it has read from the connection it will try
 * to lease more bytes and begin the process anew.
 * 
 * When there are no more bytes left to lease, the swarm sources state will be
 * marked as finished, and the connection to it will be closed.
 * 
 * Additionally the swarmSource can be checked to see if it is finished by use
 * of the isFinished() flag. This can be used to override the default behavior,
 * which is to download until there are no more bytes to lease.
 * 
 */
public class SwarmHttpSourceDownloader implements SwarmSourceDownloader, NHttpRequestExecutionHandler {

    private static final Log LOG = LogFactory.getLog(SwarmHttpSourceDownloader.class);

    private final LimeConnectingIOReactor ioReactor;

    private final IOEventDispatch eventDispatch;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final SwarmCoordinator swarmCoordinator;

    private final SwarmStats stats;

    public SwarmHttpSourceDownloader(LimeConnectingIOReactorFactory limeConnectingIOReactorFactory, SwarmCoordinator swarmCoordinator, String userAgent) {
        this.swarmCoordinator = Objects.nonNull(swarmCoordinator, "swarmCoordinator");
        this.stats = new SwarmStats();

        HttpParams params = new BasicHttpParams();
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000).setIntParameter(
                CoreConnectionPNames.CONNECTION_TIMEOUT, 2000).setIntParameter(
                CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024).setBooleanParameter(
                CoreConnectionPNames.STALE_CONNECTION_CHECK, false).setParameter(
                CoreProtocolPNames.USER_AGENT, userAgent);

        this.ioReactor = limeConnectingIOReactorFactory.createIOReactor(params);

        SwarmAsyncNHttpClientHandlerBuilder builder = new SwarmAsyncNHttpClientHandlerBuilder(
                params, this);

        AsyncNHttpClientHandler clientHandler = builder.get();

        eventDispatch = new DefaultClientIOEventDispatch(clientHandler, params);

    }
    
    /*
     * (non-Javadoc)
     * 
     * @seeorg.limewire.swarm.SwarmSourceHandler#addSource(org.limewire.swarm.
     * SwarmSource)
     */
    public void addSource(SwarmSource source) {
        LOG.tracef("Adding source: {0}", source);
        stats.incrementNumberOfSources();

        SessionRequestCallback sessionRequestCallback = new SwarmHttpSessionRequestCallback(source);

        LOG.tracef("Connecting source to ioReactor: {0}", source);

        ioReactor.connect(source.getAddress(), null, new HttpSourceAttachment(source),
                sessionRequestCallback);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.SwarmSourceHandler#isComplete()
     */
    public boolean isComplete() {
        return swarmCoordinator.isComplete();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.SwarmSourceHandler#shutdown()
     */
    public void shutdown() throws IOException {
        LOG.tracef("Shutting down: {0}", this);
        started.set(false);
        LOG.tracef("Shutting ioReactor: {0}", ioReactor);
        ioReactor.shutdown();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.SwarmSourceHandler#start()
     */
    public void start() throws IOException {
        LOG.tracef("Starting: {0}", this);
        started.set(true);
        ioReactor.execute(eventDispatch);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.SwarmSourceHandler#isActive()
     */
    public boolean isActive() {
        return started.get();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.SwarmSourceHandler#getMeasuredBandwidth(boolean)
     */
    public float getMeasuredBandwidth(boolean downstream) {
        return ioReactor.getMeasuredBandwidth(downstream);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.http.nio.protocol.NHttpRequestExecutionHandler#initalizeContext
     * (org.apache.http.protocol.HttpContext, java.lang.Object)
     */
    public void initalizeContext(HttpContext context, Object attachment) {
        LOG.tracef("initalizeContext: {0}", this);
        HttpSourceAttachment info = (HttpSourceAttachment) attachment;
        context.setAttribute(SwarmHttpExecutionContext.HTTP_SWARM_SOURCE, info.getSource());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.http.nio.protocol.NHttpRequestExecutionHandler#finalizeContext
     * (org.apache.http.protocol.HttpContext)
     */
    public void finalizeContext(HttpContext context) {
        LOG.tracef("finalizeContext: {0}", this);
        SwarmSource source = getSwarmSource(context);
        source.connectionClosed(SwarmHttpSourceDownloader.this);
        closeContentListener(context);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.http.nio.protocol.NHttpRequestExecutionHandler#handleResponse
     * (org.apache.http.HttpResponse, org.apache.http.protocol.HttpContext)
     */
    public void handleResponse(HttpResponse response, HttpContext context) throws IOException {
        LOG.tracef("handleResponse: {0}", this);
        stats.incrementNumberOfResponses();
        if (isActive()) {
            SwarmSource source = getSwarmSource(context);
            source.responseProcessed(SwarmHttpSourceDownloader.this, new SwarmHttpSourceStatus(
                    response.getStatusLine()));

            if (LOG.isTraceEnabled()) {
                LOG.trace(SwarmHttpUtils.logReponse("response", response));
            }

            int code = response.getStatusLine().getStatusCode();
            if (!(code >= 200 && code < 300)) {
                closeConnection(context);
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.http.nio.protocol.NHttpRequestExecutionHandler#responseEntity
     * (org.apache.http.HttpResponse, org.apache.http.protocol.HttpContext)
     */
    public ConsumingNHttpEntity responseEntity(HttpResponse response, HttpContext context)
            throws IOException {
        LOG.trace(this);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Handling response: " + response.getStatusLine() + ", headers: "
                    + Arrays.asList(response.getAllHeaders()));
        }
        ResponseContentListener listener = (ResponseContentListener) context
                .getAttribute(SwarmHttpExecutionContext.SWARM_RESPONSE_LISTENER);
        int code = response.getStatusLine().getStatusCode();
        if (code >= 200 && code < 300) {
            listener.initialize(response);
            context.setAttribute(SwarmHttpExecutionContext.SWARM_RESPONSE_LISTENER, null);
            return new ConsumingNHttpEntityTemplate(response.getEntity(), listener);
        } else {
            listener.finished();
            context.setAttribute(SwarmHttpExecutionContext.SWARM_RESPONSE_LISTENER, null);
            return null;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.http.nio.protocol.NHttpRequestExecutionHandler#submitRequest
     * (org.apache.http.protocol.HttpContext)
     */
    public HttpRequest submitRequest(HttpContext context) {
        LOG.trace(this);

        if (isActive()) {
            SwarmSource swarmSource = getSwarmSource(context);
            HttpRequest request = null;
            if (swarmSource.isFinished() || isComplete()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("swarmSource.isFinished(): " + swarmSource.isFinished()
                            + " isComplete(): " + isComplete());
                }
            } else {
                request = buildRequest(context);
                if (LOG.isTraceEnabled()) {
                    LOG.trace(SwarmHttpUtils.logRequest("submitRequest", request));
                }
            }

            if (request == null) {
                LOG
                        .debugf(
                                "No more data to request for this swarm source: {0} finishing all listeners then closing connection from context.",
                                swarmSource);
                closeConnection(context);
            } else {
                stats.incrementNumberOfRequests();
            }
            return request;
        } else {
            LOG.warn("submitRequest called while not active!");
            closeConnection(context);
            return null;
        }
    }

    /**
     * Returns the swarmSource from the HttpContext
     */
    private SwarmSource getSwarmSource(HttpContext context) {
        SwarmSource swarmSource = (SwarmSource) context
                .getAttribute(SwarmHttpExecutionContext.HTTP_SWARM_SOURCE);
        return swarmSource;
    }

    /**
     * Builds the next request.
     */
    private HttpRequest buildRequest(HttpContext context) {
        HttpRequest request = null;
        SwarmSource source = getSwarmSource(context);
        RequestParameters requestParameters = buildRequestParameters(source);

        if (requestParameters != null) {
            SwarmFile swarmFile = requestParameters.getSwarmFile();
            Range leaseRange = requestParameters.getLeaseRange();
            Range downloadRange = requestParameters.getDownloadRange();
            stats.incrementNumberOfBytesRequested(downloadRange.getLength());

            context.setAttribute(SwarmHttpExecutionContext.SWARM_RESPONSE_LISTENER,
                    new SwarmHttpContentListener(swarmCoordinator, swarmFile, leaseRange));

            request = new BasicHttpRequest("GET", requestParameters.getPath());
            request.addHeader(new BasicHeader("Range", "bytes=" + requestParameters.getLow() + "-"
                    + (requestParameters.getHigh())));
        }
        return request;
    }

    private void closeContentListener(HttpContext context) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("closing content listener: " + this, new Exception("debugging stack trace"));
        }
        ResponseContentListener contentListener = (ResponseContentListener) context
                .getAttribute(SwarmHttpExecutionContext.SWARM_RESPONSE_LISTENER);
        if (contentListener != null) {
            contentListener.finished();
            context.setAttribute(SwarmHttpExecutionContext.SWARM_RESPONSE_LISTENER, null);
        }
    }

    private void closeConnection(HttpContext context) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("closing connection: " + this, new Exception("debugging stack trace"));
        }
        closeContentListener(context);
        closeSwarmSource(context);
        SwarmHttpUtils.closeConnectionFromContext(context);
    }

    private void closeSwarmSource(HttpContext context) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("closing swarmSource: " + this, new Exception("debugging stack trace"));
        }
        SwarmSource swarmSource = getSwarmSource(context);
        if (swarmSource != null) {
            swarmSource.finished(SwarmHttpSourceDownloader.this);
        }
    }

    private RequestParameters buildRequestParameters(SwarmSource source) {
        IntervalSet availableRanges = source.getAvailableRanges();

        Range leaseRange = swarmCoordinator.leasePortion(availableRanges);

        if (leaseRange == null) {
            LOG.debug("No range available to lease.");
            return null;
        }

        SwarmFile swarmFile = swarmCoordinator.getSwarmFile(leaseRange);
        long fileEndByte = swarmFile.getEndBytePosition();

        if (leaseRange.getHigh() > fileEndByte) {
            Range oldRange = leaseRange;
            leaseRange = Range.createRange(leaseRange.getLow(), fileEndByte);
            leaseRange = swarmCoordinator.renewLease(oldRange, leaseRange);
        }

        long downloadRangeStart = leaseRange.getLow() - swarmFile.getStartBytePosition();
        long downloadRangeEnd = leaseRange.getHigh() - swarmFile.getStartBytePosition();

        Range downloadRange = Range.createRange(downloadRangeStart, downloadRangeEnd);

        RequestParameters requestParameters = new RequestParameters(swarmFile, source.getPath(),
                leaseRange, downloadRange);
        return requestParameters;
    }

    /**
     * Class representing the request parameters for a piece download.
     */
    private class RequestParameters {
        private final SwarmFile swarmFile;

        private final String path;

        private final Range leaseRange;

        private final Range downloadRange;

        public RequestParameters(SwarmFile swarmFile, String path, Range leaseRange,
                Range downloadRange) {
            super();
            this.swarmFile = Objects.nonNull(swarmFile, "swarmFile");
            this.path = Objects.nonNull(path, "path");
            this.leaseRange = Objects.nonNull(leaseRange, "leaseRange");
            this.downloadRange = Objects.nonNull(downloadRange, "downloadRange");
        }

        public String getPath() {
            String path = this.path;
            if (path.length() > 0 && path.charAt(path.length() - 1) == '/') {
                path += swarmFile.getPath();
            }
            return path;
        }

        public long getLow() {
            return downloadRange.getLow();
        }

        public long getHigh() {
            return downloadRange.getHigh();
        }

        public SwarmFile getSwarmFile() {
            return swarmFile;
        }

        public Range getLeaseRange() {
            return leaseRange;
        }

        public Range getDownloadRange() {
            return downloadRange;
        }
    }

    /**
     * Callback to delegate connection issues back to the swarmsource listeners.
     */
    private class SwarmHttpSessionRequestCallback implements SessionRequestCallback {
        private final SwarmSource source;

        public SwarmHttpSessionRequestCallback(SwarmSource source) {
            this.source = source;
        }

        public void cancelled(SessionRequest request) {
            source.connectFailed(SwarmHttpSourceDownloader.this);
        };

        public void completed(SessionRequest request) {
            source.connected(SwarmHttpSourceDownloader.this);
        };

        public void failed(SessionRequest request) {
            source.connectFailed(SwarmHttpSourceDownloader.this);
        };

        public void timeout(SessionRequest request) {
            source.connectFailed(SwarmHttpSourceDownloader.this);
        };

    }
}
