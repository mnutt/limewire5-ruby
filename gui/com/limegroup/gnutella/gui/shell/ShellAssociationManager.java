package com.limegroup.gnutella.gui.shell;

import java.util.Collection;

import org.limewire.core.settings.QuestionsHandler;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.DialogOption;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.actions.ConfigureOptionsAction;

/**
 * Stores all the LimeAssociationOptions that LimeWire is set to use.
 */ 
public class ShellAssociationManager {
	
	private final Collection<LimeAssociationOption> associations;
	
	public ShellAssociationManager(Collection<LimeAssociationOption> associations) {
		this.associations = associations;
	}

	/**
     * Runs through all the associations that this manager is handling and
     * checks to see if they can be enabled.  If 'prompt' is true, this will
     * prompt the user (respecting any 'do not ask again' settings) prior
     * to moving an association to LimeWire from another program.  If 'prompt'
     * is false, this will only change associations that are currently unset.
     * 
	 * @param prompt whether to prompt the user 
	 * @return true if all allowed and supported associations are registered
	 * to us. (only meaningful if prompt is false).
	 */
	public boolean checkAndGrab(boolean prompt) {
		boolean ret = true;
		for (LimeAssociationOption association : associations) {
			if (association.isAllowed()) {
				if (association.isAvailable()) // grab all available associations
					association.setEnabled(true);
				else if (!association.isEnabled())
                    ret = false;
			}
		}
        
        if(!ret && prompt) {
            DialogOption answer = GUIMediator.showYesNoOtherMessage(
                    I18n.tr("One or more files or protocols that LimeWire uses are no longer associated with LimeWire. Would you like LimeWire to re-associate them?"),
                    QuestionsHandler.GRAB_ASSOCIATIONS,
                    I18n.tr("Details"));
                        
            if(answer == DialogOption.YES) {
                for (LimeAssociationOption association : associations) {
                    if (association.isAllowed() && !association.isEnabled())
                        association.setEnabled(true);
                }
            } else if(answer == DialogOption.OTHER) {
                new ConfigureOptionsAction(I18nMarker.marktr("File Associations")).actionPerformed(null);
            }
        }
		
		return ret;
	}
}
