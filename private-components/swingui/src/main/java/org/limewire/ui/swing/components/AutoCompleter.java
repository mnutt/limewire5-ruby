package org.limewire.ui.swing.components;

import javax.swing.JComponent;

/** A component that can offer autocomplete suggestions. */
public interface AutoCompleter {
    
    /** Sets the callback that will be notified when items are selected. */
    void setAutoCompleterCallback(AutoCompleterCallback callback);

    /** 
     * Sets the new input to the autocompleter.  This can change what is
     * currently visible as suggestions.
     */
    void setInput(String input);

    /**
     * Returns true if any autocomplete suggestions are currently available, based the data
     * that was given to {@link #setInput(String)}.
     */
    boolean isAutoCompleteAvailable();

    /** Returns a component that renders the autocomplete items. */
    JComponent getRenderComponent();

    /** Returns the currently selected string. */
    String getSelectedAutoCompleteString();

    /** Increments the selection. */
    void incrementSelection();
    
    /** Decrements the selection. */
    void decrementSelection();
    
    /** A callback for users of autocompleter, so they know when items have been suggested. */
    public interface AutoCompleterCallback {
        /** Notification that an item is suggested. */
        void itemSuggested(String autoCompleteString, boolean keepPopupVisible, boolean triggerAction);
    }

}
