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

import ca.phon.session.Record;
import ca.phon.session.Session;

public class TreeBuilderException extends Exception {
	
	/**
	 * session/utterance index
	 */
	private Session session;
	
	private Record utt;

	public TreeBuilderException() {
		super();
	}

	public TreeBuilderException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public TreeBuilderException(String arg0) {
		super(arg0);
	}

	public TreeBuilderException(Throwable arg0) {
		super(arg0);
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public Record getUtt() {
		return utt;
	}

	public void setUtt(Record utt) {
		this.utt = utt;
	}

}
