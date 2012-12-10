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
