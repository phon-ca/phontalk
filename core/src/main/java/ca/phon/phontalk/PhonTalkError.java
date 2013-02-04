/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2008 The Phon Project, Memorial University <http://phon.ling.mun.ca>
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
