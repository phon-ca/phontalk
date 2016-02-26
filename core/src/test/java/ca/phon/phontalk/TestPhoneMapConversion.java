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
import ca.phon.ipa.alignment.PhoneAligner;
import ca.phon.ipa.alignment.PhoneMap;
import ca.phon.phontalk.parser.AST2Phon;
import ca.phon.phontalk.parser.AST2TalkBank;
import ca.phon.phontalk.parser.AlignTreeBuilder;
import ca.phon.phontalk.parser.TalkBank2ASTParser;
import ca.phon.phontalk.parser.TalkBankTokenSource;

@RunWith(JUnit4.class)
public class TestPhoneMapConversion {

	@Test
	public void testPhoneMap2XML() throws RecognitionException {
		final IPATranscript model = (new IPATranscriptBuilder()).append("hello").toIPATranscript();
		final IPATranscript actual = (new IPATranscriptBuilder()).append("elo").toIPATranscript();
		final PhoneMap phoneMap = (new PhoneAligner()).calculatePhoneMap(model, actual);
		
		// PhoneMap -> AST
		final AlignTreeBuilder alignTreeBuilder = new AlignTreeBuilder();
		final CommonTree modelTree = alignTreeBuilder.buildAlignmentTree(phoneMap);
		
		// AST -> XML
		final CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(modelTree);
		final AST2TalkBank walker = new AST2TalkBank(nodeStream);
		final AST2TalkBank.align_return alignRet = walker.align();
		System.out.println(alignRet.st.toString());
	}
	
	@Test
	public void testXML2PhoneMap() throws UnsupportedEncodingException, RecognitionException {
		final IPATranscript model = (new IPATranscriptBuilder()).append("hello").toIPATranscript();
		final IPATranscript actual = (new IPATranscriptBuilder()).append("elo").toIPATranscript();
		final String xml = "<align>\n" + 
				"	<col><modelref>ph0</modelref> </col> \n" + 
				"	<col><modelref>ph1</modelref>  <actualref>ph0</actualref> </col> \n" + 
				"	<col><modelref>ph2</modelref> </col> \n" + 
				"	<col><modelref>ph3</modelref>  <actualref>ph1</actualref> </col> \n" + 
				"	<col><modelref>ph4</modelref>  <actualref>ph2</actualref> </col> \n" + 
				"</align>";
		
		// XML -> AST
		final TalkBankTokenSource tokenSource = new TalkBankTokenSource(new ByteArrayInputStream(xml.getBytes("UTF-8")));
		TokenStream	tokenStream = new CommonTokenStream(tokenSource);
		final TalkBank2ASTParser parser = new TalkBank2ASTParser(tokenStream);
		parser.setPhonTalkListener( (msg) -> System.err.println(msg.toString()) );
		final CommonTree alignTree = parser.align().getTree();
		
		// AST -> PhoneMap
		final CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(alignTree);
		final AST2Phon walker = new AST2Phon(nodeStream);
		
		// turn on fragment processing
		walker.setProcessFragments(true);
		
		// these two properties are necessary to build the PhoneMap object
		walker.setProperty("model", model);
		walker.setProperty("actual", actual);
		
		final PhoneMap phoneMap = walker.align().val;
		
		System.out.println(phoneMap.toString(true));
	}
	
}
