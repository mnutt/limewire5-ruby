package org.limewire.core.impl.library;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.impl.URNImpl;
import org.limewire.core.impl.util.FilePropertyKeyPopulator;
import org.limewire.xmpp.api.client.FileMetaData;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.limegroup.gnutella.CategoryConverter;
import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.library.CreationTimeCache;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.IncompleteFileDesc;
import com.limegroup.gnutella.library.LocalFileDetailsFactory;
import com.limegroup.gnutella.xml.LimeXMLDocument;

class CoreLocalFileItem implements LocalFileItem , Comparable {

    private final Category category;

    private Map<FilePropertyKey, Object> propertiesMap;  // TODO this is a shallow copy of LimeXMLDocument.fieldToValue

    private final FileDesc fileDesc;

    private final LimeXMLDocument doc;

    private final LocalFileDetailsFactory detailsFactory;

    private final CreationTimeCache creationTimeCache;

    @AssistedInject
    public CoreLocalFileItem(@Assisted FileDesc fileDesc, LocalFileDetailsFactory detailsFactory,
            CreationTimeCache creationTimeCache) {
        this.fileDesc = fileDesc;
        this.doc = fileDesc.getXMLDocument();
        this.detailsFactory = detailsFactory;
        this.creationTimeCache = creationTimeCache;
        this.category = CategoryConverter.categoryForFile(fileDesc.getFile());
    }

    /**
     * Lazily builds the properties map for this local file item.
     */
    private Map<FilePropertyKey, Object> getPropertiesMap() {
        synchronized (this) {
            if(propertiesMap == null) {
                reloadProperties();
            }
            return propertiesMap;
        }
    }

    @Override
    public int getFriendShareCount() {
        return fileDesc.getShareListCount();
    }

    @Override
    public boolean isSharedWithGnutella() {
        return fileDesc.isSharedWithGnutella();
    }

    @Override
    public long getCreationTime() {
        return creationTimeCache.getCreationTimeAsLong(fileDesc.getSHA1Urn());
    }

    @Override
    public File getFile() {
        return fileDesc.getFile();
    }

    @Override
    public long getLastModifiedTime() {
        return fileDesc.lastModified();
    }

    @Override
    public String getName() {
        return fileDesc.getFileName();
    }

    @Override
    public long getSize() {
        return fileDesc.getFileSize();
    }

    @Override
    public int getNumHits() {
        return fileDesc.getHitCount();
    }

    @Override
    public int getNumUploads() {
        return fileDesc.getCompletedUploads();
    }
    
    @Override
    public int getNumUploadAttempts() {
        return getFileDesc().getAttemptedUploads();
    }  

    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        return getPropertiesMap().get(key);
    }

    @Override
    public String getPropertyString(FilePropertyKey key) {
        Object value = getProperty(key);
        if (value != null) {
            String stringValue = value.toString();
            return stringValue;
        } else {
            return null;
        }
    }

    @Override
    public FileMetaData toMetadata() {
        FileDetails details = getFileDetails();
        FileMetaDataImpl fileMetaData = new FileMetaDataImpl();
        fileMetaData.setCreateTime(new Date(details.getCreationTime()));
        fileMetaData.setDescription(""); // TODO
        fileMetaData.setId(details.getSHA1Urn().toString());
        fileMetaData.setIndex(details.getIndex());
        fileMetaData.setName(details.getFileName());
        fileMetaData.setSize(details.getSize());
        fileMetaData.setURNs(details.getUrns());
        return fileMetaData;
    }

    private static class FileMetaDataImpl implements FileMetaData {
        private enum Element {
            id, name, size, description, index, metadata, urns, createTime
        }

        private final Map<Element, String> data = new HashMap<Element, String>();

        public String getId() {
            return data.get(Element.id);
        }

        public void setId(String id) {
            data.put(Element.id, id);
        }

        public String getName() {
            return data.get(Element.name);
        }

        public void setName(String name) {
            data.put(Element.name, name);
        }

        public long getSize() {
            return Long.valueOf(data.get(Element.size));
        }

        public void setSize(long size) {
            data.put(Element.size, Long.toString(size));
        }

        public String getDescription() {
            return data.get(Element.description);
        }

        public void setDescription(String description) {
            data.put(Element.description, description);
        }

        public long getIndex() {
            return Long.valueOf(data.get(Element.index));
        }

        public void setIndex(long index) {
            data.put(Element.index, Long.toString(index));
        }

        public Map<String, String> getMetaData() {
            // TODO
            return null; // To change body of implemented methods use File |
            // Settings | File Templates.
        }

        public Set<String> getURNsAsString() {
            StringTokenizer st = new StringTokenizer(data.get(Element.urns), " ");
            Set<String> set = new HashSet<String>();
            while (st.hasMoreElements()) {
                set.add(st.nextToken());
            }
            return set;
        }

        public void setURNs(Set<URN> urns) {
            String urnsString = "";
            for (URN urn : urns) {
                urnsString += urn + " ";
            }
            data.put(Element.urns, urnsString);
        }

        public Date getCreateTime() {
            return new Date(Long.valueOf(data.get(Element.createTime)));
        }

        public void setCreateTime(Date date) {
            data.put(Element.createTime, Long.toString(date.getTime()));
        }

        public String toXML() {
            // TODO StringBuilder instead of concats
            String fileMetadata = "<file>";
            for (Element element : data.keySet()) {
                fileMetadata += "<" + element.toString() + ">";
                fileMetadata += data.get(element);
                fileMetadata += "</" + element.toString() + ">";
            }
            fileMetadata += "</file>";
            return fileMetadata;
        }
    }

    public FileDetails getFileDetails() {
        return detailsFactory.create(fileDesc);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getFile() == null) ? 0 : getFile().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        return getFile().equals(((CoreLocalFileItem) obj).getFile());
    }

    @Override
    public String toString() {
        return "CoreLocalFileItem for: " + fileDesc;
    }

    @Override
    public String getFileName() {
        return getFileDetails().getFileName();
    }

    @Override
    public boolean isShareable() {
        return !fileDesc.isStoreFile() && !isIncomplete();
    }
    
    @Override
    public org.limewire.core.api.URN getUrn() {
        URN urn = fileDesc.getSHA1Urn();
        return new URNImpl(urn);
    }

    @Override
    public boolean isIncomplete() {
        return fileDesc instanceof IncompleteFileDesc;
    }

    @Override
    public void setProperty(FilePropertyKey key, Object value) {
        getPropertiesMap().put(key, value);
    }

    public FileDesc getFileDesc() {
        return fileDesc;
    }

    @Override
    public int compareTo(Object obj) {
        if (getClass() != obj.getClass()) {
            return -1;
        }
        return getFileName().toLowerCase().compareTo(((CoreLocalFileItem) obj).getFileName().toLowerCase());
    }
    
    /**
     * Reloads the properties map to whatever values are stored in the
     * LimeXmlDocs for this file.
     */
    public void reloadProperties() {
        synchronized (this) {
            Map<FilePropertyKey, Object> reloadedMap = Collections
                    .synchronizedMap(new HashMap<FilePropertyKey, Object>());
            FilePropertyKeyPopulator.populateProperties(fileDesc.getFileName(), fileDesc.getFileSize(), 
                    getCreationTime(), reloadedMap, doc);
            propertiesMap = reloadedMap;
        }
    }
    
}
