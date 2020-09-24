package ca.phon.phontalk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import ca.phon.session.io.SessionInputFactory;
import ca.phon.session.io.SessionReader;
import ca.phon.util.OSInfo;
import ca.phon.worker.PhonTask.TaskStatus;

public class Phon2CHATTask extends PhonTalkTask {

	public Phon2CHATTask(File inputFile, File outputFile, PhonTalkListener listener) {
		super(inputFile, outputFile, listener);
	}

	@Override
	public String getProcessName() {
		return "Phon -> CHAT";
	}

	@Override
	public void performTask() {
		// convert transcript
		super.setStatus(TaskStatus.RUNNING);
		
		// check to make sure the file is a valid phon session
		final SessionInputFactory inputFactory = new SessionInputFactory();
		final SessionReader reader = inputFactory.createReader("phonbank", "1.2");
		try {
			final InputStream phonStream = new FileInputStream(getInputFile());
			reader.readSession(phonStream);
		} catch (IOException e) {
			if(PhonTalkUtil.isVerbose()) e.printStackTrace();
			final PhonTalkError err = new PhonTalkError(e);
			getListener().message(err);
			super.err = e;
			super.setStatus(TaskStatus.ERROR);
			return;
		}
				
		try {
			final File tempChaFile = File.createTempFile("chatter", ".cha");
			tempChaFile.deleteOnExit();
			final File tempFile = File.createTempFile("chatter", ".xml");
			tempFile.deleteOnExit();
			
			final Phon2XmlConverter converter = new Phon2XmlConverter();
			converter.convertFile(getInputFile(), tempFile, getListener());
			
			final TalkbankValidator validator = new TalkbankValidator();
			final DefaultErrorHandler errHandler = new DefaultErrorHandler(tempFile, getListener());
			if(!validator.validate(tempFile, errHandler)) {
				err = new Exception("xml not valid");
				super.setStatus(TaskStatus.ERROR);
			}
			
			// convert to CHAT
			Xml2CHATConverter chatConverter = new Xml2CHATConverter();
			chatConverter.convertFile(tempFile, tempChaFile, getListener());
			
			// copy CHAT file to destination
			try(final PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(getOutputFile()), "UTF-8"))) {
				final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tempChaFile), "UTF-8"));
				String line = null;
				while((line = in.readLine()) != null) {
					writer.write(line);
					writer.write(OSInfo.isMacOs() ? "\n" : "\r\n");
				}
				writer.flush();
				in.close();
			} catch (IOException e) {
				throw e;
			}
			
			setStatus(TaskStatus.FINISHED);
		} catch (Exception e) {
			if(PhonTalkUtil.isVerbose()) e.printStackTrace();
			final PhonTalkError err = new PhonTalkError(e);
			err.setFile(getOutputFile());
			getListener().message(err);
			super.err = e;
			super.setStatus(TaskStatus.ERROR);
		}
	}

}
