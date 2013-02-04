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

import java.io.File;

import org.antlr.runtime.Parser;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.TokenStream;

import ca.phon.phontalk.PhonTalkListener;
import ca.phon.phontalk.PhonTalkMessage;
import ca.phon.phontalk.PhonTalkUtil;

/**
 * Custom base class for antlr parsers.
 */
public class PhonTalkParser extends Parser {
	
	public PhonTalkParser(TokenStream input, RecognizerSharedState state) {
		super(input, state);
	}

	public PhonTalkParser(TokenStream input) {
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
	
	@Override
	public void reportError(RecognitionException re) {
		if(PhonTalkUtil.isVerbose()) re.printStackTrace();
		final AntlrExceptionVisitor visitor = new AntlrExceptionVisitor();
		visitor.visit(re);
		final PhonTalkMessage msg = visitor.getMessage();
		msg.setFile(new File(this.filename));
		if(getPhonTalkListener() != null && msg != null) {
			getPhonTalkListener().message(msg);
		}
	}
	
}
