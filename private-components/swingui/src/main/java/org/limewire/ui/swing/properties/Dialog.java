package org.limewire.ui.swing.properties;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.PropertiableHeadings;
import org.limewire.util.StringUtils;

public abstract class Dialog extends LimeJDialog {
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yyyy");
    private static final String AUTHOR = "Author";
    private static final String COMPANY = "Company";
    private static final String PLATFORM = "Platform";
    private static final String TRACK = "Track";
    private static final String ALBUM = "Album";
    private static final String ARTIST = "Artist";
    private static final String DESCRIPTION = "Description";
    private static final String YEAR = "Year";
    private static final String RATING = "Rating";
    private static final String GENRE = "Genre";
    private static final String TITLE = "Title";
    private static final String HASH = "Hash";
    protected final JLabel icon = new JLabel();
    protected final JLabel heading = newLabel();
    protected final JLabel filename = newLabel();
    protected final JLabel fileSize = new JLabel();
    protected final JLabel metadata = newLabel();
    protected final JTextField title = new JTextField();
    protected final JComboBox genre = new JComboBox();
    protected final JTextField unEditableGenre = new JTextField();
    protected final JComboBox rating = new JComboBox();
    protected final JTextField year = new JTextField();
    protected final JTextArea description = newTextArea();
    private final JScrollPane descriptionScrollPane = new JScrollPane(description, 
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    protected final JTextField artist = new JTextField();
    protected final JTextField album = new JTextField();
    protected final JTextField track = new JTextField();
    protected final JTextField author = new JTextField();
    protected final JComboBox platform = new JComboBox();
    protected final JTextField company = new JTextField();
    protected final DefaultTableModel readOnlyInfoModel = new ReadOnlyTableModel();
    protected final JLabel fileLocation = newLabel();
    protected final HyperlinkButton locateOnDisk;
    protected final HyperlinkButton locateInLibrary;
    protected final HyperlinkButton copyToClipboard;
    protected final HyperlinkButton moreFileInfo;
    protected final JTable readOnlyInfo = new JTable(readOnlyInfoModel);
    private final Font smallFont;
    private final Font mediumFont;
    private final Font largeFont;
    
    private final JPanel overview;
    private final JPanel details = newPanel();
    protected final JPanel location = newPanel();
    protected final JPanel mainPanel;
    protected final IconManager iconManager;
    private final PropertiableHeadings propertiableHeadings;
    
    public Dialog(DialogParam param) {
        this.iconManager = param.getIconManager();
        this.propertiableHeadings = param.getPropertiableHeadings();
        
        this.smallFont = param.getSmallFont();
        this.mediumFont = param.getMediumFont();
        this.largeFont = param.getLargeFont();
        
        mainPanel = new JPanel(new MigLayout("insets 0 3 3 0", "[fill]push[]", "[][][][]push[]"));
        locateOnDisk = newHyperlink();
        locateInLibrary = newHyperlink();
        copyToClipboard = newHyperlink();
        moreFileInfo = newHyperlink();
        
        add(mainPanel);
        mainPanel.setBackground(param.getBackgroundColor());
        
        setFont(mediumFont, heading, filename, fileSize);
        setFont(smallFont, metadata, copyToClipboard, moreFileInfo, locateOnDisk, locateInLibrary,
                title, genre, unEditableGenre, rating, year, description, artist, album, track, author, platform,
                company, fileLocation);
        //Use the same border that a textfield uses - JTextAreas by default are not given a border
        //This makes the look consistent with JTextField
        descriptionScrollPane.setBorder(artist.getBorder());
        description.setRows(3);
        
        JPanel buttons = newPanel(new MigLayout("", "push[][]", "[]"));
        buttons.add(new JButton(new OKAction()));
        buttons.add(new JButton(new CancelAction()));
        mainPanel.add(buttons, "alignx right, cell 1 4");
        
        overview = newPanel(new MigLayout("fillx", "[][]push[]", "[top]3[top]"));

        JPanel linksPanel = new JPanel(new BorderLayout());
        linksPanel.setOpaque(false);
        linksPanel.add(copyToClipboard, BorderLayout.NORTH);
        linksPanel.add(moreFileInfo, BorderLayout.SOUTH);
        
        overview.add(icon, "spany");
        overview.add(heading, "grow");
        overview.add(linksPanel, "spany, wrap");
        overview.add(metadata, "cell 1 1");

        addOverview();
        
        unEditableGenre.setVisible(false);
    }
    
    private HyperlinkButton newHyperlink() {
        HyperlinkButton link = new HyperlinkButton();
        return link;
    }

    protected JPanel newPanel(LayoutManager manager) {
        JPanel panel = newPanel();
        panel.setLayout(manager);
        return panel;
    }

    protected JPanel newPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }
    
    protected void setFont(Font font, JComponent... components) {
        if (font != null) {
            for (JComponent comp : components) {
                comp.setFont(font);
            }
        }
    }

    private JLabel newLabel() {
        //Creating a JLabel that wraps all text in HTML so that multiline word
        //wrapping will behave correctly.  The JXLabel.setMultiline() causes the
        //labels to wrap correctly, however, they don't seem to report their preferred
        //sizes correctly, so if those labels appear in a composite panel, their
        //enclosing panel won't ask for as much space as the label is trying to 
        //take up, so other components in the same panel (vertically), get truncated
        //This change is for LWC-2147
        return new JLabel() {
            @Override
            public void setText(String text) {
                if (!text.toLowerCase().startsWith("<html>")) {
                    text = "<html>" + text + "</html>";
                }
                super.setText(text);
            }
        };
    }

    private JTextArea newTextArea() {
        JTextArea area = new JTextArea();
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    protected void showDialog(String fileName, Category category) {
        setTitle(tr("\"{0}\" properties", fileName));
        
        switch(category) {
        case VIDEO:
            configureVideoDetailsLayout();
            break;
        case AUDIO:
            configureAudioDetailsLayout();
            break;
        case DOCUMENT:
            configureDocumentDetailsLayout();
            break;
        case IMAGE:
            configureImageDetailsLayout();
            break;
        case PROGRAM:
            configureProgramDetailsLayout();
            break;
        case OTHER:
            //no-op: Does not display details
        }
        
        addLocation();
        
        setPreferredSize(new Dimension(620,650));
        setModalityType(ModalityType.APPLICATION_MODAL);
        
        setDefaultCloseOperation(2);

        pack();

        setLocationRelativeTo(GuiUtils.getMainFrame());
        
        setVisible(true);
    }

    private void addOverview() {
        mainPanel.add(box(tr("Overview"), overview), "cell 0 0, spanx 2");
    }

    protected Component box(String string, JComponent component) {
        return box(string, null, component);
    }

    protected Component box(String string, JComponent bannerComponent, JComponent bodyComponent) {
        
        JPanel panel = new JPanel(new MigLayout("insets 3 3 3 3, fillx", "[fill]push[]", "[][][]"));
        panel.setOpaque(false);
        JLabel label = new JLabel(string);
        label.setFont(largeFont);
        panel.add(label, bannerComponent == null ? "wrap" : "");
        if (bannerComponent != null) {
            panel.add(bannerComponent, "wrap");
        }
        panel.add(Line.createHorizontalLine(), "span, wrap");
        panel.add(bodyComponent, "span");
        return panel;
    }

    private void addDetails() {
        mainPanel.add(box(tr("Details"), details), "cell 0 1, spanx 2");
    }

    private void addLocation() {
        mainPanel.add(box(tr("Location"), location), "cell 0 2, spanx 2");
    }

    private class OKAction extends AbstractAction {
        public OKAction() {
            super(tr("OK"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setVisible(false);
            commit();
        }
    }

    private class CancelAction extends AbstractAction {
        public CancelAction() {
            super(tr("Cancel"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
    }

    protected abstract void commit();
    
    protected void addLengthMetadata(String length, Map<String, String> metadata) {
        addMetadata(tr("Length"), length, metadata);
    }
    
    protected void addFileSizeMetadata(String fileSize, Map<String, String> metadata) {
       addMetadata(tr("Size"), fileSize, metadata); 
    }
    
    protected void addHashMetadata(URN hash, Map<String, String> metadata) {
        addMetadata(HASH, (hash != null) ? hash.toString() : null, metadata);
    }

    protected void addDateCreatedMetadata(String dateCreated, Map<String, String> metadata) {
        addMetadata(tr("Date Created"), dateCreated, metadata);
    }
    
    protected void addTypeMetadata(String str, Map<String, String> metadata) {
        addMetadata(tr("Type"), str, metadata);
    }

    protected void addBitrateMetadata(String bitRate, Map<String, String> metadata) {
        addMetadata(tr("Bitrate"), bitRate, metadata);
    }

    protected void addDimensionMetadata(String width, Object height, Map<String, String> metadata) {
        if (height != null && width != null) {
            addMetadata(tr("Dimensions"), tr("({0}px X {1}px)", width, height), metadata);
        }
    }

    private void addMetadata(String label, String value, Map<String, String> metadata) {
        if (!StringUtils.isEmpty(value)) {
            metadata.put(label, value);
        }
    }

    protected void setMetadataText(Map<String, String> metadata) {
        StringBuilder bldr = new StringBuilder().append("<html>");
        for(String key : metadata.keySet()) {
            bldr.append("<b>").append(key).append("</b>:&nbsp;").append(metadata.get(key)).append("<br/>");
        }
        bldr.append("</html>");
        this.metadata.setText(bldr.toString());
    }
    
    private JLabel newSmallLabel(String text) {
        JLabel label = new JLabel(tr(text));
        label.setFont(smallFont);
        return label;
    }

    private void configureVideoDetailsLayout() {
        details.setLayout(new MigLayout("fillx, nocache", "[20%!][20%!][]", "[][][][][][]"));
        details.add(newSmallLabel(TITLE), "wrap");
        details.add(title, "span, growx, wrap");
        details.add(newSmallLabel(GENRE));
        details.add(newSmallLabel(RATING));
        details.add(newSmallLabel(YEAR), "wrap");
        details.add(genre, "wmin 90, hidemode 3");
        details.add(unEditableGenre, "wmin 90, hidemode 3");
        details.add(rating, "wmin 90");
        details.add(year, "growx, wrap");
        details.add(newSmallLabel(DESCRIPTION), "wrap");
        details.add(descriptionScrollPane, "hmin pref, grow, span");
        
        addDetails();
    }
    
    private void configureAudioDetailsLayout() {
        details.setLayout(new MigLayout("fillx, nocache", "[50%!][20%!]0[10%][10%]", "[][][][][][]"));
        details.add(newSmallLabel(TITLE));
        details.add(newSmallLabel(ARTIST), "wrap");
        details.add(title, "growx");
        details.add(artist, "span, growx, wrap");
        details.add(newSmallLabel(ALBUM));
        details.add(newSmallLabel(GENRE));
        details.add(newSmallLabel(YEAR));
        details.add(newSmallLabel(TRACK), "wrap");
        details.add(album, "growx");
        details.add(genre, "wmin 90, hidemode 3");
        details.add(unEditableGenre, "wmin 90, hidemode 3");
        details.add(year, "wmin 90");
        details.add(track, "wrap, wmin 40");
        details.add(newSmallLabel(DESCRIPTION), "wrap");
        details.add(descriptionScrollPane, "hmin pref, grow, span");
        
        addDetails();
    }
    
    private void configureImageDetailsLayout() {
        details.setLayout(new MigLayout("fillx", "[]", "[][][][]"));
        details.add(newSmallLabel(TITLE), "wrap");
        details.add(title, "growx, wrap");
        details.add(newSmallLabel(DESCRIPTION), "wrap");
        details.add(descriptionScrollPane, "hmin pref, grow");
        
        addDetails();
    }
    
    private void configureProgramDetailsLayout() {
        details.setLayout(new MigLayout("fillx, nocache", "[30%!][]", "[][][][][][]"));
        details.add(newSmallLabel(TITLE), "wrap");
        details.add(title, "span, growx, wrap");
        details.add(newSmallLabel(PLATFORM));
        details.add(newSmallLabel(COMPANY), "wrap");
        details.add(platform, "wmin 90");
        details.add(company, "growx, wrap");
        
        addDetails();
    }
    
    private void configureDocumentDetailsLayout() {
        details.setLayout(new MigLayout("fillx", "[]", "[][][][30]"));
        details.add(newSmallLabel(AUTHOR), "wrap");
        details.add(author, "growx, wrap");
        details.add(newSmallLabel(DESCRIPTION), "wrap");
        details.add(descriptionScrollPane, "hmin pref, grow");
        
        addDetails();
    }

    protected void populateMetadata(PropertiableFile propFile) {
        Map<String, String> metadata = new LinkedHashMap<String, String>();
        addFileSizeMetadata(propertiableHeadings.getFileSize(propFile), metadata);
        
        switch(propFile.getCategory()) {
            case AUDIO:
                addLengthMetadata(propertiableHeadings.getLength(propFile), metadata);
                String bitRate = str(propFile.getProperty(FilePropertyKey.BITRATE));
                if (bitRate != null) {
                    addBitrateMetadata(bitRate + " (" + propertiableHeadings.getQualityScore(propFile) + ")", metadata);
                }
                break;
            case VIDEO:
                addDimensionMetadata(str(propFile.getProperty(FilePropertyKey.WIDTH)), str(propFile.getProperty(FilePropertyKey.HEIGHT)), metadata);
                break;
            case IMAGE:
                addDateCreatedMetadata(convertDate(propFile), metadata);
                break;
            case DOCUMENT:
                addTypeMetadata(iconManager.getMIMEDescription(propFile), metadata);
                addDateCreatedMetadata(convertDate(propFile), metadata);
                //TODO: parse TOPIC property
                break;
            case OTHER:
                addTypeMetadata(iconManager.getMIMEDescription(propFile), metadata);
        }
        addHashMetadata(propFile.getUrn(), metadata);
        setMetadataText(metadata);
    }
    
    protected String convertDate(PropertiableFile propertiable) {
        Object time = propertiable.getProperty(FilePropertyKey.DATE_CREATED);
        if (time != null  && time instanceof Long) {
            return DATE_FORMAT.format(new java.util.Date((Long) time));
        }
        return "";
    }

    protected String str(Object property) {
        return property == null ? null : property.toString();
    }

    private void disableEdit(JTextComponent... comps) {
        for (JTextComponent comp : comps) {
            comp.setEditable(false);
        }
    }

    private void disableEdit(JComboBox... comps) {
        for (JComboBox comp : comps) {
            comp.setEditable(false);
        }
    }

    protected void disableEditForAllCommonFields() {
        genre.setVisible(false);
        unEditableGenre.setVisible(true);
        disableEdit(album, author, artist, company, year, title, track, description, unEditableGenre);
        disableEdit(rating, platform);
        
        setColors(album.getForeground(), album.getBackground(), description, unEditableGenre, rating, platform);
    }

    protected void setColors(Color foreground, Color background, JComponent... comps) {
        for(JComponent comp : comps) {
            comp.setForeground(foreground);
            comp.setBackground(background);
        }
    }

    private static class ReadOnlyTableModel extends DefaultTableModel {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}
