package ca.phon.phontalk.parser;

import ca.phon.application.transcript.ITranscript;
import ca.phon.application.transcript.IUtterance;

public class TreeBuilderException extends Exception {
	
	/**
	 * session/utterance index
	 */
	private ITranscript session;
	
	private IUtterance utt;

	public TreeBuilderException() {
		super();
	}

	public TreeBuilderException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public TreeBuilderException(String arg0) {
		super(arg0);
	}

	public TreeBuilderException(Throwable arg0) {
		super(arg0);
	}

	public ITranscript getSession() {
		return session;
	}

	public void setSession(ITranscript session) {
		this.session = session;
	}

	public IUtterance getUtt() {
		return utt;
	}

	public void setUtt(IUtterance utt) {
		this.utt = utt;
	}

}
