package com.limegroup.gnutella.lws.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.protocol.HTTP;
import org.limewire.io.IOUtils;

import com.limegroup.gnutella.downloader.TestFile;

/**
 * Tiny web server that will serve one file.
 */
final class SimpleWebServer {

    private static final Log LOG = LogFactory.getLog(SimpleWebServer.class);

    private final int port;
    private final long length;
    private long bytesWritten;
    private Thread thread;
    private ServerSocket serverSocket;

    SimpleWebServer(LWSDownloadTestConstants constants) {
        this(constants.PORT, constants.LENGTH);
    }

    private SimpleWebServer(int port, long length) {
        this.port = port;
        this.length = length;
    }

    public long getBytesWritten() {
        return bytesWritten;
    }

    public void start() {
        thread = new Thread(new Runnable() {
            public void run() {
                try {
                    LOG.info("Waiting on port " + port);
                    serverSocket = new ServerSocket(port);
                    LOG.info("Have server socket " + serverSocket);
                    Socket socket = serverSocket.accept();
                    LOG.info("Have socket " + socket);
                    InputStream is = socket.getInputStream();
                    OutputStream os = socket.getOutputStream();
                    handle(new BufferedReader(new InputStreamReader(socket.getInputStream(), HTTP.DEFAULT_PROTOCOL_CHARSET)),
                            // server is only serving 7bit bytes and doesn't specify content encoding
                           new PrintStream(os, false, HTTP.DEFAULT_CONTENT_CHARSET));
                    socket.close();
                    is.close();
                    os.close();
                } catch (IOException e) {
                    LOG.error(e);
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
    }

    public void stop() {
        IOUtils.close(serverSocket);
        try {
            if (thread != null) {
                thread.join();
            }
        } catch (InterruptedException e) {
            LOG.error(e);
        }
    }

    private void handle(BufferedReader in, PrintStream out) throws IOException {

        // GET | HEAD
        String line1 = readLine(in);
        String[] parts1 = line1.split(" ");
        String command = parts1[0];
        @SuppressWarnings("unused")
        String fileWithSlash = parts1[1]; // ignored
        String protocol = parts1[2];

        // Just read the rest
        String line;
        while ((line = readLine(in)) != null) {
            if (line.equals("")) {
                break;
            }
        }

        // Response
        println(out, protocol + " 200 OK");
        println(out, "Date: " + date());
        println(out, "Content-Type: audio/mpeg");
        println(out, "Content-Length: " + length);
        println(out, "");

        // Maybe return the file for a HEAD request
        if (command.equals("GET")) {
            for (int i = 0; i < length; i++) {
                bytesWritten++;
                out.write(TestFile.getByte(i));
            }
        }

        // Finish up
        out.flush();

    }

    private String date() {
        Format f = new SimpleDateFormat("E, dd-MM-yyyy kk:mm:ss");
        Date date = Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime();
        return f.format(date) + " GMT";
    }

    private String readLine(BufferedReader in) throws IOException {
        return in.readLine();
    }

    private void println(PrintStream out, String msg) {
        out.print(msg);
        out.print("\r\n");
    }
}