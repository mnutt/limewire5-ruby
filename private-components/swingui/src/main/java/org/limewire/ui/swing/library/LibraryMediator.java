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

    private static final String LIBRARY_CARD = "LIBRARY_CARD";
    private static final String EMPTY_CARD = "EMPTY_CARD";
    private String currentCard = EMPTY_CARD;
    
    private CardLayout cardLayout;

    private JComponent libraryComponent = null;
    private JComponent sharingComponent = null;
    private JComponent emptyComponent = null;
    
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
        add(panel, LIBRARY_CARD);
        if(isLibraryCardShown()) {
            //needed because removing the card can change what card is shown
            showLibraryCard();
        }
    }
    
    protected boolean isSharingCardSet() {
        return sharingComponent != null;
    }
    
    
    protected void setEmptyCard(JComponent panel) {
        if(emptyComponent != null) {
            // Important: rm before dispose -- see note at top of class
            remove(emptyComponent);
            ((Disposable)emptyComponent).dispose();
            emptyComponent = null;
        }
        emptyComponent = panel;
        panel.validate();
        add(panel, EMPTY_CARD);
    }
    
    public void showLibraryCard() {
        currentCard = LIBRARY_CARD;
        cardLayout.show(this, LIBRARY_CARD);
        validate();
        repaint();
    }
    
    protected void showEmptyCard() {
        currentCard = EMPTY_CARD;
        cardLayout.show(this, EMPTY_CARD);
        validate();
        repaint();
    }
    
    public boolean isLibraryCardShown() {
        return LIBRARY_CARD.equals(currentCard);
    }
    
    public boolean isEmptyCardShown() {
        return EMPTY_CARD.equals(currentCard);
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
