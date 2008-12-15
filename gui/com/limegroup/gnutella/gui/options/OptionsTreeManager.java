package com.limegroup.gnutella.gui.options;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Enumeration;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.limewire.util.I18NConvert;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.gui.trees.FilteredTreeModel;
import com.limegroup.gnutella.gui.trees.LimeTreeCellRenderer;

/**
 * Manages the <code>JTree</code> instance of the the options window.  This
 * class constructs the <tt>TreeModel</tt> and forwards many method calls
 * the the contained <tt>TreeModel</tt>.<p>
 *
 * In addition, this class controls the <tt>Component</tt> that contains
 * the <tt>JTree</tt> instance and provides access to that 
 * <tt>Component</tt>.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
final class OptionsTreeManager {
	
	/**
	 * Handle to the main <tt>JScrollPane</tt> instance for the main window
	 * that contains the <tt>JTree</tt>.
	 */
	private final JScrollPane SCROLL_PANE;
	
	/**
	 * Handle to the main <tt>JTree</tt> instance that displays the options.
	 */
	private final JTree TREE;

	/**
	 * Constant handle to the tree model.
	 */
	private final OptionsTreeModel TREE_MODEL = new OptionsTreeModel();
	
	private final FilteredTreeModel FILTERED_TREE_MODEL = new FilteredTreeModel(TREE_MODEL, true);
   
	/**
	 * The constructor constructs the <tt>JTree</tt>, the <tt>TreeModel</tt>,
	 * and the <tt>JScrollPane</tt>.
	 */
	OptionsTreeManager() {
		TREE = new JTree();
		TREE.setEditable(false);
		TREE.setShowsRootHandles(true);	
		TREE.setRootVisible(false);
		TREE.putClientProperty("JTree.lineStyle", "None");
		TREE.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);	
		TREE.addTreeSelectionListener(new OptionsTreeSelectionListener(TREE));
        TREE.setCellRenderer(new LimeTreeCellRenderer());
                
		TREE.setModel(FILTERED_TREE_MODEL);

		SCROLL_PANE = new JScrollPane(TREE);
		SCROLL_PANE.getViewport().setBackground(Color.white);
		SCROLL_PANE.setPreferredSize(new Dimension(125, 2000));
		SCROLL_PANE.setMinimumSize(new Dimension(125, 300));
	}

	/**
	 * Adds a new child node to one of the top-level parent nodes. 
	 * children.  Neither key can denote the root.<p>
	 * 
	 * This should only be called during tree construction.
	 * 
	 * @param parentKey the unique identifying key of the node to add as  
	 *                  well as the key for the locale-specific name for 
	 *                  the node as it appears to the user
	 *
	 * @param childKey the unique identifying key of the child node to add 
	 *                 as well as the key for the locale-specific name for 
	 *                 the node as it appears to the user
	 *
	 * @param displayName the name of the new node as it is displayed to 
	 *                    the user
	 * @param keywordText 
	 * @return 
	 */
	final OptionsTreeNode addNode(final String parentKey, final String childKey,
					   final String displayName, String keywordText) {
		OptionsTreeNode node = TREE_MODEL.addNode(parentKey, childKey, displayName);
		assert(node != null);
        
		if (keywordText != null) {
	        String[] keywords = StringUtils.split(I18NConvert.instance().getNorm(keywordText), " \".,\';:()[]"); 
	        for (String keyword : keywords) {
				FILTERED_TREE_MODEL.addSearchKey(node, keyword);
			}
		}
		
		return node;
	}

	/**
	 * Returns the main <code>Component</code> for this class.
	 *
	 * @return a <code>Component</code> instance that is the main component
	 *         for this class
	 */
	final Component getComponent() {
		return SCROLL_PANE;
	}
	
	/**
	 * Sets the selection of the tree to the node with the given key.
	 */
	void setSelection(final String key) {
		if (key == null)
			return;
		OptionsTreeNode root = (OptionsTreeNode)TREE_MODEL.getRoot();
		OptionsTreeNode node = null;
		for (Enumeration en = root.breadthFirstEnumeration(); en.hasMoreElements(); ) {
			node = (OptionsTreeNode)en.nextElement();
			if (key.equals(node.getTitleKey())) {
				//  set selection and return
				TreePath tp = new TreePath(node.getPath());
                TREE.expandPath(tp);
				TREE.scrollPathToVisible(tp);
				TREE.setSelectionPath(tp);
				return;
			}				
		}
	}
	
	void setDefaultSelection() {
		OptionsTreeNode root = (OptionsTreeNode)TREE_MODEL.getRoot();
		if (root == null)
			return;
		OptionsTreeNode node = null;
		Enumeration<?> en = root.children();
		if (!en.hasMoreElements())
			return;
		node = (OptionsTreeNode)en.nextElement();
		if (node == null)
			return;
		TreePath tp = new TreePath(node.getPath());
		TREE.scrollPathToVisible(tp);
		TREE.setSelectionPath(tp);
		TREE.collapsePath(tp);
	}
	
	void setFilterText(String text) {
		TreePath path = TREE.getSelectionPath();
        boolean collapsed = TREE.isCollapsed(path);
		
		FILTERED_TREE_MODEL.filterByText(text);
		if (text.length() > 0) {
			expandAllNodes();
		} else {
			collapseAllNodes();
		}
		
		if (path != null) {
			TREE.setSelectionPath(path);
            if (text.length() == 0) {
                // special casing to restore tree state if search field is
                // clicked for the first time
                if (collapsed) {
                    TREE.collapsePath(path);
                } else {
                    TREE.expandPath(path);
                }
            }
            TREE.scrollPathToVisible(path);
		}
	}
	
	private void expandAllNodes()  {
		for (int i = TREE_MODEL.getChildCount(TREE_MODEL.getRoot()) - 1; i >= 0; i--) {
			Object[] path  = new Object[] { TREE_MODEL.getRoot(), TREE_MODEL.getChild(TREE_MODEL.getRoot(), i) };
			TREE.expandPath(new TreePath(path));
		}
	}

	private void collapseAllNodes()  {
		for (int i = TREE_MODEL.getChildCount(TREE_MODEL.getRoot()) - 1; i >= 0; i--) {
			Object[] path  = new Object[] { TREE_MODEL.getRoot(), TREE_MODEL.getChild(TREE_MODEL.getRoot(), i) };
			TREE.collapsePath(new TreePath(path));
		}
	}

}
