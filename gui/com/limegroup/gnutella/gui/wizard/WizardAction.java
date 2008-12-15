package com.limegroup.gnutella.gui.wizard;

/**
 * This interface represents the 4 standard actions of the wizard.
 * [Previous | Next | Finish | Cancel]
 */
public interface WizardAction {

    public void performCancel();

    public void performNext();

    public void performPrevious();

    public void performFinish();
}
