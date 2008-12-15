package com.limegroup.bittorrent.gui.options.panes;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JCheckBox;

import org.limewire.collection.MultiIterable;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.core.settings.QuestionsHandler;
import org.limewire.i18n.I18nMarker;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.Setting;

import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;
import com.limegroup.gnutella.gui.Line;
import com.limegroup.gnutella.gui.SizedWholeNumberField;
import com.limegroup.gnutella.gui.WholeNumberField;
import com.limegroup.gnutella.gui.options.panes.AbstractPaneItem;

/**
 * Defines options for BitTorrent settings.
 */
public class BittorrentPaneItem extends AbstractPaneItem {
	
    public final static String TITLE = I18n.tr("BitTorrent Settings");
    
    public final static String LABEL = I18n.tr("You can choose whether or not LimeWire should manage your BitTorrent protocol settings. It is highly recommended that LimeWire manage these settings. Invalid or inappropriate values may cause severe performance and/or memory problems.");
       
	private final JCheckBox AUTO_CHECK_BOX = new JCheckBox();	
	private WholeNumberField maxUploadsField = new SizedWholeNumberField(4);
	private WholeNumberField minUploadsField = new SizedWholeNumberField(4);
	private final JCheckBox FLUSH_CHECK_BOX = new JCheckBox();
	private final JCheckBox MMAP_CHECK_BOX = new JCheckBox();
	
	// would be nice to have a Setting<T> and Control<T>
	private Map<IntSetting, WholeNumberField> intSettingsMap = 
		new HashMap<IntSetting, WholeNumberField>();
	private Map<BooleanSetting, JCheckBox> boolSettingsMap = 
		new HashMap<BooleanSetting, JCheckBox>();
	
	private Iterable<Setting> settings = 
		new MultiIterable<Setting>(intSettingsMap.keySet(),boolSettingsMap.keySet());
	
	public BittorrentPaneItem() {
	    super(TITLE, LABEL);

		AUTO_CHECK_BOX.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean allow = !AUTO_CHECK_BOX.isSelected();
				if (allow)
					GUIMediator.showWarning(
							I18n.tr("Invalid or inappropriate custom values may cause severe performance and/or memory problems. Unless you really know what you\'re doing, it is highly recommended that you let LimeWire manage these settings."),
							QuestionsHandler.BITTORRENT_CUSTOM_SETTINGS);

				// it'd be nice to revert the settings and update the controls
				// here so that the user sees them going back to default values

				updateControls(allow);
			}

		});

		BoxPanel panel = new BoxPanel();
		LabeledComponent comp = new LabeledComponent(
				I18nMarker
                        .marktr("Let LimeWire manage my BitTorrent settings. (Recommended)"), AUTO_CHECK_BOX,
				LabeledComponent.LEFT_GLUE, LabeledComponent.LEFT);
		panel.add(comp.getComponent());
		
		panel.addLineGap();
		panel.add(createSeparator(comp.getComponent()));
		panel.addLineGap();

		comp = new LabeledComponent(I18nMarker.marktr("Maximum uploads per torrent"),
				maxUploadsField, LabeledComponent.LEFT_GLUE,
				LabeledComponent.LEFT);
		panel.add(comp.getComponent());
		panel.addVerticalComponentGap();

		comp = new LabeledComponent(I18nMarker.marktr("Minimum uploads per torrent"),
				minUploadsField, LabeledComponent.LEFT_GLUE,
				LabeledComponent.LEFT);
		panel.add(comp.getComponent());
		panel.addVerticalComponentGap();
		
		comp = new LabeledComponent(I18nMarker.marktr("Safe chunk verification"),
				FLUSH_CHECK_BOX, LabeledComponent.LEFT_GLUE,
				LabeledComponent.LEFT);
		panel.add(comp.getComponent());
		panel.addVerticalComponentGap();

		comp = new LabeledComponent(I18nMarker.marktr("Experimental disk access"),
				MMAP_CHECK_BOX, LabeledComponent.LEFT_GLUE,
				LabeledComponent.LEFT);
		panel.add(comp.getComponent());

		intSettingsMap.put(BittorrentSettings.TORRENT_MAX_UPLOADS,
				maxUploadsField);
		intSettingsMap.put(BittorrentSettings.TORRENT_MIN_UPLOADS,
				minUploadsField);

		boolSettingsMap.put(BittorrentSettings.TORRENT_FLUSH_VERIRY,
				FLUSH_CHECK_BOX);
		boolSettingsMap.put(BittorrentSettings.TORRENT_USE_MMAP,
				MMAP_CHECK_BOX);

		add(panel);
	}

	private Component createSeparator(Component component) {
		Line line = new Line();
		
		// tweak line to only expand to the width of component 
		line.setMinimumSize(new Dimension(component.getPreferredSize().width, 1));
		line.setPreferredSize(new Dimension(component.getPreferredSize().width, 1));
		line.setMaximumSize(new Dimension(component.getPreferredSize().width, 1));
		
		BoxPanel panel = new BoxPanel(BoxPanel.X_AXIS);
		panel.add(Box.createHorizontalGlue());
		panel.add(line);
		return panel;
	}

	private void updateControls(boolean userManagedSettings) {
		maxUploadsField.setEnabled(userManagedSettings);
		minUploadsField.setEnabled(userManagedSettings);
		FLUSH_CHECK_BOX.setEnabled(userManagedSettings);
		MMAP_CHECK_BOX.setEnabled(userManagedSettings);
	}

	@Override
	public boolean applyOptions() throws IOException {
		BittorrentSettings.AUTOMATIC_SETTINGS.setValue(AUTO_CHECK_BOX.isSelected());
		if (AUTO_CHECK_BOX.isSelected()) {
			for (Setting toRevert : settings)
				toRevert.revertToDefault();
		} else {
			for (IntSetting toSet : intSettingsMap.keySet())
				toSet.setValue(intSettingsMap.get(toSet).getValue());
			for (BooleanSetting toSet : boolSettingsMap.keySet())
				toSet.setValue(boolSettingsMap.get(toSet).isSelected());
		}
		return false;
	}

	@Override
	public void initOptions() {
        boolean auto = BittorrentSettings.AUTOMATIC_SETTINGS.getValue();
		AUTO_CHECK_BOX.setSelected(auto);
		for (Map.Entry<IntSetting,WholeNumberField> e : intSettingsMap.entrySet()) 
			e.getValue().setValue(e.getKey().getValue());
		for (Map.Entry<BooleanSetting,JCheckBox> e : boolSettingsMap.entrySet()) 
			e.getValue().setSelected(e.getKey().getValue());
		updateControls(!auto);
	}

	public boolean isDirty() {
		if (AUTO_CHECK_BOX.isSelected() != BittorrentSettings.AUTOMATIC_SETTINGS.getValue())
			return false;
		for (IntSetting setting : intSettingsMap.keySet()) {
			if (setting.getValue() != intSettingsMap.get(setting).getValue())
				return true;
		}
		for (BooleanSetting setting : boolSettingsMap.keySet()) {
			if (setting.getValue() != boolSettingsMap.get(setting).isSelected())
				return true;
		}
		return false;
	}

}
