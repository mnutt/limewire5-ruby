package com.limegroup.gnutella.gui.xml.editor;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTabbedPane;

import org.limewire.util.NameValue;

import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;

public abstract class MetaEditorTabbedPane extends JTabbedPane {
    
    protected final FileDesc[] fds;
    protected final LimeXMLDocument document;
    protected final LimeXMLSchema schema;
    
    public MetaEditorTabbedPane(FileDesc[] fds, String uri) {
        super();
        
        this.fds = fds;
        
        LimeXMLDocument[] docs = MetaEditorUtil.intersection(fds, uri);
        
        LimeXMLSchemaRepository rep = GuiCoreMediator.getLimeXMLSchemaRepository();
        schema = rep.getSchema(uri);
        if(schema == null)
            throw new IllegalStateException("no schema!");
        
        LimeXMLDocument doc = null;
        for(int i = 0; i < docs.length; i++) {
            if(schema.equals(docs[i].getSchema())) {
                doc = docs[i];
                break;
            }
        }
        document = doc;
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
    
    public String getInput() {
        List<NameValue<String>> nameValueList = new ArrayList<NameValue<String>>();
        final int count = getTabCount();
        for(int i = 0; i < count; i++) {
            Component tab = getComponentAt(i);
            if (tab instanceof AbstractMetaEditorPanel) {
            	AbstractMetaEditorPanel panel = (AbstractMetaEditorPanel)tab;
            	List<NameValue<String>> list = panel.getInput();
            	if(list != null && !list.isEmpty()) {
                    nameValueList.addAll(list);
                }
            }
        }
        
        if (nameValueList.isEmpty()) {
            return null;
        }
        
        return GuiCoreMediator.getLimeXMLDocumentFactory().createLimeXMLDocument(nameValueList, getSchema().getSchemaURI()).getXMLString();
    }
    
    /**
     *	Checks input values to see if they've been edited. 
     *  @return true if changes have occured, false otherwise
     */
    public boolean hasDataChanged() {
		 final int count = getTabCount();
	     for(int i = 0; i < count; i++) {
	    	 Component tab = getComponentAt(i);
	    	 if (tab instanceof AbstractMetaEditorPanel) {
	    		 AbstractMetaEditorPanel editor = (AbstractMetaEditorPanel) tab;
	    		 if(editor.checkInput())
	    		     return true;
	    	 }
	    	 //any other tab should check input here
	     } 
	     return false;
   }
    
}
