package ca.phon.phontalk;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import ca.phon.system.logger.PhonLogger;
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

import com.sun.org.apache.xml.internal.resolver.tools.CatalogResolver;

//import com.sun.org.apache.xerces.internal.util.XMLCatalogResolver;
//import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
//import com.sun.org.apache.xerces.internal.xni.XNIException;
//import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
//import com.sun.org.apache.xml.internal.resolver.tools.CatalogResolver;
//import com.sun.tools.internal.xjc.reader.xmlschema.parser.LSInputSAXWrapper;

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
				PhonLogger.severe(e.toString());
			}
		} else {
			PhonLogger.severe("Could not load talkbank schema. File not found.");
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
		  e.printStackTrace();
		} catch (IOException e) {
		  e.printStackTrace();
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
			PhonLogger.fine("Loading talkbank schema at " + localSchemaFile.getAbsolutePath());

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
			PhonLogger.fine("Loading talkbank schema at " +
					getClass().getClassLoader().getResource(schemaFileName));

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
				PhonLogger.info("Loading talkbank schema at " +
						defaultTalkbankSchemaLoc);
				Source schemaSource = new StreamSource(connection.getInputStream());
				return schemaSource;
			} else {
				return null;
			}
		} catch (MalformedURLException e) {
			PhonLogger.severe(e.toString());
			return null;
//					retVal = false;
		} catch (IOException e) {
			PhonLogger.severe(e.toString());
			return null;
//					retVal = false;
		}
	}
	
	/**
	 * Vaidate the given string.
	 */
	public boolean validate(String str) {
		// convert into a DOM tree
		try {

			ByteArrayInputStream bin = new ByteArrayInputStream(str.getBytes("UTF-8"));
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			builderFactory.setNamespaceAware(true);
			DocumentBuilder parser = builderFactory.newDocumentBuilder();
		    Document document = parser.parse(bin);
		    
		    return validate(document);
		} catch (ParserConfigurationException e) {
			PhonLogger.severe(e.toString());
			return false;
		} catch (SAXException e) {
			PhonLogger.severe(e.toString());
			return false;
		} catch (IOException e) {
			PhonLogger.severe(e.toString());
			return false;
		}
	}
	
	/**
	 * Validate the given DOM doc.
	 */
	public boolean validate(Document doc) {
		return validate(new DOMSource(doc));
	}
	
	/**
	 * Validatoe the given file
	 */
	public boolean validate(File file) {
		// convert into a DOM tree
		try {
			
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			builderFactory.setNamespaceAware(true);
			DocumentBuilder parser = builderFactory.newDocumentBuilder();
		    Document document = parser.parse(file);
		    
		    
		    return validate(document);
		} catch (ParserConfigurationException e) {
			PhonLogger.severe(e.toString());
			return false;
		} catch (SAXException e) {
			PhonLogger.severe(e.toString());
			return false;
		} catch (IOException e) {
			PhonLogger.severe(e.toString());
			return false;
		}
	}

	public boolean validate(Source src) {
		return validate(src, new ValidationHandler());
	}


	/**
	 * Validate the given source.
	 */
	public boolean validate(Source src, ValidationHandler handler) {
		boolean retVal = false;
		
		Validator validator = schema.newValidator();
		
		validator.setErrorHandler(handler);
		try {
			validator.setResourceResolver(getResourceResolver());
			validator.validate(src);
			retVal = true;
		} catch (SAXException e) {
		} catch (IOException e) {
			PhonLogger.severe(e.toString());
		}
		
		return retVal;
	}
	
	/**
	 * Default error handler for validation.
	 */
	public static class ValidationHandler implements ErrorHandler {

		private File sourceFile = null;

		public ValidationHandler() {

		}

		public ValidationHandler(File f) {
			this.sourceFile = f;
		}

		@Override
		public void error(SAXParseException exception) throws SAXException {
			if(this.sourceFile != null) {
				int recNum = lineNumberToRecordNumber(sourceFile,
						exception.getLineNumber());

				if(recNum > 0 && recNum < getNumberOfRecords(sourceFile))
					PhonLogger.severe("Validation error in record " + recNum);
			}
			PhonLogger.severe("[xml validator:error] (" + (exception.getLineNumber() + ":" + exception.getColumnNumber())
					+ ") " + exception.toString());
//			throw exception;
		}

		@Override
		public void fatalError(SAXParseException exception) throws SAXException {
			if(this.sourceFile != null) {
				int recNum = lineNumberToRecordNumber(sourceFile,
						exception.getLineNumber());

				if(recNum > 0 && recNum < getNumberOfRecords(sourceFile))
					PhonLogger.severe("Validation fatal-error in record " + recNum);
			}
			PhonLogger.severe("[xml validator:fatal error] (" + (exception.getLineNumber() + ":" + exception.getColumnNumber())
					+ ") " + exception.toString());
			throw exception;
		}

		@Override
		public void warning(SAXParseException exception) throws SAXException {
			if(this.sourceFile != null) {
				int recNum = lineNumberToRecordNumber(sourceFile,
						exception.getLineNumber());

				if(recNum > 0 && recNum < getNumberOfRecords(sourceFile))
					PhonLogger.severe("Validation warning in record " + recNum);
			}
			PhonLogger.warning("[xml validator:warning] (" + (exception.getLineNumber() + ":" + exception.getColumnNumber())
					+ ") " + exception.toString());
		}

		private int lineNumberToRecordNumber(File file, int lineNum) {
			int retVal = -1;
			// attempt to find the record number
			try {
				BufferedReader in = new BufferedReader
						(new InputStreamReader(new FileInputStream(file)));
				String line = null;
				int lineIdx = 0;
				retVal = 0;
				while( ((line = in.readLine()) != null) && (lineIdx < lineNum)) {
					line = StringUtils.strip(line);
					if(line.startsWith("<u")) {
						retVal++;
					}
					lineIdx++;
				}
				in.close();

			} catch (IOException e) {
				PhonLogger.severe(e.getMessage());
			}
			return retVal;
		}

		private int getNumberOfRecords(File file) {
			int retVal = 0;

			// it's faster to use an xpath expression
			// to determine the number of records.
			String xpathPattern = "//u";
			// open as dom file first
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setNamespaceAware(false);
			DocumentBuilder builder;
			try {
				builder = domFactory.newDocumentBuilder();
				Document doc = builder.parse(file);

				XPathFactory xpathFactory = XPathFactory.newInstance();
				XPath xpath = xpathFactory.newXPath();
				XPathExpression expr = xpath.compile(xpathPattern);

				Object result = expr.evaluate(doc, XPathConstants.NODESET);
				NodeList nodes = (NodeList) result;
				retVal = nodes.getLength();
			} catch (XPathExpressionException e) {
				PhonLogger.severe(e.toString());
			} catch (ParserConfigurationException e) {
				PhonLogger.severe(e.toString());
			} catch (SAXException e) {
				PhonLogger.severe(e.toString());
			} catch (IOException e) {
				PhonLogger.severe(e.toString());
			}

			return retVal;
		}
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
