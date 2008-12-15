package com.limegroup.gnutella.gui.tables;

import javax.swing.JLabel;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Simple renderer that centers the data.
 */
public final class CenteredRenderer extends DefaultTableCellRenderer {
	public CenteredRenderer() {
	    super();
	    setHorizontalAlignment(JLabel.CENTER);
	}
}