package com.limegroup.gnutella.gui;

import org.limewire.i18n.I18nMarker;

/** All kinds of options a dialog can return. */
public enum DialogOption {
    
    // NOTE: The IDs are serialized to disk, and should never change.
    
    /**
     * Constant for when the user selects the yes button
     * in a message.
     */
    YES(I18nMarker.marktr("Yes"), 101),
    
    /**
     * Constant for when the user selects the no button
     * in a message.
     */
    NO(I18nMarker.marktr("No"), 102),
    
    /**
     * Constant for when the user selects the cancel button
     * in a message giving the user a cancel option.
     */
    CANCEL(I18nMarker.marktr("Cancel"), 103),
    
    /**
     * Constant for when the user selects an "other" button
     * in a message giving the user an "other" option.
     */
    OTHER(null, 104),
    
    /** An invalid DialogOption */
    INVALID(null, -1);

    private final String text;
    private final int id;

    private DialogOption(String text, int id ) {
        this.text = text;
        this.id = id;
    }
    
    public String getText() {
        return I18n.tr(this.text);
    }
    
    public int toInt() {
        return id;
    }
    
    public static DialogOption parseInt(int num) {
        for(DialogOption option  : values()) {
            if(option.id == num)
                return option;
        }
        return INVALID;
    }
}

