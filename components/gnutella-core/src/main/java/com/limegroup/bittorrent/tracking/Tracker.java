package com.limegroup.bittorrent.tracking;

/**
 * Defines an interface for a torrent Tracker.
 * <p>
 * A <a href="http://en.wikipedia.org/wiki/BitTorrent_tracker">Tracker</a>
 * coordinates communication between peers attempting to download the 
 * payload of the torrents.
 */
public interface Tracker {
    /**
     * possible EVENT codes for the tracker protocol
     */
    enum Event {
        START(100, "started"), STOP(0, "stopped"), COMPLETE(20, "completed"), NONE(50, null);

        private final String numWant;

        private final String description;

        Event(int numWant, String description) {
            this.numWant = numWant > 0 ? Integer.toString(numWant) : null;
            this.description = description;
        }

        final String getNumWant() {
            return numWant;
        }

        final String getDescription() {
            return description;
        }
    }

    /**
     * Notifies the tracker that a request to it failed.
     * 
     * @return how many times it had failed previously.
     */
    void recordFailure();

    /**
     * Notifies the tracker that a request completed successfully.
     */
    void recordSuccess();

    /**
     * @return how many consecutive failures we have for this tracker.
     */
    int getFailures();

    /**
     * Does a tracker request for a certain event code
     * 
     * @param event the event code to send to the tracker
     * @return TrackerResponse holding the data the tracker sent or null if the
     *         tracker did not send any data
     */
    TrackerResponse request(Event event);

    String toString();
}