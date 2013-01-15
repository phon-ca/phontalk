package ca.phon.phontalk.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import javax.xml.stream.EventFilter;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenSource;

import ca.phon.exceptions.EmptyQueueException;
import ca.phon.system.logger.PhonLogger;
import ca.phon.util.Queue;
import ca.phon.util.StringUtils;

/**
 * Uses StAX to generate tokens for the
 * ANTLR parser <code>UtteranceParser</code>.
 * 
 *
 */
public class ChatTokenSource implements TokenSource {
	/** XML Event reader */
	private XMLEventReader reader;
	
	/** Stack of elements */
	private Stack<String> elementStack = new Stack<String>();
	
	private Queue<Token> tokenQueue = new Queue<Token>();
	
	private AntlrTokens tokenMap = new AntlrTokens("Chat.tokens");
	
	
	/**
	 * Constructor 
	 * 
	 * @param file the token def file
	 */
	public ChatTokenSource(InputStream source) {
		super();
		
		// create StAX reader
		XMLInputFactory factory = XMLInputFactory.newInstance();
		try {
			//BufferedReader in = new BufferedReader(new InputStreamReader(source, "UTF-8"));
			XMLEventReader xmlReader = factory.createXMLEventReader(source, "UTF-8");
			reader = factory.createFilteredReader(xmlReader, new XMLWhitespaceFilter());
		} catch (XMLStreamException e) {
			PhonLogger.warning(e.toString());
		}
	}

	@Override
	public String getSourceName() {
		return null;
	}

	private int tokenIndex = 0;
	@Override
	public Token nextToken() {
		Token retVal = new CommonToken(Token.EOF);
		
		if(!tokenQueue.isEmpty()) {
			try {
				retVal = tokenQueue.dequeue();
			} catch (EmptyQueueException e) {
				PhonLogger.warning(e.toString());
			}
		} else if(reader.hasNext()) {
			try {
				XMLEvent evt = reader.nextEvent();
//				System.out.println(evt);
				if(evt != null) {
					
					if(evt.isProcessingInstruction()
							|| evt.isStartDocument()
							|| evt.isEndDocument() )
					{
						return nextToken();
					}
					
					Integer tokenType = tokenMap.getANTLRTokenType(evt);
					if(tokenType == null) {
						PhonLogger.warning("Unknown type(" + 
								evt.getLocation().getLineNumber() + ":" + 
								evt.getLocation().getColumnNumber() + ") " + evt.toString());
						tokenType = -1;
					}
					retVal = new ChatToken(tokenType);
					
					// setup location info
					int lineNum = evt.getLocation().getLineNumber();
					int colNum = evt.getLocation().getColumnNumber();
					
					retVal.setLine(lineNum);
					retVal.setCharPositionInLine(colNum);
					retVal.setTokenIndex(tokenIndex++);
					
					if(evt.isCharacters()) {
						String val =
							evt.asCharacters().getData();
						
						retVal.setText(val);
					} else if(evt.isStartElement()) {
						String localName = 
							evt.asStartElement().getName().getLocalPart();
						localName = localName.replaceAll("-", "_");
						elementStack.push(localName.toUpperCase());
						
						// handle any attributes, attributes which do not
						// appear in the ANTLR grammar will be ignored
						for(Iterator i = evt.asStartElement().getAttributes(); i.hasNext();) {
							Attribute attr = (Attribute)i.next();
							String attrName = 
								attr.getName().getLocalPart();
							String tokenName = 
								localName.toUpperCase().replace("-", "_") + AntlrTokens.ATTR_TOKEN + 
								attrName.replaceAll("-", "_").toUpperCase();
							Integer type =
								tokenMap.getTokenType(tokenName);
							if(type != null) {
								Token attrToken = new ChatToken(type);
								attrToken.setLine(evt.getLocation().getLineNumber());
								attrToken.setCharPositionInLine(evt.getLocation().getColumnNumber());
								attrToken.setText(attr.getValue());
								tokenQueue.add(attrToken);
							}
						}
						
					} else if(evt.isEndElement()) {
						String localName =
							evt.asEndElement().getName().getLocalPart();
						localName = localName.replaceAll("-", "_");
						if(elementStack.peek().equals(localName.toUpperCase())){
							elementStack.pop();
						} else {
							// print a warning
							PhonLogger.warning(
									"No matching start element for '" + localName + "'");
						}
					}
					
					
				}
			} catch (XMLStreamException e) {
				PhonLogger.warning(e.toString());
			}
		}
		
		return retVal;
	}
	
	private class XMLWhitespaceFilter implements EventFilter {

		@Override
		public boolean accept(XMLEvent arg0) {
			boolean retVal = true;
			
			
			if(arg0.isCharacters() && 
					StringUtils.strip(arg0.asCharacters().getData()).length() == 0) {
				
				retVal = false;
			}
			
			return retVal;
		}

	}
	
	/**
	 * Custom token type
	 */
	 private class ChatToken extends CommonToken {
	 	 
	 	 public ChatToken(int type) {
	 	 	 super(type);
	 	 }
	 	 
	 	 @Override
	 	 public String getText() {
	 	 	 String retVal = super.getText();
	 	 	 
	 	 	 if(retVal == null) {
	 	 	 	 retVal = "<" + tokenMap.getTokenName(getType()) + ">";
	 	 	 }
	 	 	 
	 	 	 return retVal;
	 	 }
	 	 
	 }
}
