package ca.phon.phontalk;

import java.io.File;
import java.util.logging.Logger;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import ca.phon.phontalk.PhonTalkMessage.Severity;

/**
 * Default error handler for PhonTalk.  Sends error messages
 * to the given {@link PhonTalkListener}.
 *
 */
public class DefaultErrorHandler implements ErrorHandler {

	/**
	 * File
	 */
	private File file;
	
	/**
	 * Listener
	 */
	private PhonTalkListener listener;
	
	public DefaultErrorHandler(File file, PhonTalkListener listener) {
		super();
		this.file = file;
		this.listener = listener;
	}
	
	@Override
	public void error(SAXParseException e) throws SAXException {
		final PhonTalkError err = createError(e);
		listener.message(err);
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		final PhonTalkError err = createError(e);
		listener.message(err);
	}

	@Override
	public void warning(SAXParseException e) throws SAXException {
		final PhonTalkError err = createError(e);
		err.setSeverity(Severity.WARNING);
		listener.message(err);
	}
	
	// create a new error from the given exception
	private PhonTalkError createError(SAXParseException e) {
		final PhonTalkError retVal = new PhonTalkError(e);
		retVal.setFile(file);
		retVal.setLineNumber(e.getLineNumber());
		retVal.setColNumber(e.getColumnNumber());
		return retVal;
	}

}
