package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Action;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.dnd.LocalFileListTransferHandler;
import org.limewire.ui.swing.painter.GenericBarPainter;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Creates as little tables as possible. Expected to be used with a 
 * JXLayer disabling the main component
 */
public class EmptyLibraryPanel extends LibraryPanel {
    
    @Resource
    private Color innerNavTopGradient;
    @Resource
    private Color innerNavBottomGradient;
    @Resource
    private Color innerNavBorder;
    @Resource
    private Color mainPanelTopGradient;
    @Resource
    private Color mainPanelBottomGradient;
    @Resource
    private Color messageBorder;
    @Resource
    private Color messageShadowColor;
    @Resource
    private Color messageBackgroundColor;
    
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
        addButtonToHeader(new ViewSharedLibraryAction(), buttonDecorator);
        addDisposable((Disposable)messageComponent);
        createEmptyPanel(messageComponent);
        getHeaderPanel().setText(I18n.tr("Download from {0}", getFullPanelName()));
        setTransferHandler(new LocalFileListTransferHandler(friendFileList));
    }
    
    protected String getFullPanelName() {
        return friend.getRenderName();
    }
    
    protected String getShortPanelName() {
        return friend.getFirstName();
    }
    
    private void createEmptyPanel(JXPanel component) {
        JPanel p = new JPanel(new MigLayout("nocache, insets 0, gap 0, fill", "[125!][][fill]", "[fill]"));
        JXPanel leftPanel = new JXPanel();
        leftPanel.setBackgroundPainter(new GenericBarPainter<JXPanel>(
                new GradientPaint(0,0, innerNavTopGradient, 
                        0, 1, innerNavBottomGradient, false)));
        
        JXPanel rightPanel = new JXPanel();
        rightPanel.setBackgroundPainter(new GenericBarPainter<JXPanel>(
                new GradientPaint(0,0, mainPanelTopGradient,
                0, 1, mainPanelBottomGradient, false)));
        
        component.setOpaque(false);
        component.setBackgroundPainter(new BackgroundMessagePainter());
        p.add(component, "pos 0.5al 0.5al");
        
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
            putValue(Action.NAME, I18n.tr("Share with {0}", getShortPanelName()));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Show files you're sharing with {0}", getShortPanelName()));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            mediator.showSharingCard();
        }
    }
    
    /**
     * Paints the hover over panel that displays the message
     */
    private class BackgroundMessagePainter<X> extends AbstractPainter<JXPanel> {

        private int BORDER_INSETS = 5;
        private int arc = 10;
        
        public BackgroundMessagePainter() {
        }
        
        @Override
        protected void doPaint(Graphics2D g, JXPanel object, int width, int height) {
            Area shadowArea = new Area(new RoundRectangle2D.Float(0, 0, width, height, arc, arc));
            RoundRectangle2D.Float panelShape = new RoundRectangle2D.Float(BORDER_INSETS, BORDER_INSETS,width - 2 * BORDER_INSETS,height - 2 * BORDER_INSETS, arc, arc);
            Area panelArea = new Area(panelShape);
            shadowArea.subtract(panelArea);
            
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

            g2.setColor(messageShadowColor);
            g2.fill(shadowArea);
            
            g2.setColor(messageBackgroundColor);
            g2.fill(panelArea);        

            g2.setColor(messageBorder);
            g2.draw(panelShape);
            g2.dispose();         
        }
    }
}
