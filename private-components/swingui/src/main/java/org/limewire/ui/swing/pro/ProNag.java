package org.limewire.ui.swing.pro;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.Rectangle;
import java.net.URL;

import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.html.HTML;

import org.jdesktop.swingx.JXPanel;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.Application;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.ui.swing.components.HTMLPane;
import org.limewire.ui.swing.components.Resizable;
import org.limewire.ui.swing.statusbar.ProStatusPanel;
import org.limewire.ui.swing.statusbar.ProStatusPanel.InvisibilityCondition;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;

/** A nag to go to LimeWire PRO. */ 
class ProNag extends JXPanel implements Resizable {
    
    private final Application application;
    private final ProStatusPanel proStatusPanel;
    
    private final java.awt.Panel parent;
    private final HTMLPane editorPane;
    
    @Inject public ProNag(Application application, ProStatusPanel proStatusPanel, 
                          final GnutellaConnectionManager connectionManager) {
        super(new BorderLayout());
        
        this.application = application;
        this.proStatusPanel = proStatusPanel;
        
        this.parent = new Panel(new BorderLayout()); // heavyweight, to show over other things.
        this.editorPane = new HTMLPane();

        setOpaque(false);
        parent.setMinimumSize(new Dimension(350, 200));
        parent.setMaximumSize(new Dimension(350, 200));
        parent.setPreferredSize(new Dimension(350, 200));

        editorPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    // Look for the HREF attribute of the A tag...
                    Object a = e.getSourceElement().getAttributes().getAttribute(HTML.Tag.A);
                    Object href = "";
                    if(a instanceof AttributeSet) {
                        href = ((AttributeSet)a).getAttribute(HTML.Attribute.HREF);
                    }
                    if(href != null && href.equals("_hide_nag_")) {
                        ProNag.this.setVisible(false);
                    } else if(e.getURL() != null) {
                        String url = e.getURL().toExternalForm();
                        url += "&gs=" + connectionManager.getConnectionStrength().getStrengthId();
                        NativeLaunchUtils.openURL(url);
                    }
                }
            }            
        });   
        
        add(parent, BorderLayout.CENTER);
        
        JScrollPane scroller = new JScrollPane(editorPane, 
                                               JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                               JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        parent.add(scroller, BorderLayout.CENTER);
    }

    @Override
    public void resize() {
        Rectangle parentBounds = getParent().getBounds();
        Dimension childPreferredSize = parent.getPreferredSize();
        int w = childPreferredSize.width;
        int h = childPreferredSize.height; 
        setBounds(parentBounds.width / 2 - w / 2, parentBounds.height - h, w, h);
        
        notifyVisibilityChange();
    }
    
    @Override
    public void setVisible(boolean flag) {
        boolean notViz = !isVisible();
        super.setVisible(flag);
        if(notViz && isVisible()) {
            resize();
        } 
        else {
            notifyVisibilityChange();
        }
    }

    /**
     *  Add or remove the pro add shown condition for the pro add status panel.
     *    May cause the pro status add to change visibility.
     */
    private void notifyVisibilityChange() {
       if (isVisible()) {
            proStatusPanel.addCondition(InvisibilityCondition.PRO_ADD_SHOWN);
        } 
        else {
            proStatusPanel.removeCondition(InvisibilityCondition.PRO_ADD_SHOWN);    
        }
    }
    
    
    public ListeningFuture<Void> loadContents(boolean firstLaunch) {
        return editorPane.setPageAsynchronous(application.addClientInfoToUrl("http://client-data.limewire.com/client_startup/nag/?html32=true&fromFirstRun=" + firstLaunch), createDefaultPage(firstLaunch));
    }
    
    private String createDefaultPage(boolean firstLaunch) {
        
        URL closeImage = ProNag.class.getResource("/org/limewire/ui/swing/mainframe/resources/icons/static_pages/close.png");
        URL bgImage = ProNag.class.getResource("/org/limewire/ui/swing/mainframe/resources/icons/static_pages/bg.png");
        URL getImage = ProNag.class.getResource("/org/limewire/ui/swing/mainframe/resources/icons/static_pages/button_get_limewire_pro.png");
             
        
        String outgoing = application.addClientInfoToUrl("http://www.limewire.com/clientpro?offline=true&fromFirstRun=" + firstLaunch);
        outgoing = outgoing.replace("&", "&amp;"); // must HTML-encode, otherwise things get messed up
        return
           "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">"
         + "<html>"
         + "<body>"
         + "<center>"
         + "<table width=\"346\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" background=\""+ bgImage.toExternalForm() +"\">"
         + "<tr>"
         + "<td>"
         + "<table width=\"346\" height=\"58\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">"
         + "<tr>"
         + "<td align=\"right\" valign=\"top\" height=\"66\">"
         + "<a href=\"_hide_nag_\"><img src=\""+ closeImage.toExternalForm() +"\" alt=\"\" border=\"0\" /></a>"
         + "</td>"
         + "</tr>"
         + "</table>"
         + "<table width=\"346\" height=\"51\" border=\"0\" cellspacing=\"0\" cellpadding=\"25\">"
         + "<tr>"
         + "<td align=\"left\">"
         + "<a href=\"http://www.limewire.com/download/pro/?ref=lwn1&amp;nstime=1251309416&amp;rnv=z\"><img src=\""+getImage.toExternalForm()+"\" alt=\"\" border=\"0\" /></a>"
         + "</td>"
         + "</tr>"
         + "</table>"
         + "<table width=\"346\" height=\"32\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">"
         + "<tr>"
         + "<td height=\"28\"></td>"
         + "</tr>"
         + "</table>"       
         + "</td>"
         + "</tr>"
         + "</table>"
         + "</center>"
         + "</body>"
         + "</html>"
         ;
    }

}
