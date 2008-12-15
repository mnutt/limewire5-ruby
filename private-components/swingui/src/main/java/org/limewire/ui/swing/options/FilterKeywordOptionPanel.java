package org.limewire.ui.swing.options;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.settings.FilterSettings;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.table.AbstractTableFormat;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.swing.EventTableModel;

public class FilterKeywordOptionPanel extends OptionPanel {

    private JButton okButton;
    private JTextField keywordTextField;
    private JButton addKeywordButton;
    private FilterTable filterTable;
    private SpamManager spamManager;
    
    private EventList<String> eventList;
    
    public FilterKeywordOptionPanel(SpamManager spamManager, Action okAction) {
        this.spamManager = spamManager;
        setLayout(new MigLayout("gapy 10"));
        
        keywordTextField = new JTextField(30);
        addKeywordButton = new JButton(I18n.tr("Add Keyword"));
        eventList = GlazedLists.threadSafeList(new BasicEventList<String>());
        
        filterTable = new FilterTable(new EventTableModel<String>(eventList, new FilterTableFormat()));
        okButton = new JButton(okAction);
        addKeywordButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = keywordTextField.getText();
                if(text == null || text.trim().length() == 0)
                    return;
                if(!eventList.contains(text)) {
                    eventList.add(text);
                }
                keywordTextField.setText("");
            }
        });
        
        add(new MultiLineLabel(I18n.tr("LimeWire will not show files with the following keywords in your search results"), 300), "span, wrap");
        add(keywordTextField, "gapright 10");
        add(addKeywordButton,"wrap");
        add(new JScrollPane(filterTable), "span 2, grow, wrap");
        
        add(okButton, "skip 1, alignx right");
    }
    
    @Override
    boolean applyOptions() {
        String[] values = eventList.toArray(new String[eventList.size()]);
        FilterSettings.BANNED_WORDS.setValue(values);
        spamManager.adjustSpamFilters();
        return false;
    }

    @Override
    boolean hasChanged() {
        List model = Arrays.asList(FilterSettings.BANNED_WORDS.getValue());
        String[] values = eventList.toArray(new String[eventList.size()]);
        
        return model.equals(new ArrayList<String>(Arrays.asList(values)));
    }

    @Override
    public void initOptions() {
        String[] bannedWords = FilterSettings.BANNED_WORDS.getValue();
        eventList.addAll(new ArrayList<String>(Arrays.asList(bannedWords)));
    }
    
    private class FilterTable extends JXTable {
        
        public FilterTable(EventTableModel<String> model) {
            super(model);
            setShowGrid(false, false);
            setColumnSelectionAllowed(false);
            setSelectionMode(0);
            getColumn(1).setCellRenderer(new RemoveButtonRenderer(this));
            getColumn(1).setCellEditor(new RemoveButtonRenderer(this));
        }
        
        //Don't set the cell value when editing is cancelled
        @Override
        public void editingStopped(ChangeEvent e) {
            TableCellEditor editor = getCellEditor();
            if (editor != null) {          
                removeEditor();
            }
        }
        
        @Override
        public boolean isCellEditable(int row, int col) {
            if (row >= getRowCount() || col >= getColumnCount() || row < 0 || col < 0) {
                return false;
            }
            return getColumnModel().getColumn(col).getCellEditor() != null;
        }
    }
    
    private class FilterTableFormat extends AbstractTableFormat<String> {

        private static final int NAME_INDEX = 0;
        private static final int BUTTON_INDEX = 1;
        
        public FilterTableFormat() {
            super(I18n.tr("Extension"), "");
        }

        @Override
        public Object getColumnValue(String baseObject, int column) {
            switch(column) {
                case NAME_INDEX: return baseObject;
                case BUTTON_INDEX: return baseObject;
            }
                
            throw new IllegalStateException("Unknown column:" + column);
        }
    }
    
    private class RemoveButtonRenderer extends JButton implements TableCellRenderer, TableCellEditor {
        private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
        
        public RemoveButtonRenderer(final FilterTable table) {
            super(I18n.tr("remove"));
            
            setBorder(null);
            setContentAreaFilled(false);
            setFocusPainted(false);
            FontUtils.underline(this);
            setOpaque(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    eventList.remove(table.getSelectedRow());
                    cancelCellEditing();
                }
            });
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if(isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            return this;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
            return this;
        }

        @Override
        public void cancelCellEditing() {
            synchronized (listeners) {
                for (int i=0, N=listeners.size(); i<N; i++) {
                    listeners.get(i).editingCanceled(new ChangeEvent(this));
                }
            }
        }

        @Override
        public Object getCellEditorValue() {
            return null;
        }

        @Override
        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }

        @Override
        public void removeCellEditorListener(CellEditorListener lis) {
            synchronized (listeners) {
                if (listeners.contains(lis)) listeners.remove(lis);
            }
        }
        
        @Override
        public void addCellEditorListener(CellEditorListener lis) {
            synchronized (listeners) {
                if (!listeners.contains(lis)) listeners.add(lis);
            }
        }

        @Override
        public boolean shouldSelectCell(EventObject anEvent) {
            return true;
        }

        @Override
        public boolean stopCellEditing() {
            cancelCellEditing();
            return true;
        }
    }

}
