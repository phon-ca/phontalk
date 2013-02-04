package ca.phon.phontalk.parser;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;

import ca.phon.alignment.PhoneMap;
import ca.phon.exceptions.ParserException;
import ca.phon.phone.Phone;
import ca.phon.phone.PhoneSequenceMatcher;
import ca.phon.syllable.Syllable;
import ca.phon.syllable.SyllableConstituentType;
import ca.phon.system.logger.PhonLogger;
import ca.phon.util.Range;

/**
 * Helper class for building CHAT trees for the 'pho' element in
 * TalkBank.
 */
public class PhoTreeBuilder {
	
	private final AntlrTokens chatTokens = new AntlrTokens("Chat.tokens");
	
	/**
	 * Build the alignment tree for the given phonemap
	 * 
	 */
	public CommonTree buildAlignmentTree(PhoneMap pm) {
		CommonTree alignNode = AntlrUtils.createToken(chatTokens, "ALIGN_START");

		List<Phone> targetSoundPhones = new ArrayList<Phone>();
		for(Phone p:pm.getTargetRep().getPhones()) {
			boolean addPhone = true;

			if(p.getPhoneString().equals("+")) {
				addPhone = false;
			} else if(p.getScType() == SyllableConstituentType.SyllableStressMarker ||
					p.getScType() == SyllableConstituentType.WordBoundaryMarker ||
					p.getScType() == SyllableConstituentType.SyllableBoundaryMarker) {
				addPhone = false;
			} 

			if(addPhone) targetSoundPhones.add(p);
		}

		List<Phone> actualSoundPhones = new ArrayList<Phone>();
		for(Phone p:pm.getActualRep().getPhones()) {
			boolean addPhone = true;

			if(p.getPhoneString().equals("+")) {
				addPhone = false;
			} else if(p.getScType() == SyllableConstituentType.SyllableStressMarker ||
					p.getScType() == SyllableConstituentType.WordBoundaryMarker ||
					p.getScType() == SyllableConstituentType.SyllableBoundaryMarker) {
				addPhone = false;
			}

			if(addPhone) actualSoundPhones.add(p);
		}

		// add alignment columns
		int lastTIdx = 0;
		int lastAIdx = 0;

		for(int i = 0; i < pm.getAlignmentLength(); i++) {
			CommonTree colTree = AntlrUtils.createToken(chatTokens, "COL_START");
			colTree.setParent(alignNode);
			alignNode.addChild(colTree);

			Phone tP = pm.getTopAlignmentElements().get(i);
			Phone aP = pm.getBottomAlignmentElements().get(i);

			if(tP != null) {

				int tpIdx = -1;
				for(int j = lastTIdx; j < targetSoundPhones.size(); j++) {
					Phone p = targetSoundPhones.get(j);
					if(p.equals(tP)) {
						tpIdx = j;
						break;
					}
				}

				if(tpIdx < 0) {
					PhonLogger.warning("Invalid position ref for phone '" + tP
							+ "': '" +tpIdx+ "'");
					break;
				}

				CommonTree modelTree = AntlrUtils.createToken(chatTokens, "MODELREF_START");
				String modelRef = "ph" + tpIdx;
				AntlrUtils.addTextNode(modelTree, chatTokens, modelRef);
				modelTree.setParent(colTree);
				colTree.addChild(modelTree);

				lastTIdx = tpIdx;
			}

			if(aP != null) {

				int apIdx = -1;
				for(int j = lastAIdx; j < actualSoundPhones.size(); j++) {
					Phone p = actualSoundPhones.get(j);
					if(p.equals(aP)) {
						apIdx = j;
						break;
					}
				}

				if(apIdx < 0) {
					PhonLogger.warning("Invalid position ref for phone '" + aP
							+ "': '" +apIdx+ "'");
					break;
				}

				CommonTree actualTree = AntlrUtils.createToken(chatTokens, "ACTUALREF_START");
				String actualRef = "ph" + apIdx;
				AntlrUtils.addTextNode(actualTree, chatTokens, actualRef);
				actualTree.setParent(colTree);
				colTree.addChild(actualTree);

				lastAIdx = apIdx;
			}
		}
		
		return alignNode;
	}
	
	/**
	 * Build an array of &lt;ph&gt; trees from a given list of
	 * phones.
	 * 
	 * @param phones
	 * @return CommonTree[]
	 */
	public List<CommonTree> buildPwTrees(List<Phone> phones) {
		final ArrayList<CommonTree> pwTrees = 
				new ArrayList<CommonTree>();
		
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
		
		return pwTrees;
	}
	
}
