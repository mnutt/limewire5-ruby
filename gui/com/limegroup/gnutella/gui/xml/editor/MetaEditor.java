package com.limegroup.gnutella.gui.xml.editor;


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.limewire.util.MediaType;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.xml.editor.application.ApplicationTabbedPane;
import com.limegroup.gnutella.gui.xml.editor.audio.AudioTabbedPane;
import com.limegroup.gnutella.gui.xml.editor.document.DocumentTabbedPane;
import com.limegroup.gnutella.gui.xml.editor.image.ImageTabbedPane;
import com.limegroup.gnutella.gui.xml.editor.video.VideoTabbedPane;
import com.limegroup.gnutella.library.FileDesc;


/**
 * Creates a dialog that describes the current file and any meta data that may be
 * attached. A tab is added that allows the meta-data to be edited. This meta-data
 * is attached to the LimeXMLDocument and if applicable, is also changed in the 
 * meta-data such as ID3 tags for mp3s. 
 */
public class MetaEditor extends JDialog {
    
    protected final FileDesc[] fds;
    private final String fileName;
    
    /**
     * Closes dialog without saving any changes
     */
    private JButton cancelButton;
    
    /**
     * Closes dialog, writes any changes to disk
     */
    protected JButton okButton;
    
    /**
     * Tabbed pane containing the info/editor panels for this file type
     */
    protected MetaEditorTabbedPane tabbedPane;
    
    /**
     * Panel that holds the buttons
     */
    protected JPanel southPanel;
    
    
    public MetaEditor(Frame parent, FileDesc[] fds, String fileName) {
        super(parent, true);
        
        this.fds = fds;
        this.fileName = fileName;
        
        initComponents();
        
        // The Aqua L&F draws an ugly focus indicator which disappears
        // after a while. This code hides it right from the start!
        if (OSUtils.isAnyMac()) {
            tabbedPane.setFocusable(false);
        }
        
            setTitle(fds[0].getFile().getName());
            getRootPane().setDefaultButton(okButton);
        
        okButton.updateUI();
        cancelButton.updateUI();
        
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        GUIUtils.addHideAction((JComponent)getContentPane());
        pack();
    }
    
    private void initComponents() {
        initCenterPanel();
        initSouthPanel();
        
        setLocationRelativeTo(this);
        setResizable(false);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                disposeWindow();
            }
        });
        pack();
    }
    
    protected void initCenterPanel(){
        tabbedPane = createTabbedPane();
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
    }
    
    /**
     * Creates a tabbed info/editor pane based on the file type
     */
    protected MetaEditorTabbedPane createTabbedPane() {
        MetaEditorTabbedPane tabbedPane = null;
        if (MediaType.getAudioMediaType().matches(fileName)) {
        	tabbedPane = new AudioTabbedPane(fds);
        } else if (MediaType.getVideoMediaType().matches((fileName))) {
        	tabbedPane = new VideoTabbedPane(fds);
        } else if (MediaType.getProgramMediaType().matches(fileName)) {
        	tabbedPane = new ApplicationTabbedPane(fds);
        } else if (MediaType.getImageMediaType().matches(fileName)) {
        	tabbedPane = new ImageTabbedPane(fds);
        } else if (MediaType.getDocumentMediaType().matches(fileName)) {
        	tabbedPane = new DocumentTabbedPane(fds);
        }
        return tabbedPane;
    }
    
    /**
     * Creates the south panel or button panel for the dialog. This contains the ok/cancel 
     * buttons for disposing of the dialog
     */
    protected void initSouthPanel(){
        southPanel = new JPanel( new BorderLayout());

        JPanel buttonPanel = new JPanel( new FlowLayout(FlowLayout.TRAILING));

        cancelButton = new JButton();
        cancelButton.setText(I18n.tr("Cancel"));
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                disposeWindow();
            }
        });

        okButton = new JButton();
        okButton.setText(I18n.tr("OK"));
        okButton.setPreferredSize( cancelButton.getPreferredSize() );
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                saveAndDisposeWindow();
            }
        });

        JButton firstBtn, secondBtn;
        if (OSUtils.isAnyMac()) {
            firstBtn = cancelButton;
            secondBtn = okButton;
        } else {
            firstBtn = okButton;
            secondBtn = cancelButton;
        }
        
        buttonPanel.add(firstBtn);
        buttonPanel.add(secondBtn);

        southPanel.add(buttonPanel, BorderLayout.CENTER);
    
        getContentPane().add(southPanel, BorderLayout.SOUTH);
    }
    
        
        
    /**
     * Disposes of the dialog without saving any changes
     */
    protected void disposeWindow() {
        setVisible(false);
        dispose();
    	}
    	
    /**
     * If changes were made, save changes then close window
     */
    protected void saveAndDisposeWindow() { 
    	if (tabbedPane.hasDataChanged()) {
    	MetaDataSaver saver = new MetaDataSaver(tabbedPane.getFileDesc(), 
    			tabbedPane.getSchema(), tabbedPane.getInput());
        saver.saveMetaData();
    	}
        disposeWindow();
    }
}
