package ca.phon.phontalk;

import java.io.*;

public class Xml2CHATConverter {

	public Xml2CHATConverter() {
	}
	
	public void convertFile(File inputFile, File outputFile, PhonTalkListener listener) {
		final CHATRunner runner = new CHATRunner();
		try {
			final Process process = runner.convertFile(inputFile, "xml", outputFile, "cha");
			 
			final InputStream stdout = process.getInputStream();
			final BufferedReader in = new BufferedReader(new InputStreamReader(stdout, "UTF-8"));
			String line = null;
			while((line = in.readLine()) != null) {
				// ignore JVM warnings about reflective access within Chatter
				if(line.startsWith("WARNING:")) continue;
				listener.message(new PhonTalkMessage(line));
			}
		} catch (IOException e) {
			listener.message(new PhonTalkMessage(e.getMessage()));
		}
	}

}
