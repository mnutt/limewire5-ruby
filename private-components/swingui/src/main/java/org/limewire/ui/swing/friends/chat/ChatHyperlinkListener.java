package org.limewire.ui.swing.friends.chat;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.FormSubmitEvent;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.download.ResultDownloader;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.xmpp.RemoteFileItemFactory;
import org.limewire.io.InvalidDataException;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

/**
 * HyperlinkListener for links to libraries, form submissions (file offers)
 * that come from chat
 */
public class ChatHyperlinkListener implements javax.swing.event.HyperlinkListener {
    private static final Log LOG = LogFactory.getLog(ChatHyperlinkListener.class);

    private Conversation conversation;

    private final ResultDownloader downloader;
    private final RemoteFileItemFactory remoteFileItemFactory;
    private final Provider<SaveLocationExceptionHandler> saveLocationExceptionHandler;
    private final LibraryNavigator libraryNavigator;

    @Inject
    public ChatHyperlinkListener(@Assisted Conversation conversation, ResultDownloader downloader,
                                 RemoteFileItemFactory remoteFileItemFactory,
                                 Provider<SaveLocationExceptionHandler> saveLocationExceptionHandler,
                                 LibraryNavigator libraryNavigator) {

        this.conversation = conversation;
        this.downloader = downloader;
        this.remoteFileItemFactory = remoteFileItemFactory;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        this.libraryNavigator = libraryNavigator;
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e instanceof FormSubmitEvent) {
            FormSubmitEvent event = (FormSubmitEvent) e;

            //Just pushed the download the file button...
            LOG.debugf("File offer download requested. FileId: {0}", event.getData());

            try {
                String dataStr = event.getData();
                String fileIdEncoded = dataStr.substring(dataStr.indexOf("=")+1).trim();
                String fileId = URLDecoder.decode(fileIdEncoded, "UTF-8");
                downloadFileOffer(fileId);
            } catch(UnsupportedEncodingException uee) {
                throw new RuntimeException(uee); // impossible
            }
        } else if (HyperlinkEvent.EventType.ACTIVATED == e.getEventType()) {
            handleLinkClick(e.getDescription(), e.getURL());
        }
    }

    /**
     *
     * Download the file offer given a file ID
     *
     * package-private for testing
     *
     * @param fileId identifier to look up the file offer message
     */
    void downloadFileOffer(String fileId) {
        Map<String, MessageFileOffer> fileOffers = conversation.getFileOfferMessages();
        MessageFileOffer msgWithfileOffer = fileOffers.get(fileId);
        RemoteFileItem file = null;

        try {
            file = remoteFileItemFactory.create(msgWithfileOffer.getPresence(), msgWithfileOffer.getFileOffer());
            DownloadItem dl = downloader.addDownload(file);

            // Track download states by adding listeners to dl item
            addPropertyListener(dl, msgWithfileOffer);

        } catch (SaveLocationException sle) {
            final RemoteFileItem remoteFileItem = file;
            final MessageFileOffer messageFileOffer = msgWithfileOffer;
            saveLocationExceptionHandler.get().handleSaveLocationException(new DownloadAction() {
                @Override
                public void download(File saveFile, boolean overwrite)
                        throws SaveLocationException {
                    DownloadItem dl = downloader.addDownload(remoteFileItem, saveFile, overwrite);
                    addPropertyListener(dl, messageFileOffer);
                }

                @Override
                public void downloadCanceled(SaveLocationException sle) {
                    //nothing to do                    
                }

            }, sle, true);
        } catch (InvalidDataException ide) {
            // this means the FileMetaData we received isn't well-formed.
            LOG.error("Unable to access remote file", ide);
            FocusJOptionPane.showMessageDialog(null,
                    I18n.tr("Unable to access remote file"),
                    I18n.tr("Hyperlink"), JOptionPane.WARNING_MESSAGE);
        }
    }

    private void handleLinkClick(String linkDescription, URL url) {

        if (ChatDocumentBuilder.LIBRARY_LINK.equals(linkDescription)) {
            ChatFriend libraryChatFriend = conversation.getChatFriend();
            LOG.debugf("Opening a view to {0}'s library", libraryChatFriend.getName());
            libraryNavigator.selectFriendLibrary(libraryChatFriend.getFriend());

        } else if (ChatDocumentBuilder.MY_LIBRARY_LINK.equals(linkDescription)) {
            LOG.debugf("Opening a view to my library");
            libraryNavigator.selectLibrary();
        } else {
            LOG.debugf("Hyperlink clicked: {0}", linkDescription);
            if (linkDescription.startsWith("magnet")) {
                //TODO: Need to do something with magnet links

            } else if (url != null) {
                NativeLaunchUtils.openURL(url.toString());
            }
        }
    }

    private void addPropertyListener(DownloadItem dl, final MessageFileOffer msgWithfileOffer) {
        dl.addPropertyChangeListener(new PropertyChangeListener() {
               public void propertyChange(PropertyChangeEvent evt) {
                   if ("state".equals(evt.getPropertyName())) {
                       DownloadState state = (DownloadState) evt.getNewValue();
                       msgWithfileOffer.setDownloadState(state);
                       conversation.displayMessages();
                   }
               }
           });
    }
}
