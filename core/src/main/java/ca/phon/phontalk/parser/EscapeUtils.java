package ca.phon.phontalk.parser;

/**
 * Class to aid in escaping specific chars in a string.
 *
 *
 */
public class EscapeUtils {

	private final static char ESC = '\\';
	
	public static String escapeParenthesis(String input) {
		final StringBuffer buffer = new StringBuffer();
		
		for(int i = 0; i < input.length(); i++) {
			final char c = input.charAt(i);
			
			switch(c) {
			case '\\':
			case ')':
			case '(':
				buffer.append(ESC);
				
			default:
				buffer.append(c);
				break;
			}
		}
		
		return buffer.toString();
	}
	
	public static String unescapeParenthesis(String input) {
		final StringBuffer buffer = new StringBuffer();
		
		boolean hadSlash = false;
		for(int i = 0; i < input.length(); i++) {
			final char c = input.charAt(i);
			
			if(hadSlash) {
				hadSlash = false;
				buffer.append(c);
				continue;
			} else if(c == ESC) {
				hadSlash = true;
				continue;
			}
			buffer.append(c);
		}
	
		return buffer.toString();
	}
	
}
