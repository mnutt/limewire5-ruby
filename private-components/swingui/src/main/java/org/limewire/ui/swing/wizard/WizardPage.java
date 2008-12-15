package org.limewire.ui.swing.wizard;

import javax.swing.JPanel;

public abstract class WizardPage extends JPanel {
    public abstract void applySettings();
    public abstract String getLine1();
    public abstract String getLine2();
    public abstract String getFooter();
}
