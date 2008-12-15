package com.limegroup.gnutella.gui.options.panes;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import org.limewire.core.settings.SharingSettings;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.SizedTextField;
import com.limegroup.gnutella.gui.GUIUtils.SizePolicy;
import com.limegroup.gnutella.gui.layout.SpringUtilities;
import com.limegroup.gnutella.templates.StoreTemplateProcessor;

/**
 * Creates a panel that displays templates for choosing how LWS files should be 
 * chosen. There are two templates, one for choosing sub directories and one for
 * choosing file names. Each template has predefined templates that can be chosen.
 */
public class StoreFileNamePaneItem extends AbstractPaneItem {

    /**
     * Title of the item in the pane, this name will be displayed in a line border
     */
    private final static String TITLE = I18n.tr("LimeWire Store - File Name");
    
    /**
     * General description of what this item is, and what its used for 
     */
    private final static String LABEL = I18n.tr("You can choose how to name the files purchased from the LimeWire Store and where to save them.");
    
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

    
    /**
     * Labels for templates
     */
    private final String FILE_LABEL = I18n.tr("File Name") + ":";
    
    private final String DIRECTORY_LABEL = I18n.tr("SubFolder") + ":";
    
    
    /**
     * Panel to put all the textfields/combo boxes and labels in
     */
    private JPanel templatePanel;
    
    /**
     * Handle to the <tt>JTextField</tt> that displays the save template.
     */
    private JTextField currentTemplateTextField;
    
    /**
     * Drop down box containing preconfigured templates for file names
     */
    private JComboBox fileTemplates;
    
    /**
     * Drop down box containing preconfigured templates for directories
     */
    private JComboBox subDirectoryTemplates;
    
    /**
     * String for storing the saved file name template
     */
    private String oldFileName;
    
    /**
     * String for storing the saved subdirectory name template
     */
    private String oldSubDirectory;   
    
    
    public StoreFileNamePaneItem() {
        super(TITLE, LABEL);
        
        init();
    }
    
    /**
     * Load the template with the currently saved format
     */
    @Override
    public void initOptions() {       
        // save locally the old values for comparing later
        oldFileName = SharingSettings.getFileNameLWSTemplate();
        oldSubDirectory = SharingSettings.getSubDirectoryLWSTemplate();
        
        //  setup the jcombobox with the saved templates
        setJComboBox(fileTemplates, getFileNameTemplatesArray(), SharingSettings.getFileNameLWSTemplate());
        setJComboBox(subDirectoryTemplates, getSubDirectoryTemplatesArray(), SharingSettings.getSubDirectoryLWSTemplate());
        setPreviewTextField();
    }

    private void init(){
        add(getTemplateTextField());
        add(getTemplatePanel());
        add(getVerticalSeparator());
        
        setPreviewTextField();
    }
    
    /**
     * Creates the text field to display to the user what saved file will look like using
     * the selected templates
     */
    private JTextField getTemplateTextField() {
        if( currentTemplateTextField == null ) {
            currentTemplateTextField = new SizedTextField(25, SizePolicy.RESTRICT_HEIGHT);
            currentTemplateTextField.setEditable(false);
        }
        return currentTemplateTextField;
    }

    private JPanel getTemplatePanel() {
        if( templatePanel == null ) {
            templatePanel = new JPanel( new SpringLayout());
            
            templatePanel.add( new JLabel(DIRECTORY_LABEL));
            templatePanel.add(getPresetDirectoryTemplates());
            
            templatePanel.add( new JLabel(FILE_LABEL));
            templatePanel.add(getPresetFileNameTemplates());
    
            SpringUtilities.makeCompactGrid(templatePanel,
                    2,2,
                    6,6,
                    6,6);
        }
        return templatePanel;
    }

    
    /**
     * Creates a combobox with saved templates. When one of these templates is chosen, it
     * overwrites any template values that have already been defined
     */
    private JComboBox getPresetFileNameTemplates(){
        if( fileTemplates == null ) {
            fileTemplates = new JComboBox(getFileNameTemplatesArray());
            fileTemplates.setMaximumSize(new Dimension(200,25));
            fileTemplates.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setPreviewTextField();
                }
            }); 
        }
        return fileTemplates;
    }
    
    private JComboBox getPresetDirectoryTemplates(){
        if( subDirectoryTemplates == null ) {
            subDirectoryTemplates = new JComboBox(getSubDirectoryTemplatesArray());
            subDirectoryTemplates.setMaximumSize(new Dimension(200,25));
            subDirectoryTemplates.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    setPreviewTextField();
                }
            });
        }
        return subDirectoryTemplates;
    }
    
    /**
	 * Sets the textfield to preview what a file would look like given the current templates. 
	 */
    private void setPreviewTextField() {
        String directory = ((ListNode)subDirectoryTemplates.getSelectedItem()).getTemplateText();

        currentTemplateTextField.setText( ((directory.length() > 0) ? ((ListNode)subDirectoryTemplates.getSelectedItem()).getDisplayText() : "" ) + 
                    ((ListNode)fileTemplates.getSelectedItem()).getDisplayText() + ".mp3");
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
     * Try saving the template. If the template is invalid, throw an error message to the
     * user till they change it to an acceptable template format
     */
    @Override
    public boolean applyOptions() throws IOException {
        // get the english version of the template
        String subDirectoryTemplateText = ((ListNode)subDirectoryTemplates.getSelectedItem()).getTemplateText();
        String fileTemplateText = ((ListNode)fileTemplates.getSelectedItem()).getTemplateText();
        
        // if either of the templates are different, save the new value
        if (!fileTemplateText.equals(oldFileName) )
        	SharingSettings.setFileNameLWSTemplate(fileTemplateText);
                    
        if (!subDirectoryTemplateText.equals(oldSubDirectory) ) 
            SharingSettings.setSubdirectoryLWSTemplate(subDirectoryTemplateText);

        return false;
    }


 
    /**
     * Returns true if the template has changed since the last save
     */
    public boolean isDirty() {
        return !SharingSettings.getFileNameLWSTemplate().equals(oldFileName) ||
                !SharingSettings.getSubDirectoryLWSTemplate().equals(oldSubDirectory);
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
