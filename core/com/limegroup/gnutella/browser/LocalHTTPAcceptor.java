package com.limegroup.gnutella.browser;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;

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
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.URN;
import org.limewire.core.impl.URNImpl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.LimeWireCore;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.util.LimeWireUtils;
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
    private static final String LIBRARY_URL = "/library/";

    private String lastCommand;

    private long lastCommandTime;

    private long MIN_REQUEST_INTERVAL = 1500;

    private final ExternalControl externalControl;
    
    private LibraryManager libraryManager;

    public LimeWireCore core;

    @Inject
    public LocalHTTPAcceptor(ExternalControl externalControl, LimeWireCore core, LibraryManager libraryManager,
                        AuthenticationInterceptor requestAuthenticator) {
        super(createDefaultParams(LimeWireUtils.getHttpServer(), Constants.TIMEOUT),
                requestAuthenticator, SUPPORTED_METHODS);
        this.externalControl = externalControl;
        this.core = core;
        this.libraryManager = libraryManager;
        
        registerHandler("magnet:", new MagnetCommandRequestHandler());
        registerHandler("/magnet10/default.js", new MagnetCommandRequestHandler());
        registerHandler("/magnet10/pause", new MagnetPauseRequestHandler());
        registerHandler("/magcmd/detail", new MagnetDetailRequestHandler());
        registerHandler("/script/asset/*", new FileRequestHandler());
        registerHandler("/library/*", new LibraryRequestHandler());
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
            int i = uri.indexOf("/script/asset/");
            String filepath = uri.substring(i + "/script/asset/".length());
            i = filepath.lastIndexOf('/');
            String filename = filepath.substring(i + 1);
            i = filename.lastIndexOf('.');
            String extension = filename.substring(i + 1);
            System.out.println(extension);
            System.out.println(filepath);
            File file = new File("../../core/com/limegroup/scripting/resources/assets/" + filepath);
            NFileEntity entity = new NFileEntity(file, "application/binary");
            if(extension.contentEquals("js")) {
                entity.setContentType("text/javascript");
            } else if(extension.contentEquals("css")) {
                entity.setContentType("text/css");
            } else if(extension.contentEquals("swf")) {
                entity.setContentType("application/x-shockwave-flash");
            } else if(extension.contentEquals("html")) { 
                entity.setContentType("text/html");
            }   else {
                entity.setContentType("application/binary");
            }

            response.setEntity(entity);
        }
    }
    
    private class LibraryRequestHandler extends SimpleNHttpRequestHandler {
        public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                HttpContext context) throws HttpException, IOException {
            return null;
        }
        
        @Override
        public void handle(HttpRequest request, HttpResponse response,
                HttpContext context) throws HttpException, IOException {
            
            AbstractHttpEntity entity;
            String uri = request.getRequestLine().getUri();
            int i = uri.indexOf(LIBRARY_URL);
            String sha1 = uri.substring(i + LIBRARY_URL.length());
            if(sha1.indexOf("?") != -1) { 
              sha1 = sha1.substring(0, sha1.indexOf("?"));
            }
            System.out.println("sha1:" + sha1);
            
            URN urn = new URNImpl(com.limegroup.gnutella.URN.createSHA1Urn(sha1));
            if(libraryManager.getLibraryManagedList().contains(urn)) {
              FileDesc fileDesc = libraryManager.getLibraryManagedList().getFileDescsByURN(urn).get(0);
              entity = new NFileEntity(fileDesc.getFile(), "application/binary");
              entity.setContentType("application/binary");    
              response.setHeader("Content-disposition", "attachment; filename=\"" + fileDesc.getFileName() + "\";");
            } else {
                entity = new NStringEntity("File not found: " + sha1);
                entity.setContentType("text/plain");
            }
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
