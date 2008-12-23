package org.limewire.ui.swing.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.settings.FilterSettings;

import com.google.inject.Inject;

public class FilterListImpl implements FilterList {
    @Inject private SpamManager spamManager;
    
    @Override
    public void addIPToFilter(String ipAddress) {
        List<String> blackList = new ArrayList<String>(Arrays.asList(FilterSettings.BLACK_LISTED_IP_ADDRESSES.getValue()));
        blackList.add(ipAddress);
        FilterSettings.USE_NETWORK_FILTER.setValue(true);
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(blackList.toArray(new String[blackList.size()]));
        spamManager.reloadIPFilter();
    }
}
