package org.limewire.ui.swing.statusbar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.Resizable;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PainterUtils;
import org.limewire.ui.swing.util.ResizeUtils;

import com.google.inject.Inject;

public class SharedFileCountPopupPanel extends Panel implements Resizable {
   
    private final SharedFileCountPanel sharedFileCountPanel;
    private final JXPanel frame;
    
    @Resource private Color dividerForeground = PainterUtils.TRASPARENT;;
    @Resource private Color rolloverBackground = PainterUtils.TRASPARENT;
    @Resource private Color activeBackground = PainterUtils.TRASPARENT;
    @Resource private Color activeBorder = PainterUtils.TRASPARENT;
    @Resource private Color border = PainterUtils.TRASPARENT;
    @Resource private Font headerFont;
    
    private final AbstractAction closeAction = new AbstractAction(I18n.tr("Hide")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            setVisible(false);
            sharedFileCountPanel.repaint();
        }
    };
        
    @Inject
    public SharedFileCountPopupPanel(SharedFileCountPanel sharedFileCountPanel,
            HeaderBarDecorator barDecorator) {
        super(new BorderLayout());
        
        this.sharedFileCountPanel = sharedFileCountPanel;
        
        GuiUtils.assignResources(this);
        ResizeUtils.forceSize(this, new Dimension(300, 200));
        
        frame = new JXPanel(new BorderLayout());
        frame.setBorder(BorderFactory.createMatteBorder(1, 1, 0, 1, border));
        
        HeaderBar bar = new HeaderBar(I18n.tr("Sharing"));
        barDecorator.decorateBasic(bar);
        bar.setFont(headerFont);
        bar.setLayout(new BorderLayout());
        ResizeUtils.forceHeight(bar, 18);
        
        HyperlinkButton closeButton = new HyperlinkButton(closeAction);
        closeButton.setFont(headerFont);
        closeButton.setNormalForeground(Color.WHITE);
        closeButton.setRolloverForeground(Color.WHITE);
        bar.add(closeButton, BorderLayout.EAST);
        
        frame.add(bar, BorderLayout.NORTH);
        add(frame, BorderLayout.CENTER);

        setUpButton();
        
        setVisible(false);
    }
    
    @Inject
    public void register() {
        sharedFileCountPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isVisible()) {
                    resize();
                }
                setVisible(!isVisible());
                sharedFileCountPanel.repaint();
            }
        });
        
        sharedFileCountPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                if (isVisible()) {
                    resize();
                }
            }
        });
    }
    
    @Override
    public void resize() {
        Rectangle parentBounds = getParent().getBounds();
        Dimension childPreferredSize = getPreferredSize();
        int w = (int) childPreferredSize.getWidth();
        int h = (int) childPreferredSize.getHeight();
        setBounds((int)sharedFileCountPanel.getBounds().getX(),
                (int)parentBounds.getHeight() - h, w, h);
    }
    
    private void setUpButton() {
        sharedFileCountPanel.setBorder(BorderFactory.createEmptyBorder(0,8,0,8));
        sharedFileCountPanel.setFocusPainted(false);
        sharedFileCountPanel.setBorderPainted(false);
        sharedFileCountPanel.setFocusable(false);
        sharedFileCountPanel.setOpaque(false);
        sharedFileCountPanel.setBackgroundPainter(new StatusBarPopupButtonPainter());
    }

    private class StatusBarPopupButtonPainter extends AbstractPainter<JXButton>{

        public StatusBarPopupButtonPainter() {
            setAntialiasing(false);
            setCacheable(false);
        }

        @Override
        protected void doPaint(Graphics2D g, JXButton object, int width, int height) {
            if(SharedFileCountPopupPanel.this.isVisible()) {
                g.setPaint(activeBackground);
                g.fillRect(0, 0, width, height);
                g.setPaint(border);
                g.drawLine(0, 0, 0, height-1);
                g.drawLine(0, height-1, width-1, height-1);
                g.drawLine(width-1, 0, width-1, height-1);
            } else if (object.getModel().isRollover()) {
                g.setPaint(rolloverBackground);
                g.fillRect(0, 2, width-1, height-2);
                g.setPaint(activeBorder);
                g.drawLine(0, 1, 0, height-1);
                g.drawLine(width-1, 1, width-1, height-1);
            }
            else {
                g.setPaint(dividerForeground);
                g.drawLine(0, 3, 0, height-4);
                g.drawLine(width-1, 3, width-1, height-4);
            }
        }    
    }

    
}
