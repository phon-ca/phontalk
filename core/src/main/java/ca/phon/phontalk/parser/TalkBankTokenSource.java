/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2015, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
 * Dept of Linguistics, Memorial University <https://phon.ca>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.phon.phontalk.parser;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Stack;
import java.util.logging.Logger;

import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenSource;
import org.apache.commons.lang3.StringUtils;

import ca.phon.util.EmptyQueueException;
import ca.phon.util.Queue;

/**
 * Uses StAX to generate tokens for the
 * ANTLR parser <code>UtteranceParser</code>.
 * 
 *
 */
public class TalkBankTokenSource implements TokenSource {
	
	private final static Logger LOGGER = Logger.getLogger(TalkBankTokenSource.class.getName());
	
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
	public TalkBankTokenSource(InputStream source) {
		super();
		
		// create StAX reader
		XMLInputFactory factory = XMLInputFactory.newInstance();
		try {
			//BufferedReader in = new BufferedReader(new InputStreamReader(source, "UTF-8"));
			XMLEventReader xmlReader = factory.createXMLEventReader(source, "UTF-8");
			reader = factory.createFilteredReader(xmlReader, new XMLWhitespaceFilter());
		} catch (XMLStreamException e) {
			LOGGER.warning(e.toString());
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
				LOGGER.warning(e.toString());
			}
		} else if(reader.hasNext()) {
			try {
				XMLEvent evt = reader.nextEvent();
				if(evt != null) {
					
					if(evt.isProcessingInstruction()
							|| evt.isStartDocument()
							|| evt.isEndDocument() )
					{
						return nextToken();
					}
					
					Integer tokenType = tokenMap.getANTLRTokenType(evt);
					if(tokenType == null) {
						LOGGER.warning("Unknown type(" + 
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
							LOGGER.warning(
									"No matching start element for '" + localName + "'");
						}
					}
					
					
				}
			} catch (XMLStreamException e) {
				LOGGER.warning(e.toString());
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
