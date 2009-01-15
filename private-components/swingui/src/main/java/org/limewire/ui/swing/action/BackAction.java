package org.limewire.ui.swing.action;

import java.awt.event.ActionEvent;

import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class BackAction extends AbstractAction {
    
    private final Navigator navigator;
    
    @Resource private Icon pressedIcon;
    @Resource private Icon rolloverIcon;
    @Resource private Icon icon;
    
    @Inject BackAction(Navigator navigator) {
        this.navigator = navigator;
        GuiUtils.assignResources(this);
        putValue(PRESSED_ICON, pressedIcon);
        putValue(ROLLOVER_ICON, rolloverIcon);
        putValue(SMALL_ICON, icon);
        putValue(SHORT_DESCRIPTION, I18n.tr("Go back"));
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        navigator.goBack();
    }

}
