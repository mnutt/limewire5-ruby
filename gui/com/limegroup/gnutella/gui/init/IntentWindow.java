package com.limegroup.gnutella.gui.init;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JPanel;

import org.limewire.i18n.I18nMarker;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.util.LimeWireUtils;


/** State Your Intent. */
final class IntentWindow extends SetupWindow {

    // has the intent radio button been pressed yet?
    private boolean setWillNot = false;
    private Properties properties;

	IntentWindow() {
		super(I18nMarker.marktr("State Your Intent"), I18nMarker
                .marktr("One more thing..."));
    }
	
	private boolean isCurrentVersionChecked() {
	    if(properties == null) {
	        properties = new Properties();
	        try {
	            properties.load(new FileInputStream(getPropertiesFile()));
	        } catch(IOException iox) {
	        }
	    }
	    
	    String exists = properties.getProperty(LimeWireUtils.getLimeWireVersion());
	    return exists != null && exists.equals("true");
	}
	
	boolean isConfirmedWillNot() {
	    return isCurrentVersionChecked() || setWillNot;
	}
    
    protected void createPageContent() {

        JPanel innerPanel = new JPanel(new BorderLayout());
        final IntentPanel intentPanel = new IntentPanel();
        innerPanel.add(intentPanel, BorderLayout.CENTER);        
        setSetupComponent(innerPanel);

        intentPanel.addButtonListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(intentPanel.hasSelection()) {
                    setWillNot = intentPanel.isWillNot();
                    updateButtons();
                }
            }
        });
	}

    @Override
    public boolean canFlipToNextPage() {
        return false;           // this is the last page!
    }
    
    @Override
    public boolean isPageComplete() {
        return setWillNot;      // not complete until user clicks button
    }

    @Override
    public void applySettings(boolean loadCoreComponents) {
	    if(setWillNot) {
	        properties.put(LimeWireUtils.getLimeWireVersion(), "true");
	        try {
	            properties.store(new FileOutputStream(getPropertiesFile()), "Started & Ran Versions");
	        } catch(IOException ignored) {}
	    }	    
	}
	
	private File getPropertiesFile() {
	    return new File(CommonUtils.getUserSettingsDir(), "versions.props");
	}
}
