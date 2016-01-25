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

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;

import ca.phon.ipa.IPAElement;
import ca.phon.syllable.SyllableConstituentType;
import ca.phon.util.Range;

/**
 * Helper class for building CHAT trees for the 'pho' element in
 * TalkBank.
 */
public class PhoTreeBuilder {
	
	private final AntlrTokens chatTokens = new AntlrTokens("Chat.tokens");
	
	/**
	 * Helper method for bulding 'model' trees.
	 * 
	 * @param List<Phone> phones
	 * @return tree for model pho
	 */
	public CommonTree buildModelTree(List<IPAElement> phones) {
		final CommonTree retVal = AntlrUtils.createToken(chatTokens, "MODEL_START");
		final List<CommonTree> pwTrees = buildPwTrees(phones);
		for(CommonTree pwTree:pwTrees) {
			retVal.addChild(pwTree);
			pwTree.setParent(retVal);
		}
		return retVal;
	}
	
	/**
	 * Helper method for bulding 'actual' trees.
	 * 
	 * @param List<Phone> phones
	 * @return tree for actual pho
	 */
	public CommonTree buildActualTree(List<IPAElement> phones) {
		final CommonTree retVal = AntlrUtils.createToken(chatTokens, "ACTUAL_START");
		final List<CommonTree> pwTrees = buildPwTrees(phones);
		for(CommonTree pwTree:pwTrees) {
			retVal.addChild(pwTree);
			pwTree.setParent(retVal);
		}
		return retVal;
	}
	
	/**
	 * Build an array of &lt;ph&gt; trees from a given list of
	 * phones.
	 * 
	 * @param phones
	 * @return CommonTree[]
	 */
	public List<CommonTree> buildPwTrees(List<IPAElement> phones) {
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
