package com.limegroup.gnutella.gui.xml.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * 
 */
public final class MetaEditorUtil {
   
    private static final Log LOG = LogFactory.getLog(MetaEditorUtil.class);
    
    private static final Map<String, String> XSD_MESSAGEBUNDLE_BRIDGE = new HashMap<String, String>();
    
    static {
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.AUDIO, I18nMarker.marktr("Audio"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.AUDIO_TITLE, I18nMarker.marktr("Title:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.AUDIO_ARTIST, I18nMarker.marktr("Artist:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.AUDIO_ALBUM, I18nMarker.marktr("Album:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.AUDIO_GENRE, I18nMarker.marktr("Genre:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.AUDIO_YEAR, I18nMarker.marktr("Year:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.AUDIO_TYPE, I18nMarker.marktr("Type:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.AUDIO_TRACK, I18nMarker.marktr("Track:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.AUDIO_LANGUAGE, I18nMarker.marktr("Language:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.AUDIO_SECONDS, I18nMarker.marktr("Length:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.AUDIO_BITRATE, I18nMarker.marktr("Bitrate:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.AUDIO_COMMENTS, I18nMarker.marktr("Comments:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.AUDIO_SHA1, I18nMarker.marktr("SHA1:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.AUDIO_PRICE, I18nMarker.marktr("Price:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.AUDIO_LINK, I18nMarker.marktr("Link:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.AUDIO_ACTION, I18nMarker.marktr("Action:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.AUDIO_LICENSE, I18nMarker.marktr("License:"));

        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.VIDEO, I18nMarker.marktr("Video"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.VIDEO_TITLE, I18nMarker.marktr("Title:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.VIDEO_TYPE, I18nMarker.marktr("Type:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.VIDEO_YEAR, I18nMarker.marktr("Year:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.VIDEO_RATING, I18nMarker.marktr("Rating:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.VIDEO_LENGTH, I18nMarker.marktr("Length:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.VIDEO_COMMENTS, I18nMarker.marktr("Comments:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.VIDEO_LICENSE, I18nMarker.marktr("License:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.VIDEO_LICENSETYPE, I18nMarker.marktr("License Type:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.VIDEO_ACTION, I18nMarker.marktr("Action:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.VIDEO_DIRECTOR, I18nMarker.marktr("Director:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.VIDEO_STUDIO, I18nMarker.marktr("Studio:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.VIDEO_LANGUAGE, I18nMarker.marktr("Language:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.VIDEO_STARS, I18nMarker.marktr("Stars: (Please separate with comma)"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.VIDEO_PRODUCER, I18nMarker.marktr("Producer: (Please separate with comma)"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.VIDEO_SUBTITLES, I18nMarker.marktr("Subtitles: (Please separate with comma)"));
        
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.DOCUMENT, I18nMarker.marktr("Document"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.DOCUMENT_TITLE, I18nMarker.marktr("Title:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.DOCUMENT_TOPIC, I18nMarker.marktr("Topic:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.DOCUMENT_AUTHOR, I18nMarker.marktr("Author:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.DOCUMENT_LICENSE, I18nMarker.marktr("License:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.DOCUMENT_LICENSETYPE, I18nMarker.marktr("License Type:"));
        
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.APPLICATION, I18nMarker.marktr("Application"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.APPLICATION_NAME, I18nMarker.marktr("Name:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.APPLICATION_PUBLISHER, I18nMarker.marktr("Publisher:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.APPLICATION_PLATFORM, I18nMarker.marktr("Platform:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.APPLICATION_LICENSETYPE, I18nMarker.marktr("License Type:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.APPLICATION_LICENSE, I18nMarker.marktr("License:"));
        
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.IMAGE, I18nMarker.marktr("Image"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.IMAGE_TITLE, I18nMarker.marktr("Title:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.IMAGE_DESCRIPTION, I18nMarker.marktr("Description:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.IMAGE_ARTIST, I18nMarker.marktr("Artist:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.IMAGE_LICENSE, I18nMarker.marktr("License:"));
        XSD_MESSAGEBUNDLE_BRIDGE.put(LimeXMLNames.IMAGE_LICENSETYPE, I18nMarker.marktr("License Type:"));
    }
    
    private MetaEditorUtil() {
    }
    
    public static boolean contains(String resource) {
        return XSD_MESSAGEBUNDLE_BRIDGE.containsKey(resource);
    }
    
    /**
     * 
     */
    public static String getStringResource(String resourceKey) {
        String rscKey = XSD_MESSAGEBUNDLE_BRIDGE.get(resourceKey);
        assert rscKey != null : "Unknown resourceKey: " + resourceKey;
        return I18n.tr(rscKey);
    }
    
    /**
     * 
     */
    public static String getKind(File file) {
        String name = file.getName();
        
        if (LimeXMLUtils.isMP3File(name)) {
            return I18n.tr("MPEG-1 Audio Layer 3");
        } else if (LimeXMLUtils.isM4AFile(name)) {
            return I18n.tr("MPEG-4 Audio");
        } else if (LimeXMLUtils.isOGGFile(name)) {
            return I18n.tr("Ogg Vorbis");
        } else {
            return null;
        }
    }
    
    
    /** 
     * A placeholder value different from null. See 
     * intersection() and clean() for more info. 
     */
    private static final String EMPTY_VALUE = new String();
    
    /**
     * Returns the intersection LimeXMLDocument(s) of the passed FileDescs.
     * If schemaURI is null it will return the intersection of all 
     * LimeXMLDocuments (severed by schema). If schemaURI is not null
     * it will only process the specific type of Documents. If there's
     * no intersection the returned LimeXMLDocument-Array will be empty!
     */
    public static LimeXMLDocument[] intersection(FileDesc[] fds, String schemaURI) {
        
        if (schemaURI != null) {
            schemaURI = schemaURI.toLowerCase(Locale.US);
        }
        
        Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
        for(int i = 0; i < fds.length; i++) {
            List<LimeXMLDocument> docs = fds[i].getLimeXMLDocuments();
            
            // Think of the following case. Two images have meta data 
            // and an 3rd image has no meta data at all (that means no 
            // LimeXMLDocument). The consequence is that the intersection
            // of the two images is applied to the third image which is
            // wrong. Solution: we've no intersection at all!
            if (docs.isEmpty()) {
                map.clear();
                break;
            }
            
            for(LimeXMLDocument doc : docs) {
                String uri = doc.getSchemaURI().toLowerCase(Locale.US);
                
                if (schemaURI == null || schemaURI.equals(uri)) {
                    
                    Map<String, String> intersectionMap = map.get(uri);
                    if (intersectionMap == null) {
                        intersectionMap = new HashMap<String, String>();
                        map.put(uri, intersectionMap);
                    }
                    
                    // This was just the setup, 
                    // figure out the intersection now
                    intersection(doc, intersectionMap);
                    
                    if (schemaURI != null) {
                        break;
                    }
                }
            }
        }
        
        List<LimeXMLDocument> docs = new ArrayList<LimeXMLDocument>(map.size());
        for(Map.Entry<String, Map<String, String>> entry : map.entrySet()) {
            String uri = entry.getKey();
            Set<Map.Entry<String, String>> intersection = clean(entry.getValue()).entrySet();
            
            if (!intersection.isEmpty()) {
                LimeXMLSchema schema = GuiCoreMediator.getLimeXMLSchemaRepository().getSchema(uri);
                if (schema == null) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("LimeXMLSchema for " + uri + " is null");
                    }
                    continue;
                }
                
                // At least one field from the intersection must be known
                // from the schema. Otherwise it's like an empty document
                // and considered invalid!
                boolean atLeastOneKnownFieldName = false;
                Set<String> fieldNames = new HashSet<String>(schema.getCanonicalizedFieldNames());
                for(Map.Entry<String, String> etr : intersection) {
                    if (fieldNames.contains(etr.getKey())  && etr.getValue() != null) {
                        atLeastOneKnownFieldName = true;
                        break;
                    }
                }
                
                if (atLeastOneKnownFieldName) {
                    docs.add(GuiCoreMediator.getLimeXMLDocumentFactory().createLimeXMLDocument(intersection,
                            uri));
                } else {
                    if (LOG.isErrorEnabled())
                        LOG.error("All fields of " + intersection + " are unknown for Schema " + schema);
                }
            }
        }
        
        return docs.toArray(new LimeXMLDocument[0]);
    }
    
    /**
     * This is a bit triky. The Intersection is based on Keys and Values.
     * We cannot use retainAll because we're building the intersection
     * between more than two LimeXMLDocuments
     */
    private static Map intersection(LimeXMLDocument doc, Map<String, String> intersectionMap) {
        for(Map.Entry<String, String> entry : doc.getNameValueSet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            String current = intersectionMap.get(key);
            if (current != EMPTY_VALUE) {
                if (current == null || current.equals(value)) {
                    intersectionMap.put(key, value);
                } else {
                    // No intersection! 'Lock' the Key!
                    intersectionMap.put(key, EMPTY_VALUE);
                }
            }
        }
        
        Set<String> keys = new HashSet<String>(intersectionMap.keySet());
        keys.removeAll(doc.getNameSet());
        
        for(String next : keys)
            intersectionMap.put(next, EMPTY_VALUE);
        
        return intersectionMap;
    }
    
    /**
     * Removes all entries from the Map whose value is EMPTY_VALUE.
     * Call it before turning the intersection Map into a LimeXMLDocument
     */
    private static Map<String, String> clean(Map<String, String> intersectionMap) {
        for(Iterator<String> it = intersectionMap.values().iterator(); it.hasNext(); ) {
            if(it.next() == EMPTY_VALUE)
                it.remove();
        }
        return intersectionMap;
    }
    
    /**
     * Merge the current and new doc.
     */
    public static LimeXMLDocument merge(LimeXMLDocument currentDoc, LimeXMLDocument newDoc) {
        if (!currentDoc.getSchemaURI().equalsIgnoreCase(newDoc.getSchemaURI())) {
            throw new IllegalArgumentException("Current XML document and new XML document must be of the same type!");
        }
        
        Map<String, Map.Entry<String, String>> map = new HashMap<String, Map.Entry<String, String>>();
        
        // Initialize the Map with the current fields
        for(Map.Entry<String, String> entry : currentDoc.getNameValueSet())
            map.put(entry.getKey(), entry);
        
        // And overwrite everything with the new fields
        for(Map.Entry<String, String> entry : newDoc.getNameValueSet())
            map.put(entry.getKey(), entry);
        
        return GuiCoreMediator.getLimeXMLDocumentFactory().createLimeXMLDocument(map.values(), currentDoc.getSchemaURI());
    }
}
