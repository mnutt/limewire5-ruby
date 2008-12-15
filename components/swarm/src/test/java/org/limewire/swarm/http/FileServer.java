package org.limewire.swarm.http;

import java.io.File;

import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.http.handler.NotFoundHandler;
import org.mortbay.http.handler.ResourceHandler;

public class FileServer {
    private final HttpServer server;

    public FileServer(int port, File resourceBaseDirectory) {
        assert resourceBaseDirectory != null;
        assert resourceBaseDirectory.exists();
        assert resourceBaseDirectory.isDirectory();
        
        this.server = new HttpServer();
        SocketListener listener = new SocketListener();
        listener.setPort(port);
        listener.setMinThreads(1);
        listener.setMaxThreads(5);
        server.addListener(listener);

        HttpContext context = server.addContext("/");
        context.setResourceBase(resourceBaseDirectory.getAbsolutePath());

        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setAcceptRanges(true);
        resource_handler.setDirAllowed(true);
        context.addHandler(resource_handler);
        context.addHandler(new NotFoundHandler());

    }

    public void destroy() {
        server.destroy();
    }

    public void join() throws InterruptedException {
        server.join();
    }

    public final void start() throws Exception {
        server.start();
    }

    public final void stop() throws Exception {
        server.stop();
    }
}
