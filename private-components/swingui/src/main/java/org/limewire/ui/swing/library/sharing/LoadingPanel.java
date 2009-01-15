package org.limewire.ui.swing.library.sharing;

import javax.swing.JLabel;

import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.util.I18n;

import net.miginfocom.swing.MigLayout;

public class LoadingPanel extends JXPanel{
    
    public LoadingPanel(){
        super(new MigLayout("gap 15 15 15 15"));
        //TODO make this loading dialog not suck
        JLabel loadingLabel = new JLabel(I18n.tr("Loading..."));
        
        add(loadingLabel);
    }

}
