package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JMenu;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.downloads.DownloadSummaryPanel;
import org.limewire.ui.swing.friends.chat.ChatFramePanel;
import org.limewire.ui.swing.mainframe.LeftPanel;
import org.limewire.ui.swing.util.ForceInvisibleComponent;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.VisibilityListener;
import org.limewire.ui.swing.util.VisibleComponent;

import com.google.inject.Inject;

public class ViewMenu extends JMenu {
    
    @Inject
    public ViewMenu(final LeftPanel leftPanel, final DownloadSummaryPanel downloadSummaryPanel, final ChatFramePanel friendsPanel) {
        super(I18n.tr("View"));
        add(buildAction(leftPanel, I18n.tr("Hide Sidebar"), I18n.tr("Show Sidebar")));
        add(buildForceInvisibleAction(downloadSummaryPanel, I18n.tr("Hide Download Tray"), I18n.tr("Show Download Tray")));
        add(buildAction(friendsPanel, I18n.tr("Hide Chat Window"), I18n.tr("Show Chat Window")));
    }

    private Action buildAction(final VisibleComponent component, final String visibleName,
            final String notVisibleName) {
        final Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                component.toggleVisibility();
            }
        };

        addVisibilityListener(component, action, visibleName, notVisibleName); 
        setInitialText(component, action, visibleName, notVisibleName);  
        
        return action;
    }
    
    private Action buildForceInvisibleAction(final ForceInvisibleComponent component, final String visibleName,
            final String notVisibleName) {
        final Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //forcibly toggles visibility
                component.forceInvisibility(component.isVisible());
            }
        };

        addVisibilityListener(component, action, visibleName, notVisibleName); 
        setInitialText(component, action, visibleName, notVisibleName);      

        return action;
    }
    
    private void addVisibilityListener(VisibleComponent component, final Action action, final String visibleName,
            final String notVisibleName){
        component.addVisibilityListener(new VisibilityListener() {
            @Override
            public void visibilityChanged(boolean visible) {
                if (visible) {
                    action.putValue(Action.NAME, visibleName);
                } else {
                    action.putValue(Action.NAME, notVisibleName);
                }
            }
        });
    }
    
    private void setInitialText(VisibleComponent component, final Action action, final String visibleName,
            final String notVisibleName){
        if (component.isVisible()) {
            action.putValue(Action.NAME, visibleName);
        } else {
            action.putValue(Action.NAME, notVisibleName);
        }
    }
}
