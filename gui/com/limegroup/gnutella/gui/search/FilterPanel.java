package com.limegroup.gnutella.gui.search;

import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.limewire.util.MediaType;
import org.xml.sax.SAXException;

import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.xml.XMLValue;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.SchemaNotFoundException;

/**
 * The Panel that contains the various FilterBoxes.
 */
class FilterPanel extends BoxPanel {
    
	/**
	 * The current uniqueID.
	 */
	private static int uniqueIdCount = 0;

	/**
	 * The unique identifier (different than the GUID, which can change)
	 * for this FilterPanel.
	 */
	private final String ID;
	
    /**
     * The first filter box.
     */
    private final FilterBox BOX_1;
    
    /**
     * The second filter box.
     */
    private final FilterBox BOX_2;
    
    /**
     * The third filter box.
     */
    private final FilterBox BOX_3;
        
    /**
     * The ResultPanel this is filtering.
     */
    private final ResultPanel RESULTS;
    
    /**
     * The MetadataModel this is getting selectors from.
     */
    private final MetadataModel MODEL;
    
    /**
     * Constructs a new FilterPanel for the specified ResultPanel.
     *
     * The exact FilterBoxes added to FilterPanel are dependent on prior
     * user preferences.
     *
     */
    public FilterPanel(final ResultPanel results) {
        super(BoxLayout.Y_AXIS);
        
        String searchType = results.getMediaType().getSchema();
        Selector one = SelectorsHandler.getSelector(searchType, 0);
        Selector two = SelectorsHandler.getSelector(searchType, 1);
        Selector three = SelectorsHandler.getSelector(searchType, 2);
        
        MetadataModel model = results.getMetadataModel();
        
        BOX_1 = new FilterBox(model, one);
        BOX_2 = new FilterBox(model, two);
        BOX_3 = new FilterBox(model, three);
        RESULTS = results;
        MODEL = model;
        
        // Create a single listener for all boxes and dispatch
        // the event depending on which box it came from.
        new MinimizeManager(new SelectorListener());
        
        add(BOX_1.getComponent());
        add(BOX_2.getComponent());
        add(BOX_3.getComponent());
		
        setMatchingValues(null);

        ID = "" + uniqueIdCount++;
    }
    
    /**
     * Returns a description unique to this FilterPanel.
     */
    String getUniqueDescription() {
       return ID;
    }     

	/**
	 * Returns an array of the three {@FilterBox filter boxes} managed by
	 * this FilterPanel.
	 * @return
	 */
	public FilterBox[] getBoxes() {
		return new FilterBox[] { BOX_1, BOX_2, BOX_3 };
	}
	
    /**
     * Attempts to set any matching values
     * in the filter boxes.  If 'box' is null, matches against
     * all boxes.  Otherwise matches only against 'box'.
     */
    private void setMatchingValues(FilterBox box) {
        SearchInformation info = RESULTS.getSearchInformation();
        // only select on keyword searches.
        if(info.isKeywordSearch()) {
            LimeXMLDocument doc = null;
            String xml = RESULTS.getRichQuery();
            if(xml != null) {
                try {
                    doc = GuiCoreMediator.getLimeXMLDocumentFactory().createLimeXMLDocument(xml);
                }
                catch(SAXException se) {}
                catch(SchemaNotFoundException snfe) {}
                catch(IOException ioe) {}
            }

            if(box != null) {
                selectMatchingValues(box, doc, info);
            } else {
                selectMatchingValues(BOX_1, doc, info);
                selectMatchingValues(BOX_2, doc, info);
                selectMatchingValues(BOX_3, doc, info);
            }
        }
    }
    
    /**
     * Selects any values in the FilterBox if they match the document.
     */
    private void selectMatchingValues(FilterBox box, LimeXMLDocument doc,
                                      SearchInformation info) {
        // only works for field selectors.
        if(!box.getSelector().isFieldSelector())
            return;
            
        String query = info.getQuery();
        MediaType media = info.getMediaType();
            
        String value = null;
        
        // if there was a XMLDocument for searching, use that for matching
        if(doc != null) {
            // only select if the selector's schema matches the doc's schema    
            String schema = box.getSelector().getSchema();
            if(!schema.equals(doc.getSchemaDescription()))
                return;
            
            String field = box.getSelector().getValue();
            // Get the value of the selector's field from the document.
            value = doc.getValue(field);
        } else if(media == MediaType.getAnyTypeMediaType()) {
            // otherwise, if they did an any-type search,
            // try and match up in any of the boxes.
            value = query;
        }

        // If we have a matching value, use it.
        if(value != null) {
            box.setRequestedValue(value);
        }
    }
    
    /**
     * Manages when minimizing the boxes is/isn't allowed.
     */
    private class MinimizeManager implements ChangeListener {
        final ChangeListener SAVER;
        
        /**
         * Adds itself as a stateChangeListener on the boxes.
         */
        MinimizeManager(ChangeListener cl) {
            BOX_1.setStateChangeListener(this);
            BOX_2.setStateChangeListener(this);
            BOX_3.setStateChangeListener(this);
            SAVER = cl;
            stateChanged(null);
        }
        
        /**
         * Notification that a FilterBox has become minimized or restored.
         */
        public void stateChanged(ChangeEvent event) {
            boolean one, two, three;
            one = BOX_1.isMinimized();
            two = BOX_2.isMinimized();
            three = BOX_3.isMinimized();
            
            // if all three somehow got minimized,
            // make the first one restored again.
            if(one && two && three) {
                BOX_1.setStateChangeListener(null);
                BOX_1.restore();
                one = false;
                BOX_1.setStateChangeListener(this);
            }

            if(one && two) {
                BOX_3.setCanMinimize(false);
            } else if(one && three) {
                BOX_2.setCanMinimize(false);
            } else if(two && three) {
                BOX_1.setCanMinimize(false);
            } else {
                BOX_1.setCanMinimize(true);
                BOX_2.setCanMinimize(true);
                BOX_3.setCanMinimize(true);
            }

            removeAll();            
            add(BOX_1.getComponent());
            add(BOX_2.getComponent());
            add(BOX_3.getComponent());
            invalidate();
            revalidate();
            repaint();
            SAVER.stateChanged(event);
        }
    }       
    
    /**
     * A listener for the filter boxes.
     *
     * Dispatches events based on the source of the selection.
     */
    private class SelectorListener implements ListDataListener, 
                                       ListSelectionListener, ChangeListener {
        
        /**
         * "boolean marker" used to prevent contentChanged method from
         * triggering a chain of methods calls which calls itself
         * again. <pre>triggered</pre> is set to true in contentsChanged before
         * calling updateBoxes, so when it gets called again, it returns
         * immediately. Hacky but effective. The variable is reset after the
         * call.
         */
        private boolean triggered = false;
        
        /**
         * Constructs a new SelectorListener for the specified boxes.
         */
        SelectorListener() {
            BOX_1.addSelectionListener(this, this);
            BOX_2.addSelectionListener(this, this);
            BOX_3.addSelectionListener(this, this);
            
            BOX_1.setSelectorChangeListener(this);
            BOX_2.setSelectorChangeListener(this);
            BOX_3.setSelectorChangeListener(this);
        }
        
        /**
         * Notification that the selectors have changed for a box.
         * TODO: Update only the depth that changed.
         */
        public void stateChanged(ChangeEvent event) {
            String type = RESULTS.getMediaType().getSchema();
            SelectorsHandler.setSelector(type, 0, BOX_1.getSelector());
            SelectorsHandler.setSelector(type, 1, BOX_2.getSelector());
            SelectorsHandler.setSelector(type, 2, BOX_3.getSelector());
            if(event != null) {
                FilterBox box = (FilterBox)event.getSource();
                setMatchingValues(box);
            }
        }

        /**
         * Notification that the selected value has changed.
         *
         * Updates the selector on the ResultPanel and the list of the
         * latter filter boxes.
         *
         * The event is ignored if the value is adjusting (the mouse is still
         * down and moving).
         */ 
        public void valueChanged(ListSelectionEvent event) {
            // extraneous events. 
            if(event.getValueIsAdjusting())
                return; 

            // Because the source of the event is the JList,
            // we must figure out which FilterBox it came from
            // based on the list of the boxes.
            JList source = (JList)event.getSource();
            FilterBox box = null;
            int depth = -1;

            if(source == BOX_1.getList()) {
                box = BOX_1;
                depth = 1;
            } else if(source == BOX_2.getList()) {
                box = BOX_2;
                depth = 2;
            } else if(source == BOX_3.getList()) {
                box = BOX_3;
                depth = 3;
            } else {
                throw new IllegalStateException("invalid source: " + source);
            }
            
            Selector selector = box.getSelector();
            Object value = box.getSelectedValue();
            boolean isAll = event.getFirstIndex() == 0 &&
                            event.getLastIndex() == 0;
            // only update the latter boxes only if the selection actually
            // changed or we selected 'All'.  we must special-case All
            // because the filter doesn't change when we go from no selection
            // to an All selection, but we want to erase the lower selections.
            if(isAll || selectionChanged(selector, value, depth))
                updateBoxes(depth, box, true);
        }

        /**
         * Notification that contents of a FilterBox have changed, we are going
         * to use this information to update the box above the one in which
         * items were added, which will cause this box to the updated as well.
         */
        public void contentsChanged(ListDataEvent event) {
            if(triggered)
                return;

            Object source = event.getSource();
            FilterBox box = null;
            int depth = -1;

            if(source == BOX_1.getList().getModel()) {
                //box = null;
                depth = -1;
            } else if(source == BOX_2.getList().getModel()) {
                box = BOX_1;
                depth = 0;
            } else if(source == BOX_3.getList().getModel()) {
                box = BOX_2;
                depth = 1;
            } else {
                throw new IllegalStateException("invalid source: " + source);
            }
            
            if(depth > -1) {
                triggered= true;
                updateBoxes(depth, box, false);
                triggered=false;
            }
        }
        
        /** Stubbed out method for ListDataListener */
        public void intervalRemoved(ListDataEvent e) { }

        /** Stubbed out method for ListDataListener */
        public void intervalAdded(ListDataEvent e) { }

        /**
         * If the box to be updated is box1 changes the models of box2 and box3,
         * otherwise, if box2 is changed, changes the model of box3. Also
         * handles the condition when the the "all" option is selected in any of
         * the boxes.
         */
        private void updateBoxes(int depth, FilterBox box, boolean clear) {

            box.updateTitle();
            
            // If box1 changed, we must change the model of box2 & box3.
            // If box2 changed, we only change the model of box3.
            Object sel1 = BOX_1.getSelectedValue();
            boolean all1 = sel1 == null || MetadataModel.isAll(sel1);
            
            if(box == BOX_1) {
                if(all1)
                    allPossible(BOX_2);
                else 
                    changeModel(BOX_1, BOX_2, sel1);
                if(clear)
                    BOX_2.clearSelection();
            }
            
            Object sel2 = BOX_2.getSelectedValue();
            boolean all2 = sel2 == null || MetadataModel.isAll(sel2);
            if(box == BOX_2 || box == BOX_1) {
                if(all2) {
                    if(all1)
                        allPossible(BOX_3);
                    else
                        changeModel(BOX_1, BOX_3, sel1);
                } else {
                    changeModel(BOX_2, BOX_3, sel2);
                }
                if(clear)
                    BOX_3.clearSelection();                
            }
        }
        
        /**
         * Changes the model of the box to be all possible selections for
         * this box.
         */
        private void allPossible(FilterBox box) {
            box.setModel(MODEL.getListModelMap(box.getSelector()));
        }

        /**
         * Changes the model of child to a cross section of the two boxes.
         */
        private void changeModel(FilterBox parent, FilterBox child,
                                 Object value) {
            child.setModel(
                MODEL.getIntersection(
                    parent.getModel(),
                    value,
                    MODEL.getListModelMap(child.getSelector())
                )
            );
        }
        
        /**
         * Creates a new filter to give the ResultPanel.
         */
        private boolean selectionChanged(Selector selector,
                                      Object value,
                                      int depth) {
            TableLineFilter lineFilter = null;
    
            if(value == null || MetadataModel.isAll(value))
                lineFilter = AllowFilter.instance();
            else {                  
                switch(selector.getSelectorType()) {
                case Selector.SCHEMA:
                    lineFilter = new SchemaFilter((NamedMediaType)value);
                    break;
                case Selector.FIELD:
                    lineFilter = new FieldFilter(selector.getSchema(),
                                                 selector.getValue(),
                                                 (XMLValue)value);
                    break;
                case Selector.PROPERTY:
                    lineFilter = new PropertyFilter(selector.getValue(),
                                                    value);
                }
            }
            
            return RESULTS.filterChanged(lineFilter, depth);
        }
    }
}
