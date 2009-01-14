package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Font;
import java.text.MessageFormat;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Singleton;

@Singleton
public class SearchHeadingDocumentBuilderImpl implements SearchHeadingDocumentBuilder {
    private final static String DOCUMENT_START = 
        "<html>" +
            "<head>" +
                "<style>" +
                    "body '{' font-family: {0};'}'" +
                    ".title '{' " +
                        "color: {1};" +
                        "font-size: {2};'}'" +
                    "a '{' color: {3};'}'" +
                "</style>" +
            "</head>" +
            "<body>";
    static final String DOCUMENT_END = "</body></html>";
    
    private @Resource Font headingFont;
    private @Resource String linkColor;
    final String documentStartHTML;
    
    public SearchHeadingDocumentBuilderImpl() {
        GuiUtils.assignResources(this);
        
        documentStartHTML = MessageFormat.format(DOCUMENT_START, headingFont.getFamily(), 
                linkColor, headingFont.getSize(), linkColor);
    }

    public String getHeadingDocument(SearchHeading heading, BasicDownloadState downloadState, boolean isSpam) {
        StringBuilder bldr = new StringBuilder();
        bldr.append(documentStartHTML);
        if (isSpam) {
            bldr.append(tr("{0} is marked as spam.", wrapHeading(heading.getText(), false)));
        } else {
            switch(downloadState) {
            case DOWNLOADING:
                String downloadMessage = "<a href=\"#downloading\">Downloading</a> {0}...";
                    bldr.append(tr(downloadMessage, wrapHeading(heading.getText(downloadMessage), false)));
                break;
            case NOT_STARTED:
                bldr.append(wrapHeading(heading.getText(), true));
                break;
            case DOWNLOADED:
            case LIBRARY:
                String message = "{0} is in <a href=\"#library\">My Library</a>.";
                    String content = tr(message, wrapHeading(heading.getText(message), false));
                bldr.append(content);
                break;
            }
        }
        bldr.append(DOCUMENT_END);
        return bldr.toString();
    }

    private String wrapHeading(String heading, boolean underline) {
        return "<span class=\"title\">" + downloadLink(heading, underline) + "</span>";
    }

    private String downloadLink(String heading, boolean underline) {
        if (underline) {
            return "<a href=\"#download\">" + heading + "</a>";
        }
        return heading;
    }
}
