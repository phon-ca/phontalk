package ca.phon.phontalk.parser;

import org.antlr.runtime.Parser;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.TreeNodeStream;
import org.antlr.runtime.tree.TreeParser;

import ca.phon.phontalk.PhonTalkListener;
import ca.phon.phontalk.PhonTalkMessage;

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
	
	/**
	 * Override the default {@link Parser#getErrorMessage(org.antlr.runtime.RecognitionException, String[])}
	 * method to send a message to the provided listener.
	 */
	@Override
	public String getErrorMessage(RecognitionException re, String[] tokens) {
		final String retVal = super.getErrorMessage(re, tokens);
		final AntlrExceptionVisitor visitor = new AntlrExceptionVisitor();
		visitor.visit(re);
		final PhonTalkMessage msg = visitor.getMessage();
		
		if(getPhonTalkListener() != null && msg != null) {
			getPhonTalkListener().message(msg);
		}
		
		return retVal;
	}
	
}
