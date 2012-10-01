package ca.phon.phontalk;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Talkbank source for working with a single file.
 *
 */
public class FileTalkbankSource extends TalkbankSource {
	
	private String filePath;
	
	public FileTalkbankSource(String file) {
		super();
		this.filePath = file;
	}

	@Override
	public File[] listTalkbankFiles() {
		File[] retVal = { new File(filePath) };
		return retVal;
	}

	@Override
	public InputStream toInputStream(File file) throws IOException {
		return new FileInputStream(file);
	}

	@Override
	public String getSourceName() {
		return (new File(filePath)).getName();
	}

}
