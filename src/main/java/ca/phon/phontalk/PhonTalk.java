package ca.phon.phontalk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import ca.phon.application.PhonTask.TaskStatus;
import ca.phon.application.project.IPhonProject;
import ca.phon.application.project.PhonProject;
import ca.phon.engines.syllabifier.Syllabifier;
import ca.phon.phontalk.ui.PhonTalkWizard;
import ca.phon.system.logger.PhonLogger;
import ca.phon.util.PhonUtilities;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
/**
 * Application start for the PhonTalk application.
 * 
 * If no arguments are given to the main method, the wizard ui
 * is displayed.
 * 
 * Command line options:
 *  -m --mode <wizard:xml2phon|phon2xml>  default: wizard
 *  -i --input <file/directory for input>
 *  -o --output <file/directory for output>
 *  
 * 
 */
public class PhonTalk {

	/**
	 * Version info
	 */
	public static final String PHONTALK_MAJ_VERSION = "1";

	// replaced at compile time with changeset
	public static final String PHONTALK_MIN_VERSION = "3";

	public static final String PHONTALK_VERSION =
			PHONTALK_MAJ_VERSION + "." + PHONTALK_MIN_VERSION;

	public static final String PHONTALK_COMPILE_DATE = "2011-07-21";
	
	public static void main(String[] args) {
		PhonLogger.startLogging();
		
		if(args.length == 0) {
			// show wizard
			showWizard();
		} else {
			// process args
			Options cmdLineOptions = getCommandLineOptions();
			CommandLineParser cmdLineParser = new PosixParser();
			
			HelpFormatter helpFormatter = new HelpFormatter();
			try {
				CommandLine cmdLine = cmdLineParser.parse(cmdLineOptions, args);
				
				// check arguments
				String mode = "wizard";
				if(cmdLine.hasOption("mode")) {
					mode = cmdLine.getOptionValue("mode");
					
					if(!mode.equals("xml2phon") && !mode.equals("phon2xml")) {
						PhonLogger.severe("Invalid mode type '" + mode + "'");
						// print help
						helpFormatter.printHelp( "phontalk" , cmdLineOptions);
						System.exit(1);
					}
				}
				
				if(mode.equalsIgnoreCase("wizard")) {
					showWizard();
				} else {
					String inPath = null;
					if(cmdLine.hasOption("input")) {
						inPath = cmdLine.getOptionValue("input");
						
						// check to make sure path exists
						File inFile = new File(inPath);
						if(!inFile.exists()) {
							PhonLogger.severe(inPath + " not found.");
							System.exit(1);
						}
					}
					
					String outPath = null;
					if(cmdLine.hasOption("output")) {
						outPath = cmdLine.getOptionValue("output");
					}
					
					File inFile = (inPath != null ? new File(inPath) : null);
					File outFile = (outPath != null ? new File(outPath) : null);
					
					// if working on files...
					if(inFile == null || !inFile.isDirectory()) {
						if(mode.equals("xml2phon")) {
							InputStream inStream = null;
							if(inFile == null)
								inStream = System.in;
							else {
								try {
									inStream = new FileInputStream(inFile);
								} catch (IOException e) {
									e.printStackTrace();
									System.exit(2);
								}
							}
							if(inStream == null) {
								System.err.println("No input");
								System.exit(2);
							}
							
							OutputStream outStream = null;
							if(outFile == null)
								outStream = System.out;
							else {
								try {
									outStream = new FileOutputStream(outFile);
								} catch (IOException e) {
									e.printStackTrace();
									System.exit(2);
								}
							}
							
							Xml2PhonStreamTask task = new Xml2PhonStreamTask(inStream, outStream);
							task.performTask();
							
							if(task.getStatus() == TaskStatus.ERROR)
								System.exit(2);
						} else if(mode.equals("phon2xml")) {
							InputStream inStream = null;
							if(inFile == null)
								inStream = System.in;
							else {
								try {
									inStream = new FileInputStream(inFile);
								} catch (IOException e) {
									e.printStackTrace();
									System.exit(2);
								}
							}
							if(inStream == null) {
								System.err.println("No input");
								System.exit(2);
							}
							
							OutputStream outStream = null;
							if(outFile == null)
								outStream = System.out;
							else {
								try {
									outStream = new FileOutputStream(outFile);
								} catch (IOException e) {
									e.printStackTrace();
									System.exit(2);
								}
							}
								
							Phon2XmlStreamTask task = new Phon2XmlStreamTask(inStream, outStream);
							task.performTask();
							
							if(task.getStatus() == TaskStatus.ERROR)
								System.exit(2);
						}
					} else {
						// working on regular projects
						if(mode.equals("xml2phon")) {
							try {
								IPhonProject project = PhonProject.newProject(outPath);
								project.setProjectName(inFile.getName());
								
								if(cmdLine.hasOption("name")) {
									project.setProjectName(cmdLine.getOptionValue("name"));
								}
								project.save();
								
								TalkbankSource tbSrc = new DirectoryTalkbankSource(inPath);
								Xml2PhonTask task = new Xml2PhonTask(tbSrc, project);
								
								String syllabifier = null;
								if(cmdLine.hasOption('s')) {
									syllabifier = cmdLine.getOptionValue('s');
								}
								
								if(cmdLine.hasOption("parseipa")) {
									task.setReparsePhones(true);
								}
								
								if(syllabifier != null) {
									Syllabifier syllab = Syllabifier.getInstance(syllabifier);
									if(syllab != null) {
										task.setSyllabifier(syllab);
									}
								}
								
								task.performTask();
								
								if(task.getStatus() == TaskStatus.ERROR)
									System.exit(2);
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else if(mode.equals("phon2xml")) {
							try {
								IPhonProject project = PhonProject.fromFile(inPath);
								Phon2XmlTask task = new Phon2XmlTask(project, outFile);
								task.performTask();
								
								if(task.getStatus() == TaskStatus.ERROR)
									System.exit(2);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
				
			} catch (ParseException e) {
				PhonLogger.severe(e.toString());
				return;
			}
		}
		
	}
	
	/*
	 * Show wizard
	 */
	private static void showWizard() {
		// load the application helpers - mac
		if(PhonUtilities.isMacOs()) {
			// load the mac app helper through reflection so
			// this class will still compile in Windows
		
			try {
				Object appHelper = Class.forName("ca.phon.phontalk.macos.MacOSAppHelper").newInstance(); //$NON-NLS-1$
//				comps.add(appHelper);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		
		}

		PhonTalkWizard wizard = new PhonTalkWizard("PhonTalk (version " +
				PHONTALK_VERSION + ")");
		wizard.setDefaultCloseOperation(PhonTalkWizard.EXIT_ON_CLOSE);
		wizard.setSize(600, 450);
		wizard.centerWindow();
		wizard.setVisible(true);
	}
	
	/*
	 * Get command line options
	 */
	private static Options getCommandLineOptions() {
		Options retVal = new Options();
		
		retVal.addOption("m", "mode", true,
				"Conversion mode.  Either 'xml2phon' or 'phon2xml'");
		retVal.getOption("mode").setRequired(false);
		
		retVal.addOption("i", "input", true,
				"Input directory/file");
		retVal.getOption("input").setRequired(false);
		
		retVal.addOption("o", "output", true, 
				"Output directory/file");
		retVal.getOption("output").setRequired(false);
		
		retVal.addOption("s", "syllabifier", true,
				"Syllabifier name/definition file (xml2phon only).  Default 'English (simple)'");
		retVal.getOption("syllabifier").setRequired(false);
		
		retVal.addOption("n", "name", true,
				"Project name (xml2phon only).");
		retVal.getOption("name").setRequired(false);
		
//		retVal.addOption("parseipa", false, "Reparse phones list on import (xml2phon only).");
//		retVal.getOption("parseipa").setRequired(false);
		
		return retVal;
	}
	
	/*
	 * Get a reader for stdin
	 */
	private static BufferedReader getStdinReader(String charEncoding) 
		throws IOException {
		InputStreamReader isReader =
			new InputStreamReader(System.in, charEncoding);
		BufferedReader retVal = 
			new BufferedReader(isReader);
		return retVal;
	}
	
	/*
	 * Get a writer for stdout
	 */
	private static BufferedWriter getStdoutWriter(String charEncoding)
		throws IOException {
		OutputStreamWriter osWriter =
			new OutputStreamWriter(System.out, charEncoding);
		BufferedWriter retVal =
			new BufferedWriter(osWriter);
		return retVal;
	}
}
