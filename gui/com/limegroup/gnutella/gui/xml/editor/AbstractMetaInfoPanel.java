
package com.limegroup.gnutella.gui.xml.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLSchema;

/**
 *   Generic panel for laying out and display general information about a file and any information
 *   that may be contained in its attached LimeXMLDoc if it exists
 */
public abstract class AbstractMetaInfoPanel extends MetaEditorPanel {

    /**
     * Contains the basic information displayed at the top of the information panel
     * If XML docs contain info will display image, title, artist/author, album
     */
    private JPanel topPanel;
    
    /**
     * Container for an image if one exists
     */
    protected JPanel iconPanel;
    
    /**
     * Generic labels that get displayed atop the info panel. Each schema type
     * can choose what relevant information can be displayed depending on the
     * file type
     */
    protected JLabel firstLineLabel;
    protected JLabel secondLineLabel;
    protected JLabel thirdLineLabel;
       
    /**
     * Displays more detailed information about the file such as file size, last modifications, etc.
     */
    protected JPanel details;
    
    /**
     * Container for details panel
     */
    private JPanel centerPanel;

    
    /**
     * Displays the entire path of the current selected file
     */
    private JPanel fileLocationPanel;
    
    /**
     * Displays the actual file location text
     */
    protected JTextArea fileLocationTextArea;

    
    public AbstractMetaInfoPanel(FileDesc[] fds, LimeXMLSchema schema, LimeXMLDocument document) {
        super(fds, schema, document);
        
        super.setName(I18n.tr("Info"));
        
        initComponents();
        
        setValues();
    }
    
    /**
     * Sets the label names and their values depending on the type of info panel
     */
    protected abstract void setValues();

    /**
     * Creates the components for the view. Does not handle initialization of
     * any values in the components
     */
    protected void initComponents() {   
        setLayout(new BorderLayout());
        setOpaque(false);
        
        createGeneralPanel();
        createDetailsPanel();
        createFileLocationPanel();
        
        add(topPanel, BorderLayout.NORTH); 
        add(centerPanel, BorderLayout.CENTER);
        add(fileLocationPanel, BorderLayout.SOUTH);
    }

    private void createGeneralPanel() {
        if( topPanel == null ) {
            topPanel = new JPanel(new BorderLayout());
            topPanel.setOpaque(false);
    
            JPanel panel;
                
            panel = new JPanel(new GridBagLayout());
            panel.setOpaque(false);
            
            iconPanel = new IconPanel();
            firstLineLabel = new JLabel();
            secondLineLabel = new JLabel();
            thirdLineLabel = new JLabel();
            
            iconPanel.setBackground(new Color(255, 255, 255));
            iconPanel.setBorder(new LineBorder(new Color(153, 153, 153)));
            iconPanel.setPreferredSize(new Dimension(48, 48));
            
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridheight = 3;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
            panel.add(iconPanel, gridBagConstraints);
            
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
            panel.add(firstLineLabel, gridBagConstraints);
    
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            panel.add(secondLineLabel, gridBagConstraints);
    
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
            panel.add(thirdLineLabel, gridBagConstraints);
    
    
            topPanel.add(new JSeparator(), BorderLayout.SOUTH);
            topPanel.add(panel, BorderLayout.WEST);
        }
    }

    private void createDetailsPanel() {
        if( centerPanel == null ) {
            centerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            centerPanel.setOpaque(false);
            
            details = new DetailsPanel(new GridLayout(8, 2, 3, 0));
            details.setOpaque(false);
            
            centerPanel.add(details);
        }
    }

    /**
     * Creates the panel to display the file location of the selected file
     */
    private void createFileLocationPanel() {
        if( fileLocationPanel == null ) {
            fileLocationPanel = new JPanel(new BorderLayout());
            fileLocationPanel.setOpaque(false);
    
            fileLocationTextArea = new JTextArea();
            fileLocationTextArea.setEditable(false);
            fileLocationTextArea.setLineWrap(true);
            fileLocationTextArea.setRows(2);
            fileLocationTextArea.setMinimumSize(new Dimension(12, 13));
            fileLocationTextArea.setPreferredSize(new Dimension(12, 32));
            fileLocationTextArea.setOpaque(false);
            
            fileLocationPanel.add(new JSeparator(), BorderLayout.NORTH);
            fileLocationPanel.add(fileLocationTextArea, BorderLayout.SOUTH);
        }
    }

}
