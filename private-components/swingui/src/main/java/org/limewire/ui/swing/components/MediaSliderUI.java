package org.limewire.ui.swing.components;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

/**
 * A custom UI for a JSlider that allows the JSlider to be completely skinnable.
 * Five images are used to skin the JSlider, a leftTrack, a centerTrack, a
 * rightTrack, a thumb, and a pressedThumb.
 * 
 * The three track images will be buffered into single image. For space reasons,
 * in most cases the center image will be a single pixel wide and be stretched
 * to fill any space non consumed by the left or right images.
 */
public class MediaSliderUI extends BasicSliderUI {

    /**
     * The buffered image which represents the entire background image to be
     * painted
     */
    protected BufferedImage trackImageCache;

    /**
     * A flag for repainting the cachedTrack image when the jslider dimensions
     * change
     */
    protected boolean isDirty = true;

    /**
     * A reference to the owner of this UI
     */
    protected MediaSlider slider;

    /**
     * A flag for choosing which thumb image to paint. Set to true when the
     * mouse is currently pressing the thumbImage
     */
    private boolean isPressed = false;

    /**
     * Listens for mouse presses on the thumb, and paints a new image when the
     * thumb is grabbed to display focus
     */
    private ThumbMouse thumbMouseListener;

    /**
     * Listens for changes in the size of the jslider and calls a repaint on the
     * cachedTrack image
     */
    private ResizeListener componentListener;

    public MediaSliderUI(MediaSlider b) {
        super(b);
        slider = b;
        slider.setOpaque(false);

        thumbMouseListener = new ThumbMouse();
        componentListener = new ResizeListener();
    }

    @Override
    protected void installDefaults(JSlider slider) {
        super.installDefaults(slider);
        // some L&Fs change this which can produce weird painting effects
        focusInsets = new Insets(0, 0, 0, 0);
    }

    /**
     * Sets whether the buffered trackImage should be refreshed. Setting this to
     * true will cause the cachedImage to be thrown out and repainted based on
     * the current jslider bounds.
     */
    public void setDirty(boolean value) {
        isDirty = value;
        if (value == true)
            slider.repaint();
    }

    @Override
    protected void installListeners(JSlider slider) {
        super.installListeners(slider);

        slider.addMouseListener(thumbMouseListener);
        slider.addComponentListener(componentListener);
    }

    @Override
    protected void uninstallListeners(JSlider slider) {
        super.uninstallListeners(slider);

        slider.removeMouseListener(thumbMouseListener);
        slider.removeComponentListener(componentListener);
    }

    /**
     * If an image has been loaded for the thumb, set the thumb size to equal
     * that of the thumb image, otherwise get the default thumb size
     */
    @Override
    public Dimension getThumbSize() {
        Image thumb = slider.getThumbImage();

        if (thumb == null)
            return super.getThumbSize();

        return new Dimension(thumb.getWidth(slider), thumb.getHeight(slider));
    }

    /**
     * Paints the track image. The track image, which is made up of a left,
     * center, and right image, are all buffered into a single track image. This
     * single image is painted each time to draw the entire track.
     */
    @Override
    public void paintTrack(Graphics g) {
		// if our images aren't all here, just fall back to the normal painting
        if( slider.getLeftTrackImage() == null ||  slider.getCenterTrackImage() == null || slider.getRightTrackImage() == null) {
            super.paintTrack(g);
            return;
        }
        try {
            if (trackImageCache == null) { 
                createTrackCache();
            }
            else if(isDirty) {
                if( slider.getOrientation() == JSlider.HORIZONTAL ) {
                    if( trackImageCache.getWidth() != contentRect.width ) {
                        createTrackCache();
                    }
                }
                else {
                    if( trackImageCache.getHeight() != contentRect.height ) {
                        createTrackCache();
                    }
                }
            }
            isDirty = false;
        } catch (IOException e) {
            // if the images for the track can't be found, have super paint
            // default
            super.paintTrack(g);
            return;
        }
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            Rectangle trackBounds = contentRect;

            // center our image in the screen
            int cy = (trackBounds.height - trackImageCache.getHeight()) / 2;

            g.translate(trackBounds.x, trackBounds.y + cy);

            g.drawImage(trackImageCache, 0, 0, null);

            g.translate(-trackBounds.x, -(trackBounds.y + cy));
        } else {
            Rectangle trackBounds = contentRect;

            int cw = (trackBounds.width - trackImageCache.getWidth()) / 2;

            g.translate(trackBounds.x + cw, trackBounds.y);

            g.drawImage(trackImageCache, 0, 0, null);

            g.translate(-(trackBounds.x + cw), -trackBounds.y);
        }
    }

    /**
     * Calculates the track dimensions for the jslider. This is the area painted
     * by the centerImage. This is modified to take into account the left and
     * right images that can be skinned. The left and right image is not
     * considered part of the track and therefor their width is subtracted from
     * each side of the track
     */
    @Override
    protected void calculateTrackRect() {
        // if all the images aren't loaded, fall back to the default painting
        if( slider.getLeftTrackImage() == null ||  slider.getCenterTrackImage() == null || slider.getRightTrackImage() == null) {
            super.calculateTrackRect();
            return;
        }
        BufferedImage left = slider.getLeftTrackImage();
        BufferedImage right = slider.getRightTrackImage();

        int centerSpacing = 0; // used to center sliders added using
                                // BorderLayout.CENTER (bug 4275631)
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            centerSpacing = thumbRect.height;
            if (slider.getPaintTicks())
                centerSpacing += getTickLength();
            if (slider.getPaintLabels())
                centerSpacing += getHeightOfTallestLabel();
            trackRect.x = contentRect.x + left.getWidth() + trackBuffer;
            trackRect.y = contentRect.y
                    + (contentRect.height - centerSpacing - 1) / 2;
            trackRect.width = contentRect.width - trackRect.x
                    - right.getWidth() - trackBuffer;
            trackRect.height = thumbRect.height;
        } else {
            centerSpacing = thumbRect.width;
            if (slider.getComponentOrientation().isLeftToRight()) {
                if (slider.getPaintTicks())
                    centerSpacing += getTickLength();
                if (slider.getPaintLabels())
                    centerSpacing += getWidthOfWidestLabel();
            } else {
                if (slider.getPaintTicks())
                    centerSpacing -= getTickLength();
                if (slider.getPaintLabels())
                    centerSpacing -= getWidthOfWidestLabel();
            }
            trackRect.x = contentRect.x
                    + (contentRect.width - centerSpacing - 1) / 2;
            trackRect.y = contentRect.y + left.getHeight() + trackBuffer;
            trackRect.width = thumbRect.width;
            trackRect.height = contentRect.height - trackRect.x - trackBuffer;
        }
    }

    /**
     * This background track is made up of three images leftTrack + centerTrack +
     * righTrack. In most cases, center track will only be 1 pixel wide for
     * space and speed efficincies. This makes painting the track background
     * very expensive, since this image will very rarely change, we paint it to
     * a sized BufferedImage to save time. This results in about an about an 80%
     * speedup in the painting
     * 
     * @throws IOException
     */
    protected void createTrackCache() throws IOException {
                
        BufferedImage left = slider.getLeftTrackImage();
        BufferedImage center = slider.getCenterTrackImage();
        BufferedImage right = slider.getRightTrackImage();

        if (left == null || center == null || right == null)
            throw new IOException("null track image");

        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            BufferedImage b = new BufferedImage(contentRect.width, center
                    .getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = b.createGraphics();
            g.drawImage(left, trackRect.x - left.getWidth(), 0, null);
            g.drawImage(center, trackRect.x, 0, trackRect.width, center
                    .getHeight(), null);
            g.drawImage(right, (int) trackRect.getWidth() + trackRect.x, 0,
                    null);

            trackImageCache = b;
            g.dispose();
        } else {
            BufferedImage b = new BufferedImage(center.getWidth(),
                    contentRect.height, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = b.createGraphics();
            g.drawImage(left, 0, trackRect.y - left.getHeight(), null);
            g.drawImage(center, 0, trackRect.y, center.getWidth(),
                    trackRect.height, null);
            g.drawImage(right, 0, (int) (trackRect.getHeight() + trackRect.y),
                    null);

            trackImageCache = b;
            g.dispose();
        }
    }

    /**
     * Paints the correct thumbImage. If no image is found, the default thumb is
     * painted. If the thumb is pressed, the thumbPressed image is painted.
     */
    @Override
    public void paintThumb(Graphics g) {

        // if the thumbImage can't be found, have super paint the
        // default
        if (slider.getThumbImage() == null) {
            super.paintThumb(g);
            return;
        }
        Rectangle knobBounds = thumbRect;

        g.translate(knobBounds.x, knobBounds.y + 1);
        g.drawImage(
                (isPressed || slider.getThumbPressedImage() == null) ? slider
                        .getThumbPressedImage() : slider.getThumbImage(), 0, 0,
                null);

        g.translate(-knobBounds.x, -(knobBounds.y + 1));
    }

    /**
     * @return true if the buffered track image needs to be updated, false
     *         otherwise
     */
    public boolean isDirty() {
        return isDirty;
    }

    /**
     * @return the buffered track image
     */
    public BufferedImage getTrackImage() {
        return trackImageCache;
    }

    /**
     * This class listens for mouse presses on the thumb. When the thumb is
     * pressed,if both two images have been loaded for the thumb, the
     * thumbPressedImage will be displayed. This causes a visual indication the
     * user has 'captured' the thumb with the mouse.
     */
    private class ThumbMouse extends MouseAdapter {
        @Override
        public void mouseReleased(MouseEvent e) {
            if (!slider.isEnabled()) {
                return;
            }

            if (isPressed) {
                isPressed = false;
                slider.repaint();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (!slider.isEnabled()) {
                return;
            }

            if (thumbRect.contains(e.getX(), e.getY())) {
                isPressed = true;
                slider.repaint();
            }
        }

        @Override
        public void mouseEntered(MouseEvent arg0) {
        }

        @Override
        public void mouseExited(MouseEvent arg0) {
        }
    }

    /**
     * This class listens for changes to the visibility or size of the JSlider.
     * Since the track is buffered to a certain length and height, we must
     * repaint the buffered track image if a dimension changes.
     */
    private class ResizeListener extends ComponentAdapter {

        @Override
        public void componentResized(ComponentEvent e) {
            setDirty(true);
        }

        @Override
        public void componentShown(ComponentEvent e) {
            setDirty(true);
        }
    }
}
