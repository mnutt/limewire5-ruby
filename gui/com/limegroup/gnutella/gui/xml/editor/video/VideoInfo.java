package com.limegroup.gnutella.gui.xml.editor.video;

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
 *  Displays basic Video info about the file. In addition to file size and 
 *  creation date, displays any Video meta data that may exist in the LimeXMLDocument
 */
public class VideoInfo extends AbstractMetaInfoPanel {
    public VideoInfo(FileDesc[] fds, LimeXMLSchema schema, LimeXMLDocument document) {
        super(fds, schema, document);
    }

    @Override
    protected void setValues() {
        ((DetailsPanel)details).initWithFileDesc(fds[0], schema.getSchemaURI());
        
        ((IconPanel)iconPanel).initWithFileDesc(fds[0]);
        fileLocationTextArea.setFont(new JLabel().getFont());
        fileLocationTextArea.setText(fds[0].getFile().toString());
        
        String title = getValue(LimeXMLNames.VIDEO_TITLE);
        String artist = getValue(LimeXMLNames.VIDEO_DIRECTOR);
        String album = getValue(LimeXMLNames.VIDEO_STUDIO);
        
        // if title information exists in video metadata display that, else display file name
        if (title != null) {
            String length = getValue(LimeXMLNames.VIDEO_LENGTH);
            if (length != null) {
                try {
                    title += " (" + CommonUtils.seconds2time(Integer.parseInt(length)) + ")";
                } catch (NumberFormatException err) {
                }
            }
            firstLineLabel.setText(title);
        } else {
            title = fds[0].getFileName();
            if( title != null ) 
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