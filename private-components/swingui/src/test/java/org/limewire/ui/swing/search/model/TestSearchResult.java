/**
 * 
 */
package org.limewire.ui.swing.search.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.search.SearchResult;
import org.limewire.ui.swing.friends.MockFriend;
import org.limewire.ui.swing.friends.MockFriendPresence;
import org.limewire.util.FileUtils;

public class TestSearchResult implements SearchResult {

    private String urn;

    private String fileName;

    private Map<FilePropertyKey, Object> properties;

    public TestSearchResult(String urn, String fileName) {
        this.urn = urn;
        Map<FilePropertyKey, Object> properties = new HashMap<FilePropertyKey, Object>();
        properties.put(FilePropertyKey.NAME, FileUtils.getFilenameNoExtension(fileName));
        this.properties = properties;
        this.fileName = fileName;
    }

    public TestSearchResult(String urn, String fileName, Map<FilePropertyKey, Object> properties) {
        this.urn = urn;
        this.properties = properties;
        this.fileName = fileName;
    }

    @Override
    public String getFileExtension() {
        return FileUtils.getFileExtension(getFileName());
    }

    @Override
    public Map<FilePropertyKey, Object> getProperties() {
        return properties;
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        return properties.get(key);
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public List<RemoteHost> getSources() {
        List<RemoteHost> sources = new ArrayList<RemoteHost>();
        sources.add(new RemoteHost() {
            UUID randomUUID = UUID.randomUUID();

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
                return new MockFriendPresence(new MockFriend(randomUUID.toString()));
            }
        });
        return sources;
    }

    @Override
    public URN getUrn() {
        return new TestURN(urn);
    }

    public String toString() {
        return getUrn() + " - " + getProperty(FilePropertyKey.NAME);
    }

    @Override
    public Category getCategory() {
        return Category.AUDIO;
    }

    @Override
    public boolean isSpam() {
        return false;
    }

    @Override
    public String getFileName() {
        return fileName;
    }
    
    @Override
    public String getMagnetURL() {
        return null;
    }



    public int getRelevance() {
        return 0;
    }

    public boolean isLicensed() {
        return false;
    }


    private class TestURN implements URN {
        private String urn;

        public TestURN(String urn) {
            this.urn = urn;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TestURN) {
                TestURN oUrn = (TestURN) obj;
                return urn.equals(oUrn);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return urn.hashCode();
        }

        @Override
        public int compareTo(URN o) {
            if (o instanceof TestURN) {
                TestURN testURN = (TestURN) o;
                return urn.compareTo(testURN.urn);
            }
            return -1;
        }

        @Override
        public String toString() {
            return urn;
        }
    }
}