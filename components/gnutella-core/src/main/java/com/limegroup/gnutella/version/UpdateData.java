package com.limegroup.gnutella.version;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.Version;

import com.limegroup.gnutella.URN;

/**
 * A single message for updating.
 *
 * Contains information if the current architecture applies to this update.
 */
public class UpdateData implements Cloneable, UpdateInformation {
    
    private static final Log LOG = LogFactory.getLog(UpdateData.class);
    
    /** The 'from' version. */
    private volatile Version fromVersion;
    
    /** The 'to' version. */
    private volatile Version toVersion;
    
    /** The 'for' version. */
    private volatile Version forVersion;
    
    /** Is valid for pro? */
    private volatile boolean isPro;
    
    /** Is valid for free? */
    private volatile boolean isFree;
    
    /** The url to send this to. */
    private volatile String updateURL;
    
    /** The style of this update. */
    private volatile int updateStyle;
    
    /** The javafrom */
    private volatile Version fromJava;
    
    /** The javato */
    private volatile Version toJava;
    
    /** The OS[] this applies to. */
    private volatile OS[] osList;
    
    /** The language this applies to. */
    private volatile String language;
    
    /** The text of this message */
    private volatile String updateText;
    
    /** The text of button1. */
    private volatile String button1Text;
    
    /** The text of button2. */
    private volatile String button2Text;
    
    /** The text of the update title. */
    private volatile String updateTitle;
    
    /** The URN of the version. */
    private volatile URN urn;
    
    /** The Tiger Tree Root of the version. */
    private volatile String ttRoot;
    
    /** The command to run to launch the update. */
    private volatile String updateCommand;
    
    /** The filename to name the update file on disk. */
    private volatile String updateFileName;
    
    /** The size of the update on disk. */
    private volatile int size;
    
    /**
     * Constructs a new UpdateData object.
     */
    UpdateData() {}
    
    /**
     * The data.
     */
    @Override
    public String toString() {
        return "\n{" + 
            "from: " + fromVersion + ", to: " + toVersion + ", for: " + forVersion + 
            ", pro: " + isPro + ", free: " + isFree + ", url: " + updateURL + ", style: " + updateStyle +
            ", javaFrom: " + fromJava + ", javaTo: " + toJava + ", osList: " + OS.toString(osList) +
            ", language: " + language + ", text: " + updateText + ", title: " + updateTitle + 
            ", urn: " + urn + ", ttroot: " + ttRoot + ", updateCommand: " + updateCommand +
            ", updateFileName: " + updateFileName + ", size: " + size + "}";
    }
    
    /** Sets the from */
    void setFromVersion(Version v) { fromVersion = v; }
    
    /** Sets the to */
    void setToVersion(Version v) { toVersion = v; }
    
    /** Sets the forVersion */
    void setForVersion(Version v) { forVersion = v; }
    
    /** Sets the pro status */
    void setPro(boolean b) { isPro = b; }
    
    /** Sets the free status */
    void setFree(boolean b) { isFree = b; }
    
    /** Sets the update URL */
    void setUpdateURL(String s) { updateURL = s; }
    
    /** Sets the style */
    void setStyle(int s) { updateStyle = s; }
    
    /** Sets the fromJava */
    void setFromJava(Version v) { fromJava = v; }
    
    /** Sets the toJava */
    void setToJava(Version v) { toJava = v; }
    
    /** Sets the osList */
    void setOSList(OS[] os) { osList = os; }
    
    /** Sets the language */
    void setLanguage(String l) { language = l; }
    
    /** Sets the update text */
    void setUpdateText(String t) { updateText = t; }
    
    /** Sets the button1 text */
    void setButton1Text(String t) { button1Text = t; }
    
    /** Sets the button2 text */
    void setButton2Text(String t) { button2Text = t; }
    
    /** Sets the text of the title */
    void setUpdateTitle(String t) { updateTitle = t; }
    
    /** Sets the update URN. */
    void setUpdateURN(URN urn) { this.urn = urn; }
    
    /** Sets the update TT root. */
    void setUpdateTTRoot(String root) { this.ttRoot = root; }
    
    /** Sets the update command to run. */
    void setUpdateCommand(String command) { updateCommand = command; }
    
    /** Sets the filename to save the update to. */
    void setUpdateFileName(String filename) { updateFileName =  filename; }
    
    /** Sets the size of the update. */
    void setUpdateSize(int size) { this.size = size; }
    
    /** Gets the language. */
    String getLanguage() { return language; }
    
    /// the below getters implement UpdateInformation.

    /** Gets the update version as a string. */
    public String getUpdateVersion() { return forVersion.toString(); }
    
    /** Gets the update text. */
    public String getUpdateText() { return updateText; }
    
    /** Gets the update URL */
    public String getUpdateURL() { return updateURL; }
    
    /** Gets the update style. */
    public int getUpdateStyle() { return updateStyle; }
    
    /** Gets the button1 text. */
    public String getButton1Text() { return button1Text; }
    
    /** Gets the button2 text. */
    public String getButton2Text() { return button2Text; }
    
    /** Gets the update title. */
    public String getUpdateTitle() { return updateTitle; }
    
    /** Gets the update file name. */
    public String getUpdateFileName() { return updateFileName; }
    
    /** Gets the update command to run. */
    public String getUpdateCommand() { return updateCommand; }
    
    /** Gets the update URN */
    public URN getUpdateURN() { return urn; }
    
    /** Gets the TigerTreeRoot hash. */
    public String getTTRoot() { return ttRoot; }
    
    /** Gets the size of the update. */
    public long getSize() { return size; }
    
    /**
     * Determines if this matches (on all except language).
     * The OS match is taken from CommonUtils.
     */
    boolean isAllowed(Version currentV, boolean currentPro, int currentStyle, Version currentJava) {
        return currentV.compareTo(fromVersion) >= 0 && 
               currentV.compareTo(toVersion) < 0 && 
               currentStyle <= updateStyle &&
               OS.hasAcceptableOS(osList) &&
               isValidJava(currentJava) &&
               (currentPro ? isPro : isFree);
    }
    
    /**
     * Determines if the java versions are okay.
     */
    boolean isValidJava(Version currentV) {
        if(currentV == null || (fromJava == null && toJava == null))
            return true;
            
        if(fromJava == null)
            return currentV.compareTo(toJava) < 0;
        if(toJava == null)
            return currentV.compareTo(fromJava) >= 0;
        
        return currentV.compareTo(fromJava) >= 0 &&
               currentV.compareTo(toJava) < 0;
    }   
    
    /**
     * Clones a new update data that is exactly like this one.
     */
    @Override
    public Object clone() {
        Object clone = null;
        try {
            clone = super.clone();
        } catch(CloneNotSupportedException cnse) {
            LOG.error("shoulda cloned", cnse);
        }
        return clone;
    }
}
