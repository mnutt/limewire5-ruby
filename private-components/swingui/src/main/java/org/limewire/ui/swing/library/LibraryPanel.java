package org.limewire.ui.swing.library;

import javax.swing.Action;
import javax.swing.SwingUtilities;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.LimeHeaderBar;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.components.LimePromptTextField;
import org.limewire.ui.swing.painter.FilterPainter;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

class LibraryPanel extends AbstractFileListPanel {

    public LibraryPanel(LimeHeaderBarFactory headerBarFactory) {
        super(headerBarFactory);
    }
    
    @Override
    protected LimeHeaderBar createHeaderBar(LimeHeaderBarFactory headerBarFactory) {
        return headerBarFactory.createBasic();
    }
    
    @Override
    protected LimePromptTextField createFilterField(String prompt) {
        // Create filter field and install painter.
        return FilterPainter.decorate(new LimePromptTextField(prompt));
    }
    
    @Override
    protected <T extends FileItem> void addCategorySizeListener(Category category,
            Action action, FilterList<T> filteredAllFileList, FilterList<T> filteredList) {
        // Comment this out & return null if you don't want sizes added to library panels.
        ButtonSizeListener<T> listener = new ButtonSizeListener<T>(category, action, filteredList);
        filteredList.addListEventListener(listener);
        addDisposable(listener);
    }
    
    private static class ButtonSizeListener<T> implements Disposable, ListEventListener<T>, SettingListener {
        private final Category category;
        private final Action action;
        private final FilterList<T> list;
        
        private ButtonSizeListener(Category category, Action action, FilterList<T> list) {
            this.category = category;
            this.action = action;
            this.list = list;
            action.putValue(Action.NAME, I18n.tr(category.toString()));
            setText();
            if(category == Category.PROGRAM) {
                LibrarySettings.ALLOW_PROGRAMS.addSettingListener(this);
            }
        }

        private void setText() {
            //disable other category if size is 0
            if(category == Category.OTHER) {
                action.setEnabled(list.size() > 0);
            } else if(category == Category.PROGRAM) { // hide program category is not enabled
                action.setEnabled(LibrarySettings.ALLOW_PROGRAMS.getValue());
            }
        }
        
        @Override
        public void dispose() {
            list.removeListEventListener(this);
            if(category == Category.PROGRAM) {
                LibrarySettings.ALLOW_PROGRAMS.removeSettingListener(this);
            }
        }

        @Override
        public void listChanged(ListEvent<T> listChanges) {
            setText();
        }

        @Override
        public void settingChanged(SettingEvent evt) {
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    setText();                    
                }
            });
        }
    }
}
