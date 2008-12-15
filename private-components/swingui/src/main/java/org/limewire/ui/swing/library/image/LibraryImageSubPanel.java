package org.limewire.ui.swing.library.image;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.AbstractLayerUI;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.images.ImageCellRenderer;
import org.limewire.ui.swing.images.ImageList;
import org.limewire.ui.swing.images.ImageListModel;
import org.limewire.ui.swing.library.table.Configurable;
import org.limewire.ui.swing.library.table.menu.MyImageLibraryPopupHandler;
import org.limewire.ui.swing.library.table.menu.MyImageLibraryPopupHandler.ImageLibraryPopupParams;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.EventSelectionModel;

public class LibraryImageSubPanel extends JPanel implements ListEventListener<LocalFileItem> {
    
    @Resource
    private Color lineColor;
    @Resource
    private int lineSize;
    @Resource
    private Color backgroundColor;
    @Resource
    private Color mainLabelColor;
    @Resource
    private int mainLabelFontSize;
    @Resource
    private Icon panelIcon;
    
    private final ImageList imageList;    
    
    private  JXLayer<JComponent> layer;

    private EventList<LocalFileItem> currentEventList;
    private EventList<LocalFileItem> listSelection;    
    
    public LibraryImageSubPanel(final File parentFolder, EventList<LocalFileItem> eventList, LocalFileList fileList, ImageLibraryPopupParams params) {       
        GuiUtils.assignResources(this); 
        
        setBackground(backgroundColor);
        
        String name = parentFolder.getName();
        
        this.currentEventList = eventList;
        JLabel headerLabel = new JLabel(name, panelIcon, JLabel.CENTER);
        headerLabel.setForeground(mainLabelColor);
        FontUtils.setSize(headerLabel, mainLabelFontSize);
        FontUtils.bold(headerLabel);
        
        
        headerLabel.addMouseListener(new HeaderDoubleClickMouseListener(parentFolder));
        
        // black separator
        Line line = Line.createHorizontalLine(lineColor, lineSize);
        
        imageList = new ImageList(currentEventList, fileList);
        EventSelectionModel<LocalFileItem> model = new EventSelectionModel<LocalFileItem>(currentEventList);
        imageList.setSelectionModel(model);
        model.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
        this.listSelection = model.getSelected();
        //imageList.setTransferHandler(transferHandler);
        imageList.setPopupHandler(new MyImageLibraryPopupHandler(this, params));
        
        // top row should never be tall than 30pixels, the bottom row(table, should fill any remaining space
        setLayout(new MigLayout("gap 0, insets 18 4 10 4",     //layout constraints
                "[] [] ",                       // column constraints
                "[::30] [] [grow][grow]" ));    // row constraints
        
        add(headerLabel, "gapbottom 4, push, wrap");       // first row
       // add(unShareButtonLabel, "gapbottom 2, split 2");
        
        // second row
        add(line, "span 2, growx 100, height :: 3, wrap");
        
        JScrollPane scrollPane = new JScrollPane(imageList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        layer = new JXLayer<JComponent>(scrollPane, new  AbstractLayerUI<JComponent>());
        layer.getGlassPane().setLayout(null);
        //third row
        add(layer, "span 2, grow");

        eventList.addListEventListener(this);
    }
    
    public void setImageEditor(TableRendererEditor editor) {
        new MouseReaction(imageList, editor);
        layer.getGlassPane().add(editor);
    }
    
    public ImageList getImageList() {
        return imageList;
    }   
    
    public ImageListModel getModel(){
        return (ImageListModel)imageList.getModel();
    }
    
    public List<LocalFileItem> getSelectedItems(){
        return new ArrayList<LocalFileItem>(listSelection);
    }

    
    @Override
    public void listChanged(ListEvent<LocalFileItem> listChanges) {
        final int size = listChanges.getSourceList().size();
        SwingUtils.invokeLater(new Runnable(){
            public void run() {
                setVisible(size > 0);
            }
        });
    }
    
    /**
     * This class listens for double clicks on the header item for each library sub panel.
     * When a double click is detected it will explore the folder represented by this subpanel. 
     */
    private final class HeaderDoubleClickMouseListener extends MouseAdapter {
        private final File parentFolder;

        private HeaderDoubleClickMouseListener(File parentFolder) {
            this.parentFolder = parentFolder;
        }

        @Override
            public void mouseClicked(MouseEvent e) {
               if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                   NativeLaunchUtils.launchExplorer(parentFolder);
               }
            }
    }


    public class MouseReaction implements MouseListener, MouseMotionListener {

        private ImageList imageList;
        private TableRendererEditor hoverComponent;
        
        public MouseReaction(ImageList imageList, TableRendererEditor hoverComponent) {
            this.imageList = imageList;          
            this.hoverComponent = hoverComponent;

            imageList.addMouseListener(this);
            imageList.addMouseMotionListener(this);
        }
        
        @Override
        public void mouseEntered(MouseEvent e) {
            update(e.getPoint());
        }

        @Override
        public void mouseExited(MouseEvent e) { 
            if(!hoverComponent.getBounds().contains(e.getPoint())) {
                hoverComponent.setVisible(false);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            int index = imageList.locationToIndex(e.getPoint());
            if(!e.isPopupTrigger() && !e.isShiftDown() && !e.isControlDown() && !e.isMetaDown() && index > -1)
                imageList.setSelectedIndex(index);
        }
        @Override
        public void mousePressed(MouseEvent e) {}
        @Override
        public void mouseReleased(MouseEvent e) {}
        @Override
        public void mouseDragged(MouseEvent e) {}

        @Override
        public void mouseMoved(MouseEvent e) {
            update(e.getPoint());
        }
        
        private void update(Point point){
            if (imageList.getModel().getSize() > 0) {
                int index = imageList.locationToIndex(point);
                if (index > -1) {
                    hoverComponent.setVisible(true);
                    ((Configurable)hoverComponent).configure((LocalFileItem) imageList.getModel().getElementAt(index), true);
                    Rectangle bounds = imageList.getCellBounds(index, index);
                    ImageCellRenderer renderer = imageList.getImageCellRenderer();
                    hoverComponent.setLocation(bounds.x + renderer.getSubComponentLocation().x,
                            bounds.y + renderer.getSubComponentLocation().y);
                }
            }
        }
    }

  
    public void dispose() {
        ((ImageListModel)imageList.getModel()).dispose();
    }
}
