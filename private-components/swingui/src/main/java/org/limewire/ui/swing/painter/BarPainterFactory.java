package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.components.LimeHeaderBar;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BarPainterFactory {

    @Resource private Color headerBarGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color headerBarGradientBottom = PainterUtils.TRASPARENT;

    @Resource private Color specialHeaderBarGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color specialHeaderBarGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color specialHeaderBarTopBorder1 = PainterUtils.TRASPARENT;
    @Resource private Color specialHeaderBarBottomBorder1 = PainterUtils.TRASPARENT;
    
    @Resource private Color topBarGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color topBarGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color topBarBorderTop1 = PainterUtils.TRASPARENT;
    @Resource private Color topBarBorderBottom1 = PainterUtils.TRASPARENT;
    @Resource private Color topBarBorderBottom2 = PainterUtils.TRASPARENT;
    
    @Resource private Color statusBarTopGradient = PainterUtils.TRASPARENT;
    @Resource private Color statusBarBottomGradient = PainterUtils.TRASPARENT;
    @Resource private Color statusBarTopBorder1 = PainterUtils.TRASPARENT;
    @Resource private Color statusBarTopBorder2 = PainterUtils.TRASPARENT;
    
    @Resource private Color downloadSummaryBarTopGradient = PainterUtils.TRASPARENT;
    @Resource private Color downloadSummaryBarBottomGradient = PainterUtils.TRASPARENT;
    @Resource private Color downloadSummaryBarTopBorder1 = PainterUtils.TRASPARENT;
    
    @Resource private Color friendBarTopBorder1 = PainterUtils.TRASPARENT;
    
    @Inject
    BarPainterFactory() {
        GuiUtils.assignResources(this);
    }
    
    public GenericBarPainter<LimeHeaderBar> createHeaderBarPainter() {
        return new GenericBarPainter<LimeHeaderBar>(
            new GradientPaint(0,0,this.headerBarGradientTop,0,1,this.headerBarGradientBottom), 
            PainterUtils.TRASPARENT, PainterUtils.TRASPARENT, 
            PainterUtils.TRASPARENT, PainterUtils.TRASPARENT);
    }
    
    public GenericBarPainter<LimeHeaderBar> createSpecialHeaderBarPainter() {
        return new GenericBarPainter<LimeHeaderBar>(
                new GradientPaint(0,0, specialHeaderBarGradientTop,0,1, specialHeaderBarGradientBottom), 
                specialHeaderBarTopBorder1, PainterUtils.TRASPARENT,
                specialHeaderBarBottomBorder1, PainterUtils.TRASPARENT);
    }
    
    public GenericBarPainter<JXPanel> createFriendsBarPainter() {
        return new GenericBarPainter<JXPanel>(
            new GradientPaint(0,0,this.topBarGradientTop,0,1,this.topBarGradientBottom), 
            this.friendBarTopBorder1, PainterUtils.TRASPARENT, 
            this.topBarBorderBottom1, PainterUtils.TRASPARENT);
    }
    
    public GenericBarPainter<JXPanel> createTopBarPainter() {
        return new GenericBarPainter<JXPanel>(
            new GradientPaint(0,0,this.topBarGradientTop,0,1,this.topBarGradientBottom), 
            this.topBarBorderTop1, PainterUtils.TRASPARENT, 
            this.topBarBorderBottom1, topBarBorderBottom2);
    }
    
    public GenericBarPainter<JXPanel> createStatusBarPainter() {
        return new GenericBarPainter<JXPanel>(
            new GradientPaint(0,0,this.statusBarTopGradient,0,1,this.statusBarBottomGradient), 
            this.statusBarTopBorder1, this.statusBarTopBorder2, 
            PainterUtils.TRASPARENT, PainterUtils.TRASPARENT);
    }
    
    public GenericBarPainter<JXPanel> createDownloadSummaryBarPainter() {
        return new GenericBarPainter<JXPanel>(
            new GradientPaint(0,0,this.downloadSummaryBarTopGradient,0,1,this.downloadSummaryBarBottomGradient), 
            this.downloadSummaryBarTopBorder1, PainterUtils.TRASPARENT, 
            PainterUtils.TRASPARENT, PainterUtils.TRASPARENT);
    }
}
