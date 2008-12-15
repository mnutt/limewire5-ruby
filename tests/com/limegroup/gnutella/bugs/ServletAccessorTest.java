package com.limegroup.gnutella.bugs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ThreadFactory;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.gui.LimeWireGUIModule;
import com.limegroup.gnutella.util.URLDecoder;

public class ServletAccessorTest extends BaseTestCase {

    private final ThreadFactory FACTORY;

    private final String COMMENT_USER_ENTERED = "Testing User Comments";

    // content server received
    private String contentAsString = "";    

    /**
     * The amount of time to wait should the server be down (got it from
     * RemoteClientInfo)
     */
    private static final long FAILURE_TIME = 60 * 60 * 1000; // 1 hour

    public ServletAccessorTest(String name) {
        super(name);
        FACTORY = ExecutorsHelper.daemonThreadFactory(name);
        LimeTestUtils.createInjector(new LimeWireGUIModule());

    }

    //testing that server received user comments
    public void testGetRemoteBugInfoServerShouldHaveRecievedUserComments() {
        final int port = 4444; 
        
        // creates and starts a new thread for our server to run on
        // for the client to contact
        Thread serverSocket = FACTORY.newThread(new TestSocket(port));
        
        serverSocket.start();

        LocalClientInfo info = getLocalClientInfo();
        info.addUserComments(COMMENT_USER_ENTERED);
        ServletAccessor SA = new ServletAccessor(true, "http://localhost:" + port);
        RemoteClientInfo RCI = SA.getRemoteBugInfo(info);

        // Waits until the server gets the data and sends a response back
        try {
            serverSocket.join();
        } catch (InterruptedException ex) {
            fail(ex);
        }

        // makes sure the comment user entered is received by the server
        try {
            // extracts the comments entered by the user
            String userComments = contentAsString.substring(contentAsString.indexOf("&63=") + 4);
            assertEquals(COMMENT_USER_ENTERED, URLDecoder.decode(userComments));
        } catch (IOException ex) {
            fail(ex);
        }

        // makes sure the server is not down and no any error occurred during
        // the process of
        // sending data to the server and getting back a response

        assertNotEquals(FAILURE_TIME, RCI.getNextAnyBugTime());
    }

    //tests that the server did not receive any user comments
    public void testGetRemoteBugInfoServerShouldNotHaveRecievedUserComments() {
        final int port = 5555;

        // creates and starts a new thread for our server to run on
        // for the client to contact
        Thread serverSocket = FACTORY.newThread(new TestSocket(port));
        
        serverSocket.start();

        LocalClientInfo info = getLocalClientInfo();
        // info.addUserComments(COMMENT_USER_ENTERED);
        ServletAccessor SA = new ServletAccessor(true, "http://localhost:" + port);
        RemoteClientInfo RCI = SA.getRemoteBugInfo(info);

        // Waits until the server gets the data and sends a response back
        try {
            serverSocket.join();
        } catch (InterruptedException ex) {
            fail(ex);
        }

        // makes sure the comment user entered is received by the server

        //checks if the user comment is in the content server received
        boolean doesServerReceiveUserComments = contentAsString.contains(("&63="));
        //server is not expected to have the user comment
        assertEquals(false, doesServerReceiveUserComments);

        // makes sure the server is not down and no any error occurred during
        // the process of
        // sending data to the server and getting back a response

        assertNotEquals(FAILURE_TIME, RCI.getNextAnyBugTime());        
    }

    private LocalClientInfo getLocalClientInfo() {
        // creates a new error which will be logged and send to a server
        Throwable PROBLEM = new Throwable("Testing Get Remote Bug Info");
        String CURRENT_THREAD_NAME = Thread.currentThread().getName();
        LocalClientInfo info = new LocalClientInfo(PROBLEM, CURRENT_THREAD_NAME, null, false, null);
        return info;
    }

    /**
     * The thread for running the server.
     */
    private class TestSocket implements Runnable {
        private final int port;
        
        public TestSocket (int port) {
            this.port = port;
        }
        
        public void run() {

            try {
                // ServerSocket ss = new ServerSocket(port, 100, InetAddress.getByName("10.254.0.246")); 
                // if not using localhost
                // sets up the server and socket
                ServerSocket ss = new ServerSocket(port);
                Socket s = ss.accept();

                // prepares and receives data from the client
                InputStream in = s.getInputStream();
                BufferedReader BR = new BufferedReader(new InputStreamReader(in));
                String input = null;
                // length of the content received from the client
                int length = 0;

                // processes data
                while ((input = BR.readLine()).length() != 0) {
                    if (input.toLowerCase().startsWith("content-length")) {
                        length = Integer.parseInt(input.substring(input.indexOf(": ") + 2));
                    }
                }

                // collects content
                char[] content = new char[length];
                BR.read(content, 0, length);
                contentAsString = new String(content);

                // prepares and sends the response to the client
                OutputStream out = s.getOutputStream();
                String response;
                response = "HTTP/1.1 200 OK\r\n";
                out.write(response.getBytes());
                out.flush();
                response = "Connection: Close\r\n\r\n";
                out.write(response.getBytes());
                out.flush();
                s.close();
            } catch (IOException ex) {
                fail(ex);
            }
        }
    }
}
