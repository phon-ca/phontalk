package ca.phon.phontalk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import ca.phon.application.PhonTask;
import ca.phon.application.transcript.ITranscript;

/**
 * Converts a single stream of tb xml into phon xml
 *
 */
public class Xml2PhonTask extends PhonTask {
	
	private InputStream tbStream;
	
	private OutputStream phonStream;
	
	private String inStream;
	private String outStream;
	
	public Xml2PhonTask(String inFile, String outFile) {
		try {
			tbStream = new FileInputStream(inFile);
			phonStream = new FileOutputStream(outFile);
			
			inStream = inFile;
			outStream = outFile;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Xml2PhonTask(InputStream inStream, OutputStream outStream) {
		this.tbStream = inStream;
		this.phonStream = outStream;
		
		this.inStream = "";
		this.outStream = "";
	}

	@Override
	public void performTask() {
		super.setStatus(TaskStatus.RUNNING);

		TalkbankConverter converter = new TalkbankConverter();
		
		ITranscript t = converter.convertStream(inStream, tbStream);
		try {
			t.saveTranscriptData(phonStream);
		} catch (IOException e) {
			e.printStackTrace();
			super.err = e;
			super.setStatus(TaskStatus.ERROR);
			return;
		}
		
		super.setStatus(TaskStatus.FINISHED);
	}

}
