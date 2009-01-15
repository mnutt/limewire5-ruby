package org.limewire.core.impl.library;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.MetaDataManager;
import org.limewire.core.impl.util.FilePropertyKeyPopulator;
import org.limewire.util.NameValue;
import org.limewire.util.NotImplementedException;
import org.xml.sax.SAXException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection;
import com.limegroup.gnutella.xml.LimeXMLUtils;
import com.limegroup.gnutella.xml.SchemaNotFoundException;
import com.limegroup.gnutella.xml.SchemaReplyCollectionMapper;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection.MetaDataState;

@Singleton
public class MetaDataManagerImpl implements MetaDataManager {
    private final SchemaReplyCollectionMapper schemaReplyCollectionMapper;

    private final LimeXMLDocumentFactory limeXMLDocumentFactory;

    @Inject
    public MetaDataManagerImpl(LimeXMLDocumentFactory limeXMLDocumentFactory,
            SchemaReplyCollectionMapper schemaReplyCollectionMapper) {
        this.limeXMLDocumentFactory = limeXMLDocumentFactory;
        this.schemaReplyCollectionMapper = schemaReplyCollectionMapper;
    }

    @Override
    public void save(LocalFileItem localFileItem) {
        if (localFileItem instanceof CoreLocalFileItem) {
            CoreLocalFileItem coreLocalFileItem = (CoreLocalFileItem) localFileItem;
            saveMetaData(coreLocalFileItem);
        }
    }

    private void saveMetaData(CoreLocalFileItem coreLocalFileItem) {
        FileDesc fileDesc = coreLocalFileItem.getFileDesc();
        Category category = coreLocalFileItem.getCategory();

        String limeXMLSchemaUri = getLimeXmlSchemaUri(category);
        LimeXMLDocument oldDocument = fileDesc.getXMLDocument(limeXMLSchemaUri);

        String input = buildInput(fileDesc, limeXMLSchemaUri, coreLocalFileItem);

        if (oldDocument != null && (input == null || input.trim().length() == 0)) {
            removeMeta(fileDesc, limeXMLSchemaUri);
            return;
        } else if (input == null || input.trim().length() == 0) {
            return;
        }

        LimeXMLDocument newDoc = null;

        try {
            newDoc = limeXMLDocumentFactory.createLimeXMLDocument(input);
        } catch (SAXException e) {
            coreLocalFileItem.reloadProperties();
            throw new NotImplementedException(
                    "Internal Document Error. Data could not be saved.");
        } catch (SchemaNotFoundException e) {
            coreLocalFileItem.reloadProperties();
            throw new NotImplementedException(
                    "Internal Document Error. Data could not be saved.");
        } catch (IOException e) {
            coreLocalFileItem.reloadProperties();
            throw new NotImplementedException(
                    "Internal Document Error. Data could not be saved.");
        }

        String schemaURI = newDoc.getSchemaURI();
        LimeXMLReplyCollection collection = schemaReplyCollectionMapper
                .getReplyCollection(schemaURI);

        LimeXMLDocument result = null;

        if (oldDocument != null) {
            result = merge(oldDocument, newDoc);
            oldDocument = collection.replaceDoc(fileDesc, result);
        } else {
            result = newDoc;
            collection.addReply(fileDesc, result);
        }

        if (LimeXMLUtils.isSupportedFormat(fileDesc.getFileName())) {
            final MetaDataState committed = collection.mediaFileToDisk(fileDesc, result);
            if (committed != MetaDataState.NORMAL && committed != MetaDataState.UNCHANGED) {
                coreLocalFileItem.reloadProperties();
                throw new NotImplementedException(
                        "Internal Document Error. Data could not be saved.");
            }
        } else if (!collection.writeMapToDisk()) {
            coreLocalFileItem.reloadProperties();
            throw new NotImplementedException(
                    "Internal Document Error. Data could not be saved.");
        }
    }

    private String getLimeXmlSchemaUri(Category category) {
        switch (category) {
        case AUDIO:
            return LimeXMLNames.AUDIO_SCHEMA;
        case DOCUMENT:
            return LimeXMLNames.DOCUMENT_SCHEMA;
        case IMAGE:
            return LimeXMLNames.IMAGE_SCHEMA;
        case PROGRAM:
            return LimeXMLNames.APPLICATION_SCHEMA;
        case VIDEO:
            return LimeXMLNames.VIDEO_SCHEMA;
        }
        throw new UnsupportedOperationException("Category: " + category + " is not supported.");
    }

    /**
     * Merge the current and new doc.
     */
    public LimeXMLDocument merge(LimeXMLDocument currentDoc, LimeXMLDocument newDoc) {
        if (!currentDoc.getSchemaURI().equalsIgnoreCase(newDoc.getSchemaURI())) {
            throw new IllegalArgumentException(
                    "Current XML document and new XML document must be of the same type!");
        }

        Map<String, Map.Entry<String, String>> map = new HashMap<String, Map.Entry<String, String>>();

        // Initialize the Map with the current fields
        for (Map.Entry<String, String> entry : currentDoc.getNameValueSet())
            map.put(entry.getKey(), entry);

        // And overwrite everything with the new fields
        for (Map.Entry<String, String> entry : newDoc.getNameValueSet())
            map.put(entry.getKey(), entry);

        return limeXMLDocumentFactory
                .createLimeXMLDocument(map.values(), currentDoc.getSchemaURI());
    }

    private String buildInput(FileDesc fileDesc, String limeXMLSchemaUri,
            LocalFileItem localFileItem) {
        List<NameValue<String>> nameValueList = new ArrayList<NameValue<String>>();
        Category category = localFileItem.getCategory();
        
        for (FilePropertyKey filePropertyKey : FilePropertyKey.getEditableKeys()) {
            String limeXmlName = FilePropertyKeyPopulator.getLimeXmlName(category, filePropertyKey);
            if (limeXmlName != null) {
                String value = localFileItem.getPropertyString(filePropertyKey);
                NameValue<String> nameValue = new NameValue<String>(limeXmlName, value);
                nameValueList.add(nameValue);
            }
        }

        return limeXMLDocumentFactory.createLimeXMLDocument(nameValueList, limeXMLSchemaUri)
                .getXMLString();
    }

    private void removeMeta(FileDesc fileDesc, String limeXMLSchemaUri) {

        LimeXMLReplyCollection collection = schemaReplyCollectionMapper
                .getReplyCollection(limeXMLSchemaUri);

        if (!collection.removeDoc(fileDesc)) {
            throw new NotImplementedException("Internal Document Error. Data could not be saved.");
        }
    }

}
