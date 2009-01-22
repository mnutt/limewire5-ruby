package org.limewire.ui.swing.search;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;

import net.miginfocom.swing.MigLayout;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.collection.StringTrieSet;
import org.limewire.ui.swing.components.AutoCompleter;
import org.limewire.ui.swing.components.CollectionBackedListModel;
import org.limewire.ui.swing.components.ExtendedCompoundBorder;
import org.limewire.ui.swing.components.SideLineBorder;
import org.limewire.ui.swing.util.FontUtils;

/** An autocompleter that shows its suggestions in a list and can have new suggestions added. */
public class HistoryAndFriendAutoCompleter implements AutoCompleter {
    
    private final JPanel entryPanel;
    
    private AutoCompleterCallback callback;
    private String currentText;
    
    private boolean showSuggestions = true;
    private AutoCompleteDictionary historyDictionary;
    private AutoCompleteDictionary suggestionDictionary;
    
    private final AutoCompleteList entryList;
    
    public HistoryAndFriendAutoCompleter() {
        this(new StringTrieSet(true));
    }
    
    public HistoryAndFriendAutoCompleter(AutoCompleteDictionary dictionary) {
        this.suggestionDictionary = dictionary;
        
        entryPanel = new JPanel(new MigLayout("insets 0, gap 0, fill"));
        entryPanel.setBorder(UIManager.getBorder("List.border"));
        entryPanel.setBackground(UIManager.getColor("List.background"));
        
        entryList = new AutoCompleteList();
        
        JScrollPane entryScrollPane = new JScrollPane(entryList);
        entryScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        entryScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        entryPanel.add(entryScrollPane, "grow");
    }

    public void setSuggestionsShown(boolean value) {
        this.showSuggestions = value;
    }

    public void setSuggestionDictionary(AutoCompleteDictionary dictionary) {
        this.suggestionDictionary = dictionary;
    }
    
    public void setHistoryDictionary(AutoCompleteDictionary dictionary) {
        this.historyDictionary = dictionary;
    }
    
    @Override
    public void setAutoCompleterCallback(AutoCompleterCallback callback) {
        this.callback = callback;
    }

    @Override
    public boolean isAutoCompleteAvailable() {
        return entryList.getModel().getSize() != 0;
    }

    @Override
    public void decrementSelection() {
        entryList.decrementSelection();
    }

    @Override
    public JComponent getRenderComponent() {
        return entryPanel;
    }

    @Override
    public String getSelectedAutoCompleteString() {
        Object selection = entryList.getSelectedValue();
        if(selection != null) {
            return selection.toString();
        } else {
            return null;
        }
    }

    @Override
    public void incrementSelection() {
        entryList.incrementSelection();
    }

    @Override
    public void setInput(String input) {
        if(input == null) {
            input = "";
        }
        
        currentText = input;
        
        Collection<String> histories = historyDictionary.getPrefixedBy(currentText);
        ArrayList<Entry> items = new ArrayList<Entry>(histories.size());
        for(String string : histories) {
            items.add(new Entry(string, Entry.Reason.HISTORY));
        }
        
        if(showSuggestions) {
            Collection<String> suggestions = suggestionDictionary.getPrefixedBy(currentText);
            items.ensureCapacity(items.size() + suggestions.size());
            boolean needFirstSuggestion = true;
            for(String string : suggestions) {
                if(needFirstSuggestion) {
                    items.add(new Entry(string, Entry.Reason.FIRST_SUGGESTION));
                    needFirstSuggestion = false;
                } else {
                    items.add(new Entry(string, Entry.Reason.SUGGESTION));
                }
            }
        }
        
        entryList.setModel(new CollectionBackedListModel(items));
        entryList.setVisibleRowCount(Math.min(8, items.size()));
    }
    
    /** A list that's used to show auto-complete items. */
    private class AutoCompleteList extends JList {
        AutoCompleteList() {
            enableEvents(AWTEvent.MOUSE_EVENT_MASK);
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            setFocusable(false);
            setCellRenderer(new Renderer());
        }
        
        // override to return true always, to enforce '...' added
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }
        
        /**
         * Sets the text field's selection with the clicked item.
         */
        @Override
        protected void processMouseEvent(MouseEvent me) {
            super.processMouseEvent(me);
            
            if(me.getID() == MouseEvent.MOUSE_CLICKED) {
                int idx = locationToIndex(me.getPoint());
                if(idx != -1 && isSelectedIndex(idx)) {
                    callback.itemSuggested(getSelectedValue().toString(), false, true);
                }
            }
        }
        
        /**
         * Increments the selection by one.
         */
        void incrementSelection() {
            if(getSelectedIndex() == getModel().getSize() - 1) {
                callback.itemSuggested(currentText, true, false);
                clearSelection();
            } else {
                int selectedIndex = getSelectedIndex() + 1;
                setSelectedIndex(selectedIndex);
                ensureIndexIsVisible(selectedIndex);
                callback.itemSuggested(getSelectedValue().toString(), true, false);
            }
        }
        
        /**
         * Decrements the selection by one.
         */
        void decrementSelection() {
            if(getSelectedIndex() == 0) {
                callback.itemSuggested(currentText, true, false);
                clearSelection();
            } else {
                int selectedIndex = getSelectedIndex();
                if(selectedIndex == -1) {
                    selectedIndex = getModel().getSize() - 1;
                } else {
                    selectedIndex--;
                }
                setSelectedIndex(selectedIndex);
                ensureIndexIsVisible(selectedIndex);
                callback.itemSuggested(getSelectedValue().toString(), true, false);
            }
        }
    }
    
    private static class Renderer extends DefaultListCellRenderer {
        private final ExtendedCompoundBorder compoundBorder;
        private final JPanel firstSuggestionPanel;
        private final DefaultListCellRenderer firstSuggestionLabel;
        private final JLabel firstSuggestionTitle;
        private final Border firstSuggestionBorder = new SideLineBorder(Color.BLACK, SideLineBorder.Side.TOP);
        
        public Renderer() {
            compoundBorder = new ExtendedCompoundBorder(BorderFactory.createEmptyBorder(), BorderFactory.createEmptyBorder(0, 2, 0, 2));
            
            firstSuggestionPanel = new JPanel();
            firstSuggestionPanel.setLayout(new MigLayout("nocache, fill, gap 0, insets 0"));
            firstSuggestionPanel.setBorder(compoundBorder);
            
            firstSuggestionLabel = new DefaultListCellRenderer();
            firstSuggestionTitle = new DefaultListCellRenderer();
            firstSuggestionTitle.setText("Friend Suggestions");
            FontUtils.changeSize(firstSuggestionTitle, -1);
            firstSuggestionTitle.setForeground(Color.GRAY);
            
            firstSuggestionPanel.add(firstSuggestionLabel, "alignx left, grow, wmin 0, gapright 10");            
            firstSuggestionPanel.add(firstSuggestionTitle, "alignx right");            
        }
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            
            String render;
            if(value == null) {
                render = "";
            } else {
                render = value.toString();
                if(value instanceof Entry) {
                    if(((Entry)value).reason == Entry.Reason.FIRST_SUGGESTION) {
                        firstSuggestionLabel.getListCellRendererComponent(list, render, index, isSelected, cellHasFocus);
                        firstSuggestionLabel.setBorder(BorderFactory.createEmptyBorder());
                        if(isSelected) {
                            firstSuggestionTitle.setForeground(list.getSelectionForeground());
                        } else {
                            firstSuggestionTitle.setForeground(Color.GRAY);
                        }
                        if(index != 0) {
                            compoundBorder.setOuterBorder(firstSuggestionBorder);
                        } else {
                            compoundBorder.setOuterBorder(BorderFactory.createEmptyBorder());
                        }
                        firstSuggestionTitle.setBackground(firstSuggestionLabel.getBackground());
                        firstSuggestionPanel.setBackground(firstSuggestionLabel.getBackground());
                        return firstSuggestionPanel;
                    }
                }
            }
            
            super.getListCellRendererComponent(list, render, index, isSelected, cellHasFocus);
            compoundBorder.setOuterBorder(getBorder());
            setBorder(compoundBorder);
            return this;
        }
    }
    
    private static class Entry {
        private static enum Reason { HISTORY, FIRST_SUGGESTION, SUGGESTION };
        
        private final String item;
        private final Reason reason;
        
        public Entry(String item, Reason reason) {
            this.item = item;
            this.reason = reason;
        }
        
        @Override
        public String toString() {
            return item;
        }
        
        @Override
        public boolean equals(Object obj) {
            if(obj == this) {
                return true;
            } else {
                return ((Entry)obj).item.equals(item) && ((Entry)obj).reason == reason;
            }
        }
    }
    
}
