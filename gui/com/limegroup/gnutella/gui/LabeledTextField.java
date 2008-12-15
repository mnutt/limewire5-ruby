package com.limegroup.gnutella.gui;

import java.awt.Dimension;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * This class creates a text field with a label next to it.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

public class LabeledTextField extends JPanel {

	/**
	 * The <tt>JTextField</tt> part of this component.
	 */
	private JTextField _field;
	
	/**
	 * Constructor with the specified width of the text box in columns
	 * 
	 * @param lab the label for the text field
	 * @param textWidth the number of columns in the text field
	 */
	public LabeledTextField(String lab, int textWidth) {
        this(lab, textWidth, -1, 500);
	}

    
	/**
	 * Constructor with the specified width of the text box and a
	 * specified margin in pixels to the left of the labeled field.
	 *
	 * @param lab the label for the text field
	 * @param textWidth the number of columns in the text field
	 * @param strutSize the size (in pixels) of the margin to the left of
	 *  the labeled field
	 */
    public LabeledTextField(String lab, int textWidth, int strutSize) {
        this(lab, textWidth, strutSize, 500);
    }    


    /**
     * Constructor with the specified width of the text box and a
     * specified margin in pixels to the left of the labeled field.
     *
     * @param lab the label for the text field
     * @param textWidth the number of columns in the text field
     * @param strutSize the size (in pixels) of the margin to the left of
     *  the labeled field
     * @param width The Width of the textfield.
     */
	public LabeledTextField(String lab, int textWidth, int strutSize,
                            int width) {
        BoxLayout layout = new BoxLayout(this, BoxLayout.X_AXIS);
        setLayout(layout);
        Dimension d = new Dimension(width, 20);
        setPreferredSize(d);
        setMaximumSize(d);
        JLabel label = new JLabel(lab);
        _field = new LimeTextField("",textWidth);
        if (strutSize > -1)
            add(Box.createHorizontalStrut(strutSize));
        add(label);
        add(Box.createHorizontalStrut(GUIConstants.SEPARATOR));
        add(_field);
	}


	
	/**
	 * Returns the String contained in the <tt>JTextField</tt>.
	 *
	 * @return the text in the <tt>JTextField</tt>
	 */
	public String getText() {
		return _field.getText();
	}
	
	/**
	 * Sets the String contained in the <tt>JTextField</tt>.
	 *
	 * @param text the text to place in the <tt>JTextField</tt>
	 */
	public void setText(String text) {
		_field.setText(text);
	}
	
	/**
	 * Sets the tooltip for the JTextField.
	 *
	 * @param text the text to set as the tooltip for the <tt>JTextField</tt>
	 */
	@Override
    public void setToolTipText(String text) {
		_field.setToolTipText(text);
	}
	
	/**
	 * Sets whether or not the <tt>JTextField</tt> can be edited by the user.
	 *
	 * @param editable sets whether the enclosed <tt>JTextField</tt> is 
	 *                 editable or not
	 */
	public void setEditable(boolean editable) {
		_field.setEditable(editable);
	}

    /** 
     * Adds a ActionListener to the TextField.
     * @param ActionListener A ActionListener implementation. 
     */
    public void addActionListener(AbstractAction aa) {
        _field.addActionListener(aa);
    }

}
