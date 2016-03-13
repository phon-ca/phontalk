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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.stringtemplate.NoIndentWriter;
import org.antlr.stringtemplate.StringTemplateWriter;

import ca.phon.phontalk.parser.AST2TalkBank;
import ca.phon.phontalk.parser.AntlrExceptionVisitor;
import ca.phon.phontalk.parser.AntlrTokens;
import ca.phon.phontalk.parser.AntlrUtils;
import ca.phon.phontalk.parser.Phon2XmlTreeBuilder;
import ca.phon.phontalk.parser.TreeBuilderException;
import ca.phon.session.Participant;
import ca.phon.session.Record;
import ca.phon.session.Session;
import ca.phon.session.SessionFactory;
import ca.phon.session.io.SessionInputFactory;
import ca.phon.session.io.SessionReader;
import ca.phon.util.PrefHelper;

public class Phon2XmlConverter {
	
	private File sessionFile = new File("unknown.xml");
	
	private File outputFile = new File("unknown-xml.xml");
	
	public Phon2XmlConverter() {
		super();
	}
	
	public File getSessionFile() {
		return this.sessionFile;
	}
	
	public void setSessionFile(File sessionFile) {
		this.sessionFile = sessionFile;
	}
	
	public File getOutputFile() {
		return this.outputFile;
	}
	
	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}
	
	public void sessionToStream(Session session, OutputStream outputStream, PhonTalkListener msgListener) {
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
		} catch (Exception e) {
			findRecordsWithErrors(sessionFile, session, msgListener);
			return;
		}
		
		try {
			final OutputStreamWriter fwriter = new OutputStreamWriter(outputStream, "UTF-8");
			final StringTemplateWriter stWriter = new NoIndentWriter(new PrintWriter(fwriter));
			
			final CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(sessionTree);
			final AST2TalkBank walker = new AST2TalkBank(nodeStream);
			walker.setFile(outputFile.getAbsolutePath());
			final AST2TalkBank.chat_return ret = walker.chat();
			ret.st.write(stWriter);
			fwriter.flush();
			fwriter.close();
		} catch (IOException e) {
			final PhonTalkMessage err = new PhonTalkError(e);
			err.setFile(outputFile);
			if(msgListener != null) {
				msgListener.message(err);
			}
			findRecordsWithErrors(sessionFile, session, msgListener);
			return;
		} catch (RecognitionException re) {
			final AntlrExceptionVisitor visitor = new AntlrExceptionVisitor();
			visitor.visit(re);
			final PhonTalkMessage msg = visitor.getMessage();
			msg.setFile(outputFile);
			if(msgListener != null) {
				msgListener.message(msg);
			}
			findRecordsWithErrors(sessionFile, session, msgListener);
			return;
		} catch (Exception | StackOverflowError e) {
			final StackTraceElement ste = e.getStackTrace()[0];
			System.out.println(ste.toString());
			// sometime stackoverflow and other runtime errors
			// can occur during string template output
			findRecordsWithErrors(sessionFile, session, msgListener);
		}
	}
	
	public void convertFile(File sessionFile, File outputFile) {
		convertFile(sessionFile, outputFile, null);
	}
	
	public void convertFile(File sessionFile, File outputFile, PhonTalkListener msgListener) {
		setSessionFile(sessionFile);
		setOutputFile(outputFile);
		// HACK fix issues caused by changes in the way Phon 2.0 handles braces in Orthography
		final StringBuffer sessionBuffer = new StringBuffer();
		try {
			final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(sessionFile), "UTF-8"));
			String line = null;
			while((line = in.readLine()) != null) {
				sessionBuffer.append(line).append("\n");
				
			}
			in.close();
			String sessionData = sessionBuffer.toString();
			sessionData = sessionData.replaceAll("<p type=\"OPEN_BRACE\">\\{</p>", "<ig type=\"s\"/>");
			sessionData = sessionData.replaceAll("<p type=\"CLOSE_BRACE\">\\}</p>", "<ig type=\"e\"/>");
			
			final File tempFile = File.createTempFile("phontalk", sessionFile.getName());
			final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8"));
			out.write(sessionData);
			out.flush();
			out.close();
			
			sessionFile = tempFile;
		} catch (IOException e) {
			if(PhonTalkUtil.isVerbose()) e.printStackTrace();
			final PhonTalkMessage err = new PhonTalkError(e);
			err.setFile(sessionFile);
			if(msgListener != null) {
				msgListener.message(err);
			}
			return;
		}
		
		// open phon session, also performs validation of the input file
		final SessionInputFactory factory = new SessionInputFactory();
		final SessionReader reader = factory.createReaderForFile(sessionFile);
		if(reader == null) {
			final PhonTalkMessage err = new PhonTalkMessage("Unable to read session, unknown format.");
			err.setFile(sessionFile);
			if(msgListener != null) {
				msgListener.message(err);
			}
			return;
		}
		Session session = null;
		try(FileInputStream fin = new FileInputStream(sessionFile)) {
			session = reader.readSession(fin);
		} catch (IOException e) {
			if(PhonTalkUtil.isVerbose()) e.printStackTrace();
			final PhonTalkMessage err = new PhonTalkError(e);
			err.setFile(sessionFile);
			if(msgListener != null) {
				msgListener.message(err);
			}
			return;
		}
		
		try(FileOutputStream fout = new FileOutputStream(outputFile)) {
			sessionToStream(session, fout, msgListener);
		} catch (IOException e) {
			if(PhonTalkUtil.isVerbose()) e.printStackTrace();
			final PhonTalkMessage err = new PhonTalkError(e);
			err.setFile(sessionFile);
			if(msgListener != null) {
				msgListener.message(err);
			}
			return;
		}
	}
	
	/**
	 * Attempts to process a session one record at a time in order
	 * to determine records which have conversion problems.
	 * 
	 * @param t
	 */
	private void findRecordsWithErrors(File f, Session session, PhonTalkListener listener) {
		
		final SessionFactory factory = SessionFactory.newFactory();
		
		for(int i = 0; i < session.getRecordCount(); i++) {
			Session testSession = factory.createSession();
			factory.copySessionInformation(session, testSession);
			factory.copySessionMetadata(session, testSession);
			factory.copySessionTierInformation(session, testSession);
			for(Participant p:session.getParticipants()) {
				testSession.addParticipant(factory.cloneParticipant(p));
			}
			
			Record record = session.getRecord(i);
			testSession.addRecord(record);

			// now attept to convert the test Transcript
			Phon2XmlTreeBuilder treeBuilder = new Phon2XmlTreeBuilder();
			CommonTree tTree;
			try {
				tTree = treeBuilder.buildTree(testSession);
			} catch (TreeBuilderException e1) {
				final PhonTalkError err = new PhonTalkError(e1);
				if(listener != null) listener.message(err);
				continue;
			} catch (Exception e) {
				final PhonTalkError err = new PhonTalkError(e);
				if(listener != null) listener.message(err);
				continue;
			}

			CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(tTree);
			AST2TalkBank walker = new AST2TalkBank(nodeStream);

			try {
				AST2TalkBank.chat_return v = walker.chat();
				v.st.toString();
			} catch (TreeWalkerError e) {
				if(e.getCause() instanceof RecognitionException) {
					final RecognitionException re = (RecognitionException)e.getCause();
					final AntlrExceptionVisitor visitor = new AntlrExceptionVisitor(new AntlrTokens("AST2TalkBank.tokens"));
					visitor.visit(re);
					final PhonTalkMessage msg = visitor.getMessage();
					msg.setMessage("Record #" + (i+1) + " " + msg.getMessage());
					msg.setFile(f);
					if(listener != null) {
						listener.message(msg);
					}
				} else {
					final PhonTalkError err = new PhonTalkError("Record #" + (i+1) + " " + e.getMessage(), e);
					err.setFile(f);
					if(listener != null) {
						listener.message(err);
					}
				}
			} catch (RecognitionException re) {
				AntlrUtils.printTree(tTree);
				final AntlrExceptionVisitor visitor = new AntlrExceptionVisitor(new AntlrTokens("AST2TalkBank.tokens"));
				visitor.visit(re);
				final PhonTalkMessage msg = visitor.getMessage();
				msg.setMessage("Record #" + (i+1) + " " + msg.getMessage());
				msg.setFile(f);
				if(listener != null) {
					listener.message(msg);
				}
			} catch (Exception | StackOverflowError e) {
				AntlrUtils.printTree(tTree);
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
