package org.limewire.ui.swing.components;

import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.util.BaseTestCase;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

/**
 * JUnit test case for FlexibleTabList.
 */
public class FlexibleTabListTest extends BaseTestCase {
    /** Instance of class being tested. */
    private FlexibleTabList tabList;

    /**
     * Constructs a test case for the specified method name.
     */
    public FlexibleTabListTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tabList = new FlexibleTabList(createComboBoxDecorator());
    }

    @Override
    protected void tearDown() throws Exception {
        tabList = null;
        super.tearDown();
    }

    /** Tests method to add new tab using tab action map. */
    public void testAddTabActionMapAt() {
        // Create tab action map.
        Action mainAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        }; 
        TabActionMap tabActionMap = new TabActionMap(mainAction, null, null, null);
        
        // Add tab.
        tabList.addTabActionMapAt(tabActionMap, 0);
        
        // Verify number of tabs.
        int expectedReturn = 1;
        int actualReturn = tabList.getTabs().size();
        assertEquals("tab count", expectedReturn, actualReturn);
    }
    
    /** Tests method to set removable property. */
    public void testSetRemovable() {
        // Set removable property.
        tabList.setRemovable(true);
        
        // Verify tab property.
        boolean actualReturn = tabList.getTabProperties().isRemovable();
        assertTrue("removable", actualReturn);
    }
    
    /** Tests method to set tab insets. */
    public void testSetTabInsets() {
        // Set tab insets.
        Insets insets = new Insets(1, 1, 1, 1);
        tabList.setTabInsets(insets);
        
        // Verify tab property.
        Object expectedReturn = insets;
        Object actualReturn = tabList.getTabProperties().getInsets();
        assertEquals("tab insets", expectedReturn, actualReturn);
    }
    
    /** Creates a combobox decorator for tests. */
    private ComboBoxDecorator createComboBoxDecorator() {
        Injector injector = Guice.createInjector(Stage.PRODUCTION);
        return injector.getInstance(ComboBoxDecorator.class);
    }
}
