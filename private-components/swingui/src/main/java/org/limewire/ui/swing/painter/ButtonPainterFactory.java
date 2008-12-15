package org.limewire.ui.swing.painter;

import java.awt.Color;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.painter.ButtonBackgroundPainter.DrawMode;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ButtonPainterFactory {
    
    @Resource private Color miniHoverTextForeground;
    @Resource private Color miniDownTextForeground;
    
    @Resource private Color darkFullHoverTextForeground;
    @Resource private Color darkFullDownTextForeground;
    @Resource private Color darkFullDisabledTextForeground;
    
    @Inject
    ButtonPainterFactory() {
        GuiUtils.assignResources(this);
    }
    
    public ButtonForegroundPainter createMiniButtonForegroundPainter() {
        return new ButtonForegroundPainter(miniHoverTextForeground, miniDownTextForeground, Color.GRAY);
    }
    
    public ButtonForegroundPainter createLightFullButtonForegroundPainter() {
        return new ButtonForegroundPainter();
    }
    
    public ButtonForegroundPainter createDarkFullButtonForegroundPainter() {
        return new ButtonForegroundPainter(darkFullHoverTextForeground, darkFullDownTextForeground, 
                darkFullDisabledTextForeground);
    }

    public PopupButtonBackgroundPainter createMiniButtonBackgroundPainter() {
        return new PopupButtonBackgroundPainter();
    }

    public ButtonBackgroundPainter createLightFullButtonBackgroundPainter() {
        return new LightButtonBackgroundPainter();
    }
    
    public ButtonBackgroundPainter createDarkFullButtonBackgroundPainter(DrawMode mode, 
            AccentType accent) {
        return new DarkButtonBackgroundPainter(mode, accent);
    }
}
