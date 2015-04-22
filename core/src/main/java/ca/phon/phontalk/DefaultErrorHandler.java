/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2015, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
 * Dept of Linguistics, Memorial University <https://phon.ca>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
