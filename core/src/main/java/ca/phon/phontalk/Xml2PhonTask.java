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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import ca.phon.application.IPhonFactory;
import ca.phon.application.PhonTask;
import ca.phon.application.transcript.ITranscript;

/**
 * Converts a single stream of tb xml into phon xml
 *
 */
public class Xml2PhonTask extends PhonTask {
	
	/**
	 * Input file
	 */
	private File inputFile;
	
	/**
	 * Output file
	 */
	private File outputFile;
	
	/**
	 * Message listener
	 */
	private PhonTalkListener listener;
	
	public Xml2PhonTask(String inFile, String outFile, PhonTalkListener listener) {
		super();
		this.inputFile = new File(inFile);
		this.outputFile = new File(outFile);
		this.listener = listener;
	}

	@Override
	public void performTask() {
		super.setStatus(TaskStatus.RUNNING);

		final Xml2PhonConverter converter = new Xml2PhonConverter();
		converter.convertFile(inputFile, outputFile, listener);
		
		// attempt to load the transcript at outputFile to
		// ensure session was saved correctly
		final IPhonFactory factory = IPhonFactory.getDefaultFactory();
		final ITranscript session = factory.createTranscript();
		try {
			session.loadTranscriptFile(outputFile);
			super.setStatus(TaskStatus.FINISHED);
		} catch (IOException e) {
			if(PhonTalkUtil.isVerbose()) e.printStackTrace();
			final PhonTalkError err = new PhonTalkError(e);
			err.setFile(outputFile);
			if(listener != null) listener.message(err);
			super.err = e;
			setStatus(TaskStatus.ERROR);
		}
	}

}
