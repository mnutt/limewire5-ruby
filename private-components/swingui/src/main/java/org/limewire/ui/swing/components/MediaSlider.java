package org.limewire.ui.swing.components;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.plaf.SliderUI;


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
public class MediaSlider extends JSlider {

  
    /**
     * This represents the left end of the jslider track
     */
    private BufferedImage leftTrackImage;
    /**
     * This represents the right end of the jslider track
     */
    private BufferedImage rightTrackImage;
    /**
     * This represents the center of the jslider track. This image should only
     * be 1 pixel wide for speed & space saving if the track remains the same
     * across the entire JSlider
     */
    private BufferedImage centerTrackImage;
    /**
     * The thumb image
     */
    private BufferedImage thumbImage;
    /**
     * Optional value, this represents the thumb when the thumb is pressed with
     * the mouse
     */
    private BufferedImage thumbPressedImage;

    public MediaSlider(ImageIcon leftTrackImage, ImageIcon centerTrackImage,
            ImageIcon rightTrackImage, ImageIcon thumbImage, ImageIcon thumbPressedImage) {

        this.leftTrackImage = convertIconToImage(leftTrackImage);
        this.rightTrackImage = convertIconToImage(rightTrackImage);
        this.centerTrackImage = convertIconToImage(centerTrackImage);
        this.thumbImage = convertIconToImage(thumbImage);
        this.thumbPressedImage = convertIconToImage(thumbPressedImage);

        this.setFocusable(false);
        
        setUI(new MediaSliderUI(this));
        
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
