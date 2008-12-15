package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Creates as little tables as possible. Expected to be used with a 
 * JXLayer disabling the main component
 */
public class EmptyLibraryPanel extends LibraryPanel {
    
    private final Friend friend;
    private final FriendLibraryMediator mediator;
        
    @AssistedInject
    public EmptyLibraryPanel(@Assisted Friend friend,  
            @Assisted FriendLibraryMediator mediator, 
            @Assisted JComponent messageComponent,
            LimeHeaderBarFactory headerBarFactory,
            ButtonDecorator buttonDecorator) {
        super(headerBarFactory);
  
        this.friend = friend;
        this.mediator = mediator;
        addButtonToHeader(new ViewSharedLibraryAction(), buttonDecorator);
        createEmptyPanel(messageComponent);
        getHeaderPanel().setText(I18n.tr("Download from {0}", getFullPanelName()));
        
    }
    
    protected String getFullPanelName() {
        return friend.getRenderName();
    }
    
    protected String getShortPanelName() {
        return friend.getFirstName();
    }
    
    private void createEmptyPanel(JComponent component) {
        JPanel p = new JPanel(new MigLayout("insets 0, gap 0, fill"));
        p.setBackground(new Color(100,100,100,160));
        p.add(component, "alignx 50%, aligny 50%");
       
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
