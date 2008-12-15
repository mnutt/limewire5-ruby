package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.components.LimeCheckBox;
import org.limewire.ui.swing.components.LimeHeaderBar;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.dock.DockIcon;
import org.limewire.ui.swing.dock.DockIconFactory;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.FontUtils;
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
    
    public static final String NAME = "MainDownloadPanel";
	private final CardLayout cardLayout;
	private final JPanel cardPanel;
	private static final String NO_CATEGORY = "noCategory";
	private static final String CATEGORY = "category";
	
	private final DownloadMediator downloadMediator;
	
	private final LimeHeaderBar settingsPanel;
    private JButton pauseAllButton;
    private JButton resumeAllButton;
    private JXButton clearFinishedButton;
    private JCheckBox categoriseCheckBox;
    private final DockIcon dock;
	
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

    private final AbstractDownloadsAction clearAction = new AbstractDownloadsAction(I18n.tr("Clear Finished")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            downloadMediator.clearFinished();
            dock.draw(0);
        }
    };
    
    private final Action categorizeAction = new AbstractAction(I18n.tr("Categorize downloads")) {

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
	        DownloadMediator downloadMediator, AudioPlayer player,
	        LimeHeaderBarFactory headerBarFactory,
	        ButtonDecorator buttonDecorator, DockIconFactory dockIconFactory) {
	    
	    
		this.downloadMediator = downloadMediator;
		this.buttonDecorator = buttonDecorator;
		dock = dockIconFactory.createDockIcon();
		
		setLayout(new BorderLayout());
		
        resumeAction.setEnabled(false);
        pauseAction.setEnabled(false);
        clearAction.setEnabled(false);

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
		
		settingsPanel = headerBarFactory.createBasic(I18n.tr("Downloads"));
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
                clearAction.setEnablementFromDownloadSize(listChanges.getSourceList().size());
            }
        });
    }
	
	public void setCategorized(boolean categorized){
		cardLayout.show(cardPanel, categorized? CATEGORY : NO_CATEGORY);
	}
	
	private void initHeader() {
	    pauseAllButton = new JButton(pauseAction);	
	    resumeAllButton = new JButton(resumeAction);
	    clearFinishedButton = new JXButton(clearAction);
	    categoriseCheckBox = new LimeCheckBox(categorizeAction);

	    buttonDecorator.decorateDarkFullButton(clearFinishedButton);
	    
	    categoriseCheckBox.setOpaque(false);
	    categoriseCheckBox.setForeground(Color.WHITE);
	    FontUtils.changeStyle(categoriseCheckBox, Font.PLAIN);

	    categorizeAction.setEnabled(true);

	    JPanel buttonPanel = new JPanel(new FlowLayout());
	    buttonPanel.setOpaque(false);
	    buttonPanel.add(pauseAllButton);
	    buttonPanel.add(resumeAllButton);

	    this.settingsPanel.setLayout(new MigLayout("insets 0, fillx, filly","push[][]"));
	    this.settingsPanel.add(categoriseCheckBox);
	    this.settingsPanel.add(clearFinishedButton, "gapafter 10");
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
