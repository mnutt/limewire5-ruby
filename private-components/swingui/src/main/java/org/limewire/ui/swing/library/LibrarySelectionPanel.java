package org.limewire.ui.swing.library;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.LayoutManager;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

/**
 * Inner Navigator in a given LibraryPanel.
 */
class LibrarySelectionPanel extends JPanel implements Disposable {
    
    @Resource private Color backgroundColor;
    
    private Map<String, InfoPanel> categoryInfoPanels = new HashMap<String, InfoPanel>();
    private Set<String> possibleShareValues;

    /** Map of catalog headings. */
    private final Map<Catalog.Type, JComponent> headingMap = new EnumMap<Catalog.Type, JComponent>(Catalog.Type.class);

    /**used to fill empty space and hold the info bar at the bottom of the screen. */
    private final JPanel selectionGrow = new JPanel(new MigLayout("fill"));
    
    private final CardLayout cardLayout = new CardLayout();
    private JPanel cardPanel = new JPanel(cardLayout);

    public LibrarySelectionPanel() {
        super(new MigLayout("insets 0, gap 0, fill, wrap, hidemode 3", "[125!]", ""));
        possibleShareValues = Collections.emptySet();
        init();
    }

    /**
     * Updates each applicable (supports share new always) InfoPanel with the set
     * of allowable names (for example, the set of friends in the user's roster)
     *
     * @param possibleShareValues
     */
    public void updateCollectionShares(Set<String> possibleShareValues) {
        this.possibleShareValues = possibleShareValues;

        for (InfoPanel infoPanel : categoryInfoPanels.values()) {
            if (infoPanel.supportsShareNewAlways()) {
                infoPanel.updateCategoryShareCount();
            }
        }
    }
    
    private void init() {
        GuiUtils.assignResources(this);
        setBackground(backgroundColor);
        
        selectionGrow.setOpaque(false);
        selectionGrow.add(cardPanel, "dock south");        
        
        add(selectionGrow, "dock south, aligny baseline, growy");
    }
    

    public void updateLayout(LayoutManager layout) {
        super.setLayout(layout);
        add(selectionGrow, "dock south, aligny baseline, growy");
    }

    /**
     * Adds the specified heading component and catalog type to the container.
     */
    public void addHeading(JComponent heading, Catalog.Type catalogType) {
        if (catalogType == Catalog.Type.CATEGORY) {
            add(heading, "growx");
        } else {
            add(heading, "growx, gaptop 40");
        }        
        headingMap.put(catalogType, heading);
    }
    
    /**
     * Sets the heading visibility for the specified catalog type.
     */
    public void setHeadingVisible(Catalog.Type catalogType, boolean visible) {
        JComponent heading = headingMap.get(catalogType);
        if (heading != null) {
            heading.setVisible(visible);
        }        
    }
    
    /**
     * Adds an info panel for the specified catalog and its file list. 
     */
    public<T extends FileItem> void addCard(Catalog catalog, EventList<T> fileList, boolean isFriendView) {
        InfoPanel panel = new InfoPanel<T>(catalog, fileList, isFriendView);
        categoryInfoPanels.put(catalog.getId(), panel);
        cardPanel.add(panel, catalog.getId());
    }
    
    /**
     * Adds an info panel for the specified category and its file list. 
     */
    public<T extends FileItem> void addCard(Category category, EventList<T> fileList, boolean isFriendView) {
        addCard(new Catalog(category), fileList, isFriendView);
    }
    
    /**
     * Displays the specified catalog info panel.  If <code>catalog</code> is
     * null, then the entire card panel is hidden since there is no catalog
     * visible in this library.
     */
    public void showCard(Catalog catalog) {
        if (catalog == null) {
            cardPanel.setVisible(false);
        } else {
            if (!cardPanel.isVisible()) {
                cardPanel.setVisible(true);
            }
            cardLayout.show(cardPanel, catalog.getId());
        }
    }
    
    /**
	 * Selects which category info panel to show. If category
	 * is null, hides the entire card panel since there is no
     * category visible in this library.
	 */
    public void showCard(Category category) {
        if(category == null) 
            cardPanel.setVisible(false);
        else {
            showCard(new Catalog(category));
        }
    }
    
    /**
     * Creates a subPanel in the inner nav. This panel displays file information 
     * about the particular category that is currently selected.
     */
    private class InfoPanel<T extends FileItem> extends JPanel implements Disposable, ListEventListener<T>, SettingListener {
        @Resource
        private Color backgroundColor;
        @Resource
        private Color fontColor;
        @Resource
        private Font categoryFont;
        @Resource
        private Font smallFont;
        
        private JLabel categoryLabel;
        private JLabel totalLabel;
        private JLabel collectionLabel;
        
        private final EventList<T> fileList;
        private final Catalog catalog;
        private StringArraySetting shareNewAlwaysSetting;
        private SettingListener categoryCollectionListener;
        
        public InfoPanel(Catalog catalog, EventList<T> fileList, boolean isFriendView) {
            super(new MigLayout("fillx, gap 0, insets 5 10 5 0, hidemode 3"));
            
            GuiUtils.assignResources(this);
                        
            this.fileList = fileList;
            this.catalog = catalog;

            // {0}: category (Audio, Video, etc)
            categoryLabel = new JLabel(I18n.tr("{0} Info", catalog.getName()));
            categoryLabel.setFont(categoryFont);
            categoryLabel.setForeground(fontColor);

            setBackground(backgroundColor);
            add(categoryLabel, "wrap, gapbottom 2");
            
            initTotalLabel(fileList);
            if(!isFriendView && supportsShareNewAlways())
                initCollectionListener();
        }
        
        /** 
         * Displays the total number of files in this category list. 
         */
        private void initTotalLabel(final EventList<T> fileList) {
            totalLabel = new JLabel();
            totalLabel.setFont(smallFont);
            totalLabel.setForeground(fontColor);
            setTotalLabel(fileList.size());
            add(totalLabel, "wrap");
            fileList.addListEventListener(this);
        }
        
        /**
         * Displays the number of collections that are shared of this category.
         * Assumes category supports "share new always"
         */
        private void initCollectionListener() {
            shareNewAlwaysSetting = getShareCategorySetting();

            createCollectionLabel();
            updateCategoryShareCount();

            categoryCollectionListener = new SettingListener() {
                @Override
                public void settingChanged(SettingEvent evt) {
                    updateCategoryShareCount();
                }
            };
            shareNewAlwaysSetting.addSettingListener(categoryCollectionListener);
        }


        public boolean supportsShareNewAlways() {
            return (getShareCategorySetting() != null);
        }

        /**
         * @return LibrarySetting associated with the category member variable
         *         null if not applicable (does not support share all in category)
         */
        private StringArraySetting getShareCategorySetting() {
            StringArraySetting shareNewAlwaysSetting = null;

            if (catalog.getCategory() != null) {
                switch (catalog.getCategory()) {
                case AUDIO:
                    shareNewAlwaysSetting = LibrarySettings.SHARE_NEW_AUDIO_ALWAYS;
                    break;
                case VIDEO:
                    shareNewAlwaysSetting = LibrarySettings.SHARE_NEW_VIDEO_ALWAYS;
                    break;
                case IMAGE:
                    shareNewAlwaysSetting = LibrarySettings.SHARE_NEW_IMAGES_ALWAYS;
                    break;
                default:
            }
            }
            return shareNewAlwaysSetting;
        }

        private void createCollectionLabel() {
            collectionLabel = new JLabel();
            collectionLabel.setFont(smallFont);
            collectionLabel.setForeground(fontColor);
            setCollectionLabel(0);
            add(collectionLabel, "wrap");
            collectionLabel.setVisible(!LibrarySettings.SNAPSHOT_SHARING_ENABLED.getValue());
            LibrarySettings.SNAPSHOT_SHARING_ENABLED.addSettingListener(this);
        }
        
        private void setTotalLabel(int total) {
            totalLabel.setText(I18n.tr("Total: {0}", total));
        }
        
        private void setCollectionLabel(int count) {
            if(collectionLabel != null)
                collectionLabel.setText(I18n.tr("Sharing Collection: {0}", count));
        }   

        @Override
        public void dispose() {
            if(fileList != null)
                fileList.removeListEventListener(this);
            if (categoryCollectionListener != null)
                shareNewAlwaysSetting.removeSettingListener(categoryCollectionListener);
        }

        @Override
        public void listChanged(ListEvent<T> listChanges) {
            if(fileList != null)
                setTotalLabel(fileList.size());
        }

        @Override
        public void settingChanged(SettingEvent evt) {
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    collectionLabel.setVisible(!LibrarySettings.SNAPSHOT_SHARING_ENABLED.getValue());                    
                }
            });
        }

        public void updateCategoryShareCount() {
            Set<String> shareNewAlways = new HashSet<String>(Arrays.asList(shareNewAlwaysSetting.get()));
            shareNewAlways.retainAll(possibleShareValues);
            updateShareCountLabel(shareNewAlways.size());
        }

        private void updateShareCountLabel(final int shareCount) {
            SwingUtils.invokeLater(new Runnable() {
                public void run() {
                    setCollectionLabel(shareCount);
                }
            });
        }
        
        public Catalog getCatalog() {
            return catalog;
        }
    }

    @Override
    public void dispose() {
        for(Disposable disposable : categoryInfoPanels.values())
            disposable.dispose();
    }
}
