package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;
import java.util.Hashtable;

import javax.swing.JLabel;
import javax.swing.JSlider;

import org.limewire.core.settings.SharingSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;
import com.limegroup.gnutella.gui.SizedWholeNumberField;
import com.limegroup.gnutella.gui.WholeNumberField;
import com.limegroup.gnutella.gui.GUIUtils.SizePolicy;

/**
 * This class defines the advanced panel in the options window that allows 
 * the user to change the limit who is able to connect with them.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class ConnectionPreferencingPaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Connection Preferencing");
    
    public final static String LABEL = I18n.tr("You can limit who is able to connect to you based on the number of files they share.");

	/**
	 * Constant for the key of the locale-specific <code>String</code> for the 
	 * label of the box only for specifying freeloader status.
	 */
	private final String BOX_LABEL_KEY = 
		I18nMarker.marktr("Files You Must Share to Not be a Freeloader:");

	/**
	 * Constant for the key of the locale-specific <code>String</code> for the 
	 * label for the freeloader slider
	 */
	private final String SLIDER_LABEL_KEY = 
		I18nMarker.marktr("Allow Freeloaders:");

	/**
	 * Constant for the key of the locale-specific <code>String</code> for the 
	 * label for the low end of the slider.
	 */
	private final String SLIDER_MIN_LABEL_KEY = 
		I18nMarker.marktr("Rarely");

	/**
	 * Constant for the key of the locale-specific <code>String</code> for the 
	 * label for the high end of the slider.
	 */
	private final String SLIDER_MAX_LABEL_KEY = 
		I18nMarker.marktr("Always");


	/**
	 * Handle to the <tt>WholeNumberField</tt> that displays the minimum
	 * number of files you must be sharing to not be considered a freeloader
	 */
	private final WholeNumberField FILES_FIELD = 
		new SizedWholeNumberField(SharingSettings.FREELOADER_FILES.getValue(), 3, SizePolicy.RESTRICT_BOTH);

	/**
	 * Handle to the <tt>JSlider</tt> instance that allows the user to
	 * specify the frequency of applying the rule.
	 */
	private JSlider _slider;


	/**
	 * The constructor constructs all of the elements of this 
	 * <tt>AbstractPaneItem</tt>.  This includes the row of buttons that
	 * allow the user to select the save directory.
	 *
	 * @param key the key for this <tt>AbstractPaneItem</tt> that the
	 *            superclass uses to generate strings
	 */
	public ConnectionPreferencingPaneItem() {
	    super(TITLE, LABEL);
	    
		final int MIN_VALUE = 10;
		final int MAX_VALUE = 100;
        int initialValue=Math.max(MIN_VALUE, SharingSettings.FREELOADER_ALLOWED.getValue());
		_slider = new JSlider(MIN_VALUE, MAX_VALUE, initialValue);

		Hashtable<Integer, JLabel> labels=new Hashtable<Integer, JLabel>();
		String labelMinStr = I18n.tr(SLIDER_MIN_LABEL_KEY);
		String labelMaxStr = I18n.tr(SLIDER_MAX_LABEL_KEY);
		JLabel minLabel = new JLabel(labelMinStr);
		JLabel maxLabel = new JLabel(labelMaxStr);
		labels.put(new Integer(MIN_VALUE), minLabel);
		labels.put(new Integer(MAX_VALUE), maxLabel);

		_slider.setLabelTable(labels);
		_slider.setPaintLabels(true);

		LabeledComponent comp0 = 
		    new LabeledComponent(BOX_LABEL_KEY, FILES_FIELD, LabeledComponent.LEFT_GLUE, LabeledComponent.LEFT);

		LabeledComponent comp1 = 
		    new LabeledComponent(SLIDER_LABEL_KEY, _slider, LabeledComponent.LEFT_GLUE, LabeledComponent.LEFT);
		
		add(comp0.getComponent());
		add(getVerticalSeparator());
		add(comp1.getComponent());
	}

	/**
	 * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
	 *
	 * Sets the options for the fields in this <tt>PaneItem</tt> when the 
	 * window is shown.
	 */
	@Override
    public void initOptions() {
        FILES_FIELD.setValue(SharingSettings.FREELOADER_FILES.getValue());
        _slider.setValue(SharingSettings.FREELOADER_ALLOWED.getValue());
	}

	/**
	 * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
	 *
	 * Applies the options currently set in this window, displaying an
	 * error message to the user if a setting could not be applied.
	 *
	 * @throws IOException if the options could not be applied for some reason
	 */
	@Override
    public boolean applyOptions() throws IOException {
        try {
            SharingSettings.FREELOADER_FILES.setValue(FILES_FIELD.getValue());
            SharingSettings.FREELOADER_ALLOWED.setValue(_slider.getValue());
        } catch (IllegalArgumentException e) {
        }
        return false;
	}
	
    public boolean isDirty() {
        return SharingSettings.FREELOADER_FILES.getValue() != FILES_FIELD.getValue() ||
               SharingSettings.FREELOADER_ALLOWED.getValue() != _slider.getValue();
    }
}
