package com.limegroup.gnutella.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Locale;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.limewire.core.settings.StatusBarSettings;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.limegroup.gnutella.gui.actions.AbstractAction;

public class LanguageWindow extends JDialog {

    private static final int MIN_DIALOG_WIDTH = 350;

    private static final String TRANSLATE_URL = "http://www.limewire.org/translate.shtml";

    private JCheckBox showLanguageCheckbox;

    private JComboBox localeComboBox;

    private JPanel mainPanel;

    private OkayAction okayAction;

    private CancelAction cancelAction;

    private Locale currentLocale;

    private URLLabel helpTranslateLabel;

    private boolean defaultLocaleSelectable;
    
    public LanguageWindow() {
        super(GUIMediator.getAppFrame());

        this.currentLocale = GUIMediator.getLocale();

        initializeWindow();

        Font font = new Font("Dialog", Font.PLAIN, 11);
        Locale[] locales = LanguageUtils.getLocales(font);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        getContentPane().add(mainPanel, BorderLayout.CENTER);

        initializeContent(locales);
        initializeButtons();
        initializeWindow();
        
        updateLabels(currentLocale);
        pack();
        
        if (getWidth() < MIN_DIALOG_WIDTH) {
            setSize(350, getHeight());
        }
    }

    private void initializeWindow() {
        setModal(true);
        // setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        GUIUtils.addHideAction(this);
    }

    private void initializeContent(Locale[] locales) {
        FormLayout layout = new FormLayout("pref:grow");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        // add locales to model and select the best match 
        DefaultComboBoxModel localeModel = new DefaultComboBoxModel();
        int selectedScore = -1;
        int selectedIndex = -1;
        Locale systemLocale = Locale.getDefault();
        for (int i = 0; i < locales.length; i++) {
            localeModel.addElement(locales[i]);
            int score = LanguageUtils.getMatchScore(currentLocale, locales[i]);
            if (score > selectedScore) {
                selectedScore = score;
                selectedIndex = i;
            }
            if (locales[i].equals(systemLocale)) {
                defaultLocaleSelectable = true;
            }
        }

        localeComboBox = new JComboBox(localeModel);
        localeComboBox.setRenderer(LanguageFlagFactory.getListRenderer());
        localeComboBox.setMaximumRowCount(15);
        if (selectedIndex != -1) {
            localeComboBox.setSelectedIndex(selectedIndex);
        }
        builder.append(localeComboBox);
        builder.nextLine();

        // reflect the changed language right away so someone who doesn't speak
        // English or whatever language it the default can understand what the
        // buttons say
        localeComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Locale selected = (Locale) e.getItem();
                    if (selected != null && !currentLocale.equals(selected)) {
                        updateLabels(selected);
                        // hide the flag by default for english locales to save
                        // space in the status bar
                        showLanguageCheckbox.setSelected(!LanguageUtils.isEnglishLocale(selected));
                        currentLocale = selected;
                    }
                }
            }
        });

        helpTranslateLabel = new URLLabel(TRANSLATE_URL, "");
        builder.append(helpTranslateLabel);
        builder.nextLine();

        builder.append(Box.createVerticalStrut(15));
        builder.nextLine();

        showLanguageCheckbox = new JCheckBox();
        showLanguageCheckbox.setSelected(StatusBarSettings.LANGUAGE_DISPLAY_ENABLED.getValue());
        builder.append(showLanguageCheckbox);
        builder.nextLine();

        builder.append(Box.createVerticalStrut(5));
        builder.nextLine();

        mainPanel.add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initializeButtons() {
        okayAction = new OkayAction();
        cancelAction = new CancelAction();

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        buttonPanel.add(new JButton(okayAction));
        if (!LanguageUtils.isEnglishLocale(currentLocale)) {
            buttonPanel.add(new JButton(new UseEnglishAction()));
        }
        buttonPanel.add(new JButton(cancelAction));
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void switchLanguage(Locale locale, boolean showLanguageInStatusBar) {
        // if the selected locale is less specific than the default locale (e.g.
        // has no country or variant set), retain these properties, unless the user
        // specifically did not want to select the default locale
        if (!defaultLocaleSelectable && LanguageUtils.matchesDefaultLocale(locale)) {
            locale = Locale.getDefault();
        }

        if (!locale.equals(GUIMediator.getLocale())) {
            LanguageUtils.setLocale(locale);
            GUIMediator.instance().getStatusLine().updateLanguage();

            String message = I18n.trl(
                    "LimeWire must be restarted for the new language to take effect.", locale);
            GUIMediator.showMessage(message);
        }

        StatusBarSettings.LANGUAGE_DISPLAY_ENABLED.setValue(showLanguageInStatusBar);
        if (LanguageUtils.isEnglishLocale(locale)) {
            StatusBarSettings.LANGUAGE_DISPLAY_ENGLISH_ENABLED.setValue(showLanguageInStatusBar);
        }
        GUIMediator.instance().getStatusLine().refresh();
        dispose();
    }

    private void updateLabels(Locale locale) {
        setTitle(I18n.trl("Change Language", locale));
        okayAction.putValue(Action.NAME, I18n.trl("OK", locale));
        cancelAction.putValue(Action.NAME, I18n.trl("Cancel", locale));
        helpTranslateLabel.setText(I18n.trl("Help Translate LimeWire", locale));
        showLanguageCheckbox.setText(I18n.trl("Show Language in status bar", locale));
    }

    private class OkayAction extends AbstractAction {

        public OkayAction() {
        }

        public void actionPerformed(ActionEvent event) {
            Locale locale = (Locale) localeComboBox.getSelectedItem();
            switchLanguage(locale, showLanguageCheckbox.isSelected());
        }

    }

    private class CancelAction extends AbstractAction {

        public CancelAction() {
        }

        public void actionPerformed(ActionEvent event) {
            GUIUtils.getDisposeAction().actionPerformed(event);
        }

    }

    private class UseEnglishAction extends AbstractAction {

        public UseEnglishAction() {
            // note: this string is intentionally not translated
            putValue(NAME, "Use English");
        }

        public void actionPerformed(ActionEvent event) {
            switchLanguage(Locale.ENGLISH, showLanguageCheckbox.isSelected());
        }

    }

}
