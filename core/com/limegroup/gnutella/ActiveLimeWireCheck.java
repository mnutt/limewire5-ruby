package com.limegroup.gnutella;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.limewire.core.settings.NetworkSettings;
import org.limewire.io.ByteReader;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.util.CommonUtils;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.browser.ExternalControl;

/**
 * Allows one to check if a LimeWire is already running under this user's name.
 */
public class ActiveLimeWireCheck {

    private final String arg;

    private final boolean allowMultiple;
    
    public ActiveLimeWireCheck(String[] args, boolean allowMultiple) {
        this.allowMultiple = allowMultiple;
        if(args == null || args.length == 0) {
            this.arg = null;
        } else {
            this.arg = ExternalControl.preprocessArgs(args);
        }
    }
    
    /**
     * Returns true if an active instance of LimeWire is running.
     */
    public boolean checkForActiveLimeWire() {
        if(arg == null) {
            return !allowMultiple && testForLimeWire(null);
        } else {
            // Only pass through args on windows/linux.
            return (OSUtils.isWindows() || OSUtils.isLinux()) && testForLimeWire(arg); 
        }
    }

    /**  Check if the client is already running, and if so, pop it up.
     *   Sends the MAGNET message along the given socket. 
     *   @returns  true if a local LimeWire responded with a true.
     */
    private boolean testForLimeWire(String arg) {
        Socket socket = null;
        int port = NetworkSettings.PORT.getValue();
        // Check to see if the port is valid.
        // If it is not, revert it to the default value.
        // This has the side effect of possibly allowing two 
        // LimeWires to start if somehow the existing one
        // set its port to 0, but that should not happen
        // in normal program flow.
        String type = ExternalControl.isTorrentRequest(arg) ? "TORRENT" : "MAGNET";
        if( !NetworkUtils.isValidPort(port) ) {
            NetworkSettings.PORT.revertToDefault();
            port = NetworkSettings.PORT.getValue();
        }   
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1", port), 1000);
            InputStream istream = socket.getInputStream(); 
            socket.setSoTimeout(1000); 
            ByteReader byteReader = new ByteReader(istream);
            OutputStream os = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os);
            BufferedWriter out = new BufferedWriter(osw);
            out.write(type+" "+arg+" ");
            out.write("\r\n");
            out.flush();
            String str = byteReader.readLine();
            return(str != null && str.startsWith(CommonUtils.getUserName()));
        } catch (IOException ignored) {
        } finally {
            IOUtils.close(socket);
        }
        return false;
    }
}
