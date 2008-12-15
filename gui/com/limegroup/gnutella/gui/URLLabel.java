package com.limegroup.gnutella.gui;

import java.awt.Color;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.net.URL;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.UIManager;

import com.limegroup.gnutella.gui.actions.LimeAction;

/**
 * A label that has a clickable text. The text is rendered as an HTML link and
 * the mouse cursor is changed when the mouse hovers over the label.
 */
public class URLLabel extends JLabel  {
    
    private MouseListener urlListener;
    
    private PropertyChangeListener listener = null;

    private Action currentAction;
    
    private String url = "";
      
    private String text;
    
    private Color linkColor = UIManager.getColor("Label.foreground");
    /**
     * Constructs a new clickable label.
     * 
     * @param url the URL to open when the label is clicked
     * @param display the label's text
     */
    public URLLabel(final URL url, final String display) {
        this(url.toExternalForm(), display);
    }
    
    /**
     * Constructs a new clickable label.
     * 
     * @param uri the URL to open when the label is clicked
     * @param display the label's text
     */
    public URLLabel(final URI uri, final String display) {
        this(uri.toString(), display);
    }
    
    /**
     * Constructs a new clickable label with <code>url</code> as the
     * text.
     * 
     * @param url the URL to open when the label is clicked
     */
    public URLLabel(final String url) {
        this(url, url);
    }
    
    /**
     * Constructs a new clickable label.
     * 
     * @param url the URL to open when the label is clicked
     * @param text the label's text
     */
    public URLLabel(final String url, final String text) {
        this.url = url;
        setText(text);
        setToolTipText(url);
        installListener(GUIUtils.getURLInputListener(url));
    }
    
    
    /**
     * Constructs a new clickable label with an icon only.
     * 
     * @param url the URL to open when the label is clicked
     * @param icon the icon to display
     */    
    public URLLabel(final String url, final Icon icon) {
       this.url = url;
       setText(null);
       setIcon(icon);
       setToolTipText(url);
       installListener(GUIUtils.getURLInputListener(url));
    }    

   

    /**
     * Constructs a new clickable label whose text is in the hex color described.
     * 
     * @param action
     * @param color
     */
    public URLLabel(Action action) {
        setAction(action);
       
    }
    
    @Override
    public void setText(String text) {
        this.text = text;
        String htmlString = null;
        if(text != null) {
            htmlString = ("<html><a href=\"" + url + "\"" + 
                (linkColor != null ? "color=\"#" + GUIUtils.colorToHex(linkColor) + "\"" : "") +
                ">" + text + "</a></html>");
        }

        super.setText(htmlString);
    }
   
    
    public void setAction(Action action) {
        // remove old listener
        Action oldAction = getAction();
        if (oldAction != null) {
            oldAction.removePropertyChangeListener(getListener());
        }

        // add listener
        currentAction = action;
        currentAction.addPropertyChangeListener(getListener());
        installListener(GUIUtils.getURLInputListener(action));
        updateLabel();
    }
    
    
    public void setColor(Color fg) {
        linkColor = fg;
        setText(text);
    }
    
    public Action getAction(){
        return currentAction;
    }
       
    private PropertyChangeListener getListener() {
        if (listener == null) {
            listener = new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) { 
                    //update label properties
                    updateLabel();
                    
                }
            };
        }
        return listener;
    }
    
    /*
     * Update label text based on action event
     */
    public void updateLabel() {
        if (currentAction != null) {
            String display = (String) currentAction.getValue(Action.NAME);
            Color color = (Color) currentAction.getValue(LimeAction.COLOR);
            if (color != null)
                setColor(color);

            setIcon((Icon) currentAction.getValue(Action.SMALL_ICON));
            setToolTipText((String) currentAction.getValue(Action.SHORT_DESCRIPTION));

            // display
            setText(display);
        } else {
            setText(text);
            setToolTipText(url);
        }
    }
   
    private void installListener(MouseListener listener) {
        if (urlListener != null) {
            removeMouseListener(urlListener);
        }
        urlListener = listener;
        addMouseListener(urlListener);
    }
}