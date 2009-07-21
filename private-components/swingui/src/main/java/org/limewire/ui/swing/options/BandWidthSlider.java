package org.limewire.ui.swing.options;

import java.awt.FlowLayout;
import java.text.NumberFormat;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.ui.swing.util.I18n;

/**
 * Basic component that provides the user with a slider for picking a percentage
 * of bandwidth to use. The slider lets the user select from 25-100% of their
 * bandwidth for use. It displays the expected bandwidth as a label after the slider.
 */
public class BandWidthSlider extends JComponent {
    public static final int MIN_SLIDER = 25;

    public static final int MAX_SLIDER = 100;

    private JSlider bandWidthSlider;

    private JLabel bandWidthLabel;

    public BandWidthSlider() {
        setOpaque(false);
        setLayout(new FlowLayout());
        bandWidthLabel = new JLabel();
        bandWidthSlider = new JSlider(MIN_SLIDER, MAX_SLIDER);
        bandWidthSlider.setOpaque(false);
        bandWidthSlider.setMajorTickSpacing(10);
        bandWidthSlider.addChangeListener(new ThrottleChangeListener(bandWidthSlider,
                bandWidthLabel));
        add(bandWidthSlider);
        add(bandWidthLabel);
    }

    public int getValue() {
        return bandWidthSlider.getValue();
    }

    public void setValue(int value) {
        bandWidthSlider.setValue(value);
    }

    /**
     * Changes the label for the download throttling slider based on the
     * slider's current value.
     */
    private class ThrottleChangeListener implements ChangeListener {

        private JSlider slider;

        private JLabel label;

        public ThrottleChangeListener(JSlider slider, JLabel label) {
            this.slider = slider;
            this.label = label;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            float value = slider.getValue();
            String labelText = "";
            if (value == 100)
                labelText = I18n.tr("Unlimited");
            else {
                Float f = new Float(((slider.getValue() / 100.0))
                        * ConnectionSettings.CONNECTION_SPEED.getValue() / 8);
                NumberFormat formatter = NumberFormat.getInstance();
                formatter.setMaximumFractionDigits(2);
                labelText = String.valueOf(formatter.format(f)) + " KB/s";
            }
            label.setText(labelText);
        }
    }
}
