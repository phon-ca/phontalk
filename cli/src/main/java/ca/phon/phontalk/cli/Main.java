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
package ca.phon.phontalk.cli;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import ca.phon.orthography.Orthography;
import ca.phon.session.io.xml.XMLFragments;
import ca.phon.syllabifier.Syllabifier;
import ca.phon.syllabifier.SyllabifierLibrary;
import ca.phon.util.Language;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
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
	
	private final static String CMD_STRING = "java -jar phontalk-cli.jar [options]";
	
	private static Options getCLIOptions() {
		final Options retVal = new Options();

		// files
		retVal.addOption("f", "file", true,
				"""
						Input file xml file, start elements must be one of: 
							* {https://phon.ca/ns/session}session - output will be an xml file with start element {http://www.talkbank.org/ns/talkbank}CHAT
						    * {http://www.talkbank.org/ns/talkbank}CHAT - output will be an xml file with start element {https://phon.ca/ns/session}session
						    * {https://phon.ca/ns/session}ipa - output will be the ipa transcription
						    * {https://phon.ca/ns/session}u - output will be main line of utterance
						If no namespace is provided it is assumed to be https://phon.ca/ns/session
						""");
		retVal.getOption("f").setRequired(false);
		retVal.addOption("o", "output", true,
				"""
						Output file, if unspecified data will be written to stdout""");
		retVal.getOption("o").setRequired(false);

		// u <-> xml
		retVal.addOption("u", "utterance", true,
				"""
						Produce xml fragment for main line utterance.  May be combined with -mod -pho -mor -gra -trn -grt""");
		retVal.getOption("u").setRequired(false);
		retVal.getOption("u").setOptionalArg(false);

		// ipa <-> xml
		retVal.addOption("ipa", "IPATranscript", true,
				"""
						Product xml fragment for given ipa transcription.  Does not combine with -u""");
		retVal.getOption("ipa").setRequired(false);
		retVal.getOption("ipa").setOptionalArg(false);

		retVal.addOption("mod", "IPATarget", true,
				"""
						Add ipa transcript for %mod tier. Requires -u""");
		retVal.getOption("mod").setRequired(false);
		retVal.getOption("mod").setOptionalArg(false);

		retVal.addOption("pho", "IPAActual", true,
				"""
						Add ipa transcript for %. Requires -u""");
		retVal.getOption("pho").setRequired(false);
		retVal.getOption("pho").setOptionalArg(false);

		// mor tiers
		retVal.addOption("mor", "Morphology", true,
				"""
						Add %mor tier to utterance. Requires -u""");
		retVal.getOption("mor").setRequired(false);
		retVal.getOption("mor").setOptionalArg(false);

		retVal.addOption("gra", "GRASP", true,
				"""
						Add %gra tier to utterance. Requires -mor""");
		retVal.getOption("gra").setRequired(false);
		retVal.getOption("gra").setOptionalArg(false);

		retVal.addOption("trn", "Morphology", true,
				"""
						Add %trn tier to utterance. Requires -u""");
		retVal.getOption("trn").setRequired(false);
		retVal.getOption("trn").setOptionalArg(false);

		retVal.addOption("grt", "GRASP", true,
				"""
						Add %grt tier to utterance. Requires -trn""");
		retVal.getOption("grt").setRequired(false);
		retVal.getOption("grt").setOptionalArg(false);

		// syllabifiers
		retVal.addOption("sb", "syllabifier", true,
				"""
						Syllabifier language, if provided IPA data will be 'syllabified' before
						xml fragment is produced""");
		retVal.getOption("sb").setRequired(false);
		retVal.getOption("sb").setOptionalArg(false);

		retVal.addOption("lsb", "list-syllabifiers", false,
				"""
						List available syllabifier languages and exit.  This supersedes all other options.""");
		retVal.getOption("lsb").setRequired(false);

		retVal.addOption("h", "help", false, "Show usage info");
		retVal.getOption("h").setRequired(false);
		
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

			if(cmdLine.hasOption("h") || cmdLine.getOptions().length == 0) {
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(CMD_STRING, getCLIOptions());

				System.exit(0);
			}

			if(cmdLine.hasOption("lsb")) {
				// list syllabifiers and exit
				final SyllabifierLibrary library = SyllabifierLibrary.getInstance();
				final StringBuilder builder = new StringBuilder();
				builder.append("Available syllabifiers: ");
				for(Language lang:library.availableSyllabifierLanguages()) {
					builder.append(" ").append(lang);
				}
				builder.append("\n");
				System.out.println(builder.toString());
				System.exit(0);
			}

			if(cmdLine.hasOption("u") && cmdLine.hasOption("ipa")) {
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(CMD_STRING, getCLIOptions());
				System.err.println("-u and -ipa must be used independently");
				System.exit(1);
			}

			// default mode is full file conversion
			final String inputFile = cmdLine.hasOption("f") ? cmdLine.getOptionValue("f") : null;
			String mode = "phontalk";
			if(cmdLine.hasOption("u")) {
				if(inputFile != null) {
					final HelpFormatter formatter = new HelpFormatter();
					formatter.printHelp(CMD_STRING, getCLIOptions());
					throw new IllegalArgumentException("Cannot use -u with input file");
				}
				mode = "u";
			} else if(cmdLine.hasOption("ipa")) {
				if(inputFile != null) {
					final HelpFormatter formatter = new HelpFormatter();
					formatter.printHelp(CMD_STRING, getCLIOptions());
					throw new IllegalArgumentException("Cannot use -ipa with input file");
				}
				mode = "ipa";
			}

			if("phontalk".equals(mode)) {
				if(inputFile == null) {
					final HelpFormatter formatter = new HelpFormatter();
					formatter.printHelp(CMD_STRING, getCLIOptions());
					throw new IllegalArgumentException("No input file given");
				}
			}

			final String outputFile = cmdLine.hasOption("o") ? cmdLine.getOptionValue("o") : null;
			switch (mode) {
				case "u":
					try {
						String utterance = cmdLine.getOptionValue("u");
						outputUtteranceFramgent(utterance, outputFile);
					} catch (IOException e) {
						e.printStackTrace(new PrintWriter(System.err));
						System.exit(2);
					}
					break;

				case "ipa":
					break;

				default:
					final String[] fileArgs = cmdLine.getArgs();
					if(fileArgs.length != 0) {
						final HelpFormatter formatter = new HelpFormatter();
						formatter.printHelp(CMD_STRING, getCLIOptions());
						throw new IllegalArgumentException("Too many arguments");
					}

					// get the type of the input file
		//			final String rootEleName = getRootElementName(inputFile);
		//			if(rootEleName.equalsIgnoreCase("CHAT")) {
		//				// processing xml->phon
		//				final Xml2PhonConverter converter = new Xml2PhonConverter();
		//				converter.convertFile(inputFile, outputFile, new DefaultPhonTalkListener());
		//			} else if(rootEleName.equalsIgnoreCase("session")) {
		//				// processing phon->xml
		//				final Phon2XmlConverter converter = new Phon2XmlConverter();
		//				converter.convertFile(inputFile, outputFile, new DefaultPhonTalkListener());
		//			} else {
		//
		//				throw new UnrecognizedOptionException("Input file type not support.");
		//			}
			}

			// ensure no other arguments
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(CMD_STRING, getCLIOptions());
			
			System.exit(1);
		}
		
	}

	private static void outputUtteranceFramgent(String utterance, String outputFile) throws IOException {
		try {
			final Orthography orthography = Orthography.parseOrthography(utterance);
			final String xml = XMLFragments.toXml(orthography, false, false);
			outputData(xml, outputFile);
		} catch (java.text.ParseException e) {
			throw new IOException(e);
		}
	}

	private static void outputData(String data, String outputFile) throws IOException {
		try(BufferedOutputStream out = new BufferedOutputStream(outputFile == null ? System.out : new FileOutputStream(outputFile))) {
			out.write(data.getBytes(StandardCharsets.UTF_8));
			out.write('\n');
			out.flush();
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
