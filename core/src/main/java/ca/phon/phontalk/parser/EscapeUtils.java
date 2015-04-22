/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2015, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
 * Dept of Linguistics, Memorial University <https://phon.ca>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
