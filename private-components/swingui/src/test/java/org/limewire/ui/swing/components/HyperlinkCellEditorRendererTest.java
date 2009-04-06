package org.limewire.ui.swing.components;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;

import junit.framework.TestCase;

/**
 * JUnit test case for HyperlinkCellEditorRenderer.
 */
public class HyperlinkCellEditorRendererTest extends TestCase {
    /** Instance of class being tested. */
    private HyperlinkCellEditorRenderer editorRenderer;

    /**
     * Constructs a test case for the specified method name.
     */
    public HyperlinkCellEditorRendererTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        editorRenderer = new HyperlinkCellEditorRenderer();
    }

    @Override
    protected void tearDown() throws Exception {
        editorRenderer = null;
        super.tearDown();
    }

    /** Tests constructor to accept an Action. */
    public void testHyperlinkCellEditorRendererAction() {
        // Create test action.
        String name = "test";
        Action action = new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                putValue(Action.SHORT_DESCRIPTION, "done");
            }
        };

        // Create editor/renderer using action.
        editorRenderer = new HyperlinkCellEditorRenderer(action);
        
        // Verify editor/renderer text.
        String expectedReturn = name;
        String actualReturn = editorRenderer.getText();
        assertEquals("renderer text", expectedReturn, actualReturn);
        
        // Perform action and verify.
        editorRenderer.doClick();
        expectedReturn = "done";
        actualReturn = (String) action.getValue(Action.SHORT_DESCRIPTION);
        assertEquals("actionPerformed", expectedReturn, actualReturn);
    }

    /** Tests method to cancel cell editing. */
    public void testCancelCellEditing() {
        // Add cell editor listener.
        TestEditorListener listener = new TestEditorListener();
        editorRenderer.addCellEditorListener(listener);

        // Cancel editing and verify.
        editorRenderer.cancelCellEditing();
        assertTrue("cancelCellEditing", listener.isCancelled());
    }

    /** Tests method to stop cell editing. */
    public void testStopCellEditing() {
        // Add cell editor listener.
        TestEditorListener listener = new TestEditorListener();
        editorRenderer.addCellEditorListener(listener);

        // Stop editing and verify.  The current implementation always cancels
        // editing even when editing is stopped.
        editorRenderer.stopCellEditing();
        assertTrue("stopCellEditing", listener.isCancelled());
    }

    /** Tests method to get editable indicator. */
    public void testIsCellEditable() {
        // Verify cell is always editable.
        assertTrue("cell editable", editorRenderer.isCellEditable(null));
    }

    /**
     * Test implementation of CellEditorListener.
     */
    private static class TestEditorListener implements CellEditorListener {
        private boolean cancelled = false;
        private boolean stopped = false;

        @Override
        public void editingCanceled(ChangeEvent e) {
            cancelled = true;
        }

        @Override
        public void editingStopped(ChangeEvent e) {
            stopped = true;
        }
        
        public boolean isCancelled() {
            return cancelled;
        }
        
        public boolean isStopped() {
            return stopped;
        }
    }
}
