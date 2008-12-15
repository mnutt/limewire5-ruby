package com.limegroup.gnutella.gui;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.UIManager;

/**
 * A panel that emulates the look and feel of a textfield containing 
 * an icon on the left side.
 */
public class IconTextField extends BoxPanel {

	private final JTextField textField;
	
	private final JLabel iconLabel;

	/**
	 * Constructs an icon text field with an icon and a {@link JTextField} with
	 * <code>columns</code> columns.
	 * @param icon the icon that is displayed on the left side
	 * @param columns the number of columns the {@link JTextField} has
	 */
	public IconTextField(Icon icon, int columns) {
		this(icon, new JTextField(columns));
	}
	
	/**
	 * Constructs an icon text field with an icon and a given text field.
	 * @param icon the icon that is displayed on the left side
	 * @param textField the text field that is embeded in the panel
	 */
	public IconTextField(Icon icon, JTextField textField) {
		super(BoxPanel.X_AXIS);

		iconLabel = new JLabel(icon);
		iconLabel.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 4));
		iconLabel.setOpaque(false);
		add(iconLabel);
		this.textField = textField; 
		textField.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
		add(textField);
	}
	
	/**
	 * Returns the textfield that is internally used.
	 */
	public JTextField getTextField() {
		return textField;
	}
	
	@Override
	public void updateUI() {
		super.updateUI();
		setBackground(UIManager.getColor("TextField.background"));
		setBorder(UIManager.getBorder("TextField.border"));
	}
	
}
