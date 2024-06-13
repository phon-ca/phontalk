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
import java.util.List;

import ca.phon.session.io.SessionIO;
import ca.phon.session.io.SessionInputFactory;
import ca.phon.session.io.SessionReader;
import ca.phon.util.OSInfo;
import ca.phon.worker.PhonTask.TaskStatus;

public class Phon2CHATTask extends PhonTalkTask {

	public Phon2CHATTask(File inputFile, File outputFile) {
		super(inputFile, outputFile, null);
	}

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
		final List<SessionIO> phonSessionIO = inputFactory.availableReaders().stream().filter(
				(io) -> io.group().equals("ca.phon")).toList();
		final List<SessionReader> phonSessionReaders = phonSessionIO.stream().map(inputFactory::createReader).toList();
		boolean validSession = false;
		for(SessionReader reader:phonSessionReaders) {
			try {
				if(reader.canRead(getInputFile())) {
					final InputStream phonStream = new FileInputStream(getInputFile());
					reader.readSession(phonStream);
					validSession = true;
					break;
				}
			} catch (IOException e) {
				if (PhonTalkUtil.isVerbose()) e.printStackTrace();
				super.err = e;
			}
		}
		if(!validSession) {
			final PhonTalkError err = new PhonTalkError(new IOException("Invalid phon session file"));
			getListener().message(err);
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
			
			if(!getOutputFile().exists() || getOutputFile().length() == 0) {
				throw new IOException("Output file not written");
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
