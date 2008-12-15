package com.limegroup.gnutella.gui.tables; 

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeObserver;


/**
 * Draws a cell with it's default renderer, but the foreground
 * colored differently.
 *
 * Due to the nature of renderer components being shared
 * between cells, this can not act directly on the renderer
 * that's returned.  Otherwise, this has the side effect of altering
 * future renderings that aren't necesarily from a ColoredCell.
 * To still allow complete functionality of the
 * ColorRenderer to work regardless of the cell's specific renderer,
 * this will instantiate a copy of that renderer and cache it for future
 * use.
 *
 *******************************************************************
 * This requires that any cell that is being colored have a default
 * TableCellRenderer that has a parameterless constructor.
 *******************************************************************
 *
 * New renderers are created flyweight-style.
 *
 * All useful calls are wrapped and redirected to the underlying renderer.
 *
 * This class takes advantage of the potential of the 'renderer' component
 * to not be the TableCellRenderer itself.  It is merely by convention
 * that all getTableCellRendererComponent calls return 'this'.
 * However, there is nothing that specifically states the TableCellRenderer
 * does not delegate to other renderers.  Unfortunately, the JTable
 * sends UI updates to the TableCellRenderer, and not the component that
 * does the renderering.  TableCellRenderer is actually a rather poor name,
 * or more precisely, the function 'getTableCellRendererComponent' is a
 * poor name for being in an interface titled TableCellRenderer.
 * The delegate functionality is useful, but so is the rubber-stamping.
 * It would probably have been better to name getTableCel.. 'stamp'.
 *
 * This class does *NOT* instantiate new copies of the component returned
 * from getTableCellRendererComponent.  It needs new copies of the
 * TableCellRenderer, in order to call further getTableCellRendererComponents.
 * Since the two are generally the same, it works out nicely.
 *
 * UI Updates are propagated to all contained TableCellRenderers.
 *
 * For clarity, this class just extends JComponent (needed to recieve
 * an updateUI call).  It is not really a component -- it just delegates.
 *
 * NOTE: This does not color selected or focused cells.
 */
class ColorRenderer extends JComponent
  implements TableCellRenderer, ThemeObserver {
    
    /**
     * Map is from TableCellRenderer to TableCellRenderer.
     * Every instance of a renderer will have a mirrored instance as its value.
     */
    private Map<TableCellRenderer, TableCellRenderer> otherRenderers =
        new HashMap<TableCellRenderer, TableCellRenderer>();
    
    public ColorRenderer() {
        ThemeMediator.addThemeObserver(this);
    }
    
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value, 
                                                   boolean isSel, 
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {        
        ColoredCell cc=(ColoredCell)value;
        Color clr;
        Object val;
        Class<?> clazz;
        if(cc != null) {
            clr = cc.getColor();
            val = cc.getValue();
            clazz = cc.getCellClass();
        } else {
            clr = null;
            val = "";
            clazz = String.class;
        }
        
        TableCellRenderer tcr = table.getDefaultRenderer( clazz );
        tcr = getCachedOrNewRenderer( tcr );
        
        Component renderer = tcr.getTableCellRendererComponent(
                table, val, isSel, hasFocus, row, column);
        
        if ((!isSel && !hasFocus)) // || isReadable(clr, renderer.getBackground()))
            renderer.setForeground(clr);

        return renderer;
    }    
    
    public void updateTheme() {
        for( Iterator<?> i = otherRenderers.values().iterator(); i.hasNext(); ) {
            Object o = i.next();
            if ( o instanceof ThemeObserver )  {
                ((ThemeObserver)o).updateTheme();
            }
        }
    }
   
    @Override
    public void updateUI() {
        for( Iterator<?> i = otherRenderers.values().iterator(); i.hasNext(); ) {
            Object o = i.next();
            if ( o instanceof JComponent )  {
                ((JComponent)o).updateUI();
            }
        }
    }
    
    
    private TableCellRenderer getCachedOrNewRenderer( TableCellRenderer tcr ) {
        TableCellRenderer renderer = otherRenderers.get(tcr);
        
        // if it doesn't exist, put a copy of the renderer in there
        // so that the setForeground doesn't effect the real renderer.
        if ( renderer == null ) {
            Class<? extends TableCellRenderer> rendererClass = tcr.getClass();
            try {
                renderer = rendererClass.newInstance();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (ClassCastException e) {
                throw new RuntimeException(e);
            }
            otherRenderers.put( tcr, renderer );
        }
        return renderer;
    }
}
