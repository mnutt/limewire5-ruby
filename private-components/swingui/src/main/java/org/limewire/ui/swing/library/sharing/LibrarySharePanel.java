package org.limewire.ui.swing.library.sharing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboPopup;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.LimeEditableComboBox;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.ShapeComponent;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.ui.swing.library.sharing.FriendShareEvent.ShareEventType;
import org.limewire.ui.swing.library.sharing.model.LibraryShareModel;
import org.limewire.ui.swing.painter.TextShadowPainter;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GlazedListsSwingFactory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.WritableTableFormat;
import ca.odell.glazedlists.impl.ThreadSafeList;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor.Event;
import ca.odell.glazedlists.swing.EventComboBoxModel;
import ca.odell.glazedlists.swing.EventTableModel;

/**
 * This is used internally by ShareWidget.  Needs refactoring.
 */
class LibrarySharePanel extends JXPanel implements Disposable, ShapeComponent {

    private static final int SHARED_ROW_COUNT = 10;        
    
    @Resource
    private int panelWidth;
    
    private JXPanel titlePanel;
    private JXPanel tablePanel;
    private JXPanel bottomPanel;
    @Resource
    private Color bottomPanelTopGradient;
    @Resource
    private Color bottomPanelBottomGradient;
    
    private GradientPaint bottomGradient;
    
    private LibraryShareModel shareModel;
    
    private JTextField inputField;

    private String startTypingText = I18n.tr("Start typing a friend's name");
    private String addFriendText = I18n.tr("Add another friend");

    private GnutellaFilteredList noShareListEDTOnly;  
    //wraps noShareList 
    private SortedList<SharingTarget> comboBoxListEDTOnly;
    
    //TODO is EventList necessary for shareFriendListEDTOnly?  Could be ArrayList?
    /** list of shared friends */
    private EventList<SharingTarget> shareFriendListEDTOnly;
    
     /** list of all friends */
    private SortedList<SharingTarget> allFriendsSortedThreadSafe;

    private JScrollPane shareScroll;

    private MouseableTable shareTable;
    private JComboBox friendCombo;
    private LimeEditableComboBox comboPanel;
    
    private boolean bottomPanelHidden = false;
    private boolean tableHidden = false;

    private JXLabel friendLabel;
    private JLabel topLabel;
    private JXLabel bottomLabel;
    private JLabel titleLabel;
    
    private JXPanel mainPanel;
    
    /**
     * arc for RoundedRect
     */
    @Resource
    private int arc = 10;

    private JButton closeButton;
    @Resource
    private Icon closeIcon;
    @Resource
    private Icon closeIconRollover;
    @Resource
    private Icon closeIconPressed;   
     
    
    private ShapeDialog dialog;
    
    private BasicComboPopup comboPopup;
    
   // private TextMatcherEditor<SharingTarget> textMatcher;
    
    @Resource
    private Icon removeIcon;
    @Resource
    private Icon removeIconRollover;
    @Resource
    private Icon removeIconPressed;   
    @Resource
    private int shareTableIndent = 15;   
    @Resource
    private Color dividerColor;
    @Resource
    private Color borderColor;
    @Resource
    private Color titleFontColor;
    
    private final List<ShareListener> shareListeners = new CopyOnWriteArrayList<ShareListener>();
    
    private Action up = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if(!friendCombo.isPopupVisible()){
                friendCombo.showPopup();
            } else {
                int selRow = friendCombo.getSelectedIndex();
                if (selRow > 0) {
                    selRow--;
                    friendCombo.setSelectedIndex(selRow);
                }
            }
        }
    };

    private Action down = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if(!friendCombo.isPopupVisible()){
                friendCombo.showPopup();
            } else {
                int selRow = friendCombo.getSelectedIndex();
                if (selRow < friendCombo.getModel().getSize() - 1) {
                    selRow++;
                    friendCombo.setSelectedIndex(selRow);
                }
            }
        }
    };    
    

    public LibrarySharePanel(ThreadSafeList<SharingTarget> allFriends, ShapeDialog dialog) {        
        this.dialog = dialog;
        initialize(allFriends);
    }
  
    
    private void initialize(ThreadSafeList<SharingTarget> allFriends){
        GuiUtils.assignResources(this);         
                
        initializeLists(allFriends);

        initializeShareTable();

        configureComboBox();
        
        initializeLabels();
        
        initializePanels();
        
        initializePainters();
        
        initializeButton();
        
        addComponents();
        
        setChildComponentKeyStrokes(this);
        
        addComponentListener(new ComponentAdapter(){

            @Override
            public void componentResized(ComponentEvent e) {
                if (friendCombo.isPopupVisible()) {
                    friendCombo.showPopup();
                }      
            }
       });
    }
  
    private void initializeButton() {
        closeButton = new IconButton(closeIcon, closeIconRollover, closeIconPressed);
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                dialog.setVisible(false);
            }
        });
    }


    private void initializePanels(){        
        
        setLayout(new MigLayout("nocache, fill, ins 0 0 0 0 , gap 0! 0!, novisualpadding"));
        setOpaque(false);
        
        mainPanel = new JXPanel(new MigLayout("nocache, fill, ins 0 6 0 6 , gap 0! 0!, novisualpadding"));
        mainPanel.setOpaque(false);

        titlePanel = new JXPanel(new MigLayout("nocache, fill, ins 0 0 0 0 , gap 0! 0!, novisualpadding"));
        titlePanel.setOpaque(false);
        
        tablePanel = new JXPanel(new MigLayout("nocache, fill, ins 0 0 0 0 , gap 0! 0!, novisualpadding"));
        tablePanel.setOpaque(false);
        
        bottomPanel = new JXPanel(new MigLayout("nocache, fill, ins 0 0 0 0 , gap 0! 0!, novisualpadding"));
        bottomPanel.setOpaque(false);

        mainPanel.setMinimumSize(new Dimension(panelWidth, 0));
        mainPanel.setMaximumSize(new Dimension(panelWidth, Integer.MAX_VALUE));
    }
    
    private void initializePainters(){
        //TODO: sizes and shapes
        
        setBackgroundPainter(new Painter() {
           

            @Override
            public void paint(Graphics2D g, Object object, int width, int height) {
                               
                Shape mainPanelShape = getShape();
                Area panelArea = new Area(mainPanelShape);
                Point bottomLocation = SwingUtilities.convertPoint(mainPanel, bottomPanel.getLocation(), LibrarySharePanel.this);
                Area bottomArea = new Area(new Rectangle2D.Float(0, bottomLocation.y, getWidth(), 1000));
               
                bottomArea.intersect(panelArea);
                
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_SPEED);
                
               
                
                g2.setColor(Color.WHITE);
                g2.fill(panelArea);        
                if (bottomPanel.isVisible()) {
                    bottomGradient = new GradientPaint(0, bottomLocation.y, bottomPanelTopGradient,
                            0, bottomLocation.y + bottomPanel.getHeight(),
                            bottomPanelBottomGradient);
                    g2.setPaint(bottomGradient);
                    g2.fill(bottomArea);
                    g2.setColor(dividerColor);
                    Rectangle panelBounds = mainPanelShape.getBounds();
                    g2.drawLine(panelBounds.x, bottomLocation.y, panelBounds.x + panelBounds.width, bottomLocation.y);
                }
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(2));
                g2.draw(mainPanelShape);
                g2.setStroke(new BasicStroke(1));
                g2.draw(mainPanelShape);
                g2.translate(1, 1);
                g2.draw(mainPanelShape);
                
                g2.dispose();
            }
        });        
        
    }

    @Override
    public Shape getShape() {
        return new RoundRectangle2D.Float(mainPanel.getLocation().x, mainPanel.getLocation().y, mainPanel.getWidth()-2,
                mainPanel.getHeight()-2, arc, arc);
    }
    
    private void initializeLabels() {
        titleLabel = new JLabel();
        FontUtils.bold(titleLabel);
        titleLabel.setForeground(titleFontColor);
        
        
        topLabel = new JLabel(I18n.tr("Currently sharing with"));
        friendLabel = new JXLabel(startTypingText);
        friendLabel.setForeground(Color.WHITE);
        TextShadowPainter textPainter = new TextShadowPainter();
        textPainter.setCacheable(false);
        friendLabel.setForegroundPainter(textPainter);
        
        Dimension labelSize = friendLabel.getPreferredSize().width > topLabel.getPreferredSize().width ? 
                friendLabel.getPreferredSize() : topLabel.getPreferredSize();
        friendLabel.setPreferredSize(labelSize);
        
       // topLabel.setPreferredSize(labelSize);
        
        bottomLabel = new MultiLineLabel("", panelWidth);
        bottomLabel.setForeground(Color.WHITE);
     //   bottomLabel.setForegroundPainter(new TextShadowPainter());
    }

    private void initializeLists(EventList<SharingTarget> allFriendsThreadSafe) {
        allFriendsSortedThreadSafe = GlazedListsFactory.sortedList(allFriendsThreadSafe, new SharingTargetComparator());
        
        shareFriendListEDTOnly = new BasicEventList<SharingTarget>();
        shareFriendListEDTOnly.addListEventListener(new ListEventListener<SharingTarget>() {
            @Override
            public void listChanged(ListEvent<SharingTarget> listChanges) {
                adjustFriendLabel();
            }
        });
        

        noShareListEDTOnly = new GnutellaFilteredList();
        comboBoxListEDTOnly = GlazedListsFactory.sortedList(noShareListEDTOnly, new SharingTargetComparator());
        
        // force list to re-sort when textMatcher is updated
        noShareListEDTOnly.addMatcherEditorListener(new MatcherEditor.Listener<SharingTarget>() {
            @Override
            public void changedMatcher(Event<SharingTarget> matcherEvent) {
                Comparator<SharingTarget> comparator = (comboPanel.getTextField().getText() == null || comboPanel.getTextField().getText().equals("")) ? 
                        new SharingTargetComparator() : new FilteredSharingTargetComparator();
                comboBoxListEDTOnly.getReadWriteLock().writeLock().lock();
                try {
                    comboBoxListEDTOnly.setComparator(comparator);
                } finally {
                    comboBoxListEDTOnly.getReadWriteLock().writeLock().unlock();
                }
            }
        });

    }

    private void addComponents() {
        
        titlePanel.add(titleLabel, "aligny top, alignx left");
        titlePanel.add(closeButton, "aligny top, alignx right");
        
        tablePanel.add(topLabel, "hidemode 3, wrap");        
        tablePanel.add(shareScroll, "gaptop 6, gapbottom 6, growx");
        
        bottomPanel.add(friendLabel, "gaptop 6, hidemode 3, wrap");
        bottomPanel.add(comboPanel, "growx, gaptop 4, gapbottom 6, hidemode 3, wrap");
        bottomPanel.add(bottomLabel, "growy, gapbottom 6, hidemode 3");
        

        mainPanel.add(titlePanel, "growx, gaptop 6, gapbottom 6, wrap");
        mainPanel.add(tablePanel, "growx, wrap, hidemode 3");
        mainPanel.add(bottomPanel, "growx, hidemode 3");
        
     //   add(mainPanel, "growx, gapleft " + BORDER_BUFFER + ", gapright " + BORDER_BUFFER + ", gaptop " + BORDER_BUFFER + ", gapbottom " + BORDER_BUFFER); 
        int inset = 0;//shadowMode == ShadowMode.SHADOW ? SHADOW_INSETS : 0;
        add(mainPanel, "alignx 50%, aligny 50%, gapleft " + inset + ", gapright " + inset + ", gaptop " + inset + ", gapbottom " + inset); 
    }

    private void configureComboBox() {
        comboPanel = new LimeEditableComboBox();
        comboPanel.setPreferredSize(new Dimension(230, 24));
        comboPanel.setMaximumSize(comboPanel.getPreferredSize());   
        comboPanel.setMinimumSize(comboPanel.getPreferredSize());
        friendCombo = comboPanel.getComboBox();
        friendCombo.setModel(GlazedListsSwingFactory.eventComboBoxModel(comboBoxListEDTOnly));
        initializeInputField();       
        
        comboPopup = comboPanel.getPopup();           
        // mouseClicked does not register on the combo box popup so we have to
        // use mousePressed and mouseReleased. (This is a workaround for
        // ActionEvents not being fired when the selected row is clicked)   
        MouseListener shareListener = new MouseAdapter() {
            private int pressedRow;
            @Override
            public void mousePressed(MouseEvent e) {
                pressedRow = comboPopup.getList().locationToIndex(e.getPoint());
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (pressedRow == comboPopup.getList().locationToIndex(e.getPoint())) {
                    shareSelectedFriend();
                }
            }
        };
        
        comboPopup.getList().addMouseListener(shareListener);
        comboPopup.getList().setCellRenderer(new FriendListCellRenderer());
    }

    //must be called after friendCombo is initialized
    private void initializeInputField() {        
        inputField = comboPanel.getTextField();
        
        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(friendCombo.getModel().getSize() == 0 || 
                        !friendCombo.isPopupVisible() && ("".equals(inputField.getText()) || inputField.getText() == null)){
                    dialog.setVisible(false);
                } else if (friendCombo.getSelectedIndex() >= 0) {
                    shareSelectedFriend();
                }
            }
        });
        
        
        inputField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
               update();
            }
            
            private void update() {
                noShareListEDTOnly.setFilterText(inputField.getText().split(" "));
                if (friendCombo.getModel().getSize() > 0) {
                    friendCombo.setSelectedIndex(0);
                    fixPopupSize();
                }
            }
            
        });
    }

    //ensures popup is correct size when list filter is relaxed
    private void fixPopupSize() {
        if (friendCombo.isShowing()) {
            friendCombo.hidePopup();
            friendCombo.showPopup();
        }
    }


    private void initializeShareTable() {
        final int actionCol = 0;
        final int friendNameCol = 1;
        shareTable = new MouseableTable(GlazedListsSwingFactory.eventTableModel(shareFriendListEDTOnly, new LibraryShareTableFormat(actionCol)));
        shareTable.setTableHeader(null);
        final ShareRendererEditor removeEditor = new ShareRendererEditor(removeIcon, removeIconRollover, removeIconPressed);
        removeEditor.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
               unshareFriend(removeEditor.getFriend());
               removeEditor.cancelCellEditing();
               inputField.requestFocusInWindow();
            }            
        });
        removeEditor.setOpaque(false);
        //do nothing ColorHighlighter eliminates default striping
        shareTable.setHighlighters(new ColorHighlighter());
        shareTable.setRowHeight(removeEditor.getPreferredSize().height + 3);
        shareTable.getColumnModel().getColumn(actionCol).setCellEditor(removeEditor);
        shareTable.getColumnModel().getColumn(actionCol).setPreferredWidth(removeEditor.getPreferredSize().width + 4);    
        shareTable.getColumnModel().getColumn(actionCol).setMaxWidth(removeEditor.getPreferredSize().width + 4);    
        shareTable.getColumnModel().getColumn(actionCol).setCellRenderer(new ShareRendererEditor(removeIcon, removeIconRollover, removeIconPressed));      
        shareTable.getColumnModel().getColumn(friendNameCol).setCellRenderer(new FocuslessTableCellRenderer());      
        shareTable.setShowGrid(false);   
        shareTable.setIntercellSpacing(new Dimension(0, 0));
        shareTable.setOpaque(false);
        shareTable.setColumnSelectionAllowed(false);
        shareTable.setRowSelectionAllowed(false);
        
        
        shareScroll = new JScrollPane(shareTable);
        shareScroll.setBorder(new EmptyBorder(0, shareTableIndent, 0, 0));
        shareScroll.setOpaque(false);
        shareScroll.getViewport().setOpaque(false);
    
    }

    
    public void setTitleLabel(String text){
        titleLabel.setText(text);
    }
    
    public void setTopLabel(String text){
        topLabel.setText(text);
    }
    

    public void setBottomLabel(String text){
        bottomLabel.setText(text);
    }
    

    private void adjustFriendLabel() {
        friendLabel.setText(shareFriendListEDTOnly.size() == 0 ? startTypingText : addFriendText);
        friendLabel.paintImmediately(friendLabel.getVisibleRect());
    }
    
    private void shareSelectedFriend() {
        int index = friendCombo.getSelectedIndex();
        if (index > -1) {
            shareFriend((SharingTarget)friendCombo.getSelectedItem());
            inputField.setText(null);
            resetRowSelection(index);

            if (friendCombo.getModel().getSize() > 0) {
                friendCombo.showPopup();
            }
        }
    }
    
    public void setComboBoxVisible(boolean visible){
        bottomPanel.setVisible(visible);
        bottomPanelHidden = !visible;
    }
    

    public void setTableVisible(boolean visible) {
        tableHidden = !visible;
        tablePanel.setVisible(visible);
    }

    private void setShareModel(LibraryShareModel shareModel){
        this.shareModel = shareModel;
    }
    
    private void setChildComponentKeyStrokes(JComponent component){
        setKeyStrokes(component);
        for(Component child : component.getComponents()){
            if (child instanceof JComponent) {
                setChildComponentKeyStrokes((JComponent) child);                
            }
        }
    }
    
    private void setKeyStrokes(JComponent component) {
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up");
        component.getActionMap().put("up", up);
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down");
        component.getActionMap().put("down", down);       
    }

    
    public void show(final Component owner, LibraryShareModel shareModel) {        
        dialog.show(new LoadingPanel(), owner, false);
        
        SwingWorker setupWorker = new SetupWorker(shareModel);
        
        setupWorker.execute();

    }
    

    @Override
    public boolean contains(Point p) {
        return super.contains(p) || comboPopup.contains(SwingUtilities.convertPoint(this, p, comboPopup));
    }


    private void resetRowSelection(int oldSelRow) {
        if(friendCombo.getModel().getSize() == 0){
            return;
        }
        
        if(oldSelRow == -1){
            friendCombo.setSelectedIndex(0);
        } else if(friendCombo.getModel().getSize() > oldSelRow){
            friendCombo.setSelectedIndex(oldSelRow);
        } else if ((oldSelRow - 1) >= 0 && (oldSelRow - 1) < friendCombo.getModel().getSize()) {
            friendCombo.setSelectedIndex(oldSelRow - 1);
        }
    }
    
    
    private void shareFriend(SharingTarget friend) {
        shareFriendListEDTOnly.add(friend);
        noShareListEDTOnly.remove(friend);

        shareModel.shareFriend(friend);
        
        adjustSize();
        if (shareScroll.getVerticalScrollBar().isShowing()){
            shareTable.ensureRowVisible(shareTable.getRowCount() - 1);
        }
        //bit heavy handed here but it ensures that changes are reflected everywhere
        GuiUtils.getMainFrame().repaint();
        
        handleShareChanged(friend.getFriend(), ShareEventType.SHARE);
    }
    
    
    private void unshareFriend(SharingTarget friend) {
        shareFriendListEDTOnly.remove(friend);
        noShareListEDTOnly.add(friend);
        
        shareModel.unshareFriend(friend);
        
        adjustSize();
      //bit heavy handed here but it ensures that changes are reflected everywhere
        GuiUtils.getMainFrame().repaint();
        
        handleShareChanged(friend.getFriend(), ShareEventType.UNSHARE);
    }
    
    private void adjustSize(){
        adjustFriendLabel();
        bottomPanel.setVisible(!bottomPanelHidden && noShareListEDTOnly.getUnfilteredSize() > 0);
        if (!tableHidden ) {
            int visibleRows = (shareTable.getRowCount() < SHARED_ROW_COUNT) ? shareTable.getRowCount() : SHARED_ROW_COUNT;
            shareTable.setVisibleRowCount(visibleRows);
            tablePanel.setVisible(visibleRows > 0);

            //prevent flashing scrollbar
            int scrollPolicy = shareTable.getRowCount() > visibleRows ? JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED : JScrollPane.VERTICAL_SCROLLBAR_NEVER;
            shareScroll.setVerticalScrollBarPolicy(scrollPolicy);
        }
        setSize(getPreferredSize());  
    }
    
    
    
    public void dispose() {
        noShareListEDTOnly.dispose();
        allFriendsSortedThreadSafe.dispose();
        
        ((EventTableModel)shareTable.getModel()).dispose();
        ((EventComboBoxModel)comboPanel.getComboBox().getModel()).dispose();
    }
    
            
    private static class LibraryShareTableFormat implements WritableTableFormat<SharingTarget> {
        private int editColumn;
        
        public LibraryShareTableFormat(int editedColumn){
            this.editColumn = editedColumn;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return null;
        }

        @Override
        public Object getColumnValue(SharingTarget baseObject, int column) {
            if(column == editColumn){
                return baseObject;
            }
            return baseObject.getFriend().getRenderName();
        }

        @Override
        public boolean isEditable(SharingTarget baseObject, int column) {
            return true;
        }

        @Override
        public SharingTarget setColumnValue(SharingTarget baseObject, Object editedValue, int column) {
            return baseObject;
        }

    }
    
    private static class SharingTargetComparator implements Comparator<SharingTarget>{

        @Override
        public int compare(SharingTarget o1, SharingTarget o2) {
            if (o1 == o2){
                return 0;
            }
            if(o1.isGnutellaNetwork()){
                return -1;
            }
            if(o2.isGnutellaNetwork()){
                return 1;
            }
            return o1.getFriend().getRenderName().compareToIgnoreCase(o2.getFriend().getRenderName());
        }
        
    }
    
    private static class FilteredSharingTargetComparator implements Comparator<SharingTarget>{

        @Override
        public int compare(SharingTarget o1, SharingTarget o2) {
            if (o1 == o2){
                return 0;
            }
            if(o1.isGnutellaNetwork()){
                return 1;
            }
            if (o2.isGnutellaNetwork()) {
                    return -1;
            }
            return o1.getFriend().getRenderName().compareToIgnoreCase(o2.getFriend().getRenderName());
        }
        
    }

    //Need to ensure 
    private class SetupWorker extends SwingWorker<Void, Void> {
        private final List<SharingTarget> sharedFriendsTemp = Collections.synchronizedList(new ArrayList<SharingTarget>());
        private final List<SharingTarget> noShareFriendsTemp = Collections.synchronizedList(new ArrayList<SharingTarget>());
        private final LibraryShareModel setupShareModel;
        
        public SetupWorker(LibraryShareModel shareModel){
            this.setupShareModel = shareModel;
        }
        
        @Override
        protected Void doInBackground() throws Exception {
            reloadSharedFriendsInBackground();
            return null;
        }

        @Override
        protected void done() {
            shareFriendListEDTOnly.clear();
            noShareListEDTOnly.clear();
            
            shareFriendListEDTOnly.addAll(sharedFriendsTemp);
            //CompositeList doesn't support addAll
            for (SharingTarget target : noShareFriendsTemp) {
                noShareListEDTOnly.add(target);
            }
            
            setShareModel(setupShareModel);
                        
            inputField.setText(null);
            adjustSize();
            if (friendCombo.getModel().getSize() > 0) {
                friendCombo.setSelectedIndex(0);
            }
                        
            dialog.setVisible(false);
            dialog.show(LibrarySharePanel.this, null, true);
            inputField.requestFocusInWindow();
        }
        
        
        private void reloadSharedFriendsInBackground() {           

            if (setupShareModel.isGnutellaNetworkSharable()) {
                loadFriendInBackground(SharingTarget.GNUTELLA_SHARE);
            }

            allFriendsSortedThreadSafe.getReadWriteLock().readLock().lock();
            try {
                for (SharingTarget target : allFriendsSortedThreadSafe) {
                    loadFriendInBackground(target);
                }
            } finally {
                allFriendsSortedThreadSafe.getReadWriteLock().readLock().unlock();
            }
        }
        
        private void loadFriendInBackground(SharingTarget friend) {
            if (setupShareModel.isShared(friend)) {
                sharedFriendsTemp.add(friend);
            } else {
                noShareFriendsTemp.add(friend);
            }
        }  
    };
    
    public int getShareCount(){
        return shareFriendListEDTOnly.size();
    }
    
    public int getNoShareCount(){
        return noShareListEDTOnly.size();
    }
    
    public void addShareListener(ShareListener listener){
        shareListeners.add(listener);
    }
    
    public void removeShareListener(ShareListener listener){
        shareListeners.remove(listener);
    }
    
    private void handleShareChanged(Friend friend, ShareEventType type){
        FriendShareEvent event = new FriendShareEvent(friend, type);
        for(ShareListener listener : shareListeners){
            listener.sharingChanged(event);
        }
        
    }
          
}
