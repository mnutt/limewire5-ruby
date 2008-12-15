package org.limewire.ui.swing.player;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JSlider;

import org.limewire.ui.swing.components.MediaSliderUI;
import org.limewire.util.CommonUtils;


/**
 * An extension to MediaSliderUI to emulate the L&F of a JProgressBar that
 * is skinnable. A progressBar image is used to paint the progress from 
 * left to whereever the thumb is currently located. A String can also be
 * painted in the track.
 */
public class SongProgressBarUI extends MediaSliderUI {

    /**
     * Caches the progressbar image. We use roughly 1/4 the length
     * of the JSlider
     */
    protected BufferedImage progressImageCache;

    /**
     * flag for painting the thumb. true==mouse has focus in jslider
     * and paint, false otherwise
     */
    private boolean paintThumb = false;

    /**
     * A custom mouse listener for this UI.
     */
    private ThumbMouse thumbMouseListener;

    public SongProgressBarUI(SongProgressBar b) {
        super(b);
    }

    @Override
    protected void installListeners(JSlider slider) {
        super.installListeners(slider);
        thumbMouseListener = new ThumbMouse();
        slider.addMouseListener(thumbMouseListener);
    }

    @Override
    protected void uninstallListeners(JSlider slider) {
        super.uninstallListeners(slider);
        slider.removeMouseListener(thumbMouseListener);
    }

    /**
     *  Paints the track and progressbar. This uses two cached images to
     *  speed up painting. The track background is painted as a single 
     *  image, the progressbar is used to emulate the look and feel of a 
     *  JProgressBar, this is painted ontop of the the track and finally
     *  the text is painted on top of the progress bar.
     */
    @Override
    public void paintTrack(Graphics g) {
    	// if all the images aren't loaded, fall back to default painting
        if( ((SongProgressBar)slider).getProgressImage() == null) {
            super.paintTrack(g);
            return;
        }
        try {
            if (((SongProgressBar) slider).isTimePainted()){
                isDirty = true;
            }
            if (trackImageCache == null || progressImageCache == null) { 
                createTrackCache();
                createProgressCache();
            }
            else if(isDirty) {
                if( slider.getOrientation() == JSlider.HORIZONTAL ) {
                    if( trackImageCache.getWidth() != contentRect.width) { 
                        createTrackCache();
                    }
                    if( progressImageCache.getWidth() != contentRect.width / 4) {
                        createProgressCache();
                    }
                }
                else {
                    if( trackImageCache.getHeight() != contentRect.height ) {
                        createTrackCache();
                    }
                    if( progressImageCache.getHeight() != contentRect.height / 4) {
                        createProgressCache();
                    }
                }
            }
            isDirty = false;
        } catch (IOException e) {
            // if the images for the track can't be found, have super paint 
            //  default
            super.paintTrack(g);
            return;
        }

        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            Rectangle trackBounds = contentRect;

            int cy = (trackBounds.height - trackImageCache.getHeight()) / 2;
            SongProgressBar songSlider = (SongProgressBar) slider;

            g.translate(trackBounds.x, trackBounds.y + cy);

            // draw track
            g.drawImage(trackImageCache, 0, 0, null);

            // draw progress bar
            if (progressImageCache != null
                    && slider.getValue() > slider.getMinimum()) {
                if (progressImageCache.getWidth() < trackBounds.width / 4)
                    createProgressCache();
                int value = ((trackRect.width * songSlider.getValue()) / slider
                        .getMaximum());

                g.drawImage(progressImageCache, trackRect.x, 0, value,
                        progressImageCache.getHeight(), null);
            }

            //draw string
            // paint the string in the background
            if (songSlider.isStringPainted()
                    && songSlider.getString().length() > 0) {
                FontMetrics metrics = g.getFontMetrics(slider.getFont());

                int fontHeight = metrics.getHeight();

                int x = (trackRect.x + 6);
                int y = trackBounds.height / 2 + (fontHeight) / 3 - cy;

                Graphics2D g2 = (Graphics2D) g;
                g2.setFont(songSlider.getFont());

                g2.setColor(Color.BLACK);
                g2.drawString(songSlider.getString(), x, y);
            }
            
            if (songSlider.isTimePainted()){
                FontMetrics metrics = g.getFontMetrics(slider.getFont());

                int fontHeight = metrics.getHeight();

                int x = trackRect.x;
                int y = trackBounds.height / 2 + (fontHeight) / 3 - cy;

                Graphics2D g2 = (Graphics2D) g;
                //TODO:BOLD
                g2.setFont(songSlider.getFont());

                g2.setColor(Color.BLACK);

                g2.drawString(CommonUtils.seconds2time(songSlider.getValue()), x, y);
                String maxTime = CommonUtils.seconds2time(songSlider.getMaximum());
                int maxWidth = (int)metrics.getStringBounds(maxTime, g2).getWidth();
                g2.drawString(maxTime, songSlider.getWidth() - maxWidth - 6, y);
            }

            g.translate(-trackBounds.x, -(trackBounds.y + cy));
        } else {
            super.paintTrack(g);
        }
    }
    
    /**
     * This caches the progress bar image for faster rendoring. Typically
     * the progress bar image is 1 pixel wide for space and speed in loading
     * the image. This image is stretched across the jslider to fill the 
     * appropriate length. This prestores the image strechted to about 1/4
     * the length of the jslider. No significant speedups on stretching it
     * further were shown.
     */
    public void createProgressCache() {

        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            SongProgressBar slide = (SongProgressBar) slider;
            BufferedImage i = new BufferedImage(contentRect.width / 4, slide
                    .getProgressImage().getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics g = i.createGraphics();
            g.drawImage(slide.getProgressImage(), 0, 0, contentRect.width / 4,
                    slide.getProgressImage().getHeight(), null);

            progressImageCache = i;
            g.dispose();
        } else {
            SongProgressBar slide = (SongProgressBar) slider;
            BufferedImage i = new BufferedImage(slide.getProgressImage()
                    .getWidth(), (int) (contentRect.getHeight() / 4),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics g = i.createGraphics();
            g.drawImage(slide.getProgressImage(), 0, 0, slide
                    .getProgressImage().getWidth(), (int) (contentRect
                    .getHeight() / 4), null);

            progressImageCache = i;
            g.dispose();
        }
    }

    /**
     * This is overriden to hide the thumb whenever there is no mouse over
     * event
     */
    @Override
    public void paintThumb(Graphics g) {
        if (!paintThumb || !slider.isEnabled()) {
            return;
        } else {
            super.paintThumb(g);
        }
    }

    /**
     * Overrides the mouse press increment when a mouse click occurs in the 
     * jslider. Repositions the jslider directly to the location the mouse
     * click avoiding the standard step increment with each click
     * @param x - location of mouse click
     */
    protected void mouseSkip(int x) {
        slider.setValue(this.valueForXPosition(x));
        slider.repaint();
    }

    /**
     * This class hides the thumb anytime the mouse exits the focus of the
     * JSlider. This is done to mimic the current progressbar used to display
     * progress in a song.
     * 
     */
    private class ThumbMouse extends MouseAdapter {
        /**
         * Reposition the thumb on the jslider to the location of the mouse
         * click
         */
        @Override
        public void mousePressed(MouseEvent e) {
            if (!slider.isEnabled())
                return;

            mouseSkip(e.getX());
        }

        /**
         * If the mouse has entered the focus of the jslider, paint the thumb
         */
        @Override
        public void mouseEntered(MouseEvent arg0) {
            if (!slider.isEnabled()) {
                return;
            }
            paintThumb = true;
            slider.repaint();
        }

        /**
         * If the mouse exits the focus of the jslider, hide the thumb
         */
        @Override
        public void mouseExited(MouseEvent arg0) {
            if (!slider.isEnabled()) {
                return;
            }
            paintThumb = false;
            slider.repaint();
        }
    }
}
