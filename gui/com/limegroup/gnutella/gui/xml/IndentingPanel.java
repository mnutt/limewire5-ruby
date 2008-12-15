package com.limegroup.gnutella.gui.xml;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.text.Document;
import javax.swing.undo.UndoManager;

import org.limewire.util.NameValue;

import com.limegroup.gnutella.gui.ClearableAutoCompleteTextField;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.KeyProcessingTextField;
import com.limegroup.gnutella.gui.search.SearchField;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.SchemaFieldInfo;

/**
 * A panel that displays fields of an XML schema.
 */
public abstract class IndentingPanel extends JPanel implements Scrollable {
    /**
     * The property that contains the canonical key of the field
     * in each JComponent.
     */
    private static final String KEY_PROP = "lime.canonKey";
    
    /**
     * Property for whether or not this is a default entry.
     */
    private static final String DEFAULT_PROP = "lime.defaultField";

    /**
     * The number of fields to show by default.
     */
    private static final int DEFAULT_FIELDS = 5;
    
    /**
     * The first TextField for input, used to quickly set the focus on 
     * the text field when this panel wants focus.
     */
    private KeyProcessingTextField keyProcessingTextField;

    // Constructor
    public IndentingPanel(LimeXMLSchema schema, ActionListener listener, 
                          Document document, UndoManager undoer, 
                          boolean expand, boolean indent, boolean searching) {
        this.setOpaque(false);
        
        int fieldsToShow = expand ? schema.getCanonicalizedFields().size() : DEFAULT_FIELDS;
        List<SchemaFieldInfo> fields = schema.getCanonicalizedFields();
        boolean defaultField = true;
        int added = 0;
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        
        for (int i = 0; i < fields.size(); i++) {
            SchemaFieldInfo infoField = fields.get(i);
            if(infoField.isInvisible())
                continue;
            
            if(!searching && !infoField.isEditable())
                continue;

            if(added == fieldsToShow) {
                addMoreOptions(c);
                defaultField = false;
            }

            JComponent comp = addField(schema, infoField, c, defaultField, searching);
            
            // remember the first KeyProcessingTextField we add.
            if(comp instanceof KeyProcessingTextField) {
                KeyProcessingTextField kptf = (KeyProcessingTextField)comp;
                if(keyProcessingTextField == null) {
                    keyProcessingTextField = kptf;
                    if(document != null)
                        kptf.setDocument(document);
                    if(undoer != null)
                        kptf.setUndoManager(undoer);
                }
                kptf.addActionListener(listener);
            }
                
            added++;
        }
        
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;
        add(Box.createGlue(), c);
        
        setFieldsVisible(false);
    }
    
    /**
     * Adds a 'more options' listener.
     */
    public void addMoreOptionsListener(ActionListener e) {
        for(int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c instanceof JCheckBox)
                ((JCheckBox)c).addActionListener(e);
        }
    }
    
    /**
     * Creates the more options checkbox.
     */
    private void addMoreOptions(GridBagConstraints c) {
        JCheckBox moreOptions = 
            new JCheckBox(I18n.tr("More Search Options"));
        moreOptions.setOpaque(false);        
        moreOptions.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JCheckBox source = (JCheckBox)e.getSource();
                setFieldsVisible(source.isSelected());
                invalidate();
                revalidate();
            }
        });
        c.insets = new Insets(2, 0, 5, 0);
        c.fill = GridBagConstraints.NONE;
        add(moreOptions, c);
    }
    
    /**
     * Gets a new TextField of the correct variety.
     */
    private JComponent getTextField(boolean searching) {
        return searching ? new SearchField(14) : new ClearableAutoCompleteTextField(30);
    }
    
    /**
     * Gets a combo box with the correct entries.
     */
    private JComboBox getOptions(SchemaFieldInfo infoField) {
        List<NameValue<String>> values = infoField.getEnumerationList();
        int d = values.size();
        ComboBoxValue[] vals = new ComboBoxValue[d + 1];
        vals[0] = new ComboBoxValue("", "");
        for (int m = 0; m < d; m++)
            vals[m + 1] = new ComboBoxValue(values.get(m));
        Arrays.sort(vals);
        JComboBox comboBox = new JComboBox(vals);
        comboBox.setOpaque(false);
        return comboBox;
    }
    
    private JComponent addField(LimeXMLSchema schema, SchemaFieldInfo infoField,
                                GridBagConstraints c,
                                boolean defaultField, boolean searching) {
        String currField = infoField.getCanonicalizedFieldName();            
        String fieldName = XMLUtils.getResource(currField);
        JLabel label = new JLabel(fieldName);
        c.insets = new Insets(0, 0, 3, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        add(label, c);
                
        JComponent comp = null;
        switch(infoField.getFieldType()) {
        case SchemaFieldInfo.TEXTFIELD:
            comp = getTextField(searching);
            break;
        case SchemaFieldInfo.OPTIONS:
            comp = getOptions(infoField);
            break;
        default:
            throw new IllegalStateException("bad type: " + infoField.getFieldType() +
                               ", name: " + currField);
        }
            
        c.insets = new Insets(0, 0, 5, 0);
        add(comp, c);
        
        comp.putClientProperty(KEY_PROP, currField);
        if(!defaultField) {
            label.putClientProperty(DEFAULT_PROP, Boolean.FALSE);
            comp.putClientProperty(DEFAULT_PROP, Boolean.FALSE);
        }

        return comp;
    }
    
    /**
     * Iterates through fields and sets ones that aren't default to visible
     * or invisible.
     */
    private void setFieldsVisible(boolean viz) {
        for(int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c instanceof JComponent) {
                Object key = ((JComponent)c).getClientProperty(DEFAULT_PROP);
                if(key == Boolean.FALSE)
                    c.setVisible(viz);
            }
        }
    }
    
    /**
     * Requests focus for the focusRequestor instead of this.
     */
    public void requestFirstFocus() {
        if(keyProcessingTextField != null)
            keyProcessingTextField.requestFocus();
        else
            super.requestFocus();
    }
    
    /**
     * Returns the field which key processes should be forwarded to.
     */
    public KeyProcessingTextField getFirstTextField() {
        return keyProcessingTextField;
    }

    /**
     * Gets the JComponent that has the correct fieldname.
     */
    protected JComponent getField(String fieldName) {
        for(int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c instanceof JComponent) {
                Object key = ((JComponent)c).getClientProperty(KEY_PROP);
                if(key != null && key.equals(fieldName))
                    return (JComponent)c;
            }
        }
        return null;
    }

    /**
     * Clears all fields in this.
     */
    public void clear() {
        for(int i = 0; i < getComponentCount(); i++)
            clearField(getComponent(i));
    }
    
    /**
     * Clears a single component.
     */
    protected void clearField(Component c) {
        if(c instanceof JTextField)
            ((JTextField)c).setText(null);
        else if (c instanceof JComboBox)
            ((JComboBox)c).setSelectedIndex(0);
    }
    
	/**
	 * Implement a Scrollable interface to properly scroll by some
	 * sane amount and not pixels.
	 */
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}
	
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation,
		int direction) {
		switch (orientation) {
		case SwingConstants.HORIZONTAL:
			return visibleRect.width / 10;
		case SwingConstants.VERTICAL:
			return visibleRect.height / 10;
		default:
			throw new IllegalArgumentException("Unknown orientation " + orientation);
		}
	}
	
	public int getScrollableBlockIncrement( Rectangle visibleRect, int orientation,
		int direction) {
		switch (orientation) {
		case SwingConstants.HORIZONTAL:
			return visibleRect.width;
		case SwingConstants.VERTICAL:
			return visibleRect.height;
		default:
			throw new IllegalArgumentException("Unknown orientation " + orientation);
		}
	}
	
	public boolean getScrollableTracksViewportWidth() {
		return false;
	}

	public boolean getScrollableTracksViewportHeight() {
		return false;
	}
}
