package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.util.BaseTestCase;

/**
 * JUnit test case for AbstractTabList.
 */
public class AbstractTabListTest extends BaseTestCase {
    /** Instance of class being tested. */
    private AbstractTabList tabList;

    /**
     * Constructs a test case for the specified method name.
     */
    public AbstractTabListTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tabList = new TestTabList();
    }

    @Override
    protected void tearDown() throws Exception {
        tabList = null;
        super.tearDown();
    }

    /** Tests method to set all tabs. */
    public void testSetTabActionMaps() {
        // Create list of action maps.
        List<TabActionMap> actionMapList = createTabActionMapList();
        
        // Set action maps in tab list.
        tabList.setTabActionMaps(actionMapList);
        
        // Verify number of tabs.
        int expectedReturn = actionMapList.size();
        int actualReturn = tabList.getTabs().size();
        assertEquals("tab count", expectedReturn, actualReturn);
    }
    
    /** Tests method to create a new tab. */
    public void testCreateAndPrepareTab() {
        // Create tab action map.
        Action mainAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        }; 
        TabActionMap tabActionMap = new TabActionMap(mainAction, null, null, null);
        
        // Create tab.
        FancyTab tab = tabList.createAndPrepareTab(tabActionMap);
        
        // Verify tab action map.
        Object expectedReturn = tabActionMap;
        Object actualReturn = tab.getTabActionMap();
        assertEquals("tab action map", expectedReturn, actualReturn);
    }
    
    /** Tests method to add tab to list. */
    public void testAddTab() {
        // Create tab action map.
        Action mainAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        }; 
        TabActionMap tabActionMap = new TabActionMap(mainAction, null, null, null);
        
        // Create tab.
        FancyTab tab = tabList.createAndPrepareTab(tabActionMap);
        
        // Add tab.
        tabList.addTab(tab, 0);
        
        // Verify number of tabs.
        int expectedReturn = 1;
        int actualReturn = tabList.getTabs().size();
        assertEquals("tab count", expectedReturn, actualReturn);
        
        // Verify tab.
        Object expectedValue = tab;
        Object actualValue = tabList.getTabs().get(0);
        assertEquals("tab", expectedValue, actualValue);
    }
    
    /** Tests method to remove tab from list. */
    public void testRemoveTab() {
        // Create list of action maps.
        List<TabActionMap> actionMapList = createTabActionMapList();
        
        // Set action maps in tab list.
        tabList.setTabActionMaps(actionMapList);
        
        // Get initial tab count.
        List<FancyTab> initialTabList = tabList.getTabs();
        int initialTabCount = initialTabList.size();
        
        // Remove last tab.
        tabList.removeTab(initialTabList.get(initialTabCount - 1));
        
        // Verify number of tabs.
        int expectedReturn = initialTabCount - 1;
        int actualReturn = tabList.getTabs().size();
        assertEquals("tab count", expectedReturn, actualReturn);
    }
    
    /** Tests method to set highlight painter. */
    public void testSetHighlightPainter() {
        // Set highlight painter.
        Painter painter = new RectanglePainter();
        tabList.setHighlightPainter(painter);
        
        // Verify tab property.
        Object expectedReturn = painter;
        Object actualReturn = tabList.getTabProperties().getHighlightPainter();
        assertEquals("highlight painter", expectedReturn, actualReturn);
    }
    
    /** Tests method to set selection painter. */
    public void testSetSelectionPainter() {
        // Set selection painter.
        Painter painter = new RectanglePainter();
        tabList.setSelectionPainter(painter);
        
        // Verify tab property.
        Object expectedReturn = painter;
        Object actualReturn = tabList.getTabProperties().getSelectedPainter();
        assertEquals("selection painter", expectedReturn, actualReturn);
    }
    
    /** Tests method to set tab text color. */
    public void testSetTabTextColor() {
        // Create tabs.
        tabList.setTabActionMaps(createTabActionMapList());
        
        // Set tab text color.
        Color color = Color.BLACK;
        tabList.setTabTextColor(color);
        
        // Verify tab property.
        Object expectedReturn = color;
        Object actualReturn = tabList.getTabProperties().getNormalColor();
        assertEquals("text color", expectedReturn, actualReturn);
    }
    
    /** Tests method to set selected tab text color. */
    public void testSetTabTextSelectedColor() {
        // Create tabs.
        tabList.setTabActionMaps(createTabActionMapList());
        
        // Set selected tab text color.
        Color color = Color.RED;
        tabList.setTabTextSelectedColor(color);
        
        // Verify tab property.
        Object expectedReturn = color;
        Object actualReturn = tabList.getTabProperties().getSelectionColor();
        assertEquals("selected text color", expectedReturn, actualReturn);
    }
    
    /** Tests method to set tab font. */
    public void testSetTextFont() {
        // Create tabs.
        tabList.setTabActionMaps(createTabActionMapList());
        
        // Set tab font.
        Font font = new Font(Font.DIALOG, Font.PLAIN, 10);
        tabList.setTextFont(font);
        
        // Verify tab property.
        Object expectedReturn = font;
        Object actualReturn = tabList.getTabProperties().getTextFont();
        assertEquals("text font", expectedReturn, actualReturn);
    }

    /** Creates a new list of action maps for tabs. */
    private List<TabActionMap> createTabActionMapList() {
        // Define test actions.
        Action mainAction1 = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        
        Action mainAction2 = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        
        // Return list of action maps.
        return TabActionMap.createMapForMainActions(mainAction1, mainAction2);
    }
    
    /**
     * Test implementation of AbstractTabList.
     */
    private class TestTabList extends AbstractTabList {

        @Override
        protected void layoutTabs() {
        }
    }
}
