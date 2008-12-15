/**
 * 
 */
package com.limegroup.gnutella.downloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ManagedThread;
import org.limewire.io.GUID;
import org.limewire.net.SocketsManager;
import org.limewire.service.ErrorService;
import org.limewire.util.DebugRunnable;

import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.HeadPing;
import com.limegroup.gnutella.messages.vendor.HeadPong;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;

class TestUDPAcceptor implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(TestUDPAcceptor.class);
    
    private int _portC;

    private DatagramSocket sock;

    private String _fileName;

    private TestUploader _uploader;

    private GUID _g;

    public boolean sentGIV;

    private boolean noFile;

    public int pings;

    private volatile boolean shutdown;

    private final SocketsManager socketsManager;

    private final MessageFactory messageFactory;

    private final HeadPongFactory headPongFactory;
    
    private final Thread runningThread;

    TestUDPAcceptor(SocketsManager socketsManager, MessageFactory messageFactory, HeadPongFactory headPongFactory, int port, String testMethod) {
        this.socketsManager = socketsManager;
        this.messageFactory = messageFactory;
        this.headPongFactory = headPongFactory;
        noFile = true;
        try {
            sock = new DatagramSocket(port);
            // sock.connect(InetAddress.getLocalHost(),portC);
            sock.setSoTimeout(15000);
        } catch (IOException bad) {
            ErrorService.error(bad);
        }
        this.runningThread = new ManagedThread(new DebugRunnable(this), "unnamed UDP Acceptor in method: " + testMethod);
        runningThread.start();
    }

    TestUDPAcceptor(SocketsManager socketsManager, MessageFactory messageFactory, HeadPongFactory headPongFactory, int portL, int portC, String filename, TestUploader uploader, GUID g, String testMethod) {
        this.socketsManager = socketsManager;
        this.messageFactory = messageFactory;
        this.headPongFactory = headPongFactory;
        _portC = portC;
        _fileName = filename;
        _uploader = uploader;
        _g = g;
        try {
            sock = new DatagramSocket(portL);
            // sock.connect(InetAddress.getLocalHost(),portC);
            sock.setSoTimeout(15000);
        } catch (IOException bad) {
            ErrorService.error(bad, testMethod);
        }
        this.runningThread = new ManagedThread(new DebugRunnable(this), "push acceptor " + portL + "->" + portC + " in method: " + testMethod);
        runningThread.setPriority(Thread.MAX_PRIORITY);
        runningThread.start();
    }

    public void shutdown() {
        shutdown = true;
        runningThread.interrupt();
    }
    
    @Override
    public void run() {
        DatagramPacket p = new DatagramPacket(new byte[1024], 1024);
        Message m = null;
        try {
            LOG.debug("listening for push request on " + sock.getLocalPort());
            while (true) {
                sock.receive(p);
                ByteArrayInputStream bais = new ByteArrayInputStream(p.getData());
                m = messageFactory.read(bais, Network.TCP);
                LOG.debug("received " + m.getClass() + " no file? " + noFile);
                if (noFile) {
                    if (m instanceof HeadPing)
                        handleNoFile(p.getSocketAddress(), new GUID(m.getGUID()));
                    continue;
                } else if (m instanceof HeadPing)
                    continue;
                else
                    break;
            }

            Assert.assertTrue(m instanceof PushRequest);

            LOG.debug("received a push request");

            Socket s = socketsManager.connect(new InetSocketAddress("127.0.0.1", _portC), 500);

            OutputStream os = s.getOutputStream();

            String GIV = "GIV 0:" + _g.toHexString() + "/" + _fileName + "\n\n";
            os.write(GIV.getBytes());

            os.flush();

            LOG.debug("wrote GIV");
            sentGIV = true;
            _uploader.setSocket(s);

        } catch (BadPacketException bad) {
            throw new RuntimeException(bad);
        } catch (InterruptedIOException stop){
            if (!shutdown) {
                throw new RuntimeException(stop);
            }
        } catch (IOException bad) {
            throw new RuntimeException(bad);
        } finally {
            sock.close();
        }
    }

    private void handleNoFile(SocketAddress from, GUID g) {
        HeadPing ping = new HeadPing(g, UrnHelper.SHA1, 0);
        HeadPong pong = headPongFactory.create(ping);
        Assert.assertFalse(pong.hasFile());
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pong.write(baos);
            DatagramPacket pack = new DatagramPacket(baos.toByteArray(),
                    baos.toByteArray().length, from);
            sock.send(pack);
            pings++;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LOG.debug("sent a NoFile headPong to " + from);
    }
}