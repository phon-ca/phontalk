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

import java.io.File;
import java.util.logging.Level;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.TreeNodeStream;
import org.antlr.runtime.tree.TreeParser;

import ca.phon.phontalk.PhonTalkListener;
import ca.phon.phontalk.PhonTalkMessage;
import ca.phon.phontalk.PhonTalkMessage.Severity;

public class PhonTalkTreeParser extends TreeParser {

	public PhonTalkTreeParser(TreeNodeStream input, RecognizerSharedState state) {
		super(input, state);
	}

	public PhonTalkTreeParser(TreeNodeStream input) {
		super(input);
	}

	/**
	 * The file being parsed
	 */
	private String filename;
	
	public String getFile() {
		return this.filename;
	}
	
	public void setFile(String file) {
		this.filename = file;
	}
	
	/**
	 * Message listener
	 */
	private PhonTalkListener listener;
	
	public PhonTalkListener getPhonTalkListener() {
		return this.listener;
	}
	
	public void setPhonTalkListener(PhonTalkListener listener) {
		this.listener = listener;
	}

	public void reportInfo(String message) {
		reportError(message, Level.INFO);
	}
	
	public void reportWarning(String message) {
		reportError(message, Level.WARNING);
	}
	
	public void reportError(String message) {
		reportError(message, Level.SEVERE);
	}
	
	public void reportError(String message, Level level) {
		if(level.intValue() <= Level.INFO.intValue()) {
			reportError(message, Severity.INFO);
		} else if(level.intValue() <= Level.WARNING.intValue()) {
			reportError(message, Severity.WARNING);
		} else {
			reportError(message, Severity.SEVERE);
		}
	}
	
	public void reportError(String message, Severity severity) {
		PhonTalkMessage msg = new PhonTalkMessage(message, severity);
		msg.setFile(new File(getFile()));
		message(msg);
	}
	
	public void message(PhonTalkMessage message) {
		getPhonTalkListener().message(message);
	}
	
	
	@Override
	public void reportError(RecognitionException re) {
		super.reportError(re);
		
		final AntlrExceptionVisitor visitor = new AntlrExceptionVisitor();
		visitor.visit(re);
		final PhonTalkMessage msg = visitor.getMessage();
		msg.setFile(new File(this.filename));
		if(getPhonTalkListener() != null && msg != null) {
			getPhonTalkListener().message(msg);
		}
	}
	
	
}
