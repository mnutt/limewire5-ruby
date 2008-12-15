package com.limegroup.gnutella.gui;

import java.util.Arrays;

import org.limewire.i18n.I18nMarker;
import org.limewire.util.OSUtils;

public class TipOfTheDayMessages {
    
    private static final String FIRST_MESSAGE = I18nMarker.marktr("Tired of " +
            "downloads stopping halfway through? It helps to pick search results " +
            "with a higher number in the '#' column. The # is the amount of unique " +
            "places on the network that are hosting the file. The more sources, the " +
            "better the chance of getting your file. In cases where a junk result " +
            "appears to have a lot of sources, use the <font color=\"1E8E44\">Junk</font> " +
            "button to train LimeWire's junk filter.");

    /**
     * Determines whether or not the current locale language is English. Note
     * that the user setting may be empty, defaulting to the running system
     * locale which may be other than English. Here we check the effective
     * locale seen in the MessagesBundle.
     */
    static boolean hasLocalizedMessages() {
        return GUIMediator.isEnglishLocale() || !FIRST_MESSAGE.equals(I18n.tr(FIRST_MESSAGE));
    }   

    /**
     * Returns general tips that are shown on all operating systems.
     */
    public static String[] getGeneralMessages() {
        return new String[] {
                I18n.tr(FIRST_MESSAGE),
                I18n.tr("When your downloads say <font color=\"1E8E44\">Needs " + 
                        "More Sources</font>, try choosing that download and " + 
                        "then clicking <font color=\"1E8E44\">Find More " +
                        "Sources</font>. LimeWire will search the network for more " + 
                        "places to download the file and automatically resume downloading " + 
                        "if it finds any matches."),
                I18n.tr("When LimeWire starts up, it has to connect to the network. " + 
                        "During this time, the bar in the bottom-left corner will " + 
                        "change colors. Once all bars are green (or blue to indicate a" + 
                        "LimeWire PRO or an Ultrapeer connection), LimeWire has " + 
                        "completely connected."),
                I18n.tr("You can change the look and feel of LimeWire by going to "+
                        "<font color=\"1E8E44\">View</font> &gt; <font color=\"" + 
                        "1E8E44\">Apply Skins</font> and choosing a new skin. " + 
                        "Additional skins are on LimeWire's website, " +
                        "available by choosing <font color=\"1E8E44\">Get More " + 
                        "Skins</font>. If you download skins by the <font color" + 
                        "=\"1E8E44\">Download via LimeWire</font> " +
                        "link, LimeWire will automatically ask if you want to " +
                        "use the new skin when the download completes."),
                I18n.tr("You can sort your search results by clicking on a column. " + 
                        "The most useful column to sort by is the '#' column, which " + 
                        "can put the results that have the highest chance of downloading " + 
                        "at the top of the list."),
                I18n.tr("The more stars a search result has, the better the " + 
                        "chances of the download completing successfully. Four " + 
                        "blue stars signal 'Ethernet' speed and have the greatest " + 
                        "chance of completing successfully."),
                I18n.tr("To find out how many times someone has uploaded a file " + 
                        "from you, choose the <font color=\"1E8E44\">Library</font> tab and " + 
                        "look at the <font color=\"1E8E44\">Uploads</font> column. The " + 
                        "information is listed as 'X / Y', where 'Y' is the " + 
                        "number of people who have attempted to upload that file, " + 
                        "and 'X' is the number of people who have successfully uploaded it."),
                I18n.tr("Curious how many people are searching for your files? " + 
                        "The <font color=\"1E8E44\">Hits</font> column in the <font color" + 
                        "=\"1E8E44\">Library</font> tab counts how many times " + 
                        "LimeWire has returned a result from someone searching " + 
                        "for that file."),
                I18n.tr("It helps the network if you keep your LimeWire running; " + 
                        "other users will connect to the network easier and " + 
                        "searches will perform better."),
                I18n.tr("When LimeWire says your download is <font color=\"1E8E44\">" + 
                        "Waiting In Line</font>, the source you are downloading " + 
                        "from is temporarily busy and has queued your download. " + 
                        "In order to successfully complete your download, wait " + 
                        "while your request advances in line and is eventually serviced."),
                I18n.tr("If you need more help, visit <a href=\"{0}\">our message " + 
                        "boards</a> to see if others have already answered your " + 
                        "question. You can also review the <a href=\"{1}\">FAQ</a> " + 
                        "for answers to commonly asked questions or the " + 
                        "<a href=\"{2}\">User Guide</a> for more information.", "http://www.limewire.org/forum/", "http://www.limewire.com/faq", "http://www.limewire.com/userguide"),
                I18n.tr("You can resume an accidental cancel of a download by " + 
                        "clicking the <font color=\"1E8E44\">Library</font> tab, " + 
                        "then selecting <font color=\"1E8E44\">Incomplete</font> " + 
                        "folder. Then highlight the filename and click <font " + 
                        "color=\"1E8E44\">Resume</font>. LimeWire automatically " + 
                        "searches for that file and begins downloading it again."),
                I18n.tr("If you have any tips on using LimeWire, add it to <a " + 
                        "href=\"{0}\">the wiki</a> and it may make it into a future " + 
                        "tip of the day.", "http://www.limewire.org/wiki/index.php?title=Tips_Of_The_Day"),
                I18n.tr("You can restart downloads that say <font color=\"1E8E44\">" + 
                        "Awaiting Sources</font> by searching for that file again. " + 
                        "LimeWire will automatically restart the download if it finds " + 
                        "any matching results."),
                I18n.tr("Post feature suggestions on LimeWire's <a href=\"{0}\">message " + 
                        "boards</a> and it may be added to future versions.", "http://www.limewire.org/forum/"),
                I18n.tr("Visit <a href=\"{0}\">LimeWire's website</a> for detailed " + 
                        "information about how skins are created.", "http://www.limewire.com/features/skins/make.php"),
                I18n.tr("Do you want to be part of LimeWire's development? Visit " + 
                        "<a href=\"{0}\">LimeWire's Open Source website</a>.", "http://www.limewire.org/"),
                I18n.tr("You can preview downloading files to make sure they are what " + 
                        "you expect; select the download and click <font color=\"1E8E44\">Preview</font>."),
                I18n.tr("Curious what hosts LimeWire is connecting or connected to you? " + 
                        "Select <font color=\"1E8E44\">View</font> &gt; <font color=\"1E8E44\">" + 
                        "Show/Hide</font>, <font color=\"1E8E44\">Connections</font> to take a " + 
                        "look at LimeWire's connections."),
                I18n.tr("Passionate about digital rights? Visit the <a href=\"{0}\">Electronic " + 
                        "Frontier Foundation</a> to see what you can do to help.", "http://www.limewire.com/eff"),
                I18n.tr("LimeWire is translated into many different languages " + 
                        "including French, German, Greek, Italian, and many more. Visit " + 
                        "LimeWire's <a href=\"{0}\">internationalization page</a> for " + 
                        "information on how you can help translation efforts.", "http://www.limewire.org/translate.shtml"),
                I18n.tr("Do you want to sport the LimeWire look? Visit our <a " + 
                        "href=\"{0}\">LimeWire gear</a> page and purchase a " + 
                        "T-shirt or hat.", "http://www.cafepress.com/limewire"),
                I18n.tr("You can do an audio genre search?<br>Click on the <font " + 
                        "color=\"1E8E44\">Audio</font> metadata search window, " + 
                        "choose a genre from the drop-down box and then press <font " + 
                        "color=\"1E8E44\">Search</font>. Most, if not all, of the " + 
                        "files returned will be from the selected genre."),
                I18n.tr("Try a <font color=\"1E8E44\">What's New</font> search and " + 
                        "see what's recently been added to the Gnutella network."),
                I18n.tr("Wondering how to avoid a bombardment of nonsense search " + 
                        "results? Select the search result and then click the <font " + 
                        "color=\"1E8E44\">Junk</font> button. As you mark search " + 
                        "results as <font color=\"1E8E44\">Junk</font> or <font " + 
                        "color=\"1E8E44\">Not Junk</font>, LimeWire learns what " + 
                        "to block and what not to block. After a short training " + 
                        "period, LimeWire automatically blocks most junk results."),
                I18n.tr("Small variations in the search terms will still work. For " + 
                        "example, if your buddy is sharing 'Limers' but you searched " + 
                        "for 'The Limers', your buddy's file will still be found."),
                I18n.tr("LimeWire supports Gnutella and BitTorrent peer-to-peer file " + 
                        "sharing protocols."),
                I18n.tr("You can download a file with BitTorrent by clicking a " + 
                        "link to a .torrent file on the Internet, or dragging a " + 
                        ".torrent file to anywhere on the <font color=\"1E8E44\">" + 
                        "Search</font> tab."),
                I18n.tr("You can drag a file or folder to the <font color" + 
                        "=\"1E8E44\">Library</font> tab to individually share " + 
                        "files. In the <font color=\"1E8E44\">Library</font> tab, " + 
                        "select <font color=\"1E8E44\">Individually Shared Files</font>" + 
                        " to see your newly shared file."),
                I18n.tr("Search results come from other users like you. Search " + 
                        "results marked with a lime icon in the <font color" + 
                        "=\"1E8E44\">Quality</font> column are official " + 
                        "communications from Lime Wire LLC."),
                I18n.tr("After starting a search, you can customize the three " + 
                        "boxes on the left with titles like Media, Artist and " + 
                        "Album for more customized search results. Also, you can " + 
                        "change what these boxes show. Click the circle in the " + 
                        "upper left corner of a box and choose a new option from the menu."),
                I18n.tr("You can browse your friend's Library through a <font " + 
                        "color=\"1E8E44\">Direct Connect</font>. Go to <font " + 
                        "color=\"1E8E44\">Search</font> &gt; <font " + 
                        "color=\"1E8E44\">Direct Connect</font>, then enter " + 
                        "the ip address and port number of your friend's " + 
                        "computer in the following format: 'ip address:port'." + 
                        " You can find the port value via the <font " + 
                        "color=\"1E8E44\">Options</font> &gt; <font " + 
                        "color=\"1E8E44\">Advanced</font> &gt; <font " + 
                        "color=\"1E8E44\">Firewall</font> section."),
                I18n.tr("If you try to download a file that you already have, " + 
                        "LimeWire lets you know, even if the file has a different name."),
                I18n.tr("Curious what are people's keyword searches? Go to the " + 
                        "<font color=\"1E8E44\">Monitor</font> tab and select " + 
                        "<font color=\"1E8E44\">Show Incoming Searches</font> to " + 
                        "see other people's search queries. Double-click an " + 
                        "incoming query to perform a search for that keyword."),
                I18n.tr("Have a lot of files in your Saved or Shared folder? " + 
                        "Try a <font color=\"1E8E44\">Search in Shared files</font>" + 
                        " search at the top of the <font color=\"1E8E44\">Library" + 
                        "</font> tab to find a file."),
                I18n.tr("You can publish your original work on the Gnutella " + 
                        "network with a Creative Commons license. Select the " + 
                        ".ogg or .mp3 shared file in the <font " + 
                        "color=\"1E8E44\">Library</font> tab and click the " + 
                        "<font color=\"1E8E44\">Publish</font> button to " + 
                        "publish your original work. Visit the <a " + 
                        "href=\"{0}\">Creative Commons website</a> for more " + 
                        "information.", "http://creativecommons.org/"),
                I18n.tr("You can fix a file's incomplete or totally wrong " + 
                        "information about its contents by selecting the file " + 
                        "in the <font color=\"1E8E44\">Library</font> tab " + 
                        "and clicking <font color=\"1E8E44\">Describe</font>. " + 
                        "You can even edit many files at once by selecting " + 
                        "more than one file before clicking <font " + 
                        "color=\"1E8E44\">Describe</font>."),
                I18n.tr("To see if you are you behind a firewall, look for " + 
                        "the globe at the bottom of LimeWire in the status bar." + 
                        " If there is a brick wall in front of the globe, " + 
                        "your Internet connection is firewalled."),
                I18n.tr("The number in the green oval in LimeWire's status " + 
                        "bar specifies the number of files you are sharing. " + 
                        "A beige oval means LimeWire is still building your " + 
                        "Library and a red oval means you aren't sharing any files."),
                I18n.tr("The numbers next to the up and down arrows at the " + 
                        "bottom of LimeWire show how fast all of your files " + 
                        "are downloading or uploading."),
                I18n.tr("You can increase the text size via <font " + 
                        "color=\"1E8E44\">View</font> &gt; <font " + 
                        "color=\"1E8E44\">Increase Font Size</font>."),
                I18n.tr("Instead of putting all of your files in a single " + 
                        "folder, LimeWire can automatically sort downloads " + 
                        "by media type. In the <font color=\"1E8E44\">" + 
                        "Options</font> &gt; <font color=\"1E8E44\">Saving" + 
                        "</font> window, select a media type like 'Audio', " + 
                        "and click the <font color=\"1E8E44\">Browse</font> " + 
                        "button. You can sort files by media type in the <font " + 
                        "color=\"1E8E44\">Library</font> tab even if all saved " + 
                        "files are in one folder."),
                I18n.tr("While LimeWire is running, your audio and video files " + 
                        "are automatically shared with iTunes and other " + 
                        "programs that support the DAAP protocol. You can " + 
                        "disable this sharing via <font color=\"1E8E44\">" + 
                        "Options</font> &gt; <font color=\"1E8E44\">iTunes" + 
                        "</font> &gt; <font color=\"1E8E44\">Sharing</font>."),
                I18n.tr("Your search queries travel through a network of " + 
                        "interconnected computers, which is why it takes " + 
                        "time to receive all of the search results. LimeWire " + 
                        "often takes shortcuts to deliver results faster, but " + 
                        "if you think search results are blocked because your " + 
                        "computer has problems with UDP packets, you can disable " + 
                        "OOB (out-of-band) searching in the <font color=\"1E8E44\">" + 
                        "Options</font> for more reliable results."), 
                I18n.tr("If you own the copyright to a file that is being " + 
                        "shared on the network and you don't want it to be " + 
                        "shared, you can have it filtered out of the network " + 
                        "search results. <a href=\"{0}\">More information</a>" + 
                        " is available.", "http://www.limewire.com/about/copyright.php"),
                I18n.tr("Unlike other peer-to-peer file-sharing programs, " + 
                        "LimeWire can transfer files even if both parties are" + 
                        " behind a firewall. You don't have to do anything extra" + 
                        " because it happens automatically."),
                I18n.tr("LimeWire uses a technology called DHT to help users " + 
                        "better find rare files. In order to be eligible for " + 
                        "inclusion in the DHT, make sure you are not behind a " + 
                        "firewall. Running LimeWire for as long as possible " + 
                        "will improve your chances of being in the DHT."),
                I18n.tr("LimeWire supports file sizes up to 1 TB (terabyte)."), 
                I18n.tr("Want to become an Ultrapeer? If you have a fast " + 
                        "connection without a firewall, you will be promoted to " + 
                        "Ultrapeer status after a while (keep LimeWire running " + 
                        "as long as possible.). Also, make sure you have not " + 
                        "disabled Ultrapeer capabilties in the <font color=\"1E" + 
                        "8E44\">Options</font>."), 
                I18n.tr("Curious about the inner workings of LimeWire? Go to " + 
                        "<font color=\"1E8E44\">View</font> &gt; <font color=\"1" + 
                        "E8E44\">Show/Hide</font> &gt; <font color=\"1E8E44\">" + 
                        "Console</font> to see LimeWire's log statements."),
                I18n.tr("Wondering if someone browsed your shared Library? Go to " + 
                        "<font color=\"1E8E44\">View</font> &gt; <font " + 
                        "color=\"1E8E44\">Show/Hide</font> &gt; <font " + 
                        "color=\"1E8E44\">Logging</font> to see recent activity."),
                I18n.tr("Ultrapeers help the network by distributing only " + 
                        "pertinent network traffic to the leaf nodes."),
                I18n.tr("How do you see which files in shared folders are " + 
                        "indeed shared? In the <font color=\"1E8E44\">Library" + 
                        "</font> tab, the column <font color=\"1E8E44\">Shared" + 
                        "</font> displays an icon to show shared (two arrows " + 
                        "forming a circle) or not shared (an X mark)."),               
                I18n.tr("Magnet links allow users to download files through " + 
                        "LimeWire from a web page. When you put a magnet " + 
                        "link on your web page (in the 'href' attribute of " + 
                        "anchor tags) and a user clicks the link, a download " + 
                        "will start in LimeWire. Also, you can copy and paste " + 
                        "the magnet link through <font color=\"1E8E44\">File</font>" + 
                        " &gt <font color=\"1E8E44\">Open Magnet or Torrent</font> to download."), 
                I18n.tr("The blinking plug in the top-right corner of " + 
                        "LimeWire indicates that a search is in progress. "),                        
                I18n.tr("You can expand search results by right-clicking a search result, then choosing <font color=\"1E8E44\">Search More</font>, then <font color=\"1E8E44\">Get More Results</font>. "),
                I18n.tr("You can change the columns that display search result information (columns include 'Title', 'Genre', 'Track', 'Length', and many more). Right-click on the search result column header, and check the columns you want to see."),
                I18n.tr("You can turn tool tips on or off in most tables by right-clicking on the column header and choosing <font color=\"1E8E44\">More Options</font>. You can toggle other options here too, like whether or not to sort tables automatically and if you prefer the rows to be striped."),
                I18n.tr("You can sort uploads, downloads, etc., by clicking on a column. You can turn this automatic sorting behavior off by right-clicking on a column header, choosing <font color=\"1E8E44\">More Options</font> and un-checking <font color=\"1E8E44\">Sort Automatically</font>."),
                I18n.tr("You can hide the <font color=\"1E8E44\">Icon</font> (or other) column in downloads by right-clicking on the column header and unchecking that column. You can also drag the columns around to make them appear in the order you'd like."),
                I18n.tr("You can tell LimeWire which downloads to start next by right-clicking on the column header in the download area and checking the <font color=\"1E8E44\">Priority</font> column. Select the download you want to start next and click the up arrow until it's at the top."),
                I18n.tr("You can save individual downloads to custom locations by right-clicking a search result and choosing <font color=\"1E8E44\">Download As...</font>. You can even change the location of a download in progress by right-clicking the download and choosing <font color=\"1E8E44\">Change File Location...</font>. "),
                I18n.tr("You can stop sharing a recently download file through the <font color=\"1E8E44\">Library</font> tab; right-click the file and choose <font color=\"1E8E44\">Stop Sharing File</font>. "),                       
           };
    }
    
    /**
     * Returns general tips that are shown on operating systems that are <b>not</b>
     * Mac OS X. Useful for tips that reference the About Window or the
     * Preferences Window, or right-clicking
     */
    public static String[] getNonMacOSXMessages() {
        return new String[] {
                I18n.tr("LimeWire is written in Java, which means that having the " + 
                        "latest version of Java will improve LimeWire's speed and " + 
                        "stability. You can find the most recent Java release at " + 
                        "<a href=\"{0}\">www.java.com</a>.", "http://www.java.com/"),
        };
    }

    /**
     * Returns general tips that are shown on Mac OS X.
     */
    public static String[] getMacOSXMessages() {
        return new String[] {
                I18n.tr("LimeWire is written in Java, which means that having" + 
                        " the latest version of Java will improve LimeWire's " + 
                        "speed and stability. You can upgrade to the latest " + 
                        "Java release by using 'Software Update'. "),
                I18n.tr("Tired of the OS X Aqua look? Try using a new theme; " + 
                        "themes are available from the <font color=\"1E8E44\">" + 
                        "View</font> &gt; <font color=\"1E8E44\">Apply " + 
                        "Skins</font> menu. "),
                I18n.tr("You can locate a shared file on your computer through the " + 
                        "<font color=\"1E8E44\">Library</font> tab; highlight the " + 
                        "file and click <font color=\"1E8E44\">Explore</font>. " + 
                        "LimeWire will open the folder containing the file in the Finder. "),
                I18n.tr("Are you using OS X's built-in firewall? Make sure " + 
                        "LimeWire's port is opened by clicking on the Apple " + 
                        "Menu &gt; System Preferences &gt; Sharing &gt; " + 
                        "Firewall &gt; New &gt; and choosing to open " + 
                        "'Gnutella/Limewire (6346)'. "),
                I18n.tr("Be careful not to share sensitive information " + 
                        "like tax documents, passwords, etc. LimeWire will " + 
                        "warn you if you attempt to share a folder that " + 
                        "people generally use to store sensitive data (such " + 
                        "as Users, System, Desktop, etc). "),
                I18n.tr("The Library is a file manager. That means when you " + 
                        "delete a file from the Library, you have the option " + 
                        "to either permanently delete the file from your " + 
                        "computer or move it to the Trash. "),
                I18n.tr("You can play music in your default media player " + 
                        "instead of in LimeWire by right-clicking (or " + 
                        "control-clicking) the LimeWire status bar and " + 
                        "deselecting <font color=\"1E8E44\">Show Media Player</font>. "),
        };
    }
    
    /**
     * A list of custom messages that need to be post-processed by {@link #buildCustom(String[])}.
     */
    public static String[][] getCustomMessages() {
        return new String[][] {
            new String[] { I18nMarker.marktr("You can customize LimeWire to your heart's content by changing various preferences such as your saved folder, shared folder, upload bandwidth, etc. These preferences (and more) are available at {0}."), "PREF" },
            new String[] { I18nMarker.marktr("You can find out which version of LimeWire you are using by choosing {0}."), "HELP"},
            new String[] { I18nMarker.marktr("Be a good network participant, don't close LimeWire if someone is uploading from you. You can tell LimeWire to automatically close after all downloads and uploads (transfers) are complete via {0} and looking at the <font color=\"1E8E44\">System Tray</font> options."), "PREF"},
            new String[] { I18nMarker.marktr("LimeWire does not automatically share all types of files. Therefore, if you have a specific file you'd like to share, either share the file individually or make sure its extension is being shared. Select {0} &gt; <font color=\"1E8E44\">Sharing</font>, <font color=\"1E8E44\">Types</font> to modify the file extensions to share."), "PREF"},
            new String[] { I18nMarker.marktr("You can turn autocomplete for searching on or off through {0} &gt; <font color=\"1E8E44\">View</font>, <font color=\"1E8E44\">Autocomplete</font> and change the <font color=\"1E8E44\">Text Autocomplete</font> option."), "PREF"},
            new String[] { I18nMarker.marktr("You can see dialogs for questions you previously marked <font color=\"1E8E44\">Do not display this message again</font> or <font color=\"1E8E44\">Always use this answer</font> by going to {0} and checking under <font color=\"1E8E44\">Revert To Default</font> under <font color=\"1E8E44\">View</font> &gt; <font color=\"1E8E44\">Popups</font>."), "PREF"},
            new String[] { I18nMarker.marktr("You can ban certain words from appearing in your search results by going to {0} &gt; <font color=\"1E8E44\">Filters</font> &gt; <font color=\"1E8E44\">Keywords</font>, and adding new words."), "PREF"},
            new String[] { I18nMarker.marktr("Do you reach your search limit and all the results are spam or not related to your search? Use the keyword filter via {0} &gt; <font color=\"1E8E44\">Filters</font> &gt; <font color=\"1E8E44\">Keywords</font> to filter searches. Search results that contain a word from the keyword filter will not reach your computer. "), "PREF"},
            new String[] { I18nMarker.marktr("Can't connect to the Gnutella network? If you are using a firewall, make sure that a port is opened for LimeWire (both incoming and outgoing, UPD and TCP). Go to {0} &gt; <font color=\"1E8E44\">Advanced</font> &gt; <font color=\"1E8E44\">Firewall Config</font> to find the port number. "), "PREF"},
            new String[] { I18nMarker.marktr("You can save different types of files, such as audio, video or images, in different folders on your computer automatically. Go to {0} &gt; <font color=\"1E8E44\">Saving</font> to choose the location for each type of file. "), "PREF"},
            new String[] { I18nMarker.marktr("You can make LimeWire's Junk filter block more results; adjust that and more via {0} &gt; <font color=\"1E8E44\">Filters</font> &gt; <font color=\"1E8E44\">Junk</font>. "), "PREF"},
            new String[] { I18nMarker.marktr("Do you like trying out new product features as soon as they are released? Go to {0} &gt; <font color=\"1E8E44\">Updates</font> to get notified of beta releases. "), "PREF"},
        };
    }
    
    /**
     * Returns a translated String for a given entry from {@link #getCustomMessages()}.
     */
    public static String buildCustom(String[] msg) {
        if(msg[1].equals("PREF")) {
            String replace = OSUtils.isMacOSX() ?
               I18n.tr("<font color=\"1E8E44\">Preferences</font>") :
               I18n.tr("<font color=\"1E8E44\">Tools</font> &gt; <font color=\"1E8E44\">Options</font>");
            return I18n.tr(msg[0], replace);
        } else if(msg[1].equals("HELP")) {
            String replace = OSUtils.isMacOSX() ?
               I18n.tr("<font color=\"1E8E44\">LimeWire</font> &gt; <font color=\"1E8E44\">About LimeWire</font>") :
               I18n.tr("<font color=\"1E8E44\">Help</font> &gt; <font color=\"1E8E44\">About LimeWire</font>");                   
            return I18n.tr(msg[0], replace);
        } else {
            throw new RuntimeException("Invalid custom message: " + Arrays.asList(msg));
        }
        
    }

    /**
     * Returns general tips that are shown on Windows.
     */
    public static String[] getWindowsMessages() {
        return new String[] {
                I18n.tr("LimeWire automatically adds itself to the list " + 
                        "of exceptions for your Windows firewall. All " + 
                        "this is done in the background to make your " + 
                        "experience as smooth as possible."), 
                I18n.tr("You can make LimeWire look like your other Windows " + 
                        "programs via themes. Choose the Windows theme," + 
                        " available from the <font color=\"1E8E44\">" + 
                        "View</font> &gt; <font color=\"1E8E44\">Apply Skins</font> menu."),
                I18n.tr("You can play music in your default media player " + 
                        "instead of in LimeWire by right-clicking the " + 
                        "LimeWire status bar and deselecting <font " + 
                        "color=\"1E8E44\">Show Media Player</font>."),
                I18n.tr("You can find a shared file on your computer " + 
                        "by going to the <font color=\"1E8E44\">Library</font>" + 
                        " tab, highlight the file and click <font " + 
                        "color=\"1E8E44\">Explore</font>. LimeWire will open " + 
                        "the folder containing the file in the Windows Explorer."),
                I18n.tr("Be careful not to share sensitive information " + 
                        "like tax documents, passwords, etc. LimeWire " + 
                        "will warn you if you attempt to share a folder " + 
                        "that people generally use to store sensitive data" + 
                        " (such as My Documents, Desktop, Program Files, etc.)."),
                I18n.tr("The icons that you see next to your search results " + 
                        "in the '?' column are symbols of the program used to" + 
                        " open that particular type of file. To change the " + 
                        "program associated with a file, from Windows " + 
                        "Explorer, select <font color=\"1E8E44\">Tools</font>" + 
                        " &gt; <font color=\"1E8E44\">Folder Options...</font>" + 
                        " (This is a Windows setting, not a LimeWire setting)."),
                I18n.tr("The Library is a file manager. That means that when " + 
                        "you delete a file from the Library, you have the " + 
                        "option to either permanently delete the file from " + 
                        "your computer or move it to the Recycle Bin."),
                I18n.tr("You can make LimeWire your default program for " + 
                        "opening magnet links and .torrent files to get the " + 
                        "smoothest Internet experience." + 
                        "Go to <font color=\"1E8E44\">Tools</font>" + 
                        " &gt; <font color=\"1E8E44\">Options</font> &gt; " + 
                        "<font color=\"1E8E44\">Advanced</font> &gt; <font" + 
                        " color=\"1E8E44\">File Associations</font> to set" + 
                        " LimeWire as the default application to open magnet" + 
                        " links and .torrent files."),
                I18n.tr("When you close LimeWire, it minimizes to the " + 
                        "system tray. To exit, right-click the system " + 
                        "tray lime icon (next to the time), and select " + 
                        "<font color=\"1E8E44\">Exit</font>. You can " + 
                        "change this behavior via <font color=\"1E8E44\">" + 
                        "Tools</font> &gt; <font color=\"1E8E44\">Options" + 
                        "</font> &gt; <font color=\"1E8E44\">System Tray</font>."),
        };
    }

    /**
     * Returns general tips that are shown on Linux.
     */
    public static String[] getLinuxMessages() {
        return new String[] {
                I18n.tr("Are you aware of what you are sharing? Be careful " + 
                        "not to share sensitive information like tax " + 
                        "documents, passwords, etc. LimeWire will warn " + 
                        "you if you attempt to share a folder that people " + 
                        "generally use to store sensitive data (such as " + 
                        "bin, dev, home, etc.). "),
                I18n.tr("You can play music in your preferred media player" + 
                        " instead of in LimeWire by right-clicking the " + 
                        "LimeWire status bar and deselecting <font " + 
                        "color=\"1E8E44\">Show Media Player</font>. Then" + 
                        " go to <font color=\"1E8E44\">Tools</font> &gt; " + 
                        "<font color=\"1E8E44\">Options</font> &gt; <font " + 
                        "color=\"1E8E44\">Helper Apps</font> to set the " + 
                        "program of your choice."),
        };
    }

    /**
     * Returns general tips that are shown operating systems other than Windows, Mac OS X or Linux.
     */
    public static String[] getOtherMessages() {
        return new String[] {
        };
    }

    /**
     * Returns general tips that are shown for Pro.
     */
    public static String[] getProMessages() {
        return new String[] {
                I18n.tr("Thank you for supporting LimeWire! We hope " + 
                        "you enjoy LimeWire PRO. "),
        };
    }
    
    /**
     * Returns general tips that are shown for Basic.
     */
    public static String[] getBasicMessages() {
        return new String[] {
                I18n.tr("Thank you for helping the network by running LimeWire. " + 
                        "Please consider upgrading to <a href=\"{0}\">LimeWire PRO</a> " + 
                        "to help support our ongoing development to make LimeWire " + 
                        "and the network even better. ", "http://www.limewire.com/download/pro.php"),              
        };
    }

}
