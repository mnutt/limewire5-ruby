package com.limegroup.gnutella.gui.mp3;

import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.JProgressBar;
import javax.swing.event.ChangeEvent;
import javax.swing.plaf.SliderUI;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.MediaSlider;
import com.limegroup.gnutella.gui.MediaSliderUI;
import com.limegroup.gnutella.gui.themes.ThemeMediator;

/**
 *  A skinnable JSlider that mimics the behaviour of a progressbar. This
 *  class uses six images to completely skin the jslider. The background
 *  is painted using leftImage + centerImage + rightImage. Typically
 *  the centerImage is 1 pixel wide for speed and space. The center image
 *  is then stretched to fill the entire trackRectangle space. The progress
 *  Image is also typically 1 pixel wide. This is used to paint the
 *  progress across the track. Finally two image are used to paint the thumb.
 */
public class SongProgressBar extends MediaSlider {
	
    /**
     * An optional string that can be displayed on the progress bar.The default
     * is <code>null</code>. Setting this to a non-<code>null</code> value
     * does not imply that the string will be displayed. To display the string,
     * {@code paintString} must be {@code true}.
	 */
    private String progressString;
	
	/**
     * When set to true, the current value in progressString is displayed in 
     * the background of the progressbar
     */
    private boolean paintString = true;
	
	/**
     * Represents the progress bar image to load
     */
    private final String PROGRESS;

    private BufferedImage progressImage;

    public SongProgressBar(String leftTrackName, String centerTrackName,
            String rightTrackName, String thumbName, String thumbPressedName,
            String progressName) {
        super(leftTrackName, centerTrackName, rightTrackName, thumbName,
                thumbPressedName);

        PROGRESS = progressName;

        /**
         * Load a default font if no font is loaded already
         */
        if (!super.isFontSet()) {
            super.setFont(new Font("Default", Font.PLAIN, 9));
        }

        ThemeMediator.addThemeObserver(this);

        setMyImages();

        setUI(new SongProgressBarUI(this));
	}

	/**
     * Reloads all the images for the slider using the images from
     * the new theme
	 */
    @Override
    public void updateTheme() {
        setMyImages();

        ((MediaSliderUI) getUI()).setDirty(true);
	}

	/**
     * Overrides the UI to only accept UIs derived from the songProgressUI,
     * this ensures that all the appropriate images are accessable
	 */
    @Override
    public void setUI(SliderUI sliderUI) {
        if (sliderUI instanceof SongProgressBarUI)
            super.setUI(sliderUI);
	}

	/**
     *  Loads the images
	 */
    protected void setMyImages() {
        super.setImages();
        setProgressImage(convertIconToImage(GUIMediator.getThemeImage(PROGRESS)));
    }

    public void setProgressImage(Image image) {
        progressImage = (BufferedImage) image;
    }

    public BufferedImage getProgressImage() {
        return progressImage;
	}

	/**
     * @return orientation of the progress bar
	 */
    @Override
    public int getOrientation() {
        return JProgressBar.HORIZONTAL;
	}

	/**
     * Sets the string that should be painted in the background of the 
     * progress bar. Strings that are above certain length should be 
     * substringed and cycled
	 */
    public void setString(String name) {
        progressString = name;
        repaint();
    }
		
    /**
     * Returns the currently displayed string
     */
    public String getString() {
        return progressString;
	}
	
	/**
     * Toggles whether the current string should be painted
	 *
     * @param value == true : paint string ? don't paint string
	 */
    public void setPaintString(boolean value) {
        if (paintString != value) {
            paintString = value;
		repaint();
	}
    }

	/**
     * @return true if the string is to be painted in the background of the 
     * slider, false otherwise
	 */
    public boolean isStringPainted() {
        return paintString;
	}
    
    @Override
    public void stateChanged(ChangeEvent e) {
        this.setToolTipText(Integer.toString(( 100 * getValue())/getMaximum()));
	}
}
