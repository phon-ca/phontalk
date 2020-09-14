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

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import ca.phon.session.io.*;

/**
 * Converts a single stream of tb xml into phon xml
 *
 */
public class Xml2PhonTask extends PhonTalkTask {
	
	// logger for parsers
	private final static Logger LOGGER = Logger.getLogger("ca.phon.phontalk.parser");
	
	public Xml2PhonTask(String inFile, String outFile, PhonTalkListener listener) {
		super(new File(inFile), new File(outFile), listener);
	}

	@Override
	public void performTask() {
		super.setStatus(TaskStatus.RUNNING);
		
		try {
			final Xml2PhonConverter converter = new Xml2PhonConverter();
			converter.convertFile(getInputFile(), getOutputFile(), getListener());
			
			// check to make sure the file is a valid phon session
			final SessionInputFactory inputFactory = new SessionInputFactory();
			final SessionReader reader = inputFactory.createReader("phonbank", "1.2");
			reader.readSession(new FileInputStream(getOutputFile()));
			super.setStatus(TaskStatus.FINISHED);
		} catch (Exception e) {
			if(PhonTalkUtil.isVerbose()) {
				LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
			final PhonTalkError err = new PhonTalkError(e);
			err.setFile(getOutputFile());
			if(getListener() != null) getListener().message(err);
			super.err = e;
			setStatus(TaskStatus.ERROR);
		}
	}
	
	@Override
	public String getProcessName() {
		return "TalkBank -> Phon";
	}

	
	
}
