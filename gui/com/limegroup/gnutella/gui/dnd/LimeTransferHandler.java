package com.limegroup.gnutella.gui.dnd;

import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.TransferHandler;


/**
 * A better TransferHandler.
 */
public class LimeTransferHandler extends TransferHandler {
    
    private static TriggerableDragGestureRecognizer recognizer;
    
    private final int supportedActions;
    
    /**
     * Constructs a LimeTransferHandler with no supported actions.
     */
    public LimeTransferHandler() {
        this.supportedActions = NONE;
    }

    /**
     * Creates a new LimeTransferHandler that supports the given actions.
     * 
     * @param supportedActions
     */
    public LimeTransferHandler(int supportedActions) {
        this.supportedActions = supportedActions;
    }

    /**
     * Determines whether or not the given component can accept the given transferFlavors.
     * This returns false by default.
     */
    @Override
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        return false;
    }

    /**
     * Determines if the data can be imported.
     */
    public boolean canImport(JComponent c, DataFlavor[] flavors, DropInfo ddi) {
        return false;
    }    

    /**
     * Attempts to create a transferable from the given component.
     * This returns null by default.
     */
    @Override
    protected Transferable createTransferable(JComponent c) {
        return new BasicTransferableCreator(c).getTransferable();
    }

    /**
     * Does nothing.
     */
    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        // Does nothing.
    }

    /**
     * Returns the actions supported by the given component.
     * By default, this returns no actions supported.
     */
    @Override
    public int getSourceActions(JComponent c) {
        return supportedActions;
    }

    /**
     * UNUSED -- This method is an API bug, it should be returning an Image.
     * Use getImageRepresentation instead.
     * @deprecated
     */
    @Deprecated
    @Override
    public final Icon getVisualRepresentation(Transferable t) {
        throw new IllegalStateException("USE getImageRepresentation INSTEAD");
    }
    
    /**
     * Returns an image representation of the given transferable.
     * @param t
     * @return
     */
    public Image getImageRepresentation(Transferable t) {
        return new TransferVisualizer(t).getImage();
    }

    /**
     * Imports data into the given component.
     * This returns true if data was imported, false otherwise.
     */
    @Override
    public boolean importData(JComponent comp, Transferable t) {
        return false;
    }

    /**
     * Imports the data into the given component.
     */
    public boolean importData(JComponent c, Transferable t, DropInfo ddi) {
        // TODO Auto-generated method stub
        return false;
    }
    
    
    /**
     * Initiates a drag operation from the given component.
     */
    @Override
    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        int srcActions = getSourceActions(comp);
        int dragAction = srcActions & action;
        if (! (e instanceof MouseEvent))
            dragAction = NONE;
        
        if (dragAction != NONE && !GraphicsEnvironment.isHeadless()) {
            // Use a custom DragGestureRecognizer that we can automatically
            // trigger to fire a dragGestureRecognized event.
            if (recognizer == null)
                recognizer = new TriggerableDragGestureRecognizer(new BasicDragGestureListener());
            recognizer.trigger(comp, (MouseEvent)e, srcActions, dragAction);
        } else {
                exportDone(comp, null, NONE);
        }
    }
    
}
