package org.limewire.ui.swing.components;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 * SplitPane where the divider component can be specified.
 *
 */
public class LimeSplitPane extends JSplitPane{

    public LimeSplitPane(int orientation, boolean continuousLayout, Component leftComponent, Component rightComponent, JComponent dividerComponent){
        super(orientation, continuousLayout, leftComponent, rightComponent);
        
        BasicSplitPaneUI splitUI = new BasicSplitPaneUI();        
        setUI(splitUI);

        splitUI.getDivider().setBorder(BorderFactory.createEmptyBorder());
        splitUI.getDivider().setLayout(new BorderLayout());
        splitUI.getDivider().removeAll();
        splitUI.getDivider().add(dividerComponent);  
        setDividerSize(dividerComponent.getPreferredSize().height);
    }
}
