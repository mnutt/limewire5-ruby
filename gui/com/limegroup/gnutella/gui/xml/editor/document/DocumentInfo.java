package com.limegroup.gnutella.gui.xml.editor.document;

import javax.swing.JLabel;

import com.limegroup.gnutella.gui.xml.editor.AbstractMetaInfoPanel;
import com.limegroup.gnutella.gui.xml.editor.DetailsPanel;
import com.limegroup.gnutella.gui.xml.editor.IconPanel;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLSchema;

/**
 *  Displays basic Document info about the file. In addition to file size and 
 *  creation date, displays any Document XML data that may exist in the LimeXMLDocument
 */
public class DocumentInfo extends AbstractMetaInfoPanel {
    public DocumentInfo(FileDesc[] fds, LimeXMLSchema schema, LimeXMLDocument document) {
        super(fds, schema, document);
    }
    
    @Override
    protected void setValues() {
        ((DetailsPanel)details).initWithFileDesc(fds[0], schema.getSchemaURI());
        
        ((IconPanel)iconPanel).initWithFileDesc(fds[0]);
        fileLocationTextArea.setFont(new JLabel().getFont());
        fileLocationTextArea.setText(fds[0].getFile().toString());
        
        String fileName = fds[0].getFileName();
        
        if (fileName != null) {
            firstLineLabel.setText(fileName);
        } else {
            firstLineLabel.setText("");
        }
    } 
}
