package org.limewire.ui.swing.tray;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.StringTokenizer;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.ListenerSupport;
import org.limewire.ui.swing.animate.AnimatorEvent;
import org.limewire.ui.swing.animate.FadeInOutAnimator;
import org.limewire.ui.swing.animate.MoveAnimator;
import org.limewire.ui.swing.painter.BorderPainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.StringUtils;
import org.limewire.util.SystemUtils;

/**
 * Notification window for system messages. This class handles drawing the
 * window and kickstarts the animation.
 */
class NotificationWindow extends JWindow implements ListenerSupport<WindowDisposedEvent> {
    private final Notification notification;

    private final EventListenerList<WindowDisposedEvent> eventListenerList = new EventListenerList<WindowDisposedEvent>();

    @Resource
    private Icon trayNotifyClose;

    @Resource
    private Icon trayNotifyCloseRollover;

    @Resource
    private Font titleFont;

    @Resource
    private Font messageFont;

    private MoveAnimator currentMoveAnimator;

    public NotificationWindow(Icon icon, final Notification notification) {
        GuiUtils.assignResources(this);
        this.notification = notification;

        FadeInOutAnimator fadeInOutAnimator = new FadeInOutAnimator(this, 500, 2500, 500);
        fadeInOutAnimator.addListener(new EventListener<AnimatorEvent<JWindow>>() {
            @Override
            public void handleEvent(AnimatorEvent event) {
                if (event.getType() == AnimatorEvent.Type.STOPPED) {
                    eventListenerList.broadcast(new WindowDisposedEvent(NotificationWindow.this));
                }
            }
        });

        JXPanel panel = new JXPanel(new MigLayout("fillx"));
        add(panel);

        panel.setBackgroundPainter(createPainter(panel));

        JCheckBox iconCheckBox = new JCheckBox(icon);
       

        final JCheckBox closeButton = new JCheckBox(trayNotifyClose);
        closeButton.setVisible(false);
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                eventListenerList.broadcast(new WindowDisposedEvent(NotificationWindow.this));
            }
        });

        closeButton.addMouseListener(new HoverButtonMouseListener(closeButton, trayNotifyClose,
                trayNotifyCloseRollover));

        panel.addMouseListener(new HoverPanelMouseListener(closeButton));
        closeButton.addMouseListener(new HoverPanelMouseListener(closeButton));
        iconCheckBox.addMouseListener(new HoverPanelMouseListener(closeButton));

        

        String title = notification.getTitle();
        String message = notification.getMessage();

        

        JEditorPane editor = new JEditorPane();
        editor.addMouseListener(new HoverPanelMouseListener(closeButton));
        HTMLEditorKit htmlEditorKit = new HTMLEditorKit();
        StyleSheet styleSheet = new StyleSheet();
        styleSheet.setBaseFontSize(messageFont.getSize());
        htmlEditorKit.setStyleSheet(styleSheet);
        editor.setMaximumSize(new Dimension(200, 50));
        editor.setEditorKit(htmlEditorKit);
        editor.setFont(messageFont);

        message = getLines(message, htmlEditorKit, messageFont, 180);
        String html = "<html><body><a href=\"action\"> " + message + "</a></body></html>";
        editor.setContentType("text/html");
        editor.setText(html);
        editor.setEditable(false);
        editor.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (notification.getActions() != null) {
                        for (Action action : notification.getActions()) {
                            action.actionPerformed(new ActionEvent(NotificationWindow.this,
                                    ActionEvent.ACTION_PERFORMED, "Message Clicked"));
                        }
                    }
                }
            }
        });
        
        panel.add(iconCheckBox, "");
        panel.add(closeButton, "alignx right, wrap");
        if (!StringUtils.isEmpty(title)) {
            JLabel titleLabel = new JLabel(getTruncatedMessage(title, htmlEditorKit, titleFont, 180));
            titleLabel.setFont(titleFont);
            panel.add(titleLabel, "spanx 2, wrap");
        }
        panel.add(editor, "spanx 2");
        setPreferredSize(new Dimension(200, 110));
        pack();

        fadeInOutAnimator.start();
    }

    /**
     * Returns the notification represented by this window.
     */
    public Notification getNotification() {
        return notification;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SystemUtils.setWindowTopMost(this);
    }

    @Override
    public void addListener(EventListener<WindowDisposedEvent> listener) {
        eventListenerList.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<WindowDisposedEvent> listener) {
        return eventListenerList.removeListener(listener);
    }

    /**
     * Creates a painter to render the rounded corners of this component.
     */
    private CompoundPainter<JXPanel> createPainter(JXPanel panel) {
        CompoundPainter<JXPanel> compoundPainter = new CompoundPainter<JXPanel>();
        RectanglePainter<JXPanel> painter = new RectanglePainter<JXPanel>();

        int arcWidth = 5;
        int arcHeight = 5;
        Color borderColour = Color.BLACK;
        Color bevelLeft = Color.GRAY;
        Color bevelTop1 = Color.GRAY;
        Color bevelTop2 = Color.GRAY;
        Color bevelRight = Color.GRAY;
        Color bevelBottom = Color.GRAY;

        painter.setRounded(true);
        painter.setFillPaint(Color.WHITE);
        painter.setRoundWidth(arcWidth);
        painter.setRoundHeight(arcHeight);
        painter.setInsets(new Insets(1, 1, 1, 1));
        painter.setBorderPaint(null);
        painter.setFillVertical(true);
        painter.setFillHorizontal(true);
        painter.setAntialiasing(true);
        painter.setCacheable(true);
        painter.setBorderWidth(1);

        compoundPainter.setPainters(painter, new BorderPainter<JXPanel>(arcWidth, arcHeight,
                borderColour, bevelLeft, bevelTop1, bevelTop2, bevelRight, bevelBottom,
                AccentType.SHADOW));
        compoundPainter.setCacheable(true);
        return compoundPainter;
    }

    /**
     * Calculates two lines for the notification window. The first line is found
     * by finding where the last word will wrap on that line. The second line is
     * the rest of the text, truncated to a maxWidth in pixels with '...'
     * appended
     */
    private String getLines(String message, HTMLEditorKit editorKit, Font font, int pixelWidth) {
        StringTokenizer stringTokenizer = new StringTokenizer(message, " \n\t\r");
        String line1 = "";
        String line2 = "";

        // find the first line.
        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();
            int pixels = getPixelWidth(line1 + token + " ", editorKit, font);
            if (pixels < (pixelWidth)) {
                line1 += token + " ";
            } else {
                line2 = token + " ";
                break;
            }
        }
        // build the second line.
        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();
            line2 += token + " ";
        }

        // truncate the second line.
        line2 = getTruncatedMessage(line2, editorKit, font, pixelWidth);

        return (line1.trim() + " " + line2.trim()).trim();
    }

    /**
     * Truncates the given message to a maxWidth in pixels.
     */
    private String getTruncatedMessage(String message, HTMLEditorKit editorKit, Font font,
            int maxWidth) {
        String ELIPSES = "...";
        while (getPixelWidth(message, editorKit, font) > (maxWidth)) {
            message = message.substring(0, message.length() - (ELIPSES.length() + 1)) + ELIPSES;
        }
        return message;
    }

    /**
     * Returns the width of the message in the given font and editor kit.
     */
    private int getPixelWidth(String text, HTMLEditorKit editorKit, Font font) {
        StyleSheet css = editorKit.getStyleSheet();
        FontMetrics fontMetrics = css.getFontMetrics(font);
        return fontMetrics.stringWidth(text);
    }

    /**
     * Sets the closeButton visible whenever this component is hovered over.
     */
    private final class HoverPanelMouseListener extends MouseAdapter {
        private final JCheckBox closeButton;

        private HoverPanelMouseListener(JCheckBox closeButton) {
            this.closeButton = closeButton;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            closeButton.setVisible(true);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            closeButton.setVisible(false);
        }
    }

    /**
     * Sets the rollover image for the close button when moused over.
     */
    private final class HoverButtonMouseListener extends MouseAdapter {
        private final JCheckBox closeButton;

        private final Icon trayNotifyClose;

        private final Icon trayNotifyCloseRollover;

        private HoverButtonMouseListener(JCheckBox closeButton, Icon trayNotifyClose,
                Icon trayNotifyCloseRollover) {
            this.closeButton = closeButton;
            this.trayNotifyClose = trayNotifyClose;
            this.trayNotifyCloseRollover = trayNotifyCloseRollover;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            closeButton.setIcon(trayNotifyCloseRollover);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            closeButton.setIcon(trayNotifyClose);
        }
    }

    /**
     * Moves the window from its current location to the new one. 
     */
    public synchronized void moveTo(Point newLocation) {
        if(this.currentMoveAnimator != null) {
            currentMoveAnimator.stop();
        }
        MoveAnimator moveAnimator = new MoveAnimator(this, 250, newLocation);
        moveAnimator.start();
        currentMoveAnimator = moveAnimator;
    }

}