package com.limegroup.gnutella.gui.search;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.limewire.util.NameValue;

import com.limegroup.gnutella.gui.actions.SearchWhatsNewMediaTypeAction;
import com.limegroup.gnutella.gui.actions.SearchXMLFieldAction;
import com.limegroup.gnutella.gui.tables.DefaultMouseListener;
import com.limegroup.gnutella.gui.tables.MouseObserver;

/**
 * If installed for a {@link FilterPanel} watches for popup events in all three
 * filter boxes and displays a popup menu offering more detailed searches
 * depending on the selected items in the filter boxes.
 */
class FilterPopupMenuHandler {

	private FilterPanel filterPanel;

	private FilterPopupMenuHandler(FilterPanel panel) {
		filterPanel = panel;
		FilterBox[] boxes = filterPanel.getBoxes();
		for (int i = 0; i < boxes.length; i++) {
			boxes[i].getList().addMouseListener
			(new DefaultMouseListener(new MouseHandler(boxes[i])));
		}
	}

	public static void install(FilterPanel panel) {
	    new FilterPopupMenuHandler(panel);
	}
    
	private void createAndShowPopup(FilterBox box, MouseEvent e) {
	
		JList list = box.getList();
		int index = list.locationToIndex(e.getPoint());
		if (index != -1) {
			list.setSelectedIndex(index);
		}
		
		LinkedHashMap<Selector, Object> map = new LinkedHashMap<Selector, Object>();
		
		addData(box, map);
		
		FilterBox[] boxes = filterPanel.getBoxes();
		for (int i = 0; i < boxes.length; i++) {
			addData(boxes[i], map);
		}

		Action[] actions = createActions(map);
		if (actions.length > 0) {
			JPopupMenu menu = new JPopupMenu();
			for (int i = 0; i < actions.length; i++) {
				menu.add(new JMenuItem(actions[i]));
			}
			menu.show(box.getList(), e.getX(), e.getY());
		}	
	}
	
	private void addData(FilterBox box, Map<? super Selector, Object> map) {
		if (!box.getSelector().isPropertySelector()) {
			Object o = box.getSelectedValue();
			if (o != null && !MetadataModel.isAll(o)) {
				map.put(box.getSelector(), o);
			}
		}
	}
	
	private Action[] createActions(Map<? extends Selector, ? extends Object> map) {

		if (map.isEmpty()) {
			return new Action[0];
		}
		
		List<Action> actions = new ArrayList<Action>();

        for(Map.Entry<? extends Selector, ? extends Object> entry : map.entrySet()) {
			Selector sel = entry.getKey();

			if (sel.isSchemaSelector()) {
				actions.add(new SearchWhatsNewMediaTypeAction
						((NamedMediaType)entry.getValue()));
			}
			else if (sel.isFieldSelector()) {
				actions.add(new SearchXMLFieldAction
						(new NameValue<Object>(sel.getTitle(), entry.getValue()),
								sel.getValue(), entry.getValue().toString(),
								NamedMediaType.getFromDescription(sel.getSchema())));
			}
		}
		
		return actions.toArray(new Action[0]);
	}

	private class MouseHandler implements MouseObserver {
		
		private FilterBox box;
		
		public MouseHandler(FilterBox box) {
			this.box = box;
		}
		
		public void handlePopupMenu(MouseEvent e) {
			createAndShowPopup(box, e);	
		}

		public void handleMouseDoubleClick(MouseEvent e) {
		}

		public void handleRightMouseClick(MouseEvent e) {
		}
	}
}
