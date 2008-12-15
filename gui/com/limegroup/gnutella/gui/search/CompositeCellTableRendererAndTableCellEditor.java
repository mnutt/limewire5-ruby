package com.limegroup.gnutella.gui.search;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import com.limegroup.gnutella.gui.tables.LimeJTable;
import com.limegroup.gnutella.gui.themes.ThemeFileHandler;
import com.limegroup.gnutella.gui.themes.ThemeObserver;

/**
 * A composite renderer and editor for use to get mouse events to lines in 
 * a <code>LimeJTable</code>.
 */
public abstract class CompositeCellTableRendererAndTableCellEditor extends JPanel
        implements TableCellRenderer, TableCellEditor, ThemeObserver {
       
    /** Main label that always display text */
    public final JLabel mainLabel;
    
    /** Keep around a list of the buttons for hiding and showing of them */
    public final List<SmallButton> buttons = new ArrayList<SmallButton>();
    
    /** The horizontal gap in the layout. */
    private final static int HGAP = 2;
    
    /** The vertical gap in the layout. */
    private final static int VGAP = 2;
    
    JPanel buttonPanel;
    
    protected CompositeCellTableRendererAndTableCellEditor() {
        
        this.setLayout( new BorderLayout());
        
        int columns = createActions().length;
        GridLayout grid = new GridLayout(1,columns);
        grid.setHgap(HGAP);
        
        buttonPanel = new JPanel(grid);
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder( BorderFactory.createEmptyBorder(VGAP, HGAP,VGAP,HGAP));

        this.mainLabel = new JLabel();
                
        this.add(buttonPanel, BorderLayout.WEST );
        this.add(mainLabel, BorderLayout.CENTER);

        for (Action a : createActions()) {
            final SmallButton sb = new SmallButton(a);
            buttonPanel.add(sb);
            buttons.add(sb);
        }      
        
        setOpaque(true);
    }

    /**
     * @return the buttons that will go on the left of the line
     */
    protected abstract Action[] createActions();

    /** Default properties of all buttons that may be generated */
    private static class SmallButton extends JButton  {

        SmallButton(Action a) {
            super(a);
            setFocusable(false);
            
            // resize the font to fit the button
            Font font = UIManager.getFont("Table.font");
            font = font.deriveFont((float)(font.getSize() * 0.8));
            setFont(font);
            setVerticalTextPosition(SwingConstants.TOP);
        }
    }
    
    /**
     * Return the text to display in a cell for the value in that certain cell.
     * 
     * @param value the value in the cell
     * @return the text to display in a cell for the value in that certain cell
     */
    protected abstract String getNameForValue(Object value);

    /**
     * @return true if the buttons should be painted, false otherwise
     */
    protected boolean buttonsVisible(){
        return true;
    }


    // --------------------------------------------------------------------------------
    // TableCellRenderer
    // -------------------------------------------------------------------------------- 
    
    public final Component getTableCellRendererComponent(final JTable table,
            final Object value, final boolean isSel, final boolean hasFocus,
            final int row, final int column) {
        return getTableCellComponent(table, value, isSel, hasFocus, row, column);
    }

    // --------------------------------------------------------------------------------
    // TableCellEditor
    // --------------------------------------------------------------------------------

    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();

    public final Component getTableCellEditorComponent(JTable table, Object value, 
            boolean isSel, int row, int col) {
        return getTableCellComponent(table, value, true, true, row, col);
    }

    public final void addCellEditorListener(CellEditorListener lis) {
        synchronized (listeners) {
            if (!listeners.contains(lis)) listeners.add(lis);
        }
    }

    public final void cancelCellEditing() {
        synchronized (listeners) {
            for (int i=0, N=listeners.size(); i<N; i++) {
                listeners.get(i).editingCanceled(new ChangeEvent(this));
            }
        }
    }

    public final Object getCellEditorValue() {
        return null;
    }

    /**
     * @return true if there is an editor for this cell which can be accessed
     */
    public boolean isCellEditable(EventObject e) {
        return true;
    }

    public final void removeCellEditorListener(CellEditorListener lis) {
        synchronized (listeners) {
            if (listeners.contains(lis)) listeners.remove(lis);
        }
    }

    public final boolean shouldSelectCell(EventObject e) {
        return true;
    }

    public final boolean stopCellEditing() {
        synchronized (listeners) {
            for (int i=0, N=listeners.size(); i<N; i++) {
                listeners.get(i).editingStopped(new ChangeEvent(this));        
            }
        }
        return true;
    }    
    
    protected abstract Color getFontColor(Color defaultColor);
    
    // --------------------------------------------------------------------------------
    // JPanel
    // --------------------------------------------------------------------------------
    
    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        //
        // This may be null when this is called in the constructor
        //
        if (mainLabel != null) mainLabel.setBackground(bg);
            
    }
    
    @Override
    public void setForeground(Color fg) {
        super.setForeground(fg);
        //
        // This may be null when this is called in the constructor
        //
        if (mainLabel != null) 
            mainLabel.setForeground(getFontColor(fg));
    }
    
    // --------------------------------------------------------------------------------
    // Misc
    // --------------------------------------------------------------------------------
    
    /**
     * Performs the painting of each cell in both the renderer and editor. Since the
     * editor's only job is to forward button clicks to the buttons, both the 
     * renderer and editor can be painted in the same fashion
     */
    private Component getTableCellComponent(final JTable table, final Object value,
            final boolean isSel, final boolean hasFocus, final int row,
            final int column) {

        // set the label text
        final String text = getNameForValue(value);
        mainLabel.setText(text);

        // if the buttons are visible, paint them
        buttonPanel.setVisible(buttonsVisible());
                
        // update the background and foreground colors
        if (isSel ) {
            setBackground(UIManager.getColor("Table.selectionBackground"));
            setForeground(UIManager.getColor("Table.selectionForeground"));
        } else {
            LimeJTable ljt = (LimeJTable)table;
            setBackground(ljt.getBackgroundForRow(row));
            setForeground(ljt.getForeground());
        }

        return this;        
    }

    
    /**
     * When the theme changes, the JLabel will not be defaultly updated since
     * it is wrapped in a jpanel. Must explicitely update the label
     */
    public void updateTheme() {
        mainLabel.setFont(UIManager.getFont("Table.font"));
        mainLabel.setForeground(ThemeFileHandler.WINDOW8_COLOR.getValue());
    } 
}

