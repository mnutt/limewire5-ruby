package com.limegroup.gnutella.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.limewire.core.settings.QuestionsHandler;
import org.limewire.core.settings.UISettings;

import com.limegroup.gnutella.gui.dnd.DNDUtils;
import com.limegroup.gnutella.gui.themes.ThemeFileHandler;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeObserver;

/**
 * Control the monitoring of Query Requests and present the requests
 * to the user.
 */
public class MonitorView extends JPanel implements ThemeObserver {

    //  Components on this panel
    private JCheckBox showQueriesCheckBox;
    private JTextField listSizeTextField;
    private JList listOfQueries;
    private JScrollPane scrollPane;

    private int listFixedSize;
    private static final int LIST_FIXED_SIZE_DEFAULT = 32;
    private boolean bypassCountChange = false;

    private BufferListModel<String> model = new BufferListModel<String>(LIST_FIXED_SIZE_DEFAULT);

    /**
     * Creates a new MonitorView, which is the GUI component that allows
     * the user to control the monitoring of incoming search queries. 
     */
    public MonitorView() {
		GUIMediator.setSplashScreenString(
		    I18n.tr("Loading Monitor Window..."));  

        //  Create the controls 
        int pad = GUIConstants.SEPARATOR;
        JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));
		
		//  Create the "Show" check box
        showQueriesCheckBox = new JCheckBox(I18n.tr("Show Incoming Searches"));
        showQueriesCheckBox.setPreferredSize(new Dimension(200, 20));
        showQueriesCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (listOfQueries != null)
                    listOfQueries.setEnabled(showQueriesCheckBox.isSelected());
                if (listSizeTextField != null)
                    listSizeTextField.setEnabled(showQueriesCheckBox.isSelected());

                //  Uncomment this line to persist the value of the "Show" check box.
                //  UISettings.UI_MONITOR_SHOW_INCOMING_SEARCHES.setValue(showQueriesCheckBox.isSelected());

                if (e.getStateChange() == ItemEvent.SELECTED && GuiCoreMediator.getConnectionServices().isShieldedLeaf()) {
                    GUIMediator.showMessage(I18n.tr("Your client is a leaf node shielded by an Ultrapeer. Hence you will see few or no searches in the monitor window."), QuestionsHandler.MONITOR_VIEW);
                }
            }
        });
        showQueriesCheckBox.setSelected(UISettings.UI_MONITOR_SHOW_INCOMING_SEARCHES.getValue());
        
		controlPanel.add(showQueriesCheckBox);
		controlPanel.add(Box.createHorizontalGlue());
		controlPanel.add(new JLabel(I18n.tr("Show Last")));
		controlPanel.add(Box.createHorizontalStrut(pad / 2));

		//  Create the list size check box
		listFixedSize = LIST_FIXED_SIZE_DEFAULT;
		listSizeTextField = new WholeNumberField(listFixedSize, 3);
		Dimension d = new Dimension(16, 24);
		listSizeTextField.setPreferredSize(d);
		listSizeTextField.setMaximumSize(d);
        listSizeTextField.setEnabled(showQueriesCheckBox.isSelected());
        listSizeTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                handleCountChange();
            }
            public void removeUpdate(DocumentEvent e) {
                handleCountChange();
            }
            public void changedUpdate(DocumentEvent e) {
            }
        });
		
        controlPanel.add(listSizeTextField);
		controlPanel.add(Box.createHorizontalStrut(pad / 2));
        controlPanel.add(new JLabel(I18n.tr("Searches")));

		//  Create the list inside a scroll pane, and trigger search on double-click
		listOfQueries = new JList(model);
        listOfQueries.setPrototypeCellValue("                                      ");
        listOfQueries.setFixedCellHeight(16);
        listOfQueries.setEnabled(showQueriesCheckBox.isSelected());
		listOfQueries.setTransferHandler(DNDUtils.DEFAULT_TRANSFER_HANDLER);
        listOfQueries.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    int index = listOfQueries.locationToIndex(e.getPoint());
                    if(index != -1) {
                        String str = model.getElementAt(index);
                        GUIMediator.instance().triggerSearch(str);
                    }
                }
            }
        });
        scrollPane = new JScrollPane(listOfQueries);
        scrollPane.setPreferredSize(new Dimension(340, 180));

        //  Add control panel and scroll pane to this
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(pad, pad, pad, pad);
        gbc.gridy = 0;
        add(controlPanel, gbc);

        gbc.gridy = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, pad, pad, pad);
        add(scrollPane, gbc);

		updateTheme();
		ThemeMediator.addThemeObserver(this);
    }

	/**
	 * Updates the appearance of this panel based on the current theme.
	 */
	public void updateTheme() {
		Color tableColor = ThemeFileHandler.TABLE_BACKGROUND_COLOR.getValue();
		scrollPane.getViewport().setBackground(tableColor);
		listOfQueries.setBackground(tableColor);
	}

	/**
	 * The user has changed the desired list size, so enact the change. 
	 */
    private void handleCountChange() {
        if(bypassCountChange)
            return;
    
        String val = listSizeTextField.getText();
        if(val != null && !val.equals("")) {
            int size;
            try {
                size = Integer.parseInt(val);
            } catch(NumberFormatException nfe) {
                size = 1001; // hack it over the limit.
            }
            
            if(size > 1000) {
                //  need to set the count outside the document listener 
                listFixedSize = LIST_FIXED_SIZE_DEFAULT;
                Runnable runner = new Runnable() {
                    public void run() {
                        bypassCountChange = true;
                        listSizeTextField.setText(String.valueOf(listFixedSize));
                        bypassCountChange = false;
                    }
                };
                SwingUtilities.invokeLater(runner);
            } else {
                listFixedSize = size;
            }
			model.changeSize(listFixedSize);
            scrollPane.validate();
        }
    }

    /**
     * Receives a given query string, <tt>query</tt>, and presents
     * the query string to the user if queries are being shown.
     */
    public void handleQueryString(String query) {
        if(!showQueriesCheckBox.isSelected()) 
            return;
        if(query.length() == 0)
            return;

        char c = query.charAt(query.length() - 1);
        if (Character.isISOControl(c) || !Character.isDefined(c)) {
            query = query.substring(0, query.length() - 1);
        }
        model.addFirst(query);
    }
}
