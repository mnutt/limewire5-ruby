package org.limewire.ui.swing.wizard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Locale;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.LanguageUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.ResizeUtils;
import org.limewire.util.SystemUtils;

public class IntentDialog extends LimeJDialog {

    private final Color backgroundColor = Color.WHITE;
    private final Font headingFont = new Font("Arial", Font.BOLD, 16);
    private final Font normalFont = new Font("Arial", Font.PLAIN, 14);
    
    private final JLabel headingLabel;
    private final MultiLineLabel bodyLabel;
    private final JLabel agreeLabel;
    private final HyperlinkButton linkButton;
    private final JButton agreeButton;
    private final JButton exitButton;
    
    private final String learnMoreURL = "http://www.limewire.com/learnMore/intent";
    
    private final Action urlAction = new AbstractAction(){
        @Override
        public void actionPerformed(ActionEvent e) {
           NativeLaunchUtils.openURL(learnMoreURL);
        }
    };
    
    private boolean agreed = false;
    
    public IntentDialog(String version){
        super();
        
        ResizeUtils.forceSize(this, new Dimension(590,400));
                
        setTitle("LimeWire " + version);
        setModal(true);
        setResizable(false);
        setAlwaysOnTop(true);
       
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBackground(backgroundColor);
        add(contentPane);
        JPanel panel = new JPanel(new MigLayout("nogrid, insets 0, gap 0"));
        panel.setOpaque(false);
        
        headingLabel = new JLabel();
        headingLabel.setFont(headingFont);
        agreeLabel = new MultiLineLabel("", 500);
        agreeLabel.setFont(normalFont);
        linkButton = new HyperlinkButton("", urlAction);
        linkButton.setFont(normalFont);
        linkButton.setForeground(new Color(0x2152a6));
        bodyLabel = new MultiLineLabel("", 500);        
        bodyLabel.setFont(normalFont);
        agreeButton = new JButton();
        exitButton = new JButton();
        
        int indent = 50;
        panel.add(headingLabel, "gapleft 10, gaptop 15, wrap");
        panel.add(bodyLabel, "gapleft " + indent + ", gaptop 20, wrap");
        panel.add(linkButton, "gapleft " + indent +  ", gaptop 20, wrap");
        panel.add(agreeLabel, "gapleft " + indent +  ", gaptop 70, wrap");

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        JPanel innerPanel = new JPanel(new FlowLayout());
        innerPanel.setOpaque(false);
        innerPanel.add(agreeButton);
        innerPanel.add(exitButton);
        bottomPanel.add(innerPanel, BorderLayout.EAST);
        
        JPanel langInnerPanel = new JPanel(new FlowLayout());
        langInnerPanel.setOpaque(false);
        langInnerPanel.setBorder(BorderFactory.createEmptyBorder(0,44,0,0));
        JComboBox languageDropDown = createLanguageDropDown(normalFont);        
        langInnerPanel.add(languageDropDown);
        bottomPanel.add(langInnerPanel, BorderLayout.WEST);

        setTextContents();
        
        contentPane.add(panel, BorderLayout.NORTH);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);
        
        pack();        
    }
    
    @Override
    public void addNotify() {
        super.addNotify();
        setAlwaysOnTop(true);
        SystemUtils.setWindowTopMost(this);
    }
    
    private void finish(boolean agreed){
        this.agreed = agreed;
        setVisible(false);
        dispose();
    }
    
    public boolean confirmLegal(){
        setLocationRelativeTo(null);
        setVisible(true);
        return agreed;
    }
    
    private JComboBox createLanguageDropDown(Font normalFont) {
        final JComboBox languageDropDown = new JComboBox();
        Locale[] locales = LanguageUtils.getLocales(normalFont);
        languageDropDown.setRenderer(new LocaleRenderer());
        languageDropDown.setFont(normalFont);
        languageDropDown.setModel(new DefaultComboBoxModel(locales));
        
        // Attempt to guess the default locale and set accordingly
        languageDropDown.setSelectedItem(LanguageUtils.guessBestAvailableLocale(locales));
        
        // Make sure the drop down and the set locale match.  This may cause the default OS
        //  language to be overridden to English in the case of a bad guess.  This has always been
        //  a problem but in this case at least it will be obvious that the users language is being
        //  overridden.
        LanguageUtils.setLocale((Locale)languageDropDown.getSelectedItem());
                
        languageDropDown.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Locale locale = (Locale) languageDropDown.getSelectedItem();
                    LanguageUtils.setLocale(locale);
                    setTextContents();
                }
            }
            
        });

        return languageDropDown;
    }
    
    
    private static class LocaleRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
            
            if (value instanceof Locale) {
                Locale locale = (Locale) value;
                setText(locale.getDisplayName(locale));
            }
            
            return this;
        }
    }

    
    private void setTextContents() {

        String heading  = " ";
        String bodyText1
        = I18n.tr("LimeWire Basic and LimeWire PRO are peer-to-peer programs for sharing authorized files only.  Installing and using either program does not constitute a license for obtaining or distributing unauthorized content.");
        String linkText = I18n.tr("Learn more");
        String agreementText = I18n.tr("By clicking \"I Agree\", you agree to not use LimeWire for copyright infringement.");
        
        
        Action exitAction = new AbstractAction(I18n.tr("Exit")){
            @Override
            public void actionPerformed(ActionEvent e) {
                finish(false);
            }
        };
        
        Action agreeAction = new AbstractAction(I18n.tr("I Agree")){
            @Override
            public void actionPerformed(ActionEvent e) {
                finish(true);
            }
        };    
        
        headingLabel.setText(heading);
        bodyLabel.setText(bodyText1);
        linkButton.setText(linkText);
        agreeLabel.setText(agreementText);
        exitButton.setAction(exitAction);
        agreeButton.setAction(agreeAction);
    }
}
