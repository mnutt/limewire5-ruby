package com.limegroup.gnutella.gui.xml.editor.audio;

import javax.swing.JLabel;

import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.gui.xml.editor.AbstractMetaInfoPanel;
import com.limegroup.gnutella.gui.xml.editor.DetailsPanel;
import com.limegroup.gnutella.gui.xml.editor.IconPanel;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.LimeXMLSchema;

/**
 *  Displays basic Audio info about the file. In addition to file size and creation date, 
 *  displays any Audio meta data that may exist in the LimeXMLDocument
 */
public class AudioInfo extends AbstractMetaInfoPanel {
    public AudioInfo(FileDesc[] fds, LimeXMLSchema schema, LimeXMLDocument doc) {
        super(fds, schema, doc);
    }
        
    @Override
    protected void setValues(){
        ((DetailsPanel)details).initWithFileDesc(fds[0], schema.getSchemaURI());
        
        ((IconPanel)iconPanel).initWithFileDesc(fds[0]);
        fileLocationTextArea.setFont(new JLabel().getFont());
        fileLocationTextArea.setText(fds[0].getFile().toString());
        
        String title = getValue(LimeXMLNames.AUDIO_TITLE);
        String artist = getValue(LimeXMLNames.AUDIO_ARTIST);
        String album = getValue(LimeXMLNames.AUDIO_ALBUM);
        
        // if title information exists in meta-data display that, else display filename
        if (title != null) {
            String length = getValue(LimeXMLNames.AUDIO_SECONDS);
            if (length != null) {
                try {
                    title += " (" + CommonUtils.seconds2time(Integer.parseInt(length)) + ")";
                } catch (NumberFormatException err) {
                }
            }
            
            firstLineLabel.setText(title);
        } else {
            title = fds[0].getFileName();
            if (title != null )
                firstLineLabel.setText(title);
            else
                firstLineLabel.setText("");
        }
        
        if (artist != null) {
            secondLineLabel.setText(artist);
        } else {
            secondLineLabel.setText("");
        }
        
        
        if (album != null) {
            thirdLineLabel.setText(album);
        } else {
            thirdLineLabel.setText("");
        }
    }
}
