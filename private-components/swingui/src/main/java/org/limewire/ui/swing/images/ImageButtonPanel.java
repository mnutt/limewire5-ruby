package org.limewire.ui.swing.images;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.limewire.ui.swing.components.UnshareButton;

import net.miginfocom.swing.MigLayout;

/**
 * Panel to display buttons in a thumbnail view
 */
public class ImageButtonPanel extends JPanel {
    
    /**
     * For painting a cell renderer, its non editable so no need to add actions to it
     */
    public ImageButtonPanel() {              
        setOpaque(false);

        UnshareButton b = new UnshareButton();
        
        setLayout(new MigLayout("fill, insets 0 6 0 6"));
        
        add(b, "alignx right");
    }
    
    /**
     * For painting a cell editor, actions are needed since the button is clickable
     */
    public ImageButtonPanel(Action unshareAction, final ImageList imageList) {
        setOpaque(false);

        final UnshareButton b = new UnshareButton(unshareAction);
        b.addMouseListener(new MouseListener(){
            @Override
            public void mouseClicked(MouseEvent e) {}
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
            //select the cell in the table if it was clicked
            @Override
            public void mousePressed(MouseEvent e) {
                int index = imageList.locationToIndex(SwingUtilities.convertPoint(b, e.getPoint(), imageList));
                if( index > -1)
                    imageList.setSelectedIndex(index);
            }
        });
        
        setLayout(new MigLayout("fill, insets 0 6 0 6"));
        
        add(b, "alignx right");
    }
}
