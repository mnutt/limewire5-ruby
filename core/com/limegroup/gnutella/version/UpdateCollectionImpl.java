package com.limegroup.gnutella.version;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.Version;
import org.limewire.util.VersionFormatException;
import org.limewire.util.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.xml.LimeXMLUtils;

public class UpdateCollectionImpl implements UpdateCollection {
 private static final Log LOG = LogFactory.getLog(UpdateCollectionImpl.class);
    
    /**
     * The id of this UpdateCollection.
     */
    private int collectionId = Integer.MIN_VALUE;
    
    /**
     * The timestamp of this collection.
     */
    private long collectionTimestamp = -1;
    
    /**
     * The list of UpdateData's in this collection.
     */
    private final List<UpdateData> updateDataList;
    
    /**
     * The list of DownloadDatas in this collection.
     */
    private List<DownloadInformation> downloadDataList = new LinkedList<DownloadInformation>();

    private final ApplicationServices applicationServices;
    
    /**
     * Ensure that this is only created by using the factory constructor.
     */
    UpdateCollectionImpl(String xml, ApplicationServices applicationServices) {
        this.applicationServices = applicationServices;
        if(LOG.isTraceEnabled())
            LOG.trace("Parsing Update XML: " + xml);
        List<UpdateData> updateData = new ArrayList<UpdateData>();
        parse(xml, updateData);
        updateDataList = Collections.unmodifiableList(updateData);
    }
    
    /**
     * A string rep of the collection.
     */
    @Override
    public String toString() {
        return "Update Collection, id: " + collectionId + ", timestamp: " + collectionTimestamp +
               ", data: " + updateDataList;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.version.UpdateCollectionI#getId()
     */
    public int getId() {
        return collectionId;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.version.UpdateCollectionI#getTimestamp()
     */
    public long getTimestamp() {
        return collectionTimestamp;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.version.UpdateCollectionI#getUpdateData()
     */
    public List<UpdateData> getUpdateData() {
        return updateDataList;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.version.UpdateCollectionI#getUpdatesWithDownloadInformation()
     */
    public List<DownloadInformation> getUpdatesWithDownloadInformation() {
        return Collections.unmodifiableList(downloadDataList);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.version.UpdateCollectionI#getUpdateDataFor(org.limewire.util.Version, java.lang.String, boolean, int, org.limewire.util.Version)
     */
    public UpdateData getUpdateDataFor(Version currentV, String lang, boolean currentPro,
                                int currentStyle, Version currentJava) {
        UpdateData englishMatch = null;
        UpdateData exactMatch = null;
        
        // Iterate through them till we find an acceptable version.
        // Remember for the 'English' and 'Exact' match --
        // If we got an exact, use that.  Otherwise, use English.
        for(UpdateData next : updateDataList) {
            if(next.isAllowed(currentV, currentPro, currentStyle, currentJava)) {
                if(lang.equals(next.getLanguage())) {
                    exactMatch = next;
                    break;
                } else if("en".equals(next.getLanguage()) && englishMatch == null) {
                    englishMatch = next;
                }
            }
        }
        
        if(exactMatch == null)
            return englishMatch;
        else
            return exactMatch;
    }

    /**
     * Parses the XML and fills in the data of this collection.
     * @param updateDataList 
     */
    private void parse(String xml, List<UpdateData> updateDataList) {
        Document d;
        try {
            d = XMLUtils.getDocument(xml, LOG);
        } catch(IOException ioe) {
            LOG.error("Unable to parse: " + xml, ioe);
            return;
        }
        
        parseDocumentElement(d.getDocumentElement(), updateDataList);
    }
    
    /**
     * Parses the document element.
     *
     * This requires that the element be "update" and has the attribute 'id'.
     * The 'timestamp' attribute is checked (but is optional), as are child 'msg'
     * elements.
     * @param updateDataList 
     */
    private void parseDocumentElement(Node doc, List<UpdateData> updateDataList) {
        // Ensure the document element is the 'update' element.
        if(!"update".equals(doc.getNodeName()))
            return;
        
        // Parse the 'id' & 'timestamp' attributes.
        NamedNodeMap attr = doc.getAttributes();
        
        // we MUST have an id.
        String idText = getAttributeText(attr, "id");
        if(idText == null) {
            LOG.error("No id attribute.");
            return;
        }
        
        try {
            collectionId = Integer.parseInt(idText);
        } catch(NumberFormatException nfe) {
            LOG.error("Couldn't get collection id from: " + idText, nfe);
            return;
        }
        
        // Parse the optional 'timestamp' attribute.
        String timestampText = getAttributeText(attr, "timestamp");
        if(timestampText != null) {
            try {
                collectionTimestamp = Long.parseLong(timestampText);
            } catch(NumberFormatException nfe) {
                LOG.warn("Couldn't get timestamp from: " + timestampText, nfe);
            }
        }
        
        NodeList children = doc.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if("msg".equals(child.getNodeName()))
                parseMsgItem(child, updateDataList);
        }
    }
    
    /**
     * Parses a single msg item.
     *
     * The elements this parses are:
     *      from     -- OPTIONAL (defaults to 0.0.0)
     *      to       -- OPTIONAL (defaults to the 'for' value)
     *      for      -- REQUIRED
     *      pro      -- OPTIONAL (see free)
     *      free     -- OPTIONAL (if both pro & free are missing, both default to true.
                                  otherwise they defaults to false.  any non-null value == true)
     *      url      -- REQUIRED
     *      style    -- REQUIRED (accepts a number only.)
     *      javafrom -- OPTIONAL (see javato)
     *      javato   -- OPTIONAL (if both are missing, all ranges are valid.  if one is missing, defaults to above or below that.)
     *      os       -- OPTIONAL (defaults to '*' -- accepts a comma delimited list.)
     *
     * The below elements are necessary for downloading the update in the network.
     *      urn      -- The BITPRINT of the download
     *      ucommand -- The command to run to invoke the update.
     *      uname    -- The filename on disk the update should have.
     *      size     -- The size of the update when completed.
     *
     * If any values exist but error while parsing, the entire block is considered
     * invalid and ignored.
     * @param updateDataList 
     */
    private void parseMsgItem(Node msg, List<UpdateData> updateDataList) {
        UpdateData data = new UpdateData();
        
        NamedNodeMap attr = msg.getAttributes();
        String fromV = getAttributeText(attr, "from");
        String toV = getAttributeText(attr, "to");
        String forV = getAttributeText(attr, "for");
        String pro = getAttributeText(attr, "pro");
        String free = getAttributeText(attr, "free");
        String url = getAttributeText(attr, "url");
        String style = getAttributeText(attr, "style");
        String javaFrom = getAttributeText(attr, "javafrom");
        String javaTo = getAttributeText(attr, "javato");
        String os = getAttributeText(attr, "os");
        String osv = getAttributeText(attr, "osv");
        String updateURN = getAttributeText(attr, "urn");
        String updateCommand = getAttributeText(attr, "ucommand");
        String updateName = getAttributeText(attr, "uname");
        String fileSize = getAttributeText(attr, "size");
        
        if(updateURN != null) {
            try {
                URN urn = URN.createSHA1Urn(updateURN);
                String tt = URN.getTigerTreeRoot(updateURN);
                data.setUpdateURN(urn);
                data.setUpdateTTRoot(tt);
            } catch(IOException ignored) {
                LOG.warn("Invalid bitprint urn: " + updateURN, ignored);
            }
        }
        
        data.setUpdateCommand(updateCommand);
        data.setUpdateFileName(updateName);
        
        if(fileSize != null) {
            try {
                data.setUpdateSize(Integer.parseInt(fileSize));
            } catch(NumberFormatException nfe) {
                LOG.warn("Invalid size: " + fileSize);
            }
        }
        
        // if this has enough information for downloading, add it to the list of potentials.
        if(data.getUpdateURN() != null && data.getUpdateFileName() != null && data.getSize() != 0) {
            if (LOG.isDebugEnabled())
                LOG.debug("Adding new download data item: " + data);
            downloadDataList.add(data);
        }
        
        if(forV == null || url == null || style == null) {
            LOG.error("Missing required for, url, or style.");
            return;
        }
        
        if(fromV == null)
            fromV = "0.0.0";
        if(toV == null)
            toV = forV;

        try {
            data.setFromVersion(new Version(fromV));
            data.setToVersion(new Version(toV));
            data.setForVersion(new Version(forV));
            if(javaFrom != null)
                data.setFromJava(new Version(javaFrom));
            if(javaTo != null)
                data.setToJava(new Version(javaTo));
        } catch(VersionFormatException vfe) {
            LOG.error("Invalid version", vfe);
            return;
        }
        
        if(pro == null && free == null) {
            data.setPro(true);
            data.setFree(true);
        } else {
            data.setPro(pro != null);
            data.setFree(free != null);
        }
        
        // Update the URL to contain the correct pro & language.
        url = LimeWireUtils.addLWInfoToUrl(url, applicationServices.getMyGUID());
        data.setUpdateURL(url);
        
        try {
            data.setStyle(Integer.parseInt(style));
        } catch(NumberFormatException nfe) {
            LOG.error("Invalid style", nfe);
            return;
        }
        
        if(os == null)
            os = "*";
        data.setOSList(OS.createFromList(os,osv));
                
        NodeList children = msg.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if("lang".equals(child.getNodeName()))
                parseLangItem((UpdateData)data.clone(), child, updateDataList);
        }
    }
    
    /**
     * Parses a single lang item.
     *
     * Accepts attributes 'id', 'button1', and 'button2'.
     * 'id' is REQUIRD.  others are optional.
     * REQUIRES a text content inside.
     * @param updateDataList 
     */
    private void parseLangItem(UpdateData data, Node lang, List<UpdateData> updateDataList) {
        // Parse the id & url & current attributes -- all MUST exist.
        NamedNodeMap attr = lang.getAttributes();
        String id = getAttributeText(attr, "id");
        String button1 = getAttributeText(attr, "button1");
        String button2 = getAttributeText(attr, "button2");
        String title = getAttributeText(attr, "title");
        String msg = LimeXMLUtils.getTextContent(lang);
        
        if(id == null || msg == null || msg.equals("")) {
            LOG.error("Missing id or message.");
            return;
        }
            
        data.setLanguage(id);
        data.setButton1Text(button1);
        data.setButton2Text(button2);
        data.setUpdateText(msg);
        data.setUpdateTitle(title);
        
        // A-Okay -- we've got a good UpdateData.
        updateDataList.add(data);
    }
    
    /**
     * Gets the text from an attribute map.
     */
    private String getAttributeText(NamedNodeMap map, String attr) {
        Node node = map.getNamedItem(attr);
        if(node != null)
            return node.getNodeValue();
        else
            return null;
    }
}
