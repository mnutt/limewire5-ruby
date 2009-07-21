package org.limewire.ui.swing.components;

import java.awt.Component;
import java.awt.Font;
import java.util.Locale;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.LanguageUtils;

/**
 * A ComboBox for displaying and selecting the language to translate
 * text to.
 */
public class LanguageComboBox extends JComboBox {

    @Resource
    private Font font;
    
    public LanguageComboBox() {
        GuiUtils.assignResources(this);
        
        setRenderer(new LocaleRenderer());
        setFont(font);
        
        Locale[] locales = LanguageUtils.getLocales(font);
        // ensure that the combo box cannot be empty if something goes wrong.
        if(locales.length == 0)
           locales = new Locale[]{Locale.ENGLISH};
        setModel(new DefaultComboBoxModel(locales));
    }
    
    private static class LocaleRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
            
            if (value instanceof Locale) {
                Locale locale = (Locale) value;
                setText(locale.getDisplayName(locale));
            } else {
                setIcon(null);
            }
            
            return this;
        }
    }
}
