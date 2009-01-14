package org.limewire.ui.swing.util;

import java.text.Format;

import javax.swing.JComboBox;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.AutoCompleteSupport;
import ca.odell.glazedlists.swing.EventComboBoxModel;
import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.swing.EventTableModel;

/**
 * A factory for creating GlazedLists Swing models without requiring
 * that they internally create a SwingThreadProxyList.
 */
public class GlazedListsSwingFactory {
    
    /** @see EventSelectionModel#EventSelectionModel(EventList) */
    public static <E> EventSelectionModel<E> eventSelectionModel(EventList<E> source) {
        return new EventSelectionModel<E>(source, false);
    }

    /** @see AutoCompleteSupport#install(JComboBox, EventList) */
    public static <E> AutoCompleteSupport<E> install(JComboBox comboBox, EventList<E> items) {
        return AutoCompleteSupport.install(comboBox, items, false);
    }
    
    /** @see AutoCompleteSupport#install(JComboBox, EventList, TextFilterator) */
    public static <E> AutoCompleteSupport<E> install(JComboBox comboBox, EventList<E> items, TextFilterator<E> filterator) {
        return AutoCompleteSupport.install(comboBox, items, filterator, false);
    }
    
    /** @see AutoCompleteSupport#install(JComboBox, EventList, TextFilterator, Format) */
    public static <E> AutoCompleteSupport<E> install(JComboBox comboBox, EventList<E> items, TextFilterator<E> filterator, Format format) {
        return AutoCompleteSupport.install(comboBox, items, filterator, format, false);
    }
    
    /** @see EventComboBoxModel#EventComboBoxModel(EventList) */
    public static <E> EventComboBoxModel<E> eventComboBoxModel(EventList<E> source) {
        return new EventComboBoxModel<E>(source, false);
    }
    
    /** @see EventListModel#EventListModel(EventList) */
    public static <E> EventListModel<E> eventListModel(EventList<E> source) {
        return new EventListModel<E>(source, false);
    }
    
    /** @see EventTableModel#EventTableModel(EventList, TableFormat) */
    public static <E> EventTableModel<E> eventTableModel(EventList<E> source, TableFormat<E> tableFormat) {
        return new EventTableModel<E>(source, tableFormat, false);
    }
    
    

}
