package ca.phon.phontalk;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import ca.phon.application.IPhonFactory;
import ca.phon.application.project.IPhonProject;
import ca.phon.application.transcript.ITranscript;
import ca.phon.phontalk.parser.ChatParser;
import ca.phon.phontalk.parser.ChatTokenSource;
import ca.phon.phontalk.parser.ChatTokens;
import ca.phon.phontalk.parser.ChatTree;
import ca.phon.phontalk.parser.Phon2XmlWalker;
import ca.phon.system.logger.PhonLogger;

/**
 * Reads in a talkbank XML file using SAX and
 * JAXB.  Converts the file into a PhonBank v1.1 file.
 * 
 * Current talkbank schema version is 1.4.9 (2010-01-07)
 */
public class TalkbankConverter extends XmlConverter {
	
	
	public TalkbankConverter() {
	}

	@Override
	/**
	 * Convert the talkbank 1.4.9 file into a PhonBank 1.1 file.
	 * 
	 * @param file the file to convert
	 * @param params - unused, can be <code>null</code>
	 */
	public ITranscript convertStream(String name, InputStream source) {
		ITranscript retVal = null;
		
		// create token source for stream
		String tokenFileName = "ca/phon/xml2phon/parser/Chat.tokens";
		ChatTokenSource tokenSource = new ChatTokenSource(source, tokenFileName);
		TokenStream tokenStream = new CommonTokenStream(tokenSource);
		
		// parse data into an AST
		ChatParser parser = new ChatParser(tokenStream);
		parser.setFilename(name);
		ChatParser.chat_return parserRet = null;
		try {
			parserRet = parser.chat();
		} catch (RecognitionException e1) {
			PhonLogger.warning(e1.toString());
			ConverterError err = new ConverterError();
			err.setLineNumber(e1.line);
		}
		
		// only continue if there were no parsing errors
		if(parserRet != null && parser.getNumberOfSyntaxErrors() == 0) {
			CommonTree tree = (CommonTree)parserRet.getTree();
			CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(tree);
			
			ChatTree walker = new ChatTree(nodeStream);
			walker.setConverter(this);
			try {
				walker.chat();
				if(walker.getNumberOfSyntaxErrors() == 0) {
					retVal = walker.getSession();
				}
			} catch (RecognitionException e) {
				PhonLogger.warning(e.toString());
			}
		}
		
		return retVal;
	}
	
	private void printTree(CommonTree t, int tabIndex) {
		ChatTokens tokens = new ChatTokens();
		
		for(int i = 0; i < tabIndex; i++) {
			System.out.print("\t");
		}
		
		Token token = t.getToken();
		System.out.println(tokens.getTokenName(token.getType()) + " : " + token.getText());
		for(int i = 0; i < t.getChildCount(); i++) {
			printTree((CommonTree)t.getChild(i), tabIndex+1);
		}
	}

	

}
