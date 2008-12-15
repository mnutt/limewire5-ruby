package com.limegroup.gnutella.gui;

import java.awt.Component;
import java.awt.LayoutManager;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

/** 
 * This is a reusable class that creates a titled panel with the specified 
 * title and the specified padding both surrounding the panel and inside
 * the panel.  This panel also uses <tt>BoxLayout</tt> on the outer panels 
 * for layout purposes.<p>  
 * 
 * The inner panel also defaults to <tt>BoxLayout</tt>, but the user can change 
 * the layout with the overridden setLayout method.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public class TitledPaddedPanel extends JPanel {	

	/**
	 * Constant for specifying an x axis orientation for the layout.
	 */
	public static final int X_AXIS = 20;

	/**
	 * Constant for specifying a y axis orientation for the layout.
	 */
	public static final int Y_AXIS = 21;

	/**
	 * The number of pixels making up the margin of a titled panel.
	 */
    private static final int TITLED_MARGIN = 6;

	/**
	 * The number of pixels in the margin of a padded panel.
	 */
    private static final int OUT_MARGIN = 6;

	/**
	 * The inner panel that components are added to.
	 */
    private BoxPanel _mainPanel;

	/**
	 * The <tt>TitledBorder</tt> for the panel, stored to allow changing 
	 * the title.
	 */
	private TitledBorder _titledBorder;

    /** 
	 * Creates a <tt>TitledPaddedPanel</tt> with the specified title
	 * and the specified outer and inner padding.  The underlying 
	 * <tt>JPanel</tt> uses <tt>BoxLayout</tt> oriented along the y axis.
	 *
	 * @param title the title of the panel
	 * @param outerPad the padding to use on the outside of the titled border
	 * @param innerPad the padding to use on the inside of the titled border
	 */
    public TitledPaddedPanel(String title, int outerPad, int innerPad) {
        JPanel titlePanel = new JPanel();
        _mainPanel        = new BoxPanel();
        BoxLayout layout      = new BoxLayout(this,
											  BoxLayout.Y_AXIS);
        BoxLayout titleLayout = new BoxLayout(titlePanel,
											  BoxLayout.Y_AXIS);
        // the titled border adds padding above and below the title
        Border outerBorder = BorderFactory.createEmptyBorder((outerPad > 6) ? outerPad - 6 : 0,
															 outerPad,
															 outerPad,
															 outerPad);
        _titledBorder = BorderFactory.createTitledBorder(title);
        Border innerBorder = BorderFactory.createEmptyBorder((innerPad > 6) ? innerPad - 6 : 0,
															 innerPad,
															 innerPad,
															 innerPad);
        setLayout(layout);
        titlePanel.setLayout(titleLayout);
        setBorder(outerBorder);
        titlePanel.setBorder(_titledBorder);
        _mainPanel.setBorder(innerBorder);
        titlePanel.add(_mainPanel);
        super.add(titlePanel);
    }

	/**
	 * Creates a <tt>TitledPaddedPanel</tt> with the empty string as 
	 * its title.
	 */
	public TitledPaddedPanel() {
		this("", OUT_MARGIN, TITLED_MARGIN);
	}
	
	/**
	 * Creates a <tt>TitledPaddedPanel</tt> with the specified title.
	 *
	 * @param title the title to use for the panel
	 */
	public TitledPaddedPanel(String title) {
		this(title, OUT_MARGIN, TITLED_MARGIN);
	}

	/**
	 * Creates a <tt>TitledPaddedPanel</tt> with the specified title
	 * and the specified orientation for the inner panel.
	 *
	 * @param title the title to use for the panel
	 * @param orientation the orientation to use for the layout of the 
	 *                    inner panel
	 */
	public TitledPaddedPanel(String title, int orientation) 
		throws IllegalArgumentException {
		this(title, OUT_MARGIN, TITLED_MARGIN);
		if(orientation != X_AXIS && orientation != Y_AXIS)
			throw new IllegalArgumentException("Invalid orientation");
		if(orientation == X_AXIS) {
			setInnerLayout(new BoxLayout(_mainPanel, 
										 BoxLayout.X_AXIS));
		}
	}

	/**
	 * Sets the title displayed in the panel.
	 *
	 * @param title the title to use for the panel.
	 */
	public void setTitle(String title) {
		_titledBorder.setTitle(title);
	}


	/**
	 * Accessor for the title displayed in the panel.
	 *
	 * @return the title to use for the panel
	 */
	public String getTitle() {
		return _titledBorder.getTitle();
	}

    /** 
	 * Sets the layout for the main panel directly.
	 *
	 * @param mgr the <tt>LayoutManager</tt> to use for the layout
	 */
    public void setInnerLayout(LayoutManager mgr) {
        _mainPanel.setLayout(mgr);
    }

    /** 
	 * Overrides the add(Component comp) method in the <tt>Container</tt>
	 * class, adding the <tt>Component</tt> to the inner panel.
	 *
	 * @param comp the <tt>Component</tt> to add
	 */
    @Override
    public Component add(Component comp) {
        _mainPanel.addRight((JComponent)comp);
        return comp;
    }

    public Component addLeft(Component comp) {
        _mainPanel.addLeft((JComponent)comp);
        return comp;
    }

}
