package org.limewire.ui.swing.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;

import org.limewire.ui.swing.components.AutoCompleter.AutoCompleterCallback;
import org.limewire.util.OSUtils;


/** A DropDown list of autocompletable items for a JTextField. */
public class DropDownListAutoCompleteControl {
    
    private static final String PROPERTY = "limewire.text.autocompleteControl";

    /** The text field this is working on. */
    private final JTextField textField;
    
    /** The autocompleter. */
    private final AutoCompleter autoCompleter;

    /** The popup the scroll pane is in */
    protected Popup popup;

    /** Whether or not we tried to show a popup while this wasn't showing */
    protected boolean showPending;
    
    /** Whether or not this control should try to autocomplete input. */
    private boolean autoComplete = true;
    
    /** Installs a dropdown list for autocompletion on the given text field. */
    public static DropDownListAutoCompleteControl install(final JTextField textField) {
        final BasicAutoCompleter basicAutoCompleter = new BasicAutoCompleter();
        textField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                basicAutoCompleter.addAutoCompleteSuggestion(textField.getText());
            }
        });
        return install(textField, basicAutoCompleter);
    }

    /** Installs a dropdown list using the given autocompleter. */
    public static DropDownListAutoCompleteControl install(JTextField textField,
            AutoCompleter autoCompleter) {
        DropDownListAutoCompleteControl control = new DropDownListAutoCompleteControl(textField, autoCompleter);
        textField.putClientProperty(PROPERTY, control);
        Listener listener = control.new Listener();
        textField.addKeyListener(listener);
        textField.addHierarchyListener(listener);
        textField.addFocusListener(listener);
        textField.addActionListener(listener);
        autoCompleter.setAutoCompleterCallback(listener);
        return control;
    }
    
    
    /** Returns the control for the text field. */
    public static DropDownListAutoCompleteControl getDropDownListAutoCompleteControl(JTextField textField) {
        return (DropDownListAutoCompleteControl)textField.getClientProperty(PROPERTY);
    }
    
    private DropDownListAutoCompleteControl(JTextField textField, AutoCompleter autoCompleter) {
        this.textField = textField;
        this.autoCompleter = autoCompleter;
    }    
    
    /**
    * Sets whether the component is currently performing autocomplete lookups as
    * keystrokes are performed.
    *
    * @param autoComplete True or false.
    */
    public void setAutoComplete(boolean autoComplete) {
        this.autoComplete = autoComplete;
    }
    
    /**
    * Gets whether the component is currently performing autocomplete lookups as
    * keystrokes are performed. 
    *
    * @return True or false.
    */
    public boolean isAutoCompleting() {
        return autoComplete;
    }
    
    /**
     * Displays the popup window with a list of auto-completable choices,
     * if any exist.
     */
    private void autoCompleteInput() {
        // Shove this into an invokeLater to force us seeing the proper text.
        if(isAutoCompleting()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    String input = textField.getText();
                    if (input != null && input.length() > 0) {
                        autoCompleter.setInput(input);
                        if(autoCompleter.isAutoCompleteAvailable()) {
                            showPopup();
                        } else {
                            hidePopup();
                        }
                    } else {
                        hidePopup();
                    }
                }
            });
        }
    }

    /** Shows the popup. */
    private void showPopup() {
        if(autoCompleter.isAutoCompleteAvailable()) {
            if(textField.isShowing()) {
                Point origin = textField.getLocationOnScreen();
                PopupFactory pf = PopupFactory.getSharedInstance();
				Component parent = textField;
				JComponent component = autoCompleter.getRenderComponent();
				
		        // Null out our prior preferred size, then set a new one
		        // that overrides the width to be the size we want it, but
		        // preserves the height.
				Dimension priorPref = component.getPreferredSize();
		        component.setPreferredSize(null);
		        Dimension pref = component.getPreferredSize();
		        pref = new Dimension(textField.getWidth(), pref.height+10);
		        component.setPreferredSize(pref);
		        if(popup != null && priorPref.equals(pref)) {
		            return; // no need to change if sizes are same.
		        }

                // OSX doesn't handle MOUSE_CLICKED events correctly
                // using medium-weight popups, so we need to force
                // PopupFactory to return a heavy-weight popup.
                // This is done by adding a panel into a Popup, which implicitly
                // adds it into a Popup.HeavyWeightWindow, which PopupFactory happens
                // to check as a condition for returning a heavy-weight popup.
                // In an ideal world, the OSX bug would be fixed.
                // In a less ideal world, Popup & PopupFactory would be written so that
                // outside developers can correctly subclass methods.
                if(OSUtils.isMacOSX()) {
                    parent = new JPanel();
                    new MyPopup(textField, parent, 0, 0);
                }
		        
		        // If the popup exists already, hide it & reshow it to make it the right size.
				if(popup != null) {
				    popup.hide();
				}
				
                popup = pf.getPopup(parent, component, origin.x, origin.y + textField.getHeight());
                showPending = false;
                popup.show();
            } else {
                showPending = true;
            }
        }
    }

    /** Hides the popup window. */
    private void hidePopup() {
        showPending = false;
        if(popup != null) {
            popup.hide();
            popup = null;
        }
    }
    
    private class Listener implements ActionListener, KeyListener, HierarchyListener, FocusListener, AutoCompleterCallback {
        
        @Override
        public void itemSuggested(String autoCompleteString, boolean keepPopupVisible, boolean triggerAction) {
            textField.setText(autoCompleteString);
            textField.setCaretPosition(textField.getDocument().getLength());
            if(triggerAction) {
                textField.postActionEvent();
            } else if(!keepPopupVisible) {
                hidePopup();
            }
        }
        
        /**
         * Fires an action event.
         *
         * If the popup is visible, this resets the current
         * text to be the selection on the popup (if something was selected)
         * prior to firing the event.
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            if(popup != null) {
                String selection = autoCompleter.getSelectedAutoCompleteString();
                hidePopup();
                if(selection != null) {
                    textField.setText(selection);
                }
            }
        }        
        
        /** Forwards necessary events to the AutoCompleteList. */
        @Override
        public void keyPressed(KeyEvent evt) {
            if(evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
                evt.consume();    
            }
            
            if(autoCompleter != null) {
                switch(evt.getKeyCode()) {
                case KeyEvent.VK_UP:
                    if(popup != null) {
                        autoCompleter.decrementSelection();
                    } else {
                        String input = textField.getText();
                        autoCompleter.setInput(input);
                        showPopup();
                    }
                    break;
                case KeyEvent.VK_DOWN:
                    if(popup != null) {
                        autoCompleter.incrementSelection();
                    } else { 
                        String input = textField.getText();
                        autoCompleter.setInput(input);
                        showPopup();
                    }
                    break;
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_RIGHT:
                    if(popup != null) {
                        String selection = autoCompleter.getSelectedAutoCompleteString();
                        if(selection != null) {
                            hidePopup();
                        }
                    }
                    break;
                }
            }
        }
        
        /** Forwards necessary events to the AutoCompleteList. */
        @Override
        public void keyReleased(KeyEvent evt) {
            if(evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN)
                evt.consume();       
        }
        
        /** Forwards necessary events to the AutoCompleteList. */
        @Override
        public void keyTyped(KeyEvent evt) {
            if(evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
                evt.consume();                
            }
            
            if(autoCompleter != null) {
                switch(evt.getKeyChar()) {
                case KeyEvent.VK_ESCAPE:
                    if (popup != null) {
                        hidePopup();
                        textField.selectAll();
                    }
                    break;
                case KeyEvent.VK_ENTER:
                    break;
                default:
                    autoCompleteInput();
                }
            }
        }
        
        @Override
        public void hierarchyChanged(HierarchyEvent evt) {            
            if((evt.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == HierarchyEvent.SHOWING_CHANGED) {
                boolean showing = textField.isShowing();
                if(!showing && popup != null) {
                    hidePopup();
                } else if(showing && popup == null && showPending) {
                    autoCompleteInput();
                }
            }
        }
        
        @Override
        public void focusGained(FocusEvent e) {
        }

        @Override
        public void focusLost(FocusEvent evt) {
            if (evt.getID() == FocusEvent.FOCUS_LOST && popup != null) {
                hidePopup();
            }
        }
    }
	
	/**
     * Subclass that provides access to the constructor.
     */
	private static class MyPopup extends Popup {
		public MyPopup(Component owner, Component contents, int x, int y) {
			super(owner, contents, x, y);
		}
	}
}

