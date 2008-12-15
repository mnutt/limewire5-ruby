package com.limegroup.gnutella.gui.options.panes;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.limewire.util.FileUtils;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.actions.RemoveSharedDirectoryAction;
import com.limegroup.gnutella.gui.actions.SelectSharedDirectoryAction;
import com.limegroup.gnutella.gui.library.RecursiveSharingPanel;

/**
 * This class defines the panel in the options window that allows the user
 * to change the directory that are shared.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
@SuppressWarnings("deprecation")
public final class SharedDirPaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Shared Folders");
    
    public final static String LABEL = I18n.tr("You can choose the folders for sharing files. Files in these folders are displayed in the library.");

	private final RecursiveSharingPanel sharingPanel = new RecursiveSharingPanel();

	private Set<File> initialFoldersToShare;
	
	private Set<File> initialFoldersToExclude;
	
	/**
	 * The constructor constructs all of the elements of this 
	 * <tt>AbstractPaneItem</tt>.
	 *
	 * @param key the key for this <tt>AbstractPaneItem</tt> that the
	 *            superclass uses to generate strings
	 */
	public SharedDirPaneItem() {
	    super(TITLE, LABEL);
	    
		sharingPanel.setFileFilter(new FileFilter() {
//		    private final FileManager fileManager = GuiCoreMediator.getFileManager();
            public boolean accept(File pathname) {
                return true;
//                return fileManager.isFolderShareable(pathname, false);
            }
		    
		});
		sharingPanel.getTree().setRootVisible(false);
		sharingPanel.getTree().setShowsRootHandles(true);
		
		JPanel buttonPanel = new JPanel(new BorderLayout());
		buttonPanel.setBorder(new EmptyBorder(0, 4, 0, 0));
		JPanel buttons = new JPanel(new BorderLayout());
		buttons.add(new JButton(new SelectSharedDirectoryAction(sharingPanel, sharingPanel)), BorderLayout.NORTH);
		buttons.add(Box.createVerticalStrut(4), BorderLayout.CENTER);
		buttons.add(new JButton(new RemoveSharedDirectoryAction(sharingPanel)), BorderLayout.SOUTH);
		buttonPanel.add(buttons, BorderLayout.NORTH);
		sharingPanel.addEastPanel(buttonPanel);		
		add(sharingPanel);
	}

    /**
     * Adds a directory to the internal list, resetting 'dirty' only if it wasn't
     * dirty already.
     */
    void addAndKeepDirtyStatus(Set<File> foldersToShare, Set<File> foldersToExclude) {
        for (File folder : foldersToShare) {
            sharingPanel.addRoot(folder);
        }
        sharingPanel.addFoldersToExclude(foldersToExclude);
    }
    
    boolean isAlreadyGoingToBeShared(File dir) {
        if (sharingPanel.getFoldersToExclude().contains(dir)) {
            return false;
        }
        for (File folder : sharingPanel.getRootsToShare()) {
            if (FileUtils.isAncestor(folder, dir)) {
                return true;
            }
        }
        return false;
    }
        
	/**
	 * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
	 *
	 * Sets the options for the fields in this <tt>PaneItem</tt> when the 
	 * window is shown.
	 */
	@SuppressWarnings("deprecation")
    @Override
    public void initOptions() {
		File[] dirs = null; //OldLibrarySettings.DIRECTORIES_TO_SHARE.getValueAsArray();
		initialFoldersToShare = new HashSet<File>(Arrays.asList(dirs));
//		initialFoldersToExclude = GuiCoreMediator.getFileManager().getFolderNotToShare();
		sharingPanel.setRoots(dirs);
		sharingPanel.setFoldersToExclude(initialFoldersToExclude);
	}
	
	/**
	 * Gets all folders to share.
	 */
	public Set<File> getDirectoriesToShare() {
	    return sharingPanel.getRootsToShare();
	}
	
	/**
	 * Returns the folders to exclude. 
	 */
	public Set<File> getDirectorieToExclude() {
	    return sharingPanel.getFoldersToExclude();
	}

	/**
	 * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
	 *
     * This makes sure that the shared directories have, in fact, changed to
     * make sure that we don't load the <tt>FileManager</tt> twice.  This is
     * particularly relevant to the case where the save directory has changed,
     * in which case we only want to reload the <tt>FileManager</tt> once for 
     * any changes.<p>
     * 
	 * Applies the options currently set in this window, displaying an
	 * error message to the user if a setting could not be applied.
	 *
	 * @throws <tt>IOException</tt> if the options could not be applied 
     *  for some reason
	 */
	@Override
    public boolean applyOptions() throws IOException {

//	    // the actual applying of shared folders is done in OptionsPaneManager,
	    // since it needs to be _after_ everything else is done.
        return false;
	}
	
	public boolean isDirty() {
	    return !initialFoldersToShare.equals(sharingPanel.getRootsToShare())
	    || !initialFoldersToExclude.equals(sharingPanel.getFoldersToExclude());
    }
	
	public void resetDirtyState() {
	    initialFoldersToShare = sharingPanel.getRootsToShare();
	    initialFoldersToExclude = sharingPanel.getFoldersToExclude();
	}
}





