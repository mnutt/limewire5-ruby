package com.limegroup.gnutella.gui.upload;



import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.tables.ProgressBarRenderer;

/**
 * A modififed progress bar renderer that takes into account
 * BT uploads, seeding, ratios, etc.
 */
public class UploadProgressBarRenderer extends ProgressBarRenderer {

	@Override
	protected int getBarStatus(Object value) {
		if (value == null)
			return 0;
		
		UploadProgressBarData data = (UploadProgressBarData)value;
		return (int) (data.totalUploaded * 100 / data.totalSize);
	}

	@Override
	protected String getDescription(Object value) {
		if (value == null)
			return "0 %";
		
		UploadProgressBarData data = (UploadProgressBarData)value;
		if (data.totalSize == 0)
			return "0 %";
		
		int ratio = getBarStatus(value);
		
		// if not bittorrent, treat like flaky gnutella host
		if (!data.isBittorrent || ratio <= 100) 
			return Integer.toString(Math.min(100,ratio))+" %";
		
		return I18n.tr("Seeding")+
		"("+ GUIUtils.toUnitbytes(data.totalUploaded - data.totalSize)+")";
		
	}
	
	public static class UploadProgressBarData 
	implements Comparable<UploadProgressBarData> {
		public long totalUploaded;
		public long totalSize;
		public final boolean isBittorrent;
		public UploadProgressBarData(boolean isBittorrent) {
			this.isBittorrent = isBittorrent;
		}
		
		public int compareTo(UploadProgressBarData other) {
			float me = (float)totalUploaded / totalSize;
			float them = (float)other.totalUploaded / other.totalSize;
			if (me > them)
				return 1;
			else if (me < them)
				return -1;
			else return 0;
		}
	}
}
