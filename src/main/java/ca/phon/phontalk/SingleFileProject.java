package ca.phon.phontalk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;

import ca.phon.application.IPhonFactory;
import ca.phon.application.project.IPhonProject;
import ca.phon.application.project.LogData;
import ca.phon.application.project.ProjectListener;
import ca.phon.application.transcript.ITranscript;

/**
 * 'Fake' project class for working on single files.
 *
 */
public class SingleFileProject implements IPhonProject {
	
	private String sessionPath;
	
	public SingleFileProject(String file) {
		this.sessionPath = file;
	}

	@Override
	public String getProjectLocation() {
		// use parent of file
		File sessionFile = new File(sessionPath);
		File sessionParent = sessionFile.getParentFile();
		return sessionParent.getAbsolutePath();
	}

	@Override
	public String getProjectVersion() {
		return "1.4";
	}

	@Override
	public void addProjectListener(ProjectListener listener) {
	}

	@Override
	public void removeProjectListener(ProjectListener listener) {
	}

	@Override
	public String getProjectName() {
		// use name of file
		File sessionFile = new File(sessionPath);
		return sessionFile.getName();
	}

	@Override
	public void setProjectName(String name) {
	}

	@Override
	public ArrayList<String> getCorpora() {
		// return a single corpus 'default'
		ArrayList<String> retVal = new ArrayList<String>();
		retVal.add("default");
		return retVal;
	}

	@Override
	public String getCorpusDescription(String corpusName) {
		String retVal = "";
		if(corpusName.equals("default")) 
			retVal = "default corpus";
		return retVal;
	}

	@Override
	public void setCorpusDescription(String corpusName, String description) {
	}

	@Override
	public ArrayList<String> getCorpusTranscripts(String corpusName) {
		ArrayList<String> retVal = new ArrayList<String>();
		if(corpusName.equals("default")) {
			
			File sessionFile = new File(sessionPath);
			String fileName = sessionFile.getName();
			
			int dotIdx = fileName.lastIndexOf('.');
			if(dotIdx >= 0) {
				fileName = fileName.substring(0, dotIdx);
			}
			
			retVal.add(fileName);
		}
		return retVal;
	}

	@Override
	public int getTranscriptWriteLock(String corpus, String transcript) {
		return 1;
	}

	@Override
	public void duplicateTranscript(String corpus, String transcript)
			throws IOException {

	}

	@Override
	public int getTranscriptWriteLock(String corpus, String transcript,
			boolean force) {
		return 1;
	}

	@Override
	public void releaseTranscriptWriteLock(String corpus, String transcript,
			Integer writeLock) {
		

	}

	@Override
	public boolean isTranscriptLocked(String copus, String transcript) {
		return false;
	}

	@Override
	public ITranscript getTranscript(String corpus, String transcript)
			throws IOException {
		ITranscript retVal = null;
		// get the appropriate factory
		IPhonFactory factory = IPhonFactory.getDefaultFactory();
//		if(factory != null) {
			retVal = factory.createTranscript();
			retVal.loadTranscriptData(new FileInputStream(sessionPath));
//		}
		return retVal;
	}

	@Override
	public void saveTranscript(ITranscript transcript, Integer writeLock)
			throws IOException {
		transcript.saveTranscriptData(new FileOutputStream(sessionPath));
	}

	@Override
	public void autosaveTranscript(ITranscript transcript) throws IOException {
	}

	@Override
	public boolean hasAutosaveFile(String corpus, String session)
			throws IOException {
		return false;
	}

	@Override
	public ITranscript getAutosaveFile(String corpus, String session)
			throws IOException {
		return null;
	}

	@Override
	public void removeAutosaveFile(String corpus, String session)
			throws IOException {
	}

	@Override
	public ITranscript newTranscript(String corpusName, String transcriptName)
			throws IOException {
		return null;
	}

	@Override
	public void removeTranscript(String corpusName, String transcriptName)
			throws IOException {
	}

	@Override
	public void newCorpus(String corpusName, String corpusDesc)
			throws IOException {
	}

	@Override
	public void removeCorpus(String corpusName) throws IOException {
	}

	@Override
	public void renameCorpus(String corpusName, String newName)
			throws IOException {

	}

	@Override
	public ArrayList<LogData> getProjectLog() {
		return new ArrayList<LogData>();
	}

	@Override
	public void removeProjectLogEntry(int logIndex) {
	}

	@Override
	public void addProjectLogEntry(LogData data) {

	}

	@Override
	public void save() throws IOException {

	}

	@Override
	public void saveAs(String path) throws IOException {
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public String getUUID() {
		return null;
	}

	@Override
	public boolean validateTranscript(String corpus, String transcript,
			Serializable value) {
		return true;
	}

	@Override
	public boolean validateTranscriptDb(String corpus, String transcript,
			Serializable value) {
		return true;
	}

	@Override
	public File getResource(String resourcePath) throws IOException {
		return null;
	}

	@Override
	public Calendar getTranscriptModificationDate(String corpus,
			String transcript) throws IOException {
		return null;
	}

	@Override
	public int getNumberOfRecords(String corpus, String session)
			throws IOException {
		return 0;
	}

}
