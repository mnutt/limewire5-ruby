package org.limewire.ui.swing.friends.chat;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Rectangle;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.FormSubmitEvent;
import javax.swing.text.html.HTMLEditorKit;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.download.ResultDownloader;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.features.FileOfferFeature;
import org.limewire.core.api.friend.feature.features.FileOfferer;
import org.limewire.core.api.friend.feature.features.LimewireFeature;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.api.xmpp.RemoteFileItemFactory;
import org.limewire.io.InvalidDataException;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.action.CopyAction;
import org.limewire.ui.swing.action.CopyAllAction;
import org.limewire.ui.swing.action.PopupUtil;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.RuntimeTopicEventSubscriber;
import org.limewire.ui.swing.friends.chat.Message.Type;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.painter.GenericBarPainter;
import org.limewire.ui.swing.util.DNDUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.PainterUtils;
import org.limewire.ui.swing.util.ResizeUtils;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;
import org.limewire.util.FileUtils;
import org.limewire.xmpp.api.client.ChatState;
import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.XMPPException;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

/**
 *
 */
public class ConversationPane extends JPanel implements Displayable {
    private static final int PADDING = 5;
    private static final Log LOG = LogFactory.getLog(ConversationPane.class);
    private static final Color DEFAULT_BACKGROUND = new Color(224, 224, 224);
    private static final Color BACKGROUND_COLOR = Color.WHITE;

    private final List<Message> messages = new ArrayList<Message>();
    private final Map<String, MessageFileOffer> idToMessageWithFileOffer =
            new ConcurrentHashMap<String, MessageFileOffer>();

    private final JEditorPane editor;
    private final String conversationName;
    private final String friendId;
    private final String loggedInID;
    private final MessageWriter writer;
    private final ChatFriend chatFriend;
    private final ShareListManager shareListManager;
    private final IconManager iconManager;
    private final LibraryNavigator libraryNavigator;
    private HyperlinkButton downloadlink;
    private HyperlinkButton sharelink;
    private ResizingInputPanel inputPanel;
    private ChatState currentChatState;
    private final ResultDownloader downloader;
    private final RemoteFileItemFactory remoteFileItemFactory;
    private final SaveLocationExceptionHandler saveLocationExceptionHandler;
    @Resource(key="ChatConversation.toolbarTopColor") private Color toolbarTopColor;
    @Resource(key="ChatConversation.toolbarBottomColor") private Color toolbarBottomColor;
    @Resource(key="ChatConversation.toolbarBorderColor") private Color toolbarBorderColor;
    @Resource(key="ChatConversation.linkFont") private Font linkFont;

    private final JScrollPane conversationScroll;
    private final JPanel chatWrapper;
    
    
    @AssistedInject
    public ConversationPane(@Assisted MessageWriter writer, final @Assisted ChatFriend chatFriend, @Assisted String loggedInID,
                            ShareListManager libraryManager, IconManager iconManager, LibraryNavigator libraryNavigator,
                            ResultDownloader downloader, RemoteFileItemFactory remoteFileItemFactory,
                            @Named("available") ListenerSupport<FriendEvent> friendSupport,
                            SaveLocationExceptionHandler saveLocationExceptionHandler,
                            ListenerSupport<FeatureEvent> featureSupport, IconLibrary iconLibrary) {
        this.writer = writer;
        this.chatFriend = chatFriend;
        this.remoteFileItemFactory = remoteFileItemFactory;
        this.conversationName = chatFriend.getName();
        this.friendId = chatFriend.getID();
        this.loggedInID = loggedInID;
        this.shareListManager = libraryManager;
        this.iconManager = iconManager;
        this.libraryNavigator = libraryNavigator;
        this.downloader = downloader;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        
        GuiUtils.assignResources(this);

        setLayout(new BorderLayout());
        
        editor = new JEditorPane();
        editor.setEditable(false);
        editor.setContentType("text/html");
        editor.addHyperlinkListener(new HyperlinkListener());
        editor.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        HTMLEditorKit editorKit = (HTMLEditorKit) editor.getEditorKit();
        editorKit.setAutoFormSubmission(false);

        conversationScroll = new JScrollPane(editor, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        conversationScroll.setOpaque(false);
        conversationScroll.setBorder(BorderFactory.createEmptyBorder());
        
        
        final JButton closeConversation = new IconButton(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new CloseChatEvent(chatFriend).publish();
            }
        });
        closeConversation.setIcon(iconLibrary.getEndChat());
        
        // TODO: This should be done with a custom layout or 
        //        a mix of overlay layout, border layout, and mig layout.
        //        Mig layout may also have this functionality on its own.
        //        This is a proof on concept / temp bug fix.
        chatWrapper = new JPanel();
        chatWrapper.setLayout(null);
        final Rectangle closeBounds = new Rectangle(268,5,6,6);
        final Rectangle closeBoundsSlider = new Rectangle(250,5,6,6);
        closeConversation.setBounds(closeBounds);
        conversationScroll.setBounds(0,0, 278,171);
        
        chatWrapper.add(closeConversation);
        chatWrapper.add(conversationScroll);
        
        conversationScroll.getVerticalScrollBar().addComponentListener(new ComponentListener() {
            @Override
            public void componentHidden(ComponentEvent e) {
                closeConversation.setBounds(closeBounds);
            }
            @Override
            public void componentMoved(ComponentEvent e) {
            }
            @Override
            public void componentResized(ComponentEvent e) {
            }
            @Override
            public void componentShown(ComponentEvent e) {
                closeConversation.setBounds(closeBoundsSlider);
            }
        });
        
        conversationScroll.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                chatWrapper.repaint();
            }
        });
        
        add(chatWrapper, BorderLayout.CENTER);

        PopupUtil.addPopupMenus(editor, new CopyAction(editor), new CopyAllAction());

        FriendShareDropTarget friendShare = new FriendShareDropTarget(editor, libraryManager.getOrCreateFriendShareList(chatFriend.getFriend()));
        editor.setDropTarget(friendShare.getDropTarget());

        add(footerPanel(writer, chatFriend), BorderLayout.SOUTH);

        setBackground(DEFAULT_BACKGROUND);

        EventAnnotationProcessor.subscribe(this);
        friendSupport.addListener(new EventListener<FriendEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendEvent event) {
                if(event.getSource().getId().equals(friendId)) {
                    handleFriendEvent(event);
                }
            }
        });

        featureSupport.addListener(new EventListener<FeatureEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FeatureEvent event) {
                if (event.getSource().getFriend().getId().equals(friendId)) {
                    handleFeatureUpdate(event);
                }
            }
        });
    }

    @RuntimeTopicEventSubscriber(methodName="getMessageReceivedTopicName")
    public void handleConversationMessage(String topic, MessageReceivedEvent event) {
        Message message = event.getMessage();
        LOG.debugf("Message: from {0} text: {1} topic: {2}", message.getSenderName(), message.toString(), topic);
        messages.add(message);
        Type type = message.getType();

        if (type != Type.Sent) {
            currentChatState = ChatState.active;
        }

        if (message instanceof MessageFileOffer) {
            MessageFileOffer msgWithFileOffer = (MessageFileOffer)message;
            String fileOfferID = msgWithFileOffer.getFileOffer().getId();
            idToMessageWithFileOffer.put(fileOfferID, msgWithFileOffer);
        }

        displayMessages();
    }

    @RuntimeTopicEventSubscriber(methodName="getChatStateTopicName")
    public void handleChatStateUpdate(String topic, ChatStateEvent event) {
        LOG.debugf("Chat state update for {0} to {1}", event.getFriend().getName(), event.getState());
        if (currentChatState != event.getState()) {
            currentChatState = event.getState();
            displayMessages();
        }
    }

    private void handleFriendEvent(FriendEvent event) {
        switch(event.getType()) {
        case ADDED:
            displayMessages(false);
            inputPanel.getInputComponent().setEnabled(true);
            break;
        case REMOVED:
            displayMessages(true);
            inputPanel.getInputComponent().setEnabled(false);
            break;
        }
    }

    private void handleFeatureUpdate(FeatureEvent featureEvent) {
        FeatureEvent.Type featureEventType = featureEvent.getType();
        Feature feature = featureEvent.getData();

        if (feature.getID().equals(LimewireFeature.ID)) {
            if (featureEventType == FeatureEvent.Type.ADDED) {
                downloadlink.setEnabled(true);
            } else if (featureEventType == FeatureEvent.Type.REMOVED) {
                downloadlink.setEnabled(false);
            }
        }
    }

    public void setChatStateGone() {
        try {
            writer.setChatState(ChatState.gone);
        } catch (XMPPException e) {
            LOG.error("Could not set chat state while closing the conversation", e);
        }

        // TODO: remove listeners
    }

    public void destroy() {
        EventAnnotationProcessor.unsubscribe(this);
    }

    public String getMessageReceivedTopicName() {
        return MessageReceivedEvent.buildTopic(friendId);
    }

    public String getChatStateTopicName() {
        return ChatStateEvent.buildTopic(friendId);
    }

    private void displayMessages() {
        displayMessages(false);
    }

    private void displayMessages(boolean friendSignedOff) {
        String chatDoc = ChatDocumentBuilder.buildChatText(messages, currentChatState, conversationName, friendSignedOff);
        LOG.debugf("Chat doc: {0}", chatDoc);
        final JScrollBar verticalScrollBar = conversationScroll.getVerticalScrollBar();
        final int scrollValue = verticalScrollBar.getValue();
        editor.setText(chatDoc);
        
        //LWC-2262: If the scroll bar was moved above the bottom of the scrollpane, reset the value of
        //the bar to where it was before the text was updated.  This needs to be issued to the end of the
        //queue because the actual repainting/resizing of the scrollbar happens later in a 
        //task added to the EDT by the plaf listener of the editor's document.
        //A better fix for this behavior may be possible
        if (verticalScrollBar.getMaximum() > (scrollValue + verticalScrollBar.getVisibleAmount() + PADDING)) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    verticalScrollBar.setValue(scrollValue);
                }
            });
        }
        
        decorateFileOfferButtons();
        
        chatWrapper.repaint();
    }

    private void decorateFileOfferButtons() {
        //This is a hack to set the file mime-type icon for file offer buttons that may appear in the conversation
        recursiveButtonSearch(editor);
    }

    private void recursiveButtonSearch(Container container) {
        for(Component component : container.getComponents()) {
            if (component instanceof Container) {
                recursiveButtonSearch((Container)component);
            }
            if (component instanceof JButton) {
                JButton button = (JButton)component;
                String buttonText = button.getText();

                // Using end of button text to determine whether button shouild be disabled
                // then disable it.  This is because JEditorPane does not disable buttons
                // disabled in the form html
                if (buttonText.endsWith(":disabled")) {
                    buttonText = buttonText.substring(0, buttonText.lastIndexOf(":disabled"));
                    button.setText(buttonText);
                    button.setEnabled(false);
                }

                String extension = FileUtils.getFileExtension(buttonText);
                if (!extension.isEmpty()) {
                    Icon icon = iconManager.getIconForExtension(extension);
                    button.setIcon(icon);
                }

            }
        }
    }

    private JPanel footerPanel(MessageWriter writer, ChatFriend chatFriend) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_COLOR);
        
        downloadlink = new HyperlinkButton(new DownloadFromFriendLibraryAction());
        downloadlink.setFont(linkFont);

        sharelink = new HyperlinkButton(new ShareAction());
        sharelink.setFont(linkFont);
        
        
        JXPanel toolbar = new JXPanel(new MigLayout("insets 0 0 0 5, gap 10, alignx right, aligny 50%"));
        ResizeUtils.forceHeight(toolbar, 22);
        
        toolbar.setBackgroundPainter(new GenericBarPainter<JXPanel>(
                new GradientPaint(0, 0, toolbarTopColor, 0, 1, toolbarBottomColor),
                toolbarBorderColor, PainterUtils.TRASPARENT,
                toolbarBorderColor, PainterUtils.TRASPARENT));
        
        toolbar.add(downloadlink);
        toolbar.add(sharelink);

        inputPanel = new ResizingInputPanel(writer);
        inputPanel.setBorder(BorderFactory.createEmptyBorder());
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.CENTER);

        JTextComponent inputComponent = inputPanel.getInputComponent();
        FriendShareDropTarget friendShare = new FriendShareDropTarget(inputComponent, shareListManager.getOrCreateFriendShareList(chatFriend.getFriend()));
        inputComponent.setDropTarget(friendShare.getDropTarget());

        return panel;
    }

    @Override
    public void handleDisplay() {
        invalidate();
        repaint();
        inputPanel.handleDisplay();
    }

    private class DownloadFromFriendLibraryAction extends AbstractAction {
        public DownloadFromFriendLibraryAction() {
            super(tr("<html><u>{0}</u></html>", "View Files"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            libraryNavigator.selectFriendLibrary(chatFriend.getFriend());
        }
    }

    private class ShareAction extends AbstractAction {
        public ShareAction() {
            super(tr("<html><u>{0}</u></html>", "Share"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            libraryNavigator.selectFriendShareList(chatFriend.getFriend());
        }
    }

    private class HyperlinkListener implements javax.swing.event.HyperlinkListener {

        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e instanceof FormSubmitEvent) {
                FormSubmitEvent event = (FormSubmitEvent) e;
                //Just pushed the download the file button...
                LOG.debugf("File offer download requested. FileId: {0}", event.getData());

                DownloadItem dl;
                MessageFileOffer msgWithfileOffer = null;
                RemoteFileItem file = null;
                try {
                    String dataStr = event.getData();
                    String fileIdEncoded = dataStr.substring(dataStr.indexOf("=")+1).trim();
                    String fileId = URLDecoder.decode(fileIdEncoded, "UTF-8");
                    msgWithfileOffer = idToMessageWithFileOffer.get(fileId);

                    file = remoteFileItemFactory.create(chatFriend.getBestPresence(),
                           msgWithfileOffer.getFileOffer());
                    // TODO: what if offered file not in map for any reason?
                    //       Also, when would we remove items from the map?
                   dl = downloader.addDownload(file);
                   // Track download states by adding listeners to dl item
                   addPropertyListener(dl, msgWithfileOffer);
                } catch(SaveLocationException sle) {
                    final RemoteFileItem remoteFileItem = file;
                    final MessageFileOffer messageFileOffer = msgWithfileOffer;
                    saveLocationExceptionHandler.handleSaveLocationException(new DownloadAction() {
                        @Override
                        public void download(File saveFile, boolean overwrite)
                                throws SaveLocationException {
                            DownloadItem dl = downloader.addDownload(remoteFileItem, saveFile, overwrite);
                            addPropertyListener(dl, messageFileOffer);
                        }
                    }, sle, true); 
                } catch (InvalidDataException ide) {
                    // this means the FileMetaData we received isn't well-formed.
                    LOG.error("Unable to access remote file", ide);
                    FocusJOptionPane.showMessageDialog(ConversationPane.this, 
                            I18n.tr("Unable to access remote file"), 
                            I18n.tr("Hyperlink"), JOptionPane.WARNING_MESSAGE);
                } catch(UnsupportedEncodingException uee) {
                    throw new RuntimeException(uee); // impossible
                }

               

            } else if (EventType.ACTIVATED == e.getEventType()) {
                if (ChatDocumentBuilder.LIBRARY_LINK.equals(e.getDescription())) {
                    LOG.debugf("Opening a view to {0}'s library", chatFriend.getName());
                    libraryNavigator.selectFriendLibrary(chatFriend.getFriend());

                } else if (ChatDocumentBuilder.MY_LIBRARY_LINK.equals(e.getDescription())) {
                    LOG.debugf("Opening a view to my library");
                    libraryNavigator.selectLibrary();
                } else {
                    String linkDescription = e.getDescription();
                    LOG.debugf("Hyperlink clicked: {0}", linkDescription);
                    if (linkDescription.startsWith("magnet")) {
                        //TODO: Need to do something with magnet links

                    } else {
                        NativeLaunchUtils.openURL(e.getURL().toString());
                    }
                }
            }
        }

        private void addPropertyListener(DownloadItem dl, final MessageFileOffer msgWithfileOffer) {
            dl.addPropertyChangeListener(new PropertyChangeListener() {
                   public void propertyChange(PropertyChangeEvent evt) {
                       if ("state".equals(evt.getPropertyName())) {
                           DownloadState state = (DownloadState) evt.getNewValue();
                           msgWithfileOffer.setDownloadState(state);
                           displayMessages();
                       }
                   }
               });
        }
    }
    
    public void offerFolder(File folder, ListeningFuture<List<ListeningFuture<LocalFileItem>>> future) {
        // TODO: Change this to show event immediately & update as status changes.
        future.addFutureListener(new EventListener<FutureEvent<List<ListeningFuture<LocalFileItem>>>>() {
            @Override
            public void handleEvent(FutureEvent<List<ListeningFuture<LocalFileItem>>> event) {
                if(event.getResult() != null) {
                    for(ListeningFuture<LocalFileItem> future : event.getResult()) {
                        offerFile(null, future);
                    }
                }
            }
        });
    }

    public void offerFile(File file, ListeningFuture<LocalFileItem> future) {
        // TODO: Change this to show event immediately & update as status changes.
        future.addFutureListener(new EventListener<FutureEvent<LocalFileItem>>() {
            @SwingEDTEvent
            @Override
            public void handleEvent(FutureEvent<LocalFileItem> event) {
               if(event.getResult() != null) {
                   FriendPresence presence = chatFriend.getBestPresence();
                   if(presence.getFeature(FileOfferFeature.ID) != null) {
                       // update the chat state in case the remote user has not started conversation with us
                       try {
                           writer.setChatState(ChatState.active);
                       } catch (XMPPException e) {
                           LOG.error("Unable to set chat state prior to file offer", e);
                       }

                       // TODO: Listen for the file being added to the library &
                       //       update the MFOI based on status.
                       //       Send to offerer when File becomes FileItem.
                       FileOfferer fileOfferer = ((FileOfferFeature)presence.getFeature(FileOfferFeature.ID)).getFeature();
                       FileMetaData metadata = event.getResult().toMetadata();
                       fileOfferer.offerFile(metadata);
                       new MessageReceivedEvent(new MessageFileOfferImpl(loggedInID, null, friendId,
                                   Message.Type.Sent, metadata)).publish();
                   } else {
                       // TODO
                   }
               }
            } 
        });
        
    }
    
    private class FriendShareDropTarget implements DropTargetListener {
        private final DropTarget dropTarget;
        private LocalFileList fileList;
        
        public FriendShareDropTarget(JComponent component, LocalFileList fileList) {
            dropTarget = new DropTarget(component, DnDConstants.ACTION_COPY, this, true, null);
            this.fileList = fileList;
        }
        
        public void setModel(LocalFileList fileList) {
            this.fileList = fileList;
        }
        
        public DropTarget getDropTarget() {
            return dropTarget;
        }

        @Override
        public void dragEnter(DropTargetDragEvent dtde) {
            checkLimewireConnected(dtde);
        }
        
        @Override
        public void dragExit(DropTargetEvent dte) {
        }
        
        @Override
        public void dragOver(DropTargetDragEvent dtde) {
            checkLimewireConnected(dtde);
        }

        private void checkLimewireConnected(DropTargetDragEvent dtde) {
            if (!chatFriend.isSignedInToLimewire()) {
                dtde.rejectDrag();
            }
        }
        
        @Override
        public void drop(DropTargetDropEvent dtde) {
            if ((dtde.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) != 0) {
                // Accept the drop and get the transfer data
                dtde.acceptDrop(dtde.getDropAction());
                Transferable transferable = dtde.getTransferable();

                try {
                    final LocalFileList currentModel = fileList;
                    final File[] droppedFiles = DNDUtils.getFiles(transferable); 
              
                    for(File file : droppedFiles) {
                        if(file != null) {
                            if(file.isDirectory()) {
                                acceptedFolder(file, currentModel.addFolder(file));
                            } else {
                                acceptedFile(file, currentModel.addFile(file));
                            }
                        }
                    }
                    
                    dtde.dropComplete(true);
                } catch (Exception e) {
                    dtde.dropComplete(false);
                }
              } else {
                    dtde.rejectDrop();
              }
        }
        
        @Override
        public void dropActionChanged(DropTargetDragEvent dtde) {
        }
        
        protected void acceptedFile(File file, ListeningFuture<LocalFileItem> future) {
            offerFile(file, future);
        }
        
        protected void acceptedFolder(File folder, ListeningFuture<List<ListeningFuture<LocalFileItem>>> future) {
            offerFolder(folder, future);
        }
    }
}
