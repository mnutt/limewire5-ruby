package org.limewire.ui.swing.mainframe;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.util.EnabledType;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.VisibilityType;
import org.limewire.ui.swing.util.VisibleComponent;

import com.google.inject.Inject;

public class LeftPanel extends JXPanel implements VisibleComponent {
    public static final String NAME = "Library Panel";

    private final EventListenerList<VisibilityType> visibilityListenerList =
            new EventListenerList<VisibilityType>();

    private final EventListenerList<EnabledType> enabledListenerList =
            new EventListenerList<EnabledType>();
    
    @Inject
    public LeftPanel() {
        GuiUtils.assignResources(this);

        setName("LeftPanel");

        setLayout(new MigLayout("insets 0, fill, gap 0"));
//        JXPanel libraryNav = libraryNavigator.getComponent();
//        libraryNav.setName("LeftPanel.contents");
//        add(libraryNav, "top, grow");

        Line line = Line.createVerticalLine();
        line.setName("LeftPanel.rightBorder");
        add(line, "grow");
    }

    public void toggleVisibility() {
        setVisibility(!isVisible());
    }

    @Override
    public void addVisibilityListener(EventListener<VisibilityType> listener) {
        visibilityListenerList.addListener(listener);

    }

    @Override
    public void removeVisibilityListener(EventListener<VisibilityType> listener) {
        visibilityListenerList.removeListener(listener);
    }

    @Override
    public void setVisibility(boolean visible) {
        setVisible(visible);
        visibilityListenerList.broadcast(VisibilityType.valueOf(visible));
    }

    @Override
    public void addEnabledListener(EventListener<EnabledType> listener) {
        enabledListenerList.addListener(listener);
    }

    @Override
    public void removeEnabledListener(EventListener<EnabledType> listener) {
        enabledListenerList.removeListener(listener);
    }

    /**
     * Returns true if the component is enabled for use.  Always true. 
     */
    @Override
    public boolean isActionEnabled() {
        return true;
    }

}
