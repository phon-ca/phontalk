package ca.phon.phontalk;

import java.util.logging.Logger;

/**
 * Outputs messages using the default logger.
 *
 */
public class DefaultPhonTalkListener implements PhonTalkListener {

	private final static Logger LOGGER =
			Logger.getLogger("ca.phon.phontalk");
	
	@Override
	public void message(PhonTalkMessage msg) {
		final String txt = msg.getMessage();
		switch(msg.getSeverity()) {
		case INFO:
			LOGGER.info(txt);
			break;
			
		case WARNING:
			LOGGER.warning(txt);
			break;
			
		case SEVERE:
			LOGGER.severe(txt);
			break;
			
		default:
			LOGGER.fine(txt);
		}
	}

}
