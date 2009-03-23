package org.limewire.ui.swing.library;

import javax.swing.Action;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.components.decorators.TextFieldDecorator;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;

import ca.odell.glazedlists.FilterList;

//TODO: there's so little left. should just pull this up to AbstractFileListPanel
class LibraryPanel extends AbstractFileListPanel {

    public LibraryPanel(HeaderBarDecorator headerBarFactory, TextFieldDecorator textFieldDecorator) {
        super(headerBarFactory, textFieldDecorator);
    }
    
    @Override
    protected HeaderBar createHeaderBar(HeaderBarDecorator headerBarDecorator) {
        HeaderBar bar = new HeaderBar();
        headerBarDecorator.decorateBasic(bar);
        return bar;
    }
    
    @Override
    protected PromptTextField createFilterField(TextFieldDecorator decorator, String prompt) {
        PromptTextField field = new PromptTextField(prompt);
        decorator.decorateClearablePromptField(field, AccentType.SHADOW);
        return field;
    }
    
    @Override
    protected <T extends FileItem> void addCategorySizeListener(Category category, Action action,
            FilterList<T> filteredAllFileList, FilterList<T> filteredList) {
    }
}
