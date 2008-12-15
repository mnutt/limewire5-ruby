package com.limegroup.gnutella.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import org.limewire.service.Switch;
import org.limewire.setting.IntSetting;


/**
 * This class handles displaying messages to the user.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class MessageService {
	/**
	 * Constant for when the 'Always use this answer' checkbox wants to 
	 * remember the answer.
	 */
	public static final int REMEMBER_ANSWER = 1;
	
	/**
	 * Constant for when the 'Always use this answer' checkbox does not
	 * want to remember the answer.
	 */
	public static final int FORGET_ANSWER = 0;
    
    /**
     * A Map containing disposable messages
     */
    private final Map<String, JDialog> _disposableMessageMap = new HashMap<String, JDialog>();

	/**
	 * <tt>MessageService</tt> instance, following singleton.
	 */
	private static final MessageService INSTANCE = new MessageService();
	
	/** The currently visible frame. */
	private static Component currentVisibleFrame;

	/**
	 * Instance accessor for the <tt>MessageService</tt>.
	 */
	public static MessageService instance() {
		return INSTANCE;
	}

	/**
	 * Initializes all of the necessary messaging components.
	 */
	MessageService() {
		GUIMediator.setSplashScreenString(
	        I18n.tr("Loading Messages..."));
	}


	/**
	 * Display a standardly formatted error message with
	 * the specified String. 
	 *
	 * @param message the message to display to the user
	 */
	final void showError(String message) {
    	FocusJOptionPane.showMessageDialog(getParentComponent(), 
                  getLabel(message),
				  I18n.tr("Error"),
				  JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * Display a standardly formatted error message with
	 * the specified String. 
	 *
	 * @param message the message to display to the user
	 * @param ignore the Boolean setting to store/retrieve whether or not to
	 *  ignore this message in the future.	 
	 */
	final void showError(String message, Switch ignore) {
	    if ( !ignore.getValue() ) {
		    FocusJOptionPane.showMessageDialog(getParentComponent(), 
                      doNotDisplayAgainLabel(message, ignore),
					  I18n.tr("Error"),
					  JOptionPane.ERROR_MESSAGE);
        }
	}
	
	/**
	 * Display a standardly formatted warning message with
	 * the specified String. 
	 *
	 * @param message the message to display to the user
	 * @param ignore the Boolean setting to store/retrieve whether or not to
	 *  ignore this message in the future.	 
	 */
	final void showWarning(String message, Switch ignore) {
	    if ( !ignore.getValue() ) {
		    FocusJOptionPane.showMessageDialog(getParentComponent(), 
                      doNotDisplayAgainLabel(message, ignore),
					  I18n.tr("Warning"),
					  JOptionPane.WARNING_MESSAGE);
        }
	}

	/**
	 * Display a standardly formatted warning message with
	 * the specified String. 
	 *
	 * @param message the message to display to the user
	 */
	final void showWarning(String message) {
		FocusJOptionPane.showMessageDialog(getParentComponent(), 
                                      getLabel(message),
									  I18n.tr
									  ("Warning"),
									  JOptionPane.WARNING_MESSAGE);
	}
	
	
	/**
	 * Displays a standardly formatted information message with
	 * the specified Component.
	 *
	 * @param toDisplay the object to display in the message
	 */
	public final void showMessage(Component toDisplay) {
		FocusJOptionPane.showMessageDialog(getParentComponent(), 
				  toDisplay,
				  I18n.tr("Message"),
				  JOptionPane.INFORMATION_MESSAGE);	
    }

	/**
	 * Display a standardly formatted information message with
	 * the specified String. 
	 *
	 * @param message the message to display to the user
	 */	
	final void showMessage(String message) {
		FocusJOptionPane.showMessageDialog(getParentComponent(),
                  getLabel(message),
				  I18n.tr("Message"),
				  JOptionPane.INFORMATION_MESSAGE);
	}
	
	/**
	 * Display a standardly formatted information message with
	 * the specified String.  Store whether or not to display message
	 * again in the BooleanSetting ignore.
	 *
	 * @param message the message to display to the user
	 * @param ignore the Boolean setting to store/retrieve whether or not to
	 *  ignore this message in the future.
	 */	
	final void showMessage(String message, Switch ignore) {
	    if ( !ignore.getValue() ) {
    		FocusJOptionPane.showMessageDialog(getParentComponent(),
                      doNotDisplayAgainLabel(message, ignore),
					  I18n.tr("Message"),
					  JOptionPane.INFORMATION_MESSAGE);
        }
	}	

    /**
     * Display a disposable message with the specified String.
     *
     * @param dialogKey the key to use for access to showing/hiding this dialog
     * @param message The message to display int he dialog
     * @param msgType The <tt>JOptionPane</tt> message type. @see javax.swing.JOptionPane.
     *        May be one of ERROR_MESSAGE, WARNING_MESSAGE, INFORMATION_MESSAGE,
     *        or PLAIN_MESSAGE.
     */ 
    final void showDisposableMessage(String dialogKey, String message, int msgType) {
        showDisposableMessage(dialogKey, message, null, msgType);
    }
    
    /**
     * Display a disposable message with
     * the specified String.  Store whether or not to display message
     * again in the BooleanSetting ignore.
     *
     * @param dialogKey the key to use for access to showing/hiding this dialog
     * @param message The message to display int he dialog
     * @param ignore the Boolean setting to store/retrieve whether or not to
     *  ignore this message in the future.
     * @param msgType The <tt>JOptionPane</tt> message type. @see javax.swing.JOptionPane.
     *        May be one of ERROR_MESSAGE, WARNING_MESSAGE, INFORMATION_MESSAGE,
     *        or PLAIN_MESSAGE.
     */ 
    final void showDisposableMessage(
            String dialogKey,
            String message,
            Switch ignore,
            int msgType) {
        
        String title;
        switch(msgType) {
        case JOptionPane.ERROR_MESSAGE:
            title = I18n.tr("Error");
            break;
        case JOptionPane.WARNING_MESSAGE:
            title = I18n.tr("Warning");
            break;
        case JOptionPane.INFORMATION_MESSAGE:
        case JOptionPane.PLAIN_MESSAGE:
            title = I18n.tr("Message");
            break;
        default:
            throw new IllegalArgumentException("Unsupported Message Type: " + msgType);
        }
        
        if(ignore==null || !ignore.getValue()) {
            if(_disposableMessageMap.containsKey(dialogKey)) {
                JDialog dialog = _disposableMessageMap.get(dialogKey);
                dialog.toFront();
                dialog.setVisible(true);
            } else {
                Object component = message;
                if(ignore != null)
                    component = doNotDisplayAgainLabel(message, ignore);
                JOptionPane pane = new JOptionPane(component, msgType);
                boolean dispose = false;
                Component parentComponent = getParentComponent();
                if(parentComponent == null) {
                    dispose = true;
                    parentComponent = FocusJOptionPane.createFocusComponent();
                }
                JDialog dialog = pane.createDialog(parentComponent, title);
                dialog.setModal(true);
                _disposableMessageMap.put(dialogKey,dialog);
                dialog.setVisible(true);
                // dialog has been disposed by user OR by core
                _disposableMessageMap.remove(dialogKey);    
                if(dispose == true)
                    ((JFrame)parentComponent).dispose();
            }
        }
    };
    
    
    /**
     * Hides the disposable message specified by the dialogKey.
     */
    final void hideDisposableMessage(String dialogKey) {
        JDialog dialog = _disposableMessageMap.get(dialogKey);
        if(dialog != null) {
            dialog.setVisible(false);
            dialog.dispose();
        }
    }

	/**
	 * Display a standardly formatted confirmation message with
	 * the specified String. 
	 *
	 * @param message the message to display to the user
	 */	
	final void showConfirmMessage(String message) {
		FocusJOptionPane.showConfirmDialog(getParentComponent(), 
                      getLabel(message),
					  I18n.tr("Message"),
					  JOptionPane.INFORMATION_MESSAGE);
	}
	
	/**
	 * Display a standardly formatted confirmation message with
	 * the specified String.  Store whether or not to display
	 * the message again in the BooleanSetting ignore.
	 *
	 * @param message the message to display to the user
	 * @param ignore the Boolean setting to store/retrieve whether or not to
	 *  ignore this message in the future.	 
	 */	
	final void showConfirmMessage(String message, Switch ignore) {
	    if ( !ignore.getValue() ) {
    		FocusJOptionPane.showConfirmDialog(getParentComponent(), 
						  doNotDisplayAgainLabel(message, ignore),
						  I18n.tr("Message"),
						  JOptionPane.INFORMATION_MESSAGE);
        }
	}	

    
    final DialogOption showYesNoMessage(String message, DialogOption defaultOption) {
        return showYesNoMessage(message,I18n.tr("Message"), defaultOption);
    }
    
	/**
	 * Displays a message to the user and returns 
	 * MessageService.YES_OPTION if the user selects yes and
	 * MessageService.NO_OPTION if the user selects no.
	 *
	 * @param message the message to display to the user
	 */ 
	final DialogOption showYesNoMessage(String message) {
       return showYesNoMessage(message,I18n.tr("Message"));
	}

    /**
     * Displays a message and a list underneath to the user passing on the
     * return value from 
     * {@link JOptionPane#showConfirmDialog(Component, Object, String, int)}.
     *
     * @param message the message to display to the user
     * @param listModel the array of object to be displayed in the list
     * @param messageType either {@link JOptionPane#YES_NO_OPTION}, 
     * {@link JOptionPane#YES_NO_CANCEL_OPTION} or {@link JOptionPane#OK_CANCEL_OPTION}.
     * @param listRenderer an optional list cell renderer, can be <code>null</code>
     */
    final int showConfirmListMessage(String message, Object[] listModel, int messageType,
            ListCellRenderer listRenderer) {
        return showConfirmListMessage(message, listModel, messageType, listRenderer,
                I18n.tr("Message"));
    }

    /**
     * Displays a message and a list underneath to the user passing on the
     * return value from 
     * {@link JOptionPane#showConfirmDialog(Component, Object, String, int)}.
     *
     * @param message the message to display to the user
     * @param listModel the array of object to be displayed in the list
     * @param messageType either {@link JOptionPane#YES_NO_OPTION}, 
     * {@link JOptionPane#YES_NO_CANCEL_OPTION} or {@link JOptionPane#OK_CANCEL_OPTION}.
     * @param listRenderer an optional list cell renderer, can be <code>null</code>
     * @param the title shown in the dialog window bar
     */
    final int showConfirmListMessage(String message, Object[] listModel,
            int messageType, ListCellRenderer listRenderer, String title) {
            JList list = new JList(listModel);
            list.setVisibleRowCount(5);
            list.setSelectionForeground(list.getForeground());
            list.setSelectionBackground(list.getBackground());
            list.setFocusable(false);
            if (listRenderer != null) {
                list.setCellRenderer(listRenderer);
            }
            Object[] content = new Object[] {
                    new MultiLineLabel(message, 400),
                    Box.createVerticalStrut(ButtonRow.BUTTON_SEP),
                    new JScrollPane(list)
            };
            return FocusJOptionPane.showConfirmDialog(getParentComponent(), 
                    content, title, messageType);
    }

    final DialogOption showYesNoMessage(String message, String title) {
        return showYesNoMessage(message, title, DialogOption.YES);
    }
    
    /**
     * Displays a message to the user and returns 
     * MessageService.YES_OPTION if the user selects yes and
     * MessageService.NO_OPTION if the user selects no.
     *
     * @param message the message to display to the user
     * @title the title on the dialog
     */ 
    final DialogOption showYesNoMessage(String message, String title, DialogOption defaultOption) {
       final String[] options = {DialogOption.YES.getText(),
            DialogOption.NO.getText()
           };
        
        int option;
        try {
            
            option =
                FocusJOptionPane.showOptionDialog(getParentComponent(), 
                              getLabel(message), 
                              title,
                              JOptionPane.YES_NO_OPTION, 
                              JOptionPane.WARNING_MESSAGE, null,
                              options, defaultOption.getText());
        } catch(InternalError ie) {
            // happens occasionally, assume no.
            option = JOptionPane.NO_OPTION;
        }
            
        if(option == JOptionPane.YES_OPTION) {
            return DialogOption.YES;
        }
        
        return DialogOption.NO;
    }
    
    
    final DialogOption showYesNoMessage(String message, IntSetting defValue) {
        return showYesNoMessage(message, defValue, DialogOption.YES);
    }
    
    /**
     * Displays a message to the user and returns 
     * MessageService.YES_OPTION if the user selects yes and
     * MessageService.NO_OPTION if the user selects no.  Stores
     * the default response in IntSetting default.
     *
     * @param message the message to display to the user
     * @param defValue the IntSetting to store/retrieve the the default
     *  value for this question.
     */ 
    final DialogOption showYesNoMessage(String message, IntSetting defValue, DialogOption defaultOption) {
        final String[] options = {DialogOption.YES.getText(),
                DialogOption.NO.getText()
               };
        
        DialogOption ret = DialogOption.parseInt(defValue.getValue());
        if(ret == DialogOption.YES || ret == DialogOption.NO )
            return ret;

        // We only get here if the default didn't have a valid value.           
        int option;
        try {
            option =
                FocusJOptionPane.showOptionDialog(getParentComponent(),
                    alwaysUseThisAnswerLabel(message, defValue),
                    I18n.tr("Message"),
                    JOptionPane.YES_NO_OPTION, 
                    JOptionPane.WARNING_MESSAGE, null,
                    options, defaultOption.getText());
        } catch(ArrayIndexOutOfBoundsException aioobe) {
            // happens occasionally on windows, assume no.
            option = JOptionPane.NO_OPTION;
        } catch(InternalError ie) {
            // happens occasionally, assume no.
            option = JOptionPane.NO_OPTION;
        }
                
                       
        if(option == JOptionPane.YES_OPTION)
            ret = DialogOption.YES;
        else
            ret = DialogOption.NO;
            
        // If we wanted to remember the answer, remember it.            
        if (defValue.getValue() == REMEMBER_ANSWER)
            defValue.setValue(ret.toInt());
        else
            defValue.setValue(FORGET_ANSWER);
            
        return ret;
    }
    
    /**
     * Displays a message to the user and returns 
     * MessageService.YES_OPTION if the user selects yes and
     * MessageService.NO_OPTION if the user selects no.
     * MessageService.CANCEL_OPTION if the user selects cancel.
     *
     * @param message the message to display to the user
     */ 
    final DialogOption showYesNoCancelMessage(String message) {
        int option;
        try {
            option =
                FocusJOptionPane.showConfirmDialog(getParentComponent(), 
                              getLabel(message),
                              I18n.tr("Message"),
                              JOptionPane.YES_NO_CANCEL_OPTION );
        } catch(InternalError ie) {
            // happens occasionally, assume no.
            option = JOptionPane.NO_OPTION;
        }
            
        if (option == JOptionPane.YES_OPTION)
            return DialogOption.YES;
        else if (option == JOptionPane.NO_OPTION)
            return DialogOption.NO;
        return DialogOption.CANCEL;
    }
    
    /**
     * Displays a message to the user and returns 
     * MessageService.YES_OPTION if the user selects yes and
     * MessageService.NO_OPTION if the user selects no.
     * MessageService.CANCEL_OPTION if the user selects cancel.  Stores
     * the default response in IntSetting default.
     *
     * @param message the message to display to the user
     * @param defValue the IntSetting to store/retrieve the the default
     *  value for this question.
     */ 
    final DialogOption showYesNoCancelMessage(String message, IntSetting defValue) {
        // if default has a valid value, use it.
        DialogOption ret = DialogOption.parseInt(defValue.getValue());
        if (ret == DialogOption.YES || ret == DialogOption.NO)
            return ret;
            
        // We only get here if the default didn't have a valid value.           
        int option;
        try {
            option =
                FocusJOptionPane.showConfirmDialog(getParentComponent(),
                    alwaysUseThisAnswerLabel(message, defValue),
                    I18n.tr("Message"),
                    JOptionPane.YES_NO_CANCEL_OPTION );
        } catch(ArrayIndexOutOfBoundsException aioobe) {
            // happens occasionally on windows, assume cancel.
            option = JOptionPane.CANCEL_OPTION;
        } catch(InternalError ie) {
            // happens occasionally, assume cancel.
            option = JOptionPane.CANCEL_OPTION;
        }
                            
        if (option == JOptionPane.YES_OPTION)
            ret = DialogOption.YES;
        else if (option == JOptionPane.NO_OPTION)
            ret = DialogOption.NO;
        else
            ret = DialogOption.CANCEL;
            
        // If we wanted to remember the answer, remember it.            
        if (defValue.getValue() == REMEMBER_ANSWER && ret != DialogOption.CANCEL)
            defValue.setValue(ret.toInt());
        else
            defValue.setValue(FORGET_ANSWER);
            
        return ret;
    }
    
    /**
     * Displays a message to the user and returns 
     * MessageService.YES_OPTION if the user selects yes and
     * MessageService.NO_OPTION if the user selects no.
     * MessageService.OTHER_OPTION if the user selects other.  Stores
     * the default response in IntSetting default.
     *
     * @param message the message to display to the user
     * @param defValue the IntSetting to store/retrieve the the default
     *  value for this question.
     */ 
    final DialogOption showYesNoOtherMessage(String message, IntSetting defValue, String otherName) {
        final String[] options = {DialogOption.YES.getText(),
                DialogOption.NO.getText(), otherName
               };
        
        // if default has a valid value, use it.
        DialogOption ret = DialogOption.parseInt(defValue.getValue());
        if(ret == DialogOption.YES || ret == DialogOption.NO)
            return ret;
            
        // We only get here if the default didn't have a valid value.           
        int option;
        try {
            option = FocusJOptionPane.showOptionDialog(getParentComponent(),
                        alwaysUseThisAnswerLabel(message, defValue),
                        I18n.tr("Message"),
                        0,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        options,
                        DialogOption.YES.getText());
        } catch(ArrayIndexOutOfBoundsException aioobe) {
            // happens occasionally on windows, assume cancel.
            option = JOptionPane.CLOSED_OPTION;
        } catch(InternalError ie) {
            // happens occasionally, assume cancel.
            option = JOptionPane.CLOSED_OPTION;
        }
                
        if(option == 0) // Yes
            ret = DialogOption.YES;
        else if(option == 1) // No
            ret = DialogOption.NO;
        else if(option == 2) // Other
            ret = DialogOption.OTHER;
        else
            ret = DialogOption.CANCEL;
                    
        // If we wanted to remember the answer, remember it.            
        if (defValue.getValue() == REMEMBER_ANSWER && ret != DialogOption.OTHER && ret != DialogOption.CANCEL)
            defValue.setValue(ret.toInt());
        else
            defValue.setValue(FORGET_ANSWER);
            
        return ret;
    }
	
	/**
	 * Convenience method for determining which window should be the parent
	 * of message windows.
	 *
	 * @return the <tt>Component</tt> that should act as the parent of message
	 *  windows
	 */
	public static Component getParentComponent() {
	    if(currentVisibleFrame != null) {
	        return currentVisibleFrame;
	    } else if(GUIMediator.isOptionsVisible()) {
			return GUIMediator.getMainOptionsComponent();
		} else if(GUIMediator.isAppVisible()) {
		    return GUIMediator.getAppFrame();
		} else {
		    return null;
		}
	}
	
    private JComponent getLabel(String message) {
        if(message.startsWith("<html"))
            return new HTMLLabel(message);
        else
            return new MultiLineLabel(message, 400);
    }
    
    private final JComponent doNotDisplayAgainLabel(
      final String message, final Switch setting) {
        JPanel thePanel = new JPanel( new BorderLayout(0, 15) ); 
        JCheckBox option = new JCheckBox(
            I18n.tr("Do not display this message again")
        );
        JComponent lbl = getLabel(message);
        thePanel.add( lbl, BorderLayout.NORTH );
        thePanel.add( option, BorderLayout.WEST );
        option.addItemListener( new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                setting.setValue( e.getStateChange() == ItemEvent.SELECTED );
            }
        });
        return thePanel;
    }
    
    private final JComponent alwaysUseThisAnswerLabel(
      final String message, final IntSetting setting) {
        JPanel thePanel = new JPanel( new BorderLayout(0, 15) ); 
        JCheckBox option = new JCheckBox(
            I18n.tr("Always use this answer")
        );
        JComponent lbl = getLabel(message);
        thePanel.add( lbl, BorderLayout.NORTH );
        thePanel.add( option, BorderLayout.WEST );
        option.addItemListener( new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if ( e.getStateChange() == ItemEvent.SELECTED )
                    setting.setValue( REMEMBER_ANSWER );
                else
                    setting.setValue( FORGET_ANSWER );
            }
        });
        return thePanel;
    }

    public static void setCurrentFrame(FramedDialog dialogFrame) {
        currentVisibleFrame = dialogFrame;
    }    

        
}
