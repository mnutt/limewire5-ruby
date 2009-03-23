package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JMenu;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.advanced.AdvancedToolsPanel;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.event.OptionsDisplayEvent;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchCategoryUtils;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.search.advanced.AdvancedSearchPanel;
import org.limewire.ui.swing.upload.UploadPanel;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class ToolsMenu extends MnemonicMenu {

    @Inject
    public ToolsMenu(final Provider<AdvancedToolsPanel> advancedProvider,
            final Navigator navigator,
            SearchHandler searchHandler, final LibraryManager libraryManager, final Provider<UploadPanel> uploadPanelProvider) {
        super(I18n.tr("&Tools"));

        add(new AbstractAction(I18n.tr("&Downloads")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                NavItem navItem = navigator
                        .getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME);
                navItem.select();
            }
        });
        
        add(new AbstractAction(I18n.tr("&Uploads")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                NavItem navItem = navigator.getNavItem(NavCategory.UPLOAD, UploadPanel.NAME);
                if (navItem == null) {
                    navItem = navigator.createNavItem(NavCategory.UPLOAD, UploadPanel.NAME, uploadPanelProvider.get());
                }
                
                navItem.select();
            }
        });
        
        addSeparator();
        add(new AbstractAction(I18n.tr("Advanced &Search")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                NavItem navItem = navigator.getNavItem(NavCategory.LIMEWIRE, AdvancedSearchPanel.NAME);
                if (navItem != null) {
                    navItem.select();
                }
            }
        });
        
        add(createWhatsNewSubmenu(searchHandler));
        addSeparator();
        
        add(new AbstractAction(I18n.tr("&Advanced Tools...")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                AdvancedToolsPanel advancedTools = advancedProvider.get();
                advancedTools.display();
            }
        });
        if (!OSUtils.isMacOSX()) {
            addSeparator();
            add(new AbstractAction(I18n.tr("&Options...")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new OptionsDisplayEvent().publish();
                }
            });
        }
    }

    private JMenu createWhatsNewSubmenu(final SearchHandler searchHandler) {
        JMenu menu = new MnemonicMenu(I18n.tr("&What's New Search"));
        for (final SearchCategory category : SearchCategory.values()) {
            if (category == SearchCategory.OTHER) {
                continue;
            }

            Action action = new AbstractAction(SearchCategoryUtils.getWhatsNewMenuName(category)) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    searchHandler.doSearch(DefaultSearchInfo.createWhatsNewSearch(category));
                }
            };
            menu.add(action);
        }
        return menu;
    }
}
