package com.limegroup.gnutella.xml;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.metadata.MetaDataFactory;
import com.limegroup.gnutella.metadata.MetaDataReader;

@Singleton
public class LimeXMLReplyCollectionFactoryImpl implements
        LimeXMLReplyCollectionFactory {

    private final Provider<LimeXMLProperties> limeXMLProperties;

    private final Provider<FileManager> fileManager;

    private final LimeXMLDocumentFactory limeXMLDocumentFactory;

    private final MetaDataReader metaDataReader;
    
    private final MetaDataFactory metaDataFactory;
    
    @Inject
    public LimeXMLReplyCollectionFactoryImpl(
            Provider<LimeXMLProperties> limeXMLProperties, Provider<FileManager> fileManager,
            LimeXMLDocumentFactory limeXMLDocumentFactory, MetaDataReader metaDataReader,
            MetaDataFactory metaDataFactory) {
        this.limeXMLProperties = limeXMLProperties;
        this.fileManager = fileManager;
        this.limeXMLDocumentFactory = limeXMLDocumentFactory;
        this.metaDataReader = metaDataReader;
        this.metaDataFactory = metaDataFactory;
    }

    public LimeXMLReplyCollection createLimeXMLReplyCollection(String URI) {
        return new LimeXMLReplyCollection(URI, limeXMLProperties.get().getXMLDocsDir(), fileManager, limeXMLDocumentFactory, metaDataReader, metaDataFactory);
    }

}
