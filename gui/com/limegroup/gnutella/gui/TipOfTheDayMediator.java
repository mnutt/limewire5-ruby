package com.limegroup.gnutella.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;

import org.limewire.core.settings.StartupSettings;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.gui.themes.ThemeFileHandler;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeObserver;
import com.limegroup.gnutella.util.LimeWireUtils;

public final class TipOfTheDayMediator implements ThemeObserver {

    private static final int TIP_HEIGHT = 180;

    private static final int TIP_WIDTH = 360;

    /**
     * The instance of this class.
     */
    private static TipOfTheDayMediator instance;

    /**
     * The title for the TOTD window.
     */
    private static final String TOTD_TITLE = I18n.tr("Tip of the Day");

    /**
     * The 'Did You Know' intro.
     */
    private static final String TOTD_INTRO = I18n.tr("Did You Know...");

    /**
     * The 'Show Tips At Startup' string
     */
    private static final String TOTD_STARTUP = I18n
            .tr("Show Tips At Startup");

    /**
     * 'Next'.
     */
    private static final String TOTD_NEXT = I18n.tr("Next Tip");

    /**
     * 'Previous'.
     */
    private static final String TOTD_PREVIOUS = I18n.tr("Previous Tip");

    /**
     * 'Close'.
     */
    private static final String TOTD_CLOSE = I18n.tr("Close");

    /**
     * The actual TOTD JDialog.
     */
    private final JDialog dialog;

    /**
     * The JTextComponent that displays the tip.
     */
    private final JEditorPane tipPane;

    /**
     * The 'Previous' JButton. Global so it can be enabled/disabled.
     */
    private final JButton previousButton;

    /**
     * The list of Tip of the Day messages.
     */
    private final List<String> messages;

    /**
     * The index of the current tip.
     */
    private static int _currentTip;

    /**
     * The foreground color to use for text.
     */
    private static Color _foreground;

    /**
     * Whether or not we can display the TOTD dialog.
     */
    private boolean _canDisplay = true;

    /**
     * Private constructor that initiates the appropriate things for the TOTD.
     */
    private TipOfTheDayMediator() {
        tipPane = new JEditorPane();
        dialog = new JDialog();
        this.messages = new ArrayList<String>();
        initializeMessages(this.messages);

        dialog.setModal(false);
        dialog.setResizable(true);
        dialog.setTitle(TOTD_TITLE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                // free up instance for garbage collection
                instance = null;
            }
        });

        // Previous' listener must be added here instead of
        // in constructDialog because otherwise multiple
        // listeners will be added when the theme changes.
        previousButton = new JButton(TOTD_PREVIOUS);
        previousButton.addActionListener(new PreviousTipListener());
        constructDialog();
        ThemeMediator.addThemeObserver(this);
    }

    /**
     * Returns the sole instance of this class.
     */
    public static synchronized TipOfTheDayMediator instance() {
        if (instance == null)
            instance = new TipOfTheDayMediator();
        return instance;
    }
    
    public static synchronized boolean isConstructed() {
        return instance != null;
    }

    /**
     * Redraws the whole dialog upon theme change.
     */
    public void updateComponentTreeUI() {
        SwingUtilities.updateComponentTreeUI(dialog);
    }

    /**
     * Causes the TOTD window to become visible.
     */
    public void displayTipWindow() {
        GUIMediator.safeInvokeLater(new Runnable() {
            public void run() {
                if (!_canDisplay)
                    return;

                if (dialog.isShowing()) {
                    dialog.setVisible(false);
                    dialog.setVisible(true);
                    dialog.toFront();
                    return;
                }

                GUIUtils.centerOnScreen(dialog);
                dialog.setVisible(true);

                if (!"text/html".equals(tipPane.getContentType())) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            tipPane.setContentType("text/html");
                            setText(getRandomTip());
                        }
                    });
                }

                dialog.toFront();
            }
        });
    }

    /**
     * Hides the TOTD dialogue window.
     */
    public void hide() {
        dialog.setVisible(false);
    }

    /**
     * Sets the text of the tip to a new tip.
     */
    private void setText(String tip) {
        String foreHex = GUIUtils.colorToHex(_foreground);
        tipPane.setText("<html><font face='arial,helvetica,sanserif' color='#" + foreHex + "'>" + tip + "</font>");
        tipPane.setCaretPosition(0);
    }

    /**
     * Iterates through all the tips' keys and stores the ones that are valid
     * for this OS.
     */
    private void initializeMessages(List<String> messages) {
        messages.addAll(Arrays.asList(TipOfTheDayMessages.getGeneralMessages()));

        if (OSUtils.isWindows()) {
            messages.addAll(Arrays.asList(TipOfTheDayMessages.getWindowsMessages()));
        } else if (OSUtils.isMacOSX()) {
            messages.addAll(Arrays.asList(TipOfTheDayMessages.getMacOSXMessages()));
        } else if (OSUtils.isLinux()) {
            messages.addAll(Arrays.asList(TipOfTheDayMessages.getLinuxMessages()));
        } else {
            messages.addAll(Arrays.asList(TipOfTheDayMessages.getOtherMessages()));
        }

        if (!OSUtils.isMacOSX()) {
            messages.addAll(Arrays.asList(TipOfTheDayMessages.getNonMacOSXMessages()));
        }

        if (LimeWireUtils.isPro()) {
            messages.addAll(Arrays.asList(TipOfTheDayMessages.getProMessages()));
        } else {
            messages.addAll(Arrays.asList(TipOfTheDayMessages.getBasicMessages()));
        }
        
        for(String[] msg : TipOfTheDayMessages.getCustomMessages()) {
            messages.add(TipOfTheDayMessages.buildCustom(msg));
        }

        // randomize the list.
        Collections.shuffle(messages);
        _currentTip = -1;
    }

    /**
     * Retrieves a random tip and updates the _currentTip index to that tip.
     */
    private String getRandomTip() {
        if (messages.size() == 0) {
            throw new RuntimeException("No tips of the day");
        }

        // If this is our last key, reshuffle them.
        if (_currentTip == messages.size() - 1) {
            Collections.shuffle(messages);
            _currentTip = -1;
        } else if (_currentTip < -1) {
            _currentTip = -1;
        }

        String message = messages.get(++_currentTip);

        if (_currentTip == 0)
            previousButton.setEnabled(false);
        else
            previousButton.setEnabled(true);

        return message;
    }

    /**
     * Recreates the dialog box to update the theme.
     */
    public void updateTheme() {
        boolean wasShowing = dialog.isShowing();

        dialog.setVisible(false);
        dialog.getContentPane().removeAll();
        // Lower the size of the font in the TIP because
        // it's going to get larger again.
        Font tipFont = new Font(tipPane.getFont().getName(), tipPane.getFont().getStyle(), tipPane
                .getFont().getSize() - 2);
        tipPane.setFont(tipFont);
        constructDialog();
        tipPane.setContentType("text/html");
        setText(getRandomTip());

        if (wasShowing) {
            dialog.setVisible(true);
            dialog.toFront();
        }
    }

    /**
     * Builds the TOTD dialog.
     */
    private void constructDialog() {
        Color darkColor = ThemeFileHandler.TABLE_ALTERNATE_COLOR.getValue().darker();
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(darkColor);
        centerPanel.setOpaque(true);
        centerPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        // left column
        JLabel iconLabel = new JLabel(GUIMediator.getThemeImage("question"));
        _foreground = iconLabel.getForeground();
        iconLabel.setOpaque(false);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        centerPanel.add(iconLabel, BorderLayout.WEST);

        // right column
        JPanel tipPanel = new JPanel(new BorderLayout());
        centerPanel.add(tipPanel, BorderLayout.CENTER);
        tipPanel.setBackground(UIManager.getColor("TextField.background"));
        tipPanel.setOpaque(true);

        JLabel titleLabel = new JLabel(TOTD_INTRO);
        titleLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,
                0, 1, 0, darkColor), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        Font titleFont = new Font("Dialog", titleLabel.getFont().getStyle(), titleLabel
                .getFont().getSize() + 5);
        titleLabel.setFont(titleFont);
        tipPanel.add(titleLabel, BorderLayout.NORTH);

        // THE HTML ENGINE TAKES TOO LONG TO LOAD, SO WE MUST LOAD AS TEXT.
        tipPane.setContentType("text");
        tipPane.setEditable(false);
        // tipPane.setBackground(tipPanel.getBackground());
        Font tipFont = new Font("Dialog", tipPane.getFont().getStyle(),
                tipPane.getFont().getSize() + 2);
        tipPane.setFont(tipFont);
        tipPane.addHyperlinkListener(GUIUtils.getHyperlinkListener());
        tipPane.setText(I18n.tr("Loading tips..."));
        tipPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane tipScroller = new JScrollPane(tipPane);
        tipScroller.setPreferredSize(new Dimension(TIP_WIDTH, TIP_HEIGHT));
        tipScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tipScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tipScroller.setBorder(null);
        tipPanel.add(tipScroller, BorderLayout.CENTER);

        // construct button panel
        JPanel startupPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox showTips = new JCheckBox(TOTD_STARTUP);
        showTips.setSelected(StartupSettings.SHOW_TOTD.getValue());
        startupPanel.add(showTips);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        // the button takes up too much space making the dialog too wide
        // buttonPanel.add(previousButton);
        JButton next = new JButton(TOTD_NEXT);
        buttonPanel.add(next);
        JButton close = new JButton(TOTD_CLOSE);
        buttonPanel.add(close);

        JPanel navigation = new JPanel(new BorderLayout());
        navigation.add(startupPanel, BorderLayout.WEST);
        navigation.add(buttonPanel, BorderLayout.EAST);

        showTips.addActionListener(new ShowTipListener());
        next.addActionListener(new NextTipListener());
        close.addActionListener(GUIUtils.getDisposeAction());

        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // construct content pane
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        contentPanel.add(centerPanel, BorderLayout.CENTER);
        contentPanel.add(navigation, BorderLayout.SOUTH);
        dialog.setContentPane(contentPanel);
        GUIUtils.addHideAction((JComponent) dialog.getContentPane());
        try {
            dialog.pack();
        } catch (OutOfMemoryError oome) {
            // who knows why it happens, but it's an internal error.
            _canDisplay = false;
        }
    }

    /**
     * A listener for changing the state of the 'Show Tips on Startup'.
     */
    private class ShowTipListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            JCheckBox source = (JCheckBox) e.getSource();
            StartupSettings.SHOW_TOTD.setValue(source.isSelected());
        }
    }

    /**
     * A listener for showing the next tip.
     */
    private class NextTipListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            setText(getRandomTip());
        }
    }

    /**
     * A listener for showing the previous tip.
     */
    private class PreviousTipListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            _currentTip = _currentTip - 2;
            setText(getRandomTip());
        }
    }
}
