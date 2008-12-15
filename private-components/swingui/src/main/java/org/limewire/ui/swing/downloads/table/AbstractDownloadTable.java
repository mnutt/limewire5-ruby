package org.limewire.ui.swing.downloads.table;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.table.MouseableTable;

public abstract class AbstractDownloadTable extends MouseableTable {

    abstract public DownloadItem getDownloadItem(int row);

}
