package ca.phon.phontalk;

import java.io.File;

import ca.phon.application.PhonTask;

public abstract class PhonTalkTask extends PhonTask {
	
	private File inputFile;
	
	private File outputFile;
	
	/**
	 * Message listener
	 */
	private PhonTalkListener listener;
	
	public PhonTalkTask(File inputFile, File outputFile, PhonTalkListener listener) {
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		this.listener = listener;
	}

	public File getInputFile() {
		return inputFile;
	}

	public void setInputFile(File inputFile) {
		this.inputFile = inputFile;
	}

	public File getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	public PhonTalkListener getListener() {
		return listener;
	}

	public void setListener(PhonTalkListener listener) {
		this.listener = listener;
	}

	public abstract String getProcessName();
	
}
