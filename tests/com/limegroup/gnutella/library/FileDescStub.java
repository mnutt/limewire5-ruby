package com.limegroup.gnutella.library;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.licenses.License;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class FileDescStub implements FileDesc {
    public static final String DEFAULT_URN =
        "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";
    public static final URN DEFAULT_SHA1;
    public static final Set<URN> DEFAULT_SET;
    public static final int DEFAULT_SIZE = 1126400;
    
    static {
        DEFAULT_SET = new HashSet<URN>();
        URN sha1 = null;
        try {
            sha1 = URN.createSHA1Urn(DEFAULT_URN);
        } catch(IOException ioe) {
            throw new RuntimeException(ioe);
        }
        DEFAULT_SHA1 = sha1;
        DEFAULT_SET.add(DEFAULT_SHA1);
    }
    
    public FileDescStub() {
        this("abc.txt");
    }
    
    public FileDescStub(String name) {
        this(name, DEFAULT_SHA1, 0);
    }
    
    private final File file;
    private final Set<URN> urns;
    private final int index;
    private final String name;
    private final String path;
    private final long modified;
    private final long size;
    
    
    public FileDescStub(String name, URN urn, int index) {
        this.file = createStubFile(new File(name));
        this.urns = new UrnSet(urn);
        this.index = index;
        this.name = name;
        this.path = file.getAbsolutePath();
        this.modified = file.lastModified();
        this.size = file.length();
    }

    protected static File createStubFile(File file) {
        if (!file.exists()) {
            try {
                OutputStream out = new BufferedOutputStream(
                        new FileOutputStream(file));
                file.deleteOnExit();
                try {
                    int length = DEFAULT_SIZE;
                    for (int i = 0; i < length; i++) {
                        out.write('a');
                    }
                } finally {
                    out.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return file;
    }

    @Override
    public long getFileSize() {
        return size;
    }

    @Override
    public void addLimeXMLDocument(LimeXMLDocument doc) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean containsUrn(URN urn) {
        return urns.contains(urn);
    }

    @Override
    public void decrementShareListCount() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getAttemptedUploads() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getCompletedUploads() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public String getFileName() {
        return name;
    }

    @Override
    public int getHitCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public long getLastAttemptedUploadTime() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public License getLicense() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<LimeXMLDocument> getLimeXMLDocuments() {
        return Collections.emptyList();
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public URN getSHA1Urn() {
        return UrnSet.getSha1(urns);
    }

    @Override
    public int getShareListCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public URN getTTROOTUrn() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<URN> getUrns() {
        return urns;
    }

    @Override
    public LimeXMLDocument getXMLDocument() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LimeXMLDocument getXMLDocument(String schemaURI) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int incrementAttemptedUploads() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int incrementCompletedUploads() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int incrementHitCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void incrementShareListCount() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isLicensed() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isRareFile() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSharedWithGnutella() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isStoreFile() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long lastModified() {
        return modified;
    }

    @Override
    public boolean removeLimeXMLDocument(LimeXMLDocument toRemove) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean replaceLimeXMLDocument(LimeXMLDocument oldDoc, LimeXMLDocument newDoc) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setSharedWithGnutella(boolean b) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setStoreFile(boolean b) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean setTTRoot(URN ttroot) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String lookup(String key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addListener(EventListener<FileDescChangeEvent> listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean removeListener(EventListener<FileDescChangeEvent> listener) {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public Object getClientProperty(String property) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void putClientProperty(String property, Object value) {
        // TODO Auto-generated method stub
        
    }
}
