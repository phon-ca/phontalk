package ca.phon.phontalk;

import java.io.*;

/**
 * Convert CHAT to TalkBank XML.
 *
 */
public class CHAT2XmlConverter {

	public CHAT2XmlConverter() {
	}

	public void convertFile(File inputFile, File outputFile, PhonTalkListener listener) {
		final CHATRunner runner = new CHATRunner();
		try {
			final Process process = runner.convertFile(inputFile, "cha", outputFile, "xml");
			 
			final InputStream stdout = process.getInputStream();
			final BufferedReader in = new BufferedReader(new InputStreamReader(stdout, "UTF-8"));
			String line = null;
			while((line = in.readLine()) != null) {
				listener.message(new PhonTalkMessage(line));
			}
		} catch (IOException e) {
			listener.message(new PhonTalkMessage(e.getMessage()));
		}
	}
		
}
