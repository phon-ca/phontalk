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
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import ca.phon.orthography.Orthography;
import ca.phon.orthography.mor.GraspTierData;
import ca.phon.orthography.mor.MorTierData;
import ca.phon.session.Record;
import ca.phon.session.SessionFactory;
import ca.phon.session.Tier;
import ca.phon.session.UserTierType;
import ca.phon.session.io.xml.OneToOne;
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

	private final static String PHON_NAMESPACE = "https://phon.ca/ns/session";

	private final static String CHAT_NAMESPACE = "http://www.talkbank.org/ns/talkbank";
	
	private static Options getCLIOptions() {
		final Options retVal = new Options();

		// files
		retVal.addOption("f", "file", true,
				"""
						Input file xml file, start elements must be one of:
							* {https://phon.ca/ns/session}session - output will be an xml file with start element {http://www.talkbank.org/ns/talkbank}CHAT
						    * {http://www.talkbank.org/ns/talkbank}CHAT - output will be an xml file with start element {https://phon.ca/ns/session}session
						""");
		retVal.getOption("f").setRequired(false);
		retVal.addOption("o", "output", true,
				"""
						Output file, required when using -f""");
		retVal.getOption("o").setRequired(false);

		// u <-> xml
		retVal.addOption("u", "utterance", true,
				"""
						Produce xml fragment for main line utterance.  May be combined with -mod -pho -mor -gra -trn -grt""");
		retVal.getOption("u").setRequired(false);
		retVal.getOption("u").setOptionalArg(false);

		// ipa <-> xml
//		retVal.addOption("ipa", "IPATranscript", true,
//				"""
//						Product xml fragment for given ipa transcription.  Does not combine with -u""");
//		retVal.getOption("ipa").setRequired(false);
//		retVal.getOption("ipa").setOptionalArg(false);

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

		retVal.addOption("m", "formatted", false, """
				Output formatted xml.""");
		retVal.getOption("m").setRequired(false);

		retVal.addOption("n", "namespace", false, """
						Include xml namespace.""");

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

			if(cmdLine.hasOption("u") && cmdLine.hasOption("f")) {
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(CMD_STRING, getCLIOptions());
				System.err.println("-u and -f cannot be used together");
				System.exit(1);
			}

			// default mode is full file conversion
			final String inputFile = cmdLine.hasOption("f") ? cmdLine.getOptionValue("f") : null;
			String mode = "phontalk";
			final String outputFile = cmdLine.hasOption("o") ? cmdLine.getOptionValue("o") : null;
			if(inputFile == null && cmdLine.hasOption("u")) {
				mode = "fragment";
			}

			if("phontalk".equals(mode)) {
				if(inputFile == null) {
					final HelpFormatter formatter = new HelpFormatter();
					formatter.printHelp(CMD_STRING, getCLIOptions());
					throw new IllegalArgumentException("No input file given");
				} else if(outputFile == null) {
					final HelpFormatter formatter = new HelpFormatter();
					formatter.printHelp(CMD_STRING, getCLIOptions());
					throw new IllegalArgumentException("No output file given");
				}
			}

			boolean formattedOutput = cmdLine.hasOption("m");

			switch (mode) {
				case "fragment":
					try {
						String utterance = cmdLine.getOptionValue("u");
						String mod = cmdLine.getOptionValue("mod", "");
						String pho = cmdLine.getOptionValue("pho", "");
						String mor = cmdLine.getOptionValue("mor", "");
						String gra = cmdLine.getOptionValue("gra", "");
						String trn = cmdLine.getOptionValue("trn", "");
						String grt = cmdLine.getOptionValue("grt", "");
						outputUtteranceFramgent(utterance, mod, pho, mor, gra, trn, grt, outputFile, formattedOutput);
					} catch (IOException e) {
						e.printStackTrace(new PrintWriter(System.err));
						System.exit(2);
					}
					break;

				case "phontalk":
				default:
					final String[] fileArgs = cmdLine.getArgs();
					// ensure no other arguments
					if(fileArgs.length != 0) {
						final HelpFormatter formatter = new HelpFormatter();
						formatter.printHelp(CMD_STRING, getCLIOptions());
						throw new IllegalArgumentException("Too many arguments");
					}

					// get the type of the input file
					try {
						final QName rootEleName = getRootElementName(new File(inputFile));
						if (rootEleName.equals(new QName(CHAT_NAMESPACE, "CHAT"))) {
							// processing xml->phon
							final Xml2PhonConverter converter = new Xml2PhonConverter();
							converter.convertFile(new File(inputFile), new File(outputFile), new DefaultPhonTalkListener());
						} else if (rootEleName.equals(new QName(PHON_NAMESPACE, "session"))) {
							// processing phon->xml
							final Phon2XmlConverter converter = new Phon2XmlConverter();
							converter.convertFile(new File(inputFile), new File(outputFile), new DefaultPhonTalkListener());
						} else {
							throw new UnrecognizedOptionException("Input file type not support.");
						}
					} catch (IOException e) {
						throw new IllegalArgumentException(e);
					}
			}

		} catch (ParseException e) {
			System.err.println(e.getMessage());
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(CMD_STRING, getCLIOptions());
			
			System.exit(1);
		}
		
	}

	/**
	 *
	 */
	private static void outputUtteranceFramgent(String utterance, String mod, String pho,
												String mor, String gra, String trn, String grt, String outputFile, boolean formatted) throws IOException {
		final SessionFactory factory = SessionFactory.newFactory();
		final Record record = factory.createRecord();
		try {
			record.getOrthographyTier().setText(utterance);
			if(record.getOrthographyTier().isUnvalidated()) throw record.getOrthographyTier().getUnvalidatedValue().getParseError();
			record.getIPATargetTier().setText(mod);
			if(record.getIPATargetTier().isUnvalidated()) throw record.getIPATargetTier().getUnvalidatedValue().getParseError();
			record.getIPAActualTier().setText(pho);
			if(record.getIPAActualTier().isUnvalidated()) throw record.getIPAActualTier().getUnvalidatedValue().getParseError();
			if(!mor.isBlank()) {
				final Tier<MorTierData> morTier = factory.createTier(UserTierType.Mor.getTierName(), MorTierData.class, new HashMap<>(), true);
				morTier.setText(mor);
				if(morTier.isUnvalidated()) throw morTier.getUnvalidatedValue().getParseError();
				record.putTier(morTier);

				if(!gra.isBlank()) {
					final Tier<GraspTierData> graTier = factory.createTier(UserTierType.Gra.getTierName(), GraspTierData.class, new HashMap<>(), true);
					graTier.setText(gra);
					if(graTier.isUnvalidated()) throw graTier.getUnvalidatedValue().getParseError();
					record.putTier(graTier);
				}
			}
			if(!trn.isBlank()) {
				final Tier<MorTierData> trnTier = factory.createTier(UserTierType.Trn.getTierName(), MorTierData.class, new HashMap<>(), true);
				trnTier.setText(trn);
				if(trnTier.isUnvalidated()) throw trnTier.getUnvalidatedValue().getParseError();
				record.putTier(trnTier);

				if(!gra.isBlank()) {
					final Tier<GraspTierData> grtTier = factory.createTier(UserTierType.Grt.getTierName(), GraspTierData.class, new HashMap<>(), true);
					grtTier.setText(gra);
					if(grtTier.isUnvalidated()) throw grtTier.getUnvalidatedValue().getParseError();
					record.putTier(grtTier);
				}
			}
			OneToOne.annotateRecord(record);
			final Orthography orthography = record.getOrthography();
			final String xml = XMLFragments.toXml(orthography, false, formatted);
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
	
	private static QName getRootElementName(File f)
		throws IOException {
		QName retVal = null;
		
		final FileInputStream fin = new FileInputStream(f);
		final XMLInputFactory factory = XMLInputFactory.newFactory();
		try {
			final XMLStreamReader xmlStreamReader =
					factory.createXMLStreamReader(fin);
			
			while(xmlStreamReader.hasNext()) {
				final int nextType = xmlStreamReader.next();
				if(nextType == XMLStreamReader.START_ELEMENT) {
					// get the element name and break
					retVal = new QName(xmlStreamReader.getNamespaceURI(), xmlStreamReader.getLocalName());
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
