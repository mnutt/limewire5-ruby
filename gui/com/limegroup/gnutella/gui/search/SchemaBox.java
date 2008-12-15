package com.limegroup.gnutella.gui.search;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.UIManager;

import org.limewire.util.MediaType;

import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.ImageManipulator;
import com.limegroup.gnutella.gui.themes.ThemeFileHandler;
import com.limegroup.gnutella.xml.LimeXMLSchema;

/**
 * A group of radio buttons for each schema.
 */
final class SchemaBox extends JPanel {
    
    /**
     * String for 'Select Type'
     */
    private static final String SELECT_TYPE =
        I18n.tr("Select Search Type:");

    /**
     * The property that the media type is stored in.
     */
    private static final String MEDIA = "NAMED_MEDIA_TYPE";
    
    /**
     * The property to store the selected icon in.
     */
    private static final String SELECTED = "SELECTED_ICON";
    
    /**
     * The property to store the unselected icon in.
     */
    private static final String DESELECTED = "DESELECTED_ICON";
    
    /**
     * The panel containing the schemas.
     */
    private final JPanel SCHEMAS = new JPanel();

    /**
     * The button group that holds the buttons together.
     */
    private final ButtonGroup GROUP = new ButtonGroup();
    
    /**
     * The listener for changing the highlighting of buttons.
     */
    private final ItemListener HIGHLIGHTER = new Highlighter();
    
    /**
     * The clicker forwarder.
     */
    private final MouseListener CLICK_FORWARDER = new Clicker();
    
    /**
     * The ditherer for highlighted buttons.
     */
    private final Ditherer DITHERER =
            new Ditherer(20,
                        ThemeFileHandler.FILTER_TITLE_TOP_COLOR.getValue(), 
                        ThemeFileHandler.FILTER_TITLE_COLOR.getValue()
                        );

    /**
     * Constructs the SchemaBox.
     */
    SchemaBox() {
        List<NamedMediaType> allSchemas = NamedMediaType.getAllNamedMediaTypes();
        int cols, rows;
        cols = 2;
        rows = (int)Math.ceil(allSchemas.size() / 2.0);
        SCHEMAS.setBackground(ThemeFileHandler.SEARCH_GRID_COLOR.getValue());
        SCHEMAS.setLayout(new GridLayout(rows, cols, 1, 1));
        SCHEMAS.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        addSchemas(allSchemas);
        
        
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel(SELECT_TYPE));
        add(panel);
        
        setLayout( new BoxLayout(this, BoxLayout.Y_AXIS));
        JPanel p = new BoxPanel(BoxPanel.X_AXIS);
        p.add(SCHEMAS);
        p.add(Box.createHorizontalStrut(1));
        add(p);
    }
    
    /**
     * Adds the specified ActionListener to all possible buttons.
     */
    public void addSelectionListener(ActionListener listener) {
        Enumeration<AbstractButton> elements = GROUP.getElements();
        for(; elements.hasMoreElements(); ) {
            AbstractButton button = elements.nextElement();
            button.addActionListener(listener);
        }
    }
    
    /**
     * Returns the selected icon, or null if the selection has no icon.
     */
    public Icon getSelectedIcon() {
        NamedMediaType nmt = getSelectedMedia();
        if(nmt == null)
            return null;
            
        return nmt.getIcon();
    }        
    
    /**
     * Returns the selected schema, or null if the selected item has no schema.
     */
    public LimeXMLSchema getSelectedSchema() {
        NamedMediaType nmt = getSelectedMedia();
        if(nmt == null)
            return null;
            
        return nmt.getSchema();
    }
    
    /**
     * Returns the description of the selected item.
     */
    public String getSelectedItem() {
        NamedMediaType nmt = getSelectedMedia();
        if(nmt == null)
            return null;
        return nmt.getName();
    }
    
    /**
     * Returns the selected item's media type.
     */
    public MediaType getSelectedMediaType() {
        NamedMediaType nmt = getSelectedMedia();
        if(nmt == null)
            return null;
        return nmt.getMediaType();
    }
    
    /**
     * Adds the given schemas as radio buttons.
     */
    private void addSchemas(List<? extends NamedMediaType> schemas) {
        //We first add specific ones in a certain order.
        //After that, leave it to random chance.
        NamedMediaType current;
        
        // First add 'Any Type'
        current = NamedMediaType.getFromDescription(MediaType.SCHEMA_ANY_TYPE);
        schemas.remove(current);
        addMediaType(current);
        
        // Then add 'Audio'
        current = NamedMediaType.getFromDescription(MediaType.SCHEMA_AUDIO);
        schemas.remove(current);
        addMediaType(current, I18n.tr("Search For Audio Files, Including mp3, wav, ogg, and More"));
        
        // Then add 'Images'
        current = NamedMediaType.getFromDescription(MediaType.SCHEMA_IMAGES);
        schemas.remove(current);
        addMediaType(current, I18n.tr("Search For Image Files, Including jpg, gif, png and More"));
        
        // Then add 'Video'
        current = NamedMediaType.getFromDescription(MediaType.SCHEMA_VIDEO);
        schemas.remove(current);
        addMediaType(current, I18n.tr("Search For Video Files, Including avi, mpg, wmv, and More"));
        
        // Then add 'Documents'
        current = NamedMediaType.getFromDescription(MediaType.SCHEMA_DOCUMENTS);
        schemas.remove(current);
        addMediaType(current, I18n.tr("Search for Document Files, Including html, txt, pdf, and More"));
        
        // Then add 'Programs'
        current = NamedMediaType.getFromDescription(MediaType.SCHEMA_PROGRAMS);
        schemas.remove(current);
        addMediaType(current, I18n.tr("Search for Program Files, Including exe, zip, gz, and More"));
        
        // Then add anything that was left.
        for(NamedMediaType nmt : schemas)
            addMediaType(nmt);
    }    
    
	private void addMediaType(NamedMediaType type) {
		addMediaType(type, null);
	}
	
    /**
     * Adds the given NamedMediaType.
     *
     * Marks the 'Any Type' as selected.
     */
    private void addMediaType(NamedMediaType type, String toolTip) {
        Icon icon = type.getIcon();
        Icon disabledIcon = null;
        Icon rolloverIcon = null;
        AbstractButton button = new JRadioButton(type.getName());
        button.putClientProperty(MEDIA, type);
        button.putClientProperty(SELECTED, icon);
        if(icon != null) {
            disabledIcon = ImageManipulator.darken(icon);
            rolloverIcon = ImageManipulator.brighten(icon);
        }
        button.putClientProperty(DESELECTED, disabledIcon);
        button.setIcon(disabledIcon);
        button.setRolloverIcon(rolloverIcon);
        button.addItemListener(HIGHLIGHTER);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setOpaque(false);
        button.addMouseListener(CLICK_FORWARDER);
        button.setPreferredSize(new Dimension(100, 22));
		if (toolTip != null) {
			button.setToolTipText(toolTip);
		}
        GROUP.add(button);
        
        DitherPanel panel = new DitherPanel(DITHERER);
        panel.setDithering(false);
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 7, 1));
        panel.add(button);
        panel.addMouseListener(CLICK_FORWARDER);
        panel.setBackground(UIManager.getColor(
                                         "TabbedPane.background"));
        SCHEMAS.add(panel);
        
        if(type.getMediaType() == MediaType.getAnyTypeMediaType())
            button.setSelected(true);
        else
            button.setSelected(false);
    }
    
    /**
     * Iterates through all the elements in the group
     * and returns the media type of the selected button.
     */
	NamedMediaType getSelectedMedia() {
        Enumeration<AbstractButton> elements = GROUP.getElements();
        for(; elements.hasMoreElements(); ) {
            AbstractButton button = elements.nextElement();
            if(button.isSelected())
                return (NamedMediaType)button.getClientProperty(MEDIA);
        }
        return null;
    }
    
    /**
     * Listener for ItemEvent, so that the buttons can be highlighted or not
     * when selected (or not).
     */
    private static class Highlighter implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            AbstractButton button = (AbstractButton)e.getSource();
            DitherPanel parent = (DitherPanel)button.getParent();
            if(e.getStateChange() == ItemEvent.SELECTED) {
                button.setIcon((Icon)button.getClientProperty(SELECTED));
                parent.setDithering(true);
                parent.setBackground(ThemeFileHandler.FILTER_TITLE_COLOR.getValue());
            } else {
                button.setIcon((Icon)button.getClientProperty(DESELECTED));
                parent.setDithering(false);
                parent.setBackground(UIManager.getColor("TabbedPane.background"));
            }
        }
    }
    
    private static void setIfNotNull(JComponent c, String col) {
        Color color = UIManager.getColor(col);
        if(color != null)
            c.setBackground(color);
    }
    
    /**
     * Forwards click events from a panel to the panel's component.
     */
    private static class Clicker implements MouseListener {
        public void mouseEntered(MouseEvent e) {
            JComponent c = (JComponent)e.getSource();
            AbstractButton b;
            if(c instanceof AbstractButton) {
                b = (AbstractButton)c;
                c = (JComponent)c.getParent();
            } else {
                b = (AbstractButton)c.getComponent(0);
            }
            if(!b.isSelected())
                setIfNotNull(c, "TabbedPane.selected");
        }
        public void mouseExited(MouseEvent e) {
            JComponent c = (JComponent)e.getSource();
            AbstractButton b;
            if(c instanceof AbstractButton) {
                b = (AbstractButton)c;
                c = (JComponent)c.getParent();
            } else {
                b = (AbstractButton)c.getComponent(0);
            }
            if(!b.isSelected())
                setIfNotNull(c, "TabbedPane.background");
        }
        public void mouseClicked(MouseEvent e) {
            JComponent c = (JComponent)e.getSource();
            if(!(c instanceof AbstractButton)) {
                AbstractButton b = (AbstractButton)c.getComponent(0);
                b.doClick();
            }
        }
        public void mousePressed(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}
    }
} 
