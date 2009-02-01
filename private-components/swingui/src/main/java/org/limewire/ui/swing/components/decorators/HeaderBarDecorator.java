package org.limewire.ui.swing.components.decorators;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.painter.factories.BarPainterFactory;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * A singleton decorator class that sets both the l&f
 *  and sizing properties for header bars. 
 */
@Singleton
public class HeaderBarDecorator {

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
    HeaderBarDecorator(BarPainterFactory painterFactory) {
        GuiUtils.assignResources(this);
        
        this.painterFactory = painterFactory;
    }
            
    public void decorateBasic(HeaderBar bar) {
        decorateCommon(bar);
        
        bar.setBackgroundPainter(painterFactory.createHeaderBarPainter());
        bar.setForeground(basicForeground);
    }
    
    public void decorateSpecial(HeaderBar bar) {
        decorateCommon(bar);
        
        bar.setBackgroundPainter(painterFactory.createSpecialHeaderBarPainter());
        bar.setForeground(specialForeground);
    }
    
    private void decorateCommon(HeaderBar bar) {
        bar.setMinimumSize(new Dimension(0, this.height));
        bar.setPreferredSize(new Dimension(Integer.MAX_VALUE, this.height));
        bar.setDefaultComponentHeight(defaultComponentHeight);
        bar.setFont(headingFont);
    }
}
