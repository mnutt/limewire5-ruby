package com.limegroup.gnutella.gui;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.SliderUI;

import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeObserver;

/**
 * This is a skinnable JSlider. It uses a custom UI that takes a series of
 * images and paints the jSlider. The track is made up of three images, a left,
 * center and right. In most cases, the center will be a single pixel wide or
 * tall depending on the orientation of the slider. This is to conserve space
 * and speed up processing. The center image will be stretched by the UI to
 * paint the entire track in between the left and right images of the track.
 * 
 * The thumb has two images, pressed and unpressed. If pressed is null, the
 * default is unpressed. If any image besides pressed is null, the default
 * component will be painted instead.
 */
public class MediaSlider extends JSlider implements ThemeObserver, ChangeListener {

    /**
     * This represents the left end of the jslider track
     */
    private final String LEFT_TRACK;

    /**
     * This represents the right end of the jslider track
     */
    private final String RIGHT_TRACK;

    /**
     * This represents the center of the jslider track. This image should only
     * be 1 pixel wide for speed & space saving if the track remains the same
     * across the entire JSlider
     */
    private final String CENTER_TRACK;

    /**
     * The thumb image
     */
    private final String THUMB;

    /**
     * Optional value, this represents the thumb when the thumb is pressed with
     * the mouse
     */
    private final String THUMB_PRESSED;

    private BufferedImage leftTrackImage;

    private BufferedImage rightTrackImage;

    private BufferedImage centerTrackImage;

    private BufferedImage thumbImage;

    private BufferedImage thumbPressedImage;

    public MediaSlider(String leftTrackName, String centerTrackName,
            String rightTrackName, String thumbName, String thumbPressedName) {

        LEFT_TRACK = leftTrackName;
        RIGHT_TRACK = rightTrackName;
        CENTER_TRACK = centerTrackName;
        THUMB = thumbName;
        THUMB_PRESSED = thumbPressedName;

        this.setFocusable(false);

        ThemeMediator.addThemeObserver(this);

        setImages();

        setUI(new MediaSliderUI(this));
        
        this.addChangeListener(this);
    }

    /**
     * This only allows UIs that are subclassed from our own MediaSliderUI
     */
    @Override
    public void setUI(SliderUI sliderUI) {
        if (sliderUI instanceof MediaSliderUI)
            super.setUI(sliderUI);
    }

    /**
     * Loads the images to be painted. This gets called anytime the theme
     * changes and the images need to be updated.
     */
    protected void setImages() {
        setLeftTrackImage(convertIconToImage(GUIMediator
                .getThemeImage(LEFT_TRACK)));
        setRightTrackImage(convertIconToImage(GUIMediator
                .getThemeImage(RIGHT_TRACK)));
        setCenterTrackImage(convertIconToImage(GUIMediator
                .getThemeImage(CENTER_TRACK)));
        setThumbImage(convertIconToImage(GUIMediator.getThemeImage(THUMB)));
        setThumbPressedImage(convertIconToImage(GUIMediator
                .getThemeImage(THUMB_PRESSED)));
    }

    /**
     * When the theme changes, load the new images and repaint the buffered
     * track image
     */
    public void updateTheme() {
        setImages();

        ((MediaSliderUI) getUI()).setDirty(true);
    }

    /**
     * Converts the image stored in an ImageIcon into a BufferedImage. If the
	 * image is null or has not been completely loaded or loaded with errors
	 * returns null;
     */
    public static BufferedImage convertIconToImage(ImageIcon icon) {

		// make sure we have a valid image and it is loaded already
        if( icon == null || icon.getImageLoadStatus() != MediaTracker.COMPLETE )
            return null;
        BufferedImage image = new BufferedImage(icon.getIconWidth(),
                icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D bufImageGraphics = image.createGraphics();
        bufImageGraphics.drawImage(icon.getImage(), 0, 0, null);

        bufImageGraphics.dispose();

        return image;
    }

    public BufferedImage getLeftTrackImage() {
        return leftTrackImage;
    }

    public BufferedImage getCenterTrackImage() {
        return centerTrackImage;
    }

    public BufferedImage getRightTrackImage() {
        return rightTrackImage;
    }

    public BufferedImage getThumbImage() {
        return thumbImage;
    }

    public BufferedImage getThumbPressedImage() {
        return thumbPressedImage;
    }

    public void setLeftTrackImage(Image image) {
        leftTrackImage = (BufferedImage) image;
    }

    public void setCenterTrackImage(Image image) {
        centerTrackImage = (BufferedImage) image;
    }

    public void setRightTrackImage(Image image) {
        rightTrackImage = (BufferedImage) image;
    }

    public void setThumbImage(Image image) {
        thumbImage = (BufferedImage) image;
    }

    public void setThumbPressedImage(Image image) {
        thumbPressedImage = (BufferedImage) image;
    }

    public void stateChanged(ChangeEvent e) {
        this.setToolTipText(Integer.toString(getValue()));
    }
}
