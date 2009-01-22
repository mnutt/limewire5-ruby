package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.VerticalLayout;
import org.jdesktop.swingx.icon.EmptyIcon;
import org.limewire.ui.swing.util.ResizeUtils;
import org.limewire.util.Objects;

/** A combobox rendered in the LimeWire 5.0 style. */
public class LimeComboBox extends JXButton {
    
    /** The list of actions that the combobox is going to render as items. */
    private final List<Action> actions;

    /** The currently selected item. */
    private Action selectedAction;
    
    /** The currently selected component. */
    private JComponent selectedComponent;
    
    /** The currently selected label */
    private ActionLabel selectedLabel;

    /** Listeners that will be notified when a new item is selected. */
    private final List<SelectionListener> selectionListeners 
        = new ArrayList<SelectionListener>();
    
    /** True if you've supplied a custom menu via {@link #overrideMenu(JPopupMenu)} */
    private boolean customMenu = false;
    
    /** True if the menu has been updated since the last addition of an action. */
    private boolean menuDirty = false;
    
    /** The menu, lazily created. */
    private JPopupMenu menu = null;
    
    /** The cursor to display when the mouse is over the combobox. */
    private Cursor mouseOverCursor = Cursor.getDefaultCursor();
    
    /** True if the menu is visible. */
    private boolean menuVisible = false;
    
    /** The time that the menu became invisible, to allow toggling of on/off. */
    private long menuInvizTime = -1;
    
    /** True if clicking will always force visibility. */
    private boolean clickForcesVisible = false;
    
    /** Constructs an empty unskinned combo box. */
    public LimeComboBox() {
        this(null);
    }
    
    /** Constructs an empty unskinned combo box. */
    public LimeComboBox(List<Action> newActions) {        
        setText(null);
        actions = new ArrayList<Action>();
        addActions(newActions);
        
        if (!actions.isEmpty()) {
            selectedAction = actions.get(0);
        } else {
            selectedAction = null;
        }        
        initModel();
    }

    /** Sets the combobox to always display the given popupmenu. */
    public void overrideMenu(JPopupMenu menu) {
        this.menu = menu;
        customMenu = true;
        initMenu();
    }
    
    /**
     * A helper method for painting elements of overridden menus in the default style
     */
    public JComponent decorateMenuComponent(JComponent item) {
        item.setFont(getFont());
        item.setBackground(Color.WHITE);
        item.setForeground(Color.BLACK);
        item.setBorder(BorderFactory.createEmptyBorder(0,1,0,0));
        return item;
    }
    
    /**
     * A helper method for creating menu items painted in the default style of an 
     *  overridden menu.
     */
    public JMenuItem createMenuItem(Action a) {
        JMenuItem item = new JMenuItem(a);
        decorateMenuComponent(item);
        return item;
    }
    
    /**
     * Adds the given actions to the combobox.  The actions will
     * be rendered as items that can be chosen.
     * 
     * This method has no effect if the popupmenu is overriden. 
     */
    public void addActions(List<Action> newActions) {
        if (newActions == null) {
            return;
        }
        menuDirty = true;
        actions.addAll(newActions);
        if (selectedAction == null) {
            selectedAction = actions.get(0);
        }
        ResizeUtils.updateSize(this, actions);
        if (menu == null) {
            createPopupMenu();
        }
    }
    
    /** 
     * Adds a single action to the combobox.
     *
     * This method has no effect if the popupmenu is overriden.
     */
    public void addAction(Action action) {
        actions.add(Objects.nonNull(action, "action"));
        menuDirty = true;
        if (selectedAction == null) {
            selectedAction = actions.get(0);
        }
        ResizeUtils.updateSize(this, actions);
        if (menu == null) {
            createPopupMenu();
        }
    }

    /** 
     * Removes all actions & any selected action.
     * 
     * This method has no effect if the popupmenu is overriden.
     */
    public void removeAllActions() {
        menuDirty = true;
        actions.clear();
        selectedAction = null;
    }
    
    /**
     * Removes the specific action.  If it was the selected one, selection is lost. 
     *  
     * This method has no effect if the popupmenu is overriden.
     */
    public void removeAction(Action action) {
        menuDirty = true;
        actions.remove(action);
        if (action == selectedAction) {
            selectedAction = null;
        }
    }

    /** 
     * Selects the specific action.
     * 
     * This method has no effect if the popupmenu is overriden. 
     */
    public void setSelectedAction(Action action) {        
        // Make sure the selected action is in the list
        if (actions.contains(action)) {
            selectedAction = action;     
            menuDirty = true;
        }        
    }
    
    /**
     * Returns the selected action.
     * 
     * This method has no effect if the popupmenu is overriden.
     */
    public Action getSelectedAction() {
        return selectedAction;
    }
    
    
    /** Get all actions. */
    public List<Action> getActions() {
        return actions;
    }
    
    
    /** Sets the text this will display as the prompt. */
    @Override
    public void setText(String promptText) {
        super.setText(promptText);        
        if (promptText != null) {
            ResizeUtils.updateSize(this, actions);
        }
    }

    /** Sets the cursor that will be shown when the button is hovered-over. */
    public void setMouseOverCursor(Cursor cursor) {
        mouseOverCursor = cursor;
    }

    /**
     * Adds a listener to be notified when the selection changes.
     * 
     * This method has no effect if the popupmenu is overriden.
     */
    public void addSelectionListener(SelectionListener listener) {
        selectionListeners.add(listener);
    }
    
    @Override 
    public void setModel(final ButtonModel delegate) {
        super.setModel(new ButtonModel() {
            public boolean isArmed() { return delegate.isArmed(); }
            public boolean isSelected() { return delegate.isSelected(); }
            public boolean isEnabled() { return delegate.isEnabled(); }
            public boolean isPressed() {
                return delegate.isPressed() || menu != null && menu.isVisible(); 
            }
            public boolean isRollover() { return delegate.isRollover(); }
            public void setArmed(boolean b) { delegate.setArmed(b); }
            public void setSelected(boolean b) { delegate.setSelected(b); }
            public void setEnabled(boolean b) { delegate.setEnabled(b); }
            public void setPressed(boolean b) { delegate.setPressed(b); }
            public void setRollover(boolean b) { delegate.setRollover(b); }
            public void setMnemonic(int i) { delegate.setMnemonic(i); }
            public int getMnemonic() { return delegate.getMnemonic(); }
            public void setActionCommand(String string) { delegate.setActionCommand(string); }
            public String getActionCommand() { return delegate.getActionCommand(); }
            public void setGroup(ButtonGroup buttonGroup) { delegate.setGroup(buttonGroup); }
            public void addActionListener(ActionListener actionListener) { 
                delegate.addActionListener(actionListener);
            }
            public void removeActionListener(ActionListener actionListener) {
                delegate.removeActionListener(actionListener);
            }
            public Object[] getSelectedObjects() { return delegate.getSelectedObjects(); }
            public void addItemListener(ItemListener itemListener) {
                delegate.addItemListener(itemListener);
            }
            public void removeItemListener(ItemListener itemListener) { 
                delegate.removeItemListener(itemListener);
            }
            public void addChangeListener(ChangeListener changeListener) { 
                delegate.addChangeListener(changeListener);
            }
            public void removeChangeListener(ChangeListener changeListener) {
                delegate.removeChangeListener(changeListener);
            }
        });
    }
        
    /** Sets the icons of the combobox when the button is rolled over, pressed, and normal. */
    public void setIcons(Icon regular, Icon hover, Icon down) {
        setIcon(regular);
        setRolloverIcon(hover);
        setPressedIcon(down);
    }
    
    @Override
    public Icon getRolloverIcon() {
        Icon icon = super.getRolloverIcon();
        if (icon == null) {
            return getIcon();
        } else {
            return icon;
        }
    }
    
    @Override
    public Icon getPressedIcon() {
        Icon icon = super.getPressedIcon();
        if (icon == null) {
            return getIcon();
        } else {
            return icon;
        }
    }
        
    @Override
    public boolean isOpaque() {
        return false;
    }
    
    
    @Override
    public void setFont(Font f) {
        super.setFont(f);
        ResizeUtils.updateSize(this, actions);
    }
    
    /**
     * Sets whether or not clicking the combobox forces the menu to display.
     * Normally clicking it would cause a visible menu to disappear.
     * If this is true, clicking will always force the menu to appear.
     * This is useful for renderers such as in tables.
     */
    public void setClickForcesVisible(boolean clickForcesVisible) {
        this.clickForcesVisible = clickForcesVisible;
    }

    private void initModel() {
        setModel(getModel());        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setCursor(mouseOverCursor);     
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (menu != null && isEnabled()) {
                    // If the menu is visible or this is the click that
                    // caused it to become invisible, go with inviz.
                    if(!clickForcesVisible && (menuVisible || System.currentTimeMillis() - menuInvizTime <= 10f)) {
                        menu.setVisible(false);
                    } else {
                        menu.show((Component) e.getSource(), 1, getHeight()-1);
                    }
                }
            }
        });
    }

    private void createPopupMenu() {
        menu = new JPopupMenu();        
        initMenu();
    }
       
    private void updateMenu() {
        // If custom or not dirty, do nothing.
        if(customMenu || !menuDirty) {
            return;
        }
        
        // otherwise, reset up the menu.
        menuDirty = false;
        menu.removeAll();
        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActionLabel label = (ActionLabel)e.getSource();
                Action action = label.getAction();
                selectedAction = action;
                selectedComponent = (JComponent)label.getParent();
                selectedLabel = label;
                // Fire the parent listeners
                for (SelectionListener listener : selectionListeners) {
                    listener.selectionChanged(action);
                }
                repaint();
                menu.setVisible(false);
            }
        };
        
        // This is a workaround for not using JMenuItem -- it mimicks the feel
        // without requiring odd spacing.
        MouseListener mouseListener = new MouseAdapter() {
            
            private final Color foreground = UIManager.getColor("MenuItem.foreground");
            private final Color selectedForeground = UIManager.getColor("MenuItem.selectionForeground");
            
            @Override
            public void mouseEntered(MouseEvent e) {
                paintNormal(e.getSource(), true);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                paintNormal(e.getSource(), false);
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                paintNormal(e.getSource(), true);
            }
            
            private void paintNormal(Object source, boolean selected) {
                 ActionLabel label = (ActionLabel)source;
                 label.setForeground(selected ? selectedForeground : foreground );
                 
                 JComponent parent = (JComponent) label.getParent();
                 parent.setOpaque(selected);
                 parent.repaint();
                
                 // Remove highlight on the last selected component.
                 if (selectedComponent != null && selectedComponent != parent) {
                     selectedLabel.setForeground(foreground);
                     selectedComponent.setOpaque(false);
                     selectedComponent.repaint();
                     selectedLabel = null;
                     selectedComponent = null;
                 }
            }
        };
        
        Icon emptyIcon = null; 
        for(Action action : actions) {
            if(action.getValue(Action.SMALL_ICON) != null) {
                emptyIcon = new EmptyIcon(16, 16);
                break;
            }
        }
        
        selectedComponent = null;
        selectedLabel = null;
        
        for (Action action : actions) {
            
            // We create the label ourselves (instead of using JMenuItem),
            // because JMenuItem adds lots of bulky insets.
            JXPanel panel = new JXPanel(new VerticalLayout());
            ActionLabel menuItem = new ActionLabel(action, false);
            
            panel.setBackground(UIManager.getColor("MenuItem.selectionBackground"));
            
            if (action != selectedAction) {
                panel.setOpaque(false);
                menuItem.setForeground(UIManager.getColor("MenuItem.foreground"));
            } else {
                selectedComponent = panel;
                selectedLabel = menuItem;
                selectedComponent.setOpaque(true);
                selectedLabel.setForeground(UIManager.getColor("MenuItem.selectionForeground"));
            }
            
            if(menuItem.getIcon() == null) {
                menuItem.setIcon(emptyIcon);
            }
            menuItem.addMouseListener(mouseListener);
            decorateMenuComponent(menuItem);
            menuItem.setBorder(BorderFactory.createEmptyBorder(0, 6, 2, 6));
            menuItem.addActionListener(actionListener);
            panel.add(menuItem);
            menu.add(panel);
        }
    }
    
    private void initMenu() {        
        decorateMenuComponent(menu);
        menu.setBorder(BorderFactory.createLineBorder(Color.BLACK,1));
        
        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                menuVisible = false;
                menuInvizTime = System.currentTimeMillis();
            }
            
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                menuVisible = false;
                menuInvizTime = System.currentTimeMillis();
            }
            
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                menuVisible = true;
                updateMenu();
                if (getText() == null) {
                    menu.setPreferredSize(new Dimension(getWidth()-2, 
                            (int) menu.getPreferredSize().getHeight()));
                }                
            }
        });
    }
    

    /** A listener that's notified when the selection in the combobox changes. */
    public interface SelectionListener {
        /** Notification that the given action is now selected. */
        public void selectionChanged(Action item);
    }
}
