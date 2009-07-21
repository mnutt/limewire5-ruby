package org.limewire.ui.swing.downloads.table.renderer;


import java.awt.Component;
import java.util.Collection;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.friend.api.Friend;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;

public class DownloadMessageRenderer extends DefaultTableCellRenderer {
    
    @Inject
    public DownloadMessageRenderer(){
        new DownloadRendererProperties().decorateComponent(this);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        if(value instanceof DownloadItem) {
            DownloadItem item = (DownloadItem)value;
            return super.getTableCellRendererComponent(table, getPercentMessage(item) + getMessage(item.getState(), item), isSelected, false, row, column);
        } else {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }
    
    private String getPercentMessage(DownloadItem item){
        int percent = item.getPercentComplete();
        DownloadState state = item.getState();
        if (percent == 0 || state == DownloadState.DONE ||  state == DownloadState.DOWNLOADING ||  state == DownloadState.ERROR){
            return "";
        }
        return percent + "% - ";    
    }
    
    /**
     * @return the message displayed after the percentage. Can be null for non-covered states.
     */
    private String getMessage(DownloadState state, DownloadItem item) {
        switch (state) {
        case RESUMING:
            return I18n.tr("Resuming");
        case CANCELLED:
            return I18n.tr("Canceled");
        case FINISHING:
            return I18n.tr("Finishing...");
        case DONE:
            return I18n.tr("Done");
        case CONNECTING:
            Collection<RemoteHost> hosts = item.getRemoteHosts();
            if(hosts.size() == 0){
                return I18n.tr("Connecting...");
            }
            //{0}: 1 person, 2 people, etc
            return I18n.tr("Connecting to {0}", getPeopleText(item));
        case DOWNLOADING:            
            // {0}: current size
            // {1}: total size
            // {2}: download speed
            // {3}: download source
            if(item.getDownloadSourceCount() == 0){
                return I18n.tr("{0} of {1} ({2})",
                        GuiUtils.toUnitbytes(item.getCurrentSize()), 
                        GuiUtils.toUnitbytes(item.getTotalSize()),
                        GuiUtils.rate2speed(item.getDownloadSpeed()));
            } else { 
                return I18n.tr("{0} of {1} ({2}) from {3}",
                    GuiUtils.toUnitbytes(item.getCurrentSize()), 
                    GuiUtils.toUnitbytes(item.getTotalSize()),
                    GuiUtils.rate2speed(item.getDownloadSpeed()), 
                    getPeopleText(item));
            }
        case TRYING_AGAIN:
            return getTryAgainMessage(item.getRemainingTimeInState());
        case STALLED:
            return I18n.tr("Stalled - {0} of {1}", 
                    GuiUtils.toUnitbytes(item.getCurrentSize()),
                    GuiUtils.toUnitbytes(item.getTotalSize()));
        case ERROR:         
            return I18n.tr("Unable to download: ") + I18n.tr(item.getErrorState().getMessage());
        case PAUSED:
            // {0}: current size, {1} total size
            return I18n.tr("Paused - {0} of {1}", 
                    GuiUtils.toUnitbytes(item.getCurrentSize()), GuiUtils.toUnitbytes(item.getTotalSize()));
        case LOCAL_QUEUED:
            return getQueueTimeMessage(item.getRemainingTimeInState());
        case REMOTE_QUEUED:
            if(item.getRemoteQueuePosition() == -1 || item.getRemoteQueuePosition() == Integer.MAX_VALUE){
                return getQueueTimeMessage(item.getRemainingTimeInState());
            }
            return I18n.trn("Waiting - Next in line",
                    "Waiting - {0} in line",
                    item.getRemoteQueuePosition(), item.getRemoteQueuePosition());
        default:
            return null;
        }
        
    }
    
    private String getTryAgainMessage(long tryingAgainTime) {
        if(tryingAgainTime == DownloadItem.UNKNOWN_TIME){
            return I18n.tr("Looking for file...");                
        } else {
            return I18n.tr("Looking for file ({0} left)", CommonUtils.seconds2time(tryingAgainTime));
        }
    }
    
    private String getPeopleText(DownloadItem item) {
        if(item.isStoreDownload()){
            return I18n.tr("Store");
        }
        
        Collection<RemoteHost> hosts = item.getRemoteHosts();
        if (hosts.size() == 0) {
            //checking sources to support showing the number of bit torrent hosts.
            int downloadSourceCount = item.getDownloadSourceCount();
            if(downloadSourceCount < 1) {
                return I18n.tr("nobody");
            } else {
                return I18n.trn("{0} P2P User", "{0} P2P Users", downloadSourceCount);
            }
        } else if (hosts.size() == 1) {

            Friend friend = hosts.iterator().next().getFriendPresence().getFriend();
            if (friend.isAnonymous()) {
                return I18n.tr("1 P2P User");
            } else {
                return friend.getRenderName();
            }

        } else {
            boolean hasP2P = false;
            boolean hasFriend = false;
            
            for (RemoteHost host : hosts) {                
                if (host.getFriendPresence().getFriend().isAnonymous()) {
                    hasP2P = true;
                } else {
                    hasFriend = true;
                }
                
                if (hasP2P && hasFriend) {
                    // We found both.  We're done.
                    break;
                }
            }
            if (hasP2P && hasFriend ) {
                return I18n.trn("{0} Person", "{0} People", hosts.size());
            } else if (hasP2P) {
                return I18n.trn("{0} P2P User", "{0} P2P Users", hosts.size());
            } else {
                //just friends
                return I18n.trn("{0} Friend", "{0} Friends", hosts.size());
            }
        }
    }
    
    private String getQueueTimeMessage(long queueTime){
        //if(queueTime == DownloadItem.UNKNOWN_TIME){
            return I18n.tr("Waiting...");                
        //} else {
        //    return I18n.tr("Waiting...", CommonUtils.seconds2time(queueTime));
        //}
    }

}
