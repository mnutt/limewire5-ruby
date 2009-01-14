package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import org.limewire.core.api.Application;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.ScrollingTextPane;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.util.OSUtils;
import org.limewire.util.VersionUtils;

/** The about window. */
class AboutWindow {
    
	private final JDialog dialog;
	private final ScrollingTextPane textPane;
	private final JCheckBox scrollBox = new JCheckBox(I18n.tr("Automatically Scroll"));

	/**
	 * Constructs the elements of the about window.
	 */
	AboutWindow(JFrame frame, Application application) {
	    dialog = new LimeJDialog(frame);
	    
        if (!OSUtils.isMacOSX()) {
            dialog.setModal(true);
        }

		dialog.setSize(new Dimension(450, 400));            
		dialog.setResizable(false);
		dialog.setTitle(I18n.tr("About LimeWire"));
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.addWindowListener(new WindowAdapter() {
		    public void windowClosed(WindowEvent we) {
		        textPane.stopScroll();
		    }
		    public void windowClosing(WindowEvent we) {
		        textPane.stopScroll();
		    }
		});		

        //  set up scrolling pane
        textPane = createScrollingPane();
        textPane.addHyperlinkListener(GuiUtils.getHyperlinkListener());

        //  set up limewire version label
        JLabel client = new JLabel(I18n.tr("LimeWire") +
                " " + application.getVersion());
        client.setHorizontalAlignment(SwingConstants.CENTER);
        
        //  set up java version label
        JLabel java = new JLabel("Java " + VersionUtils.getJavaVersion());
        java.setHorizontalAlignment(SwingConstants.CENTER);
        
        //  set up limewire.com label
        HyperlinkButton url = new HyperlinkButton("http://www.limewire.com");
        url.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NativeLaunchUtils.openURL("http://www.limewire.com");
            }
        });
        url.setHorizontalAlignment(SwingConstants.CENTER);

        //  set up scroll check box
		scrollBox.setSelected(true);
		scrollBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (scrollBox.isSelected())
					textPane.startScroll();
				else
					textPane.stopScroll();
			}
		});

        //  set up close button
        JButton button = new JButton(I18n.tr("Close"));
        dialog.getRootPane().setDefaultButton(button);
        button.setToolTipText(I18n.tr("Close This Window"));
        button.addActionListener(GuiUtils.getDisposeAction());

        //  layout window
		JComponent pane = (JComponent)dialog.getContentPane();
		GuiUtils.addHideAction(pane);
		
		pane.setLayout(new GridBagLayout());
        pane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;
		gbc.insets = new Insets(0,0,0,0);
        gbc.gridwidth = 2;
		gbc.gridy = 0;
        
//		LogoPanel logo = new LogoPanel();
//		logo.setSearching(true);
//		pane.add(logo, gbc);

        gbc.gridy = 1;
        pane.add(Box.createVerticalStrut(4), gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 2;
        pane.add(client, gbc);

        gbc.gridy = 3;
		pane.add(java, gbc);
        
        gbc.gridy = 4;
		pane.add(url, gbc);
		
        gbc.gridy = 5;
        pane.add(Box.createVerticalStrut(4), gbc);

		gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 6;
		pane.add(textPane, gbc);

        gbc.gridy = 7;
		gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        pane.add(Box.createVerticalStrut(4), gbc);
        
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 1;
        gbc.gridy = 8;
		pane.add(scrollBox, gbc);

		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.anchor = GridBagConstraints.EAST;
		pane.add(button, gbc);
		
	}

	private ScrollingTextPane createScrollingPane() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");

        Color color = new JLabel().getForeground();
        String hex = GuiUtils.colorToHex(color);
        sb.append("<body text='#" + hex + "'>");

        //  introduction
        sb.append(I18n.tr("Inspired by LimeWire\'s owner, Mark Gorton, the LimeWire project is a " + 
                "collaborative <a href=\"http://www.limewire.org/\">open source effort</a> involving " +
                "programmers and researchers from all over the world.  " +
                "LimeWire is also, of course, the result of the countless hours of work by LimeWire\'s developers:"));
        sb.append("<ul>\n" + 
                "  <li>Mario Aquino</li>" + 
                "  <li>Felix Berger</li>" +
                "  <li>Sam Berlin</li>" + 
                "  <li>David Chen</li>" +
                "  <li>Mike Everett</li>" +              
                "  <li>Tim Julien</li>" +
                "  <li>Jorge Mancheno</li>" +                
                "  <li>Jason Pelzer</li>" +
                "  <li>Michael Rogers</li>" +
                "  <li>Anthony Roscoe</li>" +
                "  <li>Mike Sorvillo</li>" +
                "  <li>Michael Tiraborrelli</li>" +
                "  <li>Matt Turkel</li>" +
                "  <li>Peter Vertenten</li>" +
                "  <li>Peng Wang</li>" +
                "  <li>Ernie Yu</li>" +
                "</ul>");
        //behind the scenes technical
        sb.append(I18n.tr("Providing key support:"));
        sb.append("<ul>\n" +  
                "  <li>Ari Amanatidis</li>" +
                "  <li>Bobby Fonacier</li>" +                
                "  <li>Catherine Herdlick</li>" +
                "  <li>Akshay Kumar</li>" +
                "  <li>Greg Maggioncalda</li>" +
                "  <li>Dan Sullivan</li>" +
                "</ul>");        
        
        
        //  business developers
        sb.append(I18n.tr("Behind the scenes business strategy and day-to-day affairs are handled by LimeWire\'s business developers:"));
        sb.append("<ul>\n" +  
                "  <li>Kevin Bradshaw</li>" +                
                "  <li>Brian Dick</li>" +
                "  <li>Nathan Lovejoy</li>" +
                "  <li>George Searle</li>" +
                "</ul>");        
        
        //  previous developers
        sb.append(I18n.tr("In addition, the following individuals have worked on the LimeWire team in the past but have since moved on to other projects:"));
        sb.append("<ul>\n" +  
                "  <li>Aubrey Arago</li>\n" +
                "  <li>Zlatin Balevsky</li>\n" +
                "  <li>Zenzele Bell</li>\n" +                  
                "  <li>Anthony Bow</li>\n" +
                "  <li>Katie Catillaz</li>\n" +                
                "  <li>Susheel Daswani</li>\n" +
                "  <li>Luck Dookchitra</li>\n" +                
                "  <li>Kevin Faaborg</li>" +                
                "  <li>Adam Fisk</li>\n" +
                "  <li>Meghan Formel</li>\n" +
                "  <li>Jay Jeyaratnam</li>\n" +
                "  <li>Curtis Jones</li>\n" +
                "  <li>Tarun Kapoor</li>\n" +
                "  <li>Roger Kapsi</li>\n" +
                "  <li>Mark Kornfilt</li>\n" +
                "  <li>Angel Leon</li>\n" +
                "  <li>Karl Magdsick</li>\n" +
                "  <li>Yusuke Naito</li>\n" +
                "  <li>Dave Nicponski</li\n" +
                "  <li>Christine Nicponski</li>\n" +
                "  <li>Tim Olsen</li>\n" + 
                "  <li>Jeff Palm</li>\n" +                 
                "  <li>Steffen Pingel</li>\n" +
                "  <li>Christopher Rohrs</li>\n" +
                "  <li>Justin Schmidt</li>\n" +
                "  <li>Arthur Shim</li>\n" + 
                "  <li>Anurag Singla</li>\n" +
                "  <li>Francesca Slade</li>\n" +
                "  <li>Robert Soule</li>\n" +
                "  <li>Rachel Sterne</li>\n" +
                "  <li>Sumeet Thadani</li>\n" +
                "  <li>Ron Vogl</li>\n" +
                "  <li>E.J. Wolborsky</li>\n" +                
                "</ul>");

        //  open source contributors
        sb.append(I18n.tr("LimeWire open source contributors have provided significant code and many bug fixes, ideas, research, etc. to the project as well. Those listed below have either written code that is distributed with every version of LimeWire, have identified serious bugs in the code, or both:"));
        sb.append("<ul>\n" + 
                "  <li>Richie Bielak</li>\n" +
                "  <li>Johanenes Blume</li>\n" +
                "  <li>Jerry Charumilind</li>\n" +
                "  <li>Marvin Chase</li>\n" +
                "  <li>Robert Collins</li>\n" +
                "  <li>Kenneth Corbin</li>\n" +
                "  <li>Kyle Furlong</li>\n" +
                "  <li>David Graff</li>\n" +
                "  <li>Andy Hedges</li>\n" +
                "  <li>Michael Hirsch</li>\n" +
                "  <li>Panayiotis Karabassis</li>\n" +
                "  <li>Jens-Uwe Mager</li>\n" +
                "  <li>Miguel Munoz</li>\n" +
                "  <li>Gordon Mohr</li>\n" +
                "  <li>Chance Moore</li>\n" +
                "  <li>Marcin Koraszewski</li>\n" +
                "  <li>Rick T. Piazza</li>\n" +
                "  <li>Eugene Romanenko</li>\n" +
                "  <li>Gregorio Roper</li>\n" +
                "  <li>William Rucklidge</li>\n" +
                "  <li>Claudio Santini</li>\n" + 
                "  <li>Phil Schalm</li>\n" + 
                "  <li>Eric Seidel</li>\n" +
                "  <li>Philippe Verdy</li>\n" +
                "  <li>Cameron Walsh</li>\n" +
                "  <li>Stephan Weber</li>\n" +
                "  <li>Jason Winzenried</li>\n" +
                "  <li>'Tobias'</li>\n" +
                "  <li>'deacon72'</li>\n" +
                "  <li>'MaTZ'</li>\n" +
                "  <li>'RickH'</li>\n" +
                "  <li>'PNomolos'</li>\n" +
                "  <li>'ultracross'</li>\n" +
                "</ul>");
         
        //  internationalization contributors
        sb.append(I18n.tr("LimeWire would also like to thank the many contributors to the internationalization project, both for the application itself and for the LimeWire web site."));
        sb.append("<br><br>");
        
        //  community VIPs
        sb.append(I18n.tr("Several colleagues in the Gnutella community merit special thanks. These include:"));
        sb.append("<ul>\n" + 
                "  <li>Vincent Falco -- Free Peers, Inc.</li>\n" + 
                "  <li>Gordon Mohr -- Bitzi, Inc.</li>\n" + 
                "  <li>John Marshall -- Gnucleus</li>\n" +
                "  <li>Jason Thomas -- Swapper</li>\n" +
                "  <li>Brander Lien -- ToadNode</li>\n" +
                "  <li>Angelo Sotira -- www.gnutella.com</li>\n" +
                "  <li>Marc Molinaro -- www.gnutelliums.com</li>\n" +
                "  <li>Simon Bellwood -- www.gnutella.co.uk</li>\n" +
                "  <li>Serguei Osokine</li>\n" +
                "  <li>Justin Chapweske</li>\n" +
                "  <li>Mike Green</li>\n" +
                "  <li>Raphael Manfredi</li>\n" +
                "  <li>Tor Klingberg</li>\n" +
                "  <li>Mickael Prinkey</li>\n" +
                "  <li>Sean Ediger</li>\n" +
                "  <li>Kath Whittle</li>\n" +
                "</ul>");
        
        //  conclusion
        sb.append(I18n.tr("Finally, LimeWire would like to extend its sincere thanks to those developers, users, and all others who have contributed their ideas to the project. Without LimeWire users, the P2P Network would not exist."));
        
        // bt notice
        sb.append("<small>");
        sb.append("<br><br>");
        sb.append(I18n.tr("BitTorrent, the BitTorrent Logo, and Torrent are trademarks of BitTorrent, Inc. Gmail is a trademark of Google Inc. Jabber and LiveJournal are the registered trademarks of the XMPP Standards Foundation and LiveJournal, Inc., d/b/a LiveJournal.com, respectively. Neither Google Inc., the XMPP Standards Foundation or LiveJournal.com are sponsors or partners of Lime&nbsp;Wire&nbsp;LLC nor do they endorse Lime&nbsp;Wire&nbsp;LLC or the LimeWire software. Use of these trademarks is merely to refer to the technology or service of the respective owner and not to confuse Lime&nbsp;Wire as the source of the respective Gmail, Jabber and/or LiveJournal service or technology."));
        sb.append("</small>");
        
        sb.append("</body></html>");
        
        return new ScrollingTextPane(sb.toString());
    }

    /**
	 * Displays the "About" dialog window to the user.
	 */
	void showDialog() {
	    if (dialog.getParent().isVisible()) {
	        dialog.setLocationRelativeTo(dialog.getParent());
        } else { 
            dialog.setLocation(GuiUtils.getScreenCenterPoint(dialog));
        }

		if (scrollBox.isSelected()) {
			ActionListener startTimerListener = new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
				    //need to check isSelected() again,
				    //it might have changed in the past 10 seconds.
				    if (scrollBox.isSelected()) {
				        //activate scroll timer
					    textPane.startScroll();
					}
				}
			};
			
			Timer startTimer = new Timer(10000, startTimerListener);
			startTimer.setRepeats(false);			
			startTimer.start();
		}
		dialog.setVisible(true);
	}
}
