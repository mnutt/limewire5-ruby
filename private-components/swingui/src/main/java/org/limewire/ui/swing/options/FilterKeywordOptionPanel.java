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
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.GlazedListsSwingFactory;
import org.limewire.ui.swing.util.I18n;

/**
 * Creates a table to manage which words will not show up in search results.
 */
public class FilterKeywordOptionPanel extends AbstractFilterOptionPanel {

    private JButton okButton;
    private JTextField keywordTextField;
    private JButton addKeywordButton;
    private FilterTable filterTable;
    private SpamManager spamManager;
    
    public FilterKeywordOptionPanel(SpamManager spamManager, Action okAction) {
        this.spamManager = spamManager;
        setLayout(new MigLayout("gapy 10"));
        
        keywordTextField = new JTextField(30);
        addKeywordButton = new JButton(I18n.tr("Add Keyword"));
        
        filterTable = new FilterTable(GlazedListsSwingFactory.eventTableModel(eventList, new FilterTableFormat(I18n.tr("Keyword"))));
        okButton = new JButton(okAction);
        addKeywordButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = keywordTextField.getText();
                if(text == null || text.trim().length() == 0)
                    return;
                if(!eventList.contains(text)) {
                    eventList.add(text);
                }
                keywordTextField.setText("");
            }
        });
        
        add(new MultiLineLabel(I18n.tr("LimeWire will not show files with the following keywords in your search results"), 300), "span, wrap");
        add(keywordTextField, "gapright 10");
        add(addKeywordButton,"wrap");
        add(new JScrollPane(filterTable), "span 2, grow, wrap");
        
        add(okButton, "skip 1, alignx right");
    }
    
    @Override
    boolean applyOptions() {
        String[] values = eventList.toArray(new String[eventList.size()]);
        FilterSettings.BANNED_WORDS.setValue(values);
        spamManager.adjustSpamFilters();
        return false;
    }

    @Override
    boolean hasChanged() {
        List model = Arrays.asList(FilterSettings.BANNED_WORDS.getValue());
        String[] values = eventList.toArray(new String[eventList.size()]);
        
        return model.equals(new ArrayList<String>(Arrays.asList(values)));
    }

    @Override
    public void initOptions() {
        eventList.clear();
        String[] bannedWords = FilterSettings.BANNED_WORDS.getValue();
        eventList.addAll(new ArrayList<String>(Arrays.asList(bannedWords)));
    }
}
