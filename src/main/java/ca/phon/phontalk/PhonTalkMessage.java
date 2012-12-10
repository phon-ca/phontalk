package ca.phon.phontalk;

import java.io.File;
import java.io.Serializable;

/**
 * An message during the conversion process.
 */
public class PhonTalkMessage implements Serializable {
	
	private static final long serialVersionUID = 7955358836268036775L;

	/**
	 * Severity
	 */
	public static enum Severity {
		INFO,
		WARNING,
		SEVERE;
	};
	
	/**
	 * Severity of this message (default: INFO)
	 */
	private Severity severity = Severity.INFO;
	
	/**
	 * Message
	 */
	private String message = "";
	
	/**
	 * Optional file information
	 */
	private File file;
	
	private int lineNumber = -1;
	
	private int colNumber = -1;
	
	public PhonTalkMessage(String msg) {
		super();
		this.message = msg;
	}
	
	public PhonTalkMessage(String msg, Severity severity) {
		super();
		this.message = msg;
		this.severity = severity;
	}

	public Severity getSeverity() {
		return severity;
	}

	public void setSeverity(Severity severity) {
		this.severity = severity;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public int getColNumber() {
		return colNumber;
	}

	public void setColNumber(int colNumber) {
		this.colNumber = colNumber;
	}

}
