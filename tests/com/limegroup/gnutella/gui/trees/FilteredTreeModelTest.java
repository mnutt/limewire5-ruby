package com.limegroup.gnutella.gui.trees;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import junit.framework.Test;

import com.limegroup.gnutella.gui.GUIBaseTestCase;

public class FilteredTreeModelTest extends GUIBaseTestCase {

	private DefaultMutableTreeNode root;
	private DefaultTreeModel model;
	private DefaultMutableTreeNode a;
	private DefaultMutableTreeNode a1;
	private DefaultMutableTreeNode a2;
	private DefaultMutableTreeNode b;
	private FilteredTreeModel filteredModel;

	public FilteredTreeModelTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		root = new DefaultMutableTreeNode("root");
		model = new DefaultTreeModel(root);		
		filteredModel = new FilteredTreeModel(model, true);

		a = addNode(root, "a", "foo");
		a1 = addNode(a, "a.1", "bar");
		a2 = addNode(a, "a.2");
		b = addNode(root, "b", "baz");
	}
	
    private DefaultMutableTreeNode addNode(DefaultMutableTreeNode parent,
			String... keywords) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(keywords[0]);
		parent.add(node);
		for (String keyword : keywords) {
			filteredModel.addSearchKey(node, keyword);
		}
		return node;
	}

	public static Test suite() { 
        return buildTestSuite(FilteredTreeModelTest.class); 
    } 
    
	public void testIsVisible() {
		assertTrue(filteredModel.isVisible(root));
		assertTrue(filteredModel.isVisible(a));
		assertTrue(filteredModel.isVisible(a1));
		assertTrue(filteredModel.isVisible(a2));
		assertTrue(filteredModel.isVisible(b));
		
		filteredModel.filterByText("a");
		assertTrue(filteredModel.isVisible(root));
		assertTrue(filteredModel.isVisible(a));
		assertTrue(filteredModel.isVisible(a1));
		assertTrue(filteredModel.isVisible(a2));
		assertFalse(filteredModel.isVisible(b));

		filteredModel.filterByText("A");
		assertTrue(filteredModel.isVisible(root));
		assertTrue(filteredModel.isVisible(a));
		assertTrue(filteredModel.isVisible(a1));
		assertTrue(filteredModel.isVisible(a2));
		assertFalse(filteredModel.isVisible(b));

		filteredModel.filterByText("b");
		assertTrue(filteredModel.isVisible(root));
		assertTrue(filteredModel.isVisible(a));
		assertTrue(filteredModel.isVisible(a1));
		assertFalse(filteredModel.isVisible(a2));
		assertTrue(filteredModel.isVisible(b));
		
		filteredModel.filterByText(null);
		assertTrue(filteredModel.isVisible(root));
		assertTrue(filteredModel.isVisible(a));
		assertTrue(filteredModel.isVisible(a1));
		assertTrue(filteredModel.isVisible(a2));
		assertTrue(filteredModel.isVisible(b));

		filteredModel.filterByText("z");
		assertFalse(filteredModel.isVisible(root));
		assertFalse(filteredModel.isVisible(a));
		assertFalse(filteredModel.isVisible(a1));
		assertFalse(filteredModel.isVisible(a2));
		assertFalse(filteredModel.isVisible(b));		
	}
	
	public void testChildren() {
		assertEquals(model.getChildCount(root), filteredModel.getChildCount(root));
		assertEquals(2, filteredModel.getChildCount(root));
		assertEquals(2, filteredModel.getChildCount(a));
		assertEquals(0, filteredModel.getIndexOfChild(root, a));
		assertEquals(1, filteredModel.getIndexOfChild(root, b));
		assertEquals(0, filteredModel.getIndexOfChild(a, a1));
		assertEquals(1, filteredModel.getIndexOfChild(a, a2));
		
		filteredModel.filterByText("a.2");
		assertEquals(1, filteredModel.getChildCount(root));
		assertEquals(a, filteredModel.getChild(root, 0));
		assertEquals(0, filteredModel.getIndexOfChild(root, a));
		assertEquals(-1, filteredModel.getIndexOfChild(root, b));
		assertEquals(-1, filteredModel.getIndexOfChild(a, a1));
		assertEquals(0, filteredModel.getIndexOfChild(a, a2));
		assertEquals(1, filteredModel.getChildCount(a));
		assertEquals(a2, filteredModel.getChild(a, 0));
	}
	
	public void testRoot() {
		assertEquals(root, filteredModel.getRoot());
	}

	public void testModel() {
		assertEquals(model, filteredModel.getModel());
		DefaultMutableTreeNode newRoot = new DefaultMutableTreeNode();
		DefaultTreeModel newModel = new DefaultTreeModel(newRoot);
		filteredModel.setModel(newModel);
		assertEquals(newRoot, filteredModel.getRoot());
		assertEquals(0, filteredModel.getChildCount(newRoot));
		assertTrue(filteredModel.isVisible(root));
		filteredModel.filterByText("a");
		assertFalse(filteredModel.isVisible(root));
	}

	public void testAddMultipleKeywords() {
	    assertTrue(filteredModel.isVisible(a));
	    DefaultMutableTreeNode node = addNode(root, "node", "123", "456");
        filteredModel.filterByText("123");
        assertTrue(filteredModel.isVisible(node));
        assertFalse(filteredModel.isVisible(a));	    
	}

	public void testAddUpperCase() {
	    assertTrue(filteredModel.isVisible(a));
	    DefaultMutableTreeNode node = addNode(root, "node", "ABC", "deFGhi");
	    filteredModel.filterByText("def");
        assertTrue(filteredModel.isVisible(node));
        assertFalse(filteredModel.isVisible(a));
	}

}
