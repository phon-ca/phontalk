/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2008 The Phon Project, Memorial University <http://phon.ling.mun.ca>
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
package ca.phon.phontalk.parser;

import org.antlr.runtime.RecognitionException;

import ca.hedlund.dp.visitor.VisitorAdapter;
import ca.phon.phontalk.PhonTalkError;
import ca.phon.phontalk.PhonTalkMessage;

/**
 * Creates a {@link PhonTalkMessage} from antlr {@link RecognitionException}.
 * 
 */
public class AntlrExceptionVisitor extends VisitorAdapter<RecognitionException> {

	/**
	 * The generated message
	 */
	private PhonTalkMessage message;
	
	public PhonTalkMessage getMessage() {
		return this.message;
	}
	
	@Override
	public void fallbackVisit(RecognitionException obj) {
		message = new PhonTalkError(obj);
		message.setMessage((obj.getMessage() != null ? obj.getMessage() : obj.toString()));
		message.setLineNumber(obj.line);
		message.setColNumber(obj.charPositionInLine);
	}
	
}
