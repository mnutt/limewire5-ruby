package com.limegroup.gnutella.gui.xml.editor.application;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import com.limegroup.gnutella.gui.LimeTextField;
import com.limegroup.gnutella.gui.xml.ComboBoxValue;
import com.limegroup.gnutella.gui.xml.editor.MetaEditorPanel;
import com.limegroup.gnutella.gui.xml.editor.MetaEditorUtil;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.licenses.CCConstants;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.LimeXMLSchema;

/**
 * Editor for changing LimeXML information about applications
 */
public class ApplicationEditor extends MetaEditorPanel{
    
    private JComboBox licenseTypeComboBox;
    
    private JLabel licenseLabel;
    private LimeTextField licenseTextField;
    
    public ApplicationEditor(FileDesc[] fds, LimeXMLSchema schema, LimeXMLDocument doc) {
        super(fds,schema,doc);
        setName(MetaEditorUtil.getStringResource(LimeXMLNames.APPLICATION));
        
        init();
        initFields();
        
        // show license only if the file has a Creative Commons license
        ComboBoxValue val = (ComboBoxValue)licenseTypeComboBox.getSelectedItem();
        if(val==null || !(val.equals(new ComboBoxValue(CCConstants.CC_URI_PREFIX)))){
            licenseTextField.setVisible(false);
            licenseLabel.setVisible(false);
        }
    }
    
    private void init() {      
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        
        // Title       
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.APPLICATION_NAME), 
                1, 0, gbc, Location.LEFT);       
        createTextField(40, 1, 1, 3, gbc, Location.LEFT, LimeXMLNames.APPLICATION_NAME);

        
        // Publisher
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.APPLICATION_PUBLISHER), 
                1, 2, gbc, Location.LEFT);       
        createTextField(24, 1, 3, 1, gbc, Location.LEFT, LimeXMLNames.APPLICATION_PUBLISHER);

        
        // Platform
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.APPLICATION_PLATFORM),
                3, 2, gbc, Location.RIGHT);
        createComboBox(3, 3, gbc, Location.RIGHT, LimeXMLNames.APPLICATION_PLATFORM);
        
        
        // License Type
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.APPLICATION_LICENSETYPE), 
                1, 6, gbc, Location.LEFT);
        licenseTypeComboBox = createComboBox(1, 7, gbc, Location.LEFT, LimeXMLNames.APPLICATION_LICENSETYPE);
        
        
        // License
        licenseLabel = createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.APPLICATION_LICENSE), 
                1, 4, gbc, Location.LEFT);
        licenseTextField = createTextField(24, 1, 5, 1, gbc, Location.LEFT, LimeXMLNames.APPLICATION_LICENSE);
        licenseTextField.setEnabled(false);
    }
}
