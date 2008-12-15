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
     * Returns true if any suggestions are currently available, based the data
     * that was given to {@link #setInput(String)}.
     */
    boolean areSuggestionsAvailable();

    /** Returns a component that renders the suggestions. */
    JComponent getComponent();

    /** Returns the currently selected suggestion. */
    String getSelectedSuggestion();

    /** Increments the selection. */
    void incrementSelection();
    
    /** Decrements the selection. */
    void decrementSelection();
    
    /** A callback for users of autocompleter, so they know when items have been suggested. */
    public interface AutoCompleterCallback {
        void itemSuggested(String suggestion, boolean keepSuggestionsVisible);
    }

}
