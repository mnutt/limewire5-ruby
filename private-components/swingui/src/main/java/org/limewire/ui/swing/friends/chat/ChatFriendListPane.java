package org.limewire.ui.swing.friends.chat;

import static org.limewire.ui.swing.friends.chat.ChatFriendsUtil.getIcon;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
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
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.action.ItemNotifyable;
import org.limewire.ui.swing.action.PopupDecider;
import org.limewire.ui.swing.action.PopupUtil;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.RuntimeTopicPatternEventSubscriber;
import org.limewire.ui.swing.friends.chat.Message.Type;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.table.AbstractTableFormat;
import org.limewire.ui.swing.util.GlazedListsSwingFactory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.EventTableModel;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The pane that lists all available friends in the chat area.
 */
@Singleton
public class ChatFriendListPane extends JPanel {
    
    private static final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);
    private static final Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);
    private static final int PREFERRED_WIDTH = 122;
    private static final int LEFT_EDGE_PADDING_PIXELS = 5;
    private static final Log LOG = LogFactory.getLog(ChatFriendListPane.class);
    private static final String ALL_CHAT_MESSAGES_TOPIC_PATTERN = MessageReceivedEvent.buildTopic(".*");
    
    private final EventList<ChatFriend> chatFriends;
    private final JTable friendsTable;
    private final IconLibrary icons;
    private final Map<String, ChatFriend> idToFriendMap;
    private final WeakHashMap<ChatFriend, AlternatingIconTimer> friendTimerMap;
    private final LibraryNavigator libraryNavigator;
    private final JScrollPane scrollPane;

    private String myID;
    private WeakReference<ChatFriend> activeConversation = new WeakReference<ChatFriend>(null);
    private FriendHoverBean mouseHoverFriend = new FriendHoverBean();
    @Resource(key="ChatFriendList.rightEdgeBorderColor") private Color rightBorderColor;
    @Resource(key="ChatFriendList.conversationsSeparatorColor") private Color conversationsSeparatorColor;
    @Resource(key="ChatFriendList.friendColor") private Color friendColor;
    @Resource(key="ChatFriendList.friendFont") private Font friendFont;
    @Resource(key="ChatFriendList.friendSelectionColor") private Color friendSelectionColor;
    @Resource(key="ChatFriendList.activeConversationBackgroundColor") private Color activeConversationBackgroundColor;

    @Inject
    public ChatFriendListPane(IconLibrary icons, 
            LibraryNavigator libraryNavigator,
            ListenerSupport<FriendPresenceEvent> presenceSupport) {
        super(new BorderLayout());
        this.icons = icons;
        this.chatFriends = new BasicEventList<ChatFriend>();
        this.idToFriendMap = new HashMap<String, ChatFriend>();
        this.friendTimerMap = new WeakHashMap<ChatFriend, AlternatingIconTimer>();
        this.libraryNavigator = libraryNavigator;
        
        GuiUtils.assignResources(this);
        
        ObservableElementList<ChatFriend> observableList = GlazedListsFactory.observableElementList(chatFriends, GlazedLists.beanConnector(ChatFriend.class));
        SortedList<ChatFriend> sortedFriends = GlazedListsFactory.sortedList(observableList,  new FriendAvailabilityComparator());
        friendsTable = createFriendsTable(sortedFriends);

        addPopupMenus(friendsTable);
        
        setBorder(new DropShadowBorder(rightBorderColor, 1, 1.0f, 0, false, false, false, true));
        
        scrollPane = new JScrollPane(friendsTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane);
        setPreferredSize(new Dimension(PREFERRED_WIDTH, 200));
        
        presenceSupport.addListener(new EventListener<FriendPresenceEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendPresenceEvent event) {
                handlePresenceEvent(event);
            }
        });
        
        EventAnnotationProcessor.subscribe(this);
    }
    
    @Inject void register(ListenerSupport<XMPPConnectionEvent> connectionSupport) {
        connectionSupport.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
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

    private void addPopupMenus(JComponent comp) {
        FriendContext context = new FriendContext();
        ViewLibrary viewLibrary = new ViewLibrary(context);
        ViewSharedFiles viewSharedFiles = new ViewSharedFiles(context);
        JPopupMenu nonChattingPopup = PopupUtil.addPopupMenus(comp, new FriendPopupDecider(false, context), new OpenChat(context));
        nonChattingPopup.addSeparator();
        nonChattingPopup.add(viewLibrary);
        nonChattingPopup.add(viewSharedFiles);
       
        JPopupMenu chattingPopup = PopupUtil.addPopupMenus(comp, new FriendPopupDecider(true, context), viewLibrary, viewSharedFiles);
        chattingPopup.addSeparator();
        chattingPopup.add(new CloseChat(context));
    }
    
    private static class FriendPopupDecider implements PopupDecider {
        private final boolean expected;
        private final FriendContext context;
        
        public FriendPopupDecider(boolean expected, FriendContext context) {
            this.expected = expected;
            this.context = context; 
        }

        @Override
        public boolean shouldDisplay(MouseEvent e) {
            JTable table = (JTable) e.getComponent();
            int index = table.rowAtPoint(e.getPoint());
            if (index < 0) {
                return false;
            }
            //Popup selects the item (as per spec)
            table.getSelectionModel().setSelectionInterval(index, index);
            
            ChatFriend chatFriend = getFriend(table, index);
            
            context.setFriend(chatFriend);
            return chatFriend.isChatting() == expected;
        }
    }
    
    private static ChatFriend getFriend(JTable table, int index) {
        EventTableModel model = (EventTableModel)table.getModel();
        return index < 0 ? null : (ChatFriend) model.getElementAt(index);
    }
    
    private JTable createFriendsTable(final EventList<ChatFriend> friendsList) {
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
        
        final JXTable table = new CustomTooltipLocationTable(GlazedListsSwingFactory.eventTableModel(friendsList, format)); 
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
                
                editor.setFilterText(keysPressed.toArray(new String[0]));
                
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
                EventTableModel model = (EventTableModel) table.getModel();
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
    
    private void handlePresenceEvent(FriendPresenceEvent event) {
        LOG.debugf("handling presence event {0}", event);
        FriendPresence presence = event.getSource();
        Friend friend = presence.getFriend();
        ChatFriend chatFriend = idToFriendMap.get(friend.getId());
        switch(event.getType()) {
        case ADDED:
        case UPDATE:
            if(chatFriend == null) {
                chatFriend = new ChatFriendImpl((Presence)presence, myID);
                chatFriends.add(chatFriend);
                idToFriendMap.put(friend.getId(), chatFriend);
            }
            chatFriend.update();
            break;
        case REMOVED:
            if (chatFriend != null) {
                if (shouldRemoveFromFriendsList(chatFriend)) {
                    chatFriends.remove(idToFriendMap.remove(friend.getId()));
                }
                chatFriend.update();
            }
            break;
        }
    }

    @RuntimeTopicPatternEventSubscriber(methodName="getMessagingTopicPatternName")
    public void handleMessageReceived(String topic, MessageReceivedEvent event) {
        Message message = event.getMessage();
        LOG.debugf("All Messages listener: from {0} text: {1} topic: {2}", message.getSenderName(), message.toString(), topic);
        ChatFriend chatFriend = idToFriendMap.get(message.getFriendID());
        if (!chatFriend.isActiveConversation() && message.getType() != Type.Sent) {
            chatFriend.setReceivingUnviewedMessages(true);
            if (!friendTimerMap.containsKey(chatFriend)) {
                AlternatingIconTimer iconTimer = new AlternatingIconTimer(chatFriend);
                friendTimerMap.put(chatFriend, iconTimer);
                iconTimer.start();
            }
        }
    }

    /**
     * Remove from the friends list only when:
     *
     * 1. The user (buddy) associated with the chatfriend is no longer signed in, AND
     * 2. The chat has been closed (by clicking on the "x" on the friend in the friend's list)
     *
     * @param chatFriend the ChatFriend to decide whether to remove (no null check)
     * @return true if chatFriend should be removed.
     */
    private boolean shouldRemoveFromFriendsList(ChatFriend chatFriend) {
        return (!chatFriend.isChatting()) && (!chatFriend.isSignedIn());
    }
    
    public String getMessagingTopicPatternName() {
        return ALL_CHAT_MESSAGES_TOPIC_PATTERN;
    }
    
    public void setLoggedInID(String id) {
        this.myID = id;
    }

    String getLoggedInID() {
        return myID;
    }
    
    public void fireConversationStarted(String friendId) {
        ChatFriend chatFriend = idToFriendMap.get(friendId);
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
            writerWithEventDispatch = new MessageWriterImpl(myID, chatFriend, writer);
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
        chatFriend.setReceivingUnviewedMessages(false);
        chatFriend.setActiveConversation(true);
        friendTimerMap.remove(chatFriend);
    }
    
    private Icon getChatIcon(ChatFriend chatFriend) {
        AlternatingIconTimer timer = friendTimerMap.get(chatFriend);
        if (timer != null) {
            return timer.getIcon();
        }
        //Change to chatting icon because gtalk doesn't actually set mode to 'chat', so icon won't show chat bubble normally
        if (chatFriend.isChatting()) {
            return icons.getChatting();
        }
        return getIcon(chatFriend, icons);
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
                chatFriends.remove(idToFriendMap.remove(chatFriend.getID()));
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
            if (idToFriendMap.get(userId) != null) {
                idToFriendMap.remove(userId);
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
                chatStatus.setIcon(overCloseIcon ? icons.getEndChatOverIcon() : icons.getEndChat());
                chatStatus.setToolTipText(tr("Close conversation"));
                panel.add(chatStatus, "gapleft 4, gapright 2");
            } else {
                chatStatus.setIcon(getChatIcon(chatFriend));
                chatStatus.setToolTipText(null);
                panel.add(chatStatus);
            }
            friendName.setText(value.toString());
            panel.add(friendName);
        }
    }
    
    private class CustomTooltipLocationTable extends JXTable {
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
            
            EventTableModel model = (EventTableModel) friendsTable.getModel();
            ChatFriend chatFriend = (ChatFriend) model.getElementAt(row);
            
            if (chatFriend.isChatting() && isOverCloseIcon(mousePoint)) {
                return tr("Close conversation");
            }
            
            StringBuilder tooltip = new StringBuilder();
            tooltip.append("<html>")
                .append("<head>")
                .append("<style>body { margin: 2px 10px 2px 4px;}</style>")
                .append("</head>")
                .append("<body>")
                .append("<img src=\"")
                .append(ChatFriendsUtil.getIconURL(chatFriend.getMode())).append("\"/>&nbsp;")
                .append("<b>").append(chatFriend.getName()).append("</b><br/>");
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
        return point.x > LEFT_EDGE_PADDING_PIXELS && point.x < icons.getEndChat().getIconWidth() + LEFT_EDGE_PADDING_PIXELS;
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
            return flashCount % 2 == 0 ? icons.getUnviewedMessages() : icons.getChatting();
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
    
    private class FriendContext {
        private WeakReference<ChatFriend> weakFriend;
        
        public ChatFriend getFriend() {
            return weakFriend == null ? null : weakFriend.get();
        }
        
        public void setFriend(ChatFriend chatFriend) {
            weakFriend = new WeakReference<ChatFriend>(chatFriend);
        }
    }
    
    private static abstract class AbstractContextAction extends AbstractAction {
        protected final FriendContext context;
        
        public AbstractContextAction(String name, FriendContext context) {
            super(name);
            this.context = context;
        }
    }
    
    private class OpenChat extends AbstractContextAction {
        public OpenChat(FriendContext context) {
            super(I18n.tr("Open Chat"), context);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ChatFriend chatFriend = context.getFriend();
            if (chatFriend != null) {
                startOrSelectConversation(chatFriend);
            }
        }
    }
    
    class ViewLibrary extends AbstractContextAction implements ItemNotifyable {
        public ViewLibrary(FriendContext context) {
            super("", context);
        }
                
        @Override
        public void notifyItem(JMenuItem item) {
            ChatFriend chatFriend = context.getFriend();
            
            if (chatFriend == null) {
                return;
            }
            
            item.setText(I18n.tr("View Files"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ChatFriend chatFriend = context.getFriend();
            if (chatFriend != null) {
                libraryNavigator.selectFriendLibrary(chatFriend.getFriend());
            }
        }
    }
    
    private class ViewSharedFiles extends AbstractContextAction implements ItemNotifyable {
        public ViewSharedFiles(FriendContext context) {
            super("", context);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ChatFriend chatFriend = context.getFriend();
            if (chatFriend != null) {
                libraryNavigator.selectFriendShareList(chatFriend.getFriend());
            }
        }

        @Override
        public void notifyItem(JMenuItem item) {
            ChatFriend chatFriend = context.getFriend();
            
            if (chatFriend == null) {
                return;
            }
            
            item.setText(tr("Share"));
        }
    }
    
    private class CloseChat extends AbstractContextAction {
        public CloseChat(FriendContext context) {
            super(I18n.tr("Close chat"), context);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ChatFriend chatFriend = context.getFriend();
            fireCloseChat(chatFriend);
        }
    }

    private void setTableCursor(boolean useHandCursor) {
        friendsTable.setCursor(useHandCursor ? HAND_CURSOR : DEFAULT_CURSOR);
    }
}
