package com.limegroup.gnutella.gui.download;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.SaveLocationException.LocationCode;
import org.limewire.core.settings.QuestionsHandler;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.MessageService;
import com.limegroup.gnutella.gui.util.CoreExceptionHandler;
import com.limegroup.gnutella.library.FileDesc;

/**
 * Handles {@link org.limewire.core.api.download.SaveLocationException SaveLocationExceptions}
 * showing the user the exact cause of the exception and giving them a choice
 * to choose a different download location or overwrite the existing file.
 * <p>
 * The dialog stays visible as long as 
 * {@link com.limegroup.gnutella.gui.download.GuiDownloaderFactory#createDownloader(boolean)}
 * throws exceptions and it has not been cancelled.
 */
public class DownloaderDialog extends JDialog {
    
    public static final int DEFAULT_ASK = 0;
    public static final int DEFAULT_APPEND = 1;
    public static final int DEFAULT_SAVE_AS = 2;

	private GuiDownloaderFactory factory;
	private Downloader downloader;
	private JLabel titleLabel = new JLabel();
	private JLabel descLabel = new JLabel();
	private JLabel noteLabel = new JLabel();
    private ButtonRow buttons;
    private File originalFile;
        
    /**
     * Creates a new dialog for a factory and an already thrown exception.
     * @param factory
     * @param sle
     */
    DownloaderDialog(GuiDownloaderFactory factory, SaveLocationException sle) {
        super(GUIMediator.getAppFrame());
        
        this.factory = factory;
        originalFile = factory.getSaveFile();
        
        final int defaultAction = QuestionsHandler.DEFAULT_ACTION_FILE_EXISTS.getValue();
        if(defaultAction != DEFAULT_ASK) {
            if(defaultAction == DEFAULT_APPEND)
                new AppendAction().actionPerformed(new ActionEvent(DownloaderDialog.this,0,null));
            else if(defaultAction == DEFAULT_SAVE_AS)
                new SaveAsAction().actionPerformed(new ActionEvent(DownloaderDialog.this,0,null));
            return;
        }
        
        setUpDialog( sle);
    }
        
    private void setUpDialog( SaveLocationException sle) {
		// dialog
		setModal(true);
		setResizable(false);
		getContentPane().setLayout(new BorderLayout());
		GUIUtils.addHideAction((JComponent)getContentPane());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		// top panel
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(topPanel, BorderLayout.CENTER);
		
		// main panel
		JPanel mainPanel = new JPanel(new BorderLayout());
		topPanel.add(mainPanel, BorderLayout.NORTH);

		Color bg = UIManager.getColor("TextField.background");
		Color fg = UIManager.getColor("TextField.foreground");
		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBackground(bg);
		titlePanel.setBorder(BorderFactory.createEtchedBorder());
		JLabel icon = new JLabel(GUIMediator.getThemeImage("warning"));
		icon.setBackground(bg);
		icon.setBorder(new EmptyBorder(2, 5, 2, 5));
		titlePanel.add(icon, BorderLayout.WEST);
		titleLabel.setBackground(bg);
		titleLabel.setForeground(fg);
		titleLabel.setFont(new Font("Dialog", Font.BOLD, 18));
		titlePanel.add(titleLabel, BorderLayout.CENTER);
		mainPanel.add(titlePanel, BorderLayout.NORTH);
		
		// labels
		Box labelBox = Box.createVerticalBox();
		labelBox.add(descLabel);
		labelBox.add(Box.createVerticalStrut(10));
		labelBox.add(noteLabel);
		labelBox.add(Box.createVerticalStrut(5));
		labelBox.add(Box.createVerticalGlue());
		mainPanel.add(labelBox, BorderLayout.CENTER);
        
        JRadioButton saveAs = new JRadioButton(
                I18n.tr("Always Save As..."));
        JRadioButton append = new JRadioButton(
                I18n.tr("Always Append (#)"));
        JRadioButton ask = new JRadioButton(
                I18n.tr("Always Ask"));
        setupDefaultOption(saveAs, DEFAULT_SAVE_AS);
        setupDefaultOption(append, DEFAULT_APPEND);
        setupDefaultOption(ask, DEFAULT_ASK);
    	
        ButtonGroup group = new ButtonGroup();
        group.add(ask);
        group.add(append);
        group.add(saveAs);

        // default action settings panel
    	JLabel alwaysLabel = new JLabel(I18n.tr("Choose a Default Action:"));
        Box alwaysLabelBox = Box.createHorizontalBox();
        alwaysLabelBox.add(alwaysLabel);
        alwaysLabelBox.add(Box.createHorizontalGlue());
        Box alwaysBox = Box.createHorizontalBox();
        alwaysBox.add(Box.createHorizontalStrut(20));
        alwaysBox.add(ask);
        alwaysBox.add(Box.createHorizontalStrut(15));
        alwaysBox.add(saveAs);
        alwaysBox.add(Box.createHorizontalStrut(15));
        alwaysBox.add(append);
        alwaysBox.add(Box.createHorizontalGlue());
        Box outerAlways = Box.createVerticalBox();
        outerAlways.add(alwaysLabelBox);
        outerAlways.add(alwaysBox);
        outerAlways.add(Box.createVerticalStrut(15));
        topPanel.add(outerAlways, BorderLayout.CENTER);
        
		buttons = new ButtonRow(new Action[] { new OverWriteAction(),
				new SaveAsAction(),  new AppendAction(), new CancelAction() },
				ButtonRow.X_AXIS, ButtonRow.LEFT_GLUE);
		topPanel.add(buttons, BorderLayout.SOUTH);
		
		setContentFromException(sle);
		pack();
	}
    
    private void setupDefaultOption(JRadioButton button, final int option) {
        if(QuestionsHandler.DEFAULT_ACTION_FILE_EXISTS.getValue() == option)
            button.setSelected(true);
        
        button.setFocusable(false);
        button.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if ( e.getStateChange() == ItemEvent.SELECTED )
                    QuestionsHandler.DEFAULT_ACTION_FILE_EXISTS.setValue(option);
            }
        });
    }
	
	private void setLabel(JLabel label, String text) {
        StringBuilder buffer = new StringBuilder("<html><table><tr><td width=\"400\">");
		buffer.append(text);
		buffer.append("</td></tr></html>");
		label.setText(buffer.toString());
	}
	
	private void setContentFromException(SaveLocationException sle) {
		
		// special case, close this dialog if visible and show warning dialog
		if (sle.getErrorCode() == LocationCode.FILE_ALREADY_DOWNLOADING) {
			dispose();
			DownloaderUtils.showIsAlreadyDownloadingWarning(factory);
			return;
		}
		
		//Ensure that the dialog is setup correctly -- this is necessary
        //for the case where a default action is set, but performing that
        //action caused an exception, requiring the dialog to be displayed.
        //Note that setUpDialog calls this method, but with buttons != null.
		if (buttons == null) {
			setUpDialog(sle);
			setLocationRelativeTo(MessageService.getParentComponent());
			setVisible(true);
			return;
		}
		
		String error = CoreExceptionHandler.getSaveLocationErrorString(sle, true);
		
		if (sle.getErrorCode() == LocationCode.FILE_ALREADY_EXISTS) {
			setTitleLabel(MessageFormat.format(I18n.tr
					("Warning: {0}"),
					new Object[] { CoreExceptionHandler.getShortSaveLocationErrorString(sle) }));
			setDescLabel(error);
		}
		else {
			setTitleLabel(MessageFormat.format(I18n.tr
					("Error: {0}"),
					new Object[] { CoreExceptionHandler.getShortSaveLocationErrorString(sle) }));
			setDescLabel(error);
		}
		
		FileDesc desc = DownloaderUtils.getFromLibrary(factory.getURN());
		if (desc != null) {
			setNoteLabel(MessageFormat.format(I18n.tr
					("<b>Note</b>: A file with the same content already exists in the library at {0}"),
					new Object[] { "<i>" + 
                GUIUtils.convertToNonBreakingSpaces(0, desc.getFile().toString()) + "</i>" }));
		}
		
		buttons.getButtonAtIndex(0).setVisible
			(sle.getErrorCode() == LocationCode.FILE_ALREADY_EXISTS);
	}
	
	private void setTitleLabel(String text) {
		setTitle(text);
		titleLabel.setText(text);
	}
	
	private void setDescLabel(String text) {
		setLabel(descLabel, text);
	}
	
	private void setNoteLabel(String text) {
		setLabel(noteLabel, text);
	}
	
	public static Downloader handle(GuiDownloaderFactory factory,
			SaveLocationException sle) {
		
		if (sle.getErrorCode() == LocationCode.FILE_ALREADY_DOWNLOADING) {
			DownloaderUtils.showIsAlreadyDownloadingWarning(factory);
			return null;
		}
		
        // Capture the default action before we construct the dialog,
        // otherwise it can change before constructing returns.
        int defaultAction = QuestionsHandler.DEFAULT_ACTION_FILE_EXISTS.getValue();
		DownloaderDialog dlg = new DownloaderDialog(factory, sle);
		if(defaultAction == 0) {
			dlg.setLocationRelativeTo(MessageService.getParentComponent());
			dlg.setVisible(true);
		}
		return dlg.getDownloader();
	}
	
	/**
	 * Returns the successfully created downloader or <code>null</code> if
	 * the dialog was cancelled.
	 * @return
	 */
	public Downloader getDownloader() {
		return downloader;
	}
	
	/**
	 * Uses <code>factory</code> to create a downloader with a unique save
	 * filename. If a file with the same name exists <code>(#)</code> where
	 * <code>#</code> is a progressively increasing number is inserted before
	 * the file extension.
	 */
	void createUniqueFilenameDownloader() {
		String originalName = originalFile.getName();
		String preExt = originalName;
		String ext = "";
		int dot = originalName.lastIndexOf(".");
		if (dot != -1) {
		    preExt = originalName.substring(0, dot);
		    ext = originalName.substring(dot);
		}

		Set downloads = DownloadMediator.instance().getFileNames();
		File newFile = originalFile;
		String newName = originalName;
		for (int i = 1; newFile.exists() || downloads.contains(newName); i++) {
		    newName = preExt + "(" + i + ")" + ext;
		    newFile = new File(originalFile.getParent(), newName);
		}

		try {
			factory.setSaveFile(newFile);
			downloader = factory.createDownloader(false);
			dispose();
		} catch (SaveLocationException sle) {
			setContentFromException(sle);
		}
	}

	private class OverWriteAction extends AbstractAction {
		
		public OverWriteAction() {
			putValue(Action.NAME, I18n.tr
					("Overwrite"));
			putValue(Action.SHORT_DESCRIPTION, I18n.tr
					("Overwrite the Existing File"));
		}

		public void actionPerformed(ActionEvent e) {
			try {
				downloader = factory.createDownloader(true);
				dispose();
			}
			catch (SaveLocationException sle) {
				setContentFromException(sle);
			}
		}
	}
	
	private class SaveAsAction extends AbstractAction {
		
		public SaveAsAction() {
			putValue(Action.NAME, I18n.tr
					("Save As..."));
			putValue(Action.SHORT_DESCRIPTION, I18n.tr
					("Choose a different location for the Download"));
		}

		public void actionPerformed(ActionEvent e) {
			File file = DownloaderUtils.showFileChooser(factory, 
					DownloaderDialog.this);
			if (file != null) {
				try {
					factory.setSaveFile(file);
					// OSX's FileDialog box already prompts the user that they're
					// going to be overwriting a file, so we don't need to do that
					// particular check again.
					downloader = factory.createDownloader(OSUtils.isAnyMac());
					dispose();
				}
				catch (SaveLocationException sle) {
					setContentFromException(sle);
				}
			}
		}
	}
	
	private class AppendAction extends AbstractAction {
		
		public AppendAction() {
			putValue(Action.NAME, I18n.tr
					("Append (#)"));
			putValue(Action.SHORT_DESCRIPTION, I18n.tr
					("Appends (#) to filename"));
		}

		public void actionPerformed(ActionEvent e) {
            createUniqueFilenameDownloader();
		}
	}
	
	private class CancelAction extends AbstractAction {
		
		public CancelAction() {
			putValue(Action.NAME, I18n.tr
					("Cancel"));
			putValue(Action.SHORT_DESCRIPTION, I18n.tr
					("Cancel the Download"));
		}

		public void actionPerformed(ActionEvent e) {
			dispose();
		}
	}
}
