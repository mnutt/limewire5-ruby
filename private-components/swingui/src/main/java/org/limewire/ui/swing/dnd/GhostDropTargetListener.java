package org.limewire.ui.swing.dnd;

import java.awt.Component;
import java.awt.Point;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;

/**
 * Listens to drag and drop events. When files are dragged onto a
 * component implementing this listener, a semi-transparent image
 * will be appear next to mouse to give better feedback as to what
 * action the drop will result in.
 * 
 * This class is responsible for loading the glass pane, making it
 * visible, displaying the transparent drag, and hiding the glass
 * pane when the drag exits or completes.
 */
public class GhostDropTargetListener implements DropTargetListener {

    private final GhostDragGlassPane ghostDragGlassPane;
    private final Component parent;
    private final Friend friend;
    
    public GhostDropTargetListener(Component parent, GhostDragGlassPane ghostDragGlassPane) {
        this.parent = parent;
        this.ghostDragGlassPane = ghostDragGlassPane;
        this.friend = null;
    }
    
    public GhostDropTargetListener(Component parent, GhostDragGlassPane ghostDragGlassPane, String renderName) {
        this.parent = parent;
        this.ghostDragGlassPane = ghostDragGlassPane;
        this.friend = new FriendAdapter(renderName);
    }
    
    public GhostDropTargetListener(Component parent, GhostDragGlassPane ghostDragGlassPane, Friend friend) {
        this.parent = parent;
        this.ghostDragGlassPane = ghostDragGlassPane;
        this.friend = friend;
    }
    
    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        Component component = getGlassPane();
        // something is already currently occupying the glass pane and its visible
        if(!(component instanceof GhostDragGlassPane) && component.isVisible()) 
            return;
        
        // the ghost glass pane is not occupying the glass pane
        if(!(component instanceof GhostDragGlassPane)) {
            SwingUtilities.getRootPane(parent).setGlassPane(ghostDragGlassPane);
        } 
        ghostDragGlassPane.setVisible(true); 
        updateText(dtde, ghostDragGlassPane);
        ghostDragGlassPane.repaint();
    }
    
    /**
     * Converts the mouse coordinates on the component, to mouse coordinates
     * on the glass pane, positions the glass pane, then updates the image
     */
    private void updateText(DropTargetDragEvent dtde, GhostDragGlassPane ghostPane) {
        Point p = (Point) dtde.getLocation().clone();

        SwingUtilities.convertPointToScreen(p, parent);
        SwingUtilities.convertPointFromScreen(p, ghostPane); 

        ghostPane.setPoint(p);
        ghostPane.setText(friend);
    }

	/**
	 * When the drag exits, hide the glass pane
	 */
    @Override
    public void dragExit(DropTargetEvent dte) {
        if(!(getGlassPane() instanceof GhostDragGlassPane))
            return;
        GhostDragGlassPane glassPane = (GhostDragGlassPane) getGlassPane();
        glassPane.setVisible(false);
    }

    /**
     * As a drag occurs over this component, update the image's position
     * as the mouse moves.
     */
    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        if(!(getGlassPane() instanceof GhostDragGlassPane))
            return;
        GhostDragGlassPane glassPane = (GhostDragGlassPane) getGlassPane();

        Point p = (Point) dtde.getLocation().clone();
        SwingUtilities.convertPointToScreen(p, parent);
        SwingUtilities.convertPointFromScreen(p, glassPane); 
        glassPane.setPoint(p);

        glassPane.repaint(glassPane.getRepaintRect());
    }

	/**
	 * When a drop occurs, hide the glass pane
	 */
    @Override
    public void drop(DropTargetDropEvent dtde) {
        if(!(getGlassPane() instanceof GhostDragGlassPane))
            return;
        GhostDragGlassPane glassPane = (GhostDragGlassPane) getGlassPane();
        glassPane.setVisible(false);
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {}
    
    private Component getGlassPane() {
        return SwingUtilities.getRootPane(parent).getGlassPane();
    }
    
    /**
     * Takes a String and creates a Friend which returns that name.
     */
    private class FriendAdapter implements Friend {
        private String renderName;
        
        public FriendAdapter(String name) {
            this.renderName = name;
        }

        @Override
        public String getRenderName() {
            return renderName;
        }
        
        @Override
        public String getName() {return null;}
        @Override
        public String getFirstName() {return null;}
        @Override
        public Map<String, FriendPresence> getFriendPresences() {return null;}
        @Override
        public String getId() {return null;}
        @Override
        public Network getNetwork() {return null;}
        @Override
        public boolean isAnonymous() {return false;}
        @Override
        public void setName(String name) {}
    }
}
