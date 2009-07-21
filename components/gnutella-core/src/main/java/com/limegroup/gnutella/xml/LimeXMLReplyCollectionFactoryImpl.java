package com.limegroup.gnutella.xml;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.metadata.MetaDataFactory;
import com.limegroup.gnutella.metadata.MetaDataReader;

@Singleton
class LimeXMLReplyCollectionFactoryImpl implements LimeXMLReplyCollectionFactory {

    private final Provider<LimeXMLProperties> limeXMLProperties;

    private final Provider<Library> library;

    private final LimeXMLDocumentFactory limeXMLDocumentFactory;

    private final MetaDataReader metaDataReader;
    
    private final MetaDataFactory metaDataFactory;
    
    @Inject
    public LimeXMLReplyCollectionFactoryImpl(
            Provider<LimeXMLProperties> limeXMLProperties, Provider<Library> library,
            LimeXMLDocumentFactory limeXMLDocumentFactory, MetaDataReader metaDataReader,
            MetaDataFactory metaDataFactory) {
        this.limeXMLProperties = limeXMLProperties;
        this.library = library;
        this.limeXMLDocumentFactory = limeXMLDocumentFactory;
        this.metaDataReader = metaDataReader;
        this.metaDataFactory = metaDataFactory;
    }

    public LimeXMLReplyCollection createLimeXMLReplyCollection(String URI) {
        return new LimeXMLReplyCollection(URI, limeXMLProperties.get().getXMLDocsDir(), library, limeXMLDocumentFactory, metaDataReader, metaDataFactory);
    }

}
