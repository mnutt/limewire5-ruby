package com.limegroup.gnutella.gui.xml.editor.audio;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.limegroup.gnutella.gui.xml.editor.AutoCompleteComboBoxEditor;
import com.limegroup.gnutella.gui.xml.editor.MetaEditorPanel;
import com.limegroup.gnutella.gui.xml.editor.MetaEditorUtil;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.LimeXMLSchema;

/**
 * Editor for changing LimeXML information and Meta-tags about
 * audio files
 */
class AudioEditor extends MetaEditorPanel {
    
    private JScrollPane commentsScrollPane;
    private JTextArea commentsTextArea;
    
    private JComboBox genreComboBox;

    private JComboBox typeComboBox;
        
        
    public AudioEditor(FileDesc[] fds, LimeXMLSchema schema, LimeXMLDocument doc) {
        super(fds, schema, doc);
        super.setName(MetaEditorUtil.getStringResource(LimeXMLNames.AUDIO));
        
        init();
        
        AutoCompleteComboBoxEditor editor = new AutoCompleteComboBoxEditor();
        genreComboBox.setEditor(editor);
        initTextFields(); // sets the combo box model
        editor.setModel(genreComboBox.getModel()); // use the model for auto complete
    }
    
    private void initTextFields() { 
        addComponent(LimeXMLNames.AUDIO_COMMENTS, commentsTextArea);
        
        super.initFields();
    }
    
    private void init() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        
        setLayout(new GridBagLayout());
        setOpaque(false);
        
        //Title
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.AUDIO_TITLE), 
                1, 0, gbc, Location.LEFT);
        createTextField(40, 1, 1, 1, gbc, Location.LEFT, LimeXMLNames.AUDIO_TITLE);
        
        
        //Year
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.AUDIO_YEAR), 
                5, 0, gbc, Location.RIGHT);
        createTextField(12, 5, 1, 1, gbc, Location.RIGHT, LimeXMLNames.AUDIO_YEAR);
        
        
        //Artist
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.AUDIO_ARTIST), 
                1, 2, gbc, Location.LEFT);
        createTextField(40, 1, 3, 4, gbc, Location.LEFT, LimeXMLNames.AUDIO_ARTIST);
        
        
        //Track
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.AUDIO_TRACK), 
                5, 2, gbc, Location.RIGHT);
        createTextField(12, 5, 3, 1, gbc, Location.RIGHT, LimeXMLNames.AUDIO_TRACK );
        
        
        //Album
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.AUDIO_ALBUM), 
                1, 5, gbc, Location.LEFT);
        createTextField(40, 1, 6, 4, gbc, Location.LEFT, LimeXMLNames.AUDIO_ALBUM);
        
        
        //Genre
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.AUDIO_GENRE),
                5, 5, gbc, Location.RIGHT);

        genreComboBox = createComboBox(5, 6, gbc, Location.RIGHT, LimeXMLNames.AUDIO_GENRE);
        genreComboBox.setEditable(true);
        genreComboBox.setModel(new DefaultComboBoxModel(new String[] { "", "GENRE1", "GENRE2", "GENRE3" }));
        genreComboBox.setPreferredSize(new Dimension(100,23));
                
        
        //Language
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.AUDIO_LANGUAGE), 
                1, 7, gbc, Location.LEFT);
        createTextField(40, 1, 8, 4, gbc, Location.LEFT, LimeXMLNames.AUDIO_LANGUAGE);
    
    
        //Type
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.AUDIO_TYPE), 
                5, 7, gbc, Location.RIGHT);
    
        typeComboBox = createComboBox(5, 8, gbc, Location.RIGHT, LimeXMLNames.AUDIO_TYPE);
        typeComboBox.setModel(new DefaultComboBoxModel(new String[] { "", "TYPE1", "TYPE2", "TYPE3" }));
        typeComboBox.setPreferredSize( new Dimension(100,23));


        //Comment
        createLabelAndAdd(MetaEditorUtil.getStringResource(LimeXMLNames.AUDIO_COMMENTS), 
                1, 9, gbc, Location.LEFT);

        commentsTextArea = new JTextArea();
        commentsTextArea.setLineWrap(true);
        commentsTextArea.setWrapStyleWord(true);
        commentsScrollPane = new JScrollPane();
        commentsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        commentsScrollPane.setViewportView(commentsTextArea);
        commentsScrollPane.setPreferredSize(new Dimension(42, 100));
        gbc.insets = new Insets(0,3,2,2);
        gbc.gridx = 1;
        gbc.gridy = 10;
        gbc.gridwidth = 6;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        add(commentsScrollPane, gbc);
    }

}
