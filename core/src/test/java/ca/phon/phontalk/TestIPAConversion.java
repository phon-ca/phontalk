package ca.phon.phontalk;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import ca.phon.ipa.IPATranscript;
import ca.phon.ipa.IPATranscriptBuilder;
import ca.phon.phontalk.parser.AST2Phon;
import ca.phon.phontalk.parser.AST2TalkBank;
import ca.phon.phontalk.parser.TalkBank2ASTParser;
import ca.phon.phontalk.parser.TalkBankTokenSource;
import ca.phon.phontalk.parser.PhoTreeBuilder;

@RunWith(JUnit4.class)
public class TestIPAConversion {

	@Test
	public void testIPATranscriptToXML() throws RecognitionException {
		final String txt = "hello";
		final IPATranscript ipa = (new IPATranscriptBuilder()).append(txt).toIPATranscript();
	
		final PhoTreeBuilder phoTreeBuilder = new PhoTreeBuilder();
		final CommonTree modelTree = phoTreeBuilder.buildPhoTree("model", ipa);
		final CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(modelTree);
		final AST2TalkBank walker = new AST2TalkBank(nodeStream);
		walker.setPhonTalkListener( (msg) -> System.err.println(msg) );
		
		final AST2TalkBank.pho_return phoRet = walker.pho();
		
		System.out.println(phoRet.st.toString());
	}
	
	@Test
	public void testXMLToIPATranscript() throws UnsupportedEncodingException, RecognitionException {
		String xml = "<actual>\n" + 
				"	<pw>\n" + 
				"		<ph id=\"ph0\" sctype=\"UK\">h</ph> \n" + 
				"		<ph id=\"ph1\" sctype=\"UK\">e</ph> \n" + 
				"		<ph id=\"ph2\" sctype=\"UK\">l</ph> \n" + 
				"		<ph id=\"ph3\" sctype=\"UK\">l</ph> \n" + 
				"		<ph id=\"ph4\" sctype=\"UK\">o</ph> \n" + 
				"	</pw> \n" + 
				"</actual>";
		final TalkBankTokenSource tokenSource = new TalkBankTokenSource(new ByteArrayInputStream(xml.getBytes("UTF-8")));
		TokenStream	tokenStream = new CommonTokenStream(tokenSource);
		
		final TalkBank2ASTParser parser = new TalkBank2ASTParser(tokenStream);
		parser.setPhonTalkListener( (msg) -> System.err.println(msg.toString()) );
		final CommonTree phoTree = parser.pho().getTree();
		
		final CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(phoTree);
		final AST2Phon walker = new AST2Phon(nodeStream);
		walker.setProcessFragments(true);
		final IPATranscript ipa = walker.pho().ipa;
		
		System.out.println(ipa.toString(true));
	}
	
}
