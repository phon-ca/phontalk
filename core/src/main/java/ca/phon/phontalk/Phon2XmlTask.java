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

import ca.phon.session.io.*;

/**
 * Converts a single xml file as a stream
 *  from phon's format to talkbank
 *
 */
public class Phon2XmlTask extends PhonTalkTask {
	
	public Phon2XmlTask(File inFile, File outFile, PhonTalkListener listener) {
		super(inFile, outFile, listener);
	}
	
	public Phon2XmlTask(String inFile, String outFile, PhonTalkListener listener) {
		super(new File(inFile), new File(outFile), listener);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void performTask() {
		// convert transcript
		super.setStatus(TaskStatus.RUNNING);
		
		// check to make sure the file is a valid phon session
		final SessionInputFactory inputFactory = new SessionInputFactory();
		final SessionReader reader = inputFactory.createReader("phonbank", "1.2");
		try {
			final InputStream phonStream = new FileInputStream(getInputFile());
			reader.readSession(phonStream);
		} catch (IOException e) {
			if(PhonTalkUtil.isVerbose()) e.printStackTrace();
			final PhonTalkError err = new PhonTalkError(e);
			getListener().message(err);
			super.err = e;
			super.setStatus(TaskStatus.ERROR);
			return;
		}
		
		try {
			final Phon2XmlConverter converter = new Phon2XmlConverter();
			converter.convertFile(getInputFile(), getOutputFile(), getListener());
			
			final TalkbankValidator validator = new TalkbankValidator();
			final DefaultErrorHandler errHandler = new DefaultErrorHandler(getOutputFile(), getListener());
			if(!validator.validate(getOutputFile(), errHandler)) {
				err = new Exception("xml not valid");
				super.setStatus(TaskStatus.ERROR);
			} else {
				super.setStatus(TaskStatus.FINISHED);
			}
		} catch (Exception e) {
			if(PhonTalkUtil.isVerbose()) e.printStackTrace();
			final PhonTalkError err = new PhonTalkError(e);
			err.setFile(getOutputFile());
			getListener().message(err);
			super.err = e;
			super.setStatus(TaskStatus.ERROR);
		}
	}

	@Override
	public String getProcessName() {
		return "Phon -> TalkBank";
	}

}
