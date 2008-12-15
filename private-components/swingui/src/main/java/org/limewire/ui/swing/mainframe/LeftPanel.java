package org.limewire.ui.swing.mainframe;

import java.awt.Dimension;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.VisibilityListener;
import org.limewire.ui.swing.util.VisibilityListenerList;
import org.limewire.ui.swing.util.VisibleComponent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LeftPanel extends JXPanel implements VisibleComponent {
    public static final String NAME = "Library Panel";

    private final VisibilityListenerList visibilityListenerList = new VisibilityListenerList();
    
    @Inject
    public LeftPanel(Navigator navigator, LibraryNavigator libraryNavigator) {
        GuiUtils.assignResources(this);

        setName("LeftPanel");

        setLayout(new MigLayout("insets 0, fill, gap 0"));
        setMinimumSize(new Dimension(150, 0));
        setMaximumSize(new Dimension(150, 999));
        setPreferredSize(new Dimension(150, 999));
        
        JXPanel libraryNav = libraryNavigator.getComponent();
        add(libraryNav, "grow");

        Line line = Line.createVerticalLine();
        line.setName("LeftPanel.rightBorder");
        add(line, "grow");
    }

    public void toggleVisibility() {
        setVisibility(!isVisible());
    }

    @Override
    public void addVisibilityListener(VisibilityListener listener) {
        visibilityListenerList.addVisibilityListener(listener);

    }

    @Override
    public void removeVisibilityListener(VisibilityListener listener) {
        visibilityListenerList.removeVisibilityListener(listener);
    }

    @Override
    public void setVisibility(boolean visible) {
        setVisible(visible);
        visibilityListenerList.visibilityChanged(visible);
    }
}
