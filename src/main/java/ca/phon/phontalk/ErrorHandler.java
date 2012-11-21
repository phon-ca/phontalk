package ca.phon.phontalk;

/**
 * Handles error events from a converter.
 * 
 */
public interface ErrorHandler {
	
	/**
	 * Recieves converter errors as they occur.   If this
	 * method returns file processing may or may not continue
	 * depending on the severity of the error.  The conversion
	 * process can be halted by throwing a new (or the same)
	 * {@link PhonTalkError}.
	 * 
	 * @param err
	 * @throws PhonTalkError
	 */
	public void converterError(PhonTalkError err)
		throws PhonTalkError;

}
