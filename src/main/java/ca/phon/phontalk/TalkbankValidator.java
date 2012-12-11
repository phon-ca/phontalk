package ca.phon.phontalk;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.ValidationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.xml.resolver.tools.CatalogResolver;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import ca.phon.util.StringUtils;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

/**
 * Validates talkbank xml files.
 * 
 * The class looks for the schmea in the following locations:
 *   ./talkbank.xsd
 *   ./data/talkbank.xsd
 *   http://www.talkbank.org/software/talkbank.xsd
 *  
 * Errors are automatically sent to PhonLogger.
 * 
 */
public class TalkbankValidator {
	
	private final static Logger LOGGER = 
			Logger.getLogger(TalkbankValidator.class.getName());
	
	/** http location */
	private final static String defaultTalkbankSchemaLoc = "http://www.talkbank.org/software/talkbank.xsd";

	/** schema filename */
	private final static String schemaFileName = "talkbank.xsd";
	
	/** The loaded schema */
	private Schema schema;
	
	/**
	 * Constructor
	 * 
	 */
	public TalkbankValidator() {
		initSchema();
	}
	
	private void initSchema() {
		Source schemaSource = null;

		schemaSource = loadLocalSchema();
		if(schemaSource == null) schemaSource = loadEmbeddedSchema();
		if(schemaSource == null) schemaSource = loadExternalSchema();
		
		if(schemaSource != null) {
			try {
				SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			    schemaFactory.setResourceResolver(getResourceResolver());
				schema = schemaFactory.newSchema(schemaSource);
				
			} catch (SAXException e) {
				if(PhonTalkUtil.isVerbose()) {
					e.printStackTrace();
				}
				LOGGER.severe(e.toString());
			}
		} else {
			LOGGER.severe("Could not load talkbank schema. File not found.");
		}
	}
	
	/**
	 * Get the catalog resolver for loading cached files over
	 * online resources (i.e., the xml.xsd schema file).
	 */
	private CatalogResolver getCatalogResolver() {
		CatalogResolver entityResolver = new CatalogResolver(true); 
		
		try {
		  entityResolver.getCatalog().parseCatalog(getClass().getClassLoader().getResource("catalog.cat"));
		} catch (MalformedURLException e) {
		  if(PhonTalkUtil.isVerbose()) e.printStackTrace();
		  LOGGER.severe(e.getMessage());
		} catch (IOException e) {
		  if(PhonTalkUtil.isVerbose()) e.printStackTrace();
		  LOGGER.severe(e.getMessage());
		}
		
		return entityResolver;
	}
	
	/**
	 * Get the resource resolver based on the catalog resolver.
	 * 
	 */
	public LSResourceResolver getResourceResolver() {
		final CatalogResolver entityResolver = getCatalogResolver();
		
	
		LSResourceResolver retVal = new LSResourceResolver() {
		    @Override
		    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
		      if (publicId == null) {
		        publicId = namespaceURI;
		      }
		      return new InternalLSInputSAXWrapper(entityResolver.resolveEntity(publicId, systemId));
		    }
		  };
				  
		return retVal;
	}

	/**
	 * Load schema from file at ./talkbank.xsd
	 *
	 * @return the loaded schema source or null if not found
	 */
	private Source loadLocalSchema() {
		File localSchemaFile = new File(schemaFileName);
		if(localSchemaFile.exists()) {
//			LOGGER.info("Loading talkbank schema at " + localSchemaFile.getAbsolutePath());

			Source schemaSource = new StreamSource(localSchemaFile);
			return schemaSource;
		} else {
			return null;
		}
	}

	/**
	 * Load schema from class loader.
	 *
	 * @return the loaded schema source of null if not found
	 */
	private Source loadEmbeddedSchema() {
		InputStream schemaURL = getClass().getClassLoader().getResourceAsStream(schemaFileName);
		if(schemaURL != null) {
//			LOGGER.info("Loading talkbank schema at " +
//					getClass().getClassLoader().getResource(schemaFileName));

			Source schemaSource = new StreamSource(schemaURL);
			return schemaSource;
		} else {
			return null;
		}
	}

	/**
	 * Load remote schema
	 *
	 * @return the loaded schema source or null if not found
	 */
	private Source loadExternalSchema() {
		try {
			URL schemaURL = new URL(defaultTalkbankSchemaLoc);
			HttpURLConnection connection = (HttpURLConnection)schemaURL.openConnection();
			  connection.setRequestMethod("GET");
			  connection.setDoOutput(true);
			  connection.setReadTimeout(10000);
			if(connection.getResponseCode() == 200) {
//				LOGGER.info("Loading talkbank schema at " +
//						defaultTalkbankSchemaLoc);
				Source schemaSource = new StreamSource(connection.getInputStream());
				return schemaSource;
			} else {
				return null;
			}
		} catch (MalformedURLException e) {
			if(PhonTalkUtil.isVerbose()) e.printStackTrace();
			LOGGER.severe(e.getMessage());
			return null;
		} catch (IOException e) {
			if(PhonTalkUtil.isVerbose()) e.printStackTrace();
			LOGGER.severe(e.getMessage());
			return null;
		}
	}
	
//	/**
//	 * Vaidate the given string.
//	 */
//	public boolean validate(String str) {
//		// convert into a DOM tree
//		try {
//
//			ByteArrayInputStream bin = new ByteArrayInputStream(str.getBytes("UTF-8"));
//			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
//			builderFactory.setNamespaceAware(true);
//			DocumentBuilder parser = builderFactory.newDocumentBuilder();
//		    Document document = parser.parse(bin);
//		    
//		    return validate(document);
//		} catch (ParserConfigurationException e) {
//			PhonLogger.severe(e.toString());
//			return false;
//		} catch (SAXException e) {
//			PhonLogger.severe(e.toString());
//			return false;
//		} catch (IOException e) {
//			PhonLogger.severe(e.toString());
//			return false;
//		}
//	}
	
	/**
	 * Validate the given DOM doc.
	 */
	public boolean validate(Document doc) 
			throws ValidationException {
		return validate(doc, null);
	}
	
	public boolean validate(Document doc, ErrorHandler handler) 
			throws ValidationException {
		return validate(new DOMSource(doc), handler);
	}
	
	/**
	 * Validatoe the given file
	 */
	public boolean validate(File file)
		throws ValidationException {
		return validate(file, null);
	}
	
	public boolean validate(File file, ErrorHandler handler) 
		throws ValidationException {
		// convert into a DOM tree
		try {
			
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			builderFactory.setNamespaceAware(true);
			DocumentBuilder parser = builderFactory.newDocumentBuilder();
		    Document document = parser.parse(file);
		    
		    return validate(document, handler);
		} catch (ParserConfigurationException e) {
			throw new ValidationException(e.getMessage(), e);
		} catch (SAXException e) {
			throw new ValidationException(e.getMessage(), e);
		} catch (IOException e) {
			throw new ValidationException(e.getMessage(), e);
		}
	}

	public boolean validate(Source src) 
			throws ValidationException {
		return validate(src, null);
	}


	/**
	 * Validate the given source.
	 */
	public boolean validate(Source src, ErrorHandler handler) 
			throws ValidationException {
		boolean retVal = false;
		
		Validator validator = schema.newValidator();
		
		if(handler != null)
			validator.setErrorHandler(handler);
		try {
			validator.setResourceResolver(getResourceResolver());
			validator.validate(src);
			retVal = true;
		} catch (SAXException e) {
			throw new ValidationException(e.getMessage(), e);
		} catch (IOException e) {
			throw new ValidationException(e.getMessage(), e);
		}
		
		return retVal;
	}

	/**
	 * LSInput implementation that wraps a SAX InputSource
	 * 
	 * This class has been copied as a private class since
	 * tools.jar is not always in the JRE classpath for windows.
	 *
	 * @author Ryan.Shoemaker@Sun.COM
	 */
	private class InternalLSInputSAXWrapper implements LSInput {
	    private InputSource core;

	    public InternalLSInputSAXWrapper(InputSource inputSource) {
	        assert inputSource!=null;
	        core = inputSource;
	    }

	    public Reader getCharacterStream() {
	        return core.getCharacterStream();
	    }

	    public void setCharacterStream(Reader characterStream) {
	        core.setCharacterStream(characterStream);
	    }

	    public InputStream getByteStream() {
	        return core.getByteStream();
	    }

	    public void setByteStream(InputStream byteStream) {
	        core.setByteStream(byteStream);
	    }

	    public String getStringData() {
	        return null;
	    }

	    public void setStringData(String stringData) {
	        // no-op
	    }

	    public String getSystemId() {
	        return core.getSystemId();
	    }

	    public void setSystemId(String systemId) {
	        core.setSystemId(systemId);
	    }

	    public String getPublicId() {
	        return core.getPublicId();
	    }

	    public void setPublicId(String publicId) {
	        core.setPublicId(publicId);
	    }

	    public String getBaseURI() {
	        return null;
	    }

	    public void setBaseURI(String baseURI) {
	        // no-op
	    }

	    public String getEncoding() {
	        return core.getEncoding();
	    }

	    public void setEncoding(String encoding) {
	        core.setEncoding(encoding);
	    }

	    public boolean getCertifiedText() {
	        return true;
	    }

	    public void setCertifiedText(boolean certifiedText) {
	        // no-op
	    }
	}
}
