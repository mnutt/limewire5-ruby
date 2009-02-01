package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXLabel;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.settings.SharingSettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.BackAction;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.dock.DockIcon;
import org.limewire.ui.swing.dock.DockIconFactory;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.painter.TextShadowPainter;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MainDownloadPanel extends JPanel {
	
    private final ButtonDecorator buttonDecorator;
    private final ComboBoxDecorator comboBoxDecorator;
    
    public static final String NAME = "MainDownloadPanel";
	private final CardLayout cardLayout;
	private final JPanel cardPanel;
	private static final String NO_CATEGORY = "noCategory";
	private static final String CATEGORY = "category";
	
	private final DownloadMediator downloadMediator;
	
	private final HeaderBar settingsPanel;
    
	private LimeComboBox moreButton;
    private JXButton clearFinishedNowButton;
    private final DockIcon dock;
    
    private JCheckBoxMenuItem categoriseCheckBox;
    private JCheckBoxMenuItem clearFinishedCheckBox;
	
    private final AbstractDownloadsAction pauseAction = new AbstractDownloadsAction(I18n.tr("Pause All")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            downloadMediator.pauseAll();
        }
    };

    private final AbstractDownloadsAction resumeAction = new AbstractDownloadsAction(I18n.tr("Resume All")) {
        public void actionPerformed(ActionEvent e) {
            downloadMediator.resumeAll();
        }
    };

    private final AbstractDownloadsAction clearFinishedNowAction = new AbstractDownloadsAction(I18n.tr("Clear Finished")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            downloadMediator.clearFinished();
            dock.draw(0);
        }
    };
   
    private final AbstractDownloadsAction clearFinishedAction = new AbstractDownloadsAction(I18n.tr("Clear When Finished")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            SharingSettings.CLEAR_DOWNLOAD.setValue(clearFinishedCheckBox.isSelected());
        }
    };
    
    private final Action categorizeAction = new AbstractAction(I18n.tr("Categorize Downloads")) {

        @Override
        public void actionPerformed(ActionEvent e) {
            setCategorized(categoriseCheckBox.isSelected());
        }

    };
    
    private abstract class AbstractDownloadsAction extends AbstractAction {

        private AbstractDownloadsAction(String name) {
            super(name);
        }
        
        /**
         * Enables this action if the supplied downloadSize is greater than zero,
         * and updates the dock icon with the number of downloads.
         * @param downloadSize
         */
        public void setEnablementFromDownloadSize(int downloadSize) {
            setEnabled(downloadSize > 0);
            dock.draw(downloadSize);
        }
    }
    
	/**
	 * Create the panel
	 */
	@Inject
	public MainDownloadPanel(AllDownloadPanelFactory allDownloadPanelFactory, 
	        CategoryDownloadPanelFactory categoryDownloadPanelFactory,
	        DownloadMediator downloadMediator,
	        HeaderBarDecorator headerBarDecorator, ComboBoxDecorator comboBoxFactory,
	        ButtonDecorator buttonDecorator, DockIconFactory dockIconFactory,
	        BackAction backAction) {
	    
	    
		this.downloadMediator = downloadMediator;
		this.buttonDecorator = buttonDecorator;
		this.comboBoxDecorator = comboBoxFactory;
		
		dock = dockIconFactory.createDockIcon();
		
		setLayout(new BorderLayout());
		
        resumeAction.setEnabled(false);
        pauseAction.setEnabled(false);
        clearFinishedNowAction.setEnabled(false);

		cardPanel = new JPanel();
		cardLayout = new CardLayout();
		cardPanel.setLayout(cardLayout);
		add(cardPanel, BorderLayout.CENTER);
		
        
        final AllDownloadPanel noCategoryPanel = allDownloadPanelFactory.create(downloadMediator.getDownloadList());
		noCategoryPanel.setName(NO_CATEGORY);
		cardPanel.add(noCategoryPanel, noCategoryPanel.getName());
		
		final CategoryDownloadPanel categoryPanel = categoryDownloadPanelFactory.create(downloadMediator.getDownloadList());
		categoryPanel.setName(CATEGORY);
		cardPanel.add(categoryPanel, categoryPanel.getName());
		
		JPanel headerTitlePanel = new JPanel(new MigLayout("insets 0, gap 0, fill, aligny center"));
        headerTitlePanel.setOpaque(false);        
        JXLabel titleTextLabel = new JXLabel(I18n.tr("Downloads"));
        titleTextLabel.setForegroundPainter(new TextShadowPainter());
        IconButton backButton = new IconButton(backAction);
        backButton.removeActionHandListener();
        backButton.setRolloverEnabled(true);        
        headerTitlePanel.add(backButton, "gapafter 6, gapbottom 1");
        headerTitlePanel.add(titleTextLabel, "gapbottom 2");        
        
        settingsPanel = new HeaderBar(headerTitlePanel);
        settingsPanel.linkTextComponent(titleTextLabel);
        headerBarDecorator.decorateBasic(settingsPanel);
        
        this.initHeader();
		add(settingsPanel, BorderLayout.NORTH);
		
		cardLayout.show(cardPanel, NO_CATEGORY);
		
		EventList<DownloadItem> pausableList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
		        new PausableMatcher());
		EventList<DownloadItem> resumableList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new ResumableMatcher());
		EventList<DownloadItem> doneList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                        new DownloadStateMatcher(DownloadState.DONE));
		
		pausableList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                pauseAction.setEnablementFromDownloadSize(listChanges.getSourceList().size());
            }
        });

        resumableList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                resumeAction.setEnablementFromDownloadSize(listChanges.getSourceList().size());
            }
        });
        
        doneList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                clearFinishedNowAction.setEnablementFromDownloadSize(listChanges.getSourceList().size());
            }
        });
    }
	
	public void setCategorized(boolean categorized){
		cardLayout.show(cardPanel, categorized? CATEGORY : NO_CATEGORY);
	}
	
	private void initHeader() {
	    clearFinishedNowButton = new JXButton(clearFinishedNowAction);
	    buttonDecorator.decorateDarkFullButton(clearFinishedNowButton);
	    
	    moreButton = new LimeComboBox();
	    comboBoxDecorator.decorateDarkFullComboBox(moreButton);
	    moreButton.setText(I18n.tr("more"));

	    categoriseCheckBox = new JCheckBoxMenuItem(categorizeAction);
	    clearFinishedCheckBox = new JCheckBoxMenuItem(clearFinishedAction);

	    clearFinishedCheckBox.setSelected(SharingSettings.CLEAR_DOWNLOAD.getValue());
	    SharingSettings.CLEAR_DOWNLOAD.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtilities.invokeLater(new Runnable(){
                    public void run() {
                        clearFinishedCheckBox.setSelected(SharingSettings.CLEAR_DOWNLOAD.getValue());                        
                    }
                });
            }
	    });
	    
	    JPopupMenu menu = new JPopupMenu();
	    menu.add(moreButton.createMenuItem(pauseAction));
	    menu.add(moreButton.createMenuItem(resumeAction));
	    menu.addSeparator();
	    menu.add(moreButton.decorateMenuComponent(categoriseCheckBox));
	    menu.add(moreButton.decorateMenuComponent(clearFinishedCheckBox));
	    
	    moreButton.overrideMenu(menu);
	    
	    this.settingsPanel.setLayout(new MigLayout("insets 0, fillx, filly","push[][]"));
	    this.settingsPanel.add(clearFinishedNowButton, "gapafter 5");
	    this.settingsPanel.add(moreButton, "gapafter 5");
	}
	
	
	
	private static class PausableMatcher implements Matcher<DownloadItem> {
        @Override
        public boolean matches(DownloadItem item) {
            return item.getState().isPausable();
        }
    }

    private static class ResumableMatcher implements Matcher<DownloadItem> {
        @Override
        public boolean matches(DownloadItem item) {
            return item.getState().isResumable();
        }
    }

}
