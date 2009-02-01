package org.limewire.ui.swing.library.sharing;

import java.util.List;

import org.limewire.collection.glazedlists.GlazedListsFactory;

import ca.odell.glazedlists.CompositeList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.matchers.TextMatcherEditor;

/**A sorted list that can be filtered using <code>setFilterText(String[] strings)</code>.  Gnutella is always
 * included in the list, regardless of the filter strings. 
 * 
 * The order of the list depends on whether or not the list is filtered.  When the list is unfiltered, Gnutella 
 * is first in the list.  When filtered, Gnutella is last.
 * 
 * <p>This class is not thread safe*/
class GnutellaFilteredList extends CompositeList<SharingTarget>{
    private EventList<SharingTarget> noShareFriendList;
    private EventList<SharingTarget> filteredNoShareFriendList;
    private EventList<SharingTarget> noShareGnutellaList;
    private FilterList<SharingTarget> filteredNoShareGnutellaList;
    private TextMatcherEditor<SharingTarget> textMatcher;
    private Matcher<SharingTarget> gnutellaMatcher;
    
    
    
    public GnutellaFilteredList() {
        setupSubLists();
    }
    
    private void setupSubLists() {
        noShareFriendList = createMemberList();

        //using TextComponentMatcherEditor would cause problems because it also uses DocumentListener so we 
        //have no guarantee about the order of sorting and selecting
        TextFilterator<SharingTarget> textFilter = new TextFilterator<SharingTarget>() {
            @Override
            public void getFilterStrings(List<String> baseList, SharingTarget element) {
                baseList.add(element.getFriend().getName());
                baseList.add(element.getFriend().getId());
            }
        };
        textMatcher = new TextMatcherEditor<SharingTarget>(textFilter);
        filteredNoShareFriendList = GlazedListsFactory.filterList(noShareFriendList, textMatcher);   
        
        
        noShareGnutellaList = createMemberList();
        gnutellaMatcher = new Matcher<SharingTarget>() {
            @Override
            public  boolean matches(SharingTarget item) {
                return filteredNoShareFriendList.size() > 0 || textMatcher.getMatcher().matches(item);
            }
        };
        filteredNoShareGnutellaList = GlazedListsFactory.filterList(noShareGnutellaList, gnutellaMatcher);            
        
        super.addMemberList(filteredNoShareFriendList);
        super.addMemberList(filteredNoShareGnutellaList);
    }

    public void addMatcherEditorListener(MatcherEditor.Listener<SharingTarget> listener) {
        textMatcher.addMatcherEditorListener(listener);
    }

    public void setFilterText(String[] strings){
        textMatcher.setFilterText(strings);
        //re-setting the matcher forces the list to update.  doing this from
        //a listener on textMatcher works one key stroke late.
        filteredNoShareGnutellaList.setMatcher(gnutellaMatcher);
    }
    
    public int getUnfilteredSize(){
        return noShareFriendList.size() + noShareGnutellaList.size();
    }
    
    @Override
    public boolean add(SharingTarget friend){
        if(friend.isGnutellaNetwork()){
            return noShareGnutellaList.add(friend);
        } else {
            return noShareFriendList.add(friend);
        }
    }

    
    @Override
    public void addMemberList(EventList<SharingTarget> list) {
        throw new UnsupportedOperationException("Can not add Lists to GnutellaFilteredList");
    }
    
}
