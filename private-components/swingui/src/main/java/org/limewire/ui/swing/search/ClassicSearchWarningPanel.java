package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.ResizeUtils;
import org.limewire.ui.swing.util.SwingUtils;

/**
 * A warning that a classic search needs to be shown.
 * This class listens on a setting, so you need to make sure you
 * #dispose() it when you're done!
 */
class ClassicSearchWarningPanel extends JXPanel implements SettingListener {
    
    @Resource private int height;
    @Resource private Color background;
    @Resource private Font textFont;
    @Resource private Color textColor;
    @Resource private Font switchFont;
    @Resource private Color switchColor;
    @Resource private Icon switchIcon;
    @Resource private Font closeFont;
    
    public ClassicSearchWarningPanel() {
        GuiUtils.assignResources(this);
        
        setLayout(new MigLayout("insets 0, gap 0, fill"));
        ResizeUtils.forceHeight(this, height);
        setBackground(background);
        
        HyperlinkButton close = new HyperlinkButton(new AbstractAction(I18n.tr("close")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUiSettings.SHOW_CLASSIC_REMINDER.setValue(false);
                ClassicSearchWarningPanel.this.setVisible(false);
            }
        });
        close.setFont(closeFont);
        add(close, "aligny center, gapleft 8, push");
        
        JLabel text1 = new JLabel(I18n.tr("Like the way search results used to look?"));
        text1.setFont(textFont);
        text1.setForeground(textColor);
        add(text1, "gapbefore push, aligny center, alignx right");
        
        JLabel text2 = new JLabel(I18n.tr("Switch to Classic View"));
        text2.setFont(switchFont);
        text2.setForeground(switchColor);
        add(text2, "gapbefore 4, aligny center, alignx right");
        
        add(new JLabel(switchIcon), "gapbefore 4, gapright 25, aligny top, gaptop 5, alignx right");
        
        SwingUiSettings.SHOW_CLASSIC_REMINDER.addSettingListener(this);
        if(!SwingUiSettings.SHOW_CLASSIC_REMINDER.getValue()) {
            setVisible(false);
        }
    }
    
    void dispose() {
        SwingUiSettings.SHOW_CLASSIC_REMINDER.removeSettingListener(this);
    }
    
    @Override
    public void settingChanged(SettingEvent evt) {
        SwingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                setVisible(SwingUiSettings.SHOW_CLASSIC_REMINDER.getValue());
            }
        });
    }

}
