package com.limegroup.gnutella.gui.wizard;

import java.awt.CardLayout;
import java.awt.Component;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPanel;

import com.limegroup.gnutella.gui.init.ApplySettingsException;

/**
 * This class provides a generic wizard. It manages {@link WizardPage}
 * objects which are displayed in a dialog.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public abstract class Wizard implements WizardAction {

    /**
	 * the holder for the setup windows 
	 */
	private WizardPagePanel pageContainer;

	/**
	 * holder for the current setup window.
	 */
	private WizardPage currentPage;

    /**
     * manages the 4 basic wizard actions (previous|next|finish|cancel)
     */
    private final WizardButtonActionManager wizardButtonActionManager;

    /**
     * For each wizard page in the wizard, this flag determines
     * whether {@link WizardPage#createPage()} will be called immediately
     * upon being added to the wizard during {@link #launchWizard()}
     */
    private boolean createPageUponAdding;


    public Wizard(boolean createPageUponAdding) {
        this.createPageUponAdding = createPageUponAdding;
        this.wizardButtonActionManager = new WizardButtonActionManager(this);
        this.pageContainer = new WizardPagePanel();
    }


    /**
     * Entry point to the wizard pages.
     *
     * Constructs wizard and shows the first page if there are
     * any, as determined by {@link #createWizardPages()}. If there are
     * no pages, this method returns without displaying anything.
     * 
     */
    public void launchWizard() {

        // create the pages relevant to this wizard
        List<WizardPage> wizardPages = createWizardPages();

        // if none, end wizard
        if (wizardPages.size() == 0) {
            return;
        }

        // add pages to wizard
        for (WizardPage wizardPage : wizardPages) {
            addPage(wizardPage);
        }

        // set page container to the 1st page
        show(wizardPages.get(0));

        // create and show the dialog
		showDialog(pageContainer);
	}


    /**
     * Create and display main dialog, buttons, etc that
     * comprise the wizard. Body of main wizard page is passed in.
     *
     * @param wizardPageContainer Body of main wizard page
     */
    protected abstract void showDialog(JPanel wizardPageContainer);


    /**
     * Create and return the list of wizard pages used in this wizard.
     *
     * Called by {@link #launchWizard()}.
     *
     * @return
     */
    protected abstract List<WizardPage> createWizardPages();


    private void addPage(WizardPage page) {
    	page.setWizard(this);
        if (createPageUponAdding) {
            page.createPage();
        }
        pageContainer.add(page);
    }

    /****************************************************************************/
    /* Navigation methods to go from page to page, next page, previous page     */
    /****************************************************************************/
    protected final WizardPage getNextPage() {
        return pageContainer.getNext(currentPage);
    }

    protected final WizardPage getCurrentPage() {
        return currentPage;
    }

    protected final WizardPage getPreviousPage() {
        return pageContainer.getPrevious(currentPage);
    }

    final WizardPage getNextPage(WizardPage page) {
		return pageContainer.getNext(page);
	}

	final WizardPage getPreviousPage(WizardPage page) {
		return pageContainer.getPrevious(page);
	}

    /**
     * Applies settings on the wizard's current page.
     * {@link WizardPage#applySettings(boolean)}
     *
     */
    protected final void applySettings(boolean loadCoreComponents) throws ApplySettingsException {
        currentPage.applySettings(loadCoreComponents);
    }


    protected final Action[] getButtonActions() {
        return wizardButtonActionManager.getActions();
    }


    /**
	 * Show the specified page.
	 */
	protected final void show(WizardPage page) {
        page.beforePageShown();
        pageContainer.show(page.getKey());
		currentPage = page;
		page.afterPageShown();
	}
	
	/**
	 * Updates the buttons according to the status of the currently visible
	 * page.
	 */
    void updateButtons() {
		if (currentPage == null) {
            wizardButtonActionManager.enableActions(WizardButtonActionManager.ACTION_CANCEL);
		} else {
			boolean complete = currentPage.isPageComplete();
			boolean canFlipToNext = currentPage.canFlipToNextPage();

            boolean finishEnabled = (complete && !canFlipToNext);
			boolean nextEnabled = (complete && canFlipToNext);
			boolean previousEnabled = (currentPage.getPrevious() != null);

            int actions = WizardButtonActionManager.ACTION_CANCEL;

            if (finishEnabled) {
                actions |= WizardButtonActionManager.ACTION_FINISH;
            }
            if (nextEnabled) {
                actions |= WizardButtonActionManager.ACTION_NEXT;
            }
            if (previousEnabled) {
                actions |= WizardButtonActionManager.ACTION_PREVIOUS;
            }
            wizardButtonActionManager.enableActions(actions);

		}
	}


    /**
	 * Updates the language for the wizard buttons and re-displays the page
     */
    protected final void updateLanguage() {
        wizardButtonActionManager.updateLanguage();
        try {
            applySettings(false);
        } catch (ApplySettingsException ignored) {
            // NOTE: Swallowing exception!
        }
        show(currentPage);
    }




    /**
	 * This class serves two purposes.  First, it is a JPanel that
	 * contains the body of a LimeWire setup window.  Second, it 
	 * serves as a proxy for the underlying WizardPage object that
	 * that handles the actual drawing.
	 */
	private class WizardPagePanel extends JPanel {

		/**
		 * The <tt>CardLayout</tt> instance for the setup windows.
		 */
		private final CardLayout CARD_LAYOUT = new CardLayout();

		/**
		 * Sets the <tt>CardLayout</tt> for the setup windows.
		 */
		WizardPagePanel() {
			setLayout(CARD_LAYOUT);	   
		}

		/**
		 * Adds the speficied window to the CardLayout based on its title.
		 *
		 * @param page the <tt>WizardPage</tt> to add
		 */
		void add(WizardPage page) {
			add(page, page.getKey());
		}

		public WizardPage getFirst() {
			if (getComponentCount() > 0) {
				return (WizardPage) getComponent(0);
			} else {
				return null;
			}
		}

		public WizardPage getLast() {
			if (getComponentCount() > 0) {
				return (WizardPage) getComponent(getComponentCount() - 1);
			} else {
				return null;
			}
		}
		
		public WizardPage getNext(WizardPage page) {
			Component[] pages = getComponents();
			for (int i = 0; i < pages.length; i++) {
				if (pages[i] == page && i < pages.length - 1) {
					return (WizardPage) pages[i + 1];
				} 
			}
			return null;
		}

		public WizardPage getPrevious(WizardPage page) {
			Component[] pages = getComponents();
			for (int i = 0; i < pages.length; i++) {
				if (pages[i] == page && i > 0) {
					return (WizardPage) pages[i - 1];
				} 
			}
			return null;
		}

		/**
		 * Shows the window speficied by its title.
		 * 
		 * @param key the unique key of the <tt>Component</tt> to show
		 */
		void show(String key) {
			CARD_LAYOUT.show(this, key);
		}
		
	}

}
