package org.limewire.ui.swing.options;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Advanced Option View.
 */
public class AdvancedOptionPanel extends OptionPanel {
    
    private OptionPanel[] list = new OptionPanel[4];
    private List<Provider<? extends OptionPanel>> providerList = new ArrayList<Provider<? extends OptionPanel>>(4);
    private final JTabbedPane tabbedPane;
    
    @Inject
    public AdvancedOptionPanel(Provider<FilesOptionPanel> filesOptionPanel, Provider<ConnectionsOptionPanel> connectionsOptionPanel,
                    Provider<SystemOptionPanel> systemOptionPanel, Provider<ReallyAdvancedOptionPanel> reallyAdvancedOptionPanel) {
        
        providerList.add(filesOptionPanel);
        providerList.add(connectionsOptionPanel);
        providerList.add(systemOptionPanel);
        providerList.add(reallyAdvancedOptionPanel);
        
        setLayout(new MigLayout("insets 12 12 8 12, fill"));
        
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab(I18n.tr("Files"), new JPanel());
        tabbedPane.addTab(I18n.tr("Transfers"), new JPanel());
        tabbedPane.addTab(I18n.tr("System"), new JPanel());
        tabbedPane.addTab(I18n.tr("Super Really Advanced"), new JPanel());
        
        add(tabbedPane, "grow");
    }
    
    @Inject
    void register() {
        tabbedPane.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                createTab(tabbedPane.getSelectedIndex(), true);
            }
        });
    }

    @Override
    boolean applyOptions() {
        boolean restartRequired = false;
        for(OptionPanel panel : list) {
            if(panel == null)
                continue;
            restartRequired |= panel.applyOptions();
        }
        return restartRequired;
    }

    @Override
    boolean hasChanged() {
        for(OptionPanel panel : list) {
            if(panel == null)
                continue;
            if(panel.hasChanged())
                return true;
        }
        return false;
    }

    @Override
    public void initOptions() {
        
        createTab(0, false);
        
        for(OptionPanel optionPanel : list) {
            if(optionPanel != null) {
                optionPanel.initOptions();
            }
        }
    }
    
    private void createTab(int index, boolean init) {
        if(list[index] == null) {
            list[index] = providerList.get(index).get();
            tabbedPane.setComponentAt(index, list[index]);
            
            if (init) {
                list[index].initOptions();
            }
        }
    }
}
