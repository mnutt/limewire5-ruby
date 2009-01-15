package org.limewire.ui.swing.library.image;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import org.jdesktop.swingx.VerticalLayout;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.images.ImageList;
import org.limewire.ui.swing.images.ImageListModel;
import org.limewire.ui.swing.library.LibraryOperable;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GroupingList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;


public class LibraryImagePanel extends JPanel
    implements ListEventListener<List<LocalFileItem>>, Disposable, Scrollable, LibraryOperable<LocalFileItem> {
    
    private LibraryImageSubPanelFactory factory;
    private ShareWidget<File> shareWidget;
    
    private final EventList<LocalFileItem> currentEventList;

    private final GroupingList<LocalFileItem> groupingList;

    private final Comparator<LocalFileItem> groupingComparator = new Comparator<LocalFileItem>() {
        @Override
        public int compare(LocalFileItem o1, LocalFileItem o2) {
            if (o1.getFile() != null) {
                return (o2.getFile() != null) ? o1.getFile().getParent().compareTo(o2.getFile().getParent()) : 1;
            } else  {
                return (o2.getFile() != null) ? -1 : 0;
            }
        }
    };
    

    private final Map<File, LibraryImageSubPanel> panelMap;

    private final LocalFileList fileList;
    private final LocalFileList currentFriendFileList;
    
    
    private JScrollPane scrollPane;
    
    public LibraryImagePanel(String name, EventList<LocalFileItem> eventList, 
            LocalFileList fileList, JScrollPane scrollPane,
            LibraryImageSubPanelFactory factory,
            ShareWidget<File> shareWidget,
            LocalFileList selectedFriendList) {       
        super(new VerticalLayout());
        
        GuiUtils.assignResources(this); 
        
        this.fileList = fileList;
        this.currentEventList = eventList;
        this.scrollPane = scrollPane;
        this.factory = factory;
        this.shareWidget = shareWidget;
        this.currentFriendFileList = selectedFriendList;
        
        groupingList = GlazedListsFactory.groupingList(eventList, groupingComparator);
        
        groupingList.addListEventListener(this);

        panelMap = new ConcurrentHashMap<File, LibraryImageSubPanel>();
    
        initList();
    }
    
    public void dispose() {
        for(LibraryImageSubPanel subPanel : panelMap.values()){
            subPanel.dispose();
        }
        panelMap.clear();
        groupingList.removeListEventListener(this);
        groupingList.dispose();
    }

    //TODO: thread safety
    @Override
    public void listChanged(ListEvent<List<LocalFileItem>> listChanges) {
        while (listChanges.next()){
            if (listChanges.getType() == ListEvent.INSERT) {
                // INSERT adds sublist to the GroupingList - UPDATEs are also
                // fired for each file added to the sublist
                final File parent = getParentFolder(listChanges.getSourceList().get(listChanges.getIndex()).get(0));
                if (!panelMap.containsKey(parent)) {
                    final EventList<LocalFileItem> newList = GlazedListsFactory.filterList(currentEventList, new DirectoryMatcher(parent));
                    SwingUtils.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            createSubPanel(parent, newList);
                        }
                    });
                }
            }
        }       
    }
    
    private void initList() {
        for( List<LocalFileItem> fileItemList : groupingList) {
            final File parent = getParentFolder(fileItemList.get(0));
            final EventList<LocalFileItem> newList = GlazedListsFactory.filterList(currentEventList, new DirectoryMatcher(parent));
            createSubPanel(parent, newList);
        }
    }
    
    
    private void createSubPanel(File parent, EventList<LocalFileItem> list){
        LibraryImageSubPanel subPanel;
        if(shareWidget != null )
            subPanel = factory.createMyLibraryImageSubPanel(parent, list, fileList, shareWidget);
        else {
            subPanel = factory.createSharingLibraryImageSubPanel(parent, list, fileList, currentFriendFileList);
        }
        panelMap.put(parent, subPanel);
        add(subPanel);
    }
        
    private static File getParentFolder(LocalFileItem localFileItem){
        return localFileItem.isIncomplete() ? SharingSettings.INCOMPLETE_DIRECTORY.getValue() : 
            ((localFileItem.getFile() == null) ? SharingSettings.INCOMPLETE_DIRECTORY.getValue() : localFileItem.getFile().getParentFile());
    }
    
    private static class DirectoryMatcher implements Matcher<LocalFileItem>{
        
        private File parentDirectory;

        public DirectoryMatcher(File parentDirectory){
            this.parentDirectory = parentDirectory;
        }

        @Override
        public boolean matches(LocalFileItem item) {
            return getParentFolder(item).equals(parentDirectory);
        }
        
    }
    
    @Override
    public void selectAndScrollTo(File file) {
        for (LibraryImageSubPanel subPanel : panelMap.values()) {
            ImageListModel model = subPanel.getModel();
            for (int x=0; x<model.getSize(); x++) {
                LocalFileItem item = model.getFileItem(x);
                if (file.equals(item.getFile())) {
                    scrollToImageInSubPanel(subPanel, x);
                    break;
                }
            }
        }
    }
    
    @Override
    public void selectAndScrollTo(URN urn) {
        for (LibraryImageSubPanel subPanel : panelMap.values()) {
            ImageListModel model = subPanel.getModel();
            for (int x=0; x<model.getSize(); x++) {
                LocalFileItem item = model.getFileItem(x);
                if (urn.equals(item.getUrn())) {
                    scrollToImageInSubPanel(subPanel, x);
                    break;
                }
            }
        }
    }

    private void scrollToImageInSubPanel(LibraryImageSubPanel subPanel, int index) {
        ImageList imageList = subPanel.getImageList();
        imageList.setSelectedIndex(index);
        Rectangle rect = imageList.getCellBounds(index, index);
        subPanel.scrollRectToVisible(rect);
    }
    
    /**
     * Overrides getPreferredSize. getPreferredSize is used by scrollable to 
     * determine how big to make the scrollableViewPort. Uses the parent 
     * component to set the appropriate size thats currently visible. This
     * prevents the image list from growing too wide and never shrinking again.
     */
    @Override
    public Dimension getPreferredSize() {
        //ensure viewport is filled so dnd will work
        Dimension dimension = super.getPreferredSize();
        if (getParent() == null)
            return dimension;
        if (dimension.height > scrollPane.getSize().height){
            return new Dimension(scrollPane.getWidth() -20, dimension.height);
        } else {
            return scrollPane.getSize(); 
        }
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return getPosition(visibleRect, orientation, direction);
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return getPosition(visibleRect, orientation, direction);
    }
    
    /**
     * Scrolls to the appropriate location based on the height of a thumbnail image.
     */
    private int getPosition(Rectangle visibleRect, int orientation, int direction) {
        int currentPosition = 0;
        if (orientation == SwingConstants.HORIZONTAL)
            currentPosition = visibleRect.x;
        else
            currentPosition = visibleRect.y;
    
        //TODO: read this from ImageList again
        int height = 134;//imageList.getList().getFixedCellHeight();
        
        if (direction < 0) {
            int newPosition = currentPosition - (currentPosition / height) * height;
            return (newPosition == 0) ? height : newPosition;
        } else {
            return ((currentPosition / height) + 1) * height - currentPosition;
        }
    }

   @Override
    public File getNextItem(File file) {
        throw new IllegalStateException("Image library traversal not available");
    }

    @Override
    public File getPreviousItem(File file) {
        throw new IllegalStateException("Image library traversal not available");
    }

    @Override
    public List<LocalFileItem> getSelectedItems() {
        List<LocalFileItem> selectionList = new ArrayList<LocalFileItem>();
        for (LibraryImageSubPanel subPanel : panelMap.values()) {
            selectionList.addAll(subPanel.getSelectedItems());
        }
        return selectionList;
    }

    @Override
    public void selectAll() {
        for (LibraryImageSubPanel subPanel : panelMap.values()) {
            ImageList imageList = subPanel.getImageList();
            if (imageList.getElementCount() > 0) {
                imageList.setSelectionInterval(0, imageList.getElementCount() - 1);
            }
        }
    }

}