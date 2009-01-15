package org.limewire.ui.swing.browser;

/** An action taken when an href with a target is clicked in the browser. */
public interface TargetedUrlAction {
    
    public static class TargetedUrl {
        private String target;
        private String url;

        public TargetedUrl(String target, String url) {
            this.target = target;
            this.url = url;
        }

        public String getTarget() {
            return target;
        }

        public String getUrl() {
            return url;
        }
    }
    
    /** Notification that a targeted URL has been clicked. */
    public void targettedUrlClicked(TargetedUrl targetedUrl);

}
