package org.limewire.ui.swing.statusbar;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.library.FileProcessingEvent;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.painter.StatusBarPopupButtonPainter.DrawMode;
import org.limewire.ui.swing.painter.StatusBarPopupButtonPainter.PopupVisibilityChecker;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PainterUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
class FileProcessingPanel extends JXButton {
    
    @Resource private Color activeBackground = PainterUtils.TRASPARENT;
    @Resource private Color activeBorder = PainterUtils.TRASPARENT;
    
    @Resource private Font font;
    @Resource private Color foreground;
    
    private int total;
    private int finished;
    
    private FileProcessingPopupPanel popup;

    @Inject
    FileProcessingPanel(final Provider<FileProcessingPopupPanel> popupProvider, 
            ButtonDecorator decorator) {

        GuiUtils.assignResources(this);
        
        setFont(font);
        setForeground(foreground);
        
        decorator.decorateStatusPopupButton(this, new PopupVisibilityChecker() {
            @Override
            public boolean isPopupVisible() {
                return popup != null ? popup.isVisible() : false;
            }
        }, activeBackground, activeBorder, DrawMode.NORMAL);
        
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (popup == null) {
                    popup = popupProvider.get();
                    popup.addComponentListener(new ComponentAdapter() {
                        @Override
                        public void componentHidden(ComponentEvent e) {
                            closePopup();
                        }
                    });
                }
                else {
                    closePopup();
                }
            }
        });
        
        setVisible(false);
    }

    private void closePopup() {
        setVisible(total != 0);
        popup.dispose();
        popup = null;
    }
    
    @Inject
    void register(LibraryManager libraryManager) {
        libraryManager.getLibraryManagedList().addFileProcessingListener(new EventListener<FileProcessingEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(final FileProcessingEvent event) {
                switch (event.getType()) {
                case QUEUED:
                    total++;
                    update();
                    break;
                case FINISHED:
                    finished++;
                    if (finished == total) {
                        finished = 0;
                        total = 0;
                    }
                    update();
                    break;
                }
            }
        });
    }

    private void update() {
        if (total == 0) {
            if (popup != null) {
                setText(I18n.tr("Done"));
                popup.notifyDone();
            }
            else {
                setVisible(false);
            }
        } else {
            setText(I18n.tr("Adding {0} of {1}", finished + 1, total));
            setVisible(true);
        }
    }
}
