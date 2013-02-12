package ca.phon.phontalk.parser;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;

import ca.phon.alignment.PhoneMap;
import ca.phon.phone.Phone;
import ca.phon.syllable.SyllableConstituentType;
import ca.phon.system.logger.PhonLogger;

public class AlignTreeBuilder {

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
	
}
