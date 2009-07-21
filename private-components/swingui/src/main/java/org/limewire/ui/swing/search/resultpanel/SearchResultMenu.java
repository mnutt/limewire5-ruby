package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.properties.FileInfoDialogFactory;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.search.BlockUserMenuFactory;
import org.limewire.ui.swing.search.RemoteHostMenuFactory;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;


/**
 * This class implements the popup menu that is displayed when the user 
 * right-clicks on a search result.
 */
public class SearchResultMenu extends JPopupMenu {
    
    /** Defines the available display types. */
    public enum ViewType {
        List,
        Table
    }
    
    /**
     * Constructs a SearchResultMenu using the specified download handler,
     * list of selected results, properties factory, and display type.
     */
    @Inject
    public SearchResultMenu(@Assisted final DownloadHandler downloadHandler,
        @Assisted final List<VisualSearchResult> selectedItems,
        final FileInfoDialogFactory fileInfoFactory,
        RemoteHostMenuFactory browseMenuFactory,
        BlockUserMenuFactory blockUserMenuFactory, final LibraryMediator libraryMediator,
        @Assisted ViewType viewType) {

        final VisualSearchResult firstItem = selectedItems.get(0);
        
        // Determine if download is enabled.
        boolean downloadEnabled = false;
        for (VisualSearchResult visualSearchResult : selectedItems) {
            if (visualSearchResult.getDownloadState() == BasicDownloadState.NOT_STARTED) {
                downloadEnabled = true;
                break;
            }
        }
        
        // Determine indicators to enable menu items.
        boolean locateInLibraryVisible = firstItem.getDownloadState() == BasicDownloadState.LIBRARY;
        boolean showHideSimilarFileVisible = selectedItems.size() == 1 && firstItem.getSimilarResults().size() > 0 && viewType == ViewType.List;
        boolean showHideSimilarFileEnabled = selectedItems.size() == 1 && firstItem.getDownloadState() == BasicDownloadState.NOT_STARTED;
        boolean viewFileInfoEnabled = selectedItems.size() == 1;
        boolean downloadAsVisible = selectedItems.size() == 1;

        // Add Download menu item.
        add(new AbstractAction(tr("Download")) {
            public void actionPerformed(ActionEvent e) {
                for (VisualSearchResult visualSearchResult : selectedItems) {
                    if (visualSearchResult.getDownloadState() == BasicDownloadState.NOT_STARTED) {
                        downloadHandler.download(visualSearchResult);
                    }
                }
            }
        }).setEnabled(downloadEnabled);
        
        // Add Download As menu item if visible.
        if (downloadAsVisible) {
            add(new AbstractAction(tr("Download As...")) {
                public void actionPerformed(ActionEvent e) {
                    for (VisualSearchResult visualSearchResult : selectedItems) {
                        if (visualSearchResult.getDownloadState() == BasicDownloadState.NOT_STARTED) {
                            // Create suggested file.  We don't specify a directory
                            // path so we can use the last input directory.  To get
                            // the default save directory, we could call
                            // SharingSettings.getSaveDirectory(fileName).
                            String fileName = visualSearchResult.getFileName();
                            File suggestedFile = new File(fileName);

                            // Prompt user for local file name.
                            File saveFile = FileChooser.getSaveAsFile(
                                    GuiUtils.getMainFrame(), tr("Download As"), 
                                    suggestedFile);

                            // Start download if not canceled.
                            if (saveFile != null) {
                                downloadHandler.download(visualSearchResult, saveFile);
                            }
                        }
                    }
                }
            }).setEnabled(downloadEnabled);
        }

        // Add Mark/Unmark as Spam menu item.
        add(new AbstractAction(firstItem.isSpam() ? tr("Unmark as Spam") : tr("Mark as Spam")) {
            public void actionPerformed(ActionEvent e) {
                boolean spam = !firstItem.isSpam();
                for (VisualSearchResult visualSearchResult : selectedItems) {
                    visualSearchResult.setSpam(spam);
                }
            }
        });

        addSeparator();
        
        // Add Locate in Library menu item if visible.
        if(locateInLibraryVisible){
            add(new AbstractAction(tr("Locate in Library")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    libraryMediator.selectInLibrary(firstItem.getUrn());
                }
            });
            addSeparator();
        }
        
        // Add optional item for Similar Files.
        if (showHideSimilarFileVisible) {
            add(new AbstractAction(firstItem.isChildrenVisible() ? tr("Hide Similar Files") : tr("Show Similar Files")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    firstItem.toggleChildrenVisibility();
                }
            }).setEnabled(showHideSimilarFileEnabled);
            
            addSeparator();
        }

        final List<RemoteHost> allHosts = new ArrayList<RemoteHost>();
        for (VisualSearchResult result : selectedItems) {
            allHosts.addAll(result.getSources());
        }
        
        if (allHosts.size() > 0) {
            // TODO: don't show browse menuItem in browse view
            add(browseMenuFactory.createBrowseMenu(allHosts));
            //addSeparator();

            JMenu blockUserMenu = blockUserMenuFactory.createSearchBlockMenu(allHosts, selectedItems);
            if (blockUserMenu != null) {
                add(blockUserMenu);
                addSeparator();
            }
        }

        // Add View File Info menu item.
        add(new AbstractAction(tr("View File Info...")) {
            public void actionPerformed(ActionEvent e) {
                fileInfoFactory.createFileInfoDialog(firstItem, FileInfoType.REMOTE_FILE);
            }
        }).setEnabled(viewFileInfoEnabled);
    }
    

}
