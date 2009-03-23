package com.limegroup.gnutella.xml;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.xml.sax.SAXException;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.licenses.LicenseFactory;

@Singleton
public class LimeXMLDocumentFactoryImpl implements LimeXMLDocumentFactory {

    private final LicenseFactory licenseFactory;
    private final Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository;

    @Inject
    public LimeXMLDocumentFactoryImpl(LicenseFactory licenseFactory, Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository) {
        this.licenseFactory = licenseFactory;
        this.limeXMLSchemaRepository = limeXMLSchemaRepository;
    }
    
    public LimeXMLDocument createLimeXMLDocument(String xml)
            throws SAXException, SchemaNotFoundException, IOException {
        return new LimeXMLDocument(xml, licenseFactory, limeXMLSchemaRepository);
    }

    public LimeXMLDocument createLimeXMLDocument(
            Map<String, String> map, String schemaURI, String keyPrefix)
            throws IOException {
        return new LimeXMLDocument(map, schemaURI, keyPrefix, licenseFactory, limeXMLSchemaRepository);
    }

    public LimeXMLDocument createLimeXMLDocument(
            Collection<? extends Entry<String, String>> nameValueList,
            String schemaURI) {
        return new LimeXMLDocument(nameValueList, schemaURI, licenseFactory, limeXMLSchemaRepository);
    }

}
