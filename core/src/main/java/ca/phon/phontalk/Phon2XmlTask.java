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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;

import javax.management.modelmbean.XMLParseException;
import javax.xml.bind.ValidationException;

import ca.phon.application.IPhonFactory;
import ca.phon.application.PhonTask;
import ca.phon.application.transcript.ITranscript;
import ca.phon.system.logger.PhonLogger;

/**
 * Converts a single xml file as a stream
 *  from phon's format to talkbank
 *
 */
public class Phon2XmlTask extends PhonTask {
	
	private File inputFile;
	
	private File outputFile;
	
	private PhonTalkListener listener;
	
	public Phon2XmlTask(String inFile, String outFile, PhonTalkListener listener) {
		super();
		this.inputFile = new File(inFile);
		this.outputFile = new File(outFile);
		this.listener = listener;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void performTask() {
		// convert transcript
		super.setStatus(TaskStatus.RUNNING);
		
		// check to make sure the file is a valid phon session
		final ITranscript t = IPhonFactory.getDefaultFactory().createTranscript();
		try {
			final InputStream phonStream = new FileInputStream(inputFile);
			t.loadTranscriptData(phonStream);
		} catch (IOException e) {
			if(PhonTalkUtil.isVerbose()) e.printStackTrace();
			final PhonTalkError err = new PhonTalkError(e);
			listener.message(err);
			super.err = e;
			super.setStatus(TaskStatus.ERROR);
			return;
		}
		
		final Phon2XmlConverter converter = new Phon2XmlConverter();
		converter.convertFile(inputFile, outputFile, listener);
		
		final TalkbankValidator validator = new TalkbankValidator();
		final DefaultErrorHandler errHandler = new DefaultErrorHandler(outputFile, listener);
		try {
			if(!validator.validate(outputFile, errHandler)) {
				err = new Exception("xml not valid");
				super.setStatus(TaskStatus.ERROR);
			} else {
				super.setStatus(TaskStatus.FINISHED);
			}
		} catch (ValidationException e) {
			if(PhonTalkUtil.isVerbose()) e.printStackTrace();
			final PhonTalkError err = new PhonTalkError(e);
			err.setFile(outputFile);
			listener.message(err);
			super.err = e;
			super.setStatus(TaskStatus.ERROR);
		}
	}

}
