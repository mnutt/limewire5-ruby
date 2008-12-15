package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.painter.TextShadowPainter;
import org.limewire.ui.swing.util.GuiUtils;

public class LimeHeaderBar extends JXPanel {
    
    private final Component titleComponent;
    private final JPanel componentContainer;
    
    @Resource private int defaultCompHeight;

    public LimeHeaderBar() {
        this("");
    }
    
    public LimeHeaderBar(String title) {
        GuiUtils.assignResources(this);
        
        JXLabel headerLabel = new JXLabel(title);
        headerLabel.setForegroundPainter(new TextShadowPainter());
        
        this.titleComponent = headerLabel;
        this.componentContainer = new JPanel();
        
        init();
    }
    
    public LimeHeaderBar(Component titleComponent) {
        GuiUtils.assignResources(this);
        
        this.titleComponent = titleComponent;
        this.componentContainer = new JPanel();
        
        init();
    }

    private void init() {
        this.componentContainer.setOpaque(false);
        
        super.setLayout(new MigLayout("insets 0, fill, aligny center","[][]",""));
        super.add(titleComponent, "growy, dock west, gapbefore 10, gapafter 10");
        super.add(componentContainer, "grow, right");
    }
    
    @Override
    public Component add(Component comp) {
        assertHeight(comp);
        
        return this.componentContainer.add(comp);
    }
    
    @Override
    public Component add(Component comp, int index) {
        assertHeight(comp);
        
        return this.componentContainer.add(comp, index);
    }
    
    @Override
    public void add(Component comp, Object constraints) {
        assertHeight(comp);
        
        this.componentContainer.add(comp, constraints);
    }
    
    @Override
    public void add(Component comp, Object constraints, int index) {
        assertHeight(comp);
        
        this.componentContainer.add(comp, constraints, index);
    }
    
    @Override
    public void setLayout(LayoutManager mgr) {
        if (this.componentContainer == null) {
            super.setLayout(mgr);
        } 
        else {
            this.componentContainer.setLayout(mgr);
        }
    }
    
    public void setText(String text) {
        if (titleComponent instanceof JLabel) {
            ((JLabel)titleComponent).setText(text);
        }
    }
    
    @Override
    public void setFont(Font font) {
        if (this.titleComponent == null) {
            super.setFont(font);
        }
        else {            
            this.titleComponent.setFont(font);
        }
    }
    
    @Override
    public void setForeground(Color fg) {
        if (this.titleComponent == null) {
            super.setForeground(fg);
        }
        else {            
            this.titleComponent.setForeground(fg);
        }
    }
    
    private void assertHeight(Component comp) {
        comp.setMinimumSize(new Dimension((int)comp.getMinimumSize().getWidth(), defaultCompHeight));
        comp.setMaximumSize(new Dimension((int)comp.getMaximumSize().getWidth(), defaultCompHeight));
        comp.setPreferredSize(new Dimension((int)comp.getPreferredSize().getWidth(), defaultCompHeight));
        comp.setSize(new Dimension((int)comp.getSize().getWidth(), defaultCompHeight));
    }
    
}
