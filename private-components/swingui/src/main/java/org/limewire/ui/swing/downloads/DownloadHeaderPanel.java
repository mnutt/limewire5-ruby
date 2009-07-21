package org.limewire.ui.swing.downloads;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.settings.SharingSettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.dock.DockIcon;
import org.limewire.ui.swing.dock.DockIconFactory;
import org.limewire.ui.swing.downloads.DownloadMediator.SortOrder;
import org.limewire.ui.swing.downloads.table.DownloadStateExcluder;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.event.OptionsDisplayEvent;
import org.limewire.ui.swing.options.OptionsDialog;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;

import com.google.inject.Inject;

public class DownloadHeaderPanel extends JXPanel {

    @Resource
    private Color topBorderColor;

    @Resource
    private Color outerBorderColor;

    @Resource
    private Color topGradientColor;

    @Resource
    private Color bottomGradientColor;

    @Resource
    private Icon moreButtonArrow;
    @Resource
    private Icon moreButtonArrowDownState;
    @Resource
    private Font hyperlinkFont;


    @Resource
    private Icon upArrow;
    @Resource
    private Icon downArrow;

    private final DownloadMediator downloadMediator;    

    private final DockIcon dock;

    private HyperlinkButton moreButton;    
    private long menuClosedDelayTime;
    private Color moreButtonDefaultForeground;

    private JXButton clearFinishedNowButton;
    private JLabel clearFinishedLabel;

    private HyperlinkButton fixStalledButton;

    private JCheckBoxMenuItem clearFinishedCheckBox;

    private JXLabel titleTextLabel;
    private AbstractButton isDescending;
    
    
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
    
    private final AbstractDownloadsAction fixStalledAction = new AbstractDownloadsAction(I18n.tr("Fix Stalled")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            downloadMediator.fixStalled();
        }
    };
   
    private final AbstractDownloadsAction clearFinishedAction = new AbstractDownloadsAction(I18n.tr("Clear When Finished")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            SharingSettings.CLEAR_DOWNLOAD.setValue(clearFinishedCheckBox.isSelected());
            if (clearFinishedCheckBox.isSelected()){
                downloadMediator.clearFinished();
            }
        }
    };

    
    private final AbstractDownloadsAction cancelStallededAction = new AbstractDownloadsAction(I18n.tr("All Stalled")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (confirmCancellation(I18n.tr("Cancel all stalled downloads?"))) {
                downloadMediator.cancelStalled();
            }
        }
    };
    
    private final AbstractDownloadsAction cancelErrorAction = new AbstractDownloadsAction(I18n.tr("All Error")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (confirmCancellation(I18n.tr("Cancel all error downloads?"))) {
                downloadMediator.cancelError();
            }
        }
    };
    
    private final AbstractDownloadsAction cancelAllAction = new AbstractDownloadsAction(I18n.tr("All Downloads")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (confirmCancellation(I18n.tr("Cancel all downloads?"))) {
                downloadMediator.cancelAll();
            }
        }
    };
    
    private final Action statusSortAction = new SortAction(I18n.tr("Status"), SortOrder.STATUS);  
    private final Action orderSortAction = new SortAction(I18n.tr("Order Added"), SortOrder.ORDER_ADDED);  
    private final Action nameSortAction = new SortAction(I18n.tr("Name"), SortOrder.NAME);
    private final Action progressSortAction = new SortAction(I18n.tr("Progress"), SortOrder.PROGRESS);  
    private final Action timeRemainingSortAction = new SortAction(I18n.tr("Time Left"), SortOrder.TIME_REMAINING);
    private final Action speedSortAction = new SortAction(I18n.tr("Speed"), SortOrder.SPEED);
    private final Action fileTypeSortAction = new SortAction(I18n.tr("File Type"), SortOrder.FILE_TYPE);
    private final Action extensionSortAction = new SortAction(I18n.tr("File Extension"), SortOrder.EXTENSION);
     
    private final EventList<DownloadItem> activeList;
    
    private final Action downloadSettingsAction = new AbstractAction(I18n.tr("Download Options...")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            new OptionsDisplayEvent(OptionsDialog.DOWNLOADS).publish();
        }
    };

    
    @Inject
    public DownloadHeaderPanel(DownloadMediator downloadMediator, DockIconFactory dockIconFactory) {
        GuiUtils.assignResources(this);
        
        this.downloadMediator = downloadMediator;
        dock = dockIconFactory.createDockIcon();   
        

        activeList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new DownloadStateExcluder(DownloadState.ERROR, DownloadState.DONE, DownloadState.CANCELLED));

        initialize();
    }
    
    private void initialize(){
        initializeComponents();        
        layoutComponents();        
        setupPainter();
    }
  

    private void initializeComponents(){        
        clearFinishedNowButton = new HyperlinkButton(clearFinishedNowAction);
        clearFinishedNowButton.setFont(hyperlinkFont);
        clearFinishedNowButton.setVisible(false);
        clearFinishedLabel = new JLabel(clearFinishedNowButton.getText());
        clearFinishedLabel.setFont(hyperlinkFont);
        clearFinishedLabel.setFont(clearFinishedNowButton.getFont());
        clearFinishedLabel.setPreferredSize(clearFinishedNowButton.getPreferredSize());
        clearFinishedLabel.setEnabled(false);

        fixStalledButton = new HyperlinkButton(fixStalledAction);
        fixStalledButton.setFont(hyperlinkFont);
        fixStalledButton.setVisible(false);

        initializeMoreButton();
        
        titleTextLabel = new JXLabel(I18n.tr("Downloads"));
    }
    
    private void layoutComponents(){
        setLayout(new MigLayout("insets 2 0 2 0, gap 0, novisualpadding, fill"));
        add(titleTextLabel, "gapbefore 5, push");   
        add(fixStalledButton, "gapafter 5, hidemode 3");  
        add(clearFinishedNowButton, "gapafter 5, hidemode 3");
        add(clearFinishedLabel, "gapafter 5, hidemode 3");
        add(moreButton, "gapafter 5");  
    }
    
    private void setupPainter() {
        setOpaque(false); 
        setBackgroundPainter(new DownloadHeaderBackgroundPainter());
    }
    
    @Inject
    public void register(){
        downloadMediator.getDownloadList().addListEventListener(new LabelUpdateListListener());
        initializeListListeners();
    }

    private void initializeListListeners(){
        EventList<DownloadItem> pausableList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new PausableMatcher());
        EventList<DownloadItem> resumableList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new ResumableMatcher());
        EventList<DownloadItem> doneList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new DownloadStateMatcher(DownloadState.DONE));
        EventList<DownloadItem> stalledList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new DownloadStateMatcher(DownloadState.STALLED));
        EventList<DownloadItem> errorList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new DownloadStateMatcher(DownloadState.ERROR));
        
        pausableList.addListEventListener(new ActionEnablementListListener(pauseAction));
        resumableList.addListEventListener(new ActionEnablementListListener(resumeAction));        
        errorList.addListEventListener(new ActionEnablementListListener(cancelErrorAction));        
        downloadMediator.getDownloadList().addListEventListener(new ActionEnablementListListener(cancelAllAction));    
        
        doneList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                clearFinishedNowButton.setVisible(listChanges.getSourceList().size()>0);
                clearFinishedLabel.setVisible(!clearFinishedNowButton.isVisible());
            }
        });
        
        stalledList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                fixStalledButton.setVisible(listChanges.getSourceList().size() != 0);                
                cancelStallededAction.setEnablementFromDownloadSize(listChanges.getSourceList().size());
            }
        });        
    }

    private void initializeMoreButton(){
        pauseAction.setEnabled(false);
        resumeAction.setEnabled(false);
        cancelErrorAction.setEnabled(false);
        cancelAllAction.setEnabled(false);
        cancelStallededAction.setEnabled(false);
        
        clearFinishedCheckBox = new JCheckBoxMenuItem(clearFinishedAction);

        clearFinishedCheckBox.setSelected(SharingSettings.CLEAR_DOWNLOAD.getValue());
        SharingSettings.CLEAR_DOWNLOAD.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        clearFinishedCheckBox
                                .setSelected(SharingSettings.CLEAR_DOWNLOAD.getValue());
                    }
                });
            }
        });

        final JPopupMenu menu = new JPopupMenu();    
        menu.add(pauseAction);
        menu.add(resumeAction);
        menu.add(createCancelSubMenu());
        menu.addSeparator();
        menu.add(createSortSubMenu());
        menu.addSeparator();
        menu.add(clearFinishedCheckBox);
        menu.addSeparator();
        menu.add(downloadSettingsAction);

        moreButton = new HyperlinkButton(I18n.tr("Options"));
        moreButton.setFont(hyperlinkFont);
        moreButtonDefaultForeground = moreButton.getForeground();
        moreButton.setIcon(moreButtonArrow);
        moreButton.setHorizontalTextPosition(SwingConstants.LEFT);
        moreButton.setFocusPainted(false);
        moreButton.setBorder(BorderFactory.createEmptyBorder(1,6,1,6));
        moreButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!menu.isShowing() && System.currentTimeMillis() - menuClosedDelayTime > 200){
                    menu.show(moreButton, 0, moreButton.getHeight());
                }
            }      
        });      
        moreButton.setBackgroundPainter(new Painter<JXButton>() {
            @Override
            public void paint(Graphics2D g, JXButton object, int width, int height) {
                if (menu.isShowing()) {
                    g = (Graphics2D)g.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(Color.BLACK);
                    g.fillRoundRect(0, 0, width, height, 10, 10);
                    g.dispose();
                }
            }
        });
        menu.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                cancel();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                cancel();
            }

            private void cancel() {
                menuClosedDelayTime = System.currentTimeMillis();
                moreButton.setNormalForeground(moreButtonDefaultForeground);
                moreButton.setRolloverForeground(moreButtonDefaultForeground);
                moreButton.setIcon(moreButtonArrow);
            }

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                moreButton.setNormalForeground(Color.WHITE);
                moreButton.setRolloverForeground(Color.WHITE);
                moreButton.setIcon(moreButtonArrowDownState);

            }
        });
    }
    
    private JMenu createCancelSubMenu(){
        JMenu cancelSubMenu = new JMenu(I18n.tr("Cancel"));
        
        cancelSubMenu.add(cancelStallededAction);
        cancelSubMenu.add(cancelErrorAction);
        cancelSubMenu.add(cancelAllAction);

        return cancelSubMenu;
    }

    private JMenu createSortSubMenu(){
        JMenu sortSubMenu = new JMenu(I18n.tr("Sort by"));
        
        JCheckBoxMenuItem orderAdded = new JCheckBoxMenuItem(orderSortAction);
        JCheckBoxMenuItem name = new JCheckBoxMenuItem(nameSortAction);
        JCheckBoxMenuItem progress = new JCheckBoxMenuItem(progressSortAction);
        JCheckBoxMenuItem timeRemaining = new JCheckBoxMenuItem(timeRemainingSortAction);
        JCheckBoxMenuItem speed = new JCheckBoxMenuItem(speedSortAction);
        JCheckBoxMenuItem status = new JCheckBoxMenuItem(statusSortAction);
        JCheckBoxMenuItem fileType = new JCheckBoxMenuItem(fileTypeSortAction);
        JCheckBoxMenuItem extension = new JCheckBoxMenuItem(extensionSortAction);        

        final UsefulButtonGroup sortButtonGroup = new UsefulButtonGroup();
        sortButtonGroup.add(orderAdded);
        sortButtonGroup.add(name);
        sortButtonGroup.add(progress);
        sortButtonGroup.add(timeRemaining);
        sortButtonGroup.add(speed);
        sortButtonGroup.add(status);
        sortButtonGroup.add(fileType);
        sortButtonGroup.add(extension);
        
        isDescending = new JMenuItem(new AbstractAction(I18n.tr("Reverse Order")){
            {
                putValue(Action.SELECTED_KEY, true);
                putValue(Action.SMALL_ICON, downArrow);
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                putValue(Action.NAME, isDescending.isSelected()? I18n.tr("Reverse Order") : I18n.tr("Reverse Order"));
                putValue(Action.SMALL_ICON, isDescending.isSelected()? downArrow : upArrow);
                sortButtonGroup.getSelectedButton().getAction().actionPerformed(null);
            }            
        });
        isDescending.setModel(new JToggleButton.ToggleButtonModel());
        isDescending.setBorderPainted(false);
        isDescending.setContentAreaFilled(false);
        isDescending.setOpaque(false);
       // isDescending.setSelected(true);
        
        orderAdded.setSelected(true);
        
        sortSubMenu.add(orderAdded);
        sortSubMenu.add(name);
        sortSubMenu.add(progress);
        sortSubMenu.add(timeRemaining);
        sortSubMenu.add(speed);
        sortSubMenu.add(status);
        sortSubMenu.add(fileType);
        sortSubMenu.add(extension);
        sortSubMenu.addSeparator();
        sortSubMenu.add(isDescending);
        
        return sortSubMenu;
    }
    
    private boolean confirmCancellation(String message){
        return FocusJOptionPane.showConfirmDialog(GuiUtils.getMainFrame(), message, I18n.tr("Cancel"),
                JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE) == JOptionPane.YES_OPTION;
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
    
    /**
     * Painter for the background of the header
     */
    private class DownloadHeaderBackgroundPainter extends AbstractPainter<JXPanel> {

        private RectanglePainter<JXPanel> painter;

        public DownloadHeaderBackgroundPainter() {
            painter = new RectanglePainter<JXPanel>();
            painter.setFillPaint(new GradientPaint(0, 0, topGradientColor, 0, 1,
                    bottomGradientColor, false));
            painter.setFillVertical(true);
            painter.setFillHorizontal(true);
            painter.setPaintStretched(true);
            painter.setBorderPaint(null);
        }

        @Override
        protected void doPaint(Graphics2D g, JXPanel object, int width, int height) {
            painter.paint(g, object, width, height);

            // paint the top border
            g.setColor(outerBorderColor);
            g.drawLine(0, 0, width, 0);
            g.setColor(topBorderColor);
            g.drawLine(0, 1, width, 1);

            // paint the bottom border
            g.setColor(outerBorderColor);
            g.drawLine(0, height - 1, width, height - 1);
        }
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
    
    private class SortAction extends AbstractAction{
        
        private final SortOrder order;

        public SortAction(String title, SortOrder order){
            super(title);
            this.order = order;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            downloadMediator.setSortOrder(order, !isDescending.isSelected());
        }
    }; 
    
    private static class ActionEnablementListListener implements ListEventListener<DownloadItem> {
        private AbstractDownloadsAction action;
        public ActionEnablementListListener(AbstractDownloadsAction action){
            this.action = action;
        }
        @Override
        public void listChanged(ListEvent<DownloadItem> listChanges) {
            action.setEnablementFromDownloadSize(listChanges.getSourceList().size());
        }                
    }
    
    private static class UsefulButtonGroup extends ButtonGroup {
        
        public AbstractButton getSelectedButton(){
            ButtonModel selectedModel = getSelection();
            for (AbstractButton button : buttons){
                if(button.getModel() == selectedModel){
                    return button;
                }
            }
            //nothing found
            return null;
        }
    }
}
