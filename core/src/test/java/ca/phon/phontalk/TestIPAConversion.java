package ca.phon.phontalk;

import java.io.ByteArrayInputStream;
import java.io.StringBufferInputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.stream.XMLEventReader;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.stringtemplate.StringTemplateWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import ca.phon.ipa.IPATranscript;
import ca.phon.ipa.IPATranscriptBuilder;
import ca.phon.phontalk.parser.TalkBankTokenSource;
import ca.phon.phontalk.parser.PhoTreeBuilder;
import ca.phon.phontalk.parser.Phon2XmlWalker;

@RunWith(JUnit4.class)
public class TestIPAConversion {

	@Test
	public void testIPATranscriptToXML() throws RecognitionException {
		final String txt = "";
		final IPATranscript ipa = (new IPATranscriptBuilder()).append(txt).toIPATranscript();
	
		final PhoTreeBuilder phoTreeBuilder = new PhoTreeBuilder();
		final CommonTree modelTree = phoTreeBuilder.buildModelTree(ipa);
		final CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(modelTree);
		final Phon2XmlWalker walker = new Phon2XmlWalker(nodeStream);
		
		final Phon2XmlWalker.pho_return phoRet = walker.pho();
		
		System.out.println(phoRet.st.toString());
	}
	
	@Test
	public void testXMLToIPATranscript() throws UnsupportedEncodingException {
		String xml = "<model>\n" + 
				"	<pw>\n" + 
				"		<ph id=\"ph0\" sctype=\"UK\">h</ph> \n" + 
				"		<ph id=\"ph1\" sctype=\"UK\">e</ph> \n" + 
				"		<ph id=\"ph2\" sctype=\"UK\">l</ph> \n" + 
				"		<ph id=\"ph3\" sctype=\"UK\">l</ph> \n" + 
				"		<ph id=\"ph4\" sctype=\"UK\">o</ph> \n" + 
				"	</pw> \n" + 
				"</model>";
		final TalkBankTokenSource tokenSource = new TalkBankTokenSource(new ByteArrayInputStream(xml.getBytes("UTF-8")));
		TokenStream	tokenStream = new CommonTokenStream(tokenSource);
		
	}
	
}
