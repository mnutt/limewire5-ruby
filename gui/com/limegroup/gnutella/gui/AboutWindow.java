package com.limegroup.gnutella.gui;

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
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import org.limewire.util.OSUtils;
import org.limewire.util.VersionUtils;

import com.limegroup.gnutella.util.LimeWireUtils;


/**
 * Contains the <tt>JDialog</tt> instance that shows "about" information
 * for the application.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
final class AboutWindow {
	/**
	 * Constant handle to the <tt>JDialog</tt> that contains about
	 * information.
	 */
	private final JDialog DIALOG;

	/**
	 * Constant for the scolling pane of credits.
	 */
	private final ScrollingTextPane SCROLLING_PANE;

	/**
	 * Check box to specify whether to scroll or not.
	 */
	private final JCheckBox SCROLL_CHECK_BOX = 
		new JCheckBox(I18n.tr(
            "Automatically Scroll"));

	/**
	 * Constructs the elements of the about window.
	 */
	AboutWindow() {
	    DIALOG = new JDialog(GUIMediator.getAppFrame());
	    
        if (!OSUtils.isMacOSX())
            DIALOG.setModal(true);

		DIALOG.setSize(new Dimension(450, 400));            
		DIALOG.setResizable(false);
		DIALOG.setTitle(I18n.tr("About LimeWire"));
		DIALOG.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		DIALOG.addWindowListener(new WindowAdapter() {
		    public void windowClosed(WindowEvent we) {
		        SCROLLING_PANE.stopScroll();
		    }
		    public void windowClosing(WindowEvent we) {
		        SCROLLING_PANE.stopScroll();
		    }
		});		

        //  set up scrolling pane
        SCROLLING_PANE = createScrollingPane();
        SCROLLING_PANE.addHyperlinkListener(GUIUtils.getHyperlinkListener());

        //  set up limewire version label
        JLabel client = new JLabel(I18n.tr("LimeWire") +
                " " + LimeWireUtils.getLimeWireVersion());
        client.setHorizontalAlignment(SwingConstants.CENTER);
        
        //  set up java version label
        JLabel java = new JLabel("Java " + VersionUtils.getJavaVersion());
        java.setHorizontalAlignment(SwingConstants.CENTER);
        
        //  set up limewire.com label
        JLabel url = new URLLabel("http://www.limewire.com");
        url.setHorizontalAlignment(SwingConstants.CENTER);

        //  set up scroll check box
		SCROLL_CHECK_BOX.setSelected(true);
		SCROLL_CHECK_BOX.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (SCROLL_CHECK_BOX.isSelected())
					SCROLLING_PANE.startScroll();
				else
					SCROLLING_PANE.stopScroll();
			}
		});

        //  set up close button
        JButton button = new JButton(I18n.tr("Close"));
        DIALOG.getRootPane().setDefaultButton(button);
        button.setToolTipText(I18n.tr("Close This Window"));
        button.addActionListener(GUIUtils.getDisposeAction());

        //  layout window
		JComponent pane = (JComponent)DIALOG.getContentPane();
		GUIUtils.addHideAction(pane);
		
		pane.setLayout(new GridBagLayout());
        pane.setBorder(BorderFactory.createEmptyBorder(GUIConstants.SEPARATOR,
                GUIConstants.SEPARATOR, GUIConstants.SEPARATOR, GUIConstants.SEPARATOR));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;
		gbc.insets = new Insets(0,0,0,0);
        gbc.gridwidth = 2;
		gbc.gridy = 0;
        
		LogoPanel logo = new LogoPanel();
		logo.setSearching(true);
		pane.add(logo, gbc);

        gbc.gridy = 1;
        pane.add(Box.createVerticalStrut(GUIConstants.SEPARATOR), gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 2;
        pane.add(client, gbc);

        gbc.gridy = 3;
		pane.add(java, gbc);
        
        gbc.gridy = 4;
		pane.add(url, gbc);
		
        gbc.gridy = 5;
        pane.add(Box.createVerticalStrut(GUIConstants.SEPARATOR), gbc);

		gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 6;
		pane.add(SCROLLING_PANE, gbc);

        gbc.gridy = 7;
		gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        pane.add(Box.createVerticalStrut(GUIConstants.SEPARATOR), gbc);
        
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 1;
        gbc.gridy = 8;
		pane.add(SCROLL_CHECK_BOX, gbc);

		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.anchor = GridBagConstraints.EAST;
		pane.add(button, gbc);
		
	}

	private ScrollingTextPane createScrollingPane() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");

        Color color = new JLabel().getForeground();
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        String hex = GUIUtils.toHex(r) + GUIUtils.toHex(g) + GUIUtils.toHex(b);
        sb.append("<body text='#" + hex + "'>");

        //  introduction
        sb.append(I18n.tr("Inspired by LimeWire\'s owner, Mark Gorton, the LimeWire project is a collaborative <a href=\"http://www.limewire.org/\">open source effort</a> involving programmers and researchers from all over the world. The collaborative nature of Gnutella is also reflected in the Gnutella Developers Forum (GDF), of which LimeWire is a participant. The members of the GDF work in the trenches every day to make Gnutella a truly innovative collection of protocols that is constantly improving -- a testament to the power of open protocols and open standards."));
        sb.append("<br><br>");
        
        //  developers
        sb.append(I18n.tr("LimeWire is also, of course, the result of the countless hours of work by LimeWire\'s developers:"));
        sb.append("<ul>\n" + 
                "  <li>Greg Bildson</li>\n" + 
                "  <li>Sam Berlin</li>\n" + 
                "  <li>Zlatin Balevsky</li>\n" + 
                "  <li>Felix Berger</li>\n" +
                "  <li>Mike Everett</li>\n" +
                "  <li>Kevin Faaborg</li>\n" +
                "  <li>Jay Jeyaratnam</li>\n" +               
                "  <li>Curtis Jones</li>\n" +
                "  <li>Tim Julien</li>\n" +
                "  <li>Akshay Kumar</li>\n" +
                "  <li>Jeff Palm</li>\n" + 
                "  <li>Mike Sorvillo</li>\n" +
                "  <li>Dan Sullivan</li>\n" +
                "</ul>");
        
        //  business developers
        sb.append(I18n.tr("Behind the scenes business strategy and day-to-day affairs are handled by LimeWire\'s business developers:"));
        sb.append("<ul>\n" +  
                "  <li>George Searle</li>\n" +
                "  <li>Katie Catillaz</li>\n" +
                "  <li>Brian Dick</li>\n" +
                "  <li>Nathan Lovejoy</li>\n" +
                "  <li>Jesse Rubenfeld</li>\n" +
                "  <li>Luck Dookchitra</li>\n" +
                "  <li>E.J. Wolborsky</li>\n" +
                "</ul>");
        
        //  web developers
        sb.append(I18n.tr("The LimeWire <a href=\"http://www.limewire.com/\">web site</a> and LimeWire graphic design are the hard work of LimeWire\'s web team:"));
        sb.append("<ul>\n" +
                "  <li>Anthony Roscoe</li>\n" +
                "  <li>Greg Maggioncalda</li>\n" +
                "</ul>");
        
        //  support staff
        sb.append(I18n.tr("LimeWire PRO questions are dutifully answered by LimeWire technical support:"));
        sb.append("<ul>\n" + 
                "  <li>Zenzele Bell</li>\n" +  
                "  <li>Kirk Kahn</li>\n" + 
                "  <li>Dan Angeloro</li>\n" +
                "  <li>Christine Cioffari</li>\n" +
                "  <li>Sam Dingman</li>\n" +
                "</ul>");
        
        //  previous developers
        sb.append(I18n.tr("In addition, the following individuals have worked on the LimeWire team in the past but have since moved on to other projects:"));
        sb.append("<ul>\n" +  
                "  <li>Aubrey Arago</li>\n" +
                "  <li>Anthony Bow</li>\n" +
                "  <li>Susheel Daswani</li>\n" +
                "  <li>Adam Fisk</li>\n" +
                "  <li>Meghan Formel</li>\n" +
                "  <li>Tarun Kapoor</li>\n" +
                "  <li>Roger Kapsi</li>\n" +
                "  <li>Mark Kornfilt</li>\n" +
                "  <li>Angel Leon</li>\n" +
                "  <li>Karl Magdsick</li>\n" +
                "  <li>Yusuke Naito</li>\n" +
                "  <li>Dave Nicponski</li\n" +
                "  <li>Christine Nicponski</li>\n" +
                "  <li>Tim Olsen</li>\n" +  
                "  <li>Steffen Pingel</li>\n" +
                "  <li>Christopher Rohrs</li>\n" +
                "  <li>Justin Schmidt</li>\n" +
                "  <li>Arthur Shim</li>\n" + 
                "  <li>Anurag Singla</li>\n" +
                "  <li>Francesca Slade</li>\n" +
                "  <li>Robert Soule</li>\n" +
                "  <li>Rachel Sterne</li>\n" +
                "  <li>Sumeet Thadani</li>\n" +
                "  <li>Michael Tiraborrelli</li>\n" +
                "  <li>Ron Vogl</li>\n" +
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
        sb.append(I18n.tr("Finally, LimeWire would like to extend its sincere thanks to these developers, users, and all others who have contributed their ideas to the project. Without LimeWire users, the P2P network would not exist."));
        
        // bt notice
        sb.append("<small>");
        sb.append("<br><br>");
        sb.append(I18n.tr("BitTorrent, the BitTorrent Logo, and Torrent are trademarks of BitTorrent, Inc."));
        sb.append("</small>");
        
        sb.append("</body></html>");
        
        return new ScrollingTextPane(sb.toString());
    }

    /**
	 * Displays the "About" dialog window to the user.
	 */
	void showDialog() {
		GUIUtils.centerOnScreen(DIALOG);

		if (SCROLL_CHECK_BOX.isSelected()) {
			ActionListener startTimerListener = new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
				    //need to check isSelected() again,
				    //it might have changed in the past 10 seconds.
				    if (SCROLL_CHECK_BOX.isSelected()) {
				        //activate scroll timer
					    SCROLLING_PANE.startScroll();
					}
				}
			};
			
			Timer startTimer = new Timer(10000, startTimerListener);
			startTimer.setRepeats(false);			
			startTimer.start();
		}
		DIALOG.setVisible(true);
	}
}
