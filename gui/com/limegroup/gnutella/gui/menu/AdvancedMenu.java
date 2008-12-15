package com.limegroup.gnutella.gui.menu;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.limewire.mojito.Context;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.visual.ArcsVisualizer;

import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTEvent.Type;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.actions.AbstractAction;

public class AdvancedMenu extends AbstractMenu {
    
    private final EnabledListener enabledListener;
    
    AdvancedMenu() {
        super(I18n.tr("&Advanced"));
        enabledListener = new EnabledListener(getMenu());
        
        ArcsAction arcsAction = new ArcsAction();
        addMenuItem(arcsAction);
        
        GuiCoreMediator.getDHTManager().addEventListener(arcsAction);
    }
    
    @Override
    protected JMenuItem addMenuItem(Action action) {
        action.addPropertyChangeListener(enabledListener);
        if(action.isEnabled())
            enabledListener.change(true);
        
        return super.addMenuItem(action);
    }
    
    /** Simple listener to keep the main menu in sync with its submenus. */
    private static class EnabledListener implements PropertyChangeListener {
        private final JMenu mainMenu;
        private int enableds;
        
        public EnabledListener(JMenu mainMenu) {
            this.mainMenu = mainMenu;
        }
        
        public void propertyChange(PropertyChangeEvent evt) {
            if("enabled".equals(evt.getPropertyName())) {
                change(((Boolean)evt.getNewValue()).booleanValue());
            }
        }
        
        void change(boolean plus) {
            if(plus)
                enableds++;
            else
                enableds--;
                
            if(enableds > 0)
                mainMenu.setEnabled(true);
            else
                mainMenu.setEnabled(false);
        }
    }    
    
    private static class ArcsAction extends AbstractAction implements DHTEventListener {

        public ArcsAction() {
            super(I18n.tr("Mojito &DHT Arcs View"));
            putValue(LONG_DESCRIPTION, I18n
                    .tr("Display a view of the incoming and outgoing DHT messages"));
            setEnabled(GuiCoreMediator.getDHTManager().isRunning());
        }

        public void actionPerformed(ActionEvent e) {
            MojitoDHT dht = GuiCoreMediator.getDHTManager().getMojitoDHT();
            if (dht != null) {
                ArcsVisualizer.show((Context) dht);
            }
        }

        public void handleDHTEvent(final DHTEvent evt) {
            setEnabledLater(evt.getType() != Type.STOPPED);
        }
    }
}
