package ca.phon.phontalk.parser;

import java.io.File;

import org.antlr.runtime.Parser;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.TreeNodeStream;
import org.antlr.runtime.tree.TreeParser;

import ca.phon.phontalk.PhonTalkListener;
import ca.phon.phontalk.PhonTalkMessage;
import ca.phon.phontalk.PhonTalkUtil;

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
