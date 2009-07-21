package org.limewire.ui.swing.components;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.effects.ShadowPathEffect;
import org.limewire.ui.swing.util.GuiUtils;

public class ShapeDialog extends JXPanel implements Resizable {


    private AWTEventListener eventListener;

    private Component component;

    private ComponentListener componentListener;

    private Component owner;
    
    private boolean isAutoClose;
    
    private static final int SHADOW_INSETS = 5;
    

    @Resource
    private boolean isPositionedRelativeToOwner;

    public ShapeDialog() {
        super(new MigLayout("fill, ins 0 0 0 0 , gap 0! 0!, novisualpadding"));
        GuiUtils.assignResources(this);
        
        setOpaque(false);
        setVisible(false);
        
        setBackgroundPainter(new DialogShadowPainter());

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                addListeners();
                resize();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                removeListeners();
            }
        });
              
        componentListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setSize(getPreferredSize());
                repaint();
            }
        };
    }
    
    /**
     * Shows c centered in the Frame with no autoclose.
     */
    public void show(Component c){
        show(c, null, false);
    }
    
 
    /**
     * 
     * @param c the Component shown
     * @param owner the dialog owner.  Dialog will be centered in frame if this is null
     * @param autoClose Dialog will have PopupMenu close behavior (clicking away or pressing ESC closes dialog)
     */
    public void show(Component c, Component owner, boolean autoClose) {
        removeAll();
        int inset = (owner != null && isPositionedRelativeToOwner) ? 0 : SHADOW_INSETS;
        add(c, "alignx 50%, aligny 50%, gapleft " + inset + ", gapright " + inset + ", gaptop " + inset + ", gapbottom " + inset);
        this.component = c;
        this.owner = owner;
        this.isAutoClose = autoClose;
        setVisible(true);
        //make sure this actually gets shown in a timely manner
        getParent().repaint();
    }
    
    @Override
    public void setVisible(boolean visible){
        super.setVisible(visible);
        //prevent initial strange appearance when reshown
        if (!visible){
            setSize(0, 0);
        }
    }
    private void positionRelativeToOwner(){
        Point ownerLocation = SwingUtilities.convertPoint(owner.getParent(), owner.getLocation(), getParent());            
        setBounds(ownerLocation.x + owner.getWidth() - getWidth(), ownerLocation.y + owner.getHeight(), getPreferredSize().width, getPreferredSize().height);
    }
    
    


    private void addListeners() {
        if (eventListener == null) {
            initializeEventListener();
        }

        Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.MOUSE_EVENT_MASK);
        Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.KEY_EVENT_MASK);   
        if (component != null) {
            component.addComponentListener(componentListener);
        }
    }

    private void removeListeners() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(eventListener);
        if (component != null) {
            component.removeComponentListener(componentListener);
        }
    }

    private void initializeEventListener() {
        // make sharePanel disappear when the user clicks elsewhere
        eventListener = new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (isVisible() && isAutoClose) {

                    if ((event.getID() == MouseEvent.MOUSE_PRESSED)) {
                        MouseEvent e = (MouseEvent) event;                        
                        
                        if ((getMousePosition(true) == null || !contains(getMousePosition(true))) && component != e.getComponent()
                                && (!component.contains(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), component)))) {
                            setVisible(false);
                        }

                    } else if (event.getID() == KeyEvent.KEY_PRESSED) {
                        KeyEvent e = (KeyEvent) event;

                        if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
                            setVisible(false);
                        }
                    }

                }
            }
        };
    }

    @Override
    public void resize() {
        if (component != null) {
            if (!isPositionedRelativeToOwner || owner == null) {
                Rectangle parentBounds = getParent().getBounds();
                Dimension preferredSize = getPreferredSize();
                int w = preferredSize.width;
                int h = preferredSize.height;
                setBounds(parentBounds.width / 2 - w / 2, parentBounds.height / 2 - h / 2, w, h);
            } else {
                positionRelativeToOwner();
            }
        }
    }
    
    private class DialogShadowPainter implements Painter {
        private ShadowPathEffect shadow;
        
        public DialogShadowPainter(){
            shadow = new ShadowPathEffect();
            shadow.setEffectWidth(14);
            shadow.setOffset(new Point(0, 0));
        }
        
        @Override
        public void paint(Graphics2D g, Object object, int width, int height) {            
            if (component != null) {                
                Graphics2D g2 = (Graphics2D) g.create();
                Shape shape;

                if (component instanceof ShapeComponent) {
                    shape = ((ShapeComponent) component).getShape();
                    g2.translate(component.getLocation().x, component.getLocation().y);
                } else {
                    shape = component.getBounds();
                }

                shadow.apply(g2, shape, width, height);
                g2.dispose();
            }            
        }        
    }

}
