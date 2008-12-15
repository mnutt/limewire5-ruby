package com.limegroup.gnutella.gui.options.panes;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.IOException;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.limewire.core.settings.SharingSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;

public class ShareSpeciallyPaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Download Sharing");
    
    public final static String LABEL = I18n.tr("You can share downloads that are saved to unshared folders.");

	/**
	 * Constant for the key of the locale-specific <tt>String</tt> for the 
	 * check box that allows the user to connect automatically or not
	 */
	private final String CHECK_BOX_LABEL = 
		I18nMarker.marktr("Share Downloads Saved To Unshared Folders:");
	
	/**
	 * Explains what downloads are currently being shared.
	 */
	private final JLabel explanationLabel = new JLabel();

	/**
	 * Constant for the check box that determines whether or not 
	 * to send OOB searches.
	 */
	private final JCheckBox CHECK_BOX = new JCheckBox();

	public ShareSpeciallyPaneItem() {
	    super(TITLE, LABEL);
	    
		LabeledComponent comp = new LabeledComponent(CHECK_BOX_LABEL,
				CHECK_BOX, LabeledComponent.LEFT_GLUE, LabeledComponent.LEFT);
		add(comp.getComponent());

		explanationLabel.setFont(explanationLabel.getFont().deriveFont(Math.max(explanationLabel.getFont().getSize() - 2.0f, 9.0f)).deriveFont(Font.PLAIN));
		CHECK_BOX.addChangeListener(new ChangeListener() {
		    public void stateChanged(ChangeEvent e) {
		        setExplanationText();
		    }
		});
		setExplanationText();
		JPanel container = new JPanel(new BorderLayout());
		container.add(explanationLabel, BorderLayout.EAST);
		add(container);
	}
	
	private void setExplanationText() {
        if (CHECK_BOX.isSelected()) {
            explanationLabel.setText(I18n.tr("All downloads will be shared."));
        } else {
            explanationLabel.setText(I18n.tr("Only downloads saved in shared folders will be shared."));
        }
	}

	@Override
    public void initOptions() {
        CHECK_BOX.setSelected
        	(SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue());
	}

	@Override
    public boolean applyOptions() throws IOException {
		SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.
			setValue(CHECK_BOX.isSelected());
        return false;
	}

    public boolean isDirty() {
        return SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue() 
        	!= CHECK_BOX.isSelected();
    }
	
}
