package org.limewire.ui.swing.components;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.JToggleButton.ToggleButtonModel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.icon.EmptyIcon;
import org.limewire.ui.swing.search.resultpanel.SearchTabPopup;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * A fancy 'tab' for use in a {@link FancyTabList}.
 */
public class FancyTab extends JXPanel {

    private final TabActionMap tabActions;
    private final AbstractButton mainButton;
    private final AbstractButton removeButton;
    private final JXBusyLabel busyLabel;
    private final JLabel additionalText;
    private final Line underline;
    private final FancyTabProperties props;
    
    private static enum TabState {
        BACKGROUND, ROLLOVER, SELECTED;
    }
    
    private TabState currentState;
    
    @Resource
    private Icon removeActiveIcon;
    
    @Resource
    private Icon removeInactiveIcon;

    @Resource
    private Icon removeRolloverIcon;
    
    private Icon removeArmedIcon;
    private Icon removeEmptyIcon;

    public FancyTab(TabActionMap actionMap,
            ButtonGroup group,
            FancyTabProperties fancyTabProperties) {
        GuiUtils.assignResources(this);
        removeArmedIcon = new ShiftedIcon(1, 1, removeRolloverIcon);
        removeEmptyIcon = new EmptyIcon(removeActiveIcon.getIconWidth(), removeActiveIcon.getIconHeight());
        
        this.tabActions = actionMap;
        this.props = fancyTabProperties;
        this.mainButton = createMainButton();
        this.additionalText = createAdditionalText();
        this.removeButton = createRemoveButton();
        this.busyLabel = createBusyLabel();
        this.underline = Line.createHorizontalLine(props.getUnderlineColor());

        if (group != null) {
            group.add(mainButton);
        }

        mainButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                changeState(e.getStateChange() == ItemEvent.SELECTED ?
                    TabState.SELECTED : TabState.BACKGROUND);
            }
        });
            
        setOpaque(false);
        setToolTipText(getTitle());
        
        HighlightListener highlightListener = new HighlightListener();
        if (props.isRemovable()) {
            removeButton.addMouseListener(highlightListener);
            removeButton.setVisible(true);
        }
        
        addMouseListener(highlightListener);
        mainButton.addMouseListener(highlightListener);

        changeState(isSelected() ? TabState.SELECTED : TabState.BACKGROUND);
        //For some reason, setting the border on the main button resolves a layout
        //problem only visible on OSX. The problem is that the additionalText
        //label displays far to the right and well below the mainButton.
        mainButton.setBorder(BorderFactory.createEmptyBorder());
        setLayout(new MigLayout("insets 0, filly, gapy 0, hidemode 1"));        
        add(busyLabel, "gapbefore 4, alignx left, aligny bottom, hidemode 0");
        add(mainButton, "aligny bottom, width min(pref,50):pref:max, split 1");
        add(additionalText, "aligny bottom, gapbottom 0");
        add(removeButton, "gapafter 4, aligny bottom, alignx right, wrap");
        // TODO: not worrying about underlining extras for now
        add(underline, "skip 1, span 1, growx, aligny top");
    }
    
    @Override
    public String toString() {
        return "FancyTab for: " + getTitle() + ", " + super.toString();
    }
    
    @Override 
    public Insets getInsets() {
        if (props == null || props.getInsets() == null)
            return super.getInsets();
        
        return props.getInsets();
    }
    
    // TODO: Remove this hack and make a consistant model mediating 
    //        control of tab states and state existance 
    private static void setBuisySize(JComponent c, Dimension d) {
        c.setMaximumSize(d);
        c.setMinimumSize(d);
        c.setPreferredSize(d);
        c.setSize(d);
    }
    
    JXBusyLabel createBusyLabel() {
        final JXBusyLabel busy = new JXBusyLabel(new Dimension(16, 16));
        setBuisySize(busy, new Dimension(0,0));
        busy.setVisible(false);
        
        if (tabActions.getMainAction().getValue(TabActionMap.BUSY_KEY) ==
            Boolean.TRUE) {
            busy.setBusy(true);
            setBuisySize(busy, new Dimension(16,16));    
            busy.setVisible(true);
        } 
        
        tabActions.getMainAction().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(TabActionMap.BUSY_KEY)) {
                    boolean on = evt.getNewValue() == Boolean.TRUE;
                    busy.setBusy(on);
                    setBuisySize(busy, new Dimension(16,16));
                    busy.setVisible(on);                    
                }
            }
        });
        return busy;
    }
    
    JLabel createAdditionalText() {
        final JLabel label = new JLabel();
        label.setVisible(false);
        
        if (tabActions.getMoreTextAction() != null) {
            label.setOpaque(false);
            label.setFont(mainButton.getFont());
            
            String name =
                (String) tabActions.getMoreTextAction().getValue(Action.NAME);
            if (name != null && name.length() > 0) {
                label.setText("(" + name + ")");
                label.setVisible(true);
            }
            
            tabActions.getMoreTextAction().addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals(Action.NAME)) {
                        if (evt.getNewValue() != null) {
                            String newValue = (String) evt.getNewValue();
                            label.setText("(" + newValue + ")");
                            label.setVisible(true);
                        } else {
                            label.setVisible(false);
                        }
                    }
                }
            });
        }
        
        return label;
    }
    
    JButton createRemoveButton() {
        JButton button = new JButton();
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setIcon(removeEmptyIcon);
        button.setSelectedIcon(removeActiveIcon);
        button.setRolloverIcon(removeRolloverIcon);
        button.setPressedIcon(removeArmedIcon);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setAction(tabActions.getRemoveAction());
        button.setActionCommand(TabActionMap.REMOVE_COMMAND);
        button.setHideActionText(true);
        button.setVisible(false);
        if (removeButton != null) {
            for (ActionListener listener : removeButton.getActionListeners()) {
                if (listener == tabActions.getRemoveAction()) {
                    // Ignore the remove action -- it's added implicitly.
                    continue;
                }
                button.addActionListener(listener);
            }
        }
        return button;
    }
    
    AbstractButton createMainButton() {
        final AbstractButton button = new JToggleButton();
        button.setModel(new NoToggleModel());
        button.setAction(tabActions.getMainAction());
        button.setActionCommand(TabActionMap.SELECT_COMMAND);
        button.setBorder(null);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setToolTipText(getTitle());
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setOpaque(false);

        if (props.getTextFont() != null) {
            button.setFont(props.getTextFont());
        }
        
        if(Boolean.TRUE.equals(tabActions.getMainAction().getValue(TabActionMap.NEW_HINT))) {
            FontUtils.bold(button);
        } else {
            FontUtils.plain(button);
        }
        
        tabActions.getMainAction().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals(TabActionMap.NEW_HINT)) {
                    if(Boolean.TRUE.equals(evt.getNewValue())) {
                        FontUtils.bold(button);
                    } else {
                        FontUtils.plain(button);
                    }
                }
            }
        });
        
        return button;
    }
    
    public FancyTabProperties getProperties() {
        return props;
    }
    /** Gets the action underlying this tab. */
    public TabActionMap getTabActionMap() {
        return tabActions;
    }
    
    public void remove() {
        removeButton.doClick(0);
    }
    
    void select() {
        mainButton.doClick(0);
    }
    
    void addRemoveActionListener(ActionListener listener) {
        removeButton.addActionListener(listener);
    }
    
    void removeRemoveActionListener(ActionListener listener) {
        removeButton.removeActionListener(listener);
    }

    /** Selects this tab. */
    void setSelected(boolean selected) {
        mainButton.setSelected(selected);
    }

    /** Returns true if this tab is selected. */
    boolean isSelected() {
        return mainButton.isSelected();
    }

    /** Sets the foreground color of the tab. */
    void setButtonForeground(Color color) {
        mainButton.setForeground(color);
        additionalText.setForeground(color);
    }
    
    void setUnderlineEnabled(boolean enabled) {
        underline.setVisible(enabled);
    }

    /** Returns true if the tab is currently highlighted (in a rollover). */
    boolean isHighlighted() {
        return currentState == TabState.ROLLOVER;
    }

    /** Removes this tab from the button group. */
    void removeFromGroup(ButtonGroup group) {
        group.remove(mainButton);
    }
    
    public void setTextFont(Font font) {
        if (mainButton != null) {
            mainButton.setFont(font);
        }
        
        if (additionalText != null) {
            additionalText.setFont(font);
        }
    }

//  public void underline() {
//      FontUtils.underline(mainButton);
//      FontUtils.underline(additionalText);
//  }
    
    private void changeState(TabState tabState) {
        if (currentState != tabState) {
            this.currentState = tabState;
            switch(tabState) {
            case SELECTED:
                underline.setVisible(false);
                mainButton.setForeground(props.getSelectionColor());
                additionalText.setForeground(props.getSelectionColor());
                this.setBackgroundPainter(props.getSelectedPainter());
                removeButton.setIcon(removeActiveIcon);
                break;
            case BACKGROUND:
                underline.setVisible(props.isUnderlineEnabled());
                underline.setColor(props.getUnderlineColor());
                mainButton.setForeground(props.getNormalColor());
                additionalText.setForeground(props.getNormalColor());
                this.setBackgroundPainter(props.getNormalPainter());
                removeButton.setIcon(removeEmptyIcon);
                break;
            case ROLLOVER:
                underline.setVisible(props.isUnderlineEnabled());
                underline.setColor(props.getUnderlineHoverColor());
                setBackgroundPainter(props.getHighlightPainter());
                removeButton.setIcon(removeInactiveIcon);
                break;
            }
        }
    }

    public String getTitle() {
        return (String)tabActions.getMainAction().getValue(Action.NAME);
    }
    
    private void showPopup(MouseEvent e) {
        // A new popup menu needs to be created each time
        // because its contents can change.
        SearchTabPopup menu = new SearchTabPopup(this);
        menu.show(e);
    }
    
    private class HighlightListener extends MouseAdapter {
        @Override
        public void mouseEntered(MouseEvent e) {
            if (!isSelected() && mainButton.isEnabled()) {
                getTopLevelAncestor().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                changeState(TabState.ROLLOVER);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            getTopLevelAncestor().setCursor(Cursor.getDefaultCursor());            
            if (!isSelected()) {
                changeState(TabState.BACKGROUND);
            }
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            if (props.isRemovable() && SwingUtilities.isMiddleMouseButton(e)) {
                remove();
            } else if (!(e.getSource() instanceof AbstractButton) && SwingUtilities.isLeftMouseButton(e)) {
                select();
            } else if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }
        
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }
    }    
    
    private static class NoToggleModel extends ToggleButtonModel {
        @Override
        public void setPressed(boolean b) {
            if ((isPressed() == b) || !isEnabled()) {
                return;
            }

            // This is different than the super in that
            // we only go from false -> true, not true -> false.
            if (!b && isArmed() && !isSelected()) {
                setSelected(true);
            } 

            if (b) {
                stateMask |= PRESSED;
            } else {
                stateMask &= ~PRESSED;
            }

            fireStateChanged();

            if (!isPressed() && isArmed()) {
                int modifiers = 0;
                AWTEvent currentEvent = EventQueue.getCurrentEvent();
                if (currentEvent instanceof InputEvent) {
                    modifiers = ((InputEvent)currentEvent).getModifiers();
                } else if (currentEvent instanceof ActionEvent) {
                    modifiers = ((ActionEvent)currentEvent).getModifiers();
                }
                fireActionPerformed(
                    new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                                    getActionCommand(),
                                    EventQueue.getMostRecentEventTime(),
                                    modifiers));
            }
        }
    }
}
