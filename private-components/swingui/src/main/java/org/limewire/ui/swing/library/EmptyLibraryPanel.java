package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.dnd.LocalFileListTransferHandler;
import org.limewire.ui.swing.painter.BorderPainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PainterUtils;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Creates as little tables as possible. Expected to be used with a 
 * JXLayer disabling the main component
 */
public class EmptyLibraryPanel extends LibraryPanel {
    
    @Resource
    private Color navColor;
    @Resource
    private Color innerNavBorder;
    @Resource
    private Color backgroundColor;
    @Resource
    private Icon arrowIcon;
    
    private final Friend friend;
    private final FriendLibraryMediator mediator;
        
    @AssistedInject
    public EmptyLibraryPanel(@Assisted Friend friend,
            @Assisted FriendFileList friendFileList,
            @Assisted FriendLibraryMediator mediator, 
            @Assisted JXPanel messageComponent,
            LimeHeaderBarFactory headerBarFactory,
            ButtonDecorator buttonDecorator) {
        super(headerBarFactory);
  
        GuiUtils.assignResources(this);
        
        this.friend = friend;
        this.mediator = mediator;
        if(!friend.isAnonymous()) {
            addButtonToHeader(new ViewSharedLibraryAction(), buttonDecorator);
        }
        addDisposable((Disposable)messageComponent);
        createEmptyPanel(messageComponent);
        getHeaderPanel().setText(I18n.tr("Download from {0}", getFullPanelName()));
        setTransferHandler(new LocalFileListTransferHandler(friendFileList));
        
        enableFilterBox(false);
    }
    
    protected String getFullPanelName() {
        return friend.getRenderName();
    }
    
    protected String getShortPanelName() {
        return friend.getFirstName();
    }
    
    private void createEmptyPanel(JXPanel component) {
        JPanel p = new JPanel(new MigLayout("insets 0, gap 0, fill", "[125!][][fill]", "[fill]"));
        JXPanel leftPanel = new JXPanel();
        leftPanel.setBackground(navColor);
        
        JXPanel rightPanel = new JXPanel();
        rightPanel.setBackground(backgroundColor);
        
        JXPanel messageWrapper = new JXPanel(new MigLayout("insets 0 0 " + (arrowIcon.getIconHeight()-2) + " 0, gap 0"));
        messageWrapper.setOpaque(false);
        component.setOpaque(false);
        component.setBackgroundPainter(new MessagePainter());

        messageWrapper.add(new JLabel(arrowIcon), "pos (messageWrapper.x + 157) 0.99al");
        messageWrapper.add(component, "gapbefore 127, wrap");
        
        p.add(messageWrapper, "pos 0.50al 0.4al");
        
        p.add(leftPanel, "grow");
        p.add(Line.createVerticalLine(innerNavBorder), "growy, width 1!");
        p.add(rightPanel, "grow");
       
        add(p, "grow, span");
    }
    
    @Override
    protected void addMainPanels() {
    }

    private class ViewSharedLibraryAction extends AbstractAction {

        public ViewSharedLibraryAction() {
            putValue(Action.NAME, I18n.tr("Share"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Share your files with {0}", friend.getRenderName()));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            mediator.showSharingCard();
        }
    }
    
    /**
     * Paints the background and border for the message Component
     */
    //TODO: this is basically the same as ButtonBackgroundPainter, the two
    //      should be merged.
    private class MessagePainter<X> extends AbstractPainter<JXPanel> {

        @Resource private int arcWidth;
        @Resource private int arcHeight;
        @Resource private Color backgroundGradientTop = PainterUtils.TRASPARENT;
        @Resource private Color backgroundGradientBottom = PainterUtils.TRASPARENT;
        @Resource private Color borderColour = PainterUtils.TRASPARENT;
        @Resource private Color bevelTop1 = PainterUtils.TRASPARENT;
        @Resource private Color bevelTop2 = PainterUtils.TRASPARENT;
        @Resource private Color bevelLeft = PainterUtils.TRASPARENT;
        @Resource private Color bevelRightGradientTop = PainterUtils.TRASPARENT;
        @Resource private Color bevelRightGradientBottom = PainterUtils.TRASPARENT;
        @Resource private Color bevelBottom = PainterUtils.TRASPARENT;
        
        private Painter<JXPanel> normalPainter;
        
        private DrawMode drawMode = DrawMode.FULLY_ROUNDED;
        
        public MessagePainter() {
            GuiUtils.assignResources(this);
            
            GradientPaint gradientRight = new GradientPaint(0,0, this.bevelRightGradientTop, 
                    0, 1, this.bevelRightGradientBottom, false);
            
            this.normalPainter = createPainter(this.backgroundGradientTop, this.backgroundGradientBottom,
                    this.borderColour, bevelLeft,  this.bevelTop1,  this.bevelTop2, 
                    gradientRight, this.bevelBottom, this.arcWidth, this.arcHeight, AccentType.NONE);
            
            this.setCacheable(false);
        }
        
        private Painter<JXPanel> createPainter(Color gradientTop, Color gradientBottom, 
                Paint border, Paint bevelLeft, Paint bevelTop1, Paint bevelTop2, 
                Paint bevelRight, Paint bevelBottom, int arcWidth, int arcHeight, AccentType accentType) {
            
            CompoundPainter<JXPanel> compoundPainter = new CompoundPainter<JXPanel>();
            
            RectanglePainter<JXPanel> painter = new RectanglePainter<JXPanel>();
            
            int shiftX1 = 0;
            int shiftX2 = 0;
            
            switch (this.drawMode) {       
                case LEFT_ROUNDED :
                    shiftX1 = 0;
                    shiftX2 = -arcWidth+2;
                    break;
                    
                case RIGHT_ROUNDED :
                    shiftX1 = -arcWidth-2;
                    shiftX2 = 0;
                    break;
                    
                case UNROUNDED :
                    shiftX1 = -arcWidth-2;
                    shiftX2 = -arcWidth-2;
                    break;   
            }
            
            painter.setRounded(true);
            painter.setFillPaint(new GradientPaint(0,0, gradientTop, 0, 1, gradientBottom, false));
            painter.setRoundWidth(arcWidth);
            painter.setRoundHeight(arcHeight);
            painter.setInsets(new Insets(1,2+shiftX1,2,2+shiftX2));
            painter.setPaintStretched(true);
            painter.setBorderPaint(null);
            painter.setFillVertical(true);
            painter.setFillHorizontal(true);
            painter.setAntialiasing(true);
            painter.setCacheable(true);
                    
            BorderPainter borderPainter = new BorderPainter(arcWidth, arcHeight,
                    border,  bevelLeft,  bevelTop1,  bevelTop2, 
                    bevelRight,  bevelBottom, accentType);
            borderPainter.setInsets(new Insets(0,shiftX1, 0, shiftX2));        
            
            compoundPainter.setPainters(painter, borderPainter);
            compoundPainter.setCacheable(true);
            
            return compoundPainter;
        }
        
        @Override
        protected void doPaint(Graphics2D g, JXPanel object, int width, int height) {
            this.normalPainter.paint(g, object, width, height);
        }
    }
    
    /**
     * For creating buttons with different edge rounding properties
     * 
     *   Examples :     
     * 
     *       ( LEFT_ROUNDED |   | UNROUNDED |   | RIGHT_ROUNDED ) 
     */
    enum DrawMode {
        FULLY_ROUNDED, RIGHT_ROUNDED, LEFT_ROUNDED, UNROUNDED 
    }
    
}
