package com.limegroup.gnutella.gui.search;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.limewire.setting.BooleanSetting;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.dnd.DNDUtils;
import com.limegroup.gnutella.gui.tables.CircularIcon;
import com.limegroup.gnutella.gui.tables.IconAndNameHolder;
import com.limegroup.gnutella.gui.tables.SortArrowIcon;
import com.limegroup.gnutella.gui.themes.ThemeFileHandler;
import com.limegroup.gnutella.gui.xml.XMLValue;

/**
 * A listbox with a header.
 *
 * Except for the header, all backgrounds are opaque.
 */
class FilterBox extends JPanel {    
    /**
     * The renderer to use on all lists.
     */
    private static final ListCellRenderer RENDERER = new Renderer();
    
    /**
     * The listener that ensures the selected row is always visible.
     */
    private static final ListSelectionListener MOVER = new Mover();
    
    /**
     * The setting that controls row striping.
     *
     * (Ideally we wouldn't reference ResultPanel, but it's easiest.)
     */
    private static final BooleanSetting STRIPE_ROWS = 
        ResultPanel.SEARCH_SETTINGS.ROWSTRIPE;
    
    /**
     * The string to use for 'Options'.
     */
    private static final String OPTIONS =
        I18n.tr("Options");
        
    /**
     * The string to use for 'Option'.
     */
    private static final String OPTION =
        I18n.tr("Option");
    
    /**
     * The property name stored within the JList that keeps the currently
     * selected value.
     *
     * This is used when the contents of the model change so that we can
     * reselect the old value.
     */
    private static final String SELECTED = "SELECTION";
    
    /**
     * The property named stored within the JList that keeps the current
     * matching value.
     */
    private static final String MATCH = "MATCH";
    
    /**
     * The property name stored within the JList that keeps the current
     * matching index.
     */
    private static final String MATCH_IDX = "MATCH_IDX";
    
    /**
     * The ditherer drawing the title of the box.
     */
    private final Ditherer DITHERER =
            new Ditherer(10,
                        ThemeFileHandler.FILTER_TITLE_TOP_COLOR.getValue(), 
                        ThemeFileHandler.FILTER_TITLE_COLOR.getValue()
                        );    
    
    
    /**
     * The JLabel with the title.
     */
    protected final JLabel TITLE;
    
    /**
     * The panel with the title in it.
     */
    protected final JPanel TITLE_PANEL;
    
    /**
     * The JList with the list choices.
     */
    protected final JList LIST;
    
    /**
     * The panel the list is contained in.
     */
    protected final JPanel LIST_PANEL;    
    
    /**
     * The delegate model for our JList.
     */
    protected final ListModelDelegator DELEGATOR;
    
    /**
     * The label containing the icon to restore, minimize or maximize.
     */
    protected final JLabel CONTROLS;
    
    /**
     * The MetadataModel from which the selectors should
     * extract their values.
     */
    protected final MetadataModel MODEL;
    
    /**
     * The only ChangeEvent to ever use.
     */
    protected final ChangeEvent EVENT = new ChangeEvent(this);
    
    /**
     The selector that this FilterBox is acting on.
     */
    protected Selector _selector;
    
    /**
     * The ChangeListener for selectors.
     *
     * TODO: Allow more than one.
     */
    protected ChangeListener _selectorChangeListener;
    
    /**
     * The listener for the state of this filter box (minimized/maximized)
     *
     * TODO: Allow more than one.
     */
    protected ChangeListener _stateChangeListener;
    
    /**
     * The current state of this filter box (whether or not it is minimized)
     */
    private boolean _minimized = false;
    
    /**
     * Whether or not we are allowed to minimize.
     */
    private boolean _canMinimize = true;
    
    /**
     * Whether or not the mouse has been clicked on the box's selection area.
     * If it has, we stop doing 'point scoring' to select the closest matching
     * value.
     */
    private boolean _mouseClicked = false;
    
    /**
     * The currently requested value (full string).
     */
    private String _requestedValue;
    
    /**
     * The currently requested value split into tokens.
     */
    private String[] _requestedValues;
    
    /**
     * Constructs a new FilterBox with the specified model & selector.
     */
    FilterBox(MetadataModel model, Selector selector) {
        super();
        setLayout(new BorderLayout());
        if(model == null)
            throw new NullPointerException("no model");
        if(selector == null)
            throw new NullPointerException("no selector");

        CONTROLS = new JLabel();
        TITLE = new JLabel();
        TITLE.setFont(UIManager.getFont("Table.font.bold"));        
        TITLE_PANEL = createTitlePanel(TITLE, CONTROLS);
        LIST = new JList();
        LIST.setTransferHandler(DNDUtils.DEFAULT_TRANSFER_HANDLER);
        DELEGATOR = new ListModelDelegator();
        JScrollPane pane = new JScrollPane(LIST);
        LIST_PANEL = addToPanel(pane, false);
        MODEL = model;
        
        add(TITLE_PANEL, BorderLayout.NORTH);
        add(LIST_PANEL, BorderLayout.CENTER);

        LIST.setBackground(ThemeFileHandler.TABLE_BACKGROUND_COLOR.getValue());
        LIST.setCellRenderer(RENDERER);
        LIST.addListSelectionListener(MOVER);
        LIST.setModel(DELEGATOR);
        LIST.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        LIST.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
            public void mousePressed(MouseEvent e) {
                _mouseClicked = true;
                LIST.removeMouseListener(this);
            }
            public void mouseReleased(MouseEvent e) {}
        });

        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createLoweredBevelBorder())
        );

        pane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            
        setSelector(selector);
    }
    
    /**
     * Returns the list used in this box.
     */
    JList getList() {
        return LIST;
    }
    
    /**
     * Gets the component that should display.
     * If minimized, only the TITLE_PANEL.
     * Otherwise, both title & list.
     */
    JComponent getComponent() {
        if(_minimized) {
            return TITLE_PANEL;
        } else {
            removeAll();
            add(TITLE_PANEL, BorderLayout.NORTH);
            add(LIST_PANEL, BorderLayout.CENTER);
            return this;
        }
    }
   
    
    /**
     * Minimizes the list.
     */
    void minimize() {
        _minimized = true;
        _selector.setMinimized(true);
        TITLE.setFont(UIManager.getFont("Table.font"));
        TITLE_PANEL.setBorder(getBorder());
        invalidate();
        CONTROLS.setIcon(SortArrowIcon.getAscendingIcon());
        updateTitle();
        revalidate();
        if(_stateChangeListener != null)
            _stateChangeListener.stateChanged(EVENT);
    }
    
    /**
     * Restores the list.
     */
    void restore() {
        _minimized = false;
        _selector.setMinimized(false);
        TITLE.setFont(UIManager.getFont("Table.font.bold"));
        TITLE_PANEL.setBorder(null);
        updateTitle();
        CONTROLS.setIcon(SortArrowIcon.getDescendingIcon());        
        revalidate();
        if(_stateChangeListener != null)
            _stateChangeListener.stateChanged(EVENT);
    }
    
    /**
     * Determines whether or not minimizing is allowed.
     */
    void setCanMinimize(boolean allowed) {
        if(_minimized)
            return;
        
        if(allowed) {
            CONTROLS.setIcon(SortArrowIcon.getDescendingIcon());
        } else {
            CONTROLS.setIcon(null);
        }
        _canMinimize = allowed;
    }
    
    /**
     * Returns whether or not this box is minimized.
     */
    boolean isMinimized() {
        return _minimized;
    }
    
    /**
     * Returns the MetadataModel used to build the lists.
     */
    MetadataModel getMetadataModel() {
        return MODEL;
    }
    
    /**
     * Returns the active selector.
     */
    Selector getSelector() {
        return _selector;
    }
    
    /**
     * Sets the new ChangeListener for selector-changing events.
     */
    void setSelectorChangeListener(ChangeListener listener) {
        _selectorChangeListener = listener;
    }
    
    /**
     * Sets the new ChangeListener for state-changing events.
     */
    void setStateChangeListener(ChangeListener listener) {
        _stateChangeListener = listener;
    }
    
    /**
     * Sets a new active selector.
     */
    void setSelector(Selector selector) {
        if(selector == null)
            throw new NullPointerException("no selector");
    
        // erase selections & matching values.
        LIST.putClientProperty(MATCH, null);
        LIST.putClientProperty(MATCH_IDX, null);
        LIST.putClientProperty(SELECTED, null);

        ListModelMap oldModel = 
                    _selector==null ? null : MODEL.getListModelMap(_selector);
        _selector = selector;
        ListModelMap newModel = MODEL.getListModelMap(selector);
        setModel(newModel);
        DELEGATOR.changeListener(oldModel, newModel);

        if(selector.isMinimized())
            minimize();
        
        if(_selectorChangeListener != null)
            _selectorChangeListener.stateChanged(EVENT);

        updateTitle();
    }
    
    /**
     * Updates the text in the title.
     */
    void updateTitle() {
        Object sel = getSelectedValue();
        String title = getTitle(_selector);
        String oldTitle = TITLE.getText();
        if(!_minimized) {
            TITLE.setText(title);
        } else {
            String extra;
            if(sel == null || MetadataModel.isAll(sel)) {
                int size = DELEGATOR.getSize() -1;
                if(size == 1)
                    extra = size + " " + OPTION;
                else
                    extra = size + " " + OPTIONS;
            } else {
                extra = sel.toString();
            }
            TITLE.setText(title + " (" + extra + ")");
        }
        if(!oldTitle.equals(TITLE.getText()))
            TITLE.setPreferredSize(new Dimension(GUIUtils.width(TITLE), 13));
    }    
    
    /**
     * Returns the currently selected item.
     */
    Object getSelectedValue() {
        int idx = LIST.getSelectedIndex();
        if(idx < 0 || idx >= DELEGATOR.getSize())
            return null;
        else
            return LIST.getSelectedValue();
    }
    
    /**
     * Sets the value that we want to be selected if it arrives.
     */
    void setRequestedValue(String value) {
        _mouseClicked = false;
        _requestedValue = value.trim().toLowerCase();
        _requestedValues = StringUtils.split(_requestedValue, ' ');
        selectValueFromScore();
    }
    
    /**
     * Clears the selected value on the box.
     */
    void clearSelection() {
        LIST.putClientProperty(SELECTED, null);
        LIST.clearSelection();
    }
    
    /**
     * Sets the model of the underlying JList.
     */
	void setModel(ListModelMap view) {
        Object selected = LIST.getClientProperty(SELECTED);
	    DELEGATOR.setDelegate(view);
	    if(selected != null) {
	        int index = indexOf(selected);
	        if(index != -1) {
	            setSelectedIndex(index, true);
	            selectMatchingValue(false);
            } else {
                LIST.clearSelection();
                selectMatchingValue(true);
            }
        } else {
            // Make sure nothing is set.
            LIST.clearSelection();
            selectMatchingValue(true);            
        }
        updateTitle();
    }
    
    /**
     * Retrieves the model of the underlying list.
     */
    ListModelMap getModel() {
        return DELEGATOR.getDelegate();
    }    
    
    /**
     * Adds a ListSelectionListener to the underlying JList & 
     * A ListDataListener to our model delegator.
     */
    void addSelectionListener(ListDataListener ldl, ListSelectionListener lsl) {
        LIST.addListSelectionListener(lsl);
        DELEGATOR.addListDataListener(ldl);
    }
    
    /**
     * Creates the title JPanel, including the 'title' label
     * and the 'controls' label.
     */
    protected JPanel createTitlePanel(JLabel title, JLabel controls) {
        title.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel button = new JLabel(CircularIcon.instance());
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point p = e.getPoint();
                JComponent source = (JComponent)e.getSource();
                SelectorMenu menu = new SelectorMenu(FilterBox.this);
                menu.getComponent().show(source, p.x+1, p.y-6);
            }
        });
        
        controls.setIcon(SortArrowIcon.getDescendingIcon());
        controls.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(_minimized)
                    restore();
                else if(_canMinimize)
                    minimize();
                    
            }
        });
        
        JPanel panel = new DitherPanel(DITHERER);
        panel.setBackground(ThemeFileHandler.FILTER_TITLE_COLOR.getValue());        
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 5, 0, 0);
        panel.add(button, c);
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 1, 0, 1);
        panel.add(title, c);
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 5);
        panel.add(controls, c);
        
        panel.setMaximumSize(new Dimension(9999999, CircularIcon.instance().getIconHeight()+4));

        return panel;
    }
        
    /**
     * Adds the specified component within a possibly opaque JPanel.
     */
    protected JPanel addToPanel(JComponent comp, boolean opaque) {
        JPanel panel = new JPanel(new GridLayout());
        panel.add(comp);
        panel.setMaximumSize(comp.getMaximumSize());
        panel.setPreferredSize(comp.getPreferredSize());
        return panel;
    }
    
    /**
     * Selects a possible value based on the scores of all possible choices.
     */
    private void selectValueFromScore() {
        if(_requestedValue == null)
            return;
            
        ListModelMap map = DELEGATOR.getDelegate();
        int highScore = 0;
        int index = -1;
        Object matchingValue = null;
        int i = 1; // start at one because of the 'All' option we're ignoring.
        for(Iterator iter = map.iterator(); iter.hasNext(); i++) {
            Object next = iter.next();
            String val;
            
            if(next instanceof XMLValue)
                val = next.toString();
            else if(next instanceof String)
                val = (String)next;
            else // only supports these two types for matching.
                continue;
            
            int score = score(val, highScore);
            if(score > highScore) {
                highScore = score;
                index = i;
                matchingValue = next;
            }
            // If we had a perfect match, fake a mouse click so we don't
            // score any more.
            if(highScore == 100) {
                _mouseClicked = true;
                break;
            }
        }
        if(index != -1) {
            LIST.putClientProperty(MATCH, matchingValue);
            LIST.putClientProperty(MATCH_IDX, new Integer(index));
            LIST.ensureIndexIsVisible(index);
        }
    }
    
    /**
     * Ensures that the matching value is visible.
     */
    private void selectMatchingValue(boolean scroll) {
        Object match = LIST.getClientProperty(MATCH);
        if(match != null) {
            int idx = indexOf(match);
            if(idx != -1) {
                LIST.putClientProperty(MATCH_IDX, new Integer(idx));
                if(scroll)
                     LIST.ensureIndexIsVisible(idx);
            } else {
                LIST.putClientProperty(MATCH_IDX, null);
            }
        } 
    }
    
    /** 
     * Returns the index of the value in the list's model.
     */
    private int indexOf(Object value) {
        ListModelMap view = DELEGATOR.getDelegate();
        if(view != null) {
            return view.indexOf(value);
        } else {
            return -1;
        }
    }
    
    /**
     * Sets the given index to be the selected index & optionally
     * scrolls to make it visible.
     */
    private void setSelectedIndex(int index, boolean scroll) {
        LIST.setSelectedIndex(index);
        if(scroll)
            LIST.ensureIndexIsVisible(index);
        LIST.repaint();
    }
    
    /**
     * Scores how close the given value matches the requested value.
     *
     * The scoring works the following way:
     *  - 100 points for exact matches.
     *  -  99 points for matches containing the substring
     *  -  +1 points for each word that matches
     */
    private int score(String value, int oldScore) {
        value = value.toLowerCase();
        
        // Exact match.
        if(_requestedValue.equals(value.trim()))
            return 100;
        
        // no point in trying any more.
        if(oldScore > 99)
            return 0;
        
        // Exact substring match
        if(value.indexOf(_requestedValue) > -1)
            return 99;
            
        // no point in trying anymore.
        if(oldScore > 98)
            return 0;
            
        // If more than one token, iterate for matches.
        if(_requestedValues.length == 1) {
            return 0;
        } else if(_requestedValues.length == oldScore) {
            // already have the highest possible score?
            return 0;
        } else {
            int matches = 0;
            for(int i = 0; i < _requestedValues.length; i++) {
                if(value.indexOf(_requestedValues[i]) > -1)
                    matches++;
            }
            return matches;
        }
    }

    /**
     * Determines the title of the specified selector.
     */
    private static String getTitle(Selector selector) {
        return selector.getTitle();
    }
    
    /**
     * A delegate list model so that we can register for change events on a
     * single model and just change the delegate model, without losing any
     * registered listeners.
     *
     * Also ensures that the list's selection is maintained when the contents
     * of the model change.
     */
    private class ListModelDelegator extends AbstractListModel 
                                            implements ListDataListener {
        /**
         * The delegate model.
         */
        private ListModelMap _delegate = null;
        
        /**
         * Sets a new delegate model, and calls for refresh
         */
        void setDelegate(ListModelMap delegate) {
            if(_delegate == delegate)
                return;
            _delegate = delegate;
            fireContentsChanged(this, 0, getSize());
        }
        
        /**
         * Unregisters this from listening for events on the old model, and
         * registers for events on the new model
         */
        void changeListener(ListModelMap oldModel, ListModelMap newModel) {
            // remove our old listener.
            if(oldModel != null)
                oldModel.removeListDataListener(this);
            // add a new listener.
            newModel.addListDataListener(this);
        }

        /**
         * Retrieves the delegate model.
         */
        ListModelMap getDelegate() {
            return _delegate;
        }
        
        /////////////////////////////////////////////////////////////////////
        // ListModel methods.
        // These delegate to the underlying model.
        
        /**
         * Returns the size of the delegate model.
         */
        public int getSize() {
            if(_delegate != null)
                return _delegate.getSize();
            else
                return 0;
        }
        
        /**
         * Returns the element at the delegate's index.
         */
        public Object getElementAt(int idx) {
            if(_delegate != null) 
                return _delegate.getElementAt(idx);
            else
                return null;
        }
        
        /////////////////////////////////////////////////////////////////////
        // Forwarding of ListDataEvents.
        // Note that these methods use the listenerList variable,
        // a protected variable from AbstractListModel, containing
        // the list of listeners.
        
        /**
         * Forwards interval added events from the delegate list.
         */
        public void intervalAdded(ListDataEvent e) {
            e = new ListDataEvent(this, e.getType(), 
                                  e.getIndex0(), e.getIndex1());
	        Object[] listeners = listenerList.getListenerList();
        	for (int i = listeners.length - 2; i >= 0; i -= 2)
        	    if (listeners[i] == ListDataListener.class)
        	        ((ListDataListener)listeners[i+1]).intervalAdded(e);
        }
        
        /**
         * Forwards interval removed events from the delegate list.
         */
        public void intervalRemoved(ListDataEvent e) {
            e = new ListDataEvent(this, e.getType(), 
                                  e.getIndex0(), e.getIndex1());
	        Object[] listeners = listenerList.getListenerList();
        	for (int i = listeners.length - 2; i >= 0; i -= 2)
        	    if (listeners[i] == ListDataListener.class)
        	        ((ListDataListener)listeners[i+1]).intervalRemoved(e);
        }
        
        /**
         * Forwards contents changed events from the delegate list.
         */
        public void contentsChanged(ListDataEvent e) {
            e = new ListDataEvent(this, e.getType(), 
                                  e.getIndex0(), e.getIndex1());
	        Object[] listeners = listenerList.getListenerList();
        	for (int i = listeners.length - 2; i >= 0; i -= 2)
        	    if (listeners[i] == ListDataListener.class)
        	        ((ListDataListener)listeners[i+1]).contentsChanged(e);
            
            // If the user hasn't manually selected anything on the list,
            // select a value based on the score of the wanted-selection
            // and the possible choices.
            // Otherwise (the user has selected something) maintain the
            // selection.
            if(!_mouseClicked && _requestedValue != null) {
                selectValueFromScore();
            } else {
                boolean matching = LIST.getClientProperty(MATCH) != null;
                Object selected = LIST.getClientProperty(SELECTED);
                if(selected != null) {
                    setSelectedIndex(indexOf(selected), true);
                    if(matching)
                        selectMatchingValue(false);
                } else if(matching) {
                    selectMatchingValue(true);
                }
            }
            updateTitle();
        }
    }
    
    /**
     * Removes the Renderer from it's parent (a CellRendererPane) to ensure
     * that the search is fully erased from memory.
     */
    public static void clearRenderer() {
        Container parent = ((Component)RENDERER).getParent();
        if(parent != null)
            parent.remove((Component)RENDERER);
    }   
    
    /**
     * The renderer for Filter Boxes.
     *
     * Supports drawing an icon & text if the value is an IconAndNameHolder,
     * or just the text (using the toString method) if anything else.
     *
     * Draws the line transparent unless it is selected.
     */
    private static class Renderer extends DefaultListCellRenderer {
        Renderer() {
            super();
        }
        
    	/**
    	 * Returns the <tt>Component</tt> that displays the icons & names
    	 * based on the <tt>IconAndNameHolder</tt> object.
    	 */
    	@Override
        public Component getListCellRendererComponent(JList list, Object value,
                                                   int idx, boolean isSelected,
                                                   boolean cellHasFocus) {
            Integer matchIdx = (Integer)list.getClientProperty(MATCH_IDX);
            boolean match = matchIdx != null && idx == matchIdx.intValue();
            setComponentOrientation(list.getComponentOrientation());

            if (isSelected) {
				if (match) {
					setFont(UIManager.getFont("Table.font.bold"));
				} else {
					setFont(UIManager.getFont("Table.font"));
				}
                setOpaque(true);
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                if(match) {
                    setFont(UIManager.getFont("Table.font.bold"));
                    setForeground(list.getForeground());
                    //TODO: ideally we would change the color also,
                    //      but what color should we use?
                    //setForeground(
                    //  ThemeFileHandler.FILTER_TITLE_COLOR.getValue());
                } else {
                    setFont(UIManager.getFont("Table.font"));                
                    setForeground(list.getForeground());
                }
                if(idx % 2 == 0 && STRIPE_ROWS.getValue()) {
                    setOpaque(true);
                    setBackground(ThemeFileHandler.TABLE_ALTERNATE_COLOR.getValue());
                } else {
                    setOpaque(false);
                }
            }
    	    
    	    if(value instanceof IconAndNameHolder) {
    	        IconAndNameHolder in = (IconAndNameHolder)value;
                setIcon(in.getIcon());
                setText(in.getName());
            } else {
                setIcon(null);
                setText((value == null) ? "" : value.toString());
            }
            setEnabled(list.isEnabled());
            setBorder((cellHasFocus) ? 
                UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
            return this;
        }
    }
    
    /**
     * The listener for list selection events, scrolls to the selected row.
     * A single one is used for all filter boxes.
     */
    private static class Mover implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
            if(event.getValueIsAdjusting())
                return;
            
            JList source = (JList)event.getSource();
            int selIndex = source.getSelectedIndex();
            if(selIndex != -1) {
                source.ensureIndexIsVisible(selIndex);
                source.putClientProperty(SELECTED, source.getSelectedValue());
            }
        }
    }
}

