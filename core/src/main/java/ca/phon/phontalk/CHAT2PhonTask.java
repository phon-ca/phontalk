package ca.phon.phontalk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import ca.phon.session.Session;
import ca.phon.session.io.OriginalFormat;
import ca.phon.session.io.SessionIO;
import ca.phon.session.io.SessionOutputFactory;
import ca.phon.util.OSInfo;

public class CHAT2PhonTask extends PhonTalkTask {
	
	private final static Logger LOGGER = Logger.getLogger("ca.phon.phontalk.parser");

	public CHAT2PhonTask(File inputFile, File outputFile, PhonTalkListener listener) {
		super(inputFile, outputFile, listener);
	}

	@Override
	public String getProcessName() {
		return "CHAT -> Phon";
	}

	@Override
	public void performTask() {
		setStatus(TaskStatus.RUNNING);
		
		try {
			final File tempChaFile = File.createTempFile("chatter", ".cha");
			tempChaFile.deleteOnExit();
			final File tempFile = File.createTempFile("chatter", ".xml");
			tempFile.deleteOnExit();
			
			try(final PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tempChaFile), "UTF-8"))) {
				final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(getInputFile()), "UTF-8"));
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
			
			final StringBuffer buffer = new StringBuffer();
			final AtomicReference<Boolean> xmlReady = new AtomicReference<Boolean>(true);
			// use the converter to convert to XML
			final CHAT2XmlConverter converter = new CHAT2XmlConverter();
			converter.convertFile(tempChaFile, tempFile, new PhonTalkListener() {
				
				@Override
				public void message(PhonTalkMessage msg) {
					xmlReady.set(false);
					buffer.append(msg.getMessage());
					buffer.append("\n");
				}
				
			});
			
			if(isShutdown()) {
				setStatus(TaskStatus.TERMINATED);
				return;
			}
			
			if(xmlReady.get()) {
				Xml2PhonConverter xml2Phon = new Xml2PhonConverter();
				xml2Phon.setInputFile(tempFile);
				final Session retVal =  xml2Phon.convertStream(new FileInputStream(tempFile), new PhonTalkListener() {
					
					@Override
					public void message(PhonTalkMessage msg) {
						buffer.append(msg.getMessage());
					}
				});
				retVal.putExtension(OriginalFormat.class, new OriginalFormat(getClass().getAnnotation(SessionIO.class)));
				
				SessionOutputFactory factory = new SessionOutputFactory();
				factory.createWriter().writeSession(retVal, new FileOutputStream(getOutputFile()));
			} else {
				throw new IOException(buffer.toString());
			}
			
			setStatus(TaskStatus.FINISHED);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e.getLocalizedMessage());
			super.err = e;
			setStatus(TaskStatus.ERROR);
		}
	}
	
}
