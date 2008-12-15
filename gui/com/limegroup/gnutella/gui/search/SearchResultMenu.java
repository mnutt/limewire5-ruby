package com.limegroup.gnutella.gui.search;

import javax.swing.JPopupMenu;

/**
 * The search result menu.
 */
final class SearchResultMenu {
    
    private final ResultPanel PANEL;

    /**
     * Private constructor to ensure that this class can never be
     * created.
     */
    SearchResultMenu(ResultPanel rp) {
        PANEL = rp;
    }
    
    /**
     * Adds search-result specific items to the JPopupMenu.
     */
    JPopupMenu addToMenu(JPopupMenu popupMenu, TableLine[] lines, boolean markAsSpam, boolean markAsNot) {
        
        // Check if there are lines
        if (lines.length == 0) {
            return popupMenu;
		}
		
        // Now check to see if any of the table lines are different classes
        // In this case we need to show a message that only similar
        for (int i=1; i<lines.length; i++) {
            if (!lines[i-1].isSameKindAs(lines[i])) {
                // Bail! Bail!
                // Just pick the first similar ones, since we're
                // lost as to what to do otherwise...
                TableLine[] newLines = new TableLine[i-1];
                System.arraycopy(lines, 0, newLines, 0, i-1);
                lines = newLines;
                break;
        }
    }
    
        return lines[0].getSearchResult().createMenu(popupMenu, lines, markAsSpam, markAsNot, PANEL);
    
    }
}
