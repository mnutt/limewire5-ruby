package org.limewire.ui.swing.friends.chat;

import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.core.api.download.DownloadState;
import static org.limewire.ui.swing.util.I18n.tr;

/**
 * impl of a message containing a file offer
 */
public class MessageFileOfferImpl extends AbstractMessageImpl implements MessageFileOffer {

    private static final String DOWNLOAD_FROM_LIBRARY = tr("Download it now, or get it from them " +
            "{0}later{1}.","<a href=\"" + ChatDocumentBuilder.LIBRARY_LINK + "\">", "</a>");

    private final FileMetaData fileMetadata;
    private DownloadState downloadState;


    public MessageFileOfferImpl(String senderName, String friendName, String friendId, Type type, FileMetaData fileMetadata) {
        super(senderName, friendName, friendId, type);
        this.fileMetadata = fileMetadata;
        this.downloadState = null;
    }

    public FileMetaData getFileOffer() {
        return fileMetadata;
    }

    public void setDownloadState(DownloadState downloadState) {
        this.downloadState = downloadState;
    }

    public String toString() {
        String state = (downloadState == null) ? "No State" : downloadState.toString();
        String fileOffer = fileMetadata.getName();
        return fileOffer + "(" + state + ")";
    }
    
    public String format() {
        boolean isIncoming = (getType() == Message.Type.Received);
        return isIncoming ? formatIncoming() : formatOutgoing();
    }

    private String formatOutgoing() {
        String fileOfferSent = tr("Sharing file with {0}", getFriendID());
        return fileOfferSent + formatButtonText(getFileOffer().getName(), false);
    }

    private String formatButtonText(String buttonText, boolean buttonEnabled) {
        StringBuilder bldr = new StringBuilder();
        bldr.append("<br/>")
            .append("<form action=\"\"><input type=\"hidden\" name=\"fileid\" value=\"")
            .append(getFileOffer().getId())
            .append("\"/><input type=\"submit\" value=\"")
            .append(buttonText)
            .append(buttonEnabled ? "\"/>" : ":disabled\"/>")
            .append("</form><br/>");
        return bldr.toString();
    }

    private String formatIncoming() {

        String fileOfferFormatted;
        String fileOfferReceived = tr("{0} wants to share a file with you", getFriendID());
        String defaultFileOfferFormatted = fileOfferReceived + formatButtonText(tr("Download {0}", fileMetadata.getName()), true)
                    + DOWNLOAD_FROM_LIBRARY;

        if (downloadState == null) {
            fileOfferFormatted = defaultFileOfferFormatted;
        } else {

            switch (downloadState) {
                case REMOTE_QUEUED:
                case LOCAL_QUEUED:
                case CONNECTING:
                case PAUSED:
                case FINISHING:
                case DOWNLOADING:
                    fileOfferFormatted = fileOfferReceived +
                        formatButtonText(tr("Downloading"), false) +
                                DOWNLOAD_FROM_LIBRARY;

                    break;
                case CANCELLED:
                case STALLED:
                case ERROR:
                    fileOfferFormatted = fileOfferReceived +
                        formatButtonText(tr("Download {0}", fileMetadata.getName()), true) +
                                DOWNLOAD_FROM_LIBRARY;
                    break;
                case DONE:
                    fileOfferFormatted = fileOfferReceived +
                            tr("{0}Downloaded{1}","<a href=\"" +
                            ChatDocumentBuilder.MY_LIBRARY_LINK + "\">", "</a>");
                    break;
                default:
                    fileOfferFormatted = defaultFileOfferFormatted;
            }
        }
        return fileOfferFormatted;
    }
}
