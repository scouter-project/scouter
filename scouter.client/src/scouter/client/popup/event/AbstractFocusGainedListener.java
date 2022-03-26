package scouter.client.popup.event;

import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;

public abstract class AbstractFocusGainedListener implements FocusListener {
    @Override
    public final void focusLost(FocusEvent focusEvent) {
        // do nothing.
    }
}
