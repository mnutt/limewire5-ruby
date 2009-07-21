package org.limewire.xmpp.api.client;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.limewire.friend.api.FileMetaData;


public class MockFileMetadata implements FileMetaData {
    private String id;
    private String name;
    
    public MockFileMetadata(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public Date getCreateTime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getIndex() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Set<String> getUrns() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getSerializableMap() {
        return null;
    }
}
