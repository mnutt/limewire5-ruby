package com.limegroup.gnutella;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.Vector;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.net.SocketsManager.ConnectType;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.version.UpdateInformation;

/**
 * The command-line UI for the Gnutella servent.
 */
public class Main {
    public static void main(String args[]) {
        Injector injector = Guice.createInjector(Stage.PRODUCTION, new LimeWireCoreModule(MainCallback.class));
        LimeWireCore core = injector.getInstance(LimeWireCore.class);
        core.getLifecycleManager().start();        
        NetworkManager networkManager = core.getNetworkManager();
        
        System.out.println("For a command list type help.");
        BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
        for ( ; ;) {
            System.out.print("LimeRouter> ");
            try {
                String command=in.readLine();
                if (command==null)
                    break;
                else if (command.equals("help")) {
                    System.out.println("catcher                  "+
                                       "Print host catcher.");
                    System.out.println("connect <host> [<port>]  "+
                                       "Connect to a host[:port].");
                    System.out.println("help                     "+
                                       "Print this message.");
                    System.out.println("listen <port>            "+
                                       "Set the port you are listening on.");
                    //              System.out.println("push                     "+
                    //                "Print push routes.");
                    System.out.println("query <string>           "+
                                       "Send a query to the network.");
                    System.out.println("quit                     "+
                                       "Quit the application.");
                    //              System.out.println("route                    "+
                    //                "Print routing tables.");
                    //              System.out.println("stat                     "+
                    //                "Print statistics.");
                    System.out.println("update                   "+
                                       "Send pings to update the statistics.");
                }
                else if (command.equals("quit"))
                    break;
                //          //Print routing tables
                //          else if (command.equals("route"))
                //              RouterService.dumpRouteTable();
                //          //Print connections
                //          else if (command.equals("push"))
                //              RouterService.dumpPushRouteTable();
                //Print push route
            
                String[] commands=split(command);
                //Connect to remote host (establish outgoing connection)
                if (commands.length>=2 && commands[0].equals("connect")) {
                    try {
                        int port=6346;
                        if (commands.length>=3)
                            port=Integer.parseInt(commands[2]);
                        System.out.println("Connecting...");
                        core.getConnectionServices().connectToHostAsynchronously(commands[1], port, ConnectType.PLAIN);
                    } catch (NumberFormatException e) {
                        System.out.println("Please specify a valid port.");
                    }
                } else if (commands.length>=2 && commands[0].equals("query")) {
                    //Get query string from command (possibly multiple words)
                    int i=command.indexOf(' ');
                    assert(i!=-1 && i<command.length());
                    String query=command.substring(i+1);
                    SearchServices searchServices = core.getSearchServices();
                    searchServices.query(searchServices.newQueryGUID(), query);
                } else if (commands.length==2 && commands[0].equals("listen")) {
                    try {
                        int port=Integer.parseInt(commands[1]);
                        networkManager.setListeningPort(port);
                    } catch (NumberFormatException e) {
                        System.out.println("Please specify a valid port.");
                    } catch (IOException e) {
                        System.out.println("Couldn't change port.  Try another value.");
                    }
                }
            } catch (IOException e) {
                System.exit(1);
            }
        }
        System.out.println("Good bye.");
        core.getLifecycleManager().shutdown(); //write gnutella.net
    }
    
    /** Returns an array of strings containing the words of s, where
     *  a word is any sequence of characters not containing a space.
     */
    public static String[] split(String s) {
        s=s.trim();
        int n=s.length();
        if (n==0)
            return new String[0];
        Vector<String> buf=new Vector<String>();

        //s[i] is the start of the word to add to buf
        //s[j] is just past the end of the word
        for (int i=0; i<n; ) {
            assert(s.charAt(i)!=' ');
            int j=s.indexOf(' ',i+1);
            if (j==-1)
                j=n;
            buf.add(s.substring(i,j));
            //Skip past whitespace (if any) following s[j]
            for (i=j+1; j<n ; ) {
                if (s.charAt(i)!=' ')
                    break;
                i++;
            }
        }
        String[] ret=new String[buf.size()];
        for (int i=0; i<ret.length; i++)
            ret[i]= buf.get(i);
        return ret;
    }

    
    
    @Singleton
    private static class MainCallback implements ActivityCallback {

        /////////////////////////// ActivityCallback methods //////////////////////
    
        public void connectionInitializing(RoutedConnection c) {
        }
    
        public void connectionInitialized(RoutedConnection c) {
    //      String host = c.getOrigHost();
    //      int    port = c.getOrigPort();
            ;//System.out.println("Connected to "+host+":"+port+".");
        }
    
        public void connectionClosed(RoutedConnection c) {
    //      String host = c.getOrigHost();
    //      int    port = c.getOrigPort();
            //System.out.println("Connection to "+host+":"+port+" closed.");
        }
    
        public void knownHost(Endpoint e) {
            //Do nothing.
        }
    
    //     public void handleQueryReply( QueryReply qr ) {
    //      synchronized(System.out) {
    //          System.out.println("Query reply from "+qr.getIP()+":"+qr.getPort()+":");
    //          try {
    //              for (Iterator iter=qr.getResults(); iter.hasNext(); )
    //                  System.out.println("   "+((Response)iter.next()).getName());
    //          } catch (BadPacketException e) { }
    //      }
    //     }
    
        public void handleQueryResult(RemoteFileDesc rfd , QueryReply queryReply, Set<? extends IpPort> loc) {
            synchronized(System.out) {
                System.out.println("Query hit from "+rfd.getAddress() + ":");
                System.out.println("   "+rfd.getFileName());
            }
        }
    
        /**
         *  Add a query string to the monitor screen
         */
        public void handleQueryString( String query ) {
        }
    
    
        public void error(int errorCode) {
            error(errorCode, null);
        }
        
        public void error(Throwable problem, String msg) {
            problem.printStackTrace();
            System.out.println(msg);
        }
    
        /**
         * Implements ActivityCallback.
         */
        public void error(Throwable problem) {
            problem.printStackTrace();
        }
    
        public void error(int message, Throwable t) {
            System.out.println("Error: "+message);
            t.printStackTrace();
        }
    
        ///////////////////////////////////////////////////////////////////////////

    
        public void addDownload(Downloader mgr) {}
    
        public void downloadCompleted(Downloader mgr) {}
    
        public void addUpload(Uploader mgr) {}
    
        public void removeUpload(Uploader mgr) {}
    
        public boolean warnAboutSharingSensitiveDirectory(final File dir) { return false; }
        
        public void handleSharedFileUpdate(File file) {}
    
        public void downloadsComplete() {}    
        
        public void uploadsComplete() {}
    
        public void promptAboutCorruptDownload(Downloader dloader) {
            dloader.discardCorruptDownload(false);
        }
    
        public void restoreApplication() {}
    
        public void showDownloads() {}
    
        public String getHostValue(String key){
            return null;
        }
        public void browseHostFailed(GUID guid) {}
        
        public void updateAvailable(UpdateInformation update) {
            if (update.getUpdateCommand() != null)
                System.out.println("there's a new version out "+update.getUpdateVersion()+
                        ", to get it shutdown limewire and run "+update.getUpdateCommand());
            else
                System.out.println("You're running an older version.  Get " +
                             update.getUpdateVersion() + ", from " + update.getUpdateURL());
        }  
    
        public boolean isQueryAlive(GUID guid) {
            return false;
        }
        
        public void componentLoading(String state, String component) {
            System.out.println("Loading component: " + component);
        }
        
        public void handleMagnets(final MagnetOptions[] magnets) {
        }
    
    	public void handleTorrent(File torrentFile){}

        public void handleAddressStateChanged() {
        }
        
        public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
        }
        public void installationCorrupted() {
            
        }
        public void handleDAAPConnectionError(Throwable t) {  }
        public String translate(String s) { return s;}

        @Override
        public void handleSaveLocationException(DownloadAction downLoadAction,
                SaveLocationException sle, boolean supportsNewSaveDir) {
            
        }

        @Override
        public void promptTorrentUploadCancel(ManagedTorrent torrent) {
            
        }
    }
}
