package org.limewire.ui.swing.components.decorators;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.painter.ButtonBackgroundPainter.DrawMode;
import org.limewire.ui.swing.painter.factories.ButtonPainterFactory;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Class that preps and skins JXButtons with the default
 *  lw style. 
 *  
 *  Mini buttons do not have a background drawn unless mouse
 *   overed or clicked, full buttons do.
 *  
 */
@Singleton
public class ButtonDecorator {

    private final ButtonPainterFactory painterFactory;
    
    @Resource private Font  miniTextFont;
    @Resource private Color miniTextForeground;
    
    @Resource private Font  linkTextFont;
    @Resource private Color linkTextForeground;
    
    @Resource private Font  lightFullTextFont;
    @Resource private Color lightFullTextForeground;
    
    @Resource private Font  darkFullTextFont;
    @Resource private Color darkFullTextForeground;
    
    @Inject
    ButtonDecorator(ButtonPainterFactory painterFactory) {
        GuiUtils.assignResources(this);
        
        this.painterFactory = painterFactory;
        
        // TODO: Underline can not be set using resources?
        linkTextFont = FontUtils.deriveUnderline(linkTextFont, true);
    }
    
    public void decorateMiniButton(JXButton button) {
        button.setForegroundPainter(painterFactory.createMiniButtonForegroundPainter());
        button.setBackgroundPainter(painterFactory.createMiniButtonBackgroundPainter());
        
        decorateGeneral(button);
        button.setBorder(BorderFactory.createEmptyBorder(2,6,3,6));
        
        button.setForeground(miniTextForeground);
        button.setFont(miniTextFont);
    }
    
    /** 
     * The link button is similar to the mini button in that it has no background drawn until mouse
     *  over.  When inactive, text is underlined like a link, the active styles are similar to that
     *  of the mini buttons.
     */
    public void decorateLinkButton(JXButton button) {
        button.setForegroundPainter(painterFactory.createLinkButtonForegroundPainter());
        button.setBackgroundPainter(painterFactory.createLinkButtonBackgroundPainter());
        
        decorateGeneral(button);
        button.setBorder(BorderFactory.createEmptyBorder(2,6,3,6));
        
        button.setForeground(linkTextForeground);
        button.setFont(linkTextFont);
    }
    
    public void decorateDarkFullButton(JXButton button, AccentType accent) {
        decorateDarkFullButton(button, DrawMode.FULLY_ROUNDED, accent);
        button.setBorder(BorderFactory.createEmptyBorder(2,10,3,10));
    }
    
    public void decorateDarkFullButton(JXButton button) {
        decorateDarkFullButton(button, DrawMode.FULLY_ROUNDED, AccentType.SHADOW);
        button.setBorder(BorderFactory.createEmptyBorder(2,10,3,10));
    }
    
    /**
     * This button is preped for the case where an image will be displayed
     *  but no text.  The buttons icon will be centered and the remained of the
     *  button drawn around.  This button will be painted with the dark style and
     *  needs an accent to be selected. 
     */
    public void decorateDarkFullImageButton(JXButton button, AccentType accent) {
        decorateDarkFullButton(button, button.getForegroundPainter(), 
                painterFactory.createDarkFullButtonBackgroundPainter(DrawMode.FULLY_ROUNDED, accent));
        
        button.setContentAreaFilled(false);
        button.setPaintBorderInsets(true);
    }
    
    /**
     * This button is preped for the case where an image will be displayed
     *  but no text.  The buttons icon will be centered and the remained of the
     *  button drawn around.  This button will be painted with the dark style and
     *  accepts a parameter for overriding the edge rounding settings on the button face. 
     */
    public void decorateDarkFullImageButton(JXButton button, DrawMode mode) {
        decorateDarkFullButton(button, button.getForegroundPainter(), 
                painterFactory.createDarkFullButtonBackgroundPainter(mode, AccentType.SHADOW));
        
        button.setContentAreaFilled(false);
        button.setPaintBorderInsets(true);
    }
    
    public void decorateDarkFullButton(JXButton button, DrawMode mode, AccentType accent) {
        decorateDarkFullButton(button, painterFactory.createDarkFullButtonForegroundPainter(), 
                painterFactory.createDarkFullButtonBackgroundPainter(mode, accent));
        button.setBorder(BorderFactory.createEmptyBorder(2,10,3,10));
    }
    
    private void decorateDarkFullButton(JXButton button,
            Painter<JXButton> foregroundPainter, Painter<JXButton> backgroundPainter) {
        
        button.setForegroundPainter(foregroundPainter);
        button.setBackgroundPainter(backgroundPainter);
        
        decorateGeneral(button);
        
        button.setForeground(darkFullTextForeground);
        button.setFont(darkFullTextFont);
    }
    
    public void decorateLightFullButton(JXButton button) {
        button.setForegroundPainter(painterFactory.createLightFullButtonForegroundPainter());
        button.setBackgroundPainter(painterFactory.createLightFullButtonBackgroundPainter());

        decorateGeneral(button);
        button.setBorder(BorderFactory.createEmptyBorder(2,10,3,10));
        
        button.setForeground(lightFullTextForeground);
        button.setFont(lightFullTextFont);
    }
        
    public void decorateGreenFullButton(JXButton button) {
        button.setForegroundPainter(painterFactory.createLightFullButtonForegroundPainter());
        button.setBackgroundPainter(painterFactory.createGreenFullButtonBackgroundPainter());

        decorateGeneral(button);
        
        button.setForeground(lightFullTextForeground);
        button.setFont(lightFullTextFont);
    }    
        
    private static void decorateGeneral(JXButton button) {
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
    }
}
