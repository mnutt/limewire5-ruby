package org.limewire.ui.swing.pro;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Application;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.ImageViewPort;
import org.limewire.ui.swing.statusbar.ProStatusPanel;
import org.limewire.ui.swing.statusbar.ProStatusPanel.InvisibilityCondition;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;

/**
 * This is a throw back to the 4.x style upgrade to Pro popup.
 */
public class OldStyleProNag extends JPanel {
    
    @Resource
    private Icon backgroundIcon;
    @Resource
    private Color backgroundColor;
    
    private final Application application;
    private final ProStatusPanel proStatusPanel;
    private final JButton yesButton;
    
    private String defaultURL = "http://www.limewire.com/download/pro/?ref=lwn1";
    
    @Inject
    public OldStyleProNag(Application application, 
            ProStatusPanel proStatusPanel) {
        this.application = application;
        this.proStatusPanel = proStatusPanel;
        
        GuiUtils.assignResources(this);
        
        setBackground(backgroundColor);
        
        setLayout(new MigLayout("fill, insets 10 10 4 10, gap 7 5"));
                
        add(createContentArea(I18n.tr("For Turbo-Charged downloads, get LimeWire PRO. We guarantee that you will love the improved performance of PRO. Thank you for helping keep the Internet open by running LimeWire.")), "span, grow, wrap");
        
        JLabel label = new JLabel(I18n.tr("Upgrade to LimeWire PRO?"));
        add(label, "span, wrap, alignx center");
                
        yesButton = createButton(new YesAction());
        yesButton.requestFocusInWindow();
        JButton whyButton = createButton(new WhyAction());
        JButton laterButton = createButton(new NoAction());
        
        add(yesButton, "alignx center, split 3");
        add(whyButton, "alignx center");
        add(laterButton, "alignx center");
        
        proStatusPanel.addCondition(InvisibilityCondition.PRO_ADD_SHOWN);
    }
    
    /**
     * Returns the default button that should be selected in the dialog.
     */
    public JButton getDefaultButton() {
        return yesButton;
    }
    
    private JButton createButton(Action action) {
        JButton button = new JButton(action);
        button.setFocusPainted(false);
        return button;
    }
    
    private JComponent createContentArea(String text) {
        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setText(text);
        pane.setCaretPosition(0);
        
        //must be false to view the background image
        pane.setOpaque(false);
        //shift the text so as to not paint over the image
        pane.setMargin(new Insets(5,130,0,0));
        ImageViewPort imageViewPort = new ImageViewPort(((ImageIcon)backgroundIcon).getImage());
        imageViewPort.setView(pane);
        
        JScrollPane scroller = new JScrollPane();
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.setViewport(imageViewPort);
        scroller.setPreferredSize(new Dimension(350, 115));
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scroller;
    }
    
    /**
     * Closes the popup dialog and shows the status bar pro add.
     */
    private void close() {
        Window window = SwingUtilities.getWindowAncestor(OldStyleProNag.this);
        window.setVisible(false);
        window.dispose();
        
        proStatusPanel.removeCondition(InvisibilityCondition.PRO_ADD_SHOWN);  
    }
    
    /**
     * Opens a url to the pro purchase page and closes the popup.
     */
    private class YesAction extends AbstractAction {
        public YesAction() {
            super(I18n.tr("Yes"));
            
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Get LimeWire PRO Now"));        
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            NativeLaunchUtils.openURL(application.addClientInfoToUrl(defaultURL));
            close();
        }
    }
    
    /**
     * Closes the popup without any url action.
     */
    private class NoAction extends AbstractAction {
        public NoAction() {
            super(I18n.tr("Later"));
            
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Get LimeWire PRO Later"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            close();
        }
    }
    
    /**
     * Opens URL to why page and closes the popup.
     */
    private class WhyAction extends AbstractAction {
        public WhyAction() {
            super(I18n.tr("Why"));
        
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("What does PRO give me?"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            NativeLaunchUtils.openURL(application.addClientInfoToUrl(defaultURL));
            close();
        }
    }
}
