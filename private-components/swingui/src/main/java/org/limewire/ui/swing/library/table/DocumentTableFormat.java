package org.limewire.ui.swing.library.table;

import java.util.Comparator;
import java.util.Date;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

/**
 * Table format for the Document Table when it is in My Library
 */
public class DocumentTableFormat<T extends LocalFileItem> extends AbstractMyLibraryFormat<T> {
    public static enum Columns {
        NAME(I18n.tr("Filename"), true, true, 160),
        TYPE(I18n.tr("Type"), true, true, 80),
        CREATED(I18n.tr("Date Created"), true, true, 100),
        SIZE(I18n.tr("Size"), false, true, 60),
        AUTHOR(I18n.tr("Author"), false, true, 60),
        DESCRIPTION(I18n.tr("Description"), false, true, 100),
        ACTION(I18n.tr("Sharing"), true, false, 60);
        
        private final String columnName;
        private final boolean isShown;
        private final boolean isHideable;
        private final int initialWidth;
        
        Columns(String name, boolean isShown, boolean isHideable, int initialWidth) {
            this.columnName = name;
            this.isShown = isShown;
            this.isHideable = isHideable;
            this.initialWidth = initialWidth;
        }
        
        public String getColumnName() { return columnName; }
        public boolean isShown() { return isShown; }        
        public boolean isHideable() { return isHideable; }
        public int getInitialWidth() { return initialWidth; }
    }

	/** Icon manager used to find native file type information. */
	private IconManager iconManager;
	
    /**
     * Constructs a DocumentTableFormat with the specified icon manager.
     */
    public DocumentTableFormat() {
    }
	
    /**
     * Constructs a DocumentTableFormat with the specified icon manager.
     */
	public DocumentTableFormat(IconManager iconManager) {
	    this.iconManager = iconManager;
	}
	
    @Override
    public int getColumnCount() {
        return Columns.values().length;
    }

    @Override
    public String getColumnName(int column) {
        return Columns.values()[column].getColumnName();
    }

    @Override
    public Object getColumnValue(T baseObject, int column) {
        Columns other = Columns.values()[column];
        switch(other) {
         case AUTHOR: return baseObject.getProperty(FilePropertyKey.AUTHOR);
         case CREATED:
             // Return creation time if valid.
             long creationTime = baseObject.getCreationTime();
             return (creationTime >= 0) ? new Date(creationTime) : null;
         case DESCRIPTION: return "";
         case NAME: return baseObject;
         case ACTION: return baseObject;
         case SIZE: return baseObject.getSize();
         case TYPE:
             // Use icon manager to return MIME description.
             return (iconManager != null) ?
                 iconManager.getMIMEDescription(baseObject) : 
                 baseObject.getProperty(FilePropertyKey.TOPIC);
         }
         throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public int getActionColumn() {
        return Columns.ACTION.ordinal();
    }
    
    @Override
    public boolean isColumnHiddenAtStartup(int column) {
        return Columns.values()[column].isShown();
    }
    
    @Override
    public boolean isColumnHideable(int column) {
        return Columns.values()[column].isHideable();
    }

    @Override
    public int getInitialWidth(int column) {
        return Columns.values()[column].getInitialWidth();
    }

    @Override
    public boolean isEditable(T baseObject, int column) {
        return Columns.ACTION.ordinal() == column;
    }

    @Override
    public T setColumnValue(T baseObject, Object editedValue, int column) {
        return baseObject;
    }
    
    @Override
    public Class getColumnClass(int column) {
        Columns other = Columns.values()[column];
        switch(other) {
            case ACTION: return FileItem.class;
        }
        return super.getColumnClass(column);
    }

    @Override
    public Comparator getColumnComparator(int column) {
        Columns other = Columns.values()[column];
        switch(other) {
            case ACTION: return new ActionComparator();
        }
        return super.getColumnComparator(column);
    }
}
