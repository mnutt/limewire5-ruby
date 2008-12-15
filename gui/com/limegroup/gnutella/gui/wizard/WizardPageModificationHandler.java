/**
 * 
 */
package com.limegroup.gnutella.gui.wizard;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Listens to document modification and action events and invokes
 * {@link WizardPage#validateInput()} on each change.
 */
public class WizardPageModificationHandler implements DocumentListener, ActionListener {
	
	private WizardPage page;

	/**
	 * @param page the page that is notified when the document is changed
	 */
	public WizardPageModificationHandler(WizardPage page) {
		this.page = page;
	}

	public void changedUpdate(DocumentEvent e) {
		page.validateInput();
	}

	public void insertUpdate(DocumentEvent e) {
		page.validateInput();
	}

	public void removeUpdate(DocumentEvent e) {
		page.validateInput();
	}

	
	public void actionPerformed(ActionEvent e) {
		page.validateInput();
	}
	
}