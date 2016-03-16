package ca.phon.phontalk;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import junit.framework.Assert;

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
		final String txt = "j:De:Dl:Ll:Oo:N";
		final IPATranscript ipa = (new IPATranscriptBuilder()).append(txt).toIPATranscript();
	
		final PhonTalkListener listener = 
				(msg) -> System.err.println(msg);
		
		// Phon -> AST
		final PhoTreeBuilder phoTreeBuilder = new PhoTreeBuilder();
		final CommonTree modelTree = phoTreeBuilder.buildPhoTree("model", ipa);
		
		// AST -> XML
		final CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(modelTree);
		final AST2TalkBank walker = new AST2TalkBank(nodeStream);
		walker.setPhonTalkListener(listener);
		
		final AST2TalkBank.pho_return phoRet = walker.pho();
		
		final String expectedXML = "<model>\n" + 
				"	<pw>\n" + 
				"		<ph id=\"ph0\" sctype=\"N\">j</ph> \n" + 
				"		<ph id=\"ph1\" sctype=\"N\" hiatus=\"false\">e</ph> \n" + 
				"		<ph id=\"ph2\" sctype=\"LA\">l</ph> \n" + 
				"		<ph id=\"ph3\" sctype=\"O\">l</ph> \n" + 
				"		<ph id=\"ph4\" sctype=\"N\">o</ph> \n" + 
				"	</pw> \n" + 
				"</model> ";
		Assert.assertEquals(expectedXML, phoRet.st.toString());
	}
	
	@Test
	public void testXMLToIPATranscript() throws UnsupportedEncodingException, RecognitionException {
		String xml = "<actual>\n" + 
				"	<pw>\n" + 
				"		<ph id=\"ph0\" sctype=\"N\">j</ph> \n" + 
				"		<ph id=\"ph1\" sctype=\"N\" hiatus=\"false\">e</ph> \n" + 
				"		<ph id=\"ph2\" sctype=\"LA\">l</ph> \n" + 
				"		<ph id=\"ph3\" sctype=\"O\">l</ph> \n" + 
				"		<ph id=\"ph4\" sctype=\"N\">o</ph> \n" + 
				"	</pw> \n" + 
				"</actual> ";
		
		final PhonTalkListener listener = 
				(msg) -> System.err.println(msg);
		
		// XML -> AST
		final TalkBankTokenSource tokenSource = new TalkBankTokenSource(new ByteArrayInputStream(xml.getBytes("UTF-8")));
		TokenStream	tokenStream = new CommonTokenStream(tokenSource);
		
		final TalkBank2ASTParser parser = new TalkBank2ASTParser(tokenStream);
		parser.setPhonTalkListener(listener);
		final CommonTree phoTree = parser.pho().getTree();
		
		// AST -> IPATranscript
		final CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(phoTree);
		final AST2Phon walker = new AST2Phon(nodeStream);
		walker.setPhonTalkListener(listener);
		
		// turn on fragment processing
		walker.setProcessFragments(true);
		
		final IPATranscript ipa = walker.pho().ipa;
		
		final String expected = "j:De:Dl:Ll:Oo:N";
		Assert.assertEquals(expected, ipa.toString(true));
	}
	
}
