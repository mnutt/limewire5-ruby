package com.limegroup.gnutella.gui.xml.editor.video;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.limegroup.gnutella.gui.LimeTextField;
import com.limegroup.gnutella.gui.xml.editor.MetaEditorPanel;
import com.limegroup.gnutella.gui.xml.editor.MetaEditorUtil;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.LimeXMLSchema;

/**
 * Editor for changing LimeXML information and meta-data about
 * video files
 */
public class VideoEditor extends MetaEditorPanel {

    private JScrollPane commentsScrollPane;
    private JTextArea commentsTextArea;
       
    private JLabel licenseLabel;
    private LimeTextField licenseTextField;
    
    public VideoEditor(FileDesc[] fds, LimeXMLSchema schema, LimeXMLDocument doc) {
        super(fds,schema,doc);
        super.setName(MetaEditorUtil.getStringResource(LimeXMLNames.VIDEO));
        
        init();
        initFields();
        
        // show license only if the file has a Creative Commons license
        if(licenseTextField.getText().equals("")) {
            licenseTextField.setVisible(false);
            licenseLabel.setVisible(false);
        }
    }
    
    @Override
    protected void initFields() {
        addComponent(LimeXMLNames.VIDEO_COMMENTS, commentsTextArea);
        
        super.initFields();
    }
    
    private void init() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        
        // Title
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.VIDEO_TITLE), 
                1, 0, gbc, Location.LEFT);
        createTextField(40, 1, 1, 3, gbc, Location.LEFT, LimeXMLNames.VIDEO_TITLE);

        
        // Director
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.VIDEO_DIRECTOR), 
                1, 2, gbc, Location.LEFT);
        createTextField(24, 1, 3, 1, gbc, Location.LEFT, LimeXMLNames.VIDEO_DIRECTOR);

        
        // Stars
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.VIDEO_STARS), 
                1, 4, gbc, Location.LEFT);
        createTextField(24, 1, 5, 1, gbc, Location.LEFT, LimeXMLNames.VIDEO_STARS);

        
        // Producer
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.VIDEO_PRODUCER), 
                1, 6, gbc, Location.LEFT);
        createTextField(24, 1, 7, 1, gbc, Location.LEFT, LimeXMLNames.VIDEO_PRODUCER);

        
        // Studio
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.VIDEO_STUDIO), 
                1, 8, gbc, Location.LEFT);
        createTextField(24, 1, 9, 1, gbc, Location.LEFT, LimeXMLNames.VIDEO_STUDIO);

        
        // Comments
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.VIDEO_COMMENTS), 
                1, 10, gbc, Location.LEFT);
        
        gbc.insets = new Insets(0,3,2,2);
        gbc.gridx = 1;
        gbc.gridy = 11;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        commentsTextArea = new JTextArea();
        commentsTextArea.setLineWrap(true);
        commentsTextArea.setWrapStyleWord(true);
        commentsScrollPane = new JScrollPane(commentsTextArea);
        commentsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        commentsScrollPane.setPreferredSize(new Dimension(22, 50));
        add(commentsScrollPane,gbc);
        
        // Subtitles
        gbc.fill = GridBagConstraints.NONE;

        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.VIDEO_SUBTITLES), 
                1, 12, gbc, Location.LEFT);
        createTextField(24, 1, 13, 1, gbc, Location.LEFT, LimeXMLNames.VIDEO_SUBTITLES);

        
        // License
        licenseLabel = createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.VIDEO_LICENSE), 
                1, 14, gbc, Location.LEFT);
        licenseTextField = createTextField(24, 1, 15, 1, gbc, Location.LEFT, LimeXMLNames.VIDEO_LICENSE);
        licenseTextField.setEnabled(false);
        
        //right side
        
        //Year
        gbc.fill = GridBagConstraints.HORIZONTAL;
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.VIDEO_YEAR), 
                3, 2, gbc, Location.RIGHT);
        createTextField(6, 3, 3, 1, gbc, Location.RIGHT, LimeXMLNames.VIDEO_YEAR);

        
        // Rating
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.VIDEO_RATING), 
                3, 4, gbc, Location.RIGHT);
        createComboBox(3, 5, gbc, Location.RIGHT, LimeXMLNames.VIDEO_RATING);

        
        // Language
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.VIDEO_LANGUAGE), 
                3, 6, gbc, Location.RIGHT);
        createTextField(6, 3, 7, 1, gbc, Location.RIGHT, LimeXMLNames.VIDEO_LANGUAGE);
        
        
        // Type
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.VIDEO_TYPE), 
                3, 8, gbc, Location.RIGHT);
        createComboBox(3, 9, gbc, Location.RIGHT, LimeXMLNames.VIDEO_TYPE);
    }   
}
