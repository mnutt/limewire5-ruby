package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.dnd.LocalFileListTransferHandler;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Creates as little tables as possible. Expected to be used with a 
 * JXLayer disabling the main component
 */
public class EmptyLibraryPanel extends LibraryPanel {
    
    @Resource
    private Color navColor;
    @Resource
    private Color innerNavBorder;
    @Resource
    private Color backgroundColor;
    
    private final Friend friend;
    private final FriendLibraryMediator mediator;
        
    @AssistedInject
    public EmptyLibraryPanel(@Assisted Friend friend,
            @Assisted FriendFileList friendFileList,
            @Assisted FriendLibraryMediator mediator, 
            @Assisted Disposable messageComponent,
            @Assisted JComponent component,
            LimeHeaderBarFactory headerBarFactory,
            ButtonDecorator buttonDecorator) {
        super(headerBarFactory);
  
        GuiUtils.assignResources(this);
        
        this.friend = friend;
        this.mediator = mediator;
        if(!friend.isAnonymous()) {
            addButtonToHeader(new ViewSharedLibraryAction(), buttonDecorator);
        }
        addDisposable(messageComponent);
        createEmptyPanel(component);
        getHeaderPanel().setText(I18n.tr("Download from {0}", getFullPanelName()));
        setTransferHandler(new LocalFileListTransferHandler(friendFileList));
        
        enableFilterBox(false);
    }
    
    protected String getFullPanelName() {
        return friend.getRenderName();
    }
    
    protected String getShortPanelName() {
        return friend.getFirstName();
    }
    
    private void createEmptyPanel(JComponent component) {
        JPanel p = new JPanel(new MigLayout("insets 0, gap 0, fill", "[125!][][fill]", "[fill]"));
        JXPanel leftPanel = new JXPanel();
        leftPanel.setBackground(navColor);
        
        JXPanel rightPanel = new JXPanel();
        rightPanel.setBackground(backgroundColor);
        
        p.add(component, "pos 0.50al 0.4al");
        
        p.add(leftPanel, "grow");
        p.add(Line.createVerticalLine(innerNavBorder), "growy, width 1!");
        p.add(rightPanel, "grow");
       
        add(p, "grow, span");
    }
    
    @Override
    protected void addMainPanels() {
    }

    private class ViewSharedLibraryAction extends AbstractAction {

        public ViewSharedLibraryAction() {
            putValue(Action.NAME, I18n.tr("Share"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Share your files with {0}", friend.getRenderName()));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            mediator.showSharingCard();
        }
    }
}
