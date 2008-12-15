package com.limegroup.gnutella.gui.search;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.OverlayLayout;

import org.limewire.core.settings.UISettings;
import org.limewire.core.settings.UISettings.ImageInfo;

import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.themes.ThemeObserver;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * A JPanel designed to be used as an overlay in the default
 * search window.
 */
class OverlayAd extends JPanel implements ThemeObserver {
            
    /** The icon to close the overlay. */
    private final Icon CLOSER;
    
    /** The background image. */
    private ImageIcon _image = null;
    
    /** Whether or not this is still the 'Getting Started'. */
    private boolean _searchDone;
    
    /** Future for loading intro. */
    private final Future<Void> introFuture;
    
    private final ImageInfo introInfo = UISettings.INTRO_IMAGE_INFO;
    private final ImageInfo afterSearchInfo = UISettings.AFTER_SEARCH_IMAGE_INFO;
   
    /**
     * Constructs a new OverlayAd, starting with the 'Getting Started'
     * image/text.
     */
    OverlayAd() {
        super();
        setLayout(new OverlayLayout(this));
        CLOSER = GUIMediator.getThemeImage("kill_on");
        LoadImageThenRun runner = new LoadImageThenRun(introInfo,
            new LateImageRunner() {
                public void runWithImage(ImageIcon img, boolean usingBackup) {
                    if(_image != null)
                        return;
                    
                    _image = img;
                    Dimension preferredSize =
                        new Dimension(_image.getIconWidth(), _image.getIconHeight());
                    setMaximumSize(preferredSize);
                    setPreferredSize(preferredSize);
                    
                    try {
                        if(usingBackup)
                            add(createTextPanel(introInfo));
                        else if(!afterSearchInfo.canProShowPic() && LimeWireUtils.isPro())
                            add(createCloserPanel());
                        add(createImagePanel(getUrl(introInfo, usingBackup)));
                        GUIUtils.setOpaque(false, OverlayAd.this);
                    } catch(NullPointerException npe) {
                        // internal error w/ swing
                        setVisible(false);
                        _searchDone = true;
                    }
                }
            }
        , "intro");
        introFuture = runner.run();
    }
    
    /**
     * Resets everything to be opaque.
     */
    public void updateTheme() {
        GUIUtils.setOpaque(false, this);
    }
    
    /**
     * Changes the overlay after a search is done.
     */
    void searchPerformed() {
        if(_searchDone)
            return;
        
        _searchDone = true;
        
        // If no 'after search' image should show for pro, exit early.
        if(!afterSearchInfo.canProShowPic() && LimeWireUtils.isPro()) {
            setVisible(false);
            return;
        }
        
        LoadImageThenRun runner = new LoadImageThenRun(afterSearchInfo,
            new LateImageRunner() {
                public void runWithImage(ImageIcon img, boolean usingBackup) {
                    introFuture.cancel(false);
                    _image = img;
                    
                    // Don't show the ad if using backup & we're PRO.
                    if(usingBackup && LimeWireUtils.isPro()) {
                        OverlayAd.this.setVisible(false);
                        return;
                    }
                    
                    Dimension preferredSize =
                        new Dimension(_image.getIconWidth(), _image.getIconHeight());
                    setMaximumSize(preferredSize);
                    setPreferredSize(preferredSize);
                        
                    try {
                        removeAll();
                        if(usingBackup)
                            add(createTextPanel(afterSearchInfo));
                        add(createImagePanel(getUrl(afterSearchInfo, usingBackup)));
                        GUIUtils.setOpaque(false, OverlayAd.this);
                    } catch(NullPointerException npe) {
                        // internal error w/ swing
                        setVisible(false);
                        _searchDone = true;
                    }
                }
            }
        , "gopro");
        runner.run();
    }
    
    private String getUrl(ImageInfo info, boolean backup) {
        if(!info.canLink()) {
            return null;
        } else if(backup) {
            return info.getLocalLinkUrl();
        } else {
            return info.getNetworkLinkUrl();
        }
    }
    
    /**
     * Creates the background image panel.
     */
    private JPanel createImagePanel(String url) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(_image), BorderLayout.CENTER);
        panel.setMaximumSize(getMaximumSize());
        panel.setPreferredSize(getPreferredSize());
        if(url != null && !url.equals(""))
            panel.addMouseListener(new Launcher(url));
        return panel;
    }
    
    /**
     * Creates the text panel, with either 'go pro' text or 'getting started'
     * text.
     * @param goPro whether or not the text is for going pro or getting started
     */
    private JPanel createTextPanel(ImageInfo info) {
        JPanel panel = new JPanel(new BorderLayout());
        
        if(LimeWireUtils.isPro() || !info.isIntro())
            panel.add(createNorthPanel(LimeWireUtils.isPro()), BorderLayout.NORTH);
        panel.add(Box.createHorizontalStrut(18), BorderLayout.WEST);
        JPanel center = !info.isIntro() ? 
                    createGoProCenter() : createGettingStartedCenter();
        panel.add(center, BorderLayout.CENTER);
        panel.add(Box.createHorizontalStrut(18), BorderLayout.EAST);

        panel.setMaximumSize(getMaximumSize());
        panel.setPreferredSize(getPreferredSize());
        return panel;
    }
    
    private JPanel createCloserPanel() {
        JPanel panel = new JPanel(new BorderLayout());            
        panel.add(createNorthPanel(true), BorderLayout.NORTH);
        return panel;
    }
    
    /**
     * Creates the north panel, with the closer icon.
     */
    private JPanel createNorthPanel(boolean useCloser) {
        JPanel box = new BoxPanel(BoxPanel.X_AXIS);
        box.add(Box.createHorizontalGlue());
        if(useCloser) {
            JLabel closer = new JLabel(CLOSER);
            closer.addMouseListener(new MouseListener() {
                public void mouseClicked(MouseEvent e) {
                    OverlayAd.this.setVisible(false);
                }
                public void mousePressed(MouseEvent e) {}
                public void mouseReleased(MouseEvent e) {}
                public void mouseEntered(MouseEvent e) {}
                public void mouseExited(MouseEvent e) {}
            });
            box.add(closer);
        } else {
            box.add(Box.createVerticalStrut(CLOSER.getIconHeight()));
        }
        return box;
    }
    
    /** Creates the backup getting started text overlay. */
    private JPanel createGettingStartedCenter() {
        JLabel title = new JLabel(
            I18n.tr("Getting Started"));
        title.setFont(new Font("Dialog", Font.PLAIN, 24));
        title.setForeground(new Color(0x5A, 0x76, 0x94));
        
        JTextArea text = new JTextArea(
            I18n.tr("To start using LimeWire, find the text field on the left, type in what you are looking for and click the \"Search\" button. If you are looking for only one type of content (music, video, etc...), you can narrow your search results by using the buttons above the text field."));
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setEditable(false);
        text.setOpaque(false);
        text.setForeground(new Color(0x00, 0x00, 0x00));
        text.setFont(new Font("Dialog", Font.PLAIN, 14));
        
        JScrollPane pane = new JScrollPane(text);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        JPanel box = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(40, 0, 5, 0);
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        box.add(title, c);
        c.weightx = 1;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 0);
        box.add(pane, c);
        
        String url = getUrl(introInfo, true);
        if(url != null && !url.equals("")) {
            MouseListener launcher = new Launcher(url);
            box.addMouseListener(launcher);
            text.addMouseListener(launcher);
            title.addMouseListener(launcher);
            addMouseListener(launcher);
        }
        
        return box;
    }
    
    /** Creates the backup Go Pro text overlay. */
    private JPanel createGoProCenter() {
        JLabel title = new JLabel(
            I18n.tr("The Most Advanced File Sharing Program on the Planet!"));
        title.setFont(new Font("Dialog", Font.BOLD, 16));
        title.setForeground(new Color(0x43, 0x43, 0x43));
        
        JTextArea text = new JTextArea(
            I18n.tr("Better search results\nTurbo-Charged download speeds\nConnections to more sources\nPersonalized tech support\nSix months of free updates"));
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setEditable(false);
        text.setOpaque(false);
        text.setForeground(new Color(0x43, 0x43, 0x43));
        text.setFont(new Font("Dialog", Font.PLAIN, 14));
        
        JPanel textPanel = new BoxPanel(BoxPanel.X_AXIS);
        textPanel.add(Box.createHorizontalStrut(185));
        textPanel.add(text);
        
        final JPanel box = new BoxPanel(BoxPanel.Y_AXIS);
        box.add(GUIUtils.center(title));
        box.add(Box.createVerticalStrut(80));
        box.add(textPanel);
        
        String url = getUrl(afterSearchInfo, true);
        if(url != null && !url.equals("")) {
            MouseListener launcher = new Launcher(url);
            box.addMouseListener(launcher);
            text.addMouseListener(launcher);
            title.addMouseListener(launcher);
            textPanel.addMouseListener(launcher);
        }
        
        return box;
    }
    
    private static class Launcher implements MouseListener {
        private final String url;
        
        Launcher(String url) {
            this.url = url;
        }
        
        public void mouseClicked(MouseEvent e) {
            if(!e.isConsumed()) {
                e.consume();
                byte[] guid = GuiCoreMediator.getApplicationServices().getMyGUID();
                String finalUrl = LimeWireUtils.addLWInfoToUrl(url, guid);
                GUIMediator.openURL(finalUrl);
            }
        }
        
        public void mousePressed(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}
        public void mouseEntered(MouseEvent e) {
            ((JComponent)e.getComponent()).getTopLevelAncestor().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        public void mouseExited(MouseEvent e) {
            ((JComponent)e.getComponent()).getTopLevelAncestor().setCursor(Cursor.getDefaultCursor());
        }
    };    
    
    private static class LoadImageThenRun {
        private final ImageInfo imageInfo;
        private final LateImageRunner runner;
        private final String backupImage;
        
        public LoadImageThenRun(ImageInfo imageInfo, LateImageRunner runner, String backupImage) {
            this.imageInfo = imageInfo;
            this.runner = runner;
            this.backupImage = backupImage;
        }

        private ImageIcon image() {
            String url = imageInfo.getImageUrl();
            if(!imageInfo.useNetworkImage() || url == null || url.length() == 0)
                return new ImageIcon();
            
            try {
                byte[] guid = GuiCoreMediator.getApplicationServices().getMyGUID();
                url = LimeWireUtils.addLWInfoToUrl(url, guid);
                return new ImageIcon(new URL(url));
            } catch(MalformedURLException murl) {
                return new ImageIcon();
            }
        }
        
        Future<Void> run() {
            return BackgroundExecutorService.submit(new Callable<Void>() {
                public Void call() {
                    ImageIcon img = image();
                    final boolean usingBackup;
                    if(img.getIconHeight() <= 0 || img.getIconHeight() <= 0) {
                        usingBackup = true;
                        img = GUIMediator.getThemeImage(backupImage);
                    } else {
                        usingBackup = false;
                    }
                    final ImageIcon finalImg = img;
                    GUIMediator.safeInvokeLater(new Runnable() {
                        public void run() {
                            runner.runWithImage(finalImg, usingBackup);
                        }
                    });
                    return null;
                }
            });
        }
    }
    
    private interface LateImageRunner {
        void runWithImage(ImageIcon img, boolean usingBackup);
    }
    
}
     
