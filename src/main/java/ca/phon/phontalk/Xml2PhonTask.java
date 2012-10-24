package ca.phon.phontalk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import ca.phon.application.PhonTask;
import ca.phon.application.project.IPhonProject;
import ca.phon.application.transcript.ITranscript;
import ca.phon.engines.syllabifier.Syllabifier;
import ca.phon.system.logger.PhonLogger;

/**
 * 
 *
 */
public class Xml2PhonTask extends PhonTask {
	
	/** The input stream */
	private TalkbankSource source;
	
	private IPhonProject project;
	
	private Syllabifier syllabifier;
	
	/** Should the phone list be re-parsed during import? */
	private boolean reparsePhones = false;
	
	/** Property name for the generated session */
	public static final String SESSION_PROP = "_session_";
	
	public Xml2PhonTask(TalkbankSource source, IPhonProject project) {
		super("");

		this.source = source;
		this.project = project;
	}
	
	public Syllabifier getSyllabifier() {
		return syllabifier;
	}
	
	public void setSyllabifier(Syllabifier syllabifier) {
		this.syllabifier = syllabifier;
	}
	
	public boolean isReparsePhones() {
		return reparsePhones;
	}
	
	public void setReparsePhones(boolean v) {
		this.reparsePhones = v;
	}

	@Override
	public void performTask() {
		super.setStatus(TaskStatus.RUNNING);
		
		TalkbankValidator validator = new TalkbankValidator();
		
		// go through each file in the source
		// and add it to the project
		for(File f:source.listTalkbankFiles()) {
			PhonLogger.info("********");
			try {
				PhonLogger.info("Validating file '" + f.getPath() + "'");
				if(!validator.validate(f)) {
					PhonLogger.warning("Validation failed for file '" + f.getPath() + "'");
					continue;
				}
				
				// attept to validate the file first
				PhonLogger.info("Processing file '" + f.getPath() + "'");
				InputStream in = source.toInputStream(f);
				
				File parentDirectory = f.getParentFile();
				String corpusName = parentDirectory.getName();
				
				TalkbankConverter converter = new TalkbankConverter();
				converter.setProperty(XmlConverter.CONVERTER_PROP_SYLLABIFIER, syllabifier);
				converter.setProperty(XmlConverter.CONVERTER_PROP_REPARSE_PHONES, new Boolean(reparsePhones));
				
				ITranscript t = converter.convertStream(in);

				if(t == null) {
					PhonLogger.severe("Error processing file '" + f.getPath() + "'");
					continue;
				}
				
				
				t.setCorpus(corpusName);
				if(t.getMediaLocation() == null)
					t.setMediaLocation("");
				
				// get the corpus and id from the transcript
				String corpus = t.getCorpus();
				if(corpus == null || corpus.length() == 0) {
					corpus = "Undefined";
					t.setCorpus(corpus);
				}
				String session = t.getID();
				if(session == null || session.length() == 0) {
					session = "Undefined";
					t.setID(session);
				}
				
				boolean createCorpus = true;
				for(String c:project.getCorpora()) {
					if(c.equals(corpus)) {
						createCorpus = false;
						break;
					}
				}
				if(createCorpus) project.newCorpus(corpus, "");
				
				project.newTranscript(corpus, session);
				int writeLock = project.getTranscriptWriteLock(corpus, session);
				project.saveTranscript(t, writeLock);
				project.releaseTranscriptWriteLock(corpus, session, writeLock);

				
				
			} catch (Exception e) {
				e.printStackTrace();
				PhonLogger.warning(e.toString());
				PhonLogger.info("Failed to process '" + f.getPath() + "'");
			}
		}
		
		try {
			project.save();
		} catch (IOException e) {
			PhonLogger.severe(e.toString());
			super.setStatus(TaskStatus.ERROR);
			super.err = e;
			return;
		}
		
		super.setStatus(TaskStatus.FINISHED);
	}

}
