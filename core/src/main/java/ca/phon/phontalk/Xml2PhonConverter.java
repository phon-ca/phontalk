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
package ca.phon.phontalk;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.bind.ValidationException;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.stringtemplate.NoIndentWriter;
import org.antlr.stringtemplate.StringTemplateWriter;

import ca.phon.application.IPhonFactory;
import ca.phon.application.project.IPhonProject;
import ca.phon.application.transcript.ITranscript;
import ca.phon.phontalk.parser.AntlrExceptionVisitor;
import ca.phon.phontalk.parser.ChatParser;
import ca.phon.phontalk.parser.ChatTokenSource;
import ca.phon.phontalk.parser.AntlrTokens;
import ca.phon.phontalk.parser.ChatTree;
import ca.phon.phontalk.parser.Phon2XmlWalker;
import ca.phon.system.logger.PhonLogger;

/**
 * Reads in a talkbank XML file using SAX and
 * JAXB.  Converts the file into a PhonBank v1.1 file.
 * 
 */
public class Xml2PhonConverter {
	
	private final static Logger LOGGER = 
			Logger.getLogger(Xml2PhonConverter.class.getName());
	
	public Xml2PhonConverter() {
		super();
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
		// first try to validate the input file
		final TalkbankValidator validator = new TalkbankValidator();
		final DefaultErrorHandler handler = new DefaultErrorHandler(inputFile, listener);
		try {
			if(!validator.validate(inputFile, handler)) return;
		} catch (ValidationException e1) {
			if(PhonTalkUtil.isVerbose()) e1.printStackTrace();
			final PhonTalkError err = new PhonTalkError(e1.getMessage(), e1);
			err.setFile(inputFile);
			if(listener != null) listener.message(err);
			return;
		}
		
		// create input token stream
		TokenStream tokenStream;
		try {
			final FileInputStream fin = new FileInputStream(inputFile);
			final ChatTokenSource tokenSource = new ChatTokenSource(fin);
			tokenStream = new CommonTokenStream(tokenSource);
		} catch (FileNotFoundException e) {
			if(PhonTalkUtil.isVerbose()) e.printStackTrace();
			final PhonTalkError err = new PhonTalkError(e);
			err.setFile(inputFile);
			if(listener != null) listener.message(err);
			return;
		}
		
		// convert xml stream into an AST
		ChatParser.chat_return parserRet;
		try {
			final ChatParser parser = new ChatParser(tokenStream);
			parser.setFile(inputFile.getAbsolutePath());
			parser.setPhonTalkListener(listener);
			parserRet = parser.chat();
		} catch (RecognitionException re) {
			if(PhonTalkUtil.isVerbose()) re.printStackTrace();
			final AntlrExceptionVisitor visitor = new AntlrExceptionVisitor();
			visitor.visit(re);
			final PhonTalkMessage msg = visitor.getMessage();
			msg.setFile(inputFile);
			if(listener != null) listener.message(msg);
			return;
		}
		
		// walk AST and output using string template
		ITranscript session;
		try {
			final CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(parserRet.getTree());
			final ChatTree walker = new ChatTree(nodeStream);
			walker.setFile(inputFile.getAbsolutePath());
			walker.setPhonTalkListener(listener);
			
			walker.chat();
			session = walker.getSession();
		} catch (RecognitionException re) {
			if(PhonTalkUtil.isVerbose()) re.printStackTrace();
			final AntlrExceptionVisitor visitor = new AntlrExceptionVisitor();
			visitor.visit(re);
			final PhonTalkMessage msg = visitor.getMessage();
			msg.setFile(inputFile);
			if(listener != null) listener.message(msg);
			return;
		}
		
		// save the transcript to the given file (also validates)
		try {
			final FileOutputStream fout = new FileOutputStream(outputFile);
			session.saveTranscriptData(fout);
		} catch (IOException e) {
			if(PhonTalkUtil.isVerbose()) e.printStackTrace();
			final PhonTalkError err = new PhonTalkError(e);
			err.setFile(outputFile);
			if(listener != null) listener.message(err);
			return;
		}
	}

}
