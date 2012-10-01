package ca.phon.phontalk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import ca.phon.system.logger.PhonLogger;

/**
 * Provides file listings for a talkbank source.  Talkbank
 * source implementations are a parent direction or zip file.
 *
 */
public abstract class TalkbankSource {
	
	/**
	 * Lists all of the talkbank xml files found in this source.
	 * 
	 * @return the list of talkbank files
	 */
	public abstract File[] listTalkbankFiles();
	
	/** 
	 * Returns an input stream for the given file.  This is required
	 * as different sources may have different stream creation requirements.
	 * 
	 * @param file
	 * @return the generated input stream.  The stream should be closed
	 * when it's no longer needed.
	 */
	public abstract InputStream toInputStream(File file)
		throws IOException;
	
	/**
	 * Returns the name (usually location) of this talkbank source.
	 * 
	 * @return name
	 */
	public abstract String getSourceName();
	
	/**
	 * Pefroms xml validation on the given file.
	 * 
	 * @param file - the xml file
	 * @returns <code>true</code> if the validation passes,
	 * <code>false</code> otherwise. 
	 */
	private boolean validateFile(File file) {
		boolean retVal = false;
		
		try {
			InputStream stream = toInputStream(file);
			
			retVal = validateStream(stream);
			stream.close();
		} catch (IOException e1) {
			PhonLogger.warning(e1.toString());
		}
			
		return retVal;
	}
	
	/**
	 * Peforms xml validation on the given input stream.
	 * 
	 * @param source - the xml source
	 * @returns <code>true</code> if the validation passes,
	 * <code>false</code> otherwise.
	 */
	private boolean validateStream(InputStream source) {
		boolean retVal = true;
		
		// TODO validation stream
		
		return retVal;
	}
	
}
