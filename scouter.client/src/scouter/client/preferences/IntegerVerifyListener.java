package scouter.client.preferences;

import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Text;

public class IntegerVerifyListener implements VerifyListener {
	@Override
	public void verifyText(VerifyEvent e) {
		Text source = (Text) e.getSource();
        final String oldValue = source.getText();
        final String newValue = oldValue.substring(0, e.start) + e.text + oldValue.substring(e.end);
        try {
            Integer iv = new Integer(newValue);
        }
        catch (final NumberFormatException numberFormatException){
            e.doit = false;
        }
		
	}

}
