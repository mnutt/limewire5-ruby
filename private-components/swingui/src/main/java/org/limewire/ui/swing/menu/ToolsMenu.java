package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.Action;
import javax.swing.JMenu;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.advanced.AdvancedToolsPanel;
import org.limewire.ui.swing.event.OptionsDisplayEvent;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchCategoryUtils;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.search.SearchNavItem;
import org.limewire.ui.swing.search.SearchNavigator;
import org.limewire.ui.swing.upload.UploadMediator;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * The Tools menu in the main menubar.
 */
class ToolsMenu extends MnemonicMenu {

    /** Currently displayed Advanced Tools content panel. */
    private AdvancedToolsPanel advancedTools;
    
    @Inject
    public ToolsMenu(final Provider<AdvancedToolsPanel> advancedProvider, 
            final Navigator navigator, final UploadMediator uploadMediator,
            final SearchHandler searchHandler, final LibraryManager libraryManager, final SearchNavigator searchNavigator) {
        super(I18n.tr("&Tools"));

        add(new AbstractAction(I18n.tr("&Uploads")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                NavItem navItem = navigator.getNavItem(NavCategory.UPLOAD, UploadMediator.NAME);
                if (navItem == null) {
                    navItem = navigator.createNavItem(NavCategory.UPLOAD, UploadMediator.NAME, uploadMediator);
                }
                navItem.select();
            }
        });
        
        addSeparator();
        add(new AbstractAction(I18n.tr("Advanced &Search")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                SearchNavItem item = searchNavigator.addAdvancedSearch();
                item.select();
            }
        });
        
        add(createWhatsNewSubmenu(searchHandler));
        addSeparator();
        
        add(new AbstractAction(I18n.tr("&Advanced Tools...")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // If the existing Advanced Tools panel is null, then create a
                // new one, along with a listener to clear the reference when
                // the window is closed.
                WindowListener closeListener = null;
                if (advancedTools == null) {
                    advancedTools = advancedProvider.get();
                    closeListener = new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            advancedTools = null;
                        }
                    };
                }
                advancedTools.display(closeListener);
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
