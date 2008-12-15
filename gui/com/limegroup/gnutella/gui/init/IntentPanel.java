package com.limegroup.gnutella.gui.init;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;


import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.Line;
import com.limegroup.gnutella.gui.MultiLineLabel;
import com.limegroup.gnutella.gui.URLLabel;
import com.limegroup.gnutella.gui.search.DitherPanel;
import com.limegroup.gnutella.gui.search.Ditherer;
import com.limegroup.gnutella.util.LimeWireUtils;

public class IntentPanel extends JPanel {
    
    private final JRadioButton mightUseButton;
    private final JRadioButton willNotButton;

    public IntentPanel() {
        mightUseButton = new JRadioButton();
        willNotButton = new JRadioButton();
        ButtonGroup bg = new ButtonGroup();
        bg.add(mightUseButton);
        bg.add(willNotButton);
        
        setBackground(GUIUtils.hexToColor("F7F7F7"));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GUIUtils.hexToColor("C8C8C8"), 1),
                BorderFactory.createLineBorder(GUIUtils.hexToColor("FBFBFB"), 3)));
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
       
        JLabel almostDone = new JLabel(I18n.tr("You're almost done!"));
        JLabel stateIntent = new JLabel(I18n.tr("State your intent below to start using LimeWire " + LimeWireUtils.getLimeWireVersion()));
        Line line = new Line();
        MultiLineLabel description = new MultiLineLabel(I18n.tr("LimeWire Basic and LimeWire PRO are peer-to-peer programs for sharing authorized files only.  Installing and using either program does not consitute a license for obtaining or distributing unauthorized content."), 500);
        URLLabel findMore = new URLLabel("http://www.limewire.com/learnMore/intent", I18n.tr("Find out more..."));
        
        Ditherer ditherer = new Ditherer(GUIUtils.hexToColor("E2E2E2"), GUIUtils.hexToColor("ECECEC"), Ditherer.Y_AXIS, new Ditherer.PolygonShader(2f));
        DitherPanel willNot = new DitherPanel(ditherer);
        willNot.setLayout(new GridBagLayout());
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(10, 10, 10, 10);
        willNotButton.setText(I18n.tr("<html><div display=\"block\" color=\"#515151\" size=\"13\">I <b>will not</b> use LimeWire {0} for copyright infringement.</div></html>", LimeWireUtils.getLimeWireVersion()));
        willNotButton.setOpaque(false);
        willNotButton.setIconTextGap(10);
        willNot.add(willNotButton, gbc);
        willNot.setBorder(BorderFactory.createEtchedBorder(GUIUtils.hexToColor("C8C8C8"), GUIUtils.hexToColor("FBFBFB")));

        DitherPanel mightUse = new DitherPanel(ditherer);
        mightUse.setLayout(new GridBagLayout());
        mightUseButton.setText(I18n.tr("<html><div display=\"block\" color=\"#515151\" size=\"13\">I <b>might use</b> LimeWire {0} for copyright infringement.</div></html>", LimeWireUtils.getLimeWireVersion()));
        mightUseButton.setOpaque(false);
        mightUseButton.setIconTextGap(10);
        mightUse.add(mightUseButton, gbc);
        mightUse.setBorder(BorderFactory.createEtchedBorder(GUIUtils.hexToColor("C8C8C8"), GUIUtils.hexToColor("FBFBFB")));
        
        almostDone.setFont(almostDone.getFont().deriveFont(24f));
        almostDone.setForeground(GUIUtils.hexToColor("038640"));
        
        stateIntent.setFont(stateIntent.getFont().deriveFont(16f));
        stateIntent.setForeground(GUIUtils.hexToColor("333333"));
        
        description.setFont(description.getFont().deriveFont(14f));
        description.setForeground(GUIUtils.hexToColor("333333"));
        
        line.setColor(GUIUtils.hexToColor("C8C8C8"));
       
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridwidth = 3;        
        gbc.gridx = 1;
        
        add(almostDone, gbc);
        add(stateIntent, gbc);
        gbc.insets = new Insets(10, 0, 10, 0);
        add(line, gbc);
        gbc.insets = new Insets(0, 0, 5, 0);
        add(description, gbc);
        gbc.insets = new Insets(0, 0, 0, 0);
        add(findMore, gbc);
        
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 1;
        gbc.gridx = 2;
        gbc.insets = new Insets(20, 70, 0, 0);
        add(willNot, gbc); 

        gbc.insets = new Insets(13, 70, 0, 0);
       // add(mightUse, gbc);
    }
    
    boolean hasSelection() {
        return willNotButton.isSelected() || mightUseButton.isSelected();
    }
    
    boolean isWillNot() {
        return willNotButton.isSelected();
    }
    
    void addButtonListener(ActionListener changeListener) {
        willNotButton.addActionListener(changeListener);
        mightUseButton.addActionListener(changeListener);
    }
}
