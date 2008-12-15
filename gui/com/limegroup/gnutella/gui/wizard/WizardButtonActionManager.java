package com.limegroup.gnutella.gui.wizard;

import com.limegroup.gnutella.gui.I18n;

import java.awt.event.ActionEvent;

import org.limewire.i18n.I18nMarker;

import javax.swing.Action;
import javax.swing.AbstractAction;

/**
 * This object manages the 4 basic actions
 * (Back, Next, Finish, Cancel) of a standard wizard.
 *
 */
public class WizardButtonActionManager {

    private final WizardAction wizardAction;

    // replace with enumSet
    public static final int ACTION_PREVIOUS = 1;
	public static final int ACTION_NEXT = 2;
	public static final int ACTION_FINISH = 4;
	public static final int ACTION_CANCEL = 8;


    private final PreviousAction previousAction;
	private final NextAction nextAction;
	private final FinishAction finishAction;
	private final CancelAction cancelAction;

    private final LanguageAwareAction[] actions;

    public Action[] getActions() {
        return actions;
    }


    public WizardButtonActionManager(WizardAction wizardAction) {
        this.wizardAction = wizardAction;
        previousAction = new PreviousAction();
	    nextAction = new NextAction();
	    finishAction = new FinishAction();
	    cancelAction = new CancelAction();
        actions = new LanguageAwareAction[] { previousAction, nextAction, finishAction, cancelAction };
    }

	/**
	 * Enables the bitmask of specified actions, the other actions are explicitly
	 * disabled.
	 * <p>
	 * To enable finish and previous you would call
	 * {@link #enableActions(int)
     * enableActions(WizardButtonActionManager.ACTION_FINISH|WizardButtonActionManager.ACTION_PREVIOUS)}.
	 * @param actions integer with least significant 4 bits describing which actions are enabled.
	 */
	public void enableActions(int actions) {
		previousAction.setEnabled((actions & ACTION_PREVIOUS) != 0);
		nextAction.setEnabled((actions & ACTION_NEXT) != 0);
		finishAction.setEnabled((actions & ACTION_FINISH) != 0);
		cancelAction.setEnabled((actions & ACTION_CANCEL) != 0);
	}


    public void updateLanguage() {
        for (int i = 0; i < actions.length; i++) {
			actions[i].updateLanguage();
		}
    }


    private abstract class LanguageAwareAction extends AbstractAction {

		private final String nameKey;

		public LanguageAwareAction(String nameKey) {
			super(I18n.tr(nameKey));
			this.nameKey = nameKey;
		}

		public void updateLanguage() {
			putValue(Action.NAME, I18n.tr(nameKey));
		}
	}

    private class CancelAction extends LanguageAwareAction {

		public CancelAction() {
            super(I18nMarker.marktr("Cancel"));
		}

		public void actionPerformed(ActionEvent e) {
			wizardAction.performCancel();
		}
	}

	private class NextAction extends LanguageAwareAction {

		public NextAction() {
            super(I18nMarker.marktr("Next >>"));
		}

		public void actionPerformed(ActionEvent e) {
			wizardAction.performNext();
		}
	}

	private class PreviousAction extends LanguageAwareAction {

		public PreviousAction() {
			super(I18nMarker.marktr("<< Back"));
		}

		public void actionPerformed(ActionEvent e) {
			wizardAction.performPrevious();
		}
	}

	private class FinishAction extends LanguageAwareAction {

		public FinishAction() {
            super(I18nMarker.marktr("Finish"));
		}

		public void actionPerformed(ActionEvent e) {
			wizardAction.performFinish();
		}

	}
}
