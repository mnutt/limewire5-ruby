package com.limegroup.gnutella.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.limegroup.gnutella.version.UpdateInformation;

/** 
 * Creates a new update panel for prompting the user for whether 
 * or not they would like to update. 
 */
final class UpdateDialog extends JDialog {
	
    /**
     * Percentage of where the text starts compared to the left hand edge of
     * the Component. Since the dialog is resizeable and we don't want the text to
     * cover the left hand part of the image, whenever the dialog is resized, we
     * recalculate where the text should start based on the current width of the 
     * component and this percentage value
     */
    private static final float textPercentShift = .275f;
    
    /**
     * Default background image to display behind the text
     */
    private final ImageIcon defaultBackgroundImage = GUIMediator.getThemeImage("updateBackground");
    
    
	/**
	 * Constructs the dialog.
	 */
	public UpdateDialog(UpdateInformation info) {
	    super(GUIMediator.getAppFrame());
	    setModal(true);
	    setTitle(I18n.tr("New Version Available!"));
	    
	    JButton button = buildContentArea(info);
	    pack();
	    Dimension size = new Dimension(500, 300);
	    setSize(size);
	    ((JComponent)getContentPane()).setPreferredSize(size);
        getRootPane().setDefaultButton(button);
        button.requestFocus();
	}
	
	private JButton buildContentArea(UpdateInformation info) {
		JComponent title = makeText(getUpdateTitle(info.getUpdateTitle()), false);
		JComponent text = makeText(info.getUpdateText(), true);
		JButton button1 = makeButton1(info);
		JButton button2 = makeButton2(info);
		
		JComponent jc = (JComponent)getContentPane();
		jc.setLayout(new GridBagLayout());
		jc.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridBagConstraints gc = new GridBagConstraints();
		
		JPanel p = new JPanel(new GridBagLayout());		
		
		gc.gridwidth = GridBagConstraints.REMAINDER;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.insets = new Insets(0, 0, 0, 0);
		p.add(title, gc);

		jc.add(p, gc);
		
		gc.fill = GridBagConstraints.BOTH;
		gc.anchor = GridBagConstraints.CENTER;
		gc.insets = new Insets(5, 0, 5, 0);
		gc.weightx = 1;
		gc.weighty = 1;
		jc.add(text, gc);
		
		p = new JPanel(new GridBagLayout());
		gc.gridwidth = GridBagConstraints.RELATIVE;
		gc.fill = GridBagConstraints.NONE;
		gc.insets = new Insets(0, 0, 0, 3);
		gc.weightx = 0;
		gc.weighty = 0;
		gc.anchor = GridBagConstraints.EAST;		
		p.add(button1, gc);

        gc.gridwidth = GridBagConstraints.REMAINDER;
		gc.anchor = GridBagConstraints.WEST;
		gc.insets = new Insets(0, 3, 0, 0);
		p.add(button2, gc);
		
        gc.insets = new Insets(0, 0, 0, 0);
		gc.anchor = GridBagConstraints.CENTER;
		jc.add(p, gc);
		
		return button1;
	}
	
	private String getUpdateTitle(String title) {
	    if(title != null)
	        return "<b>" + title + "</b>";
	    else
	        return "<b>" + I18n.tr("A new version of LimeWire is available.") + "</b>";
	}
		    
	private JComponent makeText(String text, boolean scroll) {
	    JEditorPane pane = new JEditorPane();
	    JLabel dummy = new JLabel();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setBackground(dummy.getBackground());
        pane.setFont(dummy.getFont());
        pane.addHyperlinkListener(GUIUtils.getHyperlinkListener());
        // set the color of the foreground appropriately.
        text = updateForeground(dummy.getForeground(), text);
        pane.setText(text);
        pane.setCaretPosition(0);
        if(!scroll)
            return pane;
        
        //must be false to view the background image
        pane.setOpaque(false);
        //shift the text so as to not paint over the image
        pane.setMargin( new Insets(0,130,0,0));
        pane.addComponentListener( new ResizeListener(pane));
        ImageViewPort imageViewPort = new ImageViewPort(defaultBackgroundImage.getImage());
        imageViewPort.setView(pane);
        
        JScrollPane scroller = new JScrollPane();
        scroller.setViewport(imageViewPort);
        scroller.setPreferredSize(new Dimension(400, 100));
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroller.setBorder(null);
        return scroller;
    }
    
    private String updateForeground(Color color, String html) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        String hex = GUIUtils.toHex(r) + GUIUtils.toHex(g) + GUIUtils.toHex(b);
        return "<html><body text='#" + hex + "'>" + html + "</body></html>";
    }
        
	private JButton makeButton1(final UpdateInformation info) {
	    String text = info.getButton1Text();
	    if(text == null)
	        text = I18n.tr("Update Now");
	    
	    JButton b = new JButton(text);
	    b.setToolTipText(I18n.tr("Visit http://www.limewire.com to update!"));
	    b.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
                
                String updateCommand = info.getUpdateCommand();
                
                if (updateCommand != null) {
                    GUIMediator.flagUpdate(updateCommand);
        
                    DialogOption restartNow = 
                        GUIMediator.showYesNoTitledMessage(I18n.tr("LimeWire needs to be restarted to install the update. If you choose not to update now, LimeWire will update automatically when you close. Would you like to update now?"),
                                I18n.tr("Update Ready"), DialogOption.YES);
                    
                    if (restartNow == DialogOption.YES)
                        GUIMediator.shutdown();
                    
                } else 
                    GUIMediator.openURL(info.getUpdateURL());
                
                setVisible(false);
                dispose();
	        }
        });
        
        return b;
    }
    
	private JButton makeButton2(final UpdateInformation info) {
	    String text = info.getButton2Text();
	    if(text == null)
	        text = I18n.tr("Update Later");
	    
	    JButton b = new JButton(text);
	    b.setToolTipText(I18n.tr("Visit http://www.limewire.com to update!"));
	    b.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	            setVisible(false);
	            dispose();
	        }
        });
        
        return b;
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
            c.setMargin(new Insets(0,labelStartPos,0,0));
        }	    
	}
}