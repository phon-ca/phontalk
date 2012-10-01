package ca.phon.phontalk;

/**
 * An error during the conversion process.
 */
public class ConverterError extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7955358836268036775L;
	
	/** The filename */
	private String filename;
	
	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	/** Line number, -1 if not known */
	private int lineNumber = -1;
	
	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public ConverterError() {
		this("", "", -1);
	}
	
	public ConverterError(String file) {
		this("", file, -1);
	}
	
	public ConverterError(String msg, String file, int lineNum) {
		super(msg);
		this.filename = file;
		this.lineNumber = lineNum;
	}
	
	

}
