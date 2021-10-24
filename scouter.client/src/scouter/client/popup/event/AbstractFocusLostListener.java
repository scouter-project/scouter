package scouter.client.popup.event;

import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;

public abstract class AbstractFocusLostListener implements FocusListener {
    @Override
    public final void focusGained(FocusEvent focusEvent) {
        // do nothing.
    }
}
