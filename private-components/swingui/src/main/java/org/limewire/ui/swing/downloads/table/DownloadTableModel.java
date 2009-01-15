/**
 * 
 */
package org.limewire.ui.swing.downloads.table;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.table.LimeSingleColumnTableFormat;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventTableModel;



public class DownloadTableModel extends EventTableModel<DownloadItem> {
	
	private static final long serialVersionUID = 4079559883623594683L;
	private EventList<DownloadItem> downloadItems;

	public DownloadTableModel(EventList<DownloadItem> downloadItems) {
		super(downloadItems, new LimeSingleColumnTableFormat<DownloadItem>(DownloadItem.class), false);
		this.downloadItems = downloadItems;
	}


	public DownloadItem getDownloadItem(int index) {
		return downloadItems.get(index);
	}

}
