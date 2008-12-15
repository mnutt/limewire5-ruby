package com.limegroup.gnutella.gui.util;

import java.awt.Cursor;
import java.io.File;

import org.limewire.core.settings.QuestionsHandler;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;

import foxtrot.Job;
import foxtrot.Worker;

/**
 * Static utility class that handles launching of downloaders and
 * displaying error messages.
 */
public class GUILauncher {

	/**
	 * Provides a downloader or a file that should be launched.
	 */
	public interface LaunchableProvider {
		/**
		 * Can return if only a file is available
		 */
		Downloader getDownloader();
		/**
		 * Can return null if only a downloader is avaialable 
		 */
		File getFile();
	}
	
	/**
	 * Launches an array of <code>providers</code> delegating the time
	 * consuming construction of {@link Downloader#getDownloadFragment()}
	 * into a background threads.
	 */
	public static void launch(LaunchableProvider[] providers) {
		boolean audioLaunched = false;
		GUIMediator.instance().setFrameCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		for (LaunchableProvider provider : providers) {
			final Downloader dl = provider.getDownloader();
			if (dl == null) {
				File file = provider.getFile();
				if (file != null) {
					audioLaunched = GUIUtils.launchOrEnqueueFile(file, audioLaunched);
				}
			}
			else {
				if (dl.getState() == DownloadStatus.INVALID) {
					GUIMediator.openURL("http://filtered.limewire.com/removed");
				}
				else {
					File fragment = (File) Worker.post(new Job() {
						@Override
                        public Object run() {
							return dl.getDownloadFragment();
						}
					});
					if (fragment != null) {
						audioLaunched = GUIUtils.launchOneTimeFile(fragment);
					}
					else {
						GUIMediator.instance().setFrameCursor(Cursor.getDefaultCursor());
						GUIMediator.showMessage(I18n.tr("There is nothing to preview for file {0}.",dl.getSaveFile().getName()), 
						        QuestionsHandler.NO_PREVIEW_REPORT
								);
						GUIMediator.instance().setFrameCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					}
				}
			}
		}
		GUIMediator.instance().setFrameCursor(Cursor.getDefaultCursor());
	}
	
}
