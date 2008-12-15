package org.limewire.ui.swing.components;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import org.jdesktop.swingx.JXPanel;

import com.google.inject.Singleton;

@Singleton
public class ShapeDialog extends JXPanel implements Resizable {

    private int buffer = 10;

    private AWTEventListener eventListener;

    private Component component;

   // private Color shadowColor = new Color(0,0,0, 125);
    
    private ComponentListener componentListener;

    public ShapeDialog() {
        super(new BorderLayout());
        setOpaque(false);
        setVisible(false);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                addListeners();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                removeListeners();
            }
        });
        
//        setBackgroundPainter(new Painter() {
//            @Override
//            public void paint(Graphics2D g, Object object, int width, int height) {
//                Graphics2D g2 = (Graphics2D)g.create();
//                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//                        RenderingHints.VALUE_ANTIALIAS_ON);
//                g2.setColor(shadowColor);
//                g2.fillRoundRect(0, 0, width, height, 10, 10);
//                g2.dispose();
//            }
//        });
        
        componentListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resize();
            }
        };
    }

    public void show(Component c) {
        removeAll();
        add(c);
        this.component = c;
        setVisible(true);
        resize();
    }

    private void addListeners() {
        if (eventListener == null) {
            initializeEventListener();
        }

        Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.MOUSE_EVENT_MASK);
        Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.KEY_EVENT_MASK);
        if(component!=null){
            component.addComponentListener(componentListener);
        }
    }

    private void removeListeners() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(eventListener);
        if(component!=null){
            component.addComponentListener(componentListener);
        }
    }

    private void initializeEventListener() {
        // make sharePanel disappear when the user clicks elsewhere
        eventListener = new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (isVisible()) {

                    if ((event.getID() == MouseEvent.MOUSE_PRESSED)) {
                        MouseEvent e = (MouseEvent) event;
                        // TODO: this isn't quite right
                        if (component != e.getComponent()
                                && (!(component instanceof ShapeDialogComponent) || !((ShapeDialogComponent) component)
                                        .contains(e.getComponent()))) {
                            setVisible(false);
                        }

                    } else if (event.getID() == KeyEvent.KEY_TYPED) {
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
        if (isVisible() && component != null) {
            Rectangle parentBounds = getParent().getBounds();
            Dimension childPreferredSize = getPreferredSize();
            int w = childPreferredSize.width;
            int h = childPreferredSize.height;
            setBounds(parentBounds.width / 2 - w / 2, parentBounds.height / 2 - h, w, h);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (component != null) {
            Dimension dim = component.getPreferredSize();
            return new Dimension(dim.width + buffer * 2, dim.height + buffer * 2);
        } else {
            return new Dimension(0, 0);
        }
    }

}
