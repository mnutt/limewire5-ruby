package com.limegroup.gnutella.gui.logging;

import javax.swing.JPopupMenu;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.PaddedPanel;
import com.limegroup.gnutella.gui.dnd.DNDUtils;
import com.limegroup.gnutella.gui.tables.AbstractTableMediator;
import com.limegroup.gnutella.gui.tables.LimeJTable;
import com.limegroup.gnutella.gui.themes.ThemeMediator;

/**
 * This class acts as a mediator between all of the components of the
 * connection window.
 */
public final class LoggingMediator extends AbstractTableMediator<LoggingModel, LoggingDataLine, LogEvent> {

    private static final int MAXIMUM_EVENT_SIZE = 250;
    
    /** Instance of singleton access */
    private static final LoggingMediator INSTANCE = new LoggingMediator();
    public static LoggingMediator instance() { return INSTANCE; }
    

    /** Constructor -- private for Singleton access */
    private LoggingMediator() {
        super("LOGGING_TABLE");
        GUIMediator.addRefreshListener(this);
        ThemeMediator.addThemeObserver(this);
        doRefresh();
    }

	@Override
    protected void setupConstants() {
        MAIN_PANEL =
            new PaddedPanel(I18n.tr("Logging"));
		DATA_MODEL = new LoggingModel();
		TABLE = new LimeJTable(DATA_MODEL);
    }
	
	@Override
	protected void setupDragAndDrop() {
		TABLE.setTransferHandler(DNDUtils.DEFAULT_TRANSFER_HANDLER);
	}

    /** Overriden to remove the oldest entry if we're over the maximum limit. */
    @Override
    public void add(LogEvent o) {
        if(getSize() >= MAXIMUM_EVENT_SIZE)
            DATA_MODEL.removeOldestTime();
        super.add(o);
    }

    /** Update the splash screen */
	@Override
    protected void updateSplashScreen() {
		GUIMediator.setSplashScreenString(
                I18n.tr("Loading Logging Window..."));
    }

    @Override
    protected JPopupMenu createPopupMenu() {
        return null;
    }


    public void handleActionKey() {
    }


    public void handleNoSelection() {
    }


    public void handleSelection(int row) {
    }
    
}
