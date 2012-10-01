package ca.phon.phontalk;

import ca.phon.application.transcript.validation.TranscriptValidationEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;

import ca.phon.application.PhonTask;
import ca.phon.application.project.IPhonProject;
import ca.phon.application.transcript.ITranscript;
import ca.phon.application.transcript.validation.OneToOneValidator;
import ca.phon.application.transcript.validation.TranscriptValidationListener;
import ca.phon.application.transcript.validation.TranscriptValidator;
import ca.phon.system.logger.PhonLogger;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Phon2XmlTask extends PhonTask {
	
	/* default suffix */
	private final String suffix = ".xml";
	
	
	/** The project */
	private IPhonProject project;
	
	/** The output directory */
	private File outDir;
	
	public Phon2XmlTask(IPhonProject project, File outDir) {
		super();
		
		this.project = project;
		this.outDir = outDir;
	}

	@Override
	public void performTask() {
		super.setStatus(TaskStatus.RUNNING);
		
		Phon2XmlConverter converter = new Phon2XmlConverter();
		TalkbankValidator validator = new TalkbankValidator();
		
		// create project dir
//		File projDir = new File(outDir);
//		File projDir = new File(outDir, project.getProjectName() + "-phon2xml");
//		if(projDir.exists() && !projDir.isDirectory()) {
//			PhonLogger.severe("Location " + projDir.getAbsolutePath() + " exists but is not a directory.");
//			return;
//		}
		if(!outDir.exists()) {
			if(!outDir.mkdir()) {
				PhonLogger.severe("Could not create directory " + outDir.getAbsolutePath());
				return;
			}
		}
		
		for(String corpus:project.getCorpora()) {
			for(String session:project.getCorpusTranscripts(corpus)) {

				PhonLogger.info("********");

				File corpusFile = new File(outDir, corpus);
				if(corpusFile.exists() && !corpusFile.isDirectory()) {
					PhonLogger.severe("Location " + corpusFile.getAbsolutePath() + " exists but is not a directory.");
					continue;
				}
				if(!corpusFile.exists()) {
					if(!corpusFile.mkdir()) {
						PhonLogger.severe("Could not create directory " + corpusFile.getAbsolutePath());
					}
				}
				
				File sessionFile = new File(corpusFile, session + suffix);
				if(sessionFile.exists()) {
					PhonLogger.warning("Overwriting file " + sessionFile.getAbsolutePath());
				}
				
				try {
					PhonLogger.info("Processing session '" + corpus + "." + session + "'");
					ITranscript t = project.getTranscript(corpus, session);

					// validate transcript before conversion
//					TranscriptValidator o2oValidator =
//							new OneToOneValidator();
//					o2oValidator.addValidationListener(new TranscriptValidationListener() {
//
//						public void validationEvent(TranscriptValidationEvent ve) {
//							String msg =
//									"Record #" + (ve.getRecordIndex()+1);
//							msg += ": " + ve.getMessage();
//
//							PhonLogger.warning(msg);
//						}
//
//					});
					
//					boolean isValid =
//							o2oValidator.validateTranscript(t);

					String xml = converter.convertTranscript(t);

					// validate
//					PhonLogger.info("Validating XML...");
//					if(!validator.validate(xml)) {
//						PhonLogger.warning("XML failed validation.");
//					} else {
						PhonLogger.info("Writing file " + sessionFile.getAbsolutePath());
						// write new file
						BufferedWriter out = new BufferedWriter(
								new OutputStreamWriter(new FileOutputStream(sessionFile), "UTF-8"));
						out.write(xml);
						out.flush();
						out.close();
//					}

						SAXSource src = new SAXSource(new InputSource(new FileInputStream(sessionFile)));
						validator.validate(src,
								new TalkbankValidator.ValidationHandler(sessionFile) );
				} catch (IOException e) {
					PhonLogger.severe(e.toString());
				} catch (StackOverflowError err) {
					// stack overflow can occur during the toString() method of StringTemplate if
					// something is wrong
					PhonLogger.severe(err.toString());
					if(err.getCause() != null)
						PhonLogger.severe(err.getCause().toString());
					PhonLogger.severe("Could not convert '" + corpus + "." + session + "'");
				}
			}
		}
		
		super.setStatus(TaskStatus.FINISHED);
	}

//	/**
//	 * Validates given XML against the talkbank schema.
//	 * 
//	 * @param the xml as a string
//	 * @return true if valid XML, false otherwise.  Errors are reported
//	 * in PhonLogger.
//	 */
//	private boolean validateXML(String xml) {
//		boolean retVal = true;
//		
//		Source schemaSource = null;
//		
//		// look for the talkbank schema in ./ and data/
//		File localSchemaFile = new File("talkbank.xsd");
//		if(!localSchemaFile.exists()) {
//			localSchemaFile = new File("data", "talkbank.xsd");
//			if(!localSchemaFile.exists()) {
//				// tell the user we are using online schema
//				PhonLogger.info("Using schema at " + defaultTalkbankSchemaLoc);
//				
//				try {
//					URL schemaURL = new URL(defaultTalkbankSchemaLoc);
//					HttpURLConnection connection = (HttpURLConnection)schemaURL.openConnection();
//			          connection.setRequestMethod("GET");
//			          connection.setDoOutput(true);
//			          connection.setReadTimeout(10000);
//			        if(connection.getResponseCode() == 200) {
//			        	schemaSource = new StreamSource(connection.getInputStream());
//			        }
//				} catch (MalformedURLException e) {
//					PhonLogger.severe(e.toString());
//					retVal = false;
//				} catch (IOException e) {
//					PhonLogger.severe(e.toString());
//					retVal = false;
//				}
//				
//			} else {
//				schemaSource = new StreamSource(localSchemaFile);
//			}
//		} else {
//			schemaSource = new StreamSource(localSchemaFile);
//		}
//		
//		if(schemaSource == null) {
//			PhonLogger.severe("Talkbank schema not found.");
//			return false;
//		}
//		
//		try {
//			System.out.println("-------DEBUG-------");
//			System.out.println(xml);
//			System.out.println("-------DEBUG-------");
//			ByteArrayInputStream bin = new ByteArrayInputStream(xml.getBytes("UTF-8"));
//
//			// convert into a DOM tree
//			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//		    Document document = parser.parse(bin);
//
//		    // create a SchemaFactory capable of understanding WXS schemas
//		    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
//		    Schema schema = factory.newSchema(schemaSource);
//		    
//		    // create a validator instance
//		    Validator validator = schema.newValidator();
//		    validator.validate(new DOMSource(document));
//		    
//		} catch (ParserConfigurationException e) {
//			PhonLogger.severe(e.toString());
//			retVal = false;
//		} catch (SAXException e) {
//			PhonLogger.severe(e.toString());
//			retVal = false;
//		} catch (IOException e) {
//			PhonLogger.severe(e.toString());
//			retVal = false;
//		}
//
//		
//		return retVal;
//	}
	
}
