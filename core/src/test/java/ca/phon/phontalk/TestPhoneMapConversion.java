package ca.phon.phontalk;

import java.io.*;

import org.antlr.runtime.*;
import org.antlr.runtime.tree.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import ca.phon.ipa.*;
import ca.phon.ipa.alignment.*;
import ca.phon.phontalk.parser.*;
import junit.framework.Assert;

@RunWith(JUnit4.class)
public class TestPhoneMapConversion {

	@Test
	public void testPhoneMap2XML() throws RecognitionException {
		final IPATranscript model = (new IPATranscriptBuilder()).append("hello").toIPATranscript();
		final IPATranscript actual = (new IPATranscriptBuilder()).append("elo").toIPATranscript();
		final PhoneMap phoneMap = (new PhoneAligner()).calculatePhoneAlignment(model, actual);
		
		// PhoneMap -> AST
		final AlignTreeBuilder alignTreeBuilder = new AlignTreeBuilder();
		final CommonTree modelTree = alignTreeBuilder.buildAlignmentTree(phoneMap);
		
		// AST -> XML
		final CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(modelTree);
		final AST2TalkBank walker = new AST2TalkBank(nodeStream);
		final AST2TalkBank.align_return alignRet = walker.align();
		
		final String expected = "<align>\n" + 
				"	<col><modelref>ph0</modelref> </col> \n" + 
				"	<col><modelref>ph1</modelref>  <actualref>ph0</actualref> </col> \n" + 
				"	<col><modelref>ph2</modelref> </col> \n" + 
				"	<col><modelref>ph3</modelref>  <actualref>ph1</actualref> </col> \n" + 
				"	<col><modelref>ph4</modelref>  <actualref>ph2</actualref> </col> \n" + 
				"</align> ";
		Assert.assertEquals(expected, alignRet.st.toString());
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
		
		final PhonTalkListener listener = 
				(msg) -> System.err.println(msg);
		
		// XML -> AST
		final TalkBankTokenSource tokenSource = new TalkBankTokenSource(new ByteArrayInputStream(xml.getBytes("UTF-8")));
		TokenStream	tokenStream = new CommonTokenStream(tokenSource);
		final TalkBank2ASTParser parser = new TalkBank2ASTParser(tokenStream);
		parser.setPhonTalkListener(listener);
		final CommonTree alignTree = parser.align().getTree();
		
		// AST -> PhoneMap
		final CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(alignTree);
		final AST2Phon walker = new AST2Phon(nodeStream);
		walker.setPhonTalkListener(listener);
		
		// turn on fragment processing
		walker.setProcessFragments(true);
		
		// these two properties are necessary to build the PhoneMap object
		walker.setProperty("model", model);
		walker.setProperty("actual", actual);
		
		final PhoneMap phoneMap = walker.align().val;
		
		final String expected = "h↔∅,e↔e,l↔∅,l↔l,o↔o";
		Assert.assertEquals(expected, phoneMap.toString());
	}
	
	@Test
	public void testString2PhoneMap() {
		final IPATranscript model = (new IPATranscriptBuilder()).append("hello").toIPATranscript();
		final IPATranscript actual = (new IPATranscriptBuilder()).append("elo").toIPATranscript();
		final String alignTxt = "h↔∅,e↔e,l↔∅,l↔l,o↔o";
		
		final PhoneMap phoneMap = PhoneMap.fromString(model, actual, alignTxt);
		
		Assert.assertEquals(alignTxt, phoneMap.toString());
	}
	
}
