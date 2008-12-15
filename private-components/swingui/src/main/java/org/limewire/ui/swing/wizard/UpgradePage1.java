package org.limewire.ui.swing.wizard;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.library.LibraryData;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.I18n;

public class UpgradePage1 extends WizardPage {

    private final String line1 = I18n.tr("LimeWire is ready to fill your Library");
    private final String line2 = I18n.tr("Your Library is a central location to view, share and unshare your files " +
    		"with the P2P Network and, or your friends.");
    private final String footer = I18n.tr("You can change this option later from Tools > Options");
    
    private final String noteText = I18n.tr("LimeWire will import old shared files into your Library " +
    		"and continue sharing them");
    private final String autoTitle = I18n.tr("Automatically manage my Library");
    private final String autoCheckText = I18n.tr("Allow LimeWire to automatically scan new files into your" +
    		" Library from My Documents and the Desktop.  This will not automatically share your files.");
    
    private final LibraryData libraryData;
    
    private final JCheckBox autoCheck;
    
    public UpgradePage1(SetupComponentDecorator decorator, LibraryData libraryData) {
        this.libraryData = libraryData;

        setOpaque(false);
        setLayout(new MigLayout("insets 0, gap 0, nogrid"));
    
        autoCheck = new JCheckBox();
        decorator.decorateLargeCheckBox(autoCheck);
        
        JLabel label;
        
        label = new JLabel(noteText);
        decorator.decorateNormalText(label);
        add(label, "gaptop 25, gapleft 40, wrap");
        
        label = new JLabel(autoTitle);
        decorator.decorateHeadingText(label);
        add(label, "gaptop 30, gapleft 14, wrap");
        
        add(autoCheck, "gaptop 10, gapleft 40");
        label = new MultiLineLabel(autoCheckText, 500);
        decorator.decorateNormalText(label);       
        add(label, "gaptop 10, gapleft 10, wrap");
        
    }
    
    @Override
    public void applySettings() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getLine1() {
        return line1;
    }

    @Override
    public String getLine2() {
        return line2;
    }

    @Override
    public String getFooter() {
        return footer;
    }

}
