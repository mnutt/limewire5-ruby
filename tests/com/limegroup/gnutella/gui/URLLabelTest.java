package com.limegroup.gnutella.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.UIManager;

import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.gui.actions.AbstractAction;
import com.limegroup.gnutella.gui.actions.LimeAction;

public class URLLabelTest extends BaseTestCase {
    
    URLLabel testLabel;
    TestAction action = new TestAction("name", "description", Color.BLUE);
    
    public URLLabelTest (String name){
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
       testLabel = new URLLabel(action);
    }

    public void testActionUpdates(){
       
        //test to see if a propertyChangeListener is attached to the label
        assertEquals(1,action.getPropertyChangeListeners().length);
        
        //trigger a series property change events to see if listener detects anything
        
        action.putValue(LimeAction.NAME, "www.limewire.org");
        
        //test to see if name is set
        assertTrue(testLabel.getText().contains("www.limewire.org"));
        
        action.putValue(LimeAction.SHORT_DESCRIPTION, "LimeWire Website");
        
        //test to see if short description is set
        assertTrue(testLabel.getToolTipText().contains("LimeWire Website"));
        
        action.putValue(LimeAction.COLOR, Color.WHITE);
        
        //test to see if color is set - White is FFFFFF
        assertTrue(testLabel.getText().contains("FFFFFF"));
    }
    
    public void testLabelUpdates() {
        
        String url = "www.limewire.org";
        URLLabel urlLabel = new URLLabel(url);
        
        //test to see if name is set
        assertTrue(urlLabel.getText().contains("www.limewire.org"));
 
        //test to see if mouselistener exists
        assertNotNull(urlLabel.getMouseWheelListeners());      

        //default color is the label foreground - set to see if it matches
        assertTrue(urlLabel.getText().contains(GUIUtils.colorToHex(UIManager.getColor("Label.foreground"))));

    }
    
    public void testSetMethods(){
        
        testLabel.setText("LimeWire Website");
        
        assertTrue(testLabel.getText().contains("LimeWire Website"));
        
        testLabel.setColor(Color.WHITE);
        
        assertTrue(testLabel.getText().contains("FFFFFF"));
        
        testLabel.setAction(action);
        
        assertEquals(action, testLabel.getAction());
        
    }
    
    public void testMouseEventsForAction(){
        
        
       TestURLLabel testMouseLabel = new TestURLLabel(action);
       assertFalse(action.clicked);
       testMouseLabel.processMouseEvent(new MouseEvent(testMouseLabel, MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, 1, false));
       assertTrue(action.clicked);
    }
        
    private class TestAction extends AbstractAction
    {
        public boolean clicked = false;
        
        public TestAction(String name, String descrip, Color colorType) {
           putValue(LimeAction.NAME, name);
           putValue(LimeAction.SHORT_DESCRIPTION, descrip);
           putValue(LimeAction.COLOR, colorType);
        }

        public void actionPerformed(ActionEvent e) {
           clicked = true;
        }
    }
    
    
    /**
     * overwritten to expose processMouseEvent
     */
    private class TestURLLabel extends URLLabel{

        public TestURLLabel(Action action) {
            super(action);
            // TODO Auto-generated constructor stub
        }
        
        @Override
        public void processMouseEvent(MouseEvent e) {
            // TODO Auto-generated method stub
            super.processMouseEvent(e);
        }
        
        
    }
}
