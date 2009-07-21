package org.limewire.core.impl.search;

import java.util.AbstractList;
import java.util.List;
import java.util.Set;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.friend.GnutellaPresence;
import org.limewire.core.impl.util.FilePropertyKeyPopulator;
import org.limewire.core.settings.SearchSettings;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.LimewireFeature;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

import com.google.common.collect.ImmutableList;
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
    private final String extension;
    private final List<IpPort> locs;
    private final Category category;
    private final int quality;
    
    /** The cached relevance value from {@link #getRelevance()}, -1 is unset */
    private int relevance = -1;

    /**
     * Constructs {@link RemoteFileDescAdapter} with an anonymous Gnutella presence based on the rfd's
     *  address and a set of altlocs. 
     */
    public RemoteFileDescAdapter(RemoteFileDesc rfd,
                                 Set<? extends IpPort> locs) {
        this(rfd, locs, new GnutellaPresence.GnutellaPresenceWithGuid(rfd.getAddress(), rfd.getClientGUID()));
    }
    
    /**
     * Constructs {@link RemoteFileDescAdapter} with a specific and possibly non anonymous presence
     *  and a set of altlocs. 
     */
    public RemoteFileDescAdapter(RemoteFileDesc rfd,
            Set<? extends IpPort> locs,
            FriendPresence friendPresence) {    
        this.rfd = rfd;
        this.locs = ImmutableList.copyOf(locs);
        this.friendPresence = friendPresence;
        this.extension = FileUtils.getFileExtension(rfd.getFileName());
        this.category = CategoryConverter.categoryForExtension(extension);
        this.quality = FilePropertyKeyPopulator.calculateQuality(category, extension, rfd.getSize(), rfd.getXMLDocument());
    }
    
    /** A copy constructor for a RemoteFileDescAdapter, except it changes the presence. */
    public RemoteFileDescAdapter(RemoteFileDescAdapter copy, FriendPresence presence) {
        this.rfd = copy.rfd;
        this.locs = copy.locs;
        this.friendPresence = presence;
        this.extension = copy.extension;
        this.category = copy.category;
        this.quality = copy.quality;
        
        // and other items too, if they were constructed..
        this.relevance = copy.relevance;
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
       return rfd.getFileName();
    }
    
    /**
     * @return the extension for the sourced rfd.
     */
    @Override
    public String getFileExtension() {
        return extension;
    }
    
    @Override
    public String getFileNameWithoutExtension() {
        return FileUtils.getFilenameNoExtension(rfd.getFileName());
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
     * @return the specified property using the file key provided. 
     */
    @Override
    public Object getProperty(FilePropertyKey property) {
        switch(property) {
        case NAME: return getFileNameWithoutExtension();
        case DATE_CREATED: return rfd.getCreationTime() == -1 ? null : rfd.getCreationTime();
        case FILE_SIZE: return rfd.getSize();      
        case QUALITY: return quality == -1 ? null : Long.valueOf(quality);
        default: return FilePropertyKeyPopulator.get(category, property, rfd.getXMLDocument());
        }
    }

    /**
     * @return the category the rfd filetype falls into.
     */
    @Override
    public Category getCategory() {
        return category;
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
     * @return the sources that should be shown in the UI. Includes a limited
     * number of alt-locs. 
     */
    @Override
    public List<RemoteHost> getSources() {
        // This returns a pass-through list for memory optimizations.
        // Do not cache this or the contents of this without doing
        // memory profiling and ensuring things are OK.
        return new AbstractList<RemoteHost>() {
            
          @Override public void clear() {
              throw new UnsupportedOperationException();
          }
          
          @Override public RemoteHost get(int index) {
              if(index == 0) {
                  return new RfdRemoteHost(friendPresence, rfd);
              } else if(index - 1 < SearchSettings.ALT_LOCS_TO_DISPLAY.getValue()) {
                  return new AltLocRemoteHost(locs.get(index-1));
              } else {
                  throw new IndexOutOfBoundsException("index: " + index + ", size: " + size());
              }
          }
          @Override public boolean isEmpty() {
            return false;
          }
          
          @Override public RemoteHost remove(int index) {
              throw new UnsupportedOperationException();
          }
          @Override public int size() {
              return Math.min(SearchSettings.ALT_LOCS_TO_DISPLAY.getValue(), locs.size()) + 1;
          }
        };
    }
   
    /**
     * @return the {@link URNImpl} for the rfd.
     */
    @Override
    public URN getUrn() {
        return rfd.getSHA1Urn();
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
    static class RfdRemoteHost implements RelevantRemoteHost {
        private final FriendPresence friendPresence;
        private final boolean browseHostEnabled;
        
        public RfdRemoteHost(FriendPresence presence, RemoteFileDesc rfd) {
            this.friendPresence = presence;
            this.browseHostEnabled = rfd.isBrowseHostEnabled();
        }
        
        @Override
        public boolean isBrowseHostEnabled() {
            if(friendPresence.getFriend().isAnonymous()) {
                return browseHostEnabled;
            } else {
                //ensure friend/user still logged in through LW
                return friendPresence.hasFeatures(LimewireFeature.ID);
            }
        }

        @Override
        public boolean isChatEnabled() {
            if(friendPresence.getFriend().isAnonymous()) {
                return false;
            } else {
                //ensure friend/user still logged in through LW
                return friendPresence.hasFeatures(LimewireFeature.ID);
            }
        }

        @Override
        public boolean isSharingEnabled() {
            if(friendPresence.getFriend().isAnonymous()) {
                return false;
            } else {
                //ensure friend/user still logged in through LW
                return friendPresence.hasFeatures(LimewireFeature.ID);
            }
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
                if (browseHostEnabled) {
                    return BROWSABLE_ANONYMOUS_PEER_FACTOR;
                } else {
                    return NON_BROWSABLE_ANONYMOUS_PEER_FACTOR;
                }
            } else {
                return FRIENDLY_PEER_FACTOR;
            }
        }
        
        @Override
        public String toString() {
            return "RFD Host for: " + friendPresence;
        }
    }
    
    /**
     * An adapter class for an AltLoc based on {@link IpPort} and translated to a {@link RemoteHost}.
     */
    static class AltLocRemoteHost implements RelevantRemoteHost {
        private final FriendPresence presence;        

        AltLocRemoteHost(IpPort ipPort) {
            if(ipPort instanceof Connectable) {
                this.presence = new GnutellaPresence.GnutellaPresenceWithConnectable((Connectable)ipPort);
            } else {
                this.presence = new GnutellaPresence.GnutellaPresenceWithConnectable(new ConnectableImpl(ipPort, false));
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
        
        @Override
        public String toString() {
            return "AltLoc Host For: " + presence;
        }
    }

    /**
     * Defines a relevance calculation unique to a specific {@link RemoteHost} type.
     */
    protected static interface RelevantRemoteHost extends RemoteHost {
        public int getRelevance();
    }

}