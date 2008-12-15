package com.limegroup.gnutella.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.plaf.metal.MetalLabelUI;

import com.limegroup.gnutella.version.UpdateInformation;

final class UpdatePanel extends JLabel implements RefreshListener {

    private final String labelString = 
        I18n.tr("A newer version is available, update?");

    /**
     * Whether a new update is available.
     */
    private volatile boolean _updateAvailable;

    /**
     * The most recent UpdateInformation we know about.
     */
    private UpdateInformation _info;
    
    /**
     * Cached UpdateDialog.  Written to only from the awt thread, read from other threads.
     */
    private UpdateDialog _dialog;

    UpdatePanel() {
        super(I18n.tr("A newer version is available, update?"), SwingConstants.CENTER);
        //make the font so that it looks like a link
        setUI(new LinkLabelUI());
		FontMetrics fm = getFontMetrics(getFont());
  		int width = fm.stringWidth(labelString);
  		Dimension dim = new Dimension(width, fm.getHeight());
        //link color, could grab system attribute as well
		setForeground(Color.red); 
  		setPreferredSize(dim);
  		setMaximumSize(dim);

        //add a mouse listener
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                UpdatePanel.this.handleClick();
            }
            //change cursor, we are on a link
            @Override
            public void mouseEntered(MouseEvent e) { 
                if(_updateAvailable) 
                    e.getComponent().setCursor
                    (Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}
            //change back to normal
			@Override
            public void mouseExited(MouseEvent e) {
                if(_updateAvailable)
                    e.getComponent().setCursor(Cursor.getDefaultCursor()); 
			}
        });

        //add the link
        GUIMediator.addRefreshListener(this);
        //keep it invisible
        setVisible(false);
        _updateAvailable = false;
    }
    
    public void makeVisible(boolean popup, UpdateInformation info) {
        
        // update the dialog and dispose the old one
        Runnable disposer = null;
        synchronized(this) {
            _updateAvailable = true;
            _info = info;
            
        	final UpdateDialog currentDialog = _dialog;
        	
        	if (_info != null )
        		_dialog = new UpdateDialog(_info);
        	
        	if (currentDialog != null) {
        		disposer = new Runnable() {
        			public void run() {
        				currentDialog.setVisible(false);
        				currentDialog.dispose();        
        			}
        		};
        	}
        }
        
        if (disposer != null)
        	GUIMediator.safeInvokeLater(disposer);
        
        super.setVisible(true);
        
        if (popup) {
            GUIMediator.safeInvokeLater(new Runnable() {
                public void run() {
                    handleClick();
                }
            });
        }
    }

	/**
	 * Returns true if this update panel should be shown; namely, if
	 * an update is available. 
	 */
	public boolean shouldBeShown() {
		return _updateAvailable;
	}
	
    private void handleClick() {
        if(!_updateAvailable) //not visible? no update yet
            return;
        
        synchronized(this) {
        	if(_info != null) {
                GUIUtils.centerOnScreen(_dialog);
        		_dialog.setVisible(true);
        	}
        }
    }
    
    public void refresh() {
        if (!_updateAvailable)
            return;
        Color currCol = getForeground();
        if (currCol.equals(Color.red))
            setForeground(Color.black);
        if (currCol.equals(Color.black))
            setForeground(Color.red);           
    }


    private class LinkLabelUI extends MetalLabelUI {
        /**
         * Paint clippedText at textX, textY with the labels foreground color.
         * 
         * @see #paint
         * @see #paintDisabledText
         */
        @Override
        protected void paintEnabledText(JLabel l, Graphics g, String s, 
                                        int textX, int textY) {
            super.paintEnabledText(l, g, s, textX, textY);
			if (getText() == null)
                return;
			
			FontMetrics fm = g.getFontMetrics();
			g.fillRect(textX, fm.getAscent()+2, 
                       fm.stringWidth(getText()) - 
					   getInsets().right, 1); //X,Y,WIDTH,HEIGHT
            
        }        
    }
}
