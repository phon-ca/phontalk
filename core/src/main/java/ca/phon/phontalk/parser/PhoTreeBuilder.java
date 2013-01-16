package ca.phon.phontalk.parser;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;

import ca.phon.exceptions.ParserException;
import ca.phon.phone.Phone;
import ca.phon.phone.PhoneSequenceMatcher;
import ca.phon.syllable.Syllable;
import ca.phon.syllable.SyllableConstituentType;
import ca.phon.util.Range;

/**
 *
 */
public class PhoTreeBuilder {
	
	public static CommonTree[] buildPwTrees(List<Phone> phones) {
		final ArrayList<CommonTree> pwTrees = 
				new ArrayList<CommonTree>();
		
		final AntlrTokens chatTokens = new AntlrTokens("Chat.tokens");
		
		// split into 'words' using phonex
		PhoneSequenceMatcher phonex = null;
		try {
			phonex =
					PhoneSequenceMatcher.compile("{}:-WordBoundaryMarker*");
		} catch (ParserException ex) {
			ex.printStackTrace(); // sound never occur
		}
		
		int phIdx = 0;
		List<Range> wordRanges = phonex.findRanges(phones);
		for(Range wordRange:wordRanges) {
			final CommonTree pwTree = AntlrUtils.createToken(chatTokens, "PW_START");
			pwTrees.add(pwTree);
			
			for(int pIndex:wordRange) {
				Phone p = phones.get(pIndex);

				if(p.getPhoneString().matches("[+~]")) {
					// add wk
					CommonTree wkTree = AntlrUtils.createToken(chatTokens, "WK_START");
					wkTree.setParent(pwTree);
					pwTree.addChild(wkTree);
					
					final String wkType =
							(p.getPhoneString().equals("+") ? "cmp" : "cli");

					CommonTree wkTypeTree = AntlrUtils.createToken(chatTokens, "WK_ATTR_TYPE");
					wkTypeTree.getToken().setText(wkType);
					wkTypeTree.setParent(wkTree);
					wkTree.addChild(wkTypeTree);
				} else if(p.getScType() == SyllableConstituentType.SyllableStressMarker) {
					// add ss
					CommonTree ssTree = AntlrUtils.createToken(chatTokens, "SS_START");
					ssTree.setParent(pwTree);
					pwTree.addChild(ssTree);

					CommonTree ssTypeTree = AntlrUtils.createToken(chatTokens, "SS_ATTR_TYPE");
					if(p.getPhoneString().contains(Syllable.PrimaryStressChar+"")) {
						ssTypeTree.getToken().setText("1");
					} else if(p.getPhoneString().contains(Syllable.SecondaryStressChar+"")) {
						ssTypeTree.getToken().setText("2");
					} else {
						// something strange is happening...
						ssTypeTree.getToken().setText("1");
					}
					
					ssTypeTree.setParent(ssTree);
					ssTree.addChild(ssTypeTree);
				} else {
					// add ph
					String phId = "ph" + (phIdx++);
					CommonTree phTree = AntlrUtils.createToken(chatTokens, "PH_START");
					phTree.setParent(pwTree);
					pwTree.addChild(phTree);

					CommonTree phIdTree = AntlrUtils.createToken(chatTokens, "PH_ATTR_ID");
					phIdTree.getToken().setText(phId);
					phIdTree.setParent(phTree);
					phTree.addChild(phIdTree);

					CommonTree scTree = AntlrUtils.createToken(chatTokens, "PH_ATTR_SCTYPE");
					scTree.getToken().setText(p.getScType().getIdentifier());
					scTree.setParent(phTree);
					phTree.addChild(scTree);

					AntlrUtils.addTextNode(phTree, chatTokens, p.getPhoneString());
				}
			}
		}
		
		return pwTrees.toArray(new CommonTree[0]);
	}
	
}
