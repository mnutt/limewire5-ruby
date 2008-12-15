package com.limegroup.gnutella.xml;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.limewire.i18n.I18nMarker;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileDescChangeEvent;
import com.limegroup.gnutella.library.FileListChangedEvent;
import com.limegroup.gnutella.library.ManagedFileList;
import com.limegroup.gnutella.library.ManagedListStatusEvent;


/** 
 * Used to map schema URIs to Reply Collections.
 * 
 * @author Sumeet Thadani
 */
@Singleton
public class SchemaReplyCollectionMapper {
    
    private final Map<String, LimeXMLReplyCollection> mapper;
    
    protected final LimeXMLReplyCollectionFactory limeXMLReplyCollectionFactory;
    protected final Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository;
    

    @Inject
    private SchemaReplyCollectionMapper(LimeXMLReplyCollectionFactory limeXMLReplyCollectionFactory,
            Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository) {
        this.limeXMLReplyCollectionFactory = limeXMLReplyCollectionFactory;
        this.limeXMLSchemaRepository = limeXMLSchemaRepository;
        
        mapper = new HashMap<String, LimeXMLReplyCollection>();
    }


    /**
     * Adds the SchemaURI to a HashMap with the replyCollection.
     * <p>
     * Warning/Note:If the schemaURI already corresponds to a ReplyCollection
     * this method will replace the old reply collection with the new one. 
     * The old collection will be lost!
     */
    public synchronized void add(String schemaURI, LimeXMLReplyCollection replyCollection) {
        mapper.put(schemaURI, replyCollection);
    }
    
    /**
     * Looks up and returns the <tt>LimeXMLReplyCollection</tt> value for the
     * supplied schemaURI key.
     * 
     * @ return the <tt>LimeXMLReplyCollection</tt> for the given schema URI,
     * or <tt>null</tt> if we the requested mapping does not exist
     */
    public synchronized LimeXMLReplyCollection getReplyCollection(String schemaURI) {
        return mapper.get(schemaURI);
    }
    
    /**
     * Returns a collection of all available LimeXMLReplyCollections.
     * YOU MUST SYNCHRONIZE ITERATION OVER THE COLLECTION IF IT CAN BE MODIFIED.
     */
    public synchronized Collection<LimeXMLReplyCollection> getCollections() {
        return mapper.values();
    }
    
    @Inject void register(ServiceRegistry registry, final ManagedFileList managedList,
            final ListenerSupport<FileDescChangeEvent> fileDescSupport) {
        registry.register(new Service() {
            @Override
            public String getServiceName() {
                return I18nMarker.marktr("Metadata Loader");
            }
            @Override
            public void initialize() {
                loadSchemas();
                
                fileDescSupport.addListener(new EventListener<FileDescChangeEvent>() {
                    @Override
                    public void handleEvent(FileDescChangeEvent event) {
                        switch(event.getType()) {
                        case LOAD:
                            load(event);
                            break;
                        }                        
                    }
                });
                
                managedList.addFileListListener(new EventListener<FileListChangedEvent>() {
                    @Override
                    public void handleEvent(FileListChangedEvent event) {
                        switch(event.getType()) {
                        case REMOVED:
                            removeFileDesc(event.getFileDesc());
                            break;
                        case CHANGED:
                            removeFileDesc(event.getOldValue());
                            break; 
                        case CLEAR:
                            loadSchemas();
                            break;
                        }
                    }
                });
                
                managedList.addManagedListStatusListener(new EventListener<ManagedListStatusEvent>() {
                    @Override
                    public void handleEvent(ManagedListStatusEvent event) {
                        switch (event.getType()) {
                        case LOAD_FINISHING:
                            finishLoading();
                            break;
                        case SAVE:
                            save(event);
                            break;

                        }
                    }
                });
            }
            @Override
            public void start() {
                // TODO Auto-generated method stub
                
            }
            @Override
            public void stop() {
                // TODO Auto-generated method stub
                
            }
        });
    }
    
    private synchronized void removeFileDesc(FileDesc fd) {
        // Get the schema URI of each document and remove from the collection
        // We must remember the schemas and then remove the doc, or we will
        // get a concurrent mod exception because removing the doc also
        // removes it from the FileDesc.
        List<LimeXMLDocument> xmlDocs = fd.getLimeXMLDocuments();
        List<String> schemas = new LinkedList<String>();
        for (LimeXMLDocument doc : xmlDocs)
            schemas.add(doc.getSchemaURI());
        for (String uri : schemas) {
            LimeXMLReplyCollection col = getReplyCollection(uri);
            if (col != null)
                col.removeDoc(fd);
        }
    }
    
    /**
     * Notifies all the LimeXMLReplyCollections that the initial loading
     * has completed.
     */
    private synchronized void finishLoading() {
        Collection<LimeXMLReplyCollection> replies = getCollections();
        for (LimeXMLReplyCollection col : replies)
            col.loadFinished();
    }
    
    /**
     * Serializes the current LimeXMLReplyCollection to disk.
     */
    private void save(ManagedListStatusEvent event) {
        if (event.getList().isLoadFinished()) {
            synchronized (this) {
                Collection<LimeXMLReplyCollection> replies = getCollections();
                for (LimeXMLReplyCollection col : replies)
                    col.writeMapToDisk();                
            }
        }
    }
    
    /**
     * Loads the map with the LimeXMLDocument for a given FileDesc. If no LimeXMLDocument
     * exists for the FileDesc, one is created for it.
     */
    private synchronized void load(FileDescChangeEvent event) {
        Collection<LimeXMLReplyCollection> replies = getCollections();
        for (LimeXMLReplyCollection col : replies) {
            col.initialize(event.getSource(), event.getXmlDocs());
        }
        
        for (LimeXMLReplyCollection col : replies) {
            col.createIfNecessary(event.getSource());
        }
    }
    
    /**
     * Loads all the SchemaURI to a HashMap with the replyCollection. 
     */
    private void loadSchemas() {
        String[] schemas = limeXMLSchemaRepository.get().getAvailableSchemaURIs();
        for(String schema : schemas) {
            add(schema, limeXMLReplyCollectionFactory.createLimeXMLReplyCollection(schema));
        }
    }
}
