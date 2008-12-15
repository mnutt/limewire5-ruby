package com.limegroup.gnutella.gui.options;

import javax.swing.tree.DefaultMutableTreeNode;

import com.limegroup.gnutella.gui.options.panes.AbstractPaneItem;

/**
 * This class acts as a proxy and as a "decorator" for an underlying instance 
 * of a <tt>MutableTreeNode</tt> implementation.<p>
 *
 * This class includes the most of the functionality of a 
 * <tt>DefaultMutableTreeNode</tt>, which it simply wraps, without the 
 * coupling that directly subclassing <tt>DefaultMutableTreeNode</tt>
 * would incur.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public class OptionsTreeNode extends DefaultMutableTreeNode {
	
	/**
	 * The key for uniquely identifying this node.
	 */
	private String _titleKey;

	/**
	 * The name of this node as it is displayed to the user.
	 */
	private String _displayName;
	
    private Class<? extends AbstractPaneItem>[] clazzes;
	
	/**
	 * This constructor sets the values for the name of the node to display 
	 * to the user as well as the constant key to use for uniquely 
	 * identifying this node.
	 *
	 * @param titleKey the key for the name of the node to display to the 
	 *                 user and the unique identifier key for this node
	 *
	 * @param displayName the name of the node as it is displayed to the
	 *                    user
	 */
	OptionsTreeNode(final String titleKey, final String displayName) {
		_titleKey = titleKey;
		_displayName = displayName;
	}

	/**
	 * Defines the class' representation as a <tt>String</tt> object, used 
	 * in determining how it is displayed in the <tt>JTree</tt>.
	 *
	 * @return the <tt>String</tt> identifier for the display of this class
	 */
	@Override
    public String toString() {
		return _displayName;
	}

	/**
	 * Returns the <tt>String</tt> denoting both the title of the node
	 * as well as the unique identifying <tt>String</tt> for the node.
	 */
	public String getTitleKey() {
		return _titleKey;
	}

    public void setClasses(Class<? extends AbstractPaneItem>[] clazzes) {
        this.clazzes = clazzes;
    }
    
    public Class<? extends AbstractPaneItem>[] getClasses() {
        return clazzes;
    }
    
}
