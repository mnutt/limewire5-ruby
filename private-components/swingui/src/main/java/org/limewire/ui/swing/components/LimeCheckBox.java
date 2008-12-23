package org.limewire.ui.swing.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.ui.swing.painter.TextShadowPainter;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

public class LimeCheckBox extends JCheckBox{

    public static final Insets TEXT_INSETS = new Insets(0,16,0,0);
    
    /**
     * A shared painter that can be used to paint plain (unshadowed) check boxes.
     *  This is pooled so caching is turned off. 
     */
    public static final TextShadowPainter<JCheckBox> NORMAL_TEXT_PAINTER 
        = new TextShadowPainter<JCheckBox>(PainterUtils.TRASPARENT, TEXT_INSETS, false);
    
    @Resource private Color checkBorder;
    
    private AbstractPainter<JCheckBox> checkPainter;
    private Painter<JCheckBox> textPainter;
    
    public LimeCheckBox(String text){
        super(text);
        init();
    }
    
    public LimeCheckBox(Action action) {
        super(action);
        init();
    }
    
    public void setCheckPainter(AbstractPainter<JCheckBox> painter) {
        checkPainter = painter;
    }
    
    public void setTextPainter(Painter<JCheckBox> painter) {
        textPainter = painter;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        checkPainter.paint((Graphics2D) g, this, this.getWidth(), this.getHeight());
        textPainter.paint((Graphics2D) g, this, this.getWidth(), this.getHeight());
    }
    
    
    private void init(){        
        GuiUtils.assignResources(this);
        
        TextShadowPainter<JCheckBox> textPainter 
            = new TextShadowPainter<JCheckBox>();
        textPainter.setInsets(TEXT_INSETS);
        
        this.checkPainter = new CheckPainter();
        this.textPainter = textPainter;
        
        this.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                checkPainter.clearCache();               
            }
        });
    }
    
    
    private class CheckPainter extends AbstractPainter<JCheckBox> {

        public CheckPainter() {
            this.setCacheable(true);
            this.setAntialiasing(true);
        }
        
        @Override
        protected void doPaint(Graphics2D g, JCheckBox object, int width, int height) {
            int top = object.getHeight() / 2 - 7;
                        
            g.fillRect(0,top,12,12);
            g.setColor(checkBorder);
            g.drawRect(0,top,12,12);
            
            if (object.isSelected()) {
                g.setStroke(new BasicStroke(2));
                
                g.drawLine(3,top + 3, 9, top + 9);
                g.drawLine(9,top + 3, 3, top + 9);
            }
        }
    }
}
