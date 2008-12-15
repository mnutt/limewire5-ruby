package com.limegroup.gnutella.gui.xml.editor;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.limewire.util.NameValue;

import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.MultiLineLabel;
import com.limegroup.gnutella.library.FileDesc;


public class DetailsPanel extends JPanel {
    private Font boldFont = null;
    
    private List<NameValue<String>> list = new ArrayList<NameValue<String>>();
        
    public DetailsPanel(){
        super();
    }
    
    public DetailsPanel(LayoutManager layout) {
        super(layout);
    }
    
    private void add(String name, String value) {
        list.add(new NameValue<String>(name, value));
    }
    
    public void initWithFileDesc(FileDesc fd, String schemaUri) {
        
        String kind = MetaEditorUtil.getKind(fd.getFile());
        
        if (kind != null) {
           add(I18n.tr("Kind:"), kind);
        }
        
//        SchemaReplyCollectionMapper map = GuiCoreMediator.getSchemaReplyCollectionMapper();
//        LimeXMLReplyCollection collection = map.getReplyCollection(schemaUri);
//        LimeXMLDocument doc = null; //collection.getDocForFileDesc(fd);
        
//        LimeXMLSchemaRepository rep = GuiCoreMediator.getLimeXMLSchemaRepository();
//        LimeXMLSchema schema = rep.getSchema(schemaUri);
        
//        if (doc != null) {
//            for(SchemaFieldInfo infoField : schema.getCanonicalizedFields()) {
//                String field = infoField.getCanonicalizedFieldName();
//                
//                if (skipField(field))
//                    continue;
//                
//                String value = doc.getValue(field);
//
//                if (value != null && !value.equals("")) {
//                    String name = MetaEditorUtil.getStringResource(field);
//                    add(name, value);
//                }
//            }
//        }
        
        String name = I18n.tr("Size:");
        String value = GUIUtils.toUnitbytes(fd.getFileSize());
        add(name, value);
        
        name = I18n.tr("Date Modified:");
        value = GUIUtils.msec2DateTime(fd.lastModified());
        list.add(new NameValue<String>(name, value));
        
        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);
        GridBagConstraints c = new GridBagConstraints();
        
        for(NameValue<String> next : list)
            addLabel(next, layout, c);
    }
    
    protected void addLabel(NameValue<String> pair, GridBagLayout bag, GridBagConstraints c) {
        JLabel name = new JLabel(pair.getKey(), SwingConstants.TRAILING);
        
        if (boldFont == null) {
            Font currentFont = name.getFont();
            boldFont = new Font(currentFont.getName(), Font.BOLD, currentFont.getSize());
        }
        name.setFont(boldFont);
        
        c.anchor = GridBagConstraints.NORTHEAST;
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.insets = new Insets(0, 0, 2, 3);
        bag.setConstraints(name, c);
        add(name);
        
        MultiLineLabel value = new MultiLineLabel(pair.getValue(), 300);
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(0, 0, 0, 0);
        bag.setConstraints(value, c);
        add(value);
    }
    
//    private static boolean skipField(String field) {
//        if (field.equals(LimeXMLNames.AUDIO_TITLE))
//            return true;
//        else if (field.equals(LimeXMLNames.AUDIO_ARTIST))
//            return true;
//        else if (field.equals(LimeXMLNames.AUDIO_ALBUM))
//            return true;
//        else if (field.equals(LimeXMLNames.AUDIO_SECONDS))
//            return true;
//        else if (field.equals(LimeXMLNames.AUDIO_COMMENTS))
//            return true;
//        else if (!MetaEditorUtil.contains(field))
//            return true;
//        else
//            return false;
//    }
}
