package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.xmpp.api.client.FriendRequest;
import org.limewire.xmpp.api.client.FriendRequestEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Handles incoming friend requests by prompting the user to accept or
 * decline them and returning the user's decision in each case to the
 * appropriate <code>FriendRequestDecisionHandler</code> on a
 * background thread.
 */
@Singleton
class FriendRequestHandlerImpl implements RegisteringEventListener<FriendRequestEvent> {

    @Inject
    public void register(ListenerSupport<FriendRequestEvent> FriendRequestEventListenerSupport) {
        FriendRequestEventListenerSupport.addListener(this);
    }

    @Override
    @SwingEDTEvent
    public void handleEvent(final FriendRequestEvent event) {
        new FriendRequestConfirmationDialog(event.getSource());
    }

    private class FriendRequestConfirmationDialog extends LimeJDialog {
        FriendRequestConfirmationDialog(final FriendRequest request) {
            super(GuiUtils.getMainFrame(), tr("Friend Request"));
            setLocationRelativeTo(GuiUtils.getMainFrame());
            setModalityType(ModalityType.MODELESS);
            setLayout(new MigLayout());
            setResizable(false);

            final JLabel text = 
                new JLabel(tr("{0} would like to add you as a friend",
                        request.getFriendUsername()));

            final JButton accept = new JButton(tr("Accept"));
            accept.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                    dispose();
                    BackgroundExecutorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            request.getDecisionHandler().handleDecision(
                                    request.getFriendUsername(), true);

                        }
                    });
                }
            });

            final JButton decline = new JButton(tr("Decline"));
            decline.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                    dispose();
                    BackgroundExecutorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            request.getDecisionHandler().handleDecision(
                                    request.getFriendUsername(), false);

                        }
                    });
                }
            });

            // FIXME: der Layout ist geborken
            add(text, "span, wrap");
            add(accept);
            add(decline);

            pack();
            setVisible(true);
        }
    }
}
