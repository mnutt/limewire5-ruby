package com.limegroup.gnutella.gui.xml.editor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.xml.XMLUtils;
import com.limegroup.gnutella.gui.xml.editor.application.ApplicationTabbedPane;
import com.limegroup.gnutella.gui.xml.editor.audio.AudioTabbedPane;
import com.limegroup.gnutella.gui.xml.editor.document.DocumentTabbedPane;
import com.limegroup.gnutella.gui.xml.editor.image.ImageTabbedPane;
import com.limegroup.gnutella.gui.xml.editor.video.VideoTabbedPane;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;
import com.limegroup.gnutella.xml.SchemaReplyCollectionMapper;

/**
 * An editor for viewing and editing LimeXMLDocs for unknown file types. If the file
 * has no LimeXMLDocument attached, the user will be given the choice of which 
 * schema best describes this file type. Upon selecting a type, a tabbed pane
 * info/editor are created (the same view as all known schemas). Unlike the normal
 * tabbed pane, a back button exists to allow the user to change this schema at any point.
 * 
 * Only one schema/ LimeXMLDocument can exist for any file. If the user selects a type, 
 * then changes the schema, the old XMLDoc will be removed. 
 * 
 * When the user has previously added a LimeXMLDocument to an unknown file type, upon
 * editing it again, it will jump directly to the info/editor tabbed pane like a known
 * file type. The back button still exists to allow the user to change the file type.
 *
 */
public class XmlTypeEditor extends MetaEditor {
 
    /**
     * String keyword for the Selection Panel in card layout
     */
    private final static String SELECTION_PANEL = "SELECTION_PANEL";
    
    /**
     * String keyword for the Tabbed Panel in card layout
     */
    private final static String TABBED_PANEL = "TABBED_PANEL";
    
    
    /**
     * a Panel that uses card layout to hold both the info/editor and the list of
     * schema choices
     */
    private JPanel cardPanel;
    
    /**
     * holds the list of schemas to choose the xml doc type for this file
     */
    private JPanel selectionPanel;
    
    /**
     * Back button to return to the selection list of xml schemas to
     * choose a different schema for this document
     */
    private JButton backButton;

    /**
     * List of all the different schemas used by XMLDocs
     */
    private String[] schemas;
    
    /**
     * List of schemas in human readable form for displaying in the JList
     */
    private JList schemaList;
    
    
    public XmlTypeEditor(Frame parent, FileDesc[] fds, String fileName) {
        super(parent, fds, fileName);
        
        LimeXMLSchema schema = hasXMLSelected();
        if( schema != null ) {
            createAndDisplayTab(schema);
        }
    }
  
    @Override
    protected void initCenterPanel(){
        cardPanel = new JPanel( new CardLayout() );
        cardPanel.add(getSelectionPanel(), SELECTION_PANEL );
               
        this.add(cardPanel, BorderLayout.CENTER);
    }
    
    private JPanel getSelectionPanel(){
        selectionPanel = new JPanel();
        selectionPanel.setBorder( BorderFactory.createEmptyBorder(10,10,10,10));
        selectionPanel.setLayout(new BoxLayout(selectionPanel, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(I18n.tr("Unknown file type. Please choose the type that best describes this file."));
        label.setBorder( BorderFactory.createEmptyBorder(0,0,10,0));
        selectionPanel.add( label);
             
        schemas = getSchemaList();
        
        String[] unusedSchemas = new String[schemas.length];
        for(int j=0; j<unusedSchemas.length; j++){
            unusedSchemas[j] =  XMLUtils.getTitleForSchemaURI(schemas[j]);
        }
        schemaList = new JList(unusedSchemas);
        schemaList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane lowerLeftScroller = new JScrollPane(schemaList);
        selectionPanel.add(lowerLeftScroller);
        
        return selectionPanel;
    }
    
    @Override
    protected void initSouthPanel(){
        super.initSouthPanel();
        
        JPanel panel = new JPanel();
        backButton = new JButton(I18n.tr("Back"));
        backButton.setEnabled(false);
        backButton.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                backAction();
            }
        });
        panel.add(backButton);
        
        
        okButton.setText(I18n.tr("Next"));
        
        southPanel.add(panel, BorderLayout.WEST);
    }
    
    /**
     * The functionality changes depending on the view selected by card layout.
     * When the schema list is displayed, the info/editor tab is created and displayed.
     * When the info/editor tab is displayed, the schema data is saved
     */
    @Override
    protected void saveAndDisposeWindow() {
        // if select xml type, create xml editor
        if( selectionPanel.isVisible() ) {
            int index = schemaList.getSelectedIndex();
            if( index > -1) {
                LimeXMLSchemaRepository rep=GuiCoreMediator.getLimeXMLSchemaRepository();
                LimeXMLSchema schema = rep.getSchema(schemas[index]);
                createAndDisplayTab(schema);
            }
        }//write xml doc
        else {
            // try removing any xml docs that may already exist
            for( int i = 0; i < fds.length; i++) {
                List<LimeXMLDocument> docs = fds[i].getLimeXMLDocuments();
                for( LimeXMLDocument doc : docs ) {
                    if( doc != null ) {
                        String uri = doc.getSchemaURI();
                        SchemaReplyCollectionMapper map=GuiCoreMediator.getSchemaReplyCollectionMapper();
                        LimeXMLReplyCollection collection = map.getReplyCollection(uri);
            
                        assert collection!=null : "Trying to remove data from a non-existent collection";
                        
                        collection.removeDoc(fds[i]);
                    }
                }
            }
            
            MetaDataSaver saver = new MetaDataSaver(tabbedPane.getFileDesc(), 
            tabbedPane.getSchema(), tabbedPane.getInput());
            saver.saveMetaData();
            disposeWindow();
        }
    }
    
    /**
     * Determines if this unknown file type has a LimeXMLDoc associated with it
     * already. If so, return that doc else return null;
     */
    private LimeXMLSchema hasXMLSelected(){
        for(int i = 0; i < fds.length; i++) {
            LimeXMLDocument doc = fds[i].getXMLDocument();
            if( doc != null ) {
                doc.getXMLString();
                return doc.getSchema();
            }
        }
        return null;
    }
    
    /**
     * Returns an array of all the schemas used by LimeXMLDocuments
     */
    private String[] getSchemaList() {
        LimeXMLSchemaRepository rep = GuiCoreMediator.getLimeXMLSchemaRepository();
        return rep.getAvailableSchemaURIs();
    }
       
    private void createAndDisplayTab(LimeXMLSchema schema) {
        createTabs(schema);
        cardPanel.add(tabbedPane, TABBED_PANEL);
      
        CardLayout cl = (CardLayout)(cardPanel.getLayout());
        cl.show(cardPanel, TABBED_PANEL);

        pack();
        backButton.setEnabled(true);
        okButton.setText(I18n.tr("OK"));
    }
    
    /**
     * Creates an TabbedPane based on the user selected schema for this
     * unknown file type.
     */
    private void createTabs(LimeXMLSchema schema) {
        if( schema.getDescription().equals("audio")) { 
            if( !(tabbedPane instanceof AudioTabbedPane) )
                tabbedPane = new AudioTabbedPane(fds);
        } else if( schema.getDescription().equals("video")) {
            if( !(tabbedPane instanceof VideoTabbedPane))
                tabbedPane = new VideoTabbedPane(fds);
        } else if( schema.getDescription().equals("application")) {
            if( !(tabbedPane instanceof ApplicationTabbedPane))
                tabbedPane = new ApplicationTabbedPane(fds);
        } else if( schema.getDescription().equals("document")) {
            if( !(tabbedPane instanceof DocumentTabbedPane))
                tabbedPane = new DocumentTabbedPane(fds);
        } else if( schema.getDescription().equals("image")) {
            if( !(tabbedPane instanceof ImageTabbedPane))
                tabbedPane = new ImageTabbedPane(fds);
        }
    }
    
    /**
     * If the selection list is displayed, return, else
     * change the view to the selection list
     */
    private void backAction() {
        if( selectionPanel.isVisible() )
            return;
        
        CardLayout cl = (CardLayout)(cardPanel.getLayout());
        cl.show(cardPanel, SELECTION_PANEL);
        
        okButton.setText(I18n.tr("Next"));
        backButton.setEnabled(false);
    }
}
