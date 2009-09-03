package org.limewire.ui.swing.downloads;

import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.dock.DockIconFactory;
import org.limewire.ui.swing.downloads.table.DownloadStateExcluder;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.painter.factories.BarPainterFactory;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.ResizeUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;

/**
 * Panel that is displayed above the download table.
 */
public class DownloadHeaderPanel {

    @Resource
    private Icon moreButtonArrow;
    @Resource
    private Font hyperlinkFont;

    private final DownloadMediator downloadMediator;    
    private final DownloadHeaderPopupMenu downloadHeaderPopupMenu;
    private final ClearFinishedDownloadAction clearFinishedDownloadAction;
    private final FixStalledDownloadAction fixStalledDownloadAction;
    private final ComboBoxDecorator comboBoxDecorator;
    
    private final JXPanel component;

    private JLabel titleTextLabel;
    private HyperlinkButton fixStalledButton;
    private HyperlinkButton clearFinishedNowButton;
    private LimeComboBox moreButton;      
    
    private EventList<DownloadItem> activeList;
    
    @Inject
    public DownloadHeaderPanel(DownloadMediator downloadMediator, DownloadHeaderPopupMenu downloadHeaderPopupMenu, 
            ClearFinishedDownloadAction clearFinishedNowAction, FixStalledDownloadAction fixStalledDownloadAction,
            ComboBoxDecorator comboBoxDecorator, BarPainterFactory barPainterFactory, DockIconFactory iconFactory) {
        
        this.downloadMediator = downloadMediator;
        this.downloadHeaderPopupMenu = downloadHeaderPopupMenu;
        this.clearFinishedDownloadAction = clearFinishedNowAction;
        this.fixStalledDownloadAction = fixStalledDownloadAction;
        this.comboBoxDecorator = comboBoxDecorator;
        
        GuiUtils.assignResources(this);
        hyperlinkFont = FontUtils.deriveUnderline(hyperlinkFont, true);
        
        component = new JXPanel(new MigLayout("insets 2 0 2 0, gap 0, novisualpadding, fill"));
        component.setBackgroundPainter(barPainterFactory.createDownloadSummaryBarPainter());
        ResizeUtils.forceHeight(component, 20);
        
        // initialize the dock icon since it registers as a Service
        iconFactory.createDockIcon();
        
        initialize();
    }
    
    public JComponent getComponent() {
        return component;
    }
    
    private void initialize(){
        initializeComponents();        
        layoutComponents();        
    }

    private void initializeComponents(){        
        titleTextLabel = new JLabel(I18n.tr("Downloads"));
        
        clearFinishedNowButton = new HyperlinkButton(clearFinishedDownloadAction);
        clearFinishedNowButton.setFont(hyperlinkFont);
        clearFinishedNowButton.setEnabled(false);

        fixStalledButton = new HyperlinkButton(fixStalledDownloadAction);
        fixStalledButton.setFont(hyperlinkFont);
        fixStalledButton.setVisible(false);

        initializeMoreButton();
    }
    
    private void layoutComponents(){
        component.add(titleTextLabel, "gapbefore 5, push");   
        component.add(fixStalledButton, "gapafter 5, hidemode 3");  
        component.add(clearFinishedNowButton, "gapafter 5, hidemode 3");
        component.add(moreButton, "gapafter 5");  
    }
        
    @Inject
    public void register(){
        activeList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new DownloadStateExcluder(DownloadState.ERROR, DownloadState.DONE, DownloadState.CANCELLED));
        downloadMediator.getDownloadList().addListEventListener(new LabelUpdateListListener());

        initializeListListeners();
    }

    private void initializeListListeners(){
        EventList<DownloadItem> doneList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new DownloadStateMatcher(DownloadState.DONE));
        EventList<DownloadItem> stalledList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new DownloadStateMatcher(DownloadState.STALLED));

        doneList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                clearFinishedNowButton.setEnabled(listChanges.getSourceList().size() > 0);
            }
        });
        
        stalledList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                fixStalledButton.setVisible(listChanges.getSourceList().size() != 0);                
            }
        });        
    }

    private void initializeMoreButton(){
        moreButton = new LimeComboBox();
        moreButton.setText(I18n.tr("Options"));
        
        comboBoxDecorator.decorateMiniComboBox(moreButton);
        
        moreButton.setFont(hyperlinkFont);
        moreButton.setIcon(moreButtonArrow);
        moreButton.setForeground(fixStalledButton.getForeground());
        ResizeUtils.forceHeight(moreButton, 16);
        
        downloadHeaderPopupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                downloadHeaderPopupMenu.removeAll();
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                downloadHeaderPopupMenu.removeAll();
            }
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                downloadHeaderPopupMenu.populate();
            }
        });
        
        moreButton.overrideMenu(downloadHeaderPopupMenu);
    }
        
    private class LabelUpdateListListener implements ListEventListener<DownloadItem> {       
        @Override
        public void listChanged(ListEvent<DownloadItem> listChanges) {
            if (activeList.size() > 0) {
                titleTextLabel.setText(I18n.tr("Downloads({0})", activeList.size()));
            } else {
                titleTextLabel.setText(I18n.tr("Downloads"));
            }
        }
    }
}
