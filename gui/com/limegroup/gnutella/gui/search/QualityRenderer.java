package com.limegroup.gnutella.gui.search;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeObserver;

/**
 * This class handles rendering the "Quality" column in the search results.
 * It uses different labels depending on how many stars should be displayed.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
final class QualityRenderer implements TableCellRenderer, ThemeObserver {

	/**
	 * The default border to use -- used when the label does not have focus.
	 */
    private static Border _noFocusBorder = new EmptyBorder(1, 1, 1, 1); 

	/**
	 * Holder for the foreground color to use when a label is not selected.
	 */
    private Color _unselectedForeground; 

	/**
	 * Holder for the background color to use when a label is not selected.
	 */
    private Color _unselectedBackground; 

	/**
	 * <tt>JLabel</tt> instance for rendering one star.
	 */
	private final JLabel STAR_ONE = new JLabel();

	/**
	 * <tt>JLabel</tt> instance for rendering two stars.
	 */
	private final JLabel STAR_TWO = new JLabel();

	/**
	 * <tt>JLabel</tt> instance for rendering three stars.
	 */
	private final JLabel STAR_THREE = new JLabel();

	/**
	 * <tt>JLabel</tt> instance for rendering four stars.
	 */
	private final JLabel STAR_FOUR = new JLabel();
	
	/**
	 * <tt>JLabel</tt> instance for rendering five stars.
	 */
	private final JLabel STAR_FIVE = new JLabel();
	
	/**
     * <tt>JLabel</tt> instance for rendering special
     */
    private final JLabel STORE_SONG = new JLabel();
	
	/**
	 * <tt>JLabel</tt> instance for rendering a saved file.
	 */
	private final JLabel SAVED_FILE = new JLabel();
	
	/**
	 * <tt>JLabel</tt> instance for rendering a downloading file.
	 */
	private final JLabel DOWNLOADING_FILE = new JLabel();

	/**
	 * <tt>JLabel</tt> instance for rendering an incomplete file.
	 */
	private final JLabel INCOMPLETE_FILE = new JLabel();
	
    /**
     * <tt>JLabel</tt> instance for rendering a spam file.
     */
    private final JLabel SPAM_FILE = new JLabel();
    
    /** JLabel for rendering a secure result. */
    private final JLabel SECURE_FILE = new JLabel();
    
    /**
     * 'Quality' for spam file results.
     */
    static final int SPAM_FILE_QUALITY = 1003;
    
	/**
	 * 'Quality' for saved file results.
	 */
	static final int SAVED_FILE_QUALITY = 1002;
	
	/**
	 * 'Quality' for downloading file results.
	 */
	static final int DOWNLOADING_FILE_QUALITY = 1001;
	
	/**
	 * 'Quality' for files that are incomplete (but not downloading)
	 */
	static final int INCOMPLETE_FILE_QUALITY = 1000;
    
    /** 'Quality' for files that are considered secure results. */
    static final int SECURE_QUALITY = 999;
    
    /**
     * Quality for special results.
     */
    static final int THIRD_PARTY_RESULT_QUALITY = 5;
    
    /**
     * Number of stars ("quality") for multicast results.
     */
    static final int MULTICAST_QUALITY = 4;
    
    /**
     * Number of stars ("quality") for results from non-firewalled hosts with
     * free upload slots.
     */
    static final int EXCELLENT_QUALITY = 3;
    
    /**
     * Number of stars ("quality") for results that have a good chance of 
     * success.
     */
    static final int GOOD_QUALITY = 2;
    
    /**
     * Number of stars ("quality") for results that have a fair chance of 
     * success.
     */
    static final int FAIR_QUALITY = 1;
    
    /**
     * Number of stars ("quality") for results that have a poor chance of
     * success.
     */
    static final int POOR_QUALITY = 0;
    
	/**
	 * Makes all of the star labels opaque and sets their borders.
	 */
	QualityRenderer() {
	    fix(STAR_ONE);
	    fix(STAR_TWO);
	    fix(STAR_THREE);
	    fix(STAR_FOUR);
	    fix(STAR_FIVE);
        fix(STORE_SONG);
	    fix(SAVED_FILE);
	    fix(DOWNLOADING_FILE);
	    fix(INCOMPLETE_FILE);
        fix(SPAM_FILE);
        fix(SECURE_FILE);
        
		updateTheme();
		ThemeMediator.addThemeObserver(this);
	}
	
	private void fix(JLabel label) {
	    label.setBorder(_noFocusBorder);
	    label.setOpaque(true);
	    label.setHorizontalAlignment(SwingConstants.CENTER);
    }

	public void updateTheme() {
		STAR_ONE.setIcon(GUIMediator.getThemeImage("01_star"));
		STAR_TWO.setIcon(GUIMediator.getThemeImage("02_star"));
		STAR_THREE.setIcon(GUIMediator.getThemeImage("03_star"));
		STAR_FOUR.setIcon(GUIMediator.getThemeImage("04_star"));
		STAR_FIVE.setIcon(GUIMediator.getThemeImage("05_star"));
		SAVED_FILE.setIcon(GUIMediator.getThemeImage("complete"));
		DOWNLOADING_FILE.setIcon(GUIMediator.getThemeImage("downloading"));
		INCOMPLETE_FILE.setIcon(GUIMediator.getThemeImage("incomplete"));
        SPAM_FILE.setIcon(GUIMediator.getThemeImage("spam_mini"));
        // that's: lime hi res, not lime hires. :)
        SECURE_FILE.setIcon(GUIMediator.getThemeImage("limehires"));
        STORE_SONG.setIcon(GUIMediator.getThemeImage("limehires"));
	}

	/**
	 * Returns the <tt>Component</tt> that displays the stars based
	 * on the number of stars in the <tt>QualityHolder</tt> object.
	 */
	public Component getTableCellRendererComponent
		(JTable table,Object value,boolean isSelected,
		 boolean hasFocus,int row,int column) {
		    
        // Since "value" can be null, make sure we handle that case by simply
        // setting the quality to poor.
		int numStars = value == null ? POOR_QUALITY : 
		    ((Integer)value).intValue();
		
		JLabel curLabel;
		
		switch(numStars) {
        case SECURE_QUALITY:
            curLabel = SECURE_FILE; break;
        case SPAM_FILE_QUALITY:
            curLabel = SPAM_FILE; break;
        case SAVED_FILE_QUALITY:
            curLabel = SAVED_FILE; break;
        case DOWNLOADING_FILE_QUALITY:
            curLabel = DOWNLOADING_FILE; break;
        case INCOMPLETE_FILE_QUALITY:
            curLabel = INCOMPLETE_FILE; break;
        case MULTICAST_QUALITY:
            curLabel = STAR_FIVE; break;
        case THIRD_PARTY_RESULT_QUALITY:
            curLabel = STORE_SONG; break;            
        case EXCELLENT_QUALITY:
            curLabel = STAR_FOUR; break;
        case GOOD_QUALITY:
            curLabel = STAR_THREE; break;
        case FAIR_QUALITY:
            curLabel = STAR_TWO; break;
        default:
            curLabel = STAR_ONE;
        }
        
		if (isSelected) {
			curLabel.setForeground(table.getSelectionForeground());
			curLabel.setBackground(table.getSelectionBackground());
		}
		else {
			curLabel.setForeground((_unselectedForeground != null) ? 
									_unselectedForeground 
									: table.getForeground());
			curLabel.setBackground((_unselectedBackground != null) ? 
									_unselectedBackground 
									: table.getBackground());
		}

		return curLabel;		
	}

}
