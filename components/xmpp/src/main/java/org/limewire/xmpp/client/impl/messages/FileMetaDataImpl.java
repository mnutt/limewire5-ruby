package org.limewire.xmpp.client.impl.messages;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.limewire.util.StringUtils;
import org.limewire.xmpp.api.client.FileMetaData;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.google.inject.internal.base.Objects;

public class FileMetaDataImpl implements FileMetaData {

    /**
     * Keep casing of enum names since they are being sent over the wire
     * and xml is case sensitive.
     */
    public static enum Element {
        id, name, size, description, index, metadata, urns, createTime
    }

    private static final Element[] MANDATORY_FIELDS = new Element[] {
        Element.index, Element.name, Element.size, Element.createTime, Element.urns
    };
    
    private final Map<String, String> data = new HashMap<String, String>();

    public FileMetaDataImpl(XmlPullParser parser) throws XmlPullParserException, IOException, InvalidIQException {
        parser.nextTag();
        do {            
            int eventType = parser.getEventType();
            if(eventType == XmlPullParser.START_TAG) {
                data.put(parser.getName(), parser.nextText());
            } else if(eventType == XmlPullParser.END_TAG) {
                if(parser.getName().equals("file")) {
                    break;
                }
            }
        } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
        if (!isValid()) {
            throw new InvalidIQException("is missing mandatory fields: " + this);
        }
    }
    
    /**
     * 
     * @throws IllegalArgumentException if data is not complete
     */
    public FileMetaDataImpl(FileMetaData metaData) {
        setCreateTime(metaData.getCreateTime());
        setDescription(metaData.getDescription());
        setId(metaData.getId());
        setIndex(metaData.getIndex());
        setName(metaData.getName());
        setSize(metaData.getSize());
        setURNs(metaData.getUrns());
        if (!isValid()) {
            throw new IllegalArgumentException("is missing mandatory fields: " + this);
        }
    }
    
    /**
     * 
     * @throws IllegalArgumentException if data is not complete
     */
    public FileMetaDataImpl(Map<Element, String> data) {
        for (Entry<Element, String> entry : data.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
        if (!isValid()) {
            throw new IllegalArgumentException("is missing mandatory fields: " + this);
        }
    }
    
    private boolean isValid() {
        for (Element element : MANDATORY_FIELDS) {
            if (get(element) == null) {
                return false;
            }
        }
        try {
            getSize();
            getIndex();
            getCreateTime();
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
    
    private void put(Element element, String value) {
        data.put(element.toString(), value);
    }
    
    private String get(Element element) {
        return data.get(element.toString());
    }

    public String getId() {
        return get(Element.id);
    }
    
    public void setId(String id) {
        put(Element.id, id);
    }

    public String getName() {
        return get(Element.name);
    }
    
    public void setName(String name) {
        put(Element.name, Objects.nonNull(name, "name"));
    }

    public long getSize() {
        return Long.valueOf(get(Element.size));
    }                                                     
    
    public void setSize(long size) {
        put(Element.size, Long.toString(size));
    }

    public String getDescription() {
        return get(Element.description);
    }
    
    public void setDescription(String description) {
        put(Element.description, description);
    }

    public long getIndex() {
        return Long.valueOf(get(Element.index));
    }
    
    public void setIndex(long index) {
        put(Element.index, Long.toString(index));
    }

    public Set<String> getUrns() {
        StringTokenizer st = new StringTokenizer(get(Element.urns), " ");
        Set<String> set = new HashSet<String>();
        while(st.hasMoreElements()) {
            set.add(st.nextToken());
        }
        return set;
    }

    public void setURNs(Set<String> urns) {
        put(Element.urns, StringUtils.explode(urns, " "));
    }

    public Date getCreateTime() {
        return new Date(Long.valueOf(get(Element.createTime)));
    }
    
    public void setCreateTime(Date date) {
        put(Element.createTime, Long.toString(date.getTime()));
    }
    
    public String toXML() {
        StringBuilder fileMetadata = new StringBuilder("<file>");
        for(Entry<String, String> entry : data.entrySet()) { 
            fileMetadata.append("<").append(entry.getKey()).append(">");
            fileMetadata.append(org.jivesoftware.smack.util.StringUtils.escapeForXML(entry.getValue()));
            fileMetadata.append("</").append(entry.getKey()).append(">");
        }
        fileMetadata.append("</file>");
        return fileMetadata.toString();
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this, data);
    }
}
