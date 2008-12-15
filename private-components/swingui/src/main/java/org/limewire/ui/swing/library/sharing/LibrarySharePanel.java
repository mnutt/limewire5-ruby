package org.limewire.ui.swing.library.sharing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ComboBoxEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.ComboPopup;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.ui.swing.components.ShapeDialogComponent;
import org.limewire.ui.swing.library.Disposable;
import org.limewire.ui.swing.library.sharing.model.LibraryShareModel;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.WritableTableFormat;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.EventComboBoxModel;
import ca.odell.glazedlists.swing.EventTableModel;

/**
 * This is used internally by ShareWidget.  Needs refactoring.
 */
class LibrarySharePanel extends JXPanel implements PropertyChangeListener, Disposable, ShapeDialogComponent {

    private static final int SHARED_ROW_COUNT = 20;
    private static final int BORDER_INSETS = 10;
  //  private static final int HGAP = 5;
    //TODO: move this to properties
    private static final int WIDTH = 300;
    
    private JXPanel titlePanel;
    private JXPanel topPanel;
    private JXPanel bottomPanel;
    
    private LibraryShareModel shareModel;
    
    private JTextField inputField;

    /**
     * all unshared friends
     */    
    private EventList<SharingTarget> noShareFriendList;
    /**
     * filtered list of unshared friends
     */
    private FilterList<SharingTarget> noShareFilterList;
    /**
     * list of shared friends
     */
    private EventList<SharingTarget> shareFriendList;
    
    /** list of all friends */
    private Collection<Friend> allFriends;

    private JScrollPane shareScroll;

    private JXTable shareTable;
    private JComboBox friendCombo;
    private JXPanel comboPanel;

    private JLabel friendLabel;
    private JLabel topLabel;
    private JLabel bottomLabel;
    private JLabel titleLabel;
    
    private JXPanel mainPanel;
    private JButton closeButton;
    
   // private ShapePainter shapePainter;    
    
    private ShapeDialog dialog;
    
    private ComboPopup comboPopup;
    
    private TextMatcherEditor<SharingTarget> textMatcher;
    
    @Resource
    private Icon removeIcon;
    @Resource
    private Icon removeIconRollover;
    @Resource
    private Icon removeIconPressed;   
    @Resource
    private int shareTableIndent = 15;
    
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
                if (selRow < noShareFilterList.size() - 1) {
                    selRow++;
                    friendCombo.setSelectedIndex(selRow);
                }
            }
        }
    };
    
    
    public LibrarySharePanel(Collection<Friend> allFriends, ShapeDialog dialog) {
        this.allFriends = allFriends;
        this.dialog = dialog;
        initialize();
    }
  
    
    private void initialize(){
        GuiUtils.assignResources(this);        
        
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        
//        shapePainter = new ShapePainter();
//        shapePainter.setFillPaint(getBackground());        
//        shapePainter.setBorderPaint(Color.BLACK);
//        shapePainter.setBorderWidth(2);
//        shapePainter.setHorizontalAlignment(HorizontalAlignment.LEFT);
//        shapePainter.setVerticalAlignment(VerticalAlignment.TOP);
//        setBackgroundPainter(shapePainter);
    //    setBorder(new LineBorder(Color.BLACK));
        


        initializeLists();

        initializeShareTable();

        initializeComboBox();
        
        initializeLabels();
        
        initializePanels();
        
        initializeButton();
        
        addComponents();
        
        setKeyStrokes(this);
        setKeyStrokes(mainPanel);
        setKeyStrokes(inputField); 
     
        addComponentListener(); 
    }
  
    private void initializeButton() {
        closeButton = new JButton("X");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                dialog.setVisible(false);
            }
        });
    }


    private void initializePanels(){
        //TODO use correct colors, gradients and move to properties
        mainPanel = new JXPanel(new MigLayout("nocache, fill, ins 0 0 0 0 , gap 0! 0!, novisualpadding"));
        mainPanel.setOpaque(false);

        titlePanel = new JXPanel(new MigLayout("nocache, fill, ins 0 0 0 0 , gap 0! 0!, novisualpadding"));
       // titlePanel.setBackground(Color.WHITE);
        titlePanel.setOpaque(false);
        
        topPanel = new JXPanel(new BorderLayout());
        topPanel.setOpaque(false);
        
        bottomPanel = new JXPanel(new MigLayout("nocache, fill, ins 0 0 0 0 , gap 0! 0!, novisualpadding"));
      //  bottomPanel.setBackground(Color.GRAY);
        bottomPanel.setOpaque(false);
        
        //TODO: sizes and shapes
        setBackgroundPainter(new Painter() {
            @Override
            public void paint(Graphics2D g, Object object, int width, int height) {
                int arc = 10;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
//                g2.setColor(new Color(0, 0, 0, 125));
//                g2.fillRoundRect(0, 0, width, height, arc, arc);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(mainPanel.getLocation().x, mainPanel.getLocation().y, mainPanel.getWidth(),
                        mainPanel.getHeight() + 2, arc, arc);
                g2.setColor(Color.BLACK);
                g2.drawRoundRect(mainPanel.getLocation().x, mainPanel.getLocation().y, mainPanel.getWidth(),
                        mainPanel.getHeight() + 2, arc, arc);
                g2.dispose();
            }
        });
    }

    private void initializeLabels() {
        titleLabel = new JLabel();
        FontUtils.bold(titleLabel);
        topLabel = new JLabel(I18n.tr("Currently sharing with"));
        friendLabel = new JLabel(I18n.tr("Start typing a friend's name"));
        friendLabel.setForeground(Color.WHITE);
        
        Dimension labelSize = friendLabel.getPreferredSize().width > topLabel.getPreferredSize().width ? 
                friendLabel.getPreferredSize() : topLabel.getPreferredSize();
        friendLabel.setPreferredSize(labelSize);
        
       // topLabel.setPreferredSize(labelSize);
        
        bottomLabel = new MultiLineLabel("", WIDTH);
        bottomLabel.setForeground(Color.WHITE);
    }


    private void addComponentListener() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (isVisible()) {
                    dialog.revalidate();
                }
            }

            @Override
            public void componentShown(ComponentEvent e) {
                System.out.println("shown");
                inputField.requestFocusInWindow();
            }
        });
    }

    private void initializeLists() {
        shareFriendList = GlazedListsFactory.sortedList(new BasicEventList<SharingTarget>(), new SharingTargetComparator());
        shareFriendList.addListEventListener(new ListEventListener<SharingTarget>() {
            @Override
            public void listChanged(ListEvent<SharingTarget> listChanges) {
                adjustFriendLabelVisibility();
            }
        });

        
        noShareFriendList = GlazedListsFactory.sortedList(new BasicEventList<SharingTarget>(), new SharingTargetComparator());
        
      //using TextComponentMatcherEditor would cause problems because it also uses DocumentListener so we 
        //have no guarantee about the order of sorting and selecting
        TextFilterator<SharingTarget> textFilter = new TextFilterator<SharingTarget>() {
            @Override
            public void getFilterStrings(List<String> baseList, SharingTarget element) {
                baseList.add(element.getFriend().getName());
                baseList.add(element.getFriend().getId());
            }
        };
        textMatcher = new TextMatcherEditor<SharingTarget>(textFilter);
        noShareFilterList = GlazedListsFactory.filterList(noShareFriendList, textMatcher);        

    }

    private void addComponents() {
//        GridBagConstraints gbc = new GridBagConstraints();//        mainPanel.setLayout(new GridBagLayout());
//        
//        gbc.weightx = 1.0;
//        gbc.weighty = 0;
//        gbc.fill = GridBagConstraints.BOTH;
//        gbc.insets = new Insets(0, HGAP, 0, HGAP);
//
//        gbc.gridy = 0;
//        gbc.weighty = 0;
//        mainPanel.add(titleLabel, gbc);
//        
//        gbc.gridy++;
//        gbc.weighty = 0;
//        mainPanel.add(topLabel, gbc);
//        
//        gbc.gridy++;
//        gbc.weighty = 1.0;
//        mainPanel.add(shareScroll, gbc);
//       
//        gbc.gridy++;
//        mainPanel.add(friendLabel, gbc);
//        
//        gbc.gridy++;
//        gbc.weighty = 1.0;
//        mainPanel.add(friendCombo, gbc);
//
//        gbc.gridy++;
//        mainPanel.add(bottomLabel, gbc);
        
        titlePanel.add(titleLabel, "hidemode 3, wrap");
        titlePanel.add(topLabel, "hidemode 3");
        titlePanel.add(closeButton, "dock east, aligny top");
        
        topPanel.add(shareScroll);
        
        bottomPanel.add(friendLabel, "wrap");
        bottomPanel.add(comboPanel, "growx, gap left 5, gap right 5, wrap");
        bottomPanel.add(bottomLabel);
        

        mainPanel.add(titlePanel, "growx, wrap");
        mainPanel.add(topPanel, "growx, wrap");
        mainPanel.add(bottomPanel, "growx");
        
        add(mainPanel, "growx, gap left " + BORDER_INSETS + ", gap right " + BORDER_INSETS + ", gap above " + BORDER_INSETS + ", gap below " + BORDER_INSETS); 
    }

    private void initializeComboBox() {
        friendCombo = new JComboBox(new EventComboBoxModel<SharingTarget>(noShareFilterList));
        initializeInputField();
        ShareComboBoxUI comboUI = new ShareComboBoxUI(); 
        
        friendCombo.setUI(comboUI);
        friendCombo.setEditor(new ShareComboBoxEditor());
        friendCombo.setEditable(true);
        friendCombo.setOpaque(false);
        friendCombo.setBorder(new EmptyBorder(0, 0, 0, 0));
        
        comboPopup = comboUI.getPopup();     
        
        comboPanel = new JXPanel(new MigLayout("nocache, fill, ins 0 0 0 0 , gap 0! 0!, novisualpadding"));
        comboPanel.setOpaque(false);
        comboPanel.setBackgroundPainter(new Painter() {
            public void paint(Graphics2D g, Object object, int width, int height) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, width - 1, height - 1, 5, height - 1);
                g2.setColor(Color.BLACK);
                g2.drawRoundRect(0, 0, width - 1, height - 1, 5, height - 1);
                g2.dispose();
            }
        });
        
        comboPanel.add(friendCombo, "gap left 3, growx");
        
 
        
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
    }

    //must be called after friendCombo is initialized
    private void initializeInputField() {
        inputField = new JTextField(12);
        inputField.setBorder(new EmptyBorder(0, 0, 0, 0));
        inputField.setOpaque(false);
        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(noShareFilterList.size() == 0 || 
                        !friendCombo.isPopupVisible() && ("".equals(inputField.getText()) || inputField.getText() == null)){
                    dialog.setVisible(false);
                } else if (friendCombo.getSelectedIndex() >= 0) {
                    shareSelectedFriend();
                    friendCombo.hidePopup();
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
                textMatcher.setFilterText(inputField.getText().split(" "));
                if (noShareFilterList.size() > 0) {
                    friendCombo.setSelectedIndex(0);
                    if (isVisible()) {
                        friendCombo.showPopup();
                    }
                }
            }
            
        });
    }

    private void initializeShareTable() {
        final int actionCol = 0;
        shareTable = new MouseableTable(new EventTableModel<SharingTarget>(shareFriendList, new LibraryShareTableFormat(actionCol)));
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
        //do nothing ColorHighlighter eliminates default striping
        shareTable.setHighlighters(new ColorHighlighter());
        shareTable.setRowHeight(removeEditor.getPreferredSize().height);
        shareTable.getColumnModel().getColumn(actionCol).setCellEditor(removeEditor);
        shareTable.getColumnModel().getColumn(actionCol).setPreferredWidth(removeEditor.getPreferredSize().width);    
        shareTable.getColumnModel().getColumn(actionCol).setMaxWidth(removeEditor.getPreferredSize().width);    
        shareTable.getColumnModel().getColumn(actionCol).setCellRenderer(new ShareRendererEditor(removeIcon, removeIconRollover, removeIconPressed));      
        shareTable.setShowGrid(false);       
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
    

    private void adjustFriendLabelVisibility() {
        friendLabel.setVisible(shareFriendList.size() == 0 && friendCombo.isVisible());
    }
    
    private void shareSelectedFriend() {
        int index = friendCombo.getSelectedIndex();
        if (index > -1) {
            shareFriend(noShareFilterList.get(index));
            if (noShareFilterList.size() == 0) {
                inputField.setText(null);
            }
            resetRowSelection(index);
        }
    }
    
    public void setComboBoxVisible(boolean visible){
        friendCombo.setVisible(visible);
    }

    public void setShareModel(LibraryShareModel shareModel){
        this.shareModel = shareModel;
        shareModel.addPropertyChangeListener(this);
    }
    
    public LibraryShareModel getShareModel(){
        return shareModel;
    }

    
    private void setKeyStrokes(JComponent component) {
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up");
        component.getActionMap().put("up", up);
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down");
        component.getActionMap().put("down", down);       
    }

    
    public void show(Component c) {       
        inputField.setText(null);
        if (noShareFilterList.size() > 0) {
            friendCombo.setSelectedIndex(0);
        }
        reloadSharedBuddies();
        adjustSize();
        dialog.show(this);
    }
    
    public boolean contains(Component c) {
        for (; c != null; c = c.getParent()) {
            if (c == this || c == comboPopup) {
                return true;
            }
        }
        return false;
    }
    
 
//    private void adjustPainter(){
//        Area area = new Area(new RoundRectangle2D.Float(1, 1, mainPanel.getWidth() + HGAP * 2-1, getHeight()-2, BORDER_INSETS, BORDER_INSETS));
//        shapePainter.setShape(area);
//    }


    private void resetRowSelection(int oldSelRow) {
        if(noShareFilterList.size() == 0){
            return;
        }
        
        if(oldSelRow == -1){
            friendCombo.setSelectedIndex(0);
        } else if(noShareFilterList.size() > oldSelRow){
            friendCombo.setSelectedIndex(oldSelRow);
        } else if ((oldSelRow - 1) >= 0 && (oldSelRow - 1) < noShareFilterList.size()) {
            friendCombo.setSelectedIndex(oldSelRow - 1);
        }
    }
    
    
    private void shareFriend(SharingTarget friend) {
        shareFriendList.add(friend);
        noShareFriendList.remove(friend);

        shareModel.shareFriend(friend);
        
        adjustSize();
    }
    
    
    private void unshareFriend(SharingTarget friend) {
        shareFriendList.remove(friend);
        noShareFriendList.add(friend);
        
        shareModel.unshareFriend(friend);
        
        adjustSize();
    }
    
    private void adjustSize(){
        adjustFriendLabelVisibility();
        inputField.setVisible(noShareFriendList.size() > 1);
        
        int visibleRows = (shareTable.getRowCount() < SHARED_ROW_COUNT) ? shareTable.getRowCount() : SHARED_ROW_COUNT;
        shareTable.setVisibleRowCount(visibleRows);
        shareScroll.setVisible(visibleRows > 0);
        topLabel.setVisible(visibleRows > 0);
        
        int height = 0;
        int prefWidth = 0;
        for(Component c : getComponents()){
            if (c.isVisible()) {
                height += c.getPreferredSize().height;
            }
            prefWidth = Math.max(prefWidth, c.getPreferredSize().width);
        }
        
         mainPanel.setPreferredSize(new Dimension(WIDTH, height)); 
         mainPanel.setSize(mainPanel.getPreferredSize());
         setPreferredSize(new Dimension(WIDTH + BORDER_INSETS * 2, height + BORDER_INSETS * 2));
         setSize(getPreferredSize());
       // adjustPainter();
    }
    
    
    
    public void dispose() {
        if (noShareFriendList instanceof TransformedList) {
            ((TransformedList) noShareFriendList).dispose();
        }       
        
        ((EventTableModel)shareTable.getModel()).dispose();
    }
    
    
    private void reloadSharedBuddies() {
        shareFriendList.clear();
        noShareFriendList.clear();

        if (shareModel.isGnutellaNetworkSharable()) {
            loadFriend(SharingTarget.GNUTELLA_SHARE);
        }
        
        for (Friend friend : allFriends) {
            loadFriend(new SharingTarget(friend));
        }
    }
    
    private void loadFriend(SharingTarget friend) {
        if (isShared(friend)) {
            shareFriendList.add(friend);
        } else {
            noShareFriendList.add(friend);
        }
    }
    
    private boolean isShared(SharingTarget friend){
        return shareModel.isShared(friend);
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
    
    private class SharingTargetComparator implements Comparator<SharingTarget>{

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
            return o1.getFriend().getRenderName().compareTo(o2.getFriend().getRenderName());
        }
        
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // the share model file or category has changed - reload friends.
        reloadSharedBuddies();
    }
    
    
    private class ShareComboBoxEditor implements ComboBoxEditor{
        @Override
        public Component getEditorComponent() {
            return inputField;
        }

        @Override
        public void addActionListener(ActionListener l) {
            //Do nothing
        }       

        @Override
        public Object getItem() {
            //Do nothing
            return null;
        }

        @Override
        public void removeActionListener(ActionListener l) {
            //Do nothing
        }

        @Override
        public void selectAll() {
            //Do nothing
        }

        @Override
        public void setItem(Object anObject) {
            //Do nothing
        }
        
    }
    
    private static class ShareComboBoxUI extends BasicComboBoxUI {
        @Resource private Icon arrowIcon;
        @Resource private Icon rolloverArrowIcon;
        @Resource private Icon downArrowIcon;
        
        public ShareComboBoxUI(){
            GuiUtils.assignResources(this);
        }
        
        public ComboPopup getPopup(){
            return popup;
        }
        
        @Override
        protected JButton createArrowButton() {
            JButton button = new IconButton(arrowIcon, rolloverArrowIcon, downArrowIcon);
            button.setName("ComboBox.arrowButton");
            return button;
        }
    }

    
}
