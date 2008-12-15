package com.limegroup.gnutella.gui.xml;

import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.text.Document;
import javax.swing.undo.UndoManager;

import org.limewire.util.I18NConvert;
import org.limewire.util.NameValue;

import com.limegroup.gnutella.gui.AutoCompleteTextField;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.util.QueryUtils;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.SchemaFieldInfo;

/**
 * A panel that is used to gather information from the user about
 * what the search criterion based on a particular schema.
 * <p>
 * This Panel is popped up everytime the user want to enter a query and has
 * specified the schema that she would like to base her search on
 *
 * @author Sumeet Thadani
 */
public class InputPanel extends IndentingPanel {
   
    private final LimeXMLSchema SCHEMA;

    public InputPanel(LimeXMLSchema schema, ActionListener listener,
                      Document document, UndoManager undoer) {
        this(schema, listener, document, undoer, false, false, true);
    }
    
    public InputPanel(LimeXMLSchema schema, ActionListener listener,
                      Document document, UndoManager undoer,
                      boolean expand, boolean indent, boolean search) {
        super(schema, listener, document, undoer, expand, indent, search);
        SCHEMA = schema;
    }

    /**
     * @return The Schema URI associated with this InputPanel
     */
    public String getSchemaURI() {
        return SCHEMA.getSchemaURI();
    }

    public String getInput() {
        return getInput(false);
    }

    /**
     * Looks at the textFields and creates a string that can be converted 
     * into LimeXMLDocument, so that the client that receives the search 
     * sting is can compare it with documents in its repository.
     * 
     * @param normalize true if the returned string should be normalized, thisis
     * the case when the user is doing a rich query. Otherwise if annotating,
     * metadata the string need not be normalized.
     */
    public String getInput(boolean normalize) {
        List<NameValue<String>> namValList = getXMLInputList(normalize);
        String schemaURI = SCHEMA.getSchemaURI();
        String str = constructXML(namValList, schemaURI);
        return str;
    }
    
    private List<NameValue<String>> getXMLInputList(boolean normalize) {
        List<NameValue<String>> namValList = new LinkedList<NameValue<String>>();
        for(SchemaFieldInfo field : SCHEMA.getCanonicalizedFields()) {
            String key = field.getCanonicalizedFieldName();
            JComponent comp = getField(key);
            String value = "";
            if (comp instanceof JTextField) {
                JTextField theField = (JTextField)comp;
                value = theField.getText();
            } else if (comp instanceof JComboBox) {
                JComboBox theBox = (JComboBox)comp;
                value = ((ComboBoxValue)theBox.getSelectedItem()).getValue();
            }
            if (value != null && !value.equals("")) {
                if(normalize)
                    value = I18NConvert.instance().getNorm(value);
                namValList.add(new NameValue<String>(key, value));
            }
        }
        return namValList;
    }
    
    /**
     * Returns the number of fields that contain input values. 
     */
    public int getNumberOfFieldsWithInput() {
        return getXMLInputList(true).size();
    }

    /**
     * Scan through all the AutoTextField components
     * and store the input into their dictionaries.
     */
    public void storeInput() {
        List<SchemaFieldInfo> list = SCHEMA.getCanonicalizedFields();
        for(int i = 0; i < list.size(); i++) {
            SchemaFieldInfo field = list.get(i);
            String key = field.getCanonicalizedFieldName();
            JComponent comp = getField(key);
            if (comp instanceof AutoCompleteTextField) {
                AutoCompleteTextField theField = (AutoCompleteTextField)comp;
                if (!theField.getText().equals(""))
                    theField.addToDictionary();
            }
        }
    }
    
    /**
     * Returns the requested standardized query.
     */
    private String getCompositeQuery() {
        List<SchemaFieldInfo> list = SCHEMA.getCanonicalizedFields();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            SchemaFieldInfo field = list.get(i);
            String key = field.getCanonicalizedFieldName();
            JComponent comp = getField(key);
            String value = "";
            if (comp instanceof JTextField) {
                JTextField theField = (JTextField)comp;
                value = theField.getText();
            } else if (comp instanceof JComboBox) {
                JComboBox theBox = (JComboBox)comp;
                value = ((ComboBoxValue)theBox.getSelectedItem()).toString();
            }
            if (value != null && value.trim().length() > 1) {
                sb.append(value + " ");
            }
        }
        return sb.toString().trim();
    }
    
    /**
     * @return A string the represents a standard query (as opposed to a rich
     * query).
     * <p>
     * The order in which it checks for fields is schema specific.
     */
    public String getStandardQuery() {
        return QueryUtils.createQueryString(getCompositeQuery(), true);
    }
    
    /**
     * Returns a usable title for the query.
     */
    public String getTitleForQuery() {
        return getCompositeQuery(); // not normalized.
    }
       
    /**
     * Deligates to the the static method in LimeXMLDocument
     */
    public String constructXML(List<NameValue<String>> namValList, String uri) {
        if(namValList == null || namValList.isEmpty())
            return null;
        else
            return GuiCoreMediator.getLimeXMLDocumentFactory().createLimeXMLDocument(namValList, uri).getXMLString();
    }

    
}
