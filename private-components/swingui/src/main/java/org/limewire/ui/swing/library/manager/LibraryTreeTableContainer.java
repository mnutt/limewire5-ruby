package org.limewire.ui.swing.library.manager;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.library.LibraryData;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

/**
 * A container for {@link LibraryManagerTreeTable} that appends text at the
 * bottom and shows a border surrounding all of it.
 */
public class LibraryTreeTableContainer extends JXPanel {
    
    private final LibraryManagerTreeTable treeTable;
    private final JScrollPane scrollPane;
    private final JLabel textLabel;
    
    public LibraryTreeTableContainer(IconManager iconManager, LibraryData libraryData, ExcludedFolderCollectionManager excludedFolders) {
        this(new LibraryManagerTreeTable(iconManager, libraryData, excludedFolders));
    }
    
    public LibraryTreeTableContainer(LibraryManagerTreeTable treeTable) {
        this.treeTable = treeTable;
        this.scrollPane = new JScrollPane(treeTable);
        this.textLabel = new JLabel(I18n.tr("*Adding these folders will not automatically share your files"));
        
        setOpaque(true);
        setBackground(treeTable.getTableColors().getEvenHighlighter().getBackground());
        textLabel.setOpaque(false);
        
        setLayout(new MigLayout("fill, gap 0, insets 0"));
        add(scrollPane, "hmin 0, grow, wrap");
        add(textLabel, "gapleft 2, gaptop 2");
        
        textLabel.setForeground(Color.GRAY);
        
        setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
    }
    
    public LibraryManagerTreeTable getTable() {
        return treeTable;
    }
    
    public void setAdditionalText(String text) {
        textLabel.setText(text);
    }

}
