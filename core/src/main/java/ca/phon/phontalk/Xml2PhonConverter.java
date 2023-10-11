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
import java.util.Locale;
import java.util.logging.*;


import ca.phon.phontalk.tb2phon.TalkbankReader;
import jakarta.xml.bind.ValidationException;
import org.antlr.runtime.*;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import ca.phon.session.Session;
import ca.phon.session.io.*;
import ca.phon.syllabifier.SyllabifierLibrary;

import javax.xml.stream.XMLStreamException;

/**
 * Reads in a talkbank XML file using SAX and
 * JAXB.  Converts the file into a PhonBank v1.1 file.
 * 
 */
public class Xml2PhonConverter {
	
	private final static Logger LOGGER = 
			Logger.getLogger(Xml2PhonConverter.class.getName());
	
	private File inputFile = null;
	
	private Xml2PhonSettings settings;
	
	public Xml2PhonConverter() {
		super();
	}
	
	public File getInputFile() {
		return this.inputFile;
	}
	
	public void setInputFile(File inputFile) {
		this.inputFile = inputFile;
	}
	
	public Xml2PhonSettings getSettings() {
		return (this.settings == null ? new Xml2PhonSettings() : this.settings);
	}

	public void setSettings(Xml2PhonSettings settings) {
		this.settings = settings;
	}

	/**
	 * Convert stream to session
	 *
	 * @param stream
	 * @param listener
	 *
	 * @return session or null if stream had conversion errors
	 */
	public Session convertStream(InputStream stream, PhonTalkListener listener) {
		// TODO error handling
		final TalkbankReader reader = new TalkbankReader();
		try {
			return reader.readStream(stream);
		} catch (XMLStreamException e) {
			listener.message(new PhonTalkMessage(e.getLocalizedMessage(), PhonTalkMessage.Severity.SEVERE));
		}
		return null;
	}


	/**
	 * Convert the given talkbank file and save as the
	 * given phone file.
	 * 
	 * @param inputFile
	 * @param outputFile
	 * @param listener
	 */
	public void convertFile(File inputFile, File outputFile, PhonTalkListener listener) {
		this.inputFile = inputFile;
		
		// first try to validate the input file
		final TalkbankValidator validator = new TalkbankValidator();
		final DefaultErrorHandler handler = new DefaultErrorHandler(inputFile, listener);
		
		// turn off validation of hacked version
		try {
			if(!validator.validate(inputFile, handler)) return;
		} catch (ValidationException e1) {
			if(PhonTalkUtil.isVerbose()) e1.printStackTrace();
			final PhonTalkError err = new PhonTalkError(e1.getMessage(), e1);
			err.setFile(inputFile);
			if(listener != null) listener.message(err);
			return;
		}
		
		Session session = null;
		final TalkbankReader talkbankReader = new TalkbankReader();
		try {
			session = talkbankReader.readFile(inputFile.getAbsolutePath());
		} catch (IOException | XMLStreamException e) {
			LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}

        if(session != null) {
			// save the transcript to the given file (also validates)
			try (FileOutputStream fout = new FileOutputStream(outputFile)) {
				// set session name so Phon 2.1.8- can read files
				String name = outputFile.getName();
				if(name.indexOf('.') > 0) {
					name = name.substring(0, name.indexOf('.'));
				}
				session.setName(name);
				
				final SessionOutputFactory sessionOutputFactory =
						new SessionOutputFactory();
				final SessionWriter writer = sessionOutputFactory.createWriter();
				
				writer.writeSession(session, fout);
			} catch (IOException e) {
				if(PhonTalkUtil.isVerbose()) e.printStackTrace();
				final PhonTalkError err = new PhonTalkError(e);
				err.setFile(outputFile);
				if(listener != null) listener.message(err);
				return;
			}
		}
	}

}
