package com.limegroup.gnutella.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.text.NumberFormat;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.limewire.util.OSUtils;

import com.limegroup.gnutella.gui.GUIUtils.SizePolicy;
import com.limegroup.gnutella.gui.themes.ThemeFileHandler;

/**
 * Displays a status update in various ways, depending on the
 * operating system & JDK.
 * 
 * OSX:
 *   - Displays an indeterminate JProgressBar with a JLabel
 *     left justified above it.
 * w/o OSX:
 *   - Displays an indeterminate JProgressBar with the status text
 *     inside the progressbar.
 */
public class StatusComponent extends JPanel {
    
    /** The JProgressBar whose text is updated, if not running on OSX. */
    private final JProgressBar BAR;
    
    /** The JLabel being updated if this is running on OSX. */
    private final JLabel LABEL;
    
    /** Whether or not this status component is using steps. */
    private final boolean STEPPING;
    
    /** The NumberFormat being used for stepping. */
    private final NumberFormat NF;
        
    /** Creates a new StatusComponent with an indeterminate progressbar. */
    public StatusComponent() {
        STEPPING = false;
        NF = null;
        LABEL = new JLabel();
        BAR = new LimeJProgressBar();
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));        
        construct();
        GUIUtils.setOpaque(false, this);
        if(BAR != null && !OSUtils.isMacOSX())
            BAR.setOpaque(true);
        if(LABEL != null)
            LABEL.setForeground(ThemeFileHandler.WINDOW4_COLOR.getValue());
        BAR.setIndeterminate(true);
    }
    
    /** Creates a new StatusComponent with the specified number of steps. */
    public StatusComponent(int steps) {
        STEPPING = true;
        LABEL = new JLabel();
        LABEL.setFont(LABEL.getFont().deriveFont(Font.BOLD)); 
        BAR = new LimeJProgressBar();
        Dimension prefSize = BAR.getPreferredSize();
        BAR.setPreferredSize(new Dimension(prefSize.width, 13));
        GUIUtils.restrictSize(BAR, SizePolicy.RESTRICT_HEIGHT);
        NF = NumberFormat.getInstance(GUIMediator.getLocale());
        NF.setMaximumIntegerDigits(3);
        NF.setMaximumFractionDigits(0);
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        construct();
        GUIUtils.setOpaque(false, this);
        if(LABEL != null)
            LABEL.setForeground(ThemeFileHandler.WINDOW4_COLOR.getValue());
        
        BAR.setMaximum(steps+1);
        BAR.setMinimum(0);
        BAR.setValue(0);

    }
    
    /**
     * Sets the preferred size of the progressbar.
     */
    public void setProgressPreferredSize(Dimension dim) {
        setMinimumSize(dim);
        setMaximumSize(dim);
        setPreferredSize(dim);
        if(BAR != null) {
            BAR.setMinimumSize(dim);
            BAR.setMaximumSize(dim);
            BAR.setPreferredSize(dim);
        }
    }
    
    /**
     * Updates the status of this component.
     */
    public void setText(String text) {
        if(STEPPING) {
            BAR.setValue(BAR.getValue() + 1);
            String percent = NF.format(((double)BAR.getValue() / (double)BAR.getMaximum() * 100d));
            text = percent + "% (" + text + ")";
        }
            
        if(STEPPING || OSUtils.isMacOSX())
            LABEL.setText(text);
        else
            BAR.setString(text);
    }
    
    /**
     * Constructs the panel.
     */
    private void construct() {
        if(STEPPING || OSUtils.isMacOSX()) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(LABEL, BorderLayout.SOUTH);
            add(panel);
            add(Box.createVerticalStrut(9));
        } else {
            BAR.setStringPainted(true);
        }
        add(BAR);
        
    }
}                
        
