package org.limewire.ui.swing.options;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.settings.ContentSettings;
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
            warningMessagesPanel.validate();
            
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
            
            setLayout(new MigLayout());
            add(new JLabel(I18n.tr("For your safety, LimeWire disables you from:")),"wrap, gaptop 10");
            add(new JLabel(I18n.tr("- Searching for and sharing programs with anyone")), "gapleft 25, wrap");
            add(new JLabel(I18n.tr("- Sharing documents with the P2P Network")), "gapleft 25, wrap");
            
            add(new JLabel("We strongly recommend you do not enable these settings"), "gaptop 10, push");
            add(configureButton, "gaptop 10");
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
        private FilterFileExtensionsOptionPanel filterFileExtensionPanel;
        
        private JCheckBox copyrightContentCheckBox;
        private JCheckBox adultContentCheckBox;
        private JButton filterKeywordsButton;
        private JButton filterFileExtensionsButton;
        
        public FilteringPanel() {
            super(I18n.tr("Filtering"));
            
            filterKeywordPanel = new FilterKeywordOptionPanel(spamManager, new OKDialogAction());
            filterKeywordPanel.setPreferredSize(new Dimension(300,400));
            
            filterFileExtensionPanel = new FilterFileExtensionsOptionPanel(spamManager, new OKDialogAction());
            filterFileExtensionPanel.setPreferredSize(new Dimension(300,400));
            
            copyrightContentCheckBox = new JCheckBox(I18n.tr("Don't let me download or upload files copyright owners request not be shared"));
            copyrightContentCheckBox.setContentAreaFilled(false);
            
            adultContentCheckBox = new JCheckBox(I18n.tr("Don't show adult content in search results"));
            adultContentCheckBox.setContentAreaFilled(false);
            
            filterKeywordsButton = new JButton(new DialogDisplayAction( SecurityOptionPanel.this,
                    filterKeywordPanel, I18n.tr("Filter Keywords"),
                    I18n.tr("Filter Keywords..."),I18n.tr("Restrict files with certain words from being displayed in search results")));
            
            filterFileExtensionsButton = new JButton(new DialogDisplayAction( SecurityOptionPanel.this,
                    filterFileExtensionPanel, I18n.tr("Filter File Extensions"),
                    I18n.tr("Filter File Extensions..."), I18n.tr("Restrict files with certain extensions from being displayed in search results")));
            
            add(new JLabel(I18n.tr("In search results...")), "wrap");
            
            add(copyrightContentCheckBox, "split, gapleft 20, wrap");
            add(adultContentCheckBox, "split, gapleft 20, wrap");
            
            add(filterKeywordsButton, "split, gapright 10");
            add(filterFileExtensionsButton);
        }
        
        @Override
        boolean applyOptions() {
            ContentSettings.USER_WANTS_MANAGEMENTS.setValue(copyrightContentCheckBox.isSelected());
            ContentSettings.CONTENT_MANAGEMENT_ACTIVE.setValue(copyrightContentCheckBox.isSelected());
            
            FilterSettings.FILTER_ADULT.setValue(adultContentCheckBox.isSelected());
            return filterKeywordPanel.applyOptions() || filterFileExtensionPanel.applyOptions();
        }

        @Override
        boolean hasChanged() {
            return  ContentSettings.USER_WANTS_MANAGEMENTS.getValue() != copyrightContentCheckBox.isSelected()
                    ||  ContentSettings.CONTENT_MANAGEMENT_ACTIVE.getValue() != copyrightContentCheckBox.isSelected()
                    ||FilterSettings.FILTER_ADULT.getValue() != adultContentCheckBox.isSelected()
                    || filterKeywordPanel.hasChanged()
                    || filterFileExtensionPanel.hasChanged();
        }

        @Override
        public void initOptions() {
            copyrightContentCheckBox.setSelected(ContentSettings.USER_WANTS_MANAGEMENTS.getValue() && ContentSettings.CONTENT_MANAGEMENT_ACTIVE.getValue());
            adultContentCheckBox.setSelected(FilterSettings.FILTER_ADULT.getValue());
            filterKeywordPanel.initOptions();
            filterFileExtensionPanel.initOptions();
        }
    }
}
