package org.limewire.core.impl.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.URNImpl;
import org.limewire.core.impl.friend.GnutellaPresence;
import org.limewire.core.impl.util.FilePropertyKeyPopulator;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.CategoryConverter;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A class to generate a compatible {@link SearchResult} for the ui using an anonymous or non 
 *  anonymous {@link FriendPresence} a {@link RemoteFileDesc} and a list of alternate locations (AltLocs).
 */
public class RemoteFileDescAdapter implements SearchResult {
   
    /**
     * AltLocs are given a weight of one since we do not really know anything about them but
     *  they will improve the quality of the result if they work or are not spam.
     */
    private static int ALTLOC_FACTOR = 1;
    
    private static int BROWSABLE_ANONYMOUS_PEER_FACTOR = 6;
    private static int NON_BROWSABLE_ANONYMOUS_PEER_FACTOR = 1;

    /**
     * Non anonymous sources, XMPP friends, are considered very important. 
     */
    private static int FRIENDLY_PEER_FACTOR = 20;
    
    private final FriendPresence friendPresence;
    private final RemoteFileDesc rfd;
    private final List<IpPort> locs;
    private final Map<FilePropertyKey, Object> properties;    
    private final String extension;
    private final String fileName;
    
    /**
     * Cached lists of sources from {@link #getSources()}
     */
    private List<RemoteHost> remoteHosts;
    
    /**
     * The cached relevance value from {@link #getRelevance()}, -1 is unset
     */
    private int relevance = -1;

    /**
     * Constructs {@link RemoteFileDescAdapter} with an anonymous Gnutella presence based on the rfd's
     *  address and a set of altlocs. 
     */
    public RemoteFileDescAdapter(RemoteFileDesc rfd,
                                 Set<? extends IpPort> locs) {
        this(rfd, locs, new GnutellaPresence(rfd.getAddress(), GUID.toHexString(rfd.getClientGUID())));
    }
    
    /**
     * Constructs {@link RemoteFileDescAdapter} with a specific and possibly non anonymous presence
     *  and a set of altlocs. 
     */
    public RemoteFileDescAdapter(RemoteFileDesc rfd,
            Set<? extends IpPort> locs,
            FriendPresence friendPresence) {    
        this.rfd = rfd;
        this.locs = new ArrayList<IpPort>(locs);
        this.friendPresence = friendPresence;
        properties = new HashMap<FilePropertyKey, Object>();
        fileName = rfd.getFileName();
        extension = FileUtils.getFileExtension(rfd.getFileName());
        
        LimeXMLDocument doc = rfd.getXMLDocument();
        long fileSize = rfd.getSize();
        FilePropertyKeyPopulator.populateProperties(fileName, fileSize, rfd.getCreationTime(), properties, doc);
    }

    /**
     * Calculates a rough "relevance", which is a measure of the quality of the sources.  
     *  Non anonymous sources with active capabilities (ie. browseable) are given greatest weight.
     */
    @Override
    public int getRelevance() {
        // If the value has already been calculated take that one, since it can not change
        //  during the lifecycle of this object
        if (relevance != -1) {
            return relevance;
        }
        
        relevance = 0;

        // Calculate the relevance based on the (truncated) sources list
        for(RemoteHost remoteHost : getSources()) {
	        if (remoteHost instanceof RelevantRemoteHost) {
                relevance += ((RelevantRemoteHost) remoteHost).getRelevance();
            }
        }
       
        return relevance;
    }

    /**
     * @returns the complete list of AltLocs.
     */
    public List<IpPort> getAlts() {
        return locs;
    }

    /**
     * @return the full filename of the rfd.
     */
    @Override
    public String getFileName() {
       return fileName;
    }
    
    /**
     * @return the extension for the sourced rfd.
     */
    @Override
    public String getFileExtension() {
        return extension;
    }

    /**
     * @return if the file has licence info in its xml doc.
     */
    @Override
    public boolean isLicensed() {
        LimeXMLDocument doc = rfd.getXMLDocument();
        return (doc != null) && (doc.getLicenseString() != null);
    }
        
    
    /**
     * @return the property map associated with the rfd used to generate this adaptor.
     */
    @Override
    public Map<FilePropertyKey, Object> getProperties() {
        return properties;
    }

    /**
     * @return the specified property using the file key provided. 
     */
    @Override
    public Object getProperty(FilePropertyKey key) {
        return getProperties().get(key);
    }

    /**
     * @return the category the rfd filetype falls into.
     */
    @Override
    public Category getCategory() {
        return CategoryConverter.categoryForExtension(extension);
    }

    /**
     * @return the rfd used to generate this adapter.
     */
    public RemoteFileDesc getRfd() {
        return rfd;
    }

    /**
     * @return the {@link FriendPresence} used to generate this adaptor.  
     */
    public FriendPresence getFriendPresence() {
        return friendPresence;
    }
    
    /**
     * @return the file size of the rfd.
     */
    @Override
    public long getSize() {
        return rfd.getSize();
    }

    /**
     * @return whether the rfd has been marked as spam or not.
     */
    @Override
    public boolean isSpam() {
        return rfd.isSpam();
    }
    
    /**
     * @return the GUI relevant sources.  Includes friends plus at most two anonymous sources. 
     */
    @Override
    public List<RemoteHost> getSources() {
        
        // Check that the list has not already been retrieved
        if (remoteHosts != null) {
            return remoteHosts;
        }
        
        // Initialise a new list
        remoteHosts = new ArrayList<RemoteHost>();
        
        // TODO: setting?
        int maxAltSourcesToAdd = 1;
        
        // Add the RfdRemoteHost for the FriendPresence
        remoteHosts.add(new RfdRemoteHost());
        
        // Add a specific number of the altlocs
        for( int i=0 ; i < maxAltSourcesToAdd && i<locs.size() ; i++ ) {
            remoteHosts.add(new AltLocRemoteHost(locs.get(i)));
        }
    
        return remoteHosts;
    }
   
    /**
     * @return the {@link URNImpl} for the rfd.
     */
    @Override
    public URN getUrn() {
        com.limegroup.gnutella.URN urn = rfd.getSHA1Urn();
        return urn == null ? null : new URNImpl(urn);
    }

    /**
     * @return the a magnet link URL string for the rfd.
     */
    @Override
    public String getMagnetURL() {
        MagnetOptions magnet = MagnetOptions.createMagnet(rfd, null, rfd.getClientGUID());
        return magnet.toExternalForm();
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
    
    /**
     * An adapter that creates a compatible {@link RemoteHost} from the {@link RemoteFileDesc} and anonymous
     *  or non anonymous {@link FriendPresence} that the main {@link RemoteFileDescAdapter} was constructed with.
     */
    class RfdRemoteHost implements RelevantRemoteHost {
        @Override
        public boolean isBrowseHostEnabled() {
            return rfd.isBrowseHostEnabled();
        }

        @Override
        public boolean isChatEnabled() {
            if (!friendPresence.getFriend().isAnonymous()) {
                return true;
            }
            return false;
        }

        @Override
        public boolean isSharingEnabled() {
            if (!friendPresence.getFriend().isAnonymous()) {
                return true;
            }

            return false;
        }

        @Override
        public FriendPresence getFriendPresence() {
            return friendPresence;
        }

        /**
         * @return the relevance for a primary host calculated from its
         *          capabilities.
         */
        @Override
        public int getRelevance() {
            if(friendPresence.getFriend().isAnonymous()) {
                if (rfd.isBrowseHostEnabled()) {
                    return BROWSABLE_ANONYMOUS_PEER_FACTOR;
                }
                return NON_BROWSABLE_ANONYMOUS_PEER_FACTOR;
            } 
            return FRIENDLY_PEER_FACTOR;
        }
    }
    
    /**
     * An adapter class for an AltLoc based on {@link IpPort} and translated to a {@link RemoteHost}.
     */
    static class AltLocRemoteHost implements RelevantRemoteHost {
        private final FriendPresence presence;        

        AltLocRemoteHost(IpPort ipPort) {
            if(ipPort instanceof Connectable) {
                this.presence = new GnutellaPresence((Connectable)ipPort, ipPort.getInetSocketAddress().toString());
            } else {
                this.presence = new GnutellaPresence(new ConnectableImpl(ipPort, false), ipPort.getInetSocketAddress().toString());
            }
        }

        /**
         * Indicates that a browse host is possible, however, in this case, it actually
         *  may not be 100% of the time.  Returning true allows a browse host attempts
         *  to be started.
         */
        @Override
        public boolean isBrowseHostEnabled() {
            return true;
        }

        /**
         * Chat is unsupported for Gnutella/anonymous sources so it will
         *  never be supported in an altloc.
         */
        @Override
        public boolean isChatEnabled() {
            return false;
        }

        /**
         * Share is unsupported for Gnutella/anonymous sources so it will
         *  never be supported in an AltLoc.
         */
        @Override
        public boolean isSharingEnabled() {
            return false;
        }

        /**
         * @return the anonymous {@link FriendPresence} associated with this altloc.
         */
        @Override
        public FriendPresence getFriendPresence() {
            return presence;
        }

        @Override
        public int getRelevance() {
            return ALTLOC_FACTOR;
        }
    }

    /**
     * Defines a relevance calculation unique to a specific {@link RemoteHost} type.
     */
    protected static interface RelevantRemoteHost extends RemoteHost {
        public int getRelevance();
    }

}