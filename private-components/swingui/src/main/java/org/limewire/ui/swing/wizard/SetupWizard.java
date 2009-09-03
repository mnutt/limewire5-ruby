package org.limewire.ui.swing.wizard;

import java.awt.Frame;

import org.limewire.core.api.Application;
import org.limewire.core.api.library.LibraryData;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.settings.InstallSettings;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class SetupWizard {

    private Wizard wizard;


    @Inject
    public SetupWizard(Provider<SetupComponentDecoratorFactory> decoratorFactory,
            Provider<LibraryManager> libraryManager) {

        createWizard(decoratorFactory.get(), libraryManager.get().getLibraryData());
    }

    public static boolean shouldShowWizard(Application application) {
        if (shouldShowPage1()) {
            return true;
        }

        String lastRunVersion = InstallSettings.LAST_VERSION_RUN.get();
        if (lastRunVersion != null && !lastRunVersion.equals(application.getVersion())) {
            return true;
        }

        return false;
    }

    public void showDialog(Frame owner, Application application) {
            
        wizard.showDialog(owner);

        // Sets the upgraded flag after the setup wizard completes
        InstallSettings.UPGRADED_TO_5.setValue(true);
        InstallSettings.LAST_VERSION_RUN.set(application.getVersion());
        InstallSettings.PREVIOUS_RAN_VERSIONS.add(application.getVersion());
    }

    private void createWizard(SetupComponentDecoratorFactory decoratorFactory,
            LibraryData libraryData) {

        SetupComponentDecorator decorator = decoratorFactory.create();

        wizard = new Wizard(decorator);

        if (shouldShowPage1()) {
            wizard.addPage(new SetupPage1(decorator));
        }
        
        wizard.addPage(new SetupPage2(decorator));
    }

    private static boolean shouldShowPage1() {
        if (!InstallSettings.AUTO_SHARING_OPTION.getValue()) {
            return true;
        }
        if (!InstallSettings.ANONYMOUS_DATA_COLLECTION.getValue()) {
            return true;
        }
        if (!InstallSettings.FILTER_OPTION.getValue()) {
            return true;
        }
        if (!InstallSettings.START_STARTUP.getValue()) {
            return GuiUtils.shouldShowStartOnStartupWindow();
        }

        return false;
    }

}
