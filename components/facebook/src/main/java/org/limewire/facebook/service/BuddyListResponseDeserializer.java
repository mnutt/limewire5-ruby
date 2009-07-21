package org.limewire.facebook.service;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

class BuddyListResponseDeserializer {
    
    private static final Log LOG = LogFactory.getLog(BuddyListResponseDeserializer.class);
    
    /**
     * @return set of friend ids that are online, always returns mutable set
     */
    public Set<String> parseOnlineFriendIds(String response) throws JSONException, IOException {
        //for (;;);{"error":0,"errorSummary":"","errorDescription":"No error.","payload":{"buddy_list":{"listChanged":true,"availableCount":1,"nowAvailableList":{"UID1":{"i":false}},"wasAvailableIDs":[],"userInfos":{"UID1":{"name":"Buddy 1","firstName":"Buddy","thumbSrc":"http:\/\/static.ak.fbcdn.net\/pics\/q_default.gif","status":null,"statusTime":0,"statusTimeRel":""},"UID2":{"name":"Buddi 2","firstName":"Buddi","thumbSrc":"http:\/\/static.ak.fbcdn.net\/pics\/q_default.gif","status":null,"statusTime":0,"statusTimeRel":""}},"forcedRender":true},"time":1209560380000}}  
        //for (;;);{"error":0,"errorSummary":"","errorDescription":"No error.","payload":{"time":1214626375000,"buddy_list":{"listChanged":true,"availableCount":1,"nowAvailableList":{},"wasAvailableIDs":[],"userInfos":{"1386786477":{"name":"\u5341\u4e00","firstName":"\u4e00","thumbSrc":"http:\/\/static.ak.fbcdn.net\/pics\/q_silhouette.gif","status":null,"statusTime":0,"statusTimeRel":""}},"forcedRender":null,"flMode":false,"flData":{}},"notifications":{"countNew":0,"count":1,"app_names":{"2356318349":"\u670b\u53cb"},"latest_notif":1214502420,"latest_read_notif":1214502420,"markup":"<div id=\"presence_no_notifications\" style=\"display:none\" class=\"no_notifications\">\u65e0\u65b0\u901a\u77e5\u3002<\/div><div class=\"notification clearfix notif_2356318349\" onmouseover=\"CSS.addClass(this, 'hover');\" onmouseout=\"CSS.removeClass(this, 'hover');\"><div class=\"icon\"><img src=\"http:\/\/static.ak.fbcdn.net\/images\/icons\/friend.gif?0:41046\" alt=\"\" \/><\/div><div class=\"notif_del\" onclick=\"return presenceNotifications.showHideDialog(this, 2356318349)\"><\/div><div class=\"body\"><a href=\"http:\/\/www.facebook.com\/profile.php?id=1190346972\"   >David Willer<\/a>\u63a5\u53d7\u4e86\u60a8\u7684\u670b\u53cb\u8bf7\u6c42\u3002 <span class=\"time\">\u661f\u671f\u56db<\/span><\/div><\/div>","inboxCount":"0"}},"bootload":[{"name":"js\/common.js.pkg.php","type":"js","src":"http:\/\/static.ak.fbcdn.net\/rsrc.php\/pkg\/60\/106715\/js\/common.js.pkg.php"}]}
        
        String prefix = "for (;;);";
		if(response.startsWith(prefix)) {
			response = response.substring(prefix.length());
        }
		JSONObject respObjs = FacebookUtils.parse(response);
		if(respObjs.getInt("error") == 0) {
		    JSONObject payload = (JSONObject) respObjs.get("payload");
		    if(payload != null){
		        JSONObject buddyList = (JSONObject) payload.get("buddy_list");
		        if(buddyList != null){
		            return deserialize(buddyList);
		        } else {
		            LOG.debug("no buddy_list");
		        }
		    } else {
		        LOG.debug("no payload");
		    }
		} else {
            int errorCode = respObjs.getInt("error");
		    String errorDescription = respObjs.getString("errorDescription");
		    throw new IOException("error: " + errorCode + ": " + errorDescription);
		}
        return new HashSet<String>();
    }
    
    private Set<String> deserialize(JSONObject buddyList) throws JSONException {
        JSONObject nowAvailableList = buddyList.optJSONObject("nowAvailableList");
		LOG.debugf("nowAvailableList: {0}", nowAvailableList);
		if (nowAvailableList == null) {
		    return new HashSet<String>();
		}
		@SuppressWarnings("unchecked")
		Iterator<String> it = nowAvailableList.keys();
		Set<String> onlineIds = new HashSet<String>();  
		while (it.hasNext()) {
		    onlineIds.add(it.next());
		}
		return onlineIds;
	}
}
