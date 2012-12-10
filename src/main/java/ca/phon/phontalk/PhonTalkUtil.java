package ca.phon.phontalk;

/**
 * Static utilities for PhonTalk.
 *
 */
public class PhonTalkUtil {

	public static final String VERBOSE_SYSPROP = "phontalk.verbose";
	/**
	 * Is verbose output on?
	 * If set, the property "phontalk.verbose" will be true
	 * 
	 * @return the verbose output flag. Default: false.
	 */
	public static boolean isVerbose() {
		final Boolean defVal = Boolean.FALSE;
		final String val = System.getProperty(VERBOSE_SYSPROP, defVal.toString());
		return Boolean.parseBoolean(val);
	}
	
}
