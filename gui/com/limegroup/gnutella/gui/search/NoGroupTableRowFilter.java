package com.limegroup.gnutella.gui.search;

import java.util.ArrayList;
import java.util.List;



public class NoGroupTableRowFilter extends TableRowFilter{
    
    public NoGroupTableRowFilter(TableLineFilter f) {
        super(f);
    }
    
    @Override
    public TableLine getNewDataLine(SearchResult sr) {
        TableLine dl = createDataLine();
        dl.initialize(sr);
        return dl;
    }
    
    @Override
    void filtersChanged() {
        rebuild();
        fireTableDataChanged();
    }
    
    private void rebuild() {
        
        List<TableLine> existing = new ArrayList<TableLine>(_list);
        List<TableLine> hidden = new ArrayList<TableLine>(HIDDEN);
        simpleClear();
        
        setUseMetadata(false);
        
        // For stuff in _list, we can just re-add the DataLines as-is.
        if(isSorted()) {
            for(int i = 0; i < existing.size(); i++) {
                addSorted(existing.get(i));
            }
        } else {
            for(int i = 0; i < existing.size(); i++) {
                add(existing.get(i));
            }
        }
                
        
        if(isSorted()) {
            for(TableLine line : hidden)
                addSorted(line);
        } else {
            for(TableLine line : hidden)
                add(line);
        }
        
        setUseMetadata(true);        
    }

}
