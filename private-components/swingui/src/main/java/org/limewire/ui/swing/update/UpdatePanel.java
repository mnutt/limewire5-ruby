package org.limewire.ui.swing.update;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Application;
import org.jdesktop.application.Resource;
import org.limewire.core.api.updates.UpdateInformation;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.ImageViewPort;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

/**
 * Creates a panel to be displayed inside a Dialog for notification that a 
 * new version of LW is available.
 */
public class UpdatePanel extends JPanel {

    /**
     * Percentage of where the text starts compared to the left hand edge of
     * the Component. Since the dialog is resizeable and we don't want the text to
     * cover the left hand part of the image, whenever the dialog is resized, we
     * recalculate where the text should start based on the current width of the 
     * component and this percentage value
     */
    private static final float textPercentShift = .375f;
    
    @Resource
    private Icon backgroundIcon;
    @Resource
    private Color backgroundColor;
    @Resource
    private Color foregroundColor;
    
    private final org.limewire.core.api.Application application;
    private final UpdateInformation updateInformation;
    
    private JButton leftButton;
    private JButton rightButton;
    
    public UpdatePanel(UpdateInformation updateInformation, org.limewire.core.api.Application application) {
        GuiUtils.assignResources(this);
        
        this.updateInformation = updateInformation;
        this.application = application;
        
        setBackground(backgroundColor);
        
        setLayout(new MigLayout("fill, insets 10 10 10 10, gap 6"));
                
        add(createTopLabel(updateInformation.getUpdateTitle()), "alignx 50%, wrap");
        add(createContentArea(updateInformation.getUpdateText()), "grow, wrap");
        add(createLeftButton(new FirstButtonAction()), "alignx 50%, split, gapright 10");
        add(createRightButton(new SecondButtonAction()), "alignx 50%");
    }
    
    public JButton getDefaultButton() {
        return leftButton;
    }
    
    private JComponent createTopLabel(String text) {
        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.addHyperlinkListener(GuiUtils.getHyperlinkListener());
        pane.setOpaque(false);
        // set the color of the foreground appropriately.
        text = updateForeground(foregroundColor, text);
        pane.setText(text);
        pane.setCaretPosition(0);
        
        return pane;
    }
    
    private JComponent createContentArea(String text) {
        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.addHyperlinkListener(GuiUtils.getHyperlinkListener());
        // set the color of the foreground appropriately.
        text = updateForeground(foregroundColor, text);
        pane.setText(text);
        pane.setCaretPosition(0);
        
        //must be false to view the background image
        pane.setOpaque(false);
        //shift the text so as to not paint over the image
        pane.setMargin( new Insets(0,130,0,0));
        pane.addComponentListener( new ResizeListener(pane));
        ImageViewPort imageViewPort = new ImageViewPort(((ImageIcon)backgroundIcon).getImage());
        imageViewPort.setView(pane);
        
        JScrollPane scroller = new JScrollPane();
        scroller.setViewport(imageViewPort);
        scroller.setPreferredSize(new Dimension(400, 100));
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        return scroller;
    }
    
    private JComponent createLeftButton(Action action) {
        leftButton = new JButton(action);
        leftButton.requestFocusInWindow();
        return leftButton;
    }
    
    private JComponent createRightButton(Action action) {
        rightButton = new JButton(action);
        return rightButton;
    }
    
    private String updateForeground(Color color, String html) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        String hex = GuiUtils.toHex(r) + GuiUtils.toHex(g) + GuiUtils.toHex(b);
        return "<html><body text='#" + hex + "'>" + html + "</body></html>";
    }
    
    private void close() {
        Window window = SwingUtilities.getWindowAncestor(UpdatePanel.this);
        window.setVisible(false);
        window.dispose();
    }
    
    /**
     * The action for the button on the left. 
     */
    private class FirstButtonAction extends AbstractAction {

        public FirstButtonAction() {
            String text;
            if(updateInformation.getButton1Text() != null && updateInformation.getButton1Text().length() > 0)
                text = updateInformation.getButton1Text();
            else
                text = I18n.tr("Update Now");
            
            putValue(Action.NAME, text);
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Visit http://www.limewire.com to update!"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            
            String updateCommand = updateInformation.getUpdateCommand();

            if (updateCommand != null) {
                application.setShutdownFlag(updateCommand);
    
                int restartNow = FocusJOptionPane.showYesNoMessage(I18n.tr("LimeWire needs to be restarted to install the update. If you choose not to update now, LimeWire will update automatically when you close. Would you like to update now?"),
                      I18n.tr("Update Ready"), JOptionPane.YES_OPTION, UpdatePanel.this);
            
                if (restartNow == JOptionPane.YES_OPTION)
                      Application.getInstance().exit(e);
                
            } else
                NativeLaunchUtils.openURL(updateInformation.getUpdateURL());
            
            close();
        }
    }
    
    /**
     * Action for the button on the right.
     */
    private class SecondButtonAction extends AbstractAction {

        public SecondButtonAction() {
            String text;
            if(updateInformation.getButton2Text() != null && updateInformation.getButton2Text().length() > 0)
                text = updateInformation.getButton2Text();
            else
                text = I18n.tr("Update Later");
            
            putValue(Action.NAME, text);
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Visit http://www.limewire.com to update!"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            close();
        }
    }
    
    private class ResizeListener implements ComponentListener {

        private final JEditorPane c;
        
        public ResizeListener(JEditorPane c){
            this.c = c;
        }
        
        public void componentShown(ComponentEvent e) {}
        public void componentHidden(ComponentEvent e) {}
        public void componentMoved(ComponentEvent e) {}

        public void componentResized(ComponentEvent e) {
            int labelStartPos = (int) (c.getWidth() * textPercentShift);
            c.setMargin(new Insets(24, labelStartPos, 0, 0));
        }       
    }
}
