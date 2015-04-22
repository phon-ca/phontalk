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
package ca.phon.phontalk;

import java.io.File;
import java.io.Serializable;

/**
 * An message during the conversion process.
 */
public class PhonTalkMessage implements Serializable, Comparable<PhonTalkMessage> {
	
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

	@Override
	public int compareTo(PhonTalkMessage o) {
		final Integer myVal = getSeverity().ordinal();
		final Integer oVal = o.getSeverity().ordinal();
		return myVal.compareTo(oVal);
	}

}
