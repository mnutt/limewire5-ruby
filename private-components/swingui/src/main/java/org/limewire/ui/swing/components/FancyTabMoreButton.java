package org.limewire.ui.swing.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.GroupLayout.Group;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jdesktop.swingx.JXPanel;

public class FancyTabMoreButton extends LimeComboBox {
    
    private final FancyTabProperties props;
    private JPopupMenu menu = new JPopupMenu();
    
    public FancyTabMoreButton(List<FancyTab> tabs,
            FancyTabProperties props) {

        super(null);
        
        this.props = props;
        this.overrideMenu(menu);

        MoreListener listener = new MoreListener(tabs);
        menu.addPopupMenuListener(listener);
    }
    
    private JComponent createMenuItemFor(
            final JPopupMenu menu, final FancyTab tab) {

        JXPanel jp = new JXPanel();
        jp.setOpaque(false);
        jp.setBackgroundPainter(props.getNormalPainter());
        
        final AbstractButton selectButton = tab.createMainButton();
        selectButton.setHorizontalAlignment(SwingConstants.LEADING);
        selectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                menu.setVisible(false);
            }
        });
        
        JButton removeButton = tab.createRemoveButton();
        if(props.isRemovable()) {
            removeButton.setVisible(true);
            removeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    menu.setVisible(false);
                }
            });
        }
        
        JLabel busyLabel = tab.createBusyLabel();
        
        GroupLayout layout = new GroupLayout(jp);
        jp.setLayout(layout);
        
        SequentialGroup horGroup = layout.createSequentialGroup();
        layout.setHorizontalGroup(horGroup);
        
        Group verGroup = layout.createParallelGroup(GroupLayout.Alignment.CENTER);
        layout.setVerticalGroup(verGroup);
        
        layout.setAutoCreateGaps(true);
        
        horGroup.addGap(5)
                .addComponent(busyLabel)
                .addComponent(selectButton, 0, 120, 120)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(removeButton, 20, 20, 20);
        
        verGroup.addComponent(busyLabel)
                .addComponent(selectButton)
                .addComponent(removeButton, 20, 20, 20);
        
        layout.setHonorsVisibility(busyLabel, false);
        
        Highlighter highlighter = new Highlighter(jp, selectButton, removeButton);
        jp.addMouseListener(highlighter);
        selectButton.addMouseListener(highlighter);
        removeButton.addMouseListener(highlighter);
        
        return jp;
    }
    
    private class Highlighter extends MouseAdapter {
        private final JXPanel panel;
        private final AbstractButton selectButton;
        private final AbstractButton removeButton;
        
        public Highlighter(JXPanel panel, AbstractButton selectButton, AbstractButton removeButton) {
            this.panel = panel;
            this.selectButton = selectButton;
            this.removeButton = removeButton;
        }
        
        @Override
        public void mouseEntered(MouseEvent e) {
            panel.setBackgroundPainter(null);
            removeButton.setIcon(removeButton.getPressedIcon());
        }
        
        @Override
        public void mouseExited(MouseEvent e) {
            panel.setBackgroundPainter(props.getNormalPainter());
            removeButton.setIcon(null);
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            // Forward the click to selection if it wasn't already a button
            if(!(e.getSource() instanceof AbstractButton) && SwingUtilities.isLeftMouseButton(e)) {
                selectButton.doClick(0);
            }
        }
    }
    
    private class MoreListener implements PopupMenuListener {
        private final List<FancyTab> tabs;
        
        public MoreListener(List<FancyTab> tabs) {
            this.tabs = tabs;
        }
        
        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {}
        
        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
        
        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            menu.removeAll();            
            for (FancyTab tab : tabs) {
                menu.add(createMenuItemFor(menu, tab));
            }
        }
    }
}
