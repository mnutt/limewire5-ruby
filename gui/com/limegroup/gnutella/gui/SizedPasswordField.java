package com.limegroup.gnutella.gui;

import java.awt.Dimension;

import javax.swing.JPasswordField;

import com.limegroup.gnutella.gui.GUIUtils.SizePolicy;

/**
 * This class creates a <tt>JPasswordField</tt> with a standardized size.<p>
 *
 * It sets the preffered and maximum size of the field to the standard
 * <tt>Dimension</tt> or sets the preferred and maximum sizes to the
 * <tt>Dimension</tt> argument.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class SizedPasswordField extends JPasswordField {
	
	/**
	 * Creates a <tt>JPasswordField</tt> with a standard size.
	 */
	public SizedPasswordField() {
		setPreferredSize(SizedTextField.STANDARD_DIMENSION);
		setMaximumSize(SizedTextField.STANDARD_DIMENSION);
	}

	/**
	 * Creates a <tt>JPasswordField</tt> with a standard size and with the 
	 * specified <tt>Dimension</tt>.
	 *
	 * @param dim the <tt>Dimension</tt> to size the field to
	 */
	public SizedPasswordField(final Dimension dim) {
		setPreferredSize(dim);
		setMaximumSize(dim);
	}

	/**
	 * Creates a <tt>JPasswordField</tt> with a standard size and with the 
	 * specified number of columns.
	 *
	 * @param columns the number of columns to use in the field
	 */
	public SizedPasswordField(final int columns) {
		super(columns);
		setPreferredSize(SizedTextField.STANDARD_DIMENSION);
		setMaximumSize(SizedTextField.STANDARD_DIMENSION);
	}

	/**
	 * Creates a <tt>JPasswordField</tt> with a standard size and with the 
	 * specified number of columns and the specified <tt>Dimension</tt>..
	 *
	 * @param columns the number of columns to use in the field
	 * @param dim the <tt>Dimension</tt> to size the field to
	 */
	public SizedPasswordField(final int columns, final Dimension dim) {
		super(columns);
		setPreferredSize(dim);
		setMaximumSize(dim);
	}

	/**
	 * Creates a <tt>JPasswordField</tt> with a standard size and with the 
	 * specified number of columns.
	 *
	 * @param columns the number of columns to use in the field
	 */
	public SizedPasswordField(final int columns, final SizePolicy sizePolicy) {
		super(columns);
		
		GUIUtils.restrictSize(this, sizePolicy);
	}

}
