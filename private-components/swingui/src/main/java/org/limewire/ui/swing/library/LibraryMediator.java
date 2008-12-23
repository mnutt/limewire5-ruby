package org.limewire.ui.swing.library;

import java.awt.CardLayout;
import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.limewire.ui.swing.components.Disposable;

class LibraryMediator extends JPanel implements Disposable {
    
    // Important implementation note:
    // This class disposes of Disposables whereever possible.
    // However, it is EXTREMELY IMPORTANT that the dispose is called
    // after the components are removed from the UI.
    // Otherwise, the act of removing them may require bits of the
    // disposed code, and cause exceptions.

    private static final String LIBRRY_CARD = "LIBRARY_CARD";
    private static final String SHARING_CARD = "SHARING_CARD";
    
    private CardLayout cardLayout;

    private JComponent libraryComponent = null;
    private JComponent sharingComponent = null;
    protected boolean disposed;

    public LibraryMediator() {
        cardLayout = new CardLayout();
        
        setLayout(cardLayout);
    }
    
    protected void setLibraryCard(JComponent panel) {
        if(libraryComponent != null) {
            // Important: rm before dispose -- see note at top of class
            remove(libraryComponent);
            ((Disposable)libraryComponent).dispose();
            libraryComponent = null;
        }
        libraryComponent = panel;
        add(panel, LIBRRY_CARD);
        revalidate();
    }
    
    protected boolean isSharingCardSet() {
        return sharingComponent != null;
    }
    
    protected void setSharingCard(JComponent panel) {
        if(sharingComponent != null) {
            // Important: rm before dispose -- see note at top of class
            remove(sharingComponent);
            ((Disposable)sharingComponent).dispose();
            sharingComponent = null;
        }
        sharingComponent = panel;
        panel.validate();
        add(panel, SHARING_CARD);
    }
    
    public void showLibraryCard() {
        cardLayout.show(this, LIBRRY_CARD);
        validate();
        repaint();
    }
    
    protected void showSharingCard() {
        cardLayout.show(this, SHARING_CARD);
    }


    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        assert !disposed;
        super.addImpl(comp, constraints, index);
    }

    @Override
    public void dispose() {        
        // Important: rm before dispose -- see note at top of class
        Component[] components = getComponents();
        removeAll();
        for(Component component : components) {
            ((Disposable)component).dispose();
        }
        libraryComponent = null;
        disposed = true;
    }
}
