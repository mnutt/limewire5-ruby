package com.limegroup.gnutella.gui.startup;

import com.limegroup.gnutella.gui.I18n;

/**
 * Contains information displayed in a Go Pro dialog at startup. 
 * Contains the data for the Title, the message to display, various urls 
 * and a probability for how often to display this particular message
 */
public class StartupAd implements Comparable<StartupAd> {

    /**
     * Title of the dialog
     */
    private final String title;

    /**
     * Main message to display
     */
    private final String message;
    
    /**
     * Message above the buttons
     */
    private final String buttonMessage;
    
    /**
     * Text of first button
     */
    private final String button1;
    
    /**
     * Tooltip of first button
     */
    private final String button1Tooltip;
    
    /**
     * URL of first button, if this is null no action will be assigned to
     * the button
     */
    private final String urlButton1;
    
    /**
     * Text of second button in the row
     */
    private final String button2;
    
    /**
     * Tooltip of second button
     */
    private final String button2Tooltip;
    
    /**
     * URL of second button, if this is null no action will be assigned to 
     * the button
     */
    private final String urlButton2;
    
    /**
     * Text of third button in the row
     */
    private final String button3;
    
    /**
     * Tooltip of third button
     */
    private final String button3Tooltip;
    
    /**
     * URL of third button, if this is null no action will be assigned to
     * the button
     */
    private final String urlButton3;
    
    /**
     * URL to background image, this will not be displayed if local 
     * image loading is used instead
     */
    private final String urlImage;
    
    /**
     * How often to show this message bundle
     */
    private final float probability;
    
    
    /**
     * Takes a set of English inputs, Title, Text, and Key will all be translated if a valid
     * translation exists
     * 
     * @param title - title of the dialog
     * @param text - main message to display
     * @param key - message describing the buttons
     * @param urlYes - url for yes button to action on
     * @param urlWhy - url for why button to action on
     * @param probability - how often to show this message
     */
    public StartupAd(String title, String message, String key, String b1, String b1Tooltip,
            String b2, String b2Tooltip, String b3, String b3Tooltip, String urlButton1, 
            String urlButton2, String urlButton3, String urlImage, float probability) {
        this.title = getValue(title);
        this.message = getValue(message);
        this.buttonMessage = getValue(key);
        this.button1 = getValue(b1); 
        this.button1Tooltip = getValue(b1Tooltip);
        this.button2 = getValue(b2);
        this.button2Tooltip = getValue(b2Tooltip);
        this.button3 = getValue(b3);
        this.button3Tooltip = getValue(b3Tooltip);
        this.urlButton1 = getURL(urlButton1);
        this.urlButton2 = getURL(urlButton2);
        this.urlButton3 = getURL(urlButton3);
        this.urlImage = getURL(urlImage);
        this.probability = probability; 
    }
    
    private String getValue(String input) {
        if(input == null || input.length() == 0)
            return null;
        else
            return I18n.tr(input);
    }
    
    private String getURL(String input) {
        if(input == null || input.length() == 0)
            return null;
        else
            return input;
    }
    
    /**
     * @return the String to set as the dialog title, this title will be
     * a translated value if a valid translation exists
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * @return the main message to display, this String will be a 
     * translated value if a valid translation exists
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * @return the String to display describing the buttons, this String
     * will be translated if a valid translation exists
     */
    public String getButtonMessage() {
        return buttonMessage;
    }
    
    public String getButton1Text(){
        return button1;
    }
    
    public String getButton1ToolTip(){
        return button1Tooltip;
    }
    
    public String getButton2Text(){
        return button2;
    }
    
    public String getButton2ToolTip(){
        return button2Tooltip;
    }
    
    public String getButton3Text(){
        return button3;
    }
    
    public String getButton3ToolTip(){
        return button3Tooltip;
    }
    
    /**
     * @return the URL page if "Yes" is pressed
     */
    public String getURLButton1() {
        return urlButton1;
    }
    
    /**
     * @return the URL page if more info is requested
     */
    public String getURLButton2() {
        return urlButton2;
    }
    
    public String getURLButton3() {
        return urlButton3;
    }
    
    /**
     * @return the URL for a remote image
     */
    public String getURLImage() {
        return urlImage;
    }
    
    /**
     * @return the probability for displaying this message at startup
     */
    public float getProbability() {
        return probability;
    }
    
    /**
     * Sort by probability, highest first
     */
    public int compareTo(StartupAd other) {
        return -Float.compare(getProbability(), other.getProbability());
    }
}
