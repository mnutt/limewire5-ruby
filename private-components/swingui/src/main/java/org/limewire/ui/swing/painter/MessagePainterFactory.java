package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Insets;
import java.awt.Paint;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

public class MessagePainterFactory<T> {
    
    @Resource private int arcWidth;
    @Resource private int arcHeight;

    public MessagePainterFactory() {
        GuiUtils.assignResources(this);
    }

    public Painter<T> createGrayMessagePainter() {
        GrayMessagePainterResources resources = new GrayMessagePainterResources();
        return createPainter(resources.backgroundGradientTop, resources.backgroundGradientBottom,
                resources.border, resources.bevelTop1, resources.bevelTop2, resources.bevelLeft,
                resources.bevelRightGradientTop, resources.bevelRightGradientBottom,
                resources.bevelBottom);
    }

    public Painter<T> createGreenMessagePainter() {
        GreenMessagePainterResources resources = new GreenMessagePainterResources();
        return createPainter(resources.backgroundGradientTop, resources.backgroundGradientBottom,
                resources.border, resources.bevelTop1, resources.bevelTop2, resources.bevelLeft,
                resources.bevelRightGradientTop, resources.bevelRightGradientBottom,
                resources.bevelBottom);
    }

    /**
     * Creates a painter for a rectangular region that does not render rounded
     * corners.
     */
    public Painter<T> createGreenRectanglePainter() {
        GreenMessagePainterResources resources = new GreenMessagePainterResources();
        return createRectanglePainter(resources.backgroundGradientTop, resources.backgroundGradientBottom,
                resources.border, resources.bevelTop1, resources.bevelTop2, resources.bevelLeft,
                resources.bevelRightGradientTop, resources.bevelRightGradientBottom,
                resources.bevelBottom);
    }

    private Painter<T> createPainter(Color backgroundGradientTop, Color backgroundGradientBottom,
            Color border, Color bevelTop1, Color bevelTop2, Color bevelLeft,
            Color bevelRightGradientTop, Color bevelRightGradientBottom, Color bevelBottom) {

        Paint background = new GradientPaint(0, 0, backgroundGradientTop, 0, 1,
                backgroundGradientBottom);
        Paint bevelRight = new GradientPaint(0, 0, bevelRightGradientTop, 0, 1,
                bevelRightGradientBottom);

        return new ComponentBackgroundPainter<T>(background, border,
                bevelLeft, bevelTop1, bevelTop2, bevelRight, bevelBottom, arcWidth, arcHeight,
                AccentType.NONE);
    }

    /**
     * Creates a painter for a rectangular region that does not render rounded
     * corners.
     */
    private Painter<T> createRectanglePainter(Color backgroundGradientTop, Color backgroundGradientBottom,
            Color border, Color bevelTop1, Color bevelTop2, Color bevelLeft,
            Color bevelRightGradientTop, Color bevelRightGradientBottom, Color bevelBottom) {

        Paint background = new GradientPaint(0, 0, backgroundGradientTop, 0, 1,
                backgroundGradientBottom);
        Paint bevelRight = new GradientPaint(0, 0, bevelRightGradientTop, 0, 1,
                bevelRightGradientBottom);

        // Create background painter without rounded corners.
        RectanglePainter<T> textBackgroundPainter = new RectanglePainter<T>();
        textBackgroundPainter.setRounded(true);
        textBackgroundPainter.setFillPaint(background);
        textBackgroundPainter.setRoundWidth(0);
        textBackgroundPainter.setRoundHeight(0);
        textBackgroundPainter.setInsets(new Insets(2,2,2,2));
        textBackgroundPainter.setBorderPaint(null);
        textBackgroundPainter.setPaintStretched(true);
        textBackgroundPainter.setFillVertical(true);
        textBackgroundPainter.setFillHorizontal(true);
        textBackgroundPainter.setAntialiasing(true);
        textBackgroundPainter.setCacheable(true);
        
        // Create border painter without rounded corners.  We specify shadow
        // accent to ensure that the entire region is painted.  We set the 
        // left inset so shadow appears only along right and bottom borders.
        BorderPainter<T> borderPainter = new BorderPainter<T>(0, 0,
                border,  bevelLeft,  bevelTop1,  bevelTop2, 
                bevelRight,  bevelBottom, AccentType.SHADOW);
        borderPainter.setInsets(new Insets(0, -1, 0, 0));
        
        // Return compound painter for background and border.
        CompoundPainter<T> painter = new CompoundPainter<T>();
        painter.setPainters(textBackgroundPainter, borderPainter);
        painter.setCacheable(true);
        return painter;
    }

    private static class GreenMessagePainterResources {
        @Resource protected Color backgroundGradientTop = PainterUtils.TRASPARENT;
        @Resource protected Color backgroundGradientBottom = PainterUtils.TRASPARENT;
        @Resource protected Color border = PainterUtils.TRASPARENT;
        @Resource protected Color bevelTop1 = PainterUtils.TRASPARENT;
        @Resource protected Color bevelTop2 = PainterUtils.TRASPARENT;
        @Resource protected Color bevelLeft = PainterUtils.TRASPARENT;
        @Resource protected Color bevelRightGradientTop = PainterUtils.TRASPARENT;
        @Resource protected Color bevelRightGradientBottom = PainterUtils.TRASPARENT;
        @Resource protected Color bevelBottom = PainterUtils.TRASPARENT;

        public GreenMessagePainterResources() {
            GuiUtils.assignResources(GreenMessagePainterResources.this);
        }
    }

    private static class GrayMessagePainterResources {
        @Resource protected Color backgroundGradientTop = PainterUtils.TRASPARENT;
        @Resource protected Color backgroundGradientBottom = PainterUtils.TRASPARENT;
        @Resource protected Color border = PainterUtils.TRASPARENT;
        @Resource protected Color bevelTop1 = PainterUtils.TRASPARENT;
        @Resource protected Color bevelTop2 = PainterUtils.TRASPARENT;
        @Resource protected Color bevelLeft = PainterUtils.TRASPARENT;
        @Resource protected Color bevelRightGradientTop = PainterUtils.TRASPARENT;
        @Resource protected Color bevelRightGradientBottom = PainterUtils.TRASPARENT;
        @Resource protected Color bevelBottom = PainterUtils.TRASPARENT;

        public GrayMessagePainterResources() {
            GuiUtils.assignResources(GrayMessagePainterResources.this);
        }
    }
}
