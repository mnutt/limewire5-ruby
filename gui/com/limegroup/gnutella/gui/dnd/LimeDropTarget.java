package com.limegroup.gnutella.gui.dnd;

import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.util.TooManyListenersException;

import javax.swing.JComponent;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.UIResource;

/**
 * A DropTarget made specifically use with a BasicDropTargetListener,
 * which is made specifically for a LimeTransferHandler.
 * 
 * This class will be obsolete with the advent of Java 1.6, which
 * has a TransferSupport class that enables all the things this class
 * can do.
 */
// TODO dnd find out how to install it so it is not overridden by swing
// drop targets
public class LimeDropTarget extends DropTarget implements UIResource {

	/** The only listener we'll ever need. */
    private static DropTargetListener listener;

    /** Lazily constructs the listener and returns it. */
    private static DropTargetListener getDropTargetListener() {
        if (listener == null)
            listener = new BasicDropTargetListener();
        return listener;
    }

    /**
     * Allow multicasted listeners, so that UI classes can add support
     * for state-changing events, such as changing the selection or look
     * when the drag state changes.
     */
    private EventListenerList listenerList;
    
    /**
     * Constructs a new LimeDropTarget on this class.
     * 
     * IMPORTANT NOTE:  To allow Swing classes to add their own listeners
     * to this DropTarget, this MUST be installed on a component prior
     * to the LimeTransferHandler being installed.
     * 
     * @param c
     */
    public LimeDropTarget(JComponent c) {
    	setComponent(c);
        try {
            super.addDropTargetListener(getDropTargetListener());
        } catch (TooManyListenersException tmle) {
        }
    }

    @Override
    public void addDropTargetListener(DropTargetListener dtl) throws TooManyListenersException {
        if (listenerList == null)
            listenerList = new EventListenerList();
        listenerList.add(DropTargetListener.class, dtl);
    }

    @Override
    public void removeDropTargetListener(DropTargetListener dtl) {
        if (listenerList != null) {
            listenerList.remove(DropTargetListener.class, dtl);
        }
    }
    
    @Override
    public void dragEnter(DropTargetDragEvent e) {
        super.dragEnter(e);
        if (listenerList != null) {
            Object[] listeners = listenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i -= 2) {
                if (listeners[i] == DropTargetListener.class) {
                    ((DropTargetListener) listeners[i + 1]).dragEnter(e);
                }
            }
        }
    }

    @Override
    public void dragOver(DropTargetDragEvent e) {
        super.dragOver(e);
        if (listenerList != null) {
            Object[] listeners = listenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i -= 2) {
                if (listeners[i] == DropTargetListener.class) {
                    ((DropTargetListener) listeners[i + 1]).dragOver(e);
                }
            }
        }
    }

    @Override
    public void dragExit(DropTargetEvent e) {
        super.dragExit(e);
        if (listenerList != null) {
            Object[] listeners = listenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i -= 2) {
                if (listeners[i] == DropTargetListener.class) {
                    ((DropTargetListener) listeners[i + 1]).dragExit(e);
                }
            }
        }
    }

    @Override
    public void drop(DropTargetDropEvent e) {
        super.drop(e);
        if (listenerList != null) {
            Object[] listeners = listenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i -= 2) {
                if (listeners[i] == DropTargetListener.class) {
                    ((DropTargetListener) listeners[i + 1]).drop(e);
                }
            }
        }
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent e) {
        super.dropActionChanged(e);
        if (listenerList != null) {
            Object[] listeners = listenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i -= 2) {
                if (listeners[i] == DropTargetListener.class) {
                    ((DropTargetListener) listeners[i + 1]).dropActionChanged(e);
                }
            }
        }
    }
}
