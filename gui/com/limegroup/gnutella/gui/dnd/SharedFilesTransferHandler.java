package com.limegroup.gnutella.gui.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.limewire.core.settings.QuestionsHandler;

import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.CheckBoxList;
import com.limegroup.gnutella.gui.CheckBoxListPanel;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.IconManager;
import com.limegroup.gnutella.gui.MessageService;
import com.limegroup.gnutella.gui.MultiLineLabel;
import com.limegroup.gnutella.gui.library.RecursiveSharingDialog;
import com.limegroup.gnutella.gui.library.RecursiveSharingDialog.State;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;
import com.limegroup.gnutella.library.FileManager;

/**
 * Handles local files being dropped on limewire by asking the user if
 * s/he wants to share them.
 */
public class SharedFilesTransferHandler extends LimeTransferHandler {

	@Override
	public boolean canImport(JComponent c, DataFlavor[] flavors, DropInfo ddi) {
		return canImport(c, flavors);
	}
	
	@Override
	public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
		return !DNDUtils.containsLibraryFlavors(transferFlavors)
			&& DNDUtils.containsFileFlavors(transferFlavors);
	}
	
	@Override
	public boolean importData(JComponent c, Transferable t, DropInfo ddi) {
		return importData(c, t);
	}
	
	@Override
	public boolean importData(JComponent comp, Transferable t) {
		if (!canImport(comp, t.getTransferDataFlavors()))
			return false;
		
		try {
			File[] files = DNDUtils.getFiles(t);
			if (files.length > 0) {
				return handleFiles(files);
			}
		} catch (UnsupportedFlavorException e) {
		} catch (IOException e) {
		}
		return false;
	}

	/**
	 * Returns true if files were shared
	 * @param files
	 * @return
	 */
	public static boolean handleFiles(final File[] files) {
	
		List<File> filteredFiles = filterOutSharedFiles(files);
		
		if (filteredFiles.size() == 0) {
			return false;
		}
		
		CheckBoxListPanel<File> listPanel = new CheckBoxListPanel<File>(filteredFiles, new FileTextProvider(), false);
		listPanel.getList().setVisibleRowCount(5);
		
		Object[] content = new Object[] {
				new MultiLineLabel(I18n.tr("You dropped the following files/folders on LimeWire, please select the ones you would like to share."), 400),
				Box.createVerticalStrut(ButtonRow.BUTTON_SEP),
				listPanel,
				Box.createVerticalStrut(ButtonRow.BUTTON_SEP),
				new MultiLineLabel(I18n.tr("All folders you select will also have their subfolders shared."), 400),
		};
		
		int response = JOptionPane.showConfirmDialog(MessageService
				.getParentComponent(), content, I18n
				.tr("Share Dropped Files?"),
				JOptionPane.OK_CANCEL_OPTION);
			
		if (response != JOptionPane.YES_OPTION) {
			return false;
		}
		
		final List<File> filesToShare = listPanel.getSelectedElements();
		
		if (filesToShare.size() == 0) {
			// user does not want to see this dialog again
			if (QuestionsHandler.HIDE_EMPTY_DROPPED_SHARE_DIALOG.getValue()) {
				return false;
			}
			
			Object[] options = { 
					I18n.tr("Yes, Try Again"),
					I18n.tr("No, Share Nothing")
			};
			
			JCheckBox showAgain = new JCheckBox(I18n.tr("Do not show this message again."));
			
			Object[] message = new Object[] {
					new MultiLineLabel(I18n.tr("You clicked OK but did not select any files to be shared. Would you like to try again?"), 400),
					Box.createVerticalStrut(ButtonRow.BUTTON_SEP),
					showAgain
			};
			
			response = JOptionPane.showOptionDialog(MessageService.getParentComponent(),
					message, I18n.tr("Warning"),
					JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
					null, options, options[0]);
			QuestionsHandler.HIDE_EMPTY_DROPPED_SHARE_DIALOG.setValue(showAgain.isSelected());
			if (response == 0) {
				return handleFiles(files);
			}
			else {
				return false;
			}
		}
		
		Set<File> dirs = new HashSet<File>();
		final Set<File> fileSet = new HashSet<File>();
		final FileManager fileManager = GuiCoreMediator.getFileManager();
		
		for (File file : filesToShare) {
			if (file.isDirectory()) {
//				if (fileManager.isFolderShareable(file, true)) {
					dirs.add(file);
//				}
			} else if (file.exists()) {
				fileSet.add(file);
			}
		}
		
		if (!dirs.isEmpty()) {
			final RecursiveSharingDialog dialog = new RecursiveSharingDialog(GUIMediator.getAppFrame(), dirs.toArray(new File[0]));
			if (dialog.showChooseDialog(MessageService.getParentComponent()) == State.OK) {
				BackgroundExecutorService.schedule(new Runnable() {
					public void run() {
//						fileManager.addSharedFolders(dialog.getRootsToShare(), dialog.getFoldersToExclude());
					}
				});
			}
		}
		
		BackgroundExecutorService.schedule(new Runnable() {
			public void run() {
				for (File file : fileSet) {
					fileManager.getGnutellaFileList().add(file);
				}
			}
		});
		GUIMediator.instance().setWindow(GUIMediator.Tabs.LIBRARY);
		return true;
	}
	
	private static List<File> filterOutSharedFiles(File[] files) {
		FileManager fileManager = GuiCoreMediator.getFileManager();
		ArrayList<File> list = new ArrayList<File>(files.length);
		for (File file : files) {
			if (!fileManager.getGnutellaFileList().contains(file)) {
//			        && !fileManager.isFolderShared(file)) {
				list.add(file);
			}
		}
		return list;
	}
	
	private static class FileTextProvider implements CheckBoxList.TextProvider<File> {
		
	    public String getText(File obj) {
			return obj.getName();
		}
		
		public String getToolTipText(File obj) {
			return obj.getAbsolutePath();
		}

		public Icon getIcon(File obj) {
			Icon icon = IconManager.instance().getIconForFile(obj);
			return icon != null ? icon : new GUIUtils.EmptyIcon("", 16, 16); 
		}
		
	}
	
}
