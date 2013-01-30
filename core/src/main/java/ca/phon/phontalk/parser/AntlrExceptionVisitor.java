package ca.phon.phontalk.parser;

import org.antlr.runtime.RecognitionException;

import ca.hedlund.dp.visitor.VisitorAdapter;
import ca.phon.phontalk.PhonTalkError;
import ca.phon.phontalk.PhonTalkMessage;

/**
 * Creates a {@link PhonTalkMessage} from antlr {@link RecognitionException}.
 * 
 */
public class AntlrExceptionVisitor extends VisitorAdapter<RecognitionException> {

	/**
	 * The generated message
	 */
	private PhonTalkMessage message;
	
	public PhonTalkMessage getMessage() {
		return this.message;
	}
	
	@Override
	public void fallbackVisit(RecognitionException obj) {
		message = new PhonTalkError(obj);
		message.setMessage((obj.getMessage() != null ? obj.getMessage() : obj.toString()));
		message.setLineNumber(obj.line);
		message.setColNumber(obj.charPositionInLine);
	}
	
}
