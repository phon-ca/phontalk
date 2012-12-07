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
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.stringtemplate.NoIndentWriter;
import org.antlr.stringtemplate.StringTemplateWriter;

import ca.phon.application.transcript.ITranscript;
import ca.phon.application.transcript.IUtterance;
import ca.phon.application.transcript.TranscriptUtils;
import ca.phon.phontalk.parser.AntlrTokens;
import ca.phon.phontalk.parser.Phon2XmlWalker;
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
			final PhonTalkMessage err = new PhonTalkError(e);
			e.printStackTrace();
			if(msgListener != null) {
				msgListener.message(err);
			}
			return;
		}
		
		// convert session into a antlr common tree
		final Phon2XmlTreeBuilder builder = new Phon2XmlTreeBuilder();
		final CommonTree sessionTree = builder.buildTree(session);
		if(sessionTree == null) {
			final PhonTalkMessage err = new PhonTalkMessage("Could not build CHAT tree from session.", sessionFile.getAbsolutePath());
			errorHandler.error(err);
			return;
		}
		
		try {
			final FileOutputStream fout = new FileOutputStream(outputFile);
			final OutputStreamWriter fwriter = new OutputStreamWriter(fout, "UTF-8");
			final StringTemplateWriter stWriter = new NoIndentWriter(new PrintWriter(fwriter));
			
			
		} catch (IOException e) {
			final PhonTalkMessage err = new PhonTalkMessage(e.getMessage(), outputFile.getAbsolutePath());
			errorHandler.error(err);
			return;
		}	
	}
	
	private void printTree(CommonTree tree, int tabidx) {
		AntlrTokens tokens = new AntlrTokens("Chat.tokens");
		String tokenName = tokens.getTokenName(tree.getToken().getType());
		String tokenVal = tree.getToken().getText();
		
		for(int i = 0; i < tabidx; i++) System.out.print("\t");
		System.out.println(tokenName  + (tokenVal == null ? "" : ":" + tokenVal));
		
		for(int i = 0; i < tree.getChildCount(); i++) {
			CommonTree child = (CommonTree)tree.getChild(i);
			printTree(child, tabidx+1);
		}
	}

	/**
	 * Method to try and locate the record which causes a
	 * StackOverflowError.  For this purpose we will be performing a
	 * conversion on a test session which contains only one record
	 * (iterating through the records in the given session.)
	 */
	private void findStackOverflowError(ITranscript t) {
		
		IPhonFactory factory = IPhonFactory.getFactory(t.getVersion());

		for(int i = 0; i < t.getNumberOfUtterances(); i++) {
			ITranscript testTranscript = factory.createTranscript();

			// copy session header and participant information
			TranscriptUtils.copyTranscriptInfo(t, testTranscript);

			IUtterance utt = t.getUtterances().get(i);
			TranscriptUtils.addRecordToTranscript(testTranscript, utt, null);

			// now attept to convert the test Transcript
			Phon2XmlTreeBuilder treeBuilder = new Phon2XmlTreeBuilder();
			CommonTree tTree = treeBuilder.buildTree(testTranscript);
			
			printTree(tTree, 0);

			CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(tTree);
			Phon2XmlWalker walker = new Phon2XmlWalker(nodeStream);

			try {
				Phon2XmlWalker.chat_return v = walker.chat();
				v.st.toString();
			} catch (RecognitionException e) {
				PhonLogger.warning(e.toString());
			} catch (StackOverflowError err) {
				PhonLogger.severe("Error processing record " + (i+1));
				break;
			} catch (Exception e) {
				PhonLogger.severe("Error processing record " + (i+1));
				e.printStackTrace();
				break;
			}
		}

	}

}
