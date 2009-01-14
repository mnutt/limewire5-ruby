package org.limewire.ui.swing.wizard;

import java.awt.Frame;

import org.limewire.core.api.library.LibraryData;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.settings.InstallSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.IconManager;

import com.google.inject.Inject;

public class SetupWizard {
    
    private final IconManager iconManager;
    private Wizard wizard;
    
    @Inject
    public SetupWizard(SetupComponentDecoratorFactory decoratorFactory, 
            IconManager iconManager,
            LibraryManager libraryManager){
        this.iconManager = iconManager;
        
        if (shouldShowWizard()) {
            createWizard(decoratorFactory, libraryManager.getLibraryData());
        }
    }
    
    public boolean shouldShowWizard() {
        return needsPage1() || needsPage2() || needsUpgrade();
    }
    
    public void showDialogIfNeeded(Frame owner) {
        if (shouldShowWizard()) {
            wizard.showDialogIfNeeded(owner);
            
            // Sets the upgraded flag after the setup wizard
            //  completes
            InstallSettings.UPGRADED_TO_5.setValue(true);
        }
    }
        
    private void createWizard(SetupComponentDecoratorFactory decoratorFactory, 
            LibraryData libraryData) {
        
        SetupComponentDecorator decorator = decoratorFactory.create();
        
        wizard = new Wizard(decorator);
        
        if(needsPage1()){
            wizard.addPage(new SetupPage1(decorator));
        }        

        if (needsUpgrade()) {
            wizard.addPage(new SetupPage2(decorator, iconManager, libraryData, true));
        }
        else if(needsPage2()){
            wizard.addPage(new SetupPage2(decorator, iconManager, libraryData));
        }
    }
    
    private boolean needsUpgrade() {
        return !InstallSettings.UPGRADED_TO_5.getValue() 
            && InstallSettings.SCAN_FILES.getValue();
    }
    
    private boolean needsPage1() {
        if (!InstallSettings.FILTER_OPTION.getValue()) {
            return true;
        }
        if (!InstallSettings.START_STARTUP.getValue()) {
            return GuiUtils.shouldShowStartOnStartupWindow();
        }
        return false;
    }
    
    private boolean needsPage2() {
        return !InstallSettings.SCAN_FILES.getValue();
    }

}
