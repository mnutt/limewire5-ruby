package com.limegroup.gnutella.gui.search;

import com.limegroup.gnutella.gui.tables.Linkable;



/**
 * The ResultName displayed in a search result line. These values are rendered
 * by ResultNameRenderer.
 */
class ResultNameHolder implements Comparable<ResultNameHolder>, Linkable {
    private TableLine line;   
    private String description;

    public ResultNameHolder(TableLine line) {
        this.description = line.getFilenameNoExtension();
		this.line = line; 
    }
    
    public boolean isLink() {
        return line.isLink();
    }
    
    public String getLinkUrl() {
        return line.getLinkUrl();
    }
    
    public String getLinkDisplayUrl() {
        return line.getLinkDisplayUrl();
    }

    /** A textual */
    public String stringValue() {
        return description;
    }

    /** Returns the spam rating */
    public float getSpamRating() {
        return line.getSpamRating();
    }
    
    /**
     * Returns true if the two ResultNameHolders are exactly the same
     */
    @Override
    public boolean equals(Object other) {
        if(other instanceof ResultNameHolder) {
            ResultNameHolder o = (ResultNameHolder)other;
            return o.description.equals(description);
        } else {
            return false;
        }
    }    

    /**
     * compare by by description string
     */
    public int compareTo(ResultNameHolder o) {
        return this.description.compareTo(o.description);
    }
	
	@Override
    public String toString() {
		return stringValue();
	}
}    
