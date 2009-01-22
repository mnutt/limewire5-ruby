package org.limewire.ui.swing.library;

import java.util.Map;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.dnd.GhostDragGlassPane;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

import com.google.inject.Inject;

public class P2PNetworkSharingPanel extends SharingPanel {

    @Inject
    public P2PNetworkSharingPanel(
            LibraryManager libraryManager, 
            ShareListManager shareListManager,
            IconManager iconManager,
            CategoryIconManager categoryIconManager,
            LibraryTableFactory tableFactory,
            LimeHeaderBarFactory headerBarFactory,
            ButtonDecorator buttonDecorator,
            GhostDragGlassPane ghostPane) {
        super(libraryManager.getLibraryManagedList().getSwingModel(), 
                shareListManager.getGnutellaShareList(), categoryIconManager, 
                tableFactory, headerBarFactory, ghostPane, new P2PFriend());

        setInnerNavLayout(new MigLayout("insets 0, gap 0, fill, wrap, hidemode 3", "[138!]", ""));
        
        getHeaderPanel().setText(I18n.tr("Share with the P2P Network"));

        createMyCategories(libraryManager.getLibraryManagedList().getSwingModel(),
                           shareListManager.getGnutellaShareList());
        selectFirstVisible();
    }
    
    protected String getFullPanelName() {
        return I18n.tr("the P2P Network");
    }
    
    protected String getShortPanelName() {
        return I18n.tr("the P2P Network");
    } 
    
    private static class P2PFriend implements Friend {

        @Override
        public String getName() {
            return I18n.tr("the P2P Network");
        }
        @Override
        public String getId() {
            return Friend.P2P_FRIEND_ID;
        }
        
        @Override
        public String getFirstName() {return null;}
        @Override
        public Map<String, FriendPresence> getFriendPresences() {return null;}
        @Override
        public Network getNetwork() {return null;}
        @Override
        public String getRenderName() {return null;}
        @Override
        public boolean isAnonymous() {return false;}
        @Override
        public void setName(String name) {}
    }
}
