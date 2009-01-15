package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JLabel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.painter.BarPainterFactory;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LimeHeaderBarFactory {

    private final BarPainterFactory painterFactory;
    
    @Resource private int height;
    @Resource private Font headingFont;
    @Resource private Color specialForeground;
    @Resource private Color basicForeground;
    
    /**
     * All components added to a lime header bar will be
     *  resized to this height regardless of the layout
     *  for consistency.  
     *  
     * NOTE: If layouts with more than one row are being
     *        used or components with different sizes are 
     *        preferred consider creating a new factory
     *        method and not calling
     *        setDefaultComponentHeight()
     */
    @Resource private int defaultComponentHeight;
    
    @Inject
    LimeHeaderBarFactory(BarPainterFactory painterFactory) {
        GuiUtils.assignResources(this);
        
        this.painterFactory = painterFactory;
    }
    
    
    /**
     *  Since the title component is compound the factory method
     *  accepts two parameters for setting the title component and automatically linking
     *  the header bar to the textual inner component. It is important to do it all at once
     *  to simplify the decorating process and make sure the header text has the proper
     *  size and coloring.
     */
    public LimeHeaderBar createBasic(Component component, JLabel titleComponent) {
        LimeHeaderBar bar = new LimeHeaderBar(component);
        bar.setDefaultComponentHeight(defaultComponentHeight);
        bar.linkTextComponent(titleComponent);
        decorateBasic(bar);
        return bar;
    }
    
    public LimeHeaderBar createBasic(Component comp) {
        LimeHeaderBar bar = new LimeHeaderBar(comp);
        bar.setDefaultComponentHeight(defaultComponentHeight);
        decorateBasic(bar);
        return bar;
    }
    
    public LimeHeaderBar createBasic() {
        return createBasic("");
    }
    
    public LimeHeaderBar createBasic(String text) {
        LimeHeaderBar bar = new LimeHeaderBar(text);
        bar.setDefaultComponentHeight(defaultComponentHeight);
        this.decorateBasic(bar);
        return bar;
    }
    
    public LimeHeaderBar createSpecial(String text) {
        JLabel label = new JLabel(text);
        return createSpecial(label, label);
    }
    
    /**
     * Creates a "special header bar".  This kind of bar has a different, bolder colour 
     *  scheme then the rest of the header bars.   At the moment it is only used in
     *  FriendSharingPanel.  Since the title component is compound the factory method
     *  accepts two parameters for setting the title component and automatically linking
     *  the header bar to the textual inner component. It is important to do it all at once
     *  to simplify the decorating process and make sure the header text has the proper
     *  size and coloring.
     */
    public LimeHeaderBar createSpecial(Component titleComponent, JLabel titleTextComponent) {
        LimeHeaderBar bar = new LimeHeaderBar(titleComponent);
        bar.setDefaultComponentHeight(defaultComponentHeight);
        bar.linkTextComponent(titleTextComponent);
        this.decorateSpecial(bar);
        return bar;
    }
        
    public void decorateBasic(JXPanel bar) {
        bar.setBackgroundPainter(this.painterFactory.createHeaderBarPainter());
        bar.setMinimumSize(new Dimension(0, this.height));
        bar.setPreferredSize(new Dimension(Integer.MAX_VALUE, this.height));
        bar.setFont(this.headingFont);
        bar.setForeground(basicForeground);
    }
    
    public void decorateSpecial(JXPanel bar) {
        bar.setBackgroundPainter(this.painterFactory.createSpecialHeaderBarPainter());
        bar.setMinimumSize(new Dimension(0, this.height));
        bar.setPreferredSize(new Dimension(Integer.MAX_VALUE, this.height));
        bar.setFont(this.headingFont);
        bar.setForeground(specialForeground);
    }
}
