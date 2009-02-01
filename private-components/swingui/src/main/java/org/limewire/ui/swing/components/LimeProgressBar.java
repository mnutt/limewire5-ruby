package org.limewire.ui.swing.components;

import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.JProgressBar;

import org.jdesktop.swingx.painter.AbstractPainter;

/**
 * Progress bar that uses painters.
 * 
 * This component implements a lazy caching update model.  
 *  If cached changing the model will not fire repaints.
 */
public class LimeProgressBar extends JProgressBar {

    private static final boolean CACHING_SUPPORTED = true;
    
    private AbstractPainter<JProgressBar> foregroundPainter;
    private AbstractPainter<JComponent> backgroundPainter;
    
	private boolean isHidden = false;
	 
	public LimeProgressBar() {
	}
	
	public LimeProgressBar(int min, int max) {
	    super(min, max);        
    }

    public void setForegroundPainter(AbstractPainter<JProgressBar> painter) {
	    this.foregroundPainter = painter;
	    painter.setCacheable(true);
	}
	
	public void setBackgroundPainter(AbstractPainter<JComponent> painter) {
        this.backgroundPainter = painter;
    }

	/**
	 * Note: This component implements a lazy caching update model.  
     *        If cached changing the model will not fire repaints.
	 */
	public boolean hasCacheSupport() {
	    return CACHING_SUPPORTED;
	}
	
	@Override 
	public void setValue(int v) {
	    if (this.getValue() != v) {
            this.foregroundPainter.clearCache();
            this.backgroundPainter.clearCache();
        }
	    super.setValue(v);
	}
	
	@Override
	public void setEnabled(boolean b) {
	    if (this.isEnabled() != b) {
            this.foregroundPainter.clearCache();
            this.backgroundPainter.clearCache();
        }
	    super.setEnabled(b);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
	    if (foregroundPainter == null || backgroundPainter == null) {
	        super.paintComponent(g);
	    }
	    else if (!isHidden) {
	        backgroundPainter.paint((Graphics2D) g, this, this.getWidth(), this.getHeight());
	        foregroundPainter.paint((Graphics2D) g, this, this.getWidth(), this.getHeight());
		}
	}
	
	/**
	 * Hides progress bar but maintains size and position in layout.
	 * 
	 * @param isHidden true to hide progress bar
	 */
	public void setHidden(boolean isHidden){
		this.isHidden = isHidden;
	}

}
