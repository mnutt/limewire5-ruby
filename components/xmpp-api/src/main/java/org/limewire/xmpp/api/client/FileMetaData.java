package org.limewire.xmpp.api.client;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * The file meta-data necessary to do a file exchange
 */
public interface FileMetaData {

    public String getId();
    public String getName();
    public long getSize();
    public String getDescription();
    public long getIndex();
    public Map<String, String> getMetaData();
    public Set<String> getURNsAsString();
    public Date getCreateTime();
    public String toXML();
}
