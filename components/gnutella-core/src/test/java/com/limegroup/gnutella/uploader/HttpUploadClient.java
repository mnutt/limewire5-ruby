package com.limegroup.gnutella.uploader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;

import junit.framework.Assert;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicLineParser;
import org.limewire.util.AssertComparisons;

public class HttpUploadClient {

    private Socket socket;

    private BufferedReader in;

    private BufferedWriter out;

    public HttpUploadClient() {
    }

    public void close() throws IOException {
        if (socket != null) {
            // close connection
            socket.close();
            socket = null;
        }
    }

    public void connect(String host, int port) throws IOException {
        close();

        socket = new Socket(host, port);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket
                .getOutputStream()));
    }

    public HttpResponse sendRequest(HttpRequest request) throws Exception {
        writeRequest(request);
        return readResponse();
    }

    public void writeRequest(HttpRequest request)
            throws IOException {
        out.write(request.getRequestLine().toString());
        out.write("\r\n");
        for (org.apache.http.Header header : request.getAllHeaders()) {
            out.write(header.toString());
            out.write("\r\n");
        }
        out.write("\r\n");
        out.flush();
    }

    public HttpResponse readResponse() throws IOException,
            ProtocolException, UnsupportedEncodingException {
        // read status line
        String line = in.readLine();
        Assert.assertNotNull("Unexpected end of stream", line);
        BasicHttpResponse response = new BasicHttpResponse(BasicLineParser.parseStatusLine(line, null));

        // read headers
        while ((line = in.readLine()) != null) {
            if ("".equals(line)) {
                break;
            }

            int i = line.indexOf(":");
            AssertComparisons.assertNotEquals("Malformed header: " + line, -1, i);
            String name = line.substring(0, i);
            String value = line.substring(i + 2);
            response.addHeader(name, value);
        }
        Assert.assertNotNull("Unexpected end of stream while reading headers",
                line);
        return response;
    }

    public String readBody(HttpResponse response) throws IOException {
        int contentLength = -1;
        for (org.apache.http.Header header : response.getAllHeaders()) {
            if ("Content-Length".equals(header.getName())) {
                contentLength = Integer.parseInt(header.getValue());
            }
        }
        // read body
        StringBuilder body = new StringBuilder();
        while (contentLength == -1 || body.length() < contentLength) {
            int c = in.read();
            if (c == -1) {
                Assert.fail("Unexpected end of stream while reading body (read "
                        + body.length() + ", expected " + contentLength + "): "
                        + body.toString());
            }
            body.append((char) c);
        }
        return body.toString();
    }

    public boolean isConnected() {
        return (socket != null) ? socket.isConnected() : false;
    }

    public Socket getSocket() {
        return socket;
    }
    
}
