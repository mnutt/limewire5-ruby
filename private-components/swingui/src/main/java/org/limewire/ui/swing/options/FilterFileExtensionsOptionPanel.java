package org.limewire.ui.swing.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.settings.FilterSettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.swing.DefaultEventTableModel;

/**
 * Creates a table to manage which file extensions will not show up in search results.
 */
public class FilterFileExtensionsOptionPanel extends AbstractFilterOptionPanel {
    private JButton defaultButton;
    private JButton okButton;
    private JTextField keywordTextField;
    private JButton addKeywordButton;
    private FilterTable filterTable;
    private SpamManager spamManager;
    
    public FilterFileExtensionsOptionPanel(SpamManager spamManager, Action okAction) {
        this.spamManager = spamManager;
        setLayout(new MigLayout("gapy 10"));
        
        keywordTextField = new JTextField(30);
        addKeywordButton = new JButton(I18n.tr("Add Extension"));
        
        filterTable = new FilterTable(new DefaultEventTableModel<String>(eventList, new FilterTableFormat(I18n.tr("Extensions"))));
        okButton = new JButton(okAction);
        addKeywordButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = keywordTextField.getText();
                if(text == null || text.trim().length() == 0)
                    return;
                if(!eventList.contains(text)) {
                    if(text.charAt(0) != '.')
                        text = "." + text;
                    eventList.add(text);
                }
                keywordTextField.setText("");
            }
        });
        
        defaultButton = new JButton(new DefaultAction());
        
        add(new MultiLineLabel(I18n.tr("LimeWire will not show files with the following extensions in your search results"), 300), "span, wrap");
        add(keywordTextField, "gapright 10");
        add(addKeywordButton,"wrap");
        add(new JScrollPane(filterTable), "span 2, grow, wrap");
        
        add(defaultButton, "alignx left");
        add(okButton, "tag ok, alignx right");
    }
    
    @Override
    boolean applyOptions() {
        String[] values = eventList.toArray(new String[eventList.size()]);
        FilterSettings.BANNED_EXTENSIONS.set(values);
        spamManager.adjustSpamFilters();
        return false;
    }

    @Override
    boolean hasChanged() {
        List model = Arrays.asList(FilterSettings.BANNED_EXTENSIONS.get());
        String[] values = eventList.toArray(new String[eventList.size()]);
        
        return model.equals(new ArrayList<String>(Arrays.asList(values)));
    }

    @Override
    public void initOptions() {
        eventList.clear();
        String[] bannedWords = FilterSettings.BANNED_EXTENSIONS.get();
        eventList.addAll(new ArrayList<String>(Arrays.asList(bannedWords)));
    }
    
    /**
     * Reverts the extensions not shown in search results to the default setting.
     */
    private class DefaultAction extends AbstractAction {
        public DefaultAction() {
            putValue(Action.NAME, I18n.tr("Use Default"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Revert to default settings"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            FilterSettings.BANNED_EXTENSIONS.revertToDefault();
            String[] bannedWords = FilterSettings.BANNED_EXTENSIONS.get();
            eventList.clear();
            eventList.addAll(new ArrayList<String>(Arrays.asList(bannedWords)));
        }
    }
}
