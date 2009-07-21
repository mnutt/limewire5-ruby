package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.painter.MessagePainterFactory;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Creates a Lime Message Component. Currently
 * this is painted green by default and has a triangle, chat arrow
 * attached to the bottom of it.
 */
public class MessageComponent extends JPanel {
    
    public enum MessageBackground {
        GREEN, GRAY
    }
    
    @Resource
    private Icon grayArrowIcon;
    @Resource
    private Icon greenArrowIcon;
    @Resource
    private Font headingFont;
    @Resource
    private Color fontColor;
    @Resource
    private Font subFont;
    
    
    /**
     * Contains the actual subComponents.
     */
    private JXPanel messageContainer;
    
    public MessageComponent() {
        this(MessageBackground.GREEN);
    }
    
    public MessageComponent(MessageBackground background) {
        this(18, 22, 22, 18, background);
    }
        
        
    public MessageComponent(int topInset, int leftInset, int bottomInset, int rightInset) {
        this(topInset, leftInset, bottomInset, rightInset, MessageBackground.GREEN);
    }
    

    public MessageComponent(int topInset, int leftInset, int bottomInset, int rightInset, MessageBackground background) {
        GuiUtils.assignResources(this);
        
        Icon arrowIcon = background == MessageBackground.GREEN ? greenArrowIcon : grayArrowIcon;
        
        setLayout(new MigLayout("insets 0 0 " + (arrowIcon.getIconHeight()-2) + " 0, gap 0"));
        setOpaque(false);
        
        messageContainer = new JXPanel(new MigLayout("insets " + topInset + " " + leftInset + " " + bottomInset + " " + rightInset + ", hidemode 3"));
        messageContainer.setOpaque(false);
        if(background == MessageBackground.GREEN){
        messageContainer.setBackgroundPainter(new MessagePainterFactory<JXPanel>().createGreenMessagePainter());
        } else {
            messageContainer.setBackgroundPainter(new MessagePainterFactory<JXPanel>().createGrayMessagePainter());
        }
        
        add(new JLabel(arrowIcon), "pos (messageContainer.x + 25) 0.99al");
        add(messageContainer, "wrap");
    
    }
    
    public void addComponent(JComponent component, String layout) {
        messageContainer.add(component, layout);
    }
    
    public void decorateHeaderLabel(JComponent component) {
        component.setFont(headingFont);
        component.setForeground(fontColor);
    }
    
    public void decorateHeaderLink(HyperlinkButton link) {
        link.setFont(headingFont);
    }
    
    public void decorateSubLabel(JLabel component) {
        component.setFont(subFont);
        component.setForeground(fontColor);
    }
    
    public void decorateSubLabel(HTMLLabel label) {
        label.setHtmlFont(subFont);
        label.setHtmlForeground(fontColor);
    }
    
    public void decorateFont(JComponent component) {
        component.setFont(subFont);
    }
}
