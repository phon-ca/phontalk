package ca.phon.phontalk;

/**
 * Handles error events from a converter.
 * 
 */
public interface ErrorHandler {
	
	/**
	 * Recieves converter errors as they occur.
	 * 
	 * @param err
	 */
	public void converterError(ConverterError err);

}
