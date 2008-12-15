package com.limegroup.gnutella.gui.download;

import java.awt.Component;
import java.awt.Dialog;
import java.io.File;
import java.text.MessageFormat;

import javax.swing.JOptionPane;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.settings.QuestionsHandler;
import org.limewire.i18n.I18nMarker;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.gui.FileChooserHandler;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.MessageService;
import com.limegroup.gnutella.library.FileDesc;

/**
 * Static helper class that kicks off downloads handling all the necessary
 * consistency check and showing appropriate error/warning dialogs.
 */
public class DownloaderUtils {

	/**
	 * Tries to create a downloader for a factory performing the following
	 * consistency checks beforehand:
	 * <ul>
	 * <li>{@link #isAlreadyDownloading(GuiDownloaderFactory)}</li>
	 * <li>{@link #isSaveLocationTaken(GuiDownloaderFactory)}</li>
	 * <li>if the proposed save location is not taken
	 * {@link #continueWithOrWithoutHashConflict(GuiDownloaderFactory)} is
	 * performed</li>
	 * </ul>
	 * 
	 * @param factory The downloader factory to create the downloader
	 * @return <code>null</code> if there is another download for the same
	 *         hash, or incomplete file, or the user cancelled the download at
	 *         some point.
	 */
	public static Downloader createDownloader(GuiDownloaderFactory factory) {

		// check for already downloading conflicts
		if (isAlreadyDownloading(factory)) {
			return null;
		}

		// check for file name conflicts
		if (!isSaveLocationTaken(factory)) {

			// check for hash conflicts
			if (!continueWithOrWithoutHashConflict(factory)) {
				return null;
			}
		}

		// try to start download
		return createDownloader(factory, false);
	}

	/**
	 * Tries to create a downloader for a factory asking the user for a save
	 * location for the download.
	 * <p>
	 * Performs the following consistency checks:
	 * <ul>
	 * <li>{@link #isAlreadyDownloading(GuiDownloaderFactory)}</li>
	 * <li>{@link #continueWithOrWithoutHashConflict(GuiDownloaderFactory)}</li>
	 * </ul>
	 * 
	 * @param factory creates the downloader
	 * @return <code>null</code> if there is another download for the same
	 *         hash or incomplete file, or the user cancelled somewhere along
	 *         the way.
	 */
	public static Downloader createDownloaderAs(GuiDownloaderFactory factory) {

		// check for already downloading conflicts
		if (isAlreadyDownloading(factory)) {
			return null;
		}

		// check for hash conflicts
		if (!continueWithOrWithoutHashConflict(factory)) {
			return null;
		}

		File file = showFileChooser(factory, MessageService
				.getParentComponent());
		if (file == null) {
			return null;
		}
		factory.setSaveFile(file);

		// OSX's FileDialog box already prompts the user that they're
		// going to be overwriting a file, so we don't need to do that
		// particular check again.
		return createDownloader(factory, OSUtils.isAnyMac());
	}
	
	/**
	 * Tries to create a downloader for a magnet.
	 * <p>
	 * The magnet may also be {@link MagnetOptions#isDownloadable() not valid 
	 * for downloading}, then a warning is displayed.
	 * @param magnet information about the magnet link
	 * @return <code>null</code> if the magnet is is invalid or the user 
	 * cancelled the process
	 */
	public static Downloader createDownloader(MagnetOptions magnet) {
		String msg = magnet.getErrorMessage();
		if (!magnet.isDownloadable()) {
			if (msg == null) {
				msg = magnet.toString();
			}
			// show warning and return
			GUIMediator.showError(I18n.tr("Could not process bad MAGNET link {0}", msg));
			return null;
		}
		if (msg != null) {
			// show warning but proceed
			GUIMediator.showWarning(I18n.tr("One or more URLs in the MAGNET link were invalid. Your file may not download correctly."));
		}
		MagnetDownloaderFactory factory = new MagnetDownloaderFactory(magnet);
		if (magnet.getDisplayName() == null) {
			Downloader dl = createDownloaderAs(factory);
			if (dl != null && magnet.isHashOnly()) {
				GUIMediator.showError(I18n.tr("LimeWire may not be able to start the download you have selected unless you start a search for it."));
			}
			return dl;
		}
		else {
			return createDownloader(factory);
		}
	}

	/**
	 * Tries to create a downloader from a factory.
	 * <p>
	 * If the {@link GuiDownloaderFactory#createDownloader(boolean)} throws an
	 * exception, {@link DownloaderDialog#handle(GuiDownloaderFactory, 
	 * SaveLocationException)} is called to handle it.
	 * <p>
	 * If the process was successful, the final file is shared individually if
	 * it's not in a shared directory.
	 * 
	 * @param factory creates the downloader
	 * @param overwrite whether or not to overwrite an existing file at the
     * given location
	 * @return <code>null</code> if the user cancelled at some point
	 */
	public static Downloader createDownloader(GuiDownloaderFactory factory,
											  boolean overwrite) {
		try {
			return factory.createDownloader(overwrite);
		} catch (SaveLocationException sle) {
			return DownloaderDialog.handle(factory, sle);
		}
	}

	/**
	 * Checks if there is a conflicting download already running with the same
	 * hash or incomplete file name, shows a notification dialog and returns
	 * true.
	 * 
	 * @param factory creates the downloader
	 * @return true if there is a download already taking place
	 */
	public static boolean isAlreadyDownloading(GuiDownloaderFactory factory) {
		if (GuiCoreMediator.getDownloadManager().conflicts(factory.getURN(),
				(int)factory.getFileSize(), factory.getSaveFile())) {
			showIsAlreadyDownloadingWarning(factory);
			return true;
		}
		return false;
	}
	
	public static void showIsAlreadyDownloadingWarning(GuiDownloaderFactory factory) {
		GUIMediator.showError(
				I18n.tr("You are already downloading this file to \"{0}\".", factory.getSaveFile()),
				QuestionsHandler.ALREADY_DOWNLOADING);
	}

	/**
	 * Returns a non-incomplete FileDesc for the urn or null.
	 * 
	 * @param urn URN of a file 
	 * @return FileDesc of a file in the file manager
	 */
	public static FileDesc getFromLibrary(URN urn) {
//		if (urn == null) {
			return null;
//		}
//		FileDesc desc = GuiCoreMediator.getFileManager().getManagedFileList().getFileDesc(urn);
//		return (GuiCoreMediator.getFileManager().getIncompleteFileList().contains(desc)) ? null : desc;
	}

	/**
	 * Checks if there is already a file in the library with the same urn and
	 * shows a dialog to user if (s)he wants to continue anyway.
	 * 
	 * @param factory creates the downloader
	 * @return <code>true></code> if there is no conflict or the user wants to
	 *         continue anyway
	 */
	public static boolean continueWithOrWithoutHashConflict(
			GuiDownloaderFactory factory) {
		FileDesc desc = getFromLibrary(factory.getURN());
		if (desc != null) {
			return showHashConflict(desc);
		}
		return true;
	}

	private static boolean showHashConflict(FileDesc desc) {
		String message = MessageFormat.format(I18n
				.tr("You already have the exact same file at {0}"),
				new Object[] { desc.getFile() });
		String question = I18n
				.tr("Do you want to continue downloading anyway?");
		String continueLabel = I18n
				.tr("Continue");

		String[] content = new String[] { message, question };

		JOptionPane pane = new JOptionPane(
				content,
				JOptionPane.QUESTION_MESSAGE,
				JOptionPane.YES_NO_OPTION,
				null,
				new String[] {
						continueLabel,
						I18n.tr("Cancel") }
				);
		Dialog dialog = pane.createDialog(MessageService.getParentComponent(),
							I18n.tr("Same File Already In Library"));
		dialog.setVisible(true);
		return continueLabel.equals(pane.getValue());
	}

	/**
	 * Shows a filechooser for selecting a save location for a downloader.
	 * 
	 * @param factory creates the downloader
	 * @param c component the dialog is centered on
	 * @return the file or <code>null</code> when the user cancelled the
     *         dialog
	 */
	public static File showFileChooser(GuiDownloaderFactory factory, Component c) {
		return FileChooserHandler.getSaveAsFile(c, I18nMarker.marktr("Save Download As"),
				factory.getSaveFile());
	}

	/**
	 * Checks if the final save location already exists or is taken by another
	 * download.
	 * 
	 * @param factory creates the downloader
	 * @return true if the save file exists or will exist
	 */
	private static boolean isSaveLocationTaken(GuiDownloaderFactory factory) {
		return factory.getSaveFile().exists()
				|| GuiCoreMediator.getDownloadManager().isSaveLocationTaken(
						factory.getSaveFile());
	}

}
