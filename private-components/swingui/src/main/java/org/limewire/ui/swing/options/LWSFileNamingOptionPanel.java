package org.limewire.ui.swing.options;

import java.io.File;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.StoreTemplateProcessor;

public class LWSFileNamingOptionPanel extends OptionPanel {

    private JComboBox subFolderComboBox;
    private JComboBox fileNameComboBox;
    
    private JButton okButton;
    private JButton cancelButton;
    
    /**
     * String for storing the saved file name template
     */
    private String oldFileName;
    
    /**
     * String for storing the saved subdirectory name template
     */
    private String oldSubDirectory; 
    
    /**
     * Variables for displaying substitutable values, visual form
     * is converted to the selected language
     */
    private final String artist = I18n.tr("Artist");
    private final String album  = I18n.tr("Album");
    private final String track  = I18n.tr("Track");
    private final String title  = I18n.tr("Title");
    
    /**
     * Variables for template substitutable values, always saved in
     * English to avoid problems when converting between different languages
     */
    private final String artistVar = "<" + StoreTemplateProcessor.ARTIST_LABEL + ">";
    private final String albumVar =  "<" + StoreTemplateProcessor.ALBUM_LABEL  + ">";
    private final String trackVar =  "<" + StoreTemplateProcessor.TRACK_LABEL  + ">";
    private final String titleVar =  "<" + StoreTemplateProcessor.TITLE_LABEL  + ">";
    
    public LWSFileNamingOptionPanel(Action okAction, CancelDialogAction cancelAction) {
        setLayout(new MigLayout("insets 10 10 10 10"));
        
        cancelAction.setOptionPanel(this);
        subFolderComboBox = new JComboBox(getSubDirectoryTemplatesArray());
        fileNameComboBox = new JComboBox(getFileNameTemplatesArray());
        okButton = new JButton(okAction);
        cancelButton = new JButton(cancelAction);
        
        add(new JLabel(I18n.tr("Choose how to organize files you purchased from the LimeWire Store")), "span, gapbottom 11, wrap");
        
        add(new JLabel(I18n.tr("Subfolder")));
        add(new JLabel(I18n.tr("File Name")),"growx , wrap");
        
        add(subFolderComboBox, "gapBottom 20, push");
        add(fileNameComboBox, "gapBottom 20, growx, wrap");
        
        add(okButton, "tag ok, skip 1, alignx right, growx, split 2");
        add(cancelButton, "tag cancel");
        
    }
    
    @Override
    boolean applyOptions() {
        // get the english version of the template
        String subDirectoryTemplateText = ((ListNode)subFolderComboBox.getSelectedItem()).getTemplateText();
        String fileTemplateText = ((ListNode)fileNameComboBox.getSelectedItem()).getTemplateText();
        
        // if either of the templates are different, save the new value
        if (!fileTemplateText.equals(oldFileName) )
            SharingSettings.setFileNameLWSTemplate(fileTemplateText);
                    
        if (!subDirectoryTemplateText.equals(oldSubDirectory) ) 
            SharingSettings.setSubdirectoryLWSTemplate(subDirectoryTemplateText);

        return false;
    }

    @Override
    boolean hasChanged() {
        return !SharingSettings.getFileNameLWSTemplate().equals(oldFileName) ||
                !SharingSettings.getSubDirectoryLWSTemplate().equals(oldSubDirectory);
    }

    @Override
    public void initOptions() {
        // save locally the old values for comparing later
        oldFileName = SharingSettings.getFileNameLWSTemplate();
        oldSubDirectory = SharingSettings.getSubDirectoryLWSTemplate();
        
        //  setup the jcombobox with the saved templates
        setJComboBox(fileNameComboBox, getFileNameTemplatesArray(), SharingSettings.getFileNameLWSTemplate());
        setJComboBox(subFolderComboBox, getSubDirectoryTemplatesArray(), SharingSettings.getSubDirectoryLWSTemplate());
    }

    
    /**
     * Predefined templates for file naming conventions. Contains a list of 
     * nodes where each node contains a human readable form and a parsable form
     *      artist - title, <artist> - <title>
     *      track - artist - title, <track> - <artist> - <title>
     *      artist - title - track, <artist> - <title> - <track>
     *      artist - album - title - track, <artist> - <album> - <track> - <title> (default)
     * 
     * @return an array of ListNodes of file name templates
     */
    private ListNode[] getFileNameTemplatesArray() {
        ListNode[] templateOptionStrings = new ListNode[] {
                new ListNode(artist + " - " + album + " - " + track + " - " + title, 
                        artistVar + " - " + albumVar + " - " + trackVar + " - " + titleVar),
                new ListNode(artist + " - " + title, artistVar + " - " + titleVar),
                new ListNode(track + " - " + artist + " - " + title,
                        trackVar + " - " + artistVar + " - " + titleVar),
                new ListNode(artist + " - " + title + " - " + track,
                        artistVar + " - " + titleVar + " - " + trackVar)
        };
        return templateOptionStrings;
    }
    
    /**
     * Predefined templates for sub directory naming conventions. Contains a list of 
     * nodes where each node contains a human readable form and a parsable form
     *      No subfolder, ""
     *      album\, <album>
     *      artist\, <artist>
     *      artist\album, <artist>\<album> (default)
     * 
     * @return an array of ListNodes of sub directory templates
     */
    private ListNode[] getSubDirectoryTemplatesArray() {
        ListNode[] templateOptionStrings = new ListNode[] {
                new ListNode(artist + File.separatorChar + album + File.separatorChar, artistVar + File.separatorChar + albumVar),
                new ListNode(album + File.separatorChar, albumVar),
                new ListNode(artist + File.separatorChar, artistVar),
                new ListNode(I18n.tr("No Subfolder"), "")
        };
        return templateOptionStrings;
    }
    
    /**
     * Performs a subString search to find what item in the combobox was previously saved
     * and initializes that index
     *  
     * @param box combobox to set the initial index on
     * @param boxList list of value displayed in the combobox
     * @param subString String to search list with
     */
    private static void setJComboBox(JComboBox box, ListNode[] boxList, String subString) {
        int index = 0;
      
        if( subString != null) {
            for(ListNode node : boxList) {
                if(subString.equals(node.getTemplateText()))
                    break;
                index += 1;
            }
        }
        
        //if something went wrong, reset to base case
        if( index >= boxList.length )
            index = 0;
        
        box.setSelectedIndex(index);
    }
    
    /**
     *  Holder for items in a comboBox. The displayed value of the 
     *  combobox and the template value are different from each other
     *  to make the text more user friendly
     */
    class ListNode {
        
        /**
         * Value to display in the combo box, human readable
         */
        private final String displayText;
        
        /**
         * Value to display in the template, DFA parsable
         */
        private final String templateText;
        
        public ListNode(String displayText, String displayTemplateText){
            this.displayText = displayText;
            this.templateText = displayTemplateText;
        }
        
        public String getDisplayText(){
            return displayText;
        }
        
        public String getTemplateText() {
            return templateText;
        }
        
        @Override
        public String toString(){
            return displayText;
        }
    }
}
