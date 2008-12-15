package com.limegroup.gnutella.gui.xml.editor;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;

import org.limewire.util.NameValue;

import com.limegroup.gnutella.gui.LimeTextField;
import com.limegroup.gnutella.gui.xml.ComboBoxValue;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.licenses.CCConstants;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.SchemaFieldInfo;

public abstract class MetaEditorPanel extends AbstractMetaEditorPanel {
    
    private Map<String, JComponent> nameToComponent;
    private Map<String, NameValue<String>> nameToUneditedField;
    
    protected final FileDesc[] fds;
    protected final LimeXMLDocument document;
    protected final LimeXMLSchema schema;
    
    public static enum Location {
        LEFT, RIGHT;
    }

    private static final Insets leftInsetsLabel = new Insets(5,3,0,2);
    private static final Insets rightInsetsLabel = new Insets(5,15,0,2);
    
    private static final Insets leftInsetsComponent = new Insets(0,3,2,2);
    private static final Insets rightInsetsComponent = new Insets(0,15,2,2);
    
    public MetaEditorPanel(FileDesc[] fds, 
            LimeXMLSchema schema, LimeXMLDocument document) {
        
        this.fds = fds;
        this.schema = schema;
        this.document = document;
        
        nameToComponent = new HashMap<String, JComponent>();
        nameToUneditedField = new HashMap<String, NameValue<String>>();
        
        setBorder( BorderFactory.createEmptyBorder(10,20,10,20));
    
        initWithDocumentFields();
    }
    
    public FileDesc[] getFileDesc() {
        return fds;
    }
    
    public LimeXMLDocument getDocument() {
        return document;
    }
    
    public LimeXMLSchema getSchema() {
        return schema;
    }
    
    public String getValue(String name) {
        return (document != null) ? document.getValue(name) : null;
    }
    
    /**
     * Creates a JLabel and adds it to the view using a gridbagconstraints as the layout
     * 
     * @param text - text of label
     * @param gridx - x location in grid
     * @param gridy - y loction in grid
     * @param gbc - gb constraints
     * @param loc - enum Location.LEFT || Location.RIGHT (horz location on the screen)
     */
    public JLabel createLabelAndAdd(String text, int gridx, int gridy, GridBagConstraints gbc, Location loc) {
        JLabel label = new JLabel(text);
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        if( loc == Location.LEFT ) {
            gbc.insets = leftInsetsLabel;
        } else {
            gbc.insets = rightInsetsLabel;
        }
        add( label, gbc);
        
        return label;
    }
    
    /**
     * Creates a TextField and adds it to the view and the component list for saving data, uses gridbagcontraints 
     * as the layout
     * 
     * @param textLength- length of the text field
     * @param gridx - x location in grid
     * @param gridy - y location in grid
     * @param gridWidth - number of grids to span
     * @param gbc - gb constraints
     * @param loc - enum Location.LEFT || Location.RIGHT (horz location on the screen)
     * @param name - keyword name to use when adding component to map 
     */
    public LimeTextField createTextField(int textLength, int gridx, int gridy, int gridWidth, GridBagConstraints gbc, Location loc,
            String name){
        LimeTextField textField = new LimeTextField(textLength);
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.gridwidth = gridWidth;
        gbc.weightx = 1;
        if( loc == Location.LEFT) {
            gbc.insets = leftInsetsComponent;
        }else {
            gbc.insets = rightInsetsComponent;
        }
        add( textField, gbc );
        addComponent(name, textField);
        
        return textField;
    }
    
    /**
     * Creates a ComboBox and adds it to the view and the component list for saving data, uses gridbagcontraints 
     * as the layout
     * 
     * @param gridx - x location in grid
     * @param gridy - y location in grid
     * @param gbc - gb constraints
     * @param loc - enum Location.LEFT || Location.RIGHT (horz location on the screen)
     * @param name - keyword name to use when adding component to map 
     */
    public JComboBox createComboBox(int gridx, int gridy, GridBagConstraints gbc, Location loc, String name){
        JComboBox box = new JComboBox();
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        if( loc == Location.LEFT ) {
            gbc.insets = leftInsetsComponent;
        } else {
            gbc.insets = rightInsetsComponent;
        }
        add( box, gbc);
        addComponent(name, box);
        
        return box;
    }
    
    public void addComponent(String name, JComponent component) {
        addComponent(name, null, component);
    }
    
    public void addComponent(String name, JCheckBox checkbox, JComponent component) {
        nameToComponent.put(name, component);
    }
    
    public JComponent getComponent(String name) {
        return nameToComponent.get(name);
    }
    
    public Iterable<String> getComponentIterator() {
        return nameToComponent.keySet();
    }
    
    public Iterable<NameValue<String>> getUneditedFieldsIterator() {
        return nameToUneditedField.values();
    }
    
    public void reset() {
        for(String name : getComponentIterator()) {
            JComponent comp = getComponent(name);
            
            if (comp instanceof JTextComponent) {
                ((JTextComponent)comp).setText("");
            } else if (comp instanceof JComboBox) {
                ((JComboBox)comp).setSelectedIndex(0);
            }
        }
    }
    
    protected void initFields() {
        for(String name : getComponentIterator()) {
            String value = getValue(name);
            JComponent comp = getComponent(name);
            if(comp instanceof  JTextComponent) {
                if(value!=null) {
                    ((JTextComponent)comp).setText(value);
                    if(comp instanceof JTextArea) {
                        ((JTextArea)comp).setCaretPosition(0);
                    }
                }
            } else if(comp instanceof JComboBox) {
                JComboBox box = (JComboBox)comp;
                for(SchemaFieldInfo infoField : getSchema().getEnumerationFields()) {
                    String currField = infoField.getCanonicalizedFieldName();
                    if(currField.equals(name)) {
                        List<ComboBoxValue> values = new ArrayList<ComboBoxValue>();
                        values.add(0, new ComboBoxValue());
                        addEnums(infoField.getEnumerationList(), values);
                        
                        int index = 0;
                        
                        if (value != null && !value.equals("")) {
                            ComboBoxValue combVal = new ComboBoxValue(value);
                            if (!values.contains(combVal))
                                values.add(combVal);
                        }
                        
                        if((name.indexOf("__licensetype__") >= 0) 
                                && value!=null 
                                && !value.equals(CCConstants.CC_URI_PREFIX)) {
                            values.remove(new ComboBoxValue(CCConstants.CC_URI_PREFIX));
                        }
                        
                        Object[] arr = values.toArray(new Object[0]);
                        Arrays.sort(arr);
                        if(value != null && !value.equals(""))
                            index = Arrays.asList(arr).indexOf(new ComboBoxValue(value));
                        
                        box.setModel(new DefaultComboBoxModel(arr));
                        box.setSelectedIndex(index);
                    }
                }
            }
        }
    }
    
    @Override
    public List<NameValue<String>> getInput() {
        List<NameValue<String>> nameValueList = new ArrayList<NameValue<String>>();
    	
        for(NameValue<String> nv : getUneditedFieldsIterator()) {
            nameValueList.add(nv); 
        }
        
        for(String name : getComponentIterator()) {
            JComponent comp = getComponent(name);
            
            String value = null;
            if (comp instanceof JTextComponent) {
                value = ((JTextComponent)comp).getText().trim();
            } else if (comp instanceof JComboBox) {
                JComboBox box = (JComboBox)comp;
                ComboBoxValue cbv = (ComboBoxValue)box.getSelectedItem();
                if(cbv != null) {
                    String cbvalue = cbv.getValue();
                    if(cbvalue != null) {
                        value = cbvalue.trim();
                    }
                }
            }

            if (value != null)
                nameValueList.add(new NameValue<String>(name, value));
        }
        return nameValueList;
    }
    
    protected void addEnums(List<? extends NameValue<String>> nameValues, List<? super ComboBoxValue> comboValues) {
        for(NameValue<String> nv : nameValues)
            comboValues.add(new ComboBoxValue(nv));
    }    
    
    private void initWithDocumentFields() {
        if(document != null) {
            for(Map.Entry<String, String> entry : document.getNameValueSet()) {
                String name = entry.getKey();
                nameToUneditedField.put(name, new NameValue<String>(name, entry.getValue()));
            }
        }
    } 
    
    /**
     * @return true if changes have occurred to the meta data, if not return false and 
     * nothing will be written to disk.
     */
    @Override
    public boolean checkInput() {
        for(String name : getComponentIterator()) { 
            JComponent comp = getComponent(name);
            
            String value = null;
            if (comp instanceof JTextComponent) {
                value = ((JTextComponent)comp).getText().trim();
            } else if (comp instanceof JComboBox) {
                JComboBox box = (JComboBox)comp;
                ComboBoxValue cbv = (ComboBoxValue)box.getSelectedItem();
                if(cbv != null) {
                    String cbvalue = cbv.getValue();
                    if(cbvalue != null) {
                        value = cbvalue.trim();
                    }
                }
    }

            NameValue<String> nv = nameToUneditedField.get(name);
            // if the text field has something in it and the old field isn't empty compare value
            if( value != null && value.length() > 0 && nv != null ) {
                // if new text and old text don't match, update
                if ( !value.equals(nv.getValue()) )
                    return true;
            }
            // if the textfield has something in it and the old field is empty, must be new data so update
            else if ( value != null && value.length() > 0 && nv == null ){
                return true;
            }
            // if neither field has anything in it, must not have changed continue
            else if( (value == null || value.length() == 0) && (nv == null || (nv.getValue() == null || nv.getValue().length() == 0))){
                continue;
            }
            // else the textfield must have nothing in it and the old field did, must be updated
            else 
                return true;
        }
        return false;
    }
}
