package com.limegroup.gnutella.gui.tables;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.CellEditor;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import org.limewire.util.StringUtils;

import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.GUIConstants;
import com.limegroup.gnutella.gui.PaddedPanel;
import com.limegroup.gnutella.gui.themes.ThemeFileHandler;
import com.limegroup.gnutella.gui.themes.ThemeObserver;
import com.limegroup.gnutella.licenses.License;

/**
 * The basics of a ComponentMediator for a Table.
 * Used for:
 *   Associating a LimeJTable (TABLE) with a DataLineModel (DATA_MODEL).
 *   Associating a JPopupMenu & ButtonRow (BUTTON_ROW)
 *     with the DATA_MODEL.
 *   Holding common Action/Mouse/ListSelection listeners.
 *     (REMOVE_LISTENER, DEFAULT_LISTENER, HEADER_LISTENER, SELECTION_LISTENER)
 *   Holding common TableCellRenderers. [static]
 *     (PROGRESS_BAR_RENDERER, CHAT_RENDERER)
 *   Building a JPanel (MAIN_PANEL) of the LimeJTable, ButtonRow & JPopupMenu.
 *   Handling mouse interactions and displaying the appropriate menus.
 *      A popup menu if right-click over table.
 *      ColumnSelectionMenu if right-click over the header.
 *      Sorting the DATA_MODEL if left-click over the header.
 *   Refreshing the DATA_MODEL from the RefreshListener's call.
 *   @author Sam Berlin
 */
public abstract class AbstractTableMediator<T extends DataLineModel<E, I>, E extends DataLine<I>, I>
    implements ComponentMediator<I>, HeaderMouseObserver, ThemeObserver {

    /**
     * The ID that uniquely defines this table.
     */
    protected final String ID;

    /**
     * Variable to the main component displaying this Table.
     * MUST be initialized in setupConstants()
     */
    protected PaddedPanel MAIN_PANEL;

    /**
     * Variable to the DataLineList containg the underlying data for this table.
     * MUST be initialized in setupConstants()
     */
    protected T DATA_MODEL;

    /**
     * Variable to the LimeJTable for this table.
     * MUST be initialized in setupConstants()
     */
    protected LimeJTable TABLE;

    /**
     * Variable to the ButtonRow for this table.
     */
    protected ButtonRow BUTTON_ROW;

    /**
     * Variable for the RemoveListener for this component.
     */
    public ActionListener REMOVE_LISTENER;

    /**
     * Variable for the DefaultMouseListener for this component.
     */
    public MouseListener DEFAULT_LISTENER;

    /**
     * Variable for the HeaderMouseListener for this component.
     */
    public MouseInputListener HEADER_LISTENER;

    /**
     * Variable for the ListSelectionListener for this component.
     */
    public ListSelectionListener SELECTION_LISTENER;
    
    /**
     * KeyListener for moving based on typing.
     */
    public KeyListener AUTO_NAVIGATION_KEY_LISTENER;
    
    /**
     * Variable for the TableSettings for this component.
     */
    public TableSettings SETTINGS;
    
    /**
     * Variable for the SpeedRenderer for all components.
     */
    protected static final TableCellRenderer SPEED_RENDERER =
        new SpeedRenderer();

    /**
     * Variable for the ProgressBarRenderer for all components.
     */
    protected static final TableCellRenderer PROGRESS_BAR_RENDERER =
        new ProgressBarRenderer();

    /**
     * Variable for the ChatRenderer for all components.
     */
    protected static final TableCellRenderer CHAT_RENDERER =
        new ChatRenderer();

    /**
     * Variable for the ColorRenderer for all components.
     */
    protected static final TableCellRenderer COLOR_RENDERER =
        new ColorRenderer();
        
    /**
     * Variable for the IconRenderer for all components.
     */
    protected static final TableCellRenderer ICON_RENDERER =
        new IconRenderer();
        
    /**
     * Variable for the IconAndNameRenderer for all components.
     */
    protected static final TableCellRenderer ICON_AND_NAME_RENDERER =
        new IconAndNameRenderer();
        
    /**
     * Variable for the default renderer for all components.
     */
    protected static final TableCellRenderer DEFAULT_RENDERER =
        new DefaultTableCellRenderer();
        
    /**
     * Variable for the centered renderer.
     */
    protected static final TableCellRenderer CENTER_RENDERER =
        new CenteredRenderer();
        
    /**
     * Variable for the License renderer.
     */
    protected static final TableCellRenderer LICENSE_RENDERER = 
        new LicenseRenderer();
    
    /** Variable for the date renderer. */
    protected static final TableCellRenderer DATE_RENDERER =
        new DateRenderer();
    
    /** Renderer for strings that could be HTML. */
    protected static final TableCellRenderer LINK_RENDERER =
        new LinkRenderer();

    /**
     * A zero dimension to be used in all tables.
     */
    protected static final Dimension ZERO_DIMENSION = new Dimension(0, 0);

    /**
     * Resorter -- for doing real-time resorts.
     */
    protected Resorter RESORTER = new Resorter();

    /**
     * The <tt>Component</tt> containing the <tt>JScrollPane</tt> for the
     * table.
     */
    protected JComponent TABLE_PANE;

    /**
     * The <tt>JScrollPane</tt> instance for scrolling through the table.
     */
    protected JScrollPane SCROLL_PANE;
	
	/**
	 * Is true when the table is currently resorted. Should only be used by 
	 * package internal event listeners, which want to suppress events during
	 * resorting.
	 */
	protected boolean isResorting = false;

    /**
     * Basic constructor that uses a Template Pattern to delegate the
     * setup functions to individual methods.  The following methods
     * are called in the order they are listed.<p>
     * <ul>
     * <li> updateSplashScreen </li>
     * <li> buildSettings </li>
     * <li> buildListeners </li>
     * <li> setupConstants </li>
     * <li> setupTable </li>
     * <li> setupDragAndDrop </li>
     * <li> addActions </li>
     * <li> addListeners </li>
     * <li> setDefaultRenderers </li>
     * <li> setDefaultEditors </li>
     * <li> setupMainPanel </li>
     * <li> setupTableHeaders </li>
     * <li> handleNoSelection </li>
     * </ul>
     * Of these, some have are already written as default implementions.
     * The extending class should call GUIMediator.addRefreshListener(this)
     * if they want to be a refreshListener,
     * and ThemeMediator.addThemeObserver(this) if they want to be
     * a ThemeObserver.
     */
    protected AbstractTableMediator(String id) {
        this.ID = id;
        this.updateSplashScreen();
        this.buildSettings();
        this.buildListeners();
        this.setupConstants();
        //Assert.that(MAIN_PANEL != null, "MAIN_PANEL not set.");
        assert DATA_MODEL != null : "DATA_MODEL not set.";
        assert TABLE != null : "TABLE not set.";
        this.setupTable();
        this.setupDragAndDrop();
        this.addActions();
        this.addListeners();
        this.setDefaultRenderers();
        this.setDefaultEditors();
        this.setupMainPanel();
        this.setupTableHeaders();
        this.handleNoSelection();
    }
    
    /**
     * Sets up Drag & Drop for the table.
     * Default implementation does nothing.
     *
     * This is called prior to addListeners, because D&D wraps all
     * Mouse[Motion]Listeners behind a proxy, and we don't need to
     * proxy listeners added from here.
     */
    protected void setupDragAndDrop() {}

    /**
     * Intended for updating the splash screen while this component loads.
     */
    protected abstract void updateSplashScreen();
    
    /**
     * Retrieves or builds the correct settings.
     */
    protected void buildSettings() {
        SETTINGS = new TableSettings(ID);
    }

    /**
     * Intended for setting the DATA_MODEL and TABLE constants.
     * Optionally, MAIN_PANEL, BUTTON_ROW, and POPUP_MENU can also be set.
     */
    protected abstract void setupConstants();

    /**
     * Assigns the listeners to their slots.
     * This must be done _before_ the individual components
     * are created, incase any of them want to use a listener.
     * Extending components that want to build extra listeners
     * should call super.buildListeners and then build their own.
     * DEFAULT_LISTENER, SELECTION_LISTENER, HEADER_LISTENER,
     * and REMOVE_LISTENER are created by default.
     */
    protected void buildListeners() {
        DEFAULT_LISTENER = new DefaultMouseListener(this);
        SELECTION_LISTENER = new SelectionListener(this);
        HEADER_LISTENER = new HeaderMouseListener(this);
        REMOVE_LISTENER = new RemoveListener(this);
        AUTO_NAVIGATION_KEY_LISTENER = new KeyTypedMover();
    }

    /**
     * Adds the listeners to the table.
     * Extending components that want to add extra listeners
     * should call super.addListeners and then add their own.
     * DEFAULT_LISTENER, SELECTION_LISTENER and HEADER_LISTENER
     * are added by default.
     */
    protected void addListeners() {
        TABLE.addMouseListener(DEFAULT_LISTENER);
        TABLE.getSelectionModel().addListSelectionListener(SELECTION_LISTENER);
        TABLE.getTableHeader().addMouseListener(HEADER_LISTENER);
        TABLE.getTableHeader().addMouseMotionListener(HEADER_LISTENER);
        TABLE.addKeyListener(AUTO_NAVIGATION_KEY_LISTENER);
    }

    /**
     * Sets row heights a little larger than normal, turns off
     * the display of the grid and disallows column selections.
     */
    protected void setupTable() {
        TABLE.setRowHeight(TABLE.getRowHeight() + 1);
        TABLE.setShowGrid(false);
        TABLE.setIntercellSpacing(ZERO_DIMENSION);
        TABLE.setColumnSelectionAllowed(false);
        TABLE.setTableSettings(SETTINGS);
        TABLE.getTableHeader().addMouseListener(new FlexibleColumnResizeAdapter());
    }
    
    /**
     * Add input/action events to the table.
     *
     * Currently sets the 'action' key to call 'handleActionKey'.
     */
    protected void addActions() {
        InputMap map =
            TABLE.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        Action enter = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                handleActionKey();
            }
        };
        installAction(map, enter, KeyEvent.VK_ENTER, "limewire.action");
        
        Action delete = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!TABLE.isEditing() || TABLE.getCellEditor() instanceof DeletableCellEditor)
                    removeSelection();
            }
        };
        installAction(map, delete, KeyEvent.VK_BACK_SPACE, "limewire.delete");
        installAction(map, delete, KeyEvent.VK_DELETE, "limewire.delete");
    }
    
    /**
     * Installs an action to the table with the specified key & signifier.
     */
    private void installAction(InputMap map, Action action, int key, String sig) {
        KeyStroke stroke = KeyStroke.getKeyStroke(key, 0);
        Object obj = map.get(stroke);
        // If the action already exists in the input map, just refocus
        // the action map.
        if(obj != null) {
            TABLE.getActionMap().put(obj, action);
        } else {
        // Otherwise, install a new entry into both the input & action map.
            map.put(stroke, sig);
            TABLE.getActionMap().put(sig, action);
        }
    }

    /**
     * Intended for adding default renderers to the table.
     * Extending components that want to add extra default renderers
     * should call super.setDefaultRenderers and then add their own.
     * By default, PROGRESS_BAR_RENDERER is assigned to
     * ProgressBarHolder.class, CHAT_RENDERER is assigned to
     * ChatHolder.class, COLOR_RENDERER is assigned to
     * ColoredCell.class, ICON_RENDERER is assigned to Icon.class,
     * and ICON_AND_NAME_RENDERER is assigned to IconAndNameHolder.class.
     */
    protected void setDefaultRenderers() {
        TABLE.setDefaultRenderer(ProgressBarHolder.class, PROGRESS_BAR_RENDERER);
        TABLE.setDefaultRenderer(ChatHolder.class, CHAT_RENDERER);
        TABLE.setDefaultRenderer(ColoredCell.class, COLOR_RENDERER);
        TABLE.setDefaultRenderer(Icon.class, ICON_RENDERER);
        TABLE.setDefaultRenderer(IconAndNameHolder.class, ICON_AND_NAME_RENDERER);
        TABLE.setDefaultRenderer(Object.class, DEFAULT_RENDERER);
        TABLE.setDefaultRenderer(CenteredHolder.class, CENTER_RENDERER);
        TABLE.setDefaultRenderer(License.class, LICENSE_RENDERER);
        TABLE.setDefaultRenderer(SpeedRenderer.class, SPEED_RENDERER);
        TABLE.setDefaultRenderer(Date.class, DATE_RENDERER);
    }

    /**
     * Intended for setting up default editors.  By default,
     * no editors are added.
     */
    protected void setDefaultEditors() { }

    /**
     * Sets up the MAIN_PANEL to have a uniform look among all tables.
     * If the MAIN_PANEL is not created, nothing will happen.
     */
    protected void setupMainPanel() {
        if (MAIN_PANEL != null) {
            MAIN_PANEL.add(getScrolledTablePane());
            if (BUTTON_ROW != null) {
                MAIN_PANEL.add(Box.createVerticalStrut(GUIConstants.SEPARATOR));
                MAIN_PANEL.add(BUTTON_ROW);
            }
            MAIN_PANEL.setMinimumSize(ZERO_DIMENSION);
        }
    }

    /**
     * Organizes the table headers so that they're sized correctly,
     * in the correct order, and are either visible or not visible,
     * depending on the user's preferences.
     */
    protected void setupTableHeaders() {
        ColumnPreferenceHandler cph = createDefaultColumnPreferencesHandler();
        cph.setWidths();
        cph.setOrder();
        cph.setVisibility();
        TABLE.setColumnPreferenceHandler(cph);
    }

    /**
     * Creates the ColumnPreferencesHandler.
     *
     * Extending classes should override this to use a custom
     * ColumnPreferencesHandler.
     */
    protected ColumnPreferenceHandler createDefaultColumnPreferencesHandler() {
        return new DefaultColumnPreferenceHandler(TABLE);
    }

    /**
     * Sets up & gets the table inside a JScrollPanel inside a JPanel.
     */
    protected JComponent getScrolledTablePane() {
        // if it already exists, return it
        if (TABLE_PANE != null)
            return TABLE_PANE;

        JPanel tablePane = new JPanel();
        tablePane.setLayout(new BoxLayout(tablePane, BoxLayout.Y_AXIS));

        SCROLL_PANE = new JScrollPane(TABLE);
        //Add the nice 3d bevel to the top-right corner
        JPanel corner = new JPanel();
        corner.setBackground(TABLE.getTableHeader().getBackground());
        corner.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        SCROLL_PANE.setCorner(JScrollPane.UPPER_RIGHT_CORNER, corner);
        tablePane.add(SCROLL_PANE);

        TABLE_PANE = tablePane;
        updateTheme();
        return tablePane;
    }

    // inherit doc comment
    public void updateTheme() {
        Color tableColor = ThemeFileHandler.TABLE_BACKGROUND_COLOR.getValue();
        if (TABLE_PANE == null) return;

        TABLE_PANE.setBackground(tableColor);
        TABLE.setBackground(tableColor);
        TABLE.setOpaque(true);
        SCROLL_PANE.getViewport().setBackground(tableColor);
    }

    /**
     * Add a new DataLine initialized by Object o to the list,
     * tell the dataModel there was a row inserted,
     * and unselect the first row (to address the java bug)
     */
    public void add(I o) {
        add(o, -1);
    }

    /**
     * Adds a new DataLine initialized by Object o to the list at
     * index i. If the list is sorted or the insert is beyond the list bounds
     * the add falls back to the default add(Object o)
     * 
     * @param o - object to add
     * @param index - index to insert into if the list is not sorted
     */
    public void add(I o, int index ) {
        if (TABLE.isEditing()) {
            CellEditor editor = TABLE.getCellEditor();
            editor.cancelCellEditing();
        }

        boolean inView = TABLE.isSelectionVisible();

        int addedAt;
        if (SETTINGS.REAL_TIME_SORT.getValue() && DATA_MODEL.isSorted())
            addedAt = DATA_MODEL.addSorted(o);
        else {
            if( index >= 0 && index < DATA_MODEL.getRowCount()) {
                addedAt = DATA_MODEL.add(o, index);
            }
            else
                addedAt = DATA_MODEL.add(o);
        }
        
        // if it was added...
        fixSelection(addedAt, inView);
    }

    /**
     * Forces the object to be added unsorted.
     */
    public void addUnsorted(I o) {
        if (TABLE.isEditing()) {
            CellEditor editor = TABLE.getCellEditor();
            editor.cancelCellEditing();
        }
        
        boolean inView = TABLE.isSelectionVisible();
        int addedAt = DATA_MODEL.add(o);
        fixSelection(addedAt, inView);
    }
    
    /**
     * Removes the selection from where the row was added,
     * and puts the focus on a previously selected row.
     */
    protected void fixSelection(int addedAt, boolean inView) {
        if ( addedAt >= 0 && addedAt < DATA_MODEL.getRowCount() ) {
            // unselect the row to address a Java bug
            // (if the previous row was selected,
            //  then the newly added one will be selected also)
            TABLE.removeRowSelectionInterval(addedAt, addedAt);
            // (and must reselect an older row, 'cause unselecting moves
            //  the traversing focus)
            int selected = TABLE.getSelectedRow();
            if(selected >= 0 && selected < DATA_MODEL.getRowCount()) {
                TABLE.addRowSelectionInterval(selected, selected);
                if(inView)
                    TABLE.ensureRowVisible(selected);
            }
        }
    }

    /**
     * Removes the row associated with the Object o
     * Delegates to removeRow(int)
     * If no matching row is found, nothing is done.
     */
    public void remove(I o) {
        int idx = DATA_MODEL.getRow(o);
        if (idx != -1) removeRow(idx);
    }

    /**
     * Removes the row.
     */
    public void removeRow(int row) {
        DATA_MODEL.remove(row);
    }
    
    /**
     * Moves a row in the table to a new location. 
     * @param oldLocation - table row to move
     * @param newLocation - location to insert the table row after it had been removed
     */
    public void moveRow(int oldLocation, int newLocation) {
        if( oldLocation < 0 || oldLocation >= DATA_MODEL.getRowCount() )
            throw new IllegalArgumentException("index " + oldLocation + " must be >= 0 and < TABLE.getRowCount()");
        if( newLocation < 0 || newLocation >= DATA_MODEL.getRowCount())
            throw new IllegalArgumentException("add index " + newLocation + " must be >= 0 and < TABLE.getRowCount()");
        E e = DATA_MODEL.get(oldLocation);
        DATA_MODEL.remove(oldLocation);
        DATA_MODEL.add(e, newLocation);
    }

    /**
     * Implements RefreshListener
     * Wraps the doRefresh call so that extending classes
     * can maintain the resort & isShowing checks.
     * Extending classes should NOT override this.
     * Instead, they should override doRefresh.
     */
     public void refresh() {
        // We only want to refresh if the table is showing.
        // This saves on traversing through every item in non visible tables.
        // This will cause at most a one second lag on updating
        // statistics if a user switches to the tab.
        // There probably are ways around the lag (such as intercepting the
        // call and manually running a refresh) if necessary.
        // The lag will only be visible, currently, on the
        // Upload & Download tables, since they cache data instead
        // of acting directly on the respective loaders.
        if (TABLE.isShowing()) {
            doRefresh();
            resort();
        }
    }

    /**
     * Exists for extending classes to overwrite.
     * Mostly used for updating the values of buttons/menu-items
     * With the return value of the DATA_MODEL's refresh.
     */
    protected void doRefresh() {
        DATA_MODEL.refresh();
    }

    /**
     * Tells the model to update a specific DataLine
     */
    public void update(I o) {
        DATA_MODEL.update(o);
        resort();
    }

    /**
     * Resorts the underlying data. Maintains highlighted rows.
     */
    public void resort() {
        RESORTER.doResort(false);
    }

    /**
     * Resorts the underlying data, regardless of if the column is dynamic.
     */
    public void forceResort() {
        RESORTER.doResort(true);
    }

    /**
     * @return The main component which has all components
     */
    public JComponent getComponent() {
        return MAIN_PANEL;
    }

    /**
     * Removes all selected rows from the list
     * and fires deletions through the dataModel
     * 
     * Cancels any editing that may be occuring prior to updating the model, we
     * must do this since editing will be occuring on the row that will be
     * removed
     */
    public void removeSelection() {
        if (TABLE.isEditing()) {
            CellEditor editor = TABLE.getCellEditor();
            editor.cancelCellEditing();
        }

        int[] sel = TABLE.getSelectedRows();
        Arrays.sort(sel);
        for (int counter = sel.length - 1; counter >= 0; counter--) {
            int i = sel[counter];
            DATA_MODEL.remove(i);
        }
        clearSelection();
    }

    /**
     * Creates a new ColumnSelectionMenu JPopupMenu.
     * If the table wants to use a custom popup menu, override this
     * method and do something different.
     */
    protected JPopupMenu createColumnSelectionMenu() {
        return (new ColumnSelectionMenu(TABLE)).getComponent();
    }
    
    /**
     * Forwards a double click to the 'action key'.
     */
    public void handleMouseDoubleClick(MouseEvent e) {
        handleActionKey();
    }

    /**
     * Changes the selection in the table in response to a right-mouse click.
     * This was contributed by Chance Moore originally for the search package.
     */
    public void handleRightMouseClick(MouseEvent e) {
        Point p = e.getPoint();
        int row = TABLE.rowAtPoint(p);
        //check its valid, should always be but cheap to check
        if (row < 0) return;
        if (!TABLE.getSelectionModel().isSelectedIndex(row)) {
            //if right clicked row is not selected, make it so
            TABLE.getSelectionModel().setSelectionInterval(row, row);
        }
    }

    /**
     * Shows the popup menu at Point p
     */
    public void handlePopupMenu(MouseEvent e) {
        Point p = e.getPoint();
        handleRightMouseClick(e);

        JPopupMenu menu = createPopupMenu();
        if(menu != null) {
            try {
                menu.show(TABLE, p.x+1, p.y-6);
            } catch(IllegalComponentStateException icse) {
                // happens occasionally, ignore.
            }
        }
    }

    /**
     * Sorts the column whose header maps to the given point
     */
    public void handleHeaderColumnLeftClick(Point p) {
        JTableHeader th = TABLE.getTableHeader();
        int col = th.columnAtPoint(p);
        int c = TABLE.convertColumnIndexToModel(col);
        int oldC = DATA_MODEL.getSortColumn();
        if (c != -1) {
            sortAndMaintainSelection(c);
            // force the headers to repaint with new icons.
            th.repaint(th.getHeaderRect(col));
            if(oldC != -1 && oldC != c) {
                int oldCol = TABLE.convertColumnIndexToView(oldC);
                th.repaint(th.getHeaderRect(oldCol));
            }
        }
    }

    /**
     * Show the column selection menu.
     */
    public void handleHeaderPopupMenu(Point p) {
        createColumnSelectionMenu().show(TABLE.getTableHeader(), p.x+1, p.y-6);
    }

    /**
     * Tell the table something is pressed.
     */
    public void handleHeaderColumnPressed(Point p) {
        JTableHeader th = TABLE.getTableHeader();
        int col = th.columnAtPoint(p);
        int c = TABLE.convertColumnIndexToModel(col);
        if (c != -1) {
            TABLE.setPressedColumnIndex(c);
            // force the table to redraw the column header
            th.repaint(th.getHeaderRect(col));
        }
    }

    /**
     * Tell the table something is not pressed.
     */
    public void handleHeaderColumnReleased(Point p) {
        TABLE.setPressedColumnIndex(-1);

        JTableHeader th = TABLE.getTableHeader();
        int col = th.columnAtPoint(p);
        if (col != -1)
            // force the table to redraw the column header
            th.repaint(th.getHeaderRect(col));
    }

    /**
     * Delegates the setButtonEnabled call to the ButtonRow
     */
    public void setButtonEnabled(int buttonIdx, boolean enabled) {
        if (BUTTON_ROW != null)
            BUTTON_ROW.setButtonEnabled(buttonIdx, enabled);
    }
    
    /**
     * Gets the size of the underlying model.
     */
    public int getSize() {
        return DATA_MODEL.getRowCount();
    }

    /**
     * Clear the table of all items
     */
    public void clearTable() {
        DATA_MODEL.clear();
        handleNoSelection();
    }

    /**
     * Helper-function to clear selected items.
     */
    protected void clearSelection() {
        TABLE.clearSelection();
        handleNoSelection();
    }

    /**
     * Sorts the DATA_MODEL and maintains selections in the TABLE.
     * If columnToSort is -1, it will resort the current column.
     * Otherwise it will sort columnToSort.
     */
    @SuppressWarnings("unchecked")
    protected void sortAndMaintainSelection(int columnToSort) {
        // store the currently selected rows
        int[] rows = TABLE.getSelectedRows();
        DataLine[] dls = new DataLine[rows.length];
        Object inView = null;
        for (int i = 0; i < rows.length; i++) {
            dls[i] = DATA_MODEL.get(rows[i]);
            if(inView == null && TABLE.isRowVisible(rows[i]))
                inView = dls[i];
        }

        // do the sorting
        if (columnToSort == -1)
            DATA_MODEL.resort();
        else
            DATA_MODEL.sort(columnToSort);

        // reselect the rows.
        for(int i = 0; i < rows.length; i++) {
            int sel = DATA_MODEL.getRow((E)dls[i]);
            TABLE.addRowSelectionInterval(sel, sel);
            if(inView == dls[i]) {
                TABLE.ensureRowVisible(sel);
                inView = null;
            }            
        }
    }

    /**
     * Abstract method for creating a right-click popup menu for the
     * table.  If an implemention does not support a right-click
     * popup menu, it should return <tt>null</tt>.
     *
     * @return a new <tt>JPopupMenu</tt> to display on right-click
     */
    protected abstract JPopupMenu createPopupMenu();

    protected final class Resorter implements Runnable {
        private boolean active = false;
        private boolean force = false;

        /**
         * To save processor usage, only resort those tables that are
         * currently showing, are sorting in real time, and are not
         * actively being sorted.
         */
        public void doResort(boolean isForce) {
            // TABLE.isShowing() should be checked last, since it's a
            // recursive call (and thus the most expensive)
            // through all the parents of the table.
            if ( !active && SETTINGS.REAL_TIME_SORT.getValue() &&
                 TABLE.isShowing() ) {
                active = true;
                SwingUtilities.invokeLater(this);
            }
            force |= isForce;
        }

        /**
         * Iff the data model needs resorting (meaning the selected
         * column is a 'dynamic' column), then do the resort.
         * We need to remember what was selected because resorting
         * will invalidate the selections.
         * We cannot do it while the table is editing because
         * editing overrides what is displaying in that cell,
         * making the user think they are still editing the correct
         * line, when infact it has moved.
         */
        public void run() {
            try {
                if (!TABLE.isEditing() &&
                    (force || DATA_MODEL.needsResort())) {
					isResorting = true;
                    sortAndMaintainSelection(-1);
					isResorting = false;
                }
            } catch (Exception e) {}
            active = false;
            force = false;
        }

    }
    
    /**
     * Convenience method to generically compare any two comparable
     * things.
     *
     * Handles comparison uniquely for 'native' types.
     * We want to compare strings by lowercase comparison
     * Note that non-integer comparisons must specifically
     * check if the difference is less or greater than 0
     * so that rounding won't be wrong.
     * Of the native types, we check 'Integer' first since
     * that's the most common, Boolean,
     * then Double or Float, and finally, the rest will be caught in
     * 'Number', which just uses an int comparison.
     */
    @SuppressWarnings("unchecked")
    static int compare(Object o1, Object o2) {
        int retval;
        
        if ( o1 == null && o2 == null ) {
            retval = 0;
        } else if ( o1 == null ) {
            retval = -1;
        } else if ( o2 == null ) {
            retval = 1;
        } else if ( o1.getClass() == String.class ) {
            retval = StringUtils.compareFullPrimary( (String)o1, (String)o2 );
        } else if( o1 instanceof java.lang.Comparable ) {
            retval = ((java.lang.Comparable)o1).compareTo(o2);
        } else {
            retval = 0;
        }
        return retval;
    }

	/**
	 * Returns whether the table is resorting. 
	 */
	boolean isResorting() {
		return isResorting;
	}
}
