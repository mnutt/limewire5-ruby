package org.limewire.ui.swing.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.spam.SpamManager;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Spam Option View
 */
public class SpamOptionPanel extends OptionPanel {
    
    private SpamManager spamManager;
    
    private JButton clearSpamButton;
    
    @Inject
    public SpamOptionPanel(SpamManager spamManager) {   
        this.spamManager = spamManager;
        
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getClearPanel(), "pushx, growx");
    }
    
    private JPanel getClearPanel() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder(""));
        p.setLayout(new MigLayout("gapy 10"));
        
        clearSpamButton = new JButton(I18n.tr("Clear Spam"));
        clearSpamButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                spamManager.clearFilterData();
                //disable spam button after first click to give feedback on action performed
                clearSpamButton.setEnabled(false);
            }
        });
        
        p.add(new JLabel(I18n.tr("Reset the Spam filter by clearing all files marked as spam")), "push");
        p.add(clearSpamButton);
        
        return p;
    }
    
    @Override
    boolean applyOptions() {
        return false;
    }

    @Override
    boolean hasChanged() {
        return false;
    }

    @Override
    public void initOptions() {
        //always make the spam button clickable
        clearSpamButton.setEnabled(true);
    }
}
