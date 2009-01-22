package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;

/**
 * Table format for the Program Table for All Friends table
 */
public class AllFriendProgramTableFormat <T extends RemoteFileItem> extends RemoteProgramTableFormat<T> {
    static final int FROM_INDEX = 6;
    
    public AllFriendProgramTableFormat() {
        super(new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "ALL_LIBRARY_PROGRAM_NAME", I18n.tr("Name"), 400, true, true),     
                new ColumnStateInfo(SIZE_INDEX, "ALL_LIBRARY_PROGRAM_SIZE", I18n.tr("Size"), 60, true, true), 
                new ColumnStateInfo(EXTENSION_INDEX, "ALL_LIBRARY_PROGRAM_EXTENSION", I18n.tr("Extension"), 60, true, true), 
                new ColumnStateInfo(PLATFORM_INDEX, "ALL_LIBRARY_PLATFORM_PLATFORM", I18n.tr("Platform"), 120, false, true), 
                new ColumnStateInfo(COMPANY_INDEX, "ALL_LIBRARY_PROGRAM_COMPANY", I18n.tr("Company"), 120, true, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "ALL_LIBRARY_PROGRAM_DESCRIPTION", I18n.tr("Description"), 120, false, true),
                new ColumnStateInfo(FROM_INDEX, "ALL_LIBRARY_OTHER_FROM", I18n.tr("From"), 80, true, true)
        });
    }
    
    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
            case FROM_INDEX: return baseObject;
        }
        return super.getColumnValue(baseObject, column);
    }
}
