package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * This class is a panel containing three "action" buttons
 * for the actions "download", "View File Info" and "mark as junk".
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ActionButtonPanel extends JXPanel {

    private final String[] TOOLTIPS =
        { tr("Download"), tr("View File Info"), tr("Mark as Spam") };
    
    private static final int DOWNLOAD = 0;    
    private static final int MORE_INFO = 1;    
    private static final int MARK_AS_SPAM = 2;    

    @Resource private Icon downloadDownIcon;
    @Resource private Icon downloadOverIcon;
    @Resource private Icon downloadUpIcon;
    @Resource private Icon infoDownIcon;
    @Resource private Icon infoOverIcon;
    @Resource private Icon infoUpIcon;
    @Resource private Icon spamDownIcon;
    @Resource private Icon spamOverIcon;
    @Resource private Icon spamUpIcon;
    @Resource private Icon spamActiveIcon;

    private final DownloadHandler downloadHandler;
    private Icon[][] icons;
    private JButton downloadButton;
    private JButton infoButton;
    private JToggleButton spamButton;
    private int height;
    private VisualSearchResult vsr;

    public ActionButtonPanel(DownloadHandler downloadHandler, final JTable table) {
        GuiUtils.assignResources(this);
        
        this.downloadHandler = downloadHandler;

        downloadButton = new JButton() {
            @Override
            public Point getToolTipLocation(MouseEvent e) {
                return getToolTipOffset(this);
            }
        };
        
        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startDownload();
                table.editingStopped(new ChangeEvent(table));
            }
        });
        
        infoButton = new JButton() {
            @Override
            public Point getToolTipLocation(MouseEvent e) {
                return getToolTipOffset(this);
            }
        };
        
        spamButton = new JToggleButton() {
            @Override
            public Point getToolTipLocation(MouseEvent e) {
                return getToolTipOffset(this);
            }
        };
        
        icons = new Icon[][] {
            { downloadUpIcon, downloadOverIcon, downloadDownIcon, null },
            { infoUpIcon, infoOverIcon, infoDownIcon, null},
            { spamUpIcon, spamOverIcon, spamDownIcon, spamActiveIcon }
        };
        calculateHeight();

        setLayout(new GridLayout(1, 3));
        addButtonsToPanel();
        
        // Set the tooltip delay to zero only when the mouse is over this panel.
        addMouseListener(new MouseAdapter() {
            private int delay;

            @Override
            public void mouseEntered(MouseEvent e) {
                // Save the previous delay.
                delay = ToolTipManager.sharedInstance().getInitialDelay();
                ToolTipManager.sharedInstance().setInitialDelay(1);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Restore the previous delay.
                ToolTipManager.sharedInstance().setInitialDelay(delay);
            }
        });
    }
    
    public void startDownload() {
        downloadHandler.download(vsr);
        setDownloadingDisplay(vsr);
    }
    
    private void calculateHeight() {
        for (Icon[] iconSet : icons) {
            Icon upIcon = iconSet[0];
            height = Math.max(height, upIcon.getIconHeight());
        }
    }

    private void addButtonsToPanel() {
        int buttonIndex = 0;

        for (Icon[] iconSet : icons) {
            Icon upIcon = iconSet[0];
            Icon overIcon = iconSet[1];
            Icon downIcon = iconSet[2];
            Icon selectedIcon = iconSet[3];

            AbstractButton button = getButton(buttonIndex);
            if (button == null) continue; // should never happen

            button.setIcon(upIcon);
            button.setRolloverIcon(overIcon);
            button.setPressedIcon(downIcon);
            button.setSelectedIcon(selectedIcon == null ? downIcon : selectedIcon);

            button.setToolTipText(TOOLTIPS[buttonIndex++]);

            button.setBorderPainted(false);
            button.setContentAreaFilled(false);

            Dimension size =
                new Dimension(upIcon.getIconWidth(), upIcon.getIconHeight());
            button.setPreferredSize(size);

            add(button, buttonIndex == TOOLTIPS.length ? "wmax pref, wrap" : "wmax pref");
        }
    }

    private AbstractButton getButton(int buttonIndex) {
        return buttonIndex == DOWNLOAD ? downloadButton :
            buttonIndex == MORE_INFO ? infoButton :
            buttonIndex == MARK_AS_SPAM ? spamButton : null;
    }
    
    /**
     * Gets the "Mark as Spam" button.
     * @return the button
     */
    public JToggleButton getSpamButton() {
        return spamButton;
    }

    private Point getToolTipOffset(JComponent component) {
        // Determine the size of the tooltip.
        Font font = component.getFont();
        FontMetrics fm = component.getFontMetrics(font);
        String text = component.getToolTipText();
        int toolTipWidth = fm.stringWidth(text);
        int toolTipHeight = fm.getHeight();

        Dimension componentSize = component.getSize();

        int x = componentSize.width/2 - toolTipWidth/2 + 1;
        int y = -toolTipHeight - 3;

        return new Point(x, y);
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        
        Component[] components = getComponents();
        for (Component component : components) {
            component.setBackground(bg);
        }
    }

    public void setDownloadingDisplay(VisualSearchResult vsr) {
        downloadButton.setEnabled(vsr.getDownloadState() == BasicDownloadState.NOT_STARTED);
    }

    public void configureForListView(final JTable table) {
        removeAll();
        setLayout(new MigLayout("insets 0 0 0 0", "[]15[]15[]", "[]"));
        addButtonsToPanel();
    }
    
    public void prepareForDisplay(VisualSearchResult vsr) {
        this.vsr = vsr;
        
        boolean spam = vsr.isSpam();
        setAlpha(spam ? 0.2f : 1.0f);
        setDownloadingDisplay(vsr);
        getSpamButton().setSelected(spam);
    }
}