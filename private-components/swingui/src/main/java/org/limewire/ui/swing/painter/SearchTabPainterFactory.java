package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Factory for creating search tab painters in the lw default
 *  colour scheme. 
 */
@Singleton
public class SearchTabPainterFactory {
    @Resource private Color selectionTopBevelBackground;
    @Resource private Color selectionTopBevelBorder;
    @Resource private Color selectionBackgroundTopGradient;
    @Resource private Color selectionBackgroundBottomGradient;
    
    @Resource private Color highlightTopBevelBackground;
    @Resource private Color highlightTopBevelBorder;
    @Resource private Color highlightBackgroundTopGradient;
    @Resource private Color highlightBackgroundBottomGradient;
    
    @Inject
    public SearchTabPainterFactory() {
        GuiUtils.assignResources(this);
    }
    
    public SearchTabPainter createSelectionPainter() {
        
        return new SearchTabPainter(selectionTopBevelBackground, 
                    selectionTopBevelBorder,
                    new GradientPaint(0, 0, selectionBackgroundTopGradient, 
                        0, 1, selectionBackgroundBottomGradient));
    }
    
    public SearchTabPainter createHighlightPainter() {
        
        return new SearchTabPainter(highlightTopBevelBackground, 
                highlightTopBevelBorder,
                    new GradientPaint(0, 0, highlightBackgroundTopGradient, 
                        0, 1, highlightBackgroundBottomGradient), true);
    }
    
}
