package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.core.settings.DHTSettings;
import org.limewire.listener.EventListener;
import org.limewire.listener.SourcedEventMulticaster;
import org.limewire.util.I18NConvert;
import org.limewire.util.Objects;

import static com.limegroup.gnutella.Constants.MAX_FILE_SIZE;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.licenses.License;
import com.limegroup.gnutella.licenses.LicenseType;
import com.limegroup.gnutella.routing.HashFunction;
import com.limegroup.gnutella.xml.LimeXMLDocument;


/**
 * This class contains data for an individual shared file.  It also provides
 * various utility methods for checking against the encapsulated data.<p>
 */

public class FileDescImpl implements FileDesc {

	/**
	 * Constant for the index of this <tt>FileDesc</tt> instance in the 
	 * shared file data structure.
	 */
    private final int _index;

	/**
	 * The absolute path for the file.
	 */
    private final String _path;

	/**
	 * The name of the file, as returned by File.getName().
	 */
    private final String _name;

	/**
	 * The size of the file.
	 */
    private final long _size;

	/**
	 * The modification time of the file.
	 */
    private final long _modTime;

	/**
	 * Constant <tt>Set</tt> of <tt>URN</tt> instances for the file.  This
	 * is immutable.
	 */
    private volatile Set<URN> URNS; 

	/**
	 * Constant for the <tt>File</tt> instance.
	 */
	private final File FILE;

	/**
	 * The constant SHA1 <tt>URN</tt> instance.
	 */
	private final URN SHA1_URN;
	
	/**
	 * The License, if one exists, for this FileDesc.
	 */
	private License _license;
	
	/**
	 * The LimeXMLDocs associated with this FileDesc.
	 */
	private final CopyOnWriteArrayList<LimeXMLDocument> _limeXMLDocs = new CopyOnWriteArrayList<LimeXMLDocument>();

	/**
	 * The number of hits this file has recieved.
	 */
	private int _hits;	
	
	/** 
	 * The number of times this file has had attempted uploads
	 */
	private int _attemptedUploads;
	
    /**
     * The time when the last attempt was made to upload this file
     */
    private long lastAttemptedUploadTime = System.currentTimeMillis();
    
	/** 
	 * The number of times this file has had completed uploads
	 */
	private int _completedUploads;
	
	/** The number of sharelists this is shared in. */
	private final AtomicInteger shareListCount = new AtomicInteger(0);
	
	/** True if this is shared in the gnutella list. */
	private final AtomicBoolean sharedInGnutella = new AtomicBoolean(false);
	
	   /** True if this is a store file. */
    private final AtomicBoolean storeFile = new AtomicBoolean(false);
    
    private final SourcedEventMulticaster<FileDescChangeEvent, FileDesc> multicaster;
    private final RareFileDefinition rareFileDefinition;
    
    private final ConcurrentHashMap<String, Object> clientProperties =
        new ConcurrentHashMap<String, Object>(4, 0.75f, 4); // non-default initialCapacity,
                                                            // concurrencyLevel, saves
                                                            // ~1k memory / file.

                                                            // consider sizing even smaller,
                                                            // using other Map impls, or
                                                            // eliminating the use of a
                                                            // Map altogether

	    
    /**
	 * Constructs a new <tt>FileDesc</tt> instance from the specified 
	 * <tt>File</tt> class and the associated urns.
     * @param file the <tt>File</tt> instance to use for constructing the
	 *  <tt>FileDesc</tt>
     * @param urns the URNs to associate with this FileDesc
     * @param index the index in the FileManager
     */
    FileDescImpl(RareFileDefinition rareFileDefinition,
            SourcedEventMulticaster<FileDescChangeEvent, FileDesc> multicaster,
            File file,
            Set<? extends URN> urns,
            int index) {
		if(index < 0) {
			throw new IndexOutOfBoundsException("negative index (" + index + ") not permitted in FileDesc");
		}

		this.rareFileDefinition = rareFileDefinition;
		this.multicaster = multicaster;
		FILE = Objects.nonNull(file, "file");
        _index = index;
        _name = I18NConvert.instance().compose(FILE.getName());
        _path = FILE.getAbsolutePath();
        _size = FILE.length();
        assert _size >= 0 && _size <= MAX_FILE_SIZE : "invalid size "+_size+" of file "+FILE;
        _modTime = FILE.lastModified();
        URNS = Collections.unmodifiableSet(Objects.nonNull(urns, "urns"));
		SHA1_URN = UrnSet.getSha1(URNS);
		if(SHA1_URN == null)
			throw new IllegalArgumentException("no SHA1 URN");

        _hits = 0; // Starts off with 0 hits
    }
    
    @Override
    public boolean isRareFile() {
        return rareFileDefinition.isRareFile(this);
    }

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#hasUrns()
     */
	public boolean hasUrns() {
		return !URNS.isEmpty();
	}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getIndex()
     */
	public int getIndex() {
		return _index;
	}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getFileSize()
     */
	public long getFileSize() {
		return _size;
	}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getFileName()
     */
	public String getFileName() {
		return _name;
	}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#lastModified()
     */
	public long lastModified() {
		return _modTime;
	}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getTTROOTUrn()
     */
	public URN getTTROOTUrn() {
	    for(URN urn : URNS) {
	        if(urn.isTTRoot())
	            return urn;
	    }
	    
	    // this can happen.
	    return null;
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getFile()
     */
	public File getFile() {
	    return FILE;
	}
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getSHA1Urn()
     */
    public URN getSHA1Urn() {
        return SHA1_URN;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#setTTRoot(com.limegroup.gnutella.URN)
     */
    public void setTTRoot(URN ttroot) {
        boolean contained = getUrns().contains(ttroot);
        if(!contained) {
            UrnSet s = new UrnSet();
            s.add(SHA1_URN);
            s.add(ttroot);
            URNS = Collections.unmodifiableSet(s);
            if(multicaster != null) {
                multicaster.handleEvent(new FileDescChangeEvent(this, FileDescChangeEvent.Type.URNS_CHANGED, ttroot));
            }
        }
    }
    
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getUrns()
     */
	public Set<URN> getUrns() {
		return URNS;
	}   

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getPath()
     */
	public String getPath() {
		return FILE.getAbsolutePath();
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#addLimeXMLDocument(com.limegroup.gnutella.xml.LimeXMLDocument)
     */
	public void addLimeXMLDocument(LimeXMLDocument doc) {
        _limeXMLDocs.add(doc);
        
	    doc.initIdentifier(FILE);
	    if(doc.isLicenseAvailable())
	        _license = doc.getLicense();
	    
	    if(doc.getLicenseString() != null && doc.getLicenseString().equals(LicenseType.LIMEWIRE_STORE_PURCHASE.name()))
	            setStoreFile(true);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#replaceLimeXMLDocument(com.limegroup.gnutella.xml.LimeXMLDocument, com.limegroup.gnutella.xml.LimeXMLDocument)
     */
    public boolean replaceLimeXMLDocument(LimeXMLDocument oldDoc, 
                                          LimeXMLDocument newDoc) {
        synchronized(_limeXMLDocs) {
            int index = _limeXMLDocs.indexOf(oldDoc);
            if( index == -1 )
                return false;
            
            _limeXMLDocs.set(index, newDoc);
        }
        
        newDoc.initIdentifier(FILE);
        if(newDoc.isLicenseAvailable())
            _license = newDoc.getLicense();
        else if(_license != null && oldDoc.isLicenseAvailable())
            _license = null;        
        return true;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#removeLimeXMLDocument(com.limegroup.gnutella.xml.LimeXMLDocument)
     */
    public boolean removeLimeXMLDocument(LimeXMLDocument toRemove) {
        
        if (!_limeXMLDocs.remove(toRemove))
            return false;
        
        if(_license != null && toRemove.isLicenseAvailable())
            _license = null;
        
        return true;
    }   
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getLimeXMLDocuments()
     */
    public List<LimeXMLDocument> getLimeXMLDocuments() {
        return _limeXMLDocs;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getXMLDocument()
     */
    public LimeXMLDocument getXMLDocument() {
        List<LimeXMLDocument> docs = getLimeXMLDocuments();
        return docs.isEmpty() ? null : docs.get(0);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getXMLDocument(java.lang.String)
     */
    public LimeXMLDocument getXMLDocument(String schemaURI) {
        for(LimeXMLDocument doc : getLimeXMLDocuments()) {
            if (doc.getSchemaURI().equalsIgnoreCase(schemaURI))
                return doc;
        }
        return null;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#isLicensed()
     */
    public boolean isLicensed() {
        return _license != null;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getLicense()
     */
    public License getLicense() {
        return _license;
    }
	
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#containsUrn(com.limegroup.gnutella.URN)
     */
    public boolean containsUrn(URN urn) {
        return URNS.contains(urn);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#incrementHitCount()
     */    
    public int incrementHitCount() {
        return ++_hits;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getHitCount()
     */
    public int getHitCount() {
        return _hits;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#incrementAttemptedUploads()
     */    
    public synchronized int incrementAttemptedUploads() {
        lastAttemptedUploadTime = System.currentTimeMillis();
        return ++_attemptedUploads;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getAttemptedUploads()
     */
    public synchronized int getAttemptedUploads() {
        return _attemptedUploads;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getLastAttemptedUploadTime()
     */
    public synchronized long getLastAttemptedUploadTime() {
        return lastAttemptedUploadTime;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#incrementCompletedUploads()
     */    
    public int incrementCompletedUploads() {
        return ++_completedUploads;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getCompletedUploads()
     */
    public int getCompletedUploads() {
        return _completedUploads;
    }       
    
	// overrides Object.toString to provide a more useful description
	@Override
    public String toString() {
		return ("FileDesc:\r\n"+
				"name:     "+_name+"\r\n"+
				"index:    "+_index+"\r\n"+
				"path:     "+_path+"\r\n"+
				"size:     "+_size+"\r\n"+
				"modTime:  "+_modTime+"\r\n"+
				"File:     "+FILE+"\r\n"+
				"urns:     "+URNS+"\r\n"+
				"docs:     "+ _limeXMLDocs+"\r\n");
	}
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#lookup(java.lang.String)
     */
    public String lookup(String key) {
        if (key == null)
            return null;
        if ("hits".equals(key))
            return String.valueOf(getHitCount());
        else if ("ups".equals(key))
            return String.valueOf(getAttemptedUploads());
        else if ("cups".equals(key))
            return String.valueOf(getCompletedUploads());
        else if ("lastup".equals(key))
            return String.valueOf(System.currentTimeMillis() - getLastAttemptedUploadTime());
        else if ("licensed".equals(key))
            return String.valueOf(isLicensed());
        else if ("atUpSet".equals(key))
            return DHTSettings.RARE_FILE_ATTEMPTED_UPLOADS.getValueAsString();
        else if ("cUpSet".equals(key))
            return DHTSettings.RARE_FILE_COMPLETED_UPLOADS.getValueAsString();
        else if ("rftSet".equals(key))
            return DHTSettings.RARE_FILE_TIME.getValueAsString();
        else if ("hasXML".equals(key))
            return String.valueOf(getXMLDocument() != null);
        else if ("size".equals(key))
            return String.valueOf(_size);
        else if ("lastM".equals(key))
            return String.valueOf(lastModified());
        else if ("numKW".equals(key))
            return String.valueOf(HashFunction.keywords(getPath()).length);
        else if ("numKWP".equals(key))
            return String.valueOf(HashFunction.getPrefixes(HashFunction.keywords(getPath())).length);
        else if (key.startsWith("xml_") && getXMLDocument() != null) {
            key = key.substring(4,key.length());
            return getXMLDocument().lookup(key);
            
        // Note: Removed 'firewalled' check -- might not be necessary, but
        // should see if other ways to re-add can be done.
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#incrementShareListCount()
     */
    public void incrementShareListCount() {
        shareListCount.incrementAndGet();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#decrementShareListCount()
     */
    public void decrementShareListCount() {
        shareListCount.decrementAndGet();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getShareListCount()
     */
    public int getShareListCount() {
        return shareListCount.get();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#setSharedWithGnutella(boolean)
     */
    public void setSharedWithGnutella(boolean b) {
        sharedInGnutella.set(b);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#isSharedWithGnutella()
     */
    public boolean isSharedWithGnutella() {
        return sharedInGnutella.get();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#setStoreFile(boolean)
     */
    public void setStoreFile(boolean b) {
        storeFile.set(b);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#isStoreFile()
     */
    public boolean isStoreFile() {
        return storeFile.get();
    }
    
    @Override
    public void addListener(EventListener<FileDescChangeEvent> listener) {
        multicaster.addListener(this, listener);
    }
    
    @Override
    public boolean removeListener(EventListener<FileDescChangeEvent> listener) {
        return multicaster.removeListener(this, listener);
    }
    
    @Override
    public Object getClientProperty(String property) {
        return clientProperties.get(property);
    }
    
    @Override
    public void putClientProperty(String property, Object value) {
        clientProperties.put(property, value);
    }

}


