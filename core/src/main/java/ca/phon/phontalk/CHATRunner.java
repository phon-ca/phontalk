package ca.phon.phontalk;

import java.io.*;
import java.net.*;

import ca.phon.util.OSInfo;
import ca.phon.util.PrefHelper;

public class CHATRunner {

	public final static String CHATTER_PROP = CHATRunner.class.getName() + ".chatter";
	private final static String DEFAULT_CHATTER_JAR = "bin/chatter.jar";
	
	public CHATRunner() {
	}

	public Process convertFile(File inputFile, String inputFormat, File outputFile, String outputFormat)
		throws IOException {
		final String javaHome = System.getProperty("java.home");
		final String javaBin = javaHome + File.separator + "bin" + File.separator + "java" + 
				(OSInfo.isWindows() ? ".exe" : "");
		
		final String chatterLoc = PrefHelper.get(CHATTER_PROP, DEFAULT_CHATTER_JAR);
		File chatterFile = new File(chatterLoc);
		String chatter = chatterFile.getAbsolutePath();
		if(!chatterFile.isAbsolute()) {
			final URL chatterURL = ClassLoader.getSystemResource(chatterLoc);
			if(chatterURL != null) {
				try {
					chatter = new File(chatterURL.toURI()).getAbsolutePath();
				} catch (URISyntaxException e) {
					throw new IOException(e);
				}
			}
		}			
		
		final ProcessBuilder processBuilder = new ProcessBuilder(javaBin,
						"-cp", chatter, 
						"org.talkbank.chatter.App",
						"-allowAnyMediaName",
						"-inputFormat", inputFormat,
						"-outputFormat", outputFormat,
						"-output", outputFile.getAbsolutePath(),
						inputFile.getAbsolutePath());
		processBuilder.redirectErrorStream(true);
	
		final Process process = processBuilder.start();
		return process;
	}
	
}
