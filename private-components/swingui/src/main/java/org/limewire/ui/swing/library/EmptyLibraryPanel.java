package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.event.ActionEvent;

import javax.swing.Action;
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
import org.limewire.ui.swing.painter.BackgroundMessagePainter;
import org.limewire.ui.swing.painter.GenericBarPainter;
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
    private Color innerNavTopGradient;
    @Resource
    private Color innerNavBottomGradient;
    @Resource
    private Color innerNavBorder;
    @Resource
    private Color mainPanelTopGradient;
    @Resource
    private Color mainPanelBottomGradient;
    
    private final Friend friend;
    private final FriendLibraryMediator mediator;
        
    @AssistedInject
    public EmptyLibraryPanel(@Assisted Friend friend,
            @Assisted FriendFileList friendFileList,
            @Assisted FriendLibraryMediator mediator, 
            @Assisted JXPanel messageComponent,
            LimeHeaderBarFactory headerBarFactory,
            ButtonDecorator buttonDecorator) {
        super(headerBarFactory);
  
        GuiUtils.assignResources(this);
        
        this.friend = friend;
        this.mediator = mediator;
        if(!friend.isAnonymous()) {
            addButtonToHeader(new ViewSharedLibraryAction(), buttonDecorator);
        }
        addDisposable((Disposable)messageComponent);
        createEmptyPanel(messageComponent);
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
    
    private void createEmptyPanel(JXPanel component) {
        JPanel p = new JPanel(new MigLayout("nocache, insets 0, gap 0, fill", "[125!][][fill]", "[fill]"));
        JXPanel leftPanel = new JXPanel();
        leftPanel.setBackgroundPainter(new GenericBarPainter<JXPanel>(
                new GradientPaint(0,0, innerNavTopGradient, 
                        0, 1, innerNavBottomGradient, false)));
        
        JXPanel rightPanel = new JXPanel();
        rightPanel.setBackgroundPainter(new GenericBarPainter<JXPanel>(
                new GradientPaint(0,0, mainPanelTopGradient,
                0, 1, mainPanelBottomGradient, false)));
        
        component.setOpaque(false);
        component.setBackgroundPainter(new BackgroundMessagePainter());
        p.add(component, "pos 0.5al 0.5al");
        
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
            putValue(Action.NAME, I18n.tr("Share with {0}", getShortPanelName()));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Show files you're sharing with {0}", getShortPanelName()));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            mediator.showSharingCard();
        }
    }
}
