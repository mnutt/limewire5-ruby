package org.limewire.ui.swing.properties;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.ui.swing.table.AbstractTableFormat;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

public class FileInfoBittorrentPanel implements FileInfoPanel {

    private static final int DONT_DOWNLOAD = 0;
    private static final int LOWEST_PRIORITY = 1;
    private static final int NORMAL_PRIORITY = 2;
    private static final int HIGHEST_PRIORITY = 3;
    
    private final Torrent torrent;
    
    private final JPanel component;
    private BitTorrentTable table;
    private EventList<TorrentFileEntryWrapper> eventList;
    
    public FileInfoBittorrentPanel(Torrent torrent) {
        this.torrent = torrent;
        
        component = new JPanel(new MigLayout("fill"));

        init();
    }
    
    @Override
    public JComponent getComponent() {
        return component;
    }
    
    private void init() {
        component.setOpaque(false);
        
        eventList = GlazedLists.threadSafeList(new BasicEventList<TorrentFileEntryWrapper>());
        List<TorrentFileEntry> fileEntries = torrent.getTorrentFileEntries();
        for(TorrentFileEntry entry : fileEntries) {
            eventList.add(new TorrentFileEntryWrapper(entry));
        }
        
        table = new BitTorrentTable(new DefaultEventTableModel<TorrentFileEntryWrapper>(eventList, new BitTorrentTableFormat()));
        
        component.add(new JScrollPane(table), "grow");
    }

    @Override
    public boolean hasChanged() {
        boolean hasChanged = false;
        for(TorrentFileEntryWrapper wrapper : eventList) {
            if(wrapper.getPriority() != wrapper.getTorrentFileEntry().getPriority()) {
                hasChanged = true;
                break;
            }
        }
        return hasChanged;
    }

    @Override
    public void save() {
        if(hasChanged()) {
            for(TorrentFileEntryWrapper wrapper : eventList) {
                torrent.setTorrenFileEntryPriority(wrapper.getTorrentFileEntry(), wrapper.getPriority());
            }
        }
    }

    @Override
    public void unregisterListeners() {
        //no listeners registered
    }
    
    /**
     * We need this because we don't want to update the real entries
     * if the user hits the cancel button
     */
    private class TorrentFileEntryWrapper {
        
        private final TorrentFileEntry entry;
        private int priority;
        
        public TorrentFileEntryWrapper(TorrentFileEntry entry) {
            this.entry = entry;
            this.priority = entry.getPriority();
        }
        
        public String getPath() {
            return entry.getPath();
        }
        
        public float getProgress() {
            return entry.getProgress();
        }
        
        public int getPriority() {
            return priority;
        }
        
        public void setPriority(int priority) {
            this.priority = priority;
        }
        
        public TorrentFileEntry getTorrentFileEntry() {
            return entry;
        }
    }
    
    private class BitTorrentTable extends MouseableTable {
        public BitTorrentTable(DefaultEventTableModel<TorrentFileEntryWrapper> model) {
            super(model);
            setShowHorizontalLines(false);
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            setColumnSelectionAllowed(false);
            final CheckBoxRendererEditor checkBoxEditor = new CheckBoxRendererEditor();
            checkBoxEditor.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(checkBoxEditor.getCellEditorValue() != null) {
                        checkBoxEditor.getCellEditorValue().setPriority(checkBoxEditor.isSelected() ? 1 : 0);
                        checkBoxEditor.cancelCellEditing();
                    }
                    BitTorrentTable.this.repaint();
                }
            });
            getColumn(BitTorrentTableFormat.DOWNLOAD_INDEX).setCellRenderer(new CheckBoxRendererEditor());
            getColumn(BitTorrentTableFormat.DOWNLOAD_INDEX).setCellEditor(checkBoxEditor);
            
            getColumn(BitTorrentTableFormat.PERCENT_INDEX).setCellRenderer(new PercentRenderer());
            
            final PriorityRendererEditor editor = new PriorityRendererEditor();
            editor.getButton().addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    int oldPriority = editor.getCellEditorValue().getPriority();
                    if(oldPriority != DONT_DOWNLOAD) {
                        editor.getCellEditorValue().setPriority(oldPriority + 1 > HIGHEST_PRIORITY ? LOWEST_PRIORITY : (oldPriority + 1));
                        editor.cancelCellEditing();
                        BitTorrentTable.this.repaint();
                    }
                }
                
            });
            getColumn(BitTorrentTableFormat.PRIORITY_INDEX).setCellRenderer(new PriorityRendererEditor());
            getColumn(BitTorrentTableFormat.PRIORITY_INDEX).setCellEditor(editor);
            
            getColumnExt(BitTorrentTableFormat.DOWNLOAD_INDEX).setMaxWidth(30);
            getColumnExt(BitTorrentTableFormat.DOWNLOAD_INDEX).setMinWidth(30);
            
            getColumnExt(BitTorrentTableFormat.PERCENT_INDEX).setMaxWidth(50);
            getColumnExt(BitTorrentTableFormat.PERCENT_INDEX).setMinWidth(50);
            
            getColumnExt(BitTorrentTableFormat.PRIORITY_INDEX).setMaxWidth(60);
            getColumnExt(BitTorrentTableFormat.PRIORITY_INDEX).setMinWidth(60);
        }
    }
    
    private class BitTorrentTableFormat extends AbstractTableFormat<TorrentFileEntryWrapper> {

        private static final int DOWNLOAD_INDEX = 0;
        private static final int NAME_INDEX = 1;
        private static final int PERCENT_INDEX = 2;
        private static final int PRIORITY_INDEX = 3;
        
        public BitTorrentTableFormat() {
            super(I18n.tr("DL"),
                  I18n.tr("Name"),
                  I18n.tr("%"),
                  I18n.tr("Priority"));
        }
        
        @Override
        public Object getColumnValue(TorrentFileEntryWrapper baseObject, int column) {
            switch(column) {
                case DOWNLOAD_INDEX: return baseObject;
                case NAME_INDEX: return baseObject.getPath();
                case PERCENT_INDEX: return baseObject;
                case PRIORITY_INDEX: return baseObject;
            }
            throw new IllegalStateException("Unknown column:" + column);
        }
        
    }
    
    private class CheckBoxRendererEditor extends JCheckBox implements TableCellRenderer, TableCellEditor {

        /** List of cell editor listeners. */
        private final List<CellEditorListener> listenerList = new ArrayList<CellEditorListener>();
        
        private TorrentFileEntryWrapper currentWrapper;
        
        public CheckBoxRendererEditor() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            return getTableCellComponent(table, value, isSelected, column, column);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            return getTableCellComponent(table, value, isSelected, column, column);
        }
        
        private Component getTableCellComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            if(value instanceof TorrentFileEntryWrapper) {
                currentWrapper = (TorrentFileEntryWrapper) value;
                if(((TorrentFileEntryWrapper)value).getProgress() == 1.0f) {
                    setSelected(true);
                    setEnabled(false);
                } else {
                    setSelected(((TorrentFileEntryWrapper)value).getPriority() != DONT_DOWNLOAD);
                    setEnabled(!torrent.isFinished());
                }
            } else {
                currentWrapper = null;
                setSelected(false);
            }
            
            return this;
        }

        @Override
        public void addCellEditorListener(CellEditorListener l) {
            if (!listenerList.contains(l)) {
                listenerList.add(l);
            }
        }

        @Override
        public void cancelCellEditing() {
            ChangeEvent event = new ChangeEvent(this);
            for (int i = 0, size = listenerList.size(); i < size; i++) {
                listenerList.get(i).editingCanceled(event);
            }
        }

        @Override
        public TorrentFileEntryWrapper getCellEditorValue() {
            return currentWrapper;
        }

        @Override
        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }

        @Override
        public void removeCellEditorListener(CellEditorListener l) {
            listenerList.remove(l);
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

    private class PercentRenderer extends DefaultTableCellRenderer {

        public PercentRenderer() {
            setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            
            if(value instanceof TorrentFileEntryWrapper) {
                float percent = ((TorrentFileEntryWrapper)value).getProgress();
                if( percent == 1.0f) {
                    setText(I18n.tr("Done"));
                } else if(((TorrentFileEntryWrapper)value).getPriority() == DONT_DOWNLOAD){
                    setText("");
                } else {
                    setText((int)(percent * 100) + "%");
                }
            } else {
                setText("");
            }
            
            return this;
        }
    }
    
    private class PriorityRendererEditor extends JPanel implements TableCellRenderer, TableCellEditor {

        /** List of cell editor listeners. */
        private final List<CellEditorListener> listenerList = new ArrayList<CellEditorListener>();
        
        @Resource private Icon lowestPriorityIcon;
        @Resource private Icon normalPriorityIcon;
        @Resource private Icon highestPriorityIcon;
        @Resource private Font textFont;
        @Resource private Color fontColor;
        
        private final JButton button;
        
        private TorrentFileEntryWrapper currentWrapper;
        
        public PriorityRendererEditor() {
            super(new MigLayout("align 50%"));
            GuiUtils.assignResources(this);
            
            button = new JButton();
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setFont(textFont);
            button.setFocusPainted(false);
            button.setForeground(fontColor);
            FontUtils.underline(button);
            
            add(button);
        }
        
        public JButton getButton() {
            return button;
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            return getTableCellComponent(table, value, isSelected, row, column);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            return getTableCellComponent(table, value, isSelected, row, column);
        }
        
        private Component getTableCellComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            if(value instanceof TorrentFileEntryWrapper) {
                currentWrapper = (TorrentFileEntryWrapper) value;
                int priority = ((TorrentFileEntryWrapper)value).getPriority();
                if(((TorrentFileEntryWrapper)value).getProgress() == 1.0f) {
                    button.setIcon(null);
                    button.setText("");
//                    button.setText(I18n.tr("delete"));
                } else if(priority == DONT_DOWNLOAD) {
                    button.setIcon(null);
                    button.setText("");
                } else if(priority == LOWEST_PRIORITY) {
                    button.setIcon(lowestPriorityIcon);
                    button.setText("");
                } else if(priority == NORMAL_PRIORITY) {
                    button.setIcon(normalPriorityIcon);
                    button.setText("");
                } else if(priority == HIGHEST_PRIORITY) {
                    button.setIcon(highestPriorityIcon);
                    button.setText("");
                }
            } else {
                currentWrapper = null;
                button.setIcon(null);
                button.setText("");
            }
            return this;
        }

        @Override
        public void addCellEditorListener(CellEditorListener l) {
            if (!listenerList.contains(l)) {
                listenerList.add(l);
            }
        }

        @Override
        public void cancelCellEditing() {
            ChangeEvent event = new ChangeEvent(this);
            for (int i = 0, size = listenerList.size(); i < size; i++) {
                listenerList.get(i).editingCanceled(event);
            }
        }

        @Override
        public TorrentFileEntryWrapper getCellEditorValue() {
            return currentWrapper;
        }

        @Override
        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }

        @Override
        public void removeCellEditorListener(CellEditorListener l) {
            listenerList.remove(l);
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
