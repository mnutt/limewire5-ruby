package com.limegroup.gnutella.gui.xml.editor.image;

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
 *  Editor for modifying XML data about image files
 */
public class ImageEditor extends MetaEditorPanel {
    
    private JComboBox licenseTypeComboBox;
    
    private JLabel licenseLabel;
    private LimeTextField licenseTextField;
    
    public ImageEditor(FileDesc[] fds, LimeXMLSchema schema,
            LimeXMLDocument document) {
        super(fds, schema, document);
        super.setName(MetaEditorUtil.getStringResource(LimeXMLNames.IMAGE));
        
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
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.IMAGE_TITLE), 
                1, 0, gbc, Location.LEFT);
        createTextField(24, 1, 1, 1, gbc, Location.LEFT, LimeXMLNames.IMAGE_TITLE);

        
        // Artist (right side)
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.IMAGE_ARTIST), 
                3, 0, gbc, Location.RIGHT);   
        createTextField(24, 3, 1, 1, gbc, Location.RIGHT, LimeXMLNames.IMAGE_ARTIST);

        
        // Description
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.IMAGE_DESCRIPTION), 
                1, 2, gbc, Location.LEFT);      
        createTextField(24, 1, 3, 3, gbc, Location.LEFT, LimeXMLNames.IMAGE_DESCRIPTION);
        
        
        // License Type (right side)
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.IMAGE_LICENSETYPE), 
                3, 2, gbc, Location.RIGHT);
        licenseTypeComboBox = createComboBox(3, 3, gbc, Location.RIGHT, LimeXMLNames.IMAGE_LICENSETYPE);
        
        
        // License
        licenseLabel = createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.IMAGE_LICENSE), 
                1, 4, gbc, Location.LEFT);       
        licenseTextField = createTextField(24, 1, 5, 1, gbc, Location.LEFT, LimeXMLNames.IMAGE_LICENSE);
        licenseTextField.setEnabled(false);
    }
}
