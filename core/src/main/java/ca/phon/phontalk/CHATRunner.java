package ca.phon.phontalk;

import java.io.*;
import java.net.*;

import ca.phon.util.OSInfo;

public class CHATRunner {

	private final static String CHATTER_JAR = "data/bin/chatter.jar";
	
	public CHATRunner() {
	}

	public Process convertFile(File inputFile, String inputFormat, File outputFile, String outputFormat)
		throws IOException {
		final String javaHome = System.getProperty("java.home");
		final String javaBin = javaHome + File.separator + "bin" + File.separator + "java" + 
				(OSInfo.isWindows() ? ".exe" : "");
		
		final URL chatterURL = ClassLoader.getSystemResource(CHATTER_JAR);
		String chatter = CHATTER_JAR;
		if(chatterURL != null) {
			try {
				chatter = new File(chatterURL.toURI()).getAbsolutePath();
			} catch (URISyntaxException e) {
				throw new IOException(e);
			}
		}
		
		final ProcessBuilder processBuilder = new ProcessBuilder(javaBin,
						"--add-modules",
						"java.xml.bind",
						"-cp", chatter, 
						"org.talkbank.chatter.App",
						"-inputFormat", inputFormat,
						"-outputFormat", outputFormat,
						"-output", outputFile.getAbsolutePath(),
						inputFile.getAbsolutePath());
		processBuilder.redirectErrorStream(true);
	
		final Process process = processBuilder.start();
		return process;
	}
	
}