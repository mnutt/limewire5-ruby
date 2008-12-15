package com.limegroup.gnutella.gui.options.panes;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.limewire.core.settings.BugSettings;

import com.google.inject.Inject;
import com.limegroup.gnutella.bugs.LocalClientInfo;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LocalClientInfoFactory;
import com.limegroup.gnutella.gui.MessageService;
import com.limegroup.gnutella.gui.GUIUtils.SizePolicy;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * This class defines the panel in the options window that allows
 * the user to handle bugs.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class BugsPaneItem extends AbstractPaneItem {
    
    @Inject private static volatile LocalClientInfoFactory localClientInfoFactory;
    
    /**
     * The 'View Example Bug' string
     */
    private final String VIEW_EXAMPLE = 
        I18n.tr("View Example");
    
    /** Checkbox for deadlock. */
    private final JCheckBox DEADLOCK_OPTION = new JCheckBox();

	/**
	 * Radiobutton for sending
	 */
	private final JRadioButton SEND_BOX = new JRadioButton();

	/**
	 * Radiobutton for reviewing
	 */
	private final JRadioButton REVIEW_BOX = new JRadioButton();
	
	/**
	 * Radiobutton for discarding
	 */
	private final JRadioButton DISCARD_BOX = new JRadioButton();
	
	/**
	 * Buttongroup for radiobuttons.
	 */
	private final ButtonGroup BGROUP = new ButtonGroup();

	/**
	 * The constructor constructs all of the elements of this 
	 * <tt>AbstractPaneItem</tt>.
	 *
	 * @param key the key for this <tt>AbstractPaneItem</tt> that the
	 *            superclass uses to generate locale-specific keys
	 */
	public BugsPaneItem() {
        super(
                I18n.tr("Bug Reports"),
                I18n
                        .tr("You can choose how bug reports should be sent. To view an example bug report, click \'View Example\'. Choosing \'Always Send Immediately\' will immediately contact the bug server when LimeWire encounters an internal error. Choosing \'Always Ask for Review\' will tell LimeWire to ask for your approval before sending a bug to the bug server. Choosing \'Always Discard All Errors\' will cause LimeWire to ignore all bugs (this is not recommended)."));
		        
        JButton example = new JButton(VIEW_EXAMPLE);
        example.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                Exception e = new Exception("Example Bug");
                LocalClientInfo info = localClientInfoFactory.
                    createLocalClientInfo(e, Thread.currentThread().getName(), "Example", false);
                JTextArea textArea = new JTextArea(info.toBugReport());
                textArea.setColumns(50);
                textArea.setEditable(false);
                JScrollPane scroller = new JScrollPane(textArea);
                scroller.setBorder(BorderFactory.createEtchedBorder());
                scroller.setPreferredSize( new Dimension(500, 200) );
                MessageService.instance().showMessage(scroller);
            }
        });
        
        SEND_BOX.setText(I18n.tr("Always Send Immediately"));
        REVIEW_BOX.setText(I18n.tr("Always Ask For Review"));
        DISCARD_BOX.setText(I18n.tr("Always Discard All Errors"));
        DEADLOCK_OPTION.setText(I18n.tr("Send Errors Automatically if LimeWire is Frozen"));
                
        BGROUP.add(SEND_BOX);
        BGROUP.add(REVIEW_BOX);
        BGROUP.add(DISCARD_BOX);
        
        add(SEND_BOX);
        add(REVIEW_BOX);
        add(DISCARD_BOX);
        
        if(!LimeWireUtils.isBetaRelease())
            add(DEADLOCK_OPTION);

        add(getVerticalSeparator());
        add(getVerticalSeparator());
        
        JPanel examplePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        examplePanel.add(example);
        GUIUtils.restrictSize(examplePanel, SizePolicy.RESTRICT_HEIGHT);
        add(examplePanel);
	}

	/**
	 * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
	 *
	 * Sets the options for the fields in this <tt>PaneItem</tt> when the 
	 * window is shown.
	 */
	@Override
    public void initOptions() {
        if( !BugSettings.REPORT_BUGS.getValue() )
            BGROUP.setSelected(DISCARD_BOX.getModel(), true);
        else if (!BugSettings.SHOW_BUGS.getValue() )
            BGROUP.setSelected(SEND_BOX.getModel(), true);
        else
            BGROUP.setSelected(REVIEW_BOX.getModel(), true);
        
        DEADLOCK_OPTION.setSelected(BugSettings.SEND_DEADLOCK_BUGS.getValue());
	}

	/**
	 * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
	 *
	 * Applies the options currently set in this window, displaying an
	 * error message to the user if a setting could not be applied.
	 *
	 * @throws IOException if the options could not be applied for some reason
	 */
	@Override
    public boolean applyOptions() throws IOException {
	    ButtonModel bm = BGROUP.getSelection();
	    if( bm.equals(DISCARD_BOX.getModel()) )
	        BugSettings.REPORT_BUGS.setValue(false);
	    else if ( bm.equals(SEND_BOX.getModel()) ) {
	        BugSettings.REPORT_BUGS.setValue(true);
	        BugSettings.SHOW_BUGS.setValue(false);
	    } else {
	        BugSettings.REPORT_BUGS.setValue(true);
	        BugSettings.SHOW_BUGS.setValue(true);
	    }
        
        BugSettings.SEND_DEADLOCK_BUGS.setValue(DEADLOCK_OPTION.isSelected());
        return false;
	}
	
    public boolean isDirty() {
        if(DEADLOCK_OPTION.isSelected() != BugSettings.SEND_DEADLOCK_BUGS.getValue())
            return true;
            
        if(BGROUP.getSelection().equals(DISCARD_BOX.getModel()))
            return BugSettings.REPORT_BUGS.getValue();
        if(BGROUP.getSelection().equals(SEND_BOX.getModel()))
            return ! BugSettings.REPORT_BUGS.getValue() ||
                   BugSettings.SHOW_BUGS.getValue();
        return ! BugSettings.REPORT_BUGS.getValue() ||
               ! BugSettings.SHOW_BUGS.getValue();
    }	
}
