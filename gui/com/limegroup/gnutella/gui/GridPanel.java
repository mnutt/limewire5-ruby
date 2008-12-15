package com.limegroup.gnutella.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * This class is simply a JPanel that uses a GridBagLayout.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public class GridPanel extends JPanel {
	
	/**
	 * Creates a default <tt>JPanel</tt> with a <tt>GridBagLayout</tt>.
	 */
	public GridPanel() {
		setLayout(new GridBagLayout());
	}

	public void addExpandComponent(JLabel label, Component component) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(6, 0, 0, 0);
		constraints.fill = GridBagConstraints.NONE;
		constraints.gridwidth = GridBagConstraints.RELATIVE;
		constraints.anchor = GridBagConstraints.EAST;
		constraints.weightx = 0.0;
		add(label, constraints);
		
		constraints = new GridBagConstraints();
		constraints.insets = new Insets(6, 6, 0, 0);
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.weightx = 1.0;
		add(component, constraints);
	}

	public void addExpandLabel(JLabel label, Component component) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(6, 0, 0, 0);
		constraints.fill = GridBagConstraints.NONE;
		constraints.gridwidth = GridBagConstraints.RELATIVE;
		constraints.anchor = GridBagConstraints.EAST;
		constraints.weightx = 1.0;
		add(label, constraints);
		
		constraints = new GridBagConstraints();
		constraints.insets = new Insets(6, 6, 0, 0);
		constraints.fill = GridBagConstraints.NONE;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.weightx = 0.0;
		add(component, constraints);
	}

	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}
	
	@Override
	public Dimension getMaximumSize() {
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}
	
}
