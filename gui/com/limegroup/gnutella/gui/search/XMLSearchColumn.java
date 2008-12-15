package com.limegroup.gnutella.gui.search;

import com.limegroup.gnutella.gui.xml.XMLUtils;
import com.limegroup.gnutella.gui.xml.XMLValue;
import com.limegroup.gnutella.xml.SchemaFieldInfo;

public class XMLSearchColumn extends SearchColumn {

    private SchemaFieldInfo sfi;
    
    public XMLSearchColumn(int idx, SchemaFieldInfo sfi) {
        super(idx, 
              sfi.getCanonicalizedFieldName(),
              XMLUtils.getResource(sfi.getCanonicalizedFieldName()),
              sfi.getDefaultWidth(),
              sfi.getDefaultVisibility(),
              XMLValue.class);
        this.sfi = sfi;
    }
    
    public SchemaFieldInfo getSchemaFieldInfo() {
        return sfi;
    }

}
