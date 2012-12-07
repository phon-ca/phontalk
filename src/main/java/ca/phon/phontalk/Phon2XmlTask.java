package ca.phon.phontalk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;

import ca.phon.application.IPhonFactory;
import ca.phon.application.PhonTask;
import ca.phon.application.transcript.ITranscript;
import ca.phon.system.logger.PhonLogger;

/**
 * Converts a single xml file as a stream
 *  from phon's format to talkbank
 *
 */
public class Phon2XmlTask extends PhonTask {
	
	private InputStream phonStream;
	
	private OutputStream tbStream;
	
	public Phon2XmlTask(String inFile, String outFile) {
		try {
			this.phonStream = new FileInputStream(inFile);
			this.tbStream = new FileOutputStream(outFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public Phon2XmlTask(InputStream inStream, OutputStream outStream) {
		this.phonStream = inStream;
		this.tbStream = outStream;
	}

	@Override
	public void performTask() {
		// convert transcript
		super.setStatus(TaskStatus.RUNNING);
		
		ITranscript t = IPhonFactory.getDefaultFactory().createTranscript();
		try {
			t.loadTranscriptData(phonStream);
		} catch (IOException e) {
			PhonLogger.severe(e.getMessage());
			super.err = e;
			super.setStatus(TaskStatus.ERROR);
			return;
		}
		
		Phon2XmlConverter converter = new Phon2XmlConverter();
		
		String xml = converter.convertTranscript(t);
		TalkbankValidator validator = new TalkbankValidator();
		
		if(validator.validate(xml)) {
			try {
				tbStream.write(xml.getBytes("UTF-8"));
				tbStream.flush();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				super.err = e;
				super.setStatus(TaskStatus.ERROR);
				return;
			} catch (IOException e) {
				e.printStackTrace();
				super.err = e;
				super.setStatus(TaskStatus.ERROR);
				return;
			}
		}
		
		super.setStatus(TaskStatus.FINISHED);
	}

}
