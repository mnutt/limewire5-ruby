package org.limewire.ui.swing.painter.factories;

import java.awt.Color;
import java.awt.GradientPaint;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.painter.GenericBarPainter;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

import com.google.inject.Inject;

/**
 * Creates the various background painters to be used to paint the gradients and borders
 *  of the various on screen panels.
 *  
 * Each painter can have up to six colours.  Two for the gradient endpoints,
 *  two for the two horizontal border lines on the top edge, and two for 
 *  another two horizontal border lines on the bottom edge.
 */
@LazySingleton
public class BarPainterFactory {

    @Resource private Color headerBarGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color headerBarGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color headerBarBorderTop1 = PainterUtils.TRASPARENT;
    @Resource private Color headerBarBorderTop2 = PainterUtils.TRASPARENT;
    @Resource private Color headerBarBorderBottom1 = PainterUtils.TRASPARENT;
    @Resource private Color headerBarBorderBottom2 = PainterUtils.TRASPARENT;
    
    @Resource private Color specialHeaderBarGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color specialHeaderBarGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color specialHeaderBarBorderTop1 = PainterUtils.TRASPARENT;
    @Resource private Color specialHeaderBarBorderTop2 = PainterUtils.TRASPARENT;
    @Resource private Color specialHeaderBarBorderBottom1 = PainterUtils.TRASPARENT;
    @Resource private Color specialHeaderBarBorderBottom2 = PainterUtils.TRASPARENT;
    
    @Resource private Color topBarGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color topBarGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color topBarBorderTop1 = PainterUtils.TRASPARENT;
    @Resource private Color topBarBorderTop2 = PainterUtils.TRASPARENT;
    @Resource private Color topBarBorderBottom1 = PainterUtils.TRASPARENT;
    @Resource private Color topBarBorderBottom2 = PainterUtils.TRASPARENT;
    
    @Resource private Color statusBarGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color statusBarGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color statusBarBorderTop1 = PainterUtils.TRASPARENT;
    @Resource private Color statusBarBorderTop2 = PainterUtils.TRASPARENT;
    @Resource private Color statusBarBorderBottom1 = PainterUtils.TRASPARENT;
    @Resource private Color statusBarBorderBottom2 = PainterUtils.TRASPARENT;
        
    @Resource private Color downloadSummaryBarGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color downloadSummaryBarGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color downloadSummaryBarBorderTop1 = PainterUtils.TRASPARENT;
    @Resource private Color downloadSummaryBarBorderTop2 = PainterUtils.TRASPARENT;
    @Resource private Color downloadSummaryBarBorderBottom1 = PainterUtils.TRASPARENT;
    @Resource private Color downloadSummaryBarBorderBottom2 = PainterUtils.TRASPARENT;
    
    @Resource private Color friendBarGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color friendBarGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color friendBarBorderTop1 = PainterUtils.TRASPARENT;
    @Resource private Color friendBarBorderTop2 = PainterUtils.TRASPARENT;
    @Resource private Color friendBarBorderBottom1 = PainterUtils.TRASPARENT;
    @Resource private Color friendBarBorderBottom2 = PainterUtils.TRASPARENT;
    
    @Resource private Color popUpBarGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color popUpBarGradientBottom = PainterUtils.TRASPARENT;
    @Resource private Color popUpBarBorderTop1 = PainterUtils.TRASPARENT;
    @Resource private Color popUpBarBorderTop2 = PainterUtils.TRASPARENT;
    @Resource private Color popUpBarBorderBottom1 = PainterUtils.TRASPARENT;
    @Resource private Color popUpBarBorderBottom2 = PainterUtils.TRASPARENT;
    
    @Inject
    BarPainterFactory() {
        GuiUtils.assignResources(this);
    }
    
    public GenericBarPainter<JXPanel> createHeaderBarPainter() {
        return new GenericBarPainter<JXPanel>(
            new GradientPaint(0,0,headerBarGradientTop,0,1,headerBarGradientBottom), 
            headerBarBorderTop1, headerBarBorderTop2, 
            headerBarBorderBottom1, headerBarBorderBottom2);
    }
    
    public GenericBarPainter<JXPanel> createSpecialHeaderBarPainter() {
        return new GenericBarPainter<JXPanel>(
                new GradientPaint(0,0, specialHeaderBarGradientTop,0,1, specialHeaderBarGradientBottom), 
                specialHeaderBarBorderTop1, specialHeaderBarBorderTop2,
                specialHeaderBarBorderBottom1, specialHeaderBarBorderBottom2);
    }
    
    public GenericBarPainter<JXPanel> createFriendsBarPainter() {
        return new GenericBarPainter<JXPanel>(
            new GradientPaint(0,0,friendBarGradientTop,0,1,friendBarGradientBottom), 
            friendBarBorderTop1, friendBarBorderTop2, 
            friendBarBorderBottom1, friendBarBorderBottom2);
    }
    
    public GenericBarPainter<JXPanel> createTopBarPainter() {
        return new GenericBarPainter<JXPanel>(
            new GradientPaint(0,0,topBarGradientTop,0,1,topBarGradientBottom), 
            topBarBorderTop1, topBarBorderTop2, 
            topBarBorderBottom1, topBarBorderBottom2);
    }
    
    public GenericBarPainter<JXPanel> createStatusBarPainter() {
        return new GenericBarPainter<JXPanel>(
            new GradientPaint(0,0,statusBarGradientTop,0,1,statusBarGradientBottom), 
            statusBarBorderTop1, statusBarBorderTop2, 
            statusBarBorderBottom1, statusBarBorderBottom2);
    }
    
    public GenericBarPainter<JXPanel> createDownloadSummaryBarPainter() {
        return new GenericBarPainter<JXPanel>(
            new GradientPaint(0,0,downloadSummaryBarGradientTop,0,1,downloadSummaryBarGradientBottom), 
            downloadSummaryBarBorderTop1, downloadSummaryBarBorderTop2, 
            downloadSummaryBarBorderBottom1, downloadSummaryBarBorderBottom2);
    }
    
    public GenericBarPainter<JXPanel> createPopUpBarPainter() {
        return new GenericBarPainter<JXPanel>(
            new GradientPaint(0,0,popUpBarGradientTop,0,1,popUpBarGradientBottom), 
            popUpBarBorderTop1, popUpBarBorderTop2, 
            popUpBarBorderBottom1, popUpBarBorderBottom2);
    }
}
