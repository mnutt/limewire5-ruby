package org.limewire.ui.swing.components;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.limewire.util.BaseTestCase;

/**
 * JUnit test case for TabActionMap.
 */
public class TabActionMapTest extends BaseTestCase {
    /** Instance of class being tested. */
    private TabActionMap tabActionMap;

    /**
     * Constructs a test case for the specified method name.
     */
    public TabActionMapTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        tabActionMap = null;
        super.tearDown();
    }
    
    /** Tests constructor with several actions. */
    public void testTabActionMap() {
        // Create actions.
        Action mainAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        Action removeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        Action moreTextAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        List<Action> rightClickActionList = new ArrayList<Action>();

        // Create tab action map.
        tabActionMap = new TabActionMap(mainAction, removeAction,
                moreTextAction, rightClickActionList);

        // Verify actions.
        Object expectedReturn = mainAction;
        Object actualReturn = tabActionMap.getMainAction();
        assertEquals("main action", expectedReturn, actualReturn);
        
        expectedReturn = removeAction;
        actualReturn = tabActionMap.getRemoveAction();
        assertEquals("remove action", expectedReturn, actualReturn);
        
        expectedReturn = moreTextAction;
        actualReturn = tabActionMap.getMoreTextAction();
        assertEquals("more text action", expectedReturn, actualReturn);
        
        expectedReturn = rightClickActionList;
        actualReturn = tabActionMap.getRightClickActions();
        assertEquals("right-click actions", expectedReturn, actualReturn);
    }
    
    /** Tests method to create list of TabActionMap objects using actions. */
    public void testCreateMapForMainActions() {
        // Create list of main actions.
        List<Action> mainActionList = new ArrayList<Action>();
        mainActionList.add(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        mainActionList.add(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        
        // Create list of tab action maps.
        List<TabActionMap> actionMapList = TabActionMap.createMapForMainActions(mainActionList);
        
        // Verify tab action maps.
        for (int i = 0, size = actionMapList.size(); i < size; i++) {
            TabActionMap tabActionMap = actionMapList.get(i);
            Object expectedReturn = mainActionList.get(i);
            Object actualReturn = tabActionMap.getMainAction();
            assertEquals("main action", expectedReturn, actualReturn);
        }
    }
}
