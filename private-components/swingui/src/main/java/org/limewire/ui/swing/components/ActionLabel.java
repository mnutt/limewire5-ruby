package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.jdesktop.swingx.JXLabel;
import org.limewire.ui.swing.action.ActionKeys;
import org.limewire.ui.swing.listener.ActionHandListener;
import org.limewire.ui.swing.listener.MouseActionListener;
import org.limewire.ui.swing.util.FontUtils;


/**
 * A label that has a clickable text. The text is rendered as an HTML link and
 * the mouse cursor is changed when the mouse hovers over the label.
 */
public class ActionLabel extends JXLabel  {
    
    private MouseListener urlListener;
    
    private PropertyChangeListener listener = null;

    private Action currentAction;
          
    private String text;
    
    private final boolean renderAsLink;
    
    private Color linkColor = UIManager.getColor("Label.foreground");
    
    private final List<ActionListener> actionListeners = new CopyOnWriteArrayList<ActionListener>();
   

    /**
     * Constructs a new clickable label whose text is in the hex color described.
     * 
     * @param action
     * @param color
     */
    public ActionLabel(Action action, boolean renderAsLink, boolean showHand) {
        this.renderAsLink = renderAsLink;
        setAction(action, showHand);
        setHorizontalTextPosition(SwingConstants.RIGHT);
        setHorizontalAlignment(SwingConstants.LEFT);
    }
    
    public void addActionListener(ActionListener listener) {
        actionListeners.add(listener);
    }
    
    public void removeActionListener(ActionListener listener) {
        actionListeners.remove(listener);
    }
    
    @Override
    public void setText(String text) {
        this.text = text;
        if(renderAsLink) {
            super.setText(text);
            FontUtils.underline(this);
            if(linkColor != null) {
                setForeground(linkColor);
            } else {
                setForeground(Color.BLUE);
            }
        } else {
            super.setText(text);
            FontUtils.removeUnderline(this);
        }
    }
   
    
    public void setAction(Action action, boolean showHand) {
        // remove old listener
        Action oldAction = getAction();
        if (oldAction != null) {
            oldAction.removePropertyChangeListener(getListener());
        }

        // add listener
        currentAction = action;
        currentAction.addPropertyChangeListener(getListener());
        if(showHand) {
            installListener(new ActionHandListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    currentAction.actionPerformed(e);
                    for(ActionListener listener : actionListeners) {
                        listener.actionPerformed(e);
                    }
                }
            }));
        } else {
            installListener(new MouseActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    currentAction.actionPerformed(e);
                    for(ActionListener listener : actionListeners) {
                        listener.actionPerformed(e);
                    }
                }
            }));
        }
        updateLabel(currentAction, null);
    }
    
    
    public void setColor(Color fg) {
        linkColor = fg;
        setText(text);
    }
    
    public Action getAction(){
        return currentAction;
    }
       
    private PropertyChangeListener getListener() {
        if (listener == null) {
            listener = new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) { 
                    updateLabel(null, evt);                    
                }
            };
        }
        return listener;
    }
    
    /*
     * Update label text based on action event
     */
    public void updateLabel(Action action, PropertyChangeEvent evt) {
        if(evt == null) {
            assert action != null;
            String display = (String) currentAction.getValue(Action.NAME);
            setIcon((Icon) currentAction.getValue(Action.SMALL_ICON));
            setToolTipText((String) currentAction.getValue(Action.SHORT_DESCRIPTION));
            setVisible(!Boolean.FALSE.equals(currentAction.getValue(ActionKeys.VISIBLE)));
            if(display != null) {
                setText(display);
            }
        } else {
            assert action == null;
            String id = evt.getPropertyName();
            if(id.equals(Action.NAME)) {
                setText((String)evt.getNewValue());
            } else if(id.equals(Action.SMALL_ICON)) {
                setIcon((Icon)evt.getNewValue());
            } else if(id.equals(Action.SHORT_DESCRIPTION)) {
                setToolTipText((String)evt.getNewValue());
            } else if(id.equals(ActionKeys.VISIBLE)) {
                setVisible(!Boolean.FALSE.equals(evt.getNewValue()));
            }
        }   
    }
   
    private void installListener(MouseListener listener) {
        if (urlListener != null) {
            removeMouseListener(urlListener);
        }
        urlListener = listener;
        addMouseListener(urlListener);
    }
}