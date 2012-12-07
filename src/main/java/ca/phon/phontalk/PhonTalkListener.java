package ca.phon.phontalk;

/**
 * Handles info messages during conversion.
 * 
 */
public interface PhonTalkListener {
	
	/**
	 * Recieves converter messages as they occur.  
	 * 
	 * @param msg
	 */
	public void message(PhonTalkMessage msg);
	
}
