package ca.phon.phontalk;

/**
 * An error during the conversion process.
 * 
 */
public class PhonTalkError extends PhonTalkMessage {
	
	private Throwable cause;
	
	public PhonTalkError(String msg, Throwable cause) {
		super(msg, Severity.SEVERE);
		this.cause = cause;
	}
	
	public PhonTalkError(Throwable cause) {
		this(cause, Severity.SEVERE);
	}
	
	public PhonTalkError(Throwable cause, Severity severity) {
		super(cause.getMessage(), severity);
		this.cause = cause;
	}
	
	public Throwable getCause() {
		return this.cause;
	}
	
	public void setCause(Throwable cause) {
		this.cause = cause;
		setMessage((this.cause != null ? this.cause.getMessage() : ""));
	}
	
}
