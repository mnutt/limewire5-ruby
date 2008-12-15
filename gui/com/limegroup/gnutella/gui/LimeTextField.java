package com.limegroup.gnutella.gui;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.text.Document;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.limewire.i18n.I18nMarker;

/**
 * A better JTextField.
 */
public class LimeTextField extends JTextField {
    
    /**
     * The undo action.
     */
    private static Action UNDO_ACTION = new FieldAction(I18nMarker.marktr("Undo")) {
        public void actionPerformed(ActionEvent e) {
            getField(e).undo();
        }
    };
    
    /**
     * The cut action
     */
    private static Action CUT_ACTION = new FieldAction(I18nMarker.marktr("Cut")) {
        public void actionPerformed(ActionEvent e) {
            getField(e).cut();
        }
    };
    
    /**
     * The copy action.
     */
    private static Action COPY_ACTION = new FieldAction(I18nMarker.marktr("Copy")) {
        public void actionPerformed(ActionEvent e) {
            getField(e).copy();
        }
    };
    
    /**
     * The paste action.
     */
    private static Action PASTE_ACTION = new FieldAction(I18nMarker.marktr("Paste")) {
        public void actionPerformed(ActionEvent e) {
            getField(e).paste();
        }
    };
    
    /**
     * The delete action.
     */
    private static Action DELETE_ACTION = new FieldAction(I18nMarker.marktr("Delete")) {
        public void actionPerformed(ActionEvent e) {
            getField(e).replaceSelection("");
        }
    };
      
    /**
     * The select all action.
     */      
    private static Action SELECT_ALL_ACTION = new FieldAction(I18nMarker.marktr("Select All")) {
        public void actionPerformed(ActionEvent e) {
            getField(e).selectAll();
        }
    };    
    
    /**
     * The sole JPopupMenu that's shared among all the text fields.
     */
    private static final JPopupMenu POPUP = createPopup();
    
    /**
     * Our UndoManager.
     */
    private UndoManager undoManager;
    
    /**
     * Constructs a new LimeTextField.
     */
    public LimeTextField() {
        super();
        init();
    }
    
    /**
     * Constructs a new LimeTextField with the given text.
     */
    public LimeTextField(String text) {
        super(text);
        init();
    }
    
    /**
     * Constructs a new LimeTextField with the given amount of columns.
     */
    public LimeTextField(int columns) {
        super(columns);
        init();
    }
    
    /**
     * Constructs a new LimeTextField with the given text & number of columns.
     */
    public LimeTextField(String text, int columns) {
        super(text, columns);
        init();
    }
    
    /**
     * Constructs a new LimeTextField with the given document, text, and columns.
     */
    public LimeTextField(Document doc, String text, int columns) {
        super(doc, text, columns);
        init();
    }
    
    /**
     * Undoes the last action.
     */
    public void undo() {
        try {
            if(undoManager != null)
                undoManager.undoOrRedo();
        } catch(CannotUndoException ignored) {
        } catch(CannotRedoException ignored) {
        }
    }
    
    /**
     * Sets the UndoManager (but does NOT add it to the document).
     */
    public void setUndoManager(UndoManager undoer) {
        undoManager = undoer;
    }
    
    /**
     * Gets the undo manager.
     */
    public UndoManager getUndoManager() {
        return undoManager;
    }   
    
    /**
     * Intercept the 'setDocument' so that we can null out our manager
     * and possibly assign a new one.
     */
    @Override
    public void setDocument(Document doc) {
        if(doc != getDocument())
            undoManager = null;
        super.setDocument(doc);
    }
            
    
    /**
     * Initialize the necessary events.
     */ 
    private void init() {
        setComponentPopupMenu(POPUP);
            
        undoManager = new UndoManager();
        undoManager.setLimit(1);
        getDocument().addUndoableEditListener(undoManager);
    }
    
    /**
     * Creates the JPopupMenu that all LimeTextFields will share.
     */
    private static JPopupMenu createPopup() {
        JPopupMenu popup;

        // initialize the JPopupMenu with necessary stuff.
        popup = new JPopupMenu() {
            @Override
            public void show(Component invoker, int x, int y) {
                ((LimeTextField)invoker).updateActions();
                super.show(invoker, x, y);
            }
        };
        
        popup.add(new JMenuItem(UNDO_ACTION));
        popup.addSeparator();
        popup.add(new JMenuItem(CUT_ACTION));
        popup.add(new JMenuItem(COPY_ACTION));
        popup.add(new JMenuItem(PASTE_ACTION));
        popup.add(new JMenuItem(DELETE_ACTION));
        popup.addSeparator();
        popup.add(new JMenuItem(SELECT_ALL_ACTION));
        return popup;
    }
    
    /**
     * Updates the actions in each text just before showing the popup menu.
     */
    private void updateActions() {
        String selectedText = getSelectedText();
        if(selectedText == null)
            selectedText = "";
        
        boolean stuffSelected = !selectedText.equals("");
        boolean allSelected = selectedText.equals(getText());
        
        UNDO_ACTION.setEnabled(isEnabled() && isEditable() && isUndoAvailable());
        CUT_ACTION.setEnabled(isEnabled() && isEditable() && stuffSelected);
        COPY_ACTION.setEnabled(isEnabled() && stuffSelected);
        PASTE_ACTION.setEnabled(isEnabled() && isEditable() && isPasteAvailable());
        DELETE_ACTION.setEnabled(isEnabled() && stuffSelected);
        SELECT_ALL_ACTION.setEnabled(isEnabled() && !allSelected);
    }
    
    /**
     * Determines if an Undo is available.
     */
    private boolean isUndoAvailable() {
        return getUndoManager() != null && getUndoManager().canUndoOrRedo();
    }
    
    /**
     * Determines if paste is currently available.
     */
    private boolean isPasteAvailable() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            return clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor);
        } catch(UnsupportedOperationException he) {
            return false;
        } catch(IllegalStateException ise) {
            return false;
        }
    }

    /**
     * Base Action that all LimeTextField actions extend.
     */
    private static abstract class FieldAction extends AbstractAction {
        
        /**
         * Constructs a new FieldAction looking up the name from the MessagesBundles.
         */
        public FieldAction(String name) {
            super(I18n.tr(name));
        }
        
        /**
         * Gets the LimeTextField for the given ActionEvent.
         */
        protected LimeTextField getField(ActionEvent e) {
            JMenuItem source = (JMenuItem)e.getSource();
            JPopupMenu menu = (JPopupMenu)source.getParent();
            return (LimeTextField)menu.getInvoker();
        }

    }
}
