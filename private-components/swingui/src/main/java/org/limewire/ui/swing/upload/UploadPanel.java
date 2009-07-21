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
import org.limewire.core.api.upload.UploadListManager;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.painter.TextShadowPainter;
import org.limewire.ui.swing.upload.table.UploadTable;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class UploadPanel extends JXPanel{

    private JXButton clearAllButton;
    private HeaderBar header;
    private HeaderBarDecorator headerBarDecorator;
    
    private final Action clearAction = new AbstractAction(I18n.tr("Clear Finished")) {
        @Override
        public void actionPerformed(ActionEvent e) {
           clearFinished();
        }
    };
    
    private final ButtonDecorator buttonDecorator;
    private final UploadListManager listManager;
    
    @Inject
    public UploadPanel(UploadTable uploadTable, HeaderBarDecorator headerBarFactory,
            ButtonDecorator buttonDecorator, UploadListManager uploadListManager){
        super(new BorderLayout());
        
        this.listManager = uploadListManager;
        this.buttonDecorator = buttonDecorator;
        this.headerBarDecorator = headerBarFactory;

        uploadTable.setTableHeader(null);
        initHeader();
        
        JScrollPane scrollPane = new JScrollPane(uploadTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * Start the (polling) upload monitor.  
     * <p>
     * Note: this only make sense if this panel is created on demand.
     */
    @Inject
    public void register(ServiceRegistry serviceRegister) {
        serviceRegister.start(listManager);
    }
    
    private void clearFinished() {
        listManager.clearFinished();
    }

    private void initHeader() {
        JPanel headerTitlePanel = new JPanel(new MigLayout("insets 0, gap 0, fill, aligny center"));
        headerTitlePanel.setOpaque(false);        
        JXLabel titleTextLabel = new JXLabel(I18n.tr("Uploads"));
        titleTextLabel.setForegroundPainter(new TextShadowPainter());
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
