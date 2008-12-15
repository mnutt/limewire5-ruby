package com.limegroup.gnutella.gui.init;

import com.limegroup.gnutella.gui.wizard.WizardPage;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;

/**
 * This abstract class creates a <tt>JPanel</tt> that uses 
 * <tt>BoxLayout</tt> for setup windows.  It defines many of the 
 * basic accessor and mutator methods required by subclasses.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
abstract class SetupWindow extends WizardPage {


    /**
     * The width of the setup window.
     */
    static final int SETUP_WIDTH = 700;

    /**
     * The height of the setup window.
     */
    static final int SETUP_HEIGHT = 540;


	/**
	 * Creates a new setup window with the specified label.
	 *
	 * @param title the title of the window for use with <tt>CardLayout</tt>
	 *  and for use in obtaining the locale-specific caption for this
	 *  window
	 * @param description the key for locale-specific label to be displayed
	 *  in the window
	 */
	SetupWindow(final String title,
				final String description) {
        this(title, description, null);
    }


    /**
     * Creates a new setup window with the specified label.
     *
     * @param title the title of the window for use with <tt>CardLayout</tt>
     *  and for use in obtaining the locale-specific caption for this
     *  window
     * @param description the key for locale-specific label to be displayed
     *  in the window
     * @param moreInfoURL where more info about this option exists
     */
    SetupWindow(String title, String description, String moreInfoURL) {
        super(title, description);
        setURL(moreInfoURL, moreInfoURL);
    }


    protected void setOtherComponents() {
        JLabel jlIcon = new JLabel();
        jlIcon.setOpaque(false);
        jlIcon.setIcon(getIcon());
        jlIcon.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 8));
        getTopPanel().add(jlIcon, BorderLayout.EAST);

    }

    public Icon getIcon() {
		return null;
	}

	/**
	 * Called each time just before this window is opened.
	 */
	public void beforePageShown() {
	    createPage();
    }

    //JDOC-
    public boolean isPageComplete() {
        return true;
    }

	/**
	 *
	 * @param setupComponent the <tt>Component</tt> to add to this window
	 */
	final void setSetupComponent(JComponent setupComponent) {
        setupComponent.setBorder(new EmptyBorder(20, 10, 10, 10));
        add(setupComponent, BorderLayout.CENTER);
        revalidate();
	}

}
