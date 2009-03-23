package org.limewire.ui.swing.images;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.library.SelectAllable;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.GlazedListsSwingFactory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.components.Disposable;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.swing.EventSelectionModel;

/**
 *	Draws a list of images. Images are displayed in a horizontal left
 *  to right space before wrapping to a new line. Spaces between the
 *  images are injected with the inset values list below.
 */
public class ImageList extends JXList implements Disposable, SelectAllable<LocalFileItem> {

    @Resource
    private Color backgroundListcolor;
    @Resource
    private int imageBoxWidth;
    @Resource
    private int imageBoxHeight;
    @Resource
    private int insetTop;
    @Resource
    private int insetBottom;
    @Resource
    private int insetLeft;
    @Resource
    private int insetRight;
    
    private ImageCellRenderer renderer;
    private EventList<LocalFileItem> listSelection;
    
    public ImageList(EventList<LocalFileItem> eventList, LocalFileList fileList) {
        super(new ImageListModel(eventList, fileList));

        GuiUtils.assignResources(this); 
        
        setBackground(backgroundListcolor);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setLayoutOrientation(JList.HORIZONTAL_WRAP);
        //this must be set to negative 1 to get horizontal line wrap
        setVisibleRowCount(-1);

        // add in inset size when calculated fixed cell dimensions
        // inset spacing is the white space you will see between images
        setFixedCellHeight(imageBoxHeight + insetTop + insetBottom);
        setFixedCellWidth(imageBoxWidth + insetRight + insetLeft);
        
        //enable double click launching of image files.
        addMouseListener(new ImageDoubleClickMouseListener());
        EventSelectionModel<LocalFileItem> selectionModel = GlazedListsSwingFactory.eventSelectionModel(eventList);
        this.listSelection = selectionModel.getSelected();
        setSelectionModel(selectionModel);
        selectionModel.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
    }
    
    /** Returns a copy of all selected items. */
    @Override
    public List<LocalFileItem> getSelectedItems() {
        return new ArrayList<LocalFileItem>(listSelection);
    }
    
    @Override
    public List<LocalFileItem> getAllItems() {
        return new ArrayList<LocalFileItem>( ((ImageListModel)getModel()).getFileList().getSwingModel());
    }

    @Override
    public void selectAll() {
        getSelectionModel().setSelectionInterval(0, getModel().getSize()-1);
    }

    @Override
    public void dispose() {
        ((ImageListModel)getModel()).dispose();
        ((EventSelectionModel)getSelectionModel()).dispose();
    }

    @SuppressWarnings("unchecked")
    public EventList<LocalFileItem> getListSelection() {
        return ((EventSelectionModel<LocalFileItem>)getSelectionModel()).getSelected();
    }
    
    /**
     * Sets the popup Handler for this List. 
     */
    public void setPopupHandler(final TablePopupHandler popupHandler) {
        addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupHandler.maybeShowPopup(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }
    
    /**
     * Sets the renderer on this List. The renderer is wrapped in an empty
     * border to create white space between the cells.
     */
    public void setImageCellRenderer(ImageCellRenderer renderer) {
        this.renderer = renderer;
        renderer.setInsets(BorderFactory.createEmptyBorder(insetTop,insetLeft,insetBottom,insetRight));
        
        super.setCellRenderer(renderer);
    }
    
    /**
     * Returns the CellRenderer for this list
     */
    public ImageCellRenderer getImageCellRenderer() {
        return renderer;
    }
    
    /**
     * This class listens for double clicks inside of the ImageList.
     * When a double click is detected, the relevant item in the list is launched.
     */
    private final class ImageDoubleClickMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                ImageList imageList = (ImageList)e.getComponent();
                int index = imageList.locationToIndex(e.getPoint());
                LocalFileItem val = (LocalFileItem) imageList.getElementAt(index);
                File file = val.getFile();
                NativeLaunchUtils.safeLaunchFile(file);
            }
        }
    }
}
