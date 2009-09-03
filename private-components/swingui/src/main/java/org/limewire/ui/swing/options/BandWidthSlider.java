package org.limewire.ui.swing.options;

import java.text.NumberFormat;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.ui.swing.util.I18n;

/**
 * Basic component that provides the user with a slider for picking a percentage
 * of bandwidth to use. The slider lets the user select from 25-100% of their
 * bandwidth for use. It displays the expected bandwidth as a label after the
 * slider.
 */
public class BandWidthSlider extends LimeSlider {

    public static final int MIN_SLIDER = 25;
    public static final int MAX_SLIDER = 100;
    public static final int DEFAULT_SLIDER = MAX_SLIDER;

    public BandWidthSlider() {
        super(MIN_SLIDER, MAX_SLIDER);
    }

    @Override
    public String getMessage(int value) {
        String labelText = "";
        if (value == 100)
            labelText = I18n.tr("Unlimited");
        else {
            Float f = new Float(((value / 100.0)) * ConnectionSettings.CONNECTION_SPEED.getValue()
                    / 8);
            NumberFormat formatter = NumberFormat.getInstance();
            formatter.setMaximumFractionDigits(2);
            labelText = String.valueOf(formatter.format(f)) + " KB/s";
        }
        return labelText;
    }
}
