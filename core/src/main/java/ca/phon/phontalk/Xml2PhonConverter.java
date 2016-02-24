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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.xml.bind.ValidationException;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import ca.phon.phontalk.parser.AntlrExceptionVisitor;
import ca.phon.phontalk.parser.AntlrTokens;
import ca.phon.phontalk.parser.AntlrUtils;
import ca.phon.phontalk.parser.ChatParser;
import ca.phon.phontalk.parser.ChatTokenSource;
import ca.phon.phontalk.parser.ChatTree;
import ca.phon.session.Session;
import ca.phon.session.io.SessionOutputFactory;
import ca.phon.session.io.SessionWriter;

/**
 * Reads in a talkbank XML file using SAX and
 * JAXB.  Converts the file into a PhonBank v1.1 file.
 * 
 */
public class Xml2PhonConverter {
	
	private final static Logger LOGGER = 
			Logger.getLogger(Xml2PhonConverter.class.getName());
	
	private File inputFile = null;
	
	public Xml2PhonConverter() {
		super();
	}
	
	public File getInputFile() {
		return this.inputFile;
	}
	
	public void setInputFile(File inputFile) {
		this.inputFile = inputFile;
	}
	
	/**
	 * Convert the given steram into a session object.
	 * 
	 * @param inputStream
	 * @param listener
	 * @return session
	 * 
	 */
	public Session convertStream(InputStream inputStream, PhonTalkListener listener) {
		// create input token stream
		final ChatTokenSource tokenSource = new ChatTokenSource(inputStream);
		TokenStream	tokenStream = new CommonTokenStream(tokenSource);
		
		// convert xml stream into an AST
		ChatParser.chat_return parserRet;
		try {
			final ChatParser parser = new ChatParser(tokenStream);
			parser.setFile(inputFile.getAbsolutePath());
			parser.setPhonTalkListener(listener);
			parserRet = parser.chat();
		} catch (RecognitionException re) {
			if(PhonTalkUtil.isVerbose()) re.printStackTrace();
			final AntlrExceptionVisitor visitor = new AntlrExceptionVisitor(new AntlrTokens("ChatParser.tokens"));
			visitor.visit(re);
			final PhonTalkMessage msg = visitor.getMessage();
			msg.setFile(inputFile);
			if(listener != null) listener.message(msg);
			return null;
		}
		
		// walk AST and output using string template
		Session session = null;
		try {
			final CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(parserRet.getTree());
			final ChatTree walker = new ChatTree(nodeStream);
			walker.setFile(inputFile.getAbsolutePath());
			walker.setPhonTalkListener(listener);
			
			walker.chat();
			session = walker.getSession();
		} catch (TreeWalkerError e) {
			if(e.getCause() instanceof RecognitionException) {
				final RecognitionException re = (RecognitionException)e.getCause();
				final AntlrExceptionVisitor visitor = new AntlrExceptionVisitor(new AntlrTokens("ChatTree.tokens"));
				visitor.visit(re);
				final PhonTalkMessage msg = visitor.getMessage();
				msg.setMessage(msg.getMessage());
				if(listener != null) {
					listener.message(msg);
				}
			} else {
				final PhonTalkError err = new PhonTalkError(e.getMessage(), e);
				if(listener != null) {
					listener.message(err);
				}
			}
		} catch (RecognitionException re) {
			if(PhonTalkUtil.isVerbose()) re.printStackTrace();
			final AntlrExceptionVisitor visitor = new AntlrExceptionVisitor(new AntlrTokens("ChatTree.tokens"));
			visitor.visit(re);
			final PhonTalkMessage msg = visitor.getMessage();
			msg.setFile(inputFile);
			if(listener != null) listener.message(msg);
			return null;
		}
		
		return session;
	}

	/**
	 * Convert the given talkbank file and save as the
	 * given phone file.
	 * 
	 * @param inputFile
	 * @param outputFile
	 * @param listener
	 */
	public void convertFile(File inputFile, File outputFile, PhonTalkListener listener) {
		this.inputFile = inputFile;
		
		// first try to validate the input file
		final TalkbankValidator validator = new TalkbankValidator();
		final DefaultErrorHandler handler = new DefaultErrorHandler(inputFile, listener);
		
		// turn off validation of hacked version
		try {
			if(!validator.validate(inputFile, handler)) return;
		} catch (ValidationException e1) {
			if(PhonTalkUtil.isVerbose()) e1.printStackTrace();
			final PhonTalkError err = new PhonTalkError(e1.getMessage(), e1);
			err.setFile(inputFile);
			if(listener != null) listener.message(err);
			return;
		}
		
		Session session = null;
		try(FileInputStream fin = new FileInputStream(inputFile)) {
			session = convertStream(fin, listener);
		} catch (IOException e) {
			
		}
		
		if(session != null) {
			// save the transcript to the given file (also validates)
			try (FileOutputStream fout = new FileOutputStream(outputFile)) {
				final SessionOutputFactory sessionOutputFactory =
						new SessionOutputFactory();
				final SessionWriter writer = sessionOutputFactory.createWriter();
				
				writer.writeSession(session, fout);
			} catch (IOException e) {
				if(PhonTalkUtil.isVerbose()) e.printStackTrace();
				final PhonTalkError err = new PhonTalkError(e);
				err.setFile(outputFile);
				if(listener != null) listener.message(err);
				return;
			}
		}
	}

}
