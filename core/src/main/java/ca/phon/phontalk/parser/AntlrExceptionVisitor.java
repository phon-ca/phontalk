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
package ca.phon.phontalk.parser;

import org.antlr.runtime.MismatchedTokenException;
import org.antlr.runtime.MismatchedTreeNodeException;
import org.antlr.runtime.RecognitionException;

import ca.hedlund.dp.visitor.VisitorAdapter;
import ca.hedlund.dp.visitor.annotation.Visits;
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
	
	private AntlrTokens tokens;
	
	public AntlrExceptionVisitor() {
		
	}
	
	public AntlrExceptionVisitor(AntlrTokens tokens) {
		this.tokens = tokens;
	}
	
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
	
	@Visits
	public void visitMismatchedTokenException(MismatchedTokenException mte) {
		// convert token ids to names if possible
		if(tokens != null) {
			String expectedToken = tokens.getTokenName(mte.expecting);
			String unexpectedToken = tokens.getTokenName(mte.getUnexpectedType());
			
			String msg = (mte.getMessage() != null ? mte.getMessage() : mte.toString()) + " Expecting " + expectedToken + ", Got " + unexpectedToken;
			message = new PhonTalkError(mte);
			message.setMessage(msg);
			message.setLineNumber(mte.line);
			message.setColNumber(mte.charPositionInLine);
		} else {
			fallbackVisit(mte);
		}
	}
	
	@Visits
	public void visitMismatchedTreeNodeException(MismatchedTreeNodeException mte) {
		// convert token ids to names if possible
		if(tokens != null) {
			String expectedToken = tokens.getTokenName(mte.expecting);
			String unexpectedToken = tokens.getTokenName(mte.getUnexpectedType());
			
			String msg = (mte.getMessage() != null ? mte.getMessage() : mte.toString()) + " Expecting " + expectedToken + ", Got " + unexpectedToken;
			message = new PhonTalkError(mte);
			message.setMessage(msg);
			message.setLineNumber(mte.line);
			message.setColNumber(mte.charPositionInLine);
		} else {
			fallbackVisit(mte);
		}
	}
	
}
