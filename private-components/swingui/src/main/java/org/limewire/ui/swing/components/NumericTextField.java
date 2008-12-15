package org.limewire.ui.swing.components;

import java.awt.Toolkit;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * A JTextField component that only accepts integer values as input. Any other
 * input will not be added and a system beep will be issued. Additionally, 
 * a min and max number can be set.
 */
public class NumericTextField extends JTextField {

    /**
     * Create a Textfield with a specified number of columns displayed.
     */
    public NumericTextField(int columns) {
        this(columns, 0, Integer.MAX_VALUE);
    }

    /**
     * Create a Textfield with a specified number of columns displayed,
     * and minimum and maximum integer values which will be accepted.
     */
    public NumericTextField(int columns, int minValue, int maxValue) {
        super(columns);
        addIntegerWithMaxValueFilter(minValue, maxValue);
    }

    /**
     * @return the value of the field as an int.  If field is empty, or the field text
     * cannot be retrieved as an int for any other reason, return the specified default value
     */
    public int getValue(int defaultValue) {
        try {
            return Integer.parseInt(getText());
        } catch (NumberFormatException e) {
            setValue(defaultValue);
            return defaultValue;
        }
    }

    /**
     * Given an int, set the content of the text field
     *
     * @param value set on the field
     */
    public void setValue(int value) {
        setText(String.valueOf(value));
    }

    private void addIntegerWithMaxValueFilter(int minValue, int maxValue) {
        ((AbstractDocument) this.getDocument()).setDocumentFilter(new NumericDocumentFilter(minValue, maxValue));
    }


    class NumericDocumentFilter extends DocumentFilter {

        private final int maxValue;
        private final int minValue;

        NumericDocumentFilter(int minValue, int maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;    
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {

            if (string == null)
                return;
            if (isValidValue(string, offset, 0)) {
                super.insertString(fb, offset, string, attr);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (text == null)
                return;
            if (isValidValue(text, offset, length)) {
                super.replace(fb, offset, length, text, attrs);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }

        private boolean isStringNumeric(String string) {
            try {
                Integer.parseInt(string);
            } catch (NumberFormatException e) {
                return false;
            }
            return true;
        }

        private boolean isValidValue(String text, int offset, int length) {
            String currentValue = getText();
            String newValue = currentValue.substring(0, offset) + text + currentValue.substring(offset+length);

            if (isStringNumeric(newValue) &&
                    Integer.parseInt(newValue) >= minValue &&
                    Integer.parseInt(newValue) <= maxValue) {
                return true;
            }
            return false;
        }
    }
}
