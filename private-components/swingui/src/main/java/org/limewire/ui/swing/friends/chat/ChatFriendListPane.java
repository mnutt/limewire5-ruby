package org.limewire.ui.swing.friends.chat;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.border.DropShadowBorder;
import org.jdesktop.swingx.decorator.BorderHighlighter;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.Network;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.RuntimeTopicPatternEventSubscriber;
import org.limewire.ui.swing.friends.chat.Message.Type;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.library.ShareListIcons;
import org.limewire.ui.swing.search.FriendPresenceActions;
import org.limewire.ui.swing.table.AbstractTableFormat;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableColors;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * The pane that lists all available friends in the chat area.
 */
public class ChatFriendListPane extends JPanel { 
    
    private static final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);
    private static final Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);
    private static final int PREFERRED_WIDTH = 122;
    private static final int LEFT_EDGE_PADDING_PIXELS = 5;
    private static final Log LOG = LogFactory.getLog(ChatFriendListPane.class);
    private static final String ALL_CHAT_MESSAGES_TOPIC_PATTERN = MessageReceivedEvent.buildTopic(".*");
    
    private final EventList<ChatFriend> chatFriends;
    private final MouseableTable friendsTable;
    private final ChatModel chatModel;
    private final WeakHashMap<ChatFriend, AlternatingIconTimer> friendTimerMap;
    private final Provider<FriendPresenceActions> remoteHostActions;
    private final Provider<SharedFileListManager> sharedFileListManager;
    private final Provider<LibraryMediator> library;

    private WeakReference<ChatFriend> activeConversation = new WeakReference<ChatFriend>(null);
    private FriendHoverBean mouseHoverFriend = new FriendHoverBean();
    @Resource private Color rightEdgeBorderColor;
    @Resource private Color conversationsSeparatorColor;
    @Resource private Color friendColor;
    @Resource private Font  friendFont;
    @Resource private Color friendSelectionColor;
    @Resource private Color activeConversationBackgroundColor;
    @Resource private Icon unviewedMessageIcon;
    @Resource private Icon availableIcon;
    @Resource private Icon doNotDisturbIcon;
    @Resource private Icon chattingIcon;
    @Resource private Icon endChatIcon;
    @Resource private Icon endChatOverIcon;  
    @Resource private Icon awayIcon;

    @Inject
    public ChatFriendListPane(ChatModel chatModel, 
            Provider<FriendPresenceActions> remoteHostActions, 
            Provider<SharedFileListManager> sharedFileListManager,
            Provider<LibraryMediator> library) {
        super(new BorderLayout());
        this.chatModel = chatModel;
        this.friendTimerMap = new WeakHashMap<ChatFriend, AlternatingIconTimer>();
        this.remoteHostActions = remoteHostActions;
        this.sharedFileListManager = sharedFileListManager;
        this.library = library;
        
        this.chatFriends = chatModel.getChatFriendList();
        
        GuiUtils.assignResources(this);
        
        ObservableElementList<ChatFriend> observableList = GlazedListsFactory.observableElementList(chatModel.getChatFriendList(), GlazedLists.beanConnector(ChatFriend.class));
        SortedList<ChatFriend> sortedFriends = GlazedListsFactory.sortedList(observableList,  new FriendAvailabilityComparator());
        friendsTable = createFriendsTable(sortedFriends);

        friendsTable.setPopupHandler(new FriendPopupHandler());
        
        setBorder(new DropShadowBorder(rightEdgeBorderColor, 1, 1.0f, 0, false, false, false, true));
        
        JScrollPane scrollPane = new JScrollPane(friendsTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane);
        setPreferredSize(new Dimension(PREFERRED_WIDTH, 200));
        setMinimumSize(new Dimension(PREFERRED_WIDTH, 200));
        
        EventAnnotationProcessor.subscribe(this);
    }
    
    @Inject void register(ListenerSupport<FriendConnectionEvent> connectionSupport) {
        connectionSupport.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendConnectionEvent event) {
                switch(event.getType()) {
                case DISCONNECTED:
                    handleSignoff();
                    break;
                }
            }
        });
    }

    private void handleSignoff() {
        closeAllChats();
    }

    
    private class FriendPopupHandler implements TablePopupHandler {

        @Override
        public boolean isPopupShowing(int row) {
            return false;
        }

        @Override
        public void maybeShowPopup(Component component, int x, int y) {
            int index = friendsTable.rowAtPoint(new Point(x, y));
            
            // Popup selects the item (as per spec)
            friendsTable.getSelectionModel().setSelectionInterval(index, index);

            ChatFriend chatFriend = getFriend(friendsTable, index);
            createPopupMenu(chatFriend).show(component, x, y);
        }

        private JPopupMenu createPopupMenu(ChatFriend chatFriend) {
            JPopupMenu popup = new JPopupMenu();

            if (chatFriend.isChatting()) {
                popup.add(createShareListSubMenu(chatFriend.getID()));
                popup.addSeparator();                
                popup.add(new BrowseFiles(chatFriend));
                popup.addSeparator();
                popup.add(new CloseChat(chatFriend));
            } else {
                popup.add(new OpenChat(chatFriend));
                popup.addSeparator();
                popup.add(createShareListSubMenu(chatFriend.getID()));
                popup.addSeparator();                
                popup.add(new BrowseFiles(chatFriend));
            }

            return popup;
        }
        
        private JMenu createShareListSubMenu(String friendID) {
            JMenu shareListMenu = new JMenu(I18n.tr("Share List"));
            for (SharedFileList list : sharedFileListManager.get().getModel()) {
                if(list.isPublic()){//skip the public share list
                    continue;
                }
                
                if (list.getFriendIds().contains(friendID)) {
                    shareListMenu.add(new AddRemoveCheckBoxMenuItem(friendID, list));
                } else {
                    shareListMenu.add(new AddRemoveCheckBoxMenuItem(friendID, list));
                }
            }
            shareListMenu.addSeparator();
            shareListMenu.add(new ShareNewList(friendID));

            return shareListMenu;
        }
    }
    
    private static ChatFriend getFriend(JTable table, int index) {
        DefaultEventTableModel model = (DefaultEventTableModel)table.getModel();
        return index < 0 ? null : (ChatFriend) model.getElementAt(index);
    }
    
    private MouseableTable createFriendsTable(final EventList<ChatFriend> friendsList) {
        TextFilterator<ChatFriend> friendFilterator = new TextFilterator<ChatFriend>() {
            @Override
            public void getFilterStrings(List<String> baseList, ChatFriend element) {
                baseList.add(element.getName());
            }
        };
        
        final TextMatcherEditor<ChatFriend> editor = new TextMatcherEditor<ChatFriend>(friendFilterator);
        final FilterList<ChatFriend> filter = GlazedListsFactory.filterList(friendsList, editor);
        
        TableFormat<ChatFriend> format = new AbstractTableFormat<ChatFriend>(tr("Name")) {
            @Override
            public Object getColumnValue(ChatFriend chatFriend, int column) {
                if (column == 0) {
                    return chatFriend.getName();
                }
                throw new IllegalArgumentException("Couldn't find value for unknown friends chat table column: " + column);
            }
        };
        
        final MouseableTable table = new CustomTooltipLocationTable(new DefaultEventTableModel<ChatFriend>(friendsList, format)); 
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setEmptyRowsPainted(false);
        TableColors colors = new TableColors();
        table.setHighlighters(new ColorHighlighter(HighlightPredicate.ALWAYS, colors.evenColor,
                colors.evenForeground, colors.selectionColor,
                colors.selectionForeground));
        table.addMouseListener(new LaunchChatListener());
        //Add as mouse listener and motion listener because it cares about MouseExit and MouseMove events
        CloseChatListener closeChatListener = new CloseChatListener();
        table.addMouseListener(closeChatListener);
        table.addMouseMotionListener(closeChatListener);
        
        table.addKeyListener(new KeyAdapter() {
            ArrayList<String> keysPressed = new ArrayList<String>();
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (keyCode == KeyEvent.VK_DELETE) {
                    keysPressed.clear();
                } else if (keyCode == KeyEvent.VK_BACK_SPACE && keysPressed.size() > 0) { 
                    keysPressed.remove(keysPressed.size() - 1);
                } else {
                    keysPressed.add(Character.toString(e.getKeyChar()));
                }
                
                editor.setFilterText(keysPressed.toArray(new String[keysPressed.size()]));
                
                if (LOG.isDebugEnabled()) {
                    LOG.debugf("FriendsPane keyPressed(): {0} {1} ", KeyEvent.getKeyText(e.getKeyCode()), getKeyPressed());
                }
                
                if (filter.size() > 0) {
                    ChatFriend firstFriend = filter.get(0);
                    int index = friendsList.indexOf(firstFriend);
                    table.getSelectionModel().setSelectionInterval(index, index);
                }
            }
            
            private String getKeyPressed() {
                StringBuilder builder = new StringBuilder();
                for(String s : keysPressed) {
                    builder.append(s);
                }
                return builder.toString();
            }
        });
        
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setCellRenderer(new FriendCellRenderer());
        
        table.setShowGrid(false, false);
        
        table.setTableHeader(null);
        table.setColumnMargin(0);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        table.addHighlighter(new BorderHighlighter(new ChattingUnderlineHighlightPredicate(), 
                new DropShadowBorder(conversationsSeparatorColor, 1, 1.0f, 1, false, false, true, false)));
        
        return table;
    }
    
    private class ChattingUnderlineHighlightPredicate implements HighlightPredicate {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            //Clear the border on the renderer because the same panel is being reused for every cell
            //and it will be set to an underlining border if this method returns true
            JComponent panel = (JComponent)renderer;
            panel.setBorder(BorderFactory.createEmptyBorder());

            JXTable table = (JXTable) adapter.getComponent();
            
            int row = adapter.row;
            ChatFriend chatFriend = getFriend(table, row);
            
            if (chatFriend.isChatting()) {
                DefaultEventTableModel model = (DefaultEventTableModel) table.getModel();
                if (model.getRowCount() > (row + 1)) {
                    ChatFriend nextFriend = (ChatFriend) model.getElementAt(row + 1);
                    if (!nextFriend.isChatting()) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @RuntimeTopicPatternEventSubscriber(methodName="getMessagingTopicPatternName")
    public void handleMessageReceived(String topic, MessageReceivedEvent event) {
        Message message = event.getMessage();
        LOG.debugf("All Messages listener: from {0} text: {1} topic: {2}", message.getSenderName(), message.toString(), topic);
        ChatFriend chatFriend = chatModel.getChatFriend(message.getFriendID());

        if (message.getType() != Type.SENT) {
            chatFriend.setReceivedUnviewedMessages(true);
        }
        if (!chatFriend.isActiveConversation() && message.getType() != Type.SENT) {
            if (!friendTimerMap.containsKey(chatFriend)) {
                AlternatingIconTimer iconTimer = new AlternatingIconTimer(chatFriend);
                friendTimerMap.put(chatFriend, iconTimer);
                iconTimer.start();
            }
        }
    }
    
    public String getMessagingTopicPatternName() {
        return ALL_CHAT_MESSAGES_TOPIC_PATTERN;
    }
    
    public void fireConversationStarted(String friendId) {
        ChatFriend chatFriend = chatModel.getChatFriend(friendId);
        if(chatFriend != null) {
            startOrSelectConversation(chatFriend);
        } else {
            //TODO notify that chat no longer possible.
        }
    }

    private void startOrSelectConversation(ChatFriend chatFriend) {
        MessageWriter writerWithEventDispatch = null;
        if (!chatFriend.isChatting() && chatFriend.isSignedIn()) {
            MessageWriter writer = chatFriend.createChat(new MessageReaderImpl(chatFriend));
            writerWithEventDispatch = new MessageWriterImpl(chatFriend, writer);
        }
        new ConversationSelectedEvent(chatFriend, writerWithEventDispatch, true).publish();
        setActiveConversation(chatFriend);
    }

    private void setActiveConversation(ChatFriend chatFriend) {
        LOG.debugf("Setting active conversation: {0}", chatFriend.getName());
        ChatFriend oldActiveConversation = activeConversation.get();
        if (oldActiveConversation != null) {
            oldActiveConversation.setActiveConversation(false);
        }
        activeConversation = new WeakReference<ChatFriend>(chatFriend);
        chatFriend.setReceivedUnviewedMessages(false);
        chatFriend.setActiveConversation(true);
        friendTimerMap.remove(chatFriend);
    }
    
    /**
     * Marks the active conversation as having no unseen messages.
     */
    public void markActiveConversationRead() {
        ChatFriend activeFriend = activeConversation.get();
        if(activeFriend != null) {
            activeFriend.setReceivedUnviewedMessages(false);
        }
    }
    
    private Icon getChatIcon(ChatFriend chatFriend) {
        AlternatingIconTimer timer = friendTimerMap.get(chatFriend);
        if (timer != null) {
            return timer.getIcon();
        }
        
        if(!chatFriend.isActiveConversation() && chatFriend.hasReceivedUnviewedMessages()) {
            return unviewedMessageIcon;
        }
        
        //Change to chatting icon because gtalk doesn't actually set mode to 'chat', so icon won't show chat bubble normally
        if (chatFriend.isChatting()) {
            return chattingIcon;
        }
        return getIcon(chatFriend);
    }
    
    private Icon getIcon(ChatFriend chatFriend) {
        FriendPresence.Mode mode = chatFriend.getMode();
        switch(mode) {
        case available:
            return availableIcon;
        case chat:
            return chattingIcon;
        case dnd:
            return doNotDisturbIcon;
        }
        return awayIcon;
    }
    
    private void fireCloseChat(ChatFriend chatFriend) {
        new CloseChatEvent(chatFriend).publish();
    }
    
    @EventSubscriber
    public void handleCloseChatEvent(CloseChatEvent event) {
        closeChat(event.getFriend());
    }

    private void closeChat(ChatFriend chatFriend) {
        if (chatFriend != null) {

            chatFriend.stopChat();
            if (!chatFriend.isSignedIn()) {
                chatFriends.remove(chatModel.removeChatFriend(chatFriend.getID()));
            }
            
            ChatFriend nextFriend = null;
            for(ChatFriend tmpFriend : chatFriends) {
                if (tmpFriend == chatFriend || !tmpFriend.isChatting()) {
                    continue;
                }
                if (nextFriend == null || tmpFriend.getChatStartTime() > nextFriend.getChatStartTime()) {
                    nextFriend = tmpFriend;
                }
            }

            if (nextFriend != null) {
                startOrSelectConversation(nextFriend);
            }
        }
    }

    private void closeAllChats() {
        for (ChatFriend chatFriend : chatFriends) {
            String userId = chatFriend.getID();
            if (chatModel.getChatFriend(userId) != null) {
                chatModel.removeChatFriend(userId);
            }
            chatFriend.stopChat();
        }
        chatFriends.clear();
    }

    private class FriendCellRenderer implements TableCellRenderer {
        private final JXPanel cell; 
        private final JXLabel friendName;
        private final JXLabel chatStatus;
        
        public FriendCellRenderer() {
            cell = new JXPanel(new MigLayout("insets 2 2 0 0", "[]4[]"));
            
            this.friendName = new JXLabel();
            this.chatStatus = new JXLabel();

            friendName.setFont(friendFont);
            friendName.setForeground(friendColor);
            friendName.setMaximumSize(new Dimension(85, 12));
        }
        
        @Override
        public final Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            cell.removeAll();
            cell.setBackgroundPainter(null);
            
            ChatFriend chatFriend = getFriend(table, row);
            
            //Handle null possible sent in via AccessibleJTable inner class
            value = value == null ? table.getValueAt(row, column) : value;
            boolean isChatHoveredOver = (chatFriend == mouseHoverFriend.getFriend() && chatFriend.isChatting());
            renderComponent(cell, value, chatFriend, isChatHoveredOver);
            
            if (isSelected) {
                cell.setBackground(friendSelectionColor);
            } else if (chatFriend.isActiveConversation()) {
                cell.setBackground(activeConversationBackgroundColor);
            } else  {
                cell.setBackground(table.getBackground());
                cell.setForeground(table.getForeground());
            }

            return cell;
        }

        protected void renderComponent(JPanel panel, Object value, ChatFriend chatFriend, boolean isChatHoveredOver) {
            if (isChatHoveredOver) {
                Point hoverPoint = mouseHoverFriend.getHoverPoint();
                boolean overCloseIcon = isOverCloseIcon(hoverPoint);
                chatStatus.setIcon(overCloseIcon ? endChatOverIcon : endChatIcon);
                chatStatus.setToolTipText(tr("Close Conversation"));
                panel.add(chatStatus, "gapleft 4, gapright 2");
            } else {
                chatStatus.setIcon(getChatIcon(chatFriend));
                chatStatus.setToolTipText(null);
                panel.add(chatStatus);
            }
            if(value != null)
                friendName.setText(value.toString());
            else
                friendName.setText("");
            panel.add(friendName);
        }
    }
    
    private class CustomTooltipLocationTable extends MouseableTable {
        private final Color GRAY_BACKGROUND = new Color(172, 172, 172);

        public CustomTooltipLocationTable(TableModel dm) {
            super(dm);
        }

        @Override
        public JToolTip createToolTip() {
            JToolTip tooltip = super.createToolTip();
            tooltip.setBackground(GRAY_BACKGROUND);
            return tooltip;
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            Point mousePoint = event.getPoint();
            int row = friendsTable.rowAtPoint(mousePoint);
            if (row == -1) {
                return null;
            }
            
            DefaultEventTableModel model = (DefaultEventTableModel) friendsTable.getModel();
            ChatFriend chatFriend = (ChatFriend) model.getElementAt(row);
            
            if (chatFriend.isChatting() && isOverCloseIcon(mousePoint)) {
                return tr("Close Conversation");
            }
            
            StringBuilder tooltip = new StringBuilder();
            tooltip.append("<html>")
                .append("<head>")
                .append("<style>body { margin: 2px 10px 2px 4px;}</style>")
                .append("</head>")
                .append("<body>")
                .append("<img src=\"")
                .append(ChatFriendsUtil.getIconURL(chatFriend.getMode())).append("\"/>&nbsp;")
                .append("<b>").append(chatFriend.getName());
            
            if(chatFriend.getFriend().getNetwork().getType() != Network.Type.FACEBOOK) {
                tooltip.append(" &lt;").append(chatFriend.getID())
                    .append("&gt;");
            }
                
            tooltip.append("</b><br/>");
            String status = chatFriend.getStatus();
            if (status != null && status.length() > 0) {
                //using width to limit the size of the tooltip, unfortunately looks like max-width does not work 
                tooltip.append("<div color=\"rgb(255,255,255)\" style=\"width: 300px;\">").append(status).append("</div>");
            }
            tooltip.append("</body>")
                .append("</html>");
            return tooltip.toString();
        }
    }
    
    private boolean isOverCloseIcon(Point point) {
        return point.x > LEFT_EDGE_PADDING_PIXELS && point.x < endChatIcon.getIconWidth() + LEFT_EDGE_PADDING_PIXELS;
    }
    
    private class AlternatingIconTimer {
        private WeakReference<ChatFriend> friendRef;
        private int flashCount;
        
        public AlternatingIconTimer(ChatFriend chatFriend) {
            this.friendRef = new WeakReference<ChatFriend>(chatFriend);
        }

        public void start() {
            Timer timer = new Timer(1500, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    ChatFriend chatFriend = friendRef.get();
                    if (chatFriend == null || flashCount == 4) {
                        stopTimer(e);
                        return;
                    }
                    
                    int friendIndex = chatFriends.indexOf(chatFriend);
                    if (friendIndex > -1) {
                        AbstractTableModel model = (AbstractTableModel) friendsTable.getModel();
                        model.fireTableCellUpdated(friendIndex, 0);
                        flashCount++;
                    }
                }
            });
            timer.start();
        }

        private void stopTimer(ActionEvent e) {
            Timer timer = (Timer) e.getSource();
            timer.stop();
        }
        
        private Icon getIcon() {
            return flashCount % 2 == 0 ? unviewedMessageIcon : chattingIcon;
        }
    }
    
    private class LaunchChatListener extends MouseAdapter {
        
        @Override
        public void mouseClicked(MouseEvent e) {
            JTable table = (JTable)e.getSource();
            
            ChatFriend chatFriend = getFriend(table, table.rowAtPoint(e.getPoint()));
            
            if (chatFriend == null) {
                return;
            }
            
            if (e.getClickCount() == 2 || chatFriend.isChatting()) {
                startOrSelectConversation(chatFriend);
                mouseHoverFriend.clearHoverDetails();
            }
        }
    }
    
    /**
     * Figures out which row  & friend the mouse is hovering over and asks the table
     * to repaint itself.  This information is needed by the cell renderer to decide
     * whether to paint a close chat icon at the right corner of the cell (if appropriate) 
     */
    private class CloseChatListener extends MouseAdapter {

        @Override
        public void mouseMoved(MouseEvent e) {
            ChatFriend friend = getFriendFromPoint(e);
            
            if (friend == null) {
                //Mouse is not hovering over a friend row
                ChatFriend previousMouseHoverFriend = mouseHoverFriend.getFriend();
                if (previousMouseHoverFriend != null) {
                    //Mouse used to be hovering over a friend, but has moved
                    int previousMouseHoverFriendIndex = chatFriends.indexOf(previousMouseHoverFriend);
                    if (previousMouseHoverFriendIndex > -1) {
                        //Friend is still in the list, but is no longer being hovered over
                        mouseHoverFriend.clearHoverDetails();
                        repaintTableCell(previousMouseHoverFriendIndex);
                    }
                }
                setTableCursor(false);
                return;
            }
            
            int friendIndex = chatFriends.indexOf(friend);
            if (friendIndex > -1) {
                mouseHoverFriend.setHoverDetails(friend, e.getPoint());
                repaintTableCell(friendIndex);
                setTableCursor(isOverCloseIcon(e.getPoint()) && friend.isChatting());
            }
        }
        
        private void repaintTableCell(int friendIndex) {
            AbstractTableModel model = (AbstractTableModel) friendsTable.getModel();
            model.fireTableCellUpdated(friendIndex, 0);
        }

        private ChatFriend getFriendFromPoint(MouseEvent e) {
            JTable table = (JTable)e.getSource();
            
            return getFriend(table, table.rowAtPoint(e.getPoint()));
        }

        /**
         * Clears the mouseHoverFriend when the mouse leaves the table
         */
        @Override
        public void mouseExited(MouseEvent e) {
            ChatFriend chatFriend = mouseHoverFriend.getFriend();
            if (chatFriend != null) {
                mouseHoverFriend.clearHoverDetails();
                int friendIndex = chatFriends.indexOf(chatFriend);
                repaintTableCell(friendIndex);
            }
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            if (isOverCloseIcon(e.getPoint())) {
                ChatFriend chatFriend = getFriendFromPoint(e);
                if (e.getClickCount() == 1 && chatFriend != null && chatFriend.isChatting()) {
                    fireCloseChat(chatFriend);
                    setTableCursor(false);
                }
            } 
        }
    }
    
    private static class FriendHoverBean {
        private WeakReference<ChatFriend> chatFriend;
        private Point hoverPoint;
        
        public void clearHoverDetails() {
            setHoverDetails(null, null);
        }
        
        public void setHoverDetails(ChatFriend chatFriend, Point point) {
            this.chatFriend = new WeakReference<ChatFriend>(chatFriend);
            this.hoverPoint = point;
        }

        public ChatFriend getFriend() {
            return chatFriend == null ? null : chatFriend.get();
        }
        
        public Point getHoverPoint() {
            return hoverPoint;
        }
    }
          
    private class OpenChat extends AbstractAction {
        private final ChatFriend chatFriend;

        public OpenChat(ChatFriend chatFriend) {
            super(I18n.tr("Open Conversation"));
            this.chatFriend = chatFriend;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            startOrSelectConversation(chatFriend);
        }
    }
    
    class BrowseFiles extends AbstractAction {
        private final Friend friend;
        
        public BrowseFiles(ChatFriend chatFriend) {
            super(I18n.tr("Browse Files"));
            setEnabled(chatFriend.isSignedInToLimewire());
            friend = chatFriend.getFriend();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
                remoteHostActions.get().viewFriendLibrary(friend);
        }
    }
   
    private class CloseChat extends AbstractAction {
        private ChatFriend chatFriend;

        public CloseChat(ChatFriend chatFriend) {
            super(I18n.tr("Close Conversation"));
            this.chatFriend = chatFriend;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            fireCloseChat(chatFriend);
        }
    }
    
    private class ShareNewList extends  AbstractAction {
        private final String friendID;
        public ShareNewList(String friendID) {
            super(I18n.tr("Share New List"));
            this.friendID = friendID;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final int id = sharedFileListManager.get().createNewSharedFileList(I18n.tr("Untitled"));
            SharedFileList list = sharedFileListManager.get().getModel().get(id);
            list.addFriend(friendID);
            library.get().selectAndRenameSharedList(list);
        }
    } 
    
    private class AddRemoveCheckBoxMenuItem extends JCheckBoxMenuItem {
        public AddRemoveCheckBoxMenuItem(final String friendID, final SharedFileList shareList) {            
            super(shareList.getCollectionName(), new ShareListIcons().getListIcon(shareList));
            
            setSelected(shareList.getFriendIds().contains(friendID));
            
            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                    
                    if(isSelected()){
                        shareList.addFriend(friendID);
                    } else {
                        shareList.removeFriend(friendID);
                    }
                    
                    setIcon(new ShareListIcons().getListIcon(shareList));
                }
            });
            
        }
        
    }


    private void setTableCursor(boolean useHandCursor) {
        friendsTable.setCursor(useHandCursor ? HAND_CURSOR : DEFAULT_CURSOR);
    }
}
