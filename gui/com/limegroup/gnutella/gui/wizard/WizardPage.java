package com.limegroup.gnutella.gui.wizard;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.URLLabel;
import com.limegroup.gnutella.gui.init.ApplySettingsException;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/**
 * This abstract class creates a <tt>JPanel</tt> that uses <tt>BoxLayout</tt>
 * for setup windows. It defines many of the basic accessor and mutator methods
 * required by subclasses.
 */
// 2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public abstract class WizardPage extends JPanel {

	/**
	 * Variable for the name of this window for use with <tt>CardLayout</tt>.
	 */
	private String key;

	/**
	 * Variable for the key of the title to display.
	 */
	private String titleKey;

	/**
	 * Variable for the key of the label to display.
	 */
	private String descriptionKey;

	/** Variable for the URL where more info exists. Null if none. */
	private String url;

	/** Variable for the URL where more info exists. Null if none. */
	private String urlLabelKey;

	/**
	 * The dialog that displays the page.
	 */
	private Wizard wizard;

    /** UI Components Here */
    // --------------------------------------------------
    private JPanel topPanel; 


    /**
	 * Creates a new wizard page with the specified label.
	 * 
	 * @param key a unique identifier for this page
	 * @param titleKey
	 *            the title of the window for use with <tt>CardLayout</tt> and
	 *            for use in obtaining the locale-specific caption for this
	 *            window
	 * @param descriptionKey
	 *            the key for locale-specific label to be displayed in the
	 *            window
	 */
	protected WizardPage(String key, String titleKey, String descriptionKey) {
		this.key = key;
		this.titleKey = titleKey;
		this.descriptionKey = descriptionKey;
	}
	
	protected WizardPage(String titleKey, String descriptionKey) {
		this(titleKey, titleKey, descriptionKey);
	}

	/**
	 * Creates the wizard page controls. 
	 *
	 * @see #createPageContent()
	 */
	protected void createPage() {
        initLayout();

        topPanel = setTopPanel();

        JPanel titlePanel = setTitlePanel();

        setTitleLabel(titlePanel);
        setMultiLineLabel(titlePanel);

		if (url != null) {
            setMoreInfoUrlLabel(titlePanel);
		}

        setOtherComponents();
		
		createPageContent();
    }

    /**
     * Adds any other additional components (such as a status label)
     * to the wizard page.
     *
     * By default do nothing
     */
    protected void setOtherComponents() { }


    private void setMoreInfoUrlLabel(JPanel titlePanel) {
        String label = (urlLabelKey != null) ? I18n.tr(urlLabelKey) : url;
        JLabel urlLabel = new URLLabel(url, label);
        urlLabel.setOpaque(false);
        urlLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        urlLabel.setForeground(Color.black);
        urlLabel.setOpaque(false);
        titlePanel.add(urlLabel, BorderLayout.SOUTH);
    }

    private void setMultiLineLabel(JPanel titlePanel) {
        WizardMultiLineLabel descriptionLabel = new WizardMultiLineLabel(I18n
                .tr(descriptionKey));
        descriptionLabel.setOpaque(false);
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        descriptionLabel.setForeground(Color.black);
        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(Font.PLAIN));
        titlePanel.add(descriptionLabel, BorderLayout.CENTER);
    }

    private void setTitleLabel(JPanel titlePanel) {
        JLabel titleLabel = new JLabel(I18n.tr(titleKey));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 16));
        titleLabel.setForeground(Color.black);
        titleLabel.setOpaque(false);
        titlePanel.add(titleLabel, BorderLayout.NORTH);
    }

    private JPanel setTitlePanel() {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        this.topPanel.add(titlePanel, BorderLayout.CENTER);
        return titlePanel;
    }

    private JPanel setTopPanel() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.setBackground(Color.white);
        topPanel.setBorder(BorderFactory.createEtchedBorder());
        add(topPanel, BorderLayout.NORTH);
        return topPanel;
    }

    private void initLayout() {
        removeAll();
        setLayout(new BorderLayout());
    }


    public boolean canFlipToNextPage() {
		return isPageComplete() && getNext() != null;
	}
	
	/**
	 * Creates the main controls of the wizard page.
     * Also creates the panels the controls need to be added to.
	 * 
	 */
	protected abstract void createPageContent();


	/**
	 * Accessor for the unique identifying key of the window in the
	 * <tt>CardLayout</tt>.
	 * 
	 * @return the unique identifying key for the window.
	 */
	public String getKey() {
		return key;
	}


	/**
	 * Accessor for the next page in the sequence.
	 * 
	 * @return the next window in the sequence
	 */
	public WizardPage getNext() {
		return (wizard != null) ?  wizard.getNextPage(this) : null;
	}

	/**
	 * Accessor for the previous page in the sequence.
	 * 
	 * @return the previous window in the sequence
	 */
	public WizardPage getPrevious() {
		return (wizard != null) ?  wizard.getPreviousPage(this) : null;
	}

    /**
     * Is input valid to leave this page (next page or finish wizard)
     *
     * @return true, if input is valid and the user may proceed to the following
	 * wizard page or close the wizard if this is the last page. Else return false.
     */
    public abstract boolean isPageComplete();

	/**
	 * Invoked immediately after the page becomes the active page. Validates the input and
	 * updates the buttons of the wizard.
	 */
	public void afterPageShown() {
		validateInput();
        updateButtons();
    }


	/**
	 * Applies the settings currently set in this window.
	 *
	 * If loadCoreComponents is false, core components should not be loaded.
	 * This is useful when you just want to temporarily save the current settings,
	 * but do not plan on finishing this step immediately.
	 * <p>
	 * @param loadCoreComponents true if settings should be applied
	 * AND core components should be loaded.  false if only settings
	 * should be applied (but components shouldn't be loaded).
	 *
	 * @throws com.limegroup.gnutella.gui.init.ApplySettingsException if there was a problem applying the
	 *         settings
	 */
    public void applySettings(boolean loadCoreComponents) throws ApplySettingsException {}


    /**
	 * Subclasses should reimplement this method to validate input.
	 *
	 * @see #isPageComplete()
	 */
	public void validateInput() {}


    void setWizard(Wizard wizard) {
		this.wizard = wizard;
	}
	
	/**
	 * Sets a url that is displayed in the title area of the wizard page.
	 * 
	 * @param url
	 *            the url
	 * @param urlLabelKey
	 *            the resource key of the label used to display url; if null,
	 *            url will be used as label
	 */
	public void setURL(String url, String urlLabelKey) {
		this.url = url;
		this.urlLabelKey = urlLabelKey;
	}


    protected JPanel getTopPanel() {
        return topPanel;
    }

    /**
     * Method called prior to wizard page being displayed
     */
    protected void beforePageShown() {}

    /**
     * Update the buttons (enabled, disabled for prev/next/finish/cancel buttons)
     */
    public void updateButtons() {
        wizard.updateButtons();
    }

    /**
	 * Label that wraps text. Used to display the description.
	 */
	protected static class WizardMultiLineLabel extends JTextArea {

		/**
		 * Creates a label that can have multiple lines and that has the default
		 * width.
		 * 
		 * @param s
		 *            the <tt>String</tt> to display in the label
		 */
		public WizardMultiLineLabel(String s) {
			setEditable(false);
			setLineWrap(true);
			setWrapStyleWord(true);
			setHighlighter(null);
			LookAndFeel.installBorder(this, "Label.border");
			LookAndFeel.installColorsAndFont(this, "Label.background",
					"Label.foreground", "Label.font");
			setSelectedTextColor(UIManager.getColor("Label.foreground"));
			setText(s);
		}

		public WizardMultiLineLabel() {
			this(" ");
		}

	}
	
}
