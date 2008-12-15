package com.limegroup.gnutella.browser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.bsf.BSFException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.entity.NFileEntity;
import org.apache.http.nio.protocol.SimpleNHttpRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.http.BasicHttpAcceptor;
import org.limewire.http.auth.AuthenticationInterceptor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.LimeWireCore;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.URN;
import com.limegroup.scripting.*;

@Singleton
public class LocalHTTPAcceptor extends BasicHttpAcceptor {

    private static final Log LOG = LogFactory.getLog(LocalHTTPAcceptor.class);

    private static final String[] SUPPORTED_METHODS = new String[] { "GET",
        "HEAD", };
    
    private final Executor magnetExecutor = ExecutorsHelper.newProcessingQueue("magnet-handler");

    /** Magnet request for a default action on parameters */
//    private static final String MAGNET_DEFAULT = "/magnet10/default.js?";

    /** Magnet request for a paused response */
//    private static final String MAGNET_PAUSE = "/magnet10/pause";

    /** Start of Magnet URI */
    private static final String MAGNET = "magnet:?";

    /** Magnet detail command */
    private static final String MAGNET_DETAIL = "magcmd/detail?";
    private static final String FILE_URL = "/file/";

    private String lastCommand;

    private long lastCommandTime;

    private long MIN_REQUEST_INTERVAL = 1500;

    private final ExternalControl externalControl;

    public LimeWireCore core;

    @Inject
    public LocalHTTPAcceptor(ExternalControl externalControl, LimeWireCore core,
                        AuthenticationInterceptor requestAuthenticator) {
        super(createDefaultParams(LimeWireUtils.getHttpServer(), Constants.TIMEOUT),
                requestAuthenticator, SUPPORTED_METHODS);
        this.externalControl = externalControl;
	this.core = core;
        
        registerHandler("magnet:", new MagnetCommandRequestHandler());
        registerHandler("/magnet10/default.js", new MagnetCommandRequestHandler());
        registerHandler("/magnet10/pause", new MagnetPauseRequestHandler());
        registerHandler("/magcmd/detail", new MagnetDetailRequestHandler());
	registerHandler("/script*", new RubyRequestHandler());
	registerHandler("/file/*", new FileRequestHandler());
	registerHandler("/crossdomain.xml", new CrossDomainRequestHandler());
        // TODO figure out which files we want to serve from the local file system
        //registerHandler("*", new FileRequestHandler(new File("root"), new BasicMimeTypeProvider()));
    }
   
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    @Override
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Magnet Processor");
    }
    
    private class MagnetCommandRequestHandler extends SimpleNHttpRequestHandler  {
        public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                HttpContext context) throws HttpException, IOException {
            return null;
        }
        
        @Override
        public void handle(HttpRequest request, HttpResponse response,
                HttpContext context) throws HttpException, IOException {
            final String uri = request.getRequestLine().getUri();
            magnetExecutor.execute(new Runnable() {
                public void run() {
                    try {
                        triggerMagnetHandling(uri);
                    } catch(IOException ignored) {}
                }
            });
        }
    }

    private class MagnetPauseRequestHandler extends SimpleNHttpRequestHandler {
        public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                HttpContext context) throws HttpException, IOException {
            return null;
        }
        
        @Override
        public void handle(HttpRequest request, HttpResponse response,
                HttpContext context) throws HttpException, IOException {
            response.setStatusCode(HttpStatus.SC_NO_CONTENT);
            magnetExecutor.execute(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException e) {
                    }
                }
            });
        }
    }

    private class RubyRequestHandler extends SimpleNHttpRequestHandler {
        public RubyEvaluator reval = null;
        public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                HttpContext context) throws HttpException, IOException {
                System.out.println("RRHandler");
            return null;
        }
        
        @Override
        public void handle(HttpRequest request, HttpResponse response,
                HttpContext context) throws HttpException, IOException {
            if(reval == null) {
                reval = new RubyEvaluator();
            }
            System.out.println("going to evaluate");
            AbstractHttpEntity entity = null;
            try {
                entity = reval.eval(core, request);
                response.setEntity(entity);
            } catch (BSFException e) {
                e.printStackTrace();
            }
        }
    }

    private class MagnetDetailRequestHandler extends SimpleNHttpRequestHandler {
        public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                HttpContext context) throws HttpException, IOException {
            return null;
        }
        
        @Override
        public void handle(HttpRequest request, HttpResponse response,
                HttpContext context) throws HttpException, IOException {
            String uri = request.getRequestLine().getUri();
            int i = uri.indexOf(MAGNET_DETAIL);
            String command = uri.substring(i + MAGNET_DETAIL.length());
            String page = MagnetHTML.buildMagnetDetailPage(command);
            NStringEntity entity = new NStringEntity(page);
            entity.setContentType("text/html");
            response.setEntity(entity);
        }
    }

    
    private class CrossDomainRequestHandler extends SimpleNHttpRequestHandler {
        public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                HttpContext context) throws HttpException, IOException {
            return null;
        }
        
        @Override
        public void handle(HttpRequest request, HttpResponse response,
                HttpContext context) throws HttpException, IOException {
            String page = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
            page += "<cross-domain-policy xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.adobe.com/xml/schemas/PolicyFile.xsd\">";
            page += "<allow-access-from domain=\"*.opentape.fm\" />";
            page += "</cross-domain-policy>";
            NStringEntity entity = new NStringEntity(page);
            entity.setContentType("text/html");
            response.setEntity(entity);
        }
    }
    
    private class FileRequestHandler extends SimpleNHttpRequestHandler {
        public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                HttpContext context) throws HttpException, IOException {
            return null;
        }
        
        @Override
        public void handle(HttpRequest request, HttpResponse response,
                HttpContext context) throws HttpException, IOException {
            
            String uri = request.getRequestLine().getUri();
            int i = uri.indexOf(FILE_URL);
            String sha1 = uri.substring(i + FILE_URL.length());
            URN urn = URN.createSHA1Urn(sha1);
            FileDesc fileDesc = core.getFileManager().getGnutellaFileList().getFileDesc(urn);
            NFileEntity entity = new NFileEntity(fileDesc.getFile(), "text/html");
            entity.setContentType("application/binary");    
            response.setHeader("Content-disposition", "attachment; filename=\"" + fileDesc.getFileName() + "\";");
            response.setEntity(entity);
        }
    }

    private synchronized void triggerMagnetHandling(String uri)
            throws IOException {
        int i = uri.indexOf("?");
        if (i == -1) {
            throw new IOException("Invalid command");
        }
        String command = uri.substring(i + 1);

        // suppress duplicate requests from some browsers
        long currentTime = System.currentTimeMillis();
        if (!command.equals(lastCommand) || (currentTime - lastCommandTime) >= MIN_REQUEST_INTERVAL) {
            // trigger an operation
            externalControl.handleMagnetRequest(MAGNET + command);
            lastCommand = command;
            lastCommandTime = currentTime;

        } else {
            LOG.warn("Ignoring duplicate request: " + command);
        }
    }
    
}
