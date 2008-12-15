package org.limewire.ui.swing.options;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.settings.FilterSettings;
import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.options.actions.DialogDisplayAction;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Security Option View
 */
public class SecurityOptionPanel extends OptionPanel {
    
    private WarningMessagesPanel warningMessagesPanel;
    private UnsafeTypesPanel unsafeTypesPanel;
    private FilteringPanel filteringPanel;
    private final SpamManager spamManager;
    private final LibraryManager libraryManager;
    private final ShareListManager shareListManager;
    
    @Inject
    public SecurityOptionPanel(SpamManager spamManager, LibraryManager libraryManager, ShareListManager shareListManager) {
        this.spamManager = spamManager;
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getWarningMessagesPanel(), "pushx, growx");
        add(getUnsafeTypesPanel(), "pushx, growx");
        add(getFilteringPanel(), "pushx, growx");
    }
    
    private OptionPanel getWarningMessagesPanel() {
        if(warningMessagesPanel == null) {
            warningMessagesPanel = new WarningMessagesPanel();
        }
        return warningMessagesPanel;
    }
    
    private OptionPanel getUnsafeTypesPanel() {
        if(unsafeTypesPanel == null) {
            unsafeTypesPanel = new UnsafeTypesPanel();
        }
        return unsafeTypesPanel;
    }
    
    private OptionPanel getFilteringPanel() {
        if(filteringPanel == null) {
            filteringPanel = new FilteringPanel();
        }
        return filteringPanel;
    }

    @Override
    boolean applyOptions() {
        boolean restart = getWarningMessagesPanel().applyOptions();
        restart |= getUnsafeTypesPanel().applyOptions();
        restart |= getFilteringPanel().applyOptions();

        return restart;
    }

    @Override
    boolean hasChanged() {
        return getWarningMessagesPanel().hasChanged() || getUnsafeTypesPanel().hasChanged() || getFilteringPanel().hasChanged();
    }

    @Override
    public void initOptions() {
        getWarningMessagesPanel().initOptions();
        getUnsafeTypesPanel().initOptions();
        getFilteringPanel().initOptions();
    }
    
    private class WarningMessagesPanel extends OptionPanel {

        private WarningMessagesOptionPanel warningMessagesPanel;
        private JButton settingsButton;
        
        public WarningMessagesPanel() {
            super(I18n.tr("Warning Messages"));
            
            warningMessagesPanel = new WarningMessagesOptionPanel(new OKDialogAction(), new CancelDialogAction());
            warningMessagesPanel.setPreferredSize(new Dimension(400,130));
            
            settingsButton = new JButton(new DialogDisplayAction(SecurityOptionPanel.this ,
                    warningMessagesPanel, I18n.tr("Warning Messages"),
                    I18n.tr("Settings..."), I18n.tr("Choose which warning messags to display")));
            
            add(new JLabel(I18n.tr("Choose which warning messages you want to see when using LimeWire")), "push");
            add(settingsButton);
        }
        
        @Override
        boolean applyOptions() {
            return warningMessagesPanel.applyOptions();
        }

        @Override
        boolean hasChanged() {
            return warningMessagesPanel.hasChanged();
        }

        @Override
        public void initOptions() {
            warningMessagesPanel.initOptions();
        }
    }
    
    private class UnsafeTypesPanel extends OptionPanel {
        private UnsafeTypeOptionPanel unsafeOptionPanel;
        private JButton configureButton;
        
        public UnsafeTypesPanel() {
            super(I18n.tr("Unsafe Categories"));
            
          unsafeOptionPanel = new UnsafeTypeOptionPanel(new OKDialogAction(), libraryManager, shareListManager);
            
            configureButton = new JButton(new DialogDisplayAction( SecurityOptionPanel.this,
                    unsafeOptionPanel, I18n.tr("Unsafe Categories"),
                    I18n.tr("Configure..."), I18n.tr("Configure unsafe categories")));
            
            add(new JLabel(I18n.tr("For your safety, LimeWire disables you from:")),"wrap");
            add(new JLabel(I18n.tr("- Searching for and sharing programs with anyone")), "gapleft 25, wrap");
            add(new JLabel(I18n.tr("- Sharing documents with the P2P Network")), "gapleft 25, wrap");
            
            add(new JLabel("We strongly recommend you do not enable these settings"), "push");
            add(configureButton);
        }
        
        @Override
        boolean applyOptions() {
            return unsafeOptionPanel.applyOptions();
        }

        @Override
        boolean hasChanged() {
            return unsafeOptionPanel.hasChanged();
        }

        @Override
        public void initOptions() {
            unsafeOptionPanel.initOptions();
        }
    }
    
    private class FilteringPanel extends OptionPanel {

        private FilterKeywordOptionPanel filterKeywordPanel;
        
        private JCheckBox adultContentCheckBox;
        private JButton filterKeywordsButton;
        
        public FilteringPanel() {
            super(I18n.tr("Filtering"));
            
            filterKeywordPanel = new FilterKeywordOptionPanel(spamManager, new OKDialogAction());
            filterKeywordPanel.setPreferredSize(new Dimension(300,400));
            
            adultContentCheckBox = new JCheckBox(I18n.tr("Don't show adult content"));
            adultContentCheckBox.setContentAreaFilled(false);
            filterKeywordsButton = new JButton(new DialogDisplayAction( SecurityOptionPanel.this,
                    filterKeywordPanel, I18n.tr("Filter Keywords"),
                    I18n.tr("Filter Keywords..."),I18n.tr("Restrict files with certain words from being displayed in search results")));
            
            add(new JLabel(I18n.tr("In search results...")), "wrap");
            
            add(adultContentCheckBox, "split, gapleft 20, wrap");
            
            add(filterKeywordsButton);
        }
        
        @Override
        boolean applyOptions() {
            FilterSettings.FILTER_ADULT.setValue(adultContentCheckBox.isSelected());
            return filterKeywordPanel.applyOptions();
        }

        @Override
        boolean hasChanged() {
            return  FilterSettings.FILTER_ADULT.getValue() != adultContentCheckBox.isSelected()
                    || filterKeywordPanel.hasChanged();
        }

        @Override
        public void initOptions() {
            adultContentCheckBox.setSelected(FilterSettings.FILTER_ADULT.getValue());
            filterKeywordPanel.initOptions();
        }
    }
}
