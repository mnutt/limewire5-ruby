package com.limegroup.gnutella.gui.properties;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import org.limewire.i18n.I18nMarker;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.search.TableLine;
import com.limegroup.gnutella.gui.xml.XMLUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/** Composes information about a search result into text for display to the user. */
public class ResultProperties {

    /** Gets the "Properties" title, which is "Get Info" on a Mac. */
    public static String title() {
        if (OSUtils.isMacOSX())
            return I18n.tr("Get Info");
        else
            return I18n.tr("Properties");
    }

    /** File properties. */
    private Map<String, String> file;
    /** Properties from metadata. */
    private Map<String, String> meta;
    /** XML metadata. */
    private String xml;

    /** Composes information from the given objects into text for display in the search result properties dialog box. */
    public ResultProperties(TableLine line) {

        // Default properties
        file = new HashMap<String, String>();
        file.put(I18nMarker.marktr("Name"), line.getFilename());
        file.put(I18nMarker.marktr("Size"), size(line.getSize()));
        file.put(I18nMarker.marktr("Hash"), line.getSHA1().toString());
        if (line.getNamedMediaType() != null)
            file.put(I18nMarker.marktr("Type"), line.getNamedMediaType().getName());

        // Additional properties from metadata
        LimeXMLDocument document = line.getXMLDocument();
        if (document != null) {
            meta = XMLUtils.getDisplayProperties(document);
            xml = document.toString();
            if (xml.equals(""))
                xml = null; // Don't show blank text
        }
        
        // Add units
        if (meta != null && meta.containsKey("Bitrate"))
            meta.put(I18nMarker.marktr("Bitrate"),  I18n.tr("{0} Kbps", meta.get("Bitrate")));
    }

    /** Gets the file properties. */
    public Map<String, String> getFileProperties() {
        return file;
    }
    
    /** Gets the properties from metadata. */
    public Map<String, String> getMetaProperties() {
        return meta;
    }
    
    /** Gets the XML metadata. */
    public String getXML() {
        return xml;
    }
    
    /** Composes size into descriptive text. */
    private static String size(long size) {
        return I18n.tr("{0} ({1}) bytes", GUIUtils.toUnitbytes(size), NumberFormat.getInstance().format(size));
    }
}
