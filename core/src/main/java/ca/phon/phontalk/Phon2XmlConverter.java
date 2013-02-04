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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

import ca.phon.application.IPhonFactory;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.stringtemplate.NoIndentWriter;
import org.antlr.stringtemplate.StringTemplateWriter;

import ca.phon.application.transcript.ITranscript;
import ca.phon.application.transcript.IUtterance;
import ca.phon.application.transcript.TranscriptUtils;
import ca.phon.phontalk.PhonTalkMessage.Severity;
import ca.phon.phontalk.parser.AntlrExceptionVisitor;
import ca.phon.phontalk.parser.AntlrTokens;
import ca.phon.phontalk.parser.Phon2XmlTreeBuilder;
import ca.phon.phontalk.parser.Phon2XmlWalker;
import ca.phon.phontalk.parser.TreeBuilderException;
import ca.phon.system.logger.PhonLogger;

public class Phon2XmlConverter {
	
	public Phon2XmlConverter() {
		super();
	}
	
	public void convertFile(File sessionFile, File outputFile) {
		convertFile(sessionFile, outputFile, null);
	}
	
	public void convertFile(File sessionFile, File outputFile, PhonTalkListener msgListener) {
		// open phon session, also performs validation of the input file
		final IPhonFactory factory = IPhonFactory.getDefaultFactory();
		final ITranscript session = factory.createTranscript();
		try {
			session.loadTranscriptFile(sessionFile);
		} catch (IOException e) {
			if(PhonTalkUtil.isVerbose()) e.printStackTrace();
			final PhonTalkMessage err = new PhonTalkError(e);
			err.setFile(sessionFile);
			if(msgListener != null) {
				msgListener.message(err);
			}
			return;
		}
		
		// convert session into a antlr common tree
		final Phon2XmlTreeBuilder builder = new Phon2XmlTreeBuilder();
		CommonTree sessionTree;
		try {
			sessionTree = builder.buildTree(session);
		} catch (TreeBuilderException e1) {
			if(PhonTalkUtil.isVerbose()) e1.printStackTrace();
			final PhonTalkError err = new PhonTalkError(e1);
			err.setFile(sessionFile);
			if(msgListener != null) msgListener.message(err);
			return;
		}
		
		try {
			final FileOutputStream fout = new FileOutputStream(outputFile);
			final OutputStreamWriter fwriter = new OutputStreamWriter(fout, "UTF-8");
			final StringTemplateWriter stWriter = new NoIndentWriter(new PrintWriter(fwriter));
			
			final CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(sessionTree);
			final Phon2XmlWalker walker = new Phon2XmlWalker(nodeStream);
			walker.setFile(outputFile.getAbsolutePath());
			final Phon2XmlWalker.chat_return ret = walker.chat();
			ret.st.write(stWriter);
			fwriter.flush();
			fwriter.close();
		} catch (IOException e) {
			final PhonTalkMessage err = new PhonTalkError(e);
			err.setFile(outputFile);
			if(msgListener != null) {
				msgListener.message(err);
			}
			return;
		} catch (RecognitionException re) {
			final AntlrExceptionVisitor visitor = new AntlrExceptionVisitor();
			visitor.visit(re);
			final PhonTalkMessage msg = visitor.getMessage();
			msg.setFile(outputFile);
			if(msgListener != null) {
				msgListener.message(msg);
			}
			return;
		} catch (Exception e) {
			// sometime stackoverflow and other runtime errors
			// can occur during string template output
			findRecordsWithErrors(sessionFile, session, msgListener);
		}
	}
	
	/**
	 * Attempts to process a session one record at a time in order
	 * to determine records which have conversion problems.
	 * 
	 * @param t
	 */
	private void findRecordsWithErrors(File f, ITranscript t, PhonTalkListener listener) {
		
		IPhonFactory factory = IPhonFactory.getFactory(t.getVersion());

		for(int i = 0; i < t.getNumberOfUtterances(); i++) {
			ITranscript testTranscript = factory.createTranscript();

			// copy session header and participant information
			TranscriptUtils.copyTranscriptInfo(t, testTranscript);

			IUtterance utt = t.getUtterances().get(i);
			TranscriptUtils.addRecordToTranscript(testTranscript, utt, null);

			// now attept to convert the test Transcript
			Phon2XmlTreeBuilder treeBuilder = new Phon2XmlTreeBuilder();
			CommonTree tTree;
			try {
				tTree = treeBuilder.buildTree(testTranscript);
			} catch (TreeBuilderException e1) {
				final PhonTalkError err = new PhonTalkError(e1);
				if(listener != null) listener.message(err);
				continue;
			}

			CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(tTree);
			Phon2XmlWalker walker = new Phon2XmlWalker(nodeStream);

			try {
				Phon2XmlWalker.chat_return v = walker.chat();
				v.st.toString();
			} catch (RecognitionException re) {
				final AntlrExceptionVisitor visitor = new AntlrExceptionVisitor();
				visitor.visit(re);
				final PhonTalkMessage msg = visitor.getMessage();
				msg.setMessage("Record #" + (i+1) + " " + msg.getMessage());
				msg.setFile(f);
				if(listener != null) {
					listener.message(msg);
				}
			} catch (Exception e) {
				final PhonTalkError err = new PhonTalkError("Record #" + (i+1) + " " + e.getMessage(), e);
				err.setFile(f);
				if(listener != null) {
					listener.message(err);
				}
				return;
			}
		}

	}

}
