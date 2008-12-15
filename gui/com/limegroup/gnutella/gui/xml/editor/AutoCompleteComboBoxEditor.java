package com.limegroup.gnutella.gui.xml.editor;

import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;

import com.limegroup.gnutella.gui.AutoCompleteTextField;
import com.limegroup.gnutella.gui.xml.ComboBoxValue;

/**
 * A ComboBoxEditor with "auto complete" support.
 */
public class AutoCompleteComboBoxEditor implements ComboBoxEditor {
        
    private AutoCompleteTextField textField;
    private ComboBoxModel model;
    
    public AutoCompleteComboBoxEditor() {
        textField = new AutoCompleteTextField();
    }
    
    public AutoCompleteComboBoxEditor(ComboBoxModel model) {
        this();
        setModel(model);
    }
    
    public void setModel(ComboBoxModel model) {
        this.model = model;
        for(int i = model.getSize(); --i >= 0;) {
            addToDictionary(model.getElementAt(i).toString());
        }
    }
    
    public Component getEditorComponent() {
        return textField;
    }

    public Object getItem() {
        String text = textField.getText();
        for(int i = 0; i < model.getSize(); i++) {
            ComboBoxValue val = (ComboBoxValue)model.getElementAt(i);
            if(text.equalsIgnoreCase(val.toString()))
                return val;
        }
        return new ComboBoxValue(text);
    }

    public void setItem(Object obj) {
        textField.setText(obj.toString());
    }

    public void addActionListener(ActionListener l) {
        textField.addActionListener(l);
    }

    public void removeActionListener(ActionListener l) {
        textField.removeActionListener(l);
    }

    public void selectAll() {
        textField.selectAll();
    }

    public void addToDictionary(String str) {
        textField.addToDictionary(str);
    }

    public void addToDictionary(ComboBoxModel model) {
        for(int i = model.getSize(); --i >= 0;) {
            addToDictionary(model.getElementAt(i).toString());
        }
    }
}