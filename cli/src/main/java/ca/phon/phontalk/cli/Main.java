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
package ca.phon.phontalk.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;

import ca.phon.phontalk.DefaultPhonTalkListener;
import ca.phon.phontalk.Phon2XmlConverter;
import ca.phon.phontalk.Xml2PhonConverter;

/**
 * Entry point for PhonTalk CLI.
 * 
 * The CLI accepts an input file and output file path as
 * arguments.  It may also accept an optional '-v' switch
 * for turning on verbose output.  The input file is first
 * scanned to see if it is either a TablkBank or PhonBank file.
 * The appropriate conversion is then automatically selected.
 * 
 */
public class Main {
	
	private final static String CMD_STRING = "java -jar phontalk.jar [options] <input file> <output file>";
	
	private static Options getCLIOptions() {
		final Options retVal = new Options();
		
		// add option for verbose output
		retVal.addOption("v", false, "Enable verbose output");
		retVal.getOption("v").setRequired(false);
		
		retVal.addOption("?", false, "Show usage info");
		retVal.getOption("?").setRequired(false);
		
		return retVal;
	}

	/**
	 * Main method.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		final CommandLineParser parser = new PosixParser();
		try {
			final CommandLine cmdLine = parser.parse( getCLIOptions(), args );
			
			if(cmdLine.hasOption("v")) {
				System.setProperty("phontalk.verbose", Boolean.TRUE.toString());
			}
			
			if(cmdLine.hasOption("?")) {
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(CMD_STRING, getCLIOptions());
				
				System.exit(0);
			}
			
			final String[] fileArgs = cmdLine.getArgs();
			if(fileArgs.length == 0) {
				throw new MissingArgumentException("<input file>");
			} else if(fileArgs.length == 1) {
				throw new MissingArgumentException("<output file>");
			} else if(fileArgs.length > 2) {
				throw new UnrecognizedOptionException(fileArgs[2]);
			}
			
			final File inputFile = new File(fileArgs[0]);
			final File outputFile = new File(fileArgs[1]);
			
			// get the type of the input file
			final String rootEleName = getRootElementName(inputFile);
			if(rootEleName.equalsIgnoreCase("CHAT")) {
				// processing xml->phon
				final Xml2PhonConverter converter = new Xml2PhonConverter();
				converter.convertFile(inputFile, outputFile, new DefaultPhonTalkListener());
			} else if(rootEleName.equalsIgnoreCase("session")) {
				// processing phon->xml
				final Phon2XmlConverter converter = new Phon2XmlConverter();
				converter.convertFile(inputFile, outputFile, new DefaultPhonTalkListener());
			} else {
				throw new UnrecognizedOptionException("Input file type not support.");
			}
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(CMD_STRING, getCLIOptions());
			
			System.exit(1);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
		
	}
	
	private static String getRootElementName(File f) 
		throws IOException {
		String retVal = null;
		
		final FileInputStream fin = new FileInputStream(f);
		final XMLInputFactory factory = XMLInputFactory.newFactory();
		try {
			final XMLStreamReader xmlStreamReader =
					factory.createXMLStreamReader(fin);
			
			while(xmlStreamReader.hasNext()) {
				final int nextType = xmlStreamReader.next();
				if(nextType == XMLStreamReader.START_ELEMENT) {
					// get the element name and break
					retVal = xmlStreamReader.getName().getLocalPart();
					break;
				}
			}
			xmlStreamReader.close();
		} catch (XMLStreamException e) {
			throw new IOException(e);
		}
		
		return retVal;
	}
}
