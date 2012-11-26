package ca.phon.phontalk.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.events.XMLEvent;

import ca.phon.system.logger.PhonLogger;

/**
 * Handles mapping from String <-> ANTLR Token type
 * 
 *
 */
public class AntlrTokens {
	public static final String TEXT_TOKEN = "TEXT";
	public static final String END_SUFFIX = "_END";
	public static final String START_SUFFIX = "_START";
	public static final String ATTR_TOKEN = "_ATTR_";
	
	private static final int UNDEFINED_TYPE = Integer.MAX_VALUE;
	
	/** Token map */
	private Map<String, Integer> tokenMap =
		new HashMap<String, Integer>();
	
	public AntlrTokens(String tokenFile) {
		super();
		initTokenMap(getClass().getClassLoader().getResource(tokenFile));
	}
	
	private void initTokenMap(URL url) {
		try {
//			File tokenFile = new File(file);
			URLConnection uc = url.openConnection();
			
			BufferedReader in = new BufferedReader(
					new InputStreamReader(uc.getInputStream()));
			String line = null;
			while((line = in.readLine()) != null) {
				String[] vals = line.split("=");
				if(vals.length == 2) {
					String tokenName = vals[0];
					String tokenNum = vals[1];
					Integer iToken = Integer.parseInt(tokenNum);
					tokenMap.put(tokenName, iToken);
				}
			}
			in.close();
		} catch (IOException e) {
			PhonLogger.warning(e.toString());
		}
	}
	
	/**
	 * Returns null if type is not found
	 * @param tokenName
	 * @return
	 */
	public Integer getTokenType(String tokenName) {
		Integer retVal = null;
		
		if(tokenMap.containsKey(tokenName))
			retVal = tokenMap.get(tokenName);
		
		return retVal;
	}
	
	/**
	 * Returns null if type is not found
	 * @param evt
	 * @return
	 */
	public Integer getANTLRTokenType(XMLEvent evt) {
		Integer retVal = null;
		
		if(evt.isCharacters()) {
			retVal = tokenMap.get(TEXT_TOKEN);
		} else if(evt.isStartElement()) {
			String localName =
				evt.asStartElement().getName().getLocalPart();
			localName = localName.replaceAll("-", "_");
			
			String tokenName = 
				localName.toUpperCase() + START_SUFFIX;
			Integer i = tokenMap.get(tokenName);
			if(i != null)
				retVal = i.intValue();
		} else if(evt.isEndElement()) {
			String localName = 
				evt.asEndElement().getName().getLocalPart();
			localName = localName.replaceAll("-", "_");
			String tokenName = 
				localName.toUpperCase() + END_SUFFIX;
			Integer i = tokenMap.get(tokenName);
			if(i != null)
				retVal = i;
		}
		
		return retVal;
	}
	
	public String getTokenName(int iToken) {
		String retVal = "Undefined";
		
		for(String k:tokenMap.keySet()) {
			if(tokenMap.get(k).equals(iToken)) {
				retVal = k;
				break;
			}
		}
		
		return retVal;
	}
	
}
