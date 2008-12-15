package com.limegroup.gnutella.gui.xml.editor;

import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.MessageService;
import com.limegroup.gnutella.gui.URLLabel;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.licenses.PublishedCCLicense;

/**
 * This class provides a dialog that displays the RDF representation of the
 * license for a shared file. It listens to
 * <tt>FileManagerEvents<tt> and pops-up when the URN of a particular file has been recalculated. 
 * 
 * @see CCPublishWizard
 */
public class CCRDFOuptut /*implements MetaDataEventListener*/ {
	
	private final String CCPUBLISHER_TITLE = I18n.tr("License RDF Output");
	
	private static final int DIALOG_WIDTH = 480;
	
	private static final int DIALOG_HEIGHT = 360;
	
	private final JLabel RDF_OUTPUT_LABEL = new JLabel("<html>" + I18n.tr("In order to publish your file, please insert the following code in your verification page at:") + "</html>");
	
	private final JTextArea RDF_OUTPUT = new JTextArea(6, 20);
	
	private CopyAction copyAction = new CopyAction();
	
	private FinishAction finishAction = new FinishAction();
	
	private JDialog dialog;
	
//	private final FileDesc _fd;
	
	private final String _holder,_title,_year,_description,_url;
	
	private final int _type;
	
	private boolean _isEventHandled;
	
	public CCRDFOuptut(FileDesc fd,String holder, String title, 
            String year, String description, String url,int type) {
//		_fd = fd;
		_holder = holder;
		_title = title;
		_year = year;
		_description = description;
		_url = url;
		_type = type;
	}
	
//	public void handleEvent(final FileManagerEvent event) {
//	    if(!event.isChangeEvent() || (event.getOldFileDesc() == null && event.getNewFileDesc() == null) )
//             return;
//        
//        if(_fd.equals(event.getOldFileDesc())) {
//        	showDialog(event.getNewFileDesc());
//        }
//	}
	
	public void metaDataUnchanged(FileDesc fd) {
		showDialog(fd);
	}

	private void showDialog(final FileDesc newFD) {
        synchronized(this) {
            if (_isEventHandled) {
                return;
            }
            _isEventHandled = true;
        }
        
        GUIMediator.safeInvokeLater(new Runnable() {
            public void run() {
            	initDialog();
            	
            	String RDFString = 
            		PublishedCCLicense.getRDFRepresentation(_holder,_title,_year,_description,
            				newFD.getSHA1Urn().httpStringValue(),_type);
            	RDF_OUTPUT.setText(RDFString);
            	RDF_OUTPUT.setCaretPosition(0);
            	
            	dialog.setLocationRelativeTo(MessageService.getParentComponent());
            	dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        		GUIUtils.addHideAction(dialog);
            	dialog.setVisible(true);
            }
        });

	}
	
	private void initDialog() {
		dialog = new JDialog(GUIMediator.getAppFrame(),true);
		dialog.setTitle(CCPUBLISHER_TITLE);

		BoxPanel mainPanel = new BoxPanel();
		mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		mainPanel.addLeft(RDF_OUTPUT_LABEL);
		mainPanel.addLineGap();
		
		JLabel urlLabel = new URLLabel(_url);
		mainPanel.addLeft(urlLabel);
		mainPanel.addLineGap();
		
		RDF_OUTPUT.setEditable(false);
		RDF_OUTPUT.setLineWrap(true);
		mainPanel.add(new JScrollPane(RDF_OUTPUT, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		mainPanel.addVerticalComponentGap();
		
		JButton copyButton = new JButton(copyAction);
		mainPanel.addCenter(copyButton);
		mainPanel.addLineGap();
		
		ButtonRow buttonRow = new ButtonRow(new Action[] { finishAction }, ButtonRow.X_AXIS, ButtonRow.LEFT_GLUE);
		mainPanel.add(buttonRow);

		dialog.setContentPane(mainPanel);
		dialog.setSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
	}
	
	private class CopyAction extends AbstractAction {
		
		public CopyAction() {
			putValue(Action.NAME, I18n.tr("Copy to Clipboard"));
		}

		public void actionPerformed(ActionEvent e) {
			try {
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(new StringSelection(RDF_OUTPUT.getText()), null);
			} catch (HeadlessException doNothingException) {}			
		}
		
	}

	private class FinishAction extends AbstractAction {
		
		public FinishAction() {
			putValue(Action.NAME, I18n.tr("Finish"));
		}

		public void actionPerformed(ActionEvent e) {
			dialog.dispose();
		}
		
	}


}
