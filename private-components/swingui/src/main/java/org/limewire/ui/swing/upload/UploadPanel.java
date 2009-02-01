package org.limewire.ui.swing.upload;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadListManager;
import org.limewire.ui.swing.action.BackAction;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.components.decorators.ProgressBarDecorator;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.painter.TextShadowPainter;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.upload.table.UploadTable;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UploadPanel extends JXPanel{
    
    public static final String NAME = "UploadPanel";
    private JXButton clearAllButton;
    private HeaderBar header;
    private HeaderBarDecorator headerBarDecorator;
    
    private final Action clearAction = new AbstractAction(I18n.tr("Clear finished")) {
        @Override
        public void actionPerformed(ActionEvent e) {
           clearFinished();
        }
    };
    
    private ButtonDecorator buttonDecorator;
    private UploadListManager listManager;
    
    @Inject
    public UploadPanel(UploadListManager listManager, HeaderBarDecorator headerBarFactory,
            ButtonDecorator buttonDecorator, CategoryIconManager categoryIconManager, ProgressBarDecorator progressBarFactory, 
            PropertiesFactory<UploadItem> propertiesFactory, LibraryNavigator libraryNavigator,
            BackAction backAction, LibraryManager libraryManager){
        super(new BorderLayout());
        
        this.listManager = listManager;
        this.buttonDecorator = buttonDecorator;
        this.headerBarDecorator = headerBarFactory;

        UploadTable table = new UploadTable(listManager, categoryIconManager, progressBarFactory, propertiesFactory, libraryNavigator, libraryManager);
        table.setTableHeader(null);
        initHeader(backAction);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void clearFinished() {
        listManager.clearFinished();
    }

    private void initHeader(Action backAction) {
        JPanel headerTitlePanel = new JPanel(new MigLayout("insets 0, gap 0, fill, aligny center"));
        headerTitlePanel.setOpaque(false);        
        JXLabel titleTextLabel = new JXLabel(I18n.tr("Uploads"));
        titleTextLabel.setForegroundPainter(new TextShadowPainter());
        IconButton backButton = new IconButton(backAction);
        backButton.setRolloverEnabled(true);        
        headerTitlePanel.add(backButton, "gapafter 6, gapbottom 1");
        headerTitlePanel.add(titleTextLabel, "gapbottom 2");        
        
        header = new HeaderBar(headerTitlePanel);
        header.linkTextComponent(titleTextLabel);
        headerBarDecorator.decorateBasic(header);
        
        clearAllButton = new JXButton(clearAction);  
        buttonDecorator.decorateDarkFullButton(clearAllButton);     
 
        header.setLayout(new MigLayout("insets 0, fillx, filly","push[][]"));
        header.add(clearAllButton, "gapafter 10");
    }

}
