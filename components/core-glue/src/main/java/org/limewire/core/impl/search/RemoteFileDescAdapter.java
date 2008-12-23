package org.limewire.core.impl.search;

import java.util.AbstractList;
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

public class RemoteFileDescAdapter implements SearchResult {

    private final FriendPresence friendPresence;
    private final RemoteFileDesc rfd;
    private final List<IpPort> locs;
    private final Map<FilePropertyKey, Object> properties;    
    private final Category category;    
    private final String extension;
    private final String fileName;

    public RemoteFileDescAdapter(RemoteFileDesc rfd,
                                 Set<? extends IpPort> locs) {
        this(rfd, locs, new GnutellaPresence(rfd.getAddress(), GUID.toHexString(rfd.getClientGUID())));
    }
    
    public RemoteFileDescAdapter(RemoteFileDesc rfd,
            Set<? extends IpPort> locs,
            FriendPresence friendPresence) {    
        this.rfd = rfd;
        this.locs = new ArrayList<IpPort>(locs);
        this.friendPresence = friendPresence;
        this.properties = new HashMap<FilePropertyKey, Object>();
        fileName = rfd.getFileName();
        extension = FileUtils.getFileExtension(rfd.getFileName());
        category = CategoryConverter.categoryForExtension(extension);

        LimeXMLDocument doc = rfd.getXMLDocument();
        long fileSize = rfd.getSize();
        FilePropertyKeyPopulator.populateProperties(fileName, fileSize, rfd.getCreationTime(), properties, doc);
    }

    /**
     * TODO come up with a better algorithm, right now just using number of sources and friends.
     * Friends are given a greater weight.
     */
    public int getRelevance() {
        int relevance = 0;
        for(RemoteHost remoteHost : getSources()) {
            if(remoteHost.getFriendPresence().getFriend().isAnonymous()) {
                if(remoteHost.isBrowseHostEnabled()) {
                    relevance += 5;
                }
                
                if(remoteHost.isChatEnabled()) {
                    relevance += 2;
                }
                
                if(remoteHost.isSharingEnabled()) {
                    relevance += 10;
                }
                
                relevance++;//TODO might want to drop this line, spammers have lots of sources with nothing enabled.
            } else {
                relevance += 20;
            }
        }
        return relevance;
    }

    public List<IpPort> getAlts() {
        return locs;
    }

    @Override
    public String getFileExtension() {
        return extension;
    }

    @Override
    public boolean isLicensed() {
        LimeXMLDocument doc = rfd.getXMLDocument();
        return (doc != null) && (doc.getLicenseString() != null);
    }
    
    

    @Override
    public Map<FilePropertyKey, Object> getProperties() {
        return properties;
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        return getProperties().get(key);
    }

    @Override
    public Category getCategory() {
        return category;
    }

    public RemoteFileDesc getRfd() {
        return rfd;
    }

    @Override
    public long getSize() {
        return rfd.getSize();
    }
    
    @Override
    public List<RemoteHost> getSources() {
        List<RemoteHost> remoteHosts = new ArrayList<RemoteHost>();
        int maxToAdd = 2;
        int numAdded = 0;
        
        for(RemoteHost remoteHost : buildSources()) {
            boolean anonymous = remoteHost.getFriendPresence().getFriend().isAnonymous();
            if(!anonymous) {
                remoteHosts.add(remoteHost);
            } else if(numAdded < maxToAdd) {
                remoteHosts.add(remoteHost);
                numAdded++;
            }
        }
        return remoteHosts;
    }
    
    private List<RemoteHost> buildSources() {
        return new AbstractList<RemoteHost>() {
            @Override
            public RemoteHost get(final int index) {
                if (index == 0) {
                    return new RfdRemoteHost();
                } else {
                    return new AltLocRemoteHost(index);
                }
            }

            @Override
            public int size() {
                return 1 + locs.size();
            }
        };
    }

    @Override
    public URN getUrn() {
        com.limegroup.gnutella.URN urn = rfd.getSHA1Urn();
        return urn == null ? null : new URNImpl(urn);
    }

    @Override
    public boolean isSpam() {
        return rfd.isSpam();
    }

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

    public FriendPresence getFriendPresence() {
        return friendPresence;
    }
    
    private final class RfdRemoteHost implements RemoteHost {
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

        public FriendPresence getFriendPresence() {
            return friendPresence;
        }
    }
    
    
    private final class AltLocRemoteHost implements RemoteHost {
        private final FriendPresence presence;        

        private AltLocRemoteHost(int index) {
            IpPort ipPort = locs.get(index - 1);
            if(ipPort instanceof Connectable) {
                this.presence = new GnutellaPresence((Connectable)ipPort, ipPort.getInetSocketAddress().toString());
            } else {
                this.presence = new GnutellaPresence(new ConnectableImpl(ipPort, false), ipPort.getInetSocketAddress().toString());
            }
        }

        @Override
        public boolean isBrowseHostEnabled() {
            return false;
        }

        @Override
        public boolean isChatEnabled() {
            return false;
        }

        @Override
        public boolean isSharingEnabled() {
            return false;
        }

        @Override
        public FriendPresence getFriendPresence() {
            return presence;
        }
    }

    @Override
    public String getFileName() {
       return fileName;
    }

    @Override
    public String getMagnetURL() {
        MagnetOptions magnet = MagnetOptions.createMagnet(rfd, null, rfd.getClientGUID());
        return magnet.toExternalForm();
    }

}