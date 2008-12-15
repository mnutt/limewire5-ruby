package com.limegroup.gnutella.gui.library;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.text.Document;

import org.limewire.util.MediaType;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.gui.AutoCompleteTextField;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.search.SearchField;
import com.limegroup.gnutella.gui.search.SearchInformation;
import com.limegroup.gnutella.gui.search.SearchMediator;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.SharedFilesKeywordIndex;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;

/**
 * Panel that embeds the search bar for the library panel. 
 * 
 * Contains a label, a search field and a button horizontally aligned. The
 * button triggers a search action that searches the shared files for the
 * keywords in the text field and hands the search results to the
 * {@link LibrarySearchResultsHolder}.
 */
public class LibrarySearchPanel extends JPanel {

	private AutoCompleteSearchField queryField = new AutoCompleteSearchField(40);
	
	private final QueryRequestFactory queryRequestFactory;
	        
	LibrarySearchPanel(QueryRequestFactory queryRequestFactory) {
		super(new GridBagLayout());
		this.queryRequestFactory = queryRequestFactory;
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(0, 2, 0, 2);
		gbc.anchor = GridBagConstraints.WEST;
		JLabel label = new JLabel(I18n.tr("Search In Shared Files:") + " ");
		label.setLabelFor(queryField);
		label.setDisplayedMnemonic('S');
		add(label, gbc);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		add(queryField, gbc);
		Action a = new SearchLibraryAction();
		GUIUtils.bindKeyToAction(queryField, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), a);
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		add(new JButton(a), gbc);
	}
        
	private class SearchLibraryAction extends AbstractAction {
		
		public SearchLibraryAction() {
			putValue(Action.NAME,
					I18n.tr("Search"));
		}
		
		public boolean validate(SearchInformation info) {
		    switch (SearchMediator.validateInfo(info)) {
		    case SearchMediator.QUERY_EMPTY:
		        return false;
//		    case SearchMediator.QUERY_TOO_SHORT:  // not a problem for library
//	            GUIMediator.showError(I18n.tr("Your search must be at least three characters long to avoid too many meaningless results."));
//	            return false;
//	        case SearchMediator.QUERY_TOO_LONG:   // not possible here, since field doesn't accept it
//	            GUIMediator.showError(I18n.tr("Your search is too long. Please make your search smaller and try again."));
//	            return false;
	        case SearchMediator.QUERY_XML_TOO_LONG:
	            // cannot happen
	        case SearchMediator.QUERY_VALID:
	        default:
	            return true;
		    }
		}
		
		public void actionPerformed(ActionEvent e) {
			String query = queryField.getText().trim();
			if (query.length() == 0) {
				queryField.getToolkit().beep();
				return;
			}
			final SearchInformation info = SearchInformation.createKeywordSearch(query, null, MediaType.getAnyTypeMediaType());
			if (!validate(info)) {
			    return;
			}
			queryField.addToDictionary();
			BackgroundExecutorService.schedule(new Runnable() {
				public void run() {
					QueryRequest request = queryRequestFactory.createQuery(info.getQuery());
					FileManager fileManager = GuiCoreMediator.getFileManager();
					SharedFilesKeywordIndex keywordIndex = GuiCoreMediator.getSharedFilesKeywordIndex();
					Response[] resps = keywordIndex.query(request);
					ArrayList<File> files = new ArrayList<File>(resps.length);
					for (Response response : resps) {
					    FileDesc fd = fileManager.getGnutellaFileList().getFileDescForIndex((int)response.getIndex());
						if (fd != null) {
							files.add(fd.getFile());
						}
					}
					final File[] filesArray = files.toArray(new File[files.size()]);
					Runnable r = new Runnable() {
						public void run() {
							LibraryTree.instance().getSearchResultsHolder().setResults(filesArray);
							LibraryTree.instance().setSearchResultsNodeSelected();
						}
					};
					GUIMediator.safeInvokeLater(r);
				}
			});
		}
		
	}
	
	private class AutoCompleteSearchField extends AutoCompleteTextField {
		
		public AutoCompleteSearchField(int columns) {
			super(columns);
		}
		
		@Override
		protected Document createDefaultModel() {
			return new SearchField.SearchFieldDocument();
		}
		
	}
	
}