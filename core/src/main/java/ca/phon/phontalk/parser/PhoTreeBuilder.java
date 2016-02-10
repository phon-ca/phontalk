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
import java.util.logging.Logger;

import org.antlr.runtime.tree.CommonTree;

import ca.phon.ipa.CompoundWordMarker;
import ca.phon.ipa.Contraction;
import ca.phon.ipa.IPAElement;
import ca.phon.ipa.IPATranscript;
import ca.phon.ipa.IntonationGroup;
import ca.phon.ipa.IntraWordPause;
import ca.phon.ipa.Linker;
import ca.phon.ipa.Pause;
import ca.phon.ipa.Phone;
import ca.phon.ipa.Sandhi;
import ca.phon.ipa.StressMarker;
import ca.phon.ipa.StressType;
import ca.phon.ipa.SyllableBoundary;
import ca.phon.syllable.SyllableConstituentType;
import ca.phon.syllable.SyllableStress;
import ca.phon.util.Range;
import ca.phon.visitor.VisitorAdapter;
import ca.phon.visitor.annotation.Visits;

/**
 * Helper class for building CHAT trees for the 'pho' element in
 * TalkBank.
 */
public class PhoTreeBuilder {
	
	private final static Logger LOGGER = Logger.getLogger(PhoTreeBuilder.class.getName());
	
	private final AntlrTokens chatTokens = new AntlrTokens("Chat.tokens");
	
	public CommonTree buildPhoTree(String eleName, IPATranscript ipa) {
		final CommonTree retVal = AntlrUtils.createToken(chatTokens, eleName.toUpperCase() + "_START");
		final List<CommonTree> pwTrees = buildPwTrees(ipa);
		for(CommonTree pwTree:pwTrees) {
			retVal.addChild(pwTree);
			pwTree.setParent(retVal);
		}
		return retVal;
	}
	
	/**
	 * Helper method for building 'model' trees.
	 * 
	 * @param List<Phone> phones
	 * @return tree for model pho
	 */
	public CommonTree buildModelTree(IPATranscript ipa) {
		return buildPhoTree("model", ipa);
	}
	
	/**
	 * Helper method for building 'actual' trees.
	 * 
	 * @param List<Phone> phones
	 * @return tree for actual pho
	 */
	public CommonTree buildActualTree(IPATranscript ipa) {
		return buildPhoTree("actual", ipa);
	}
	
	/**
	 * Build an array of &lt;ph&gt; trees from a given list of
	 * phones.
	 * 
	 * @param phones
	 * @return CommonTree[]
	 */
	public List<CommonTree> buildPwTrees(IPATranscript ipa) {
		final ArrayList<CommonTree> pwTrees = 
				new ArrayList<CommonTree>();
		
		for(IPATranscript word:ipa.words()) {
			final CommonTree pwTree = AntlrUtils.createToken(chatTokens, "PW_START");
			pwTrees.add(pwTree);
			
			word.accept(new ElementVisitor(pwTree));
		}
		
		return pwTrees;
	}
	
	class ElementVisitor extends VisitorAdapter<IPAElement> {
		
		CommonTree pwTree;
		
		private int phIdx = 0;
		
		public ElementVisitor(CommonTree tree) {
			pwTree = tree;
		}

		@Override
		public void fallbackVisit(IPAElement p) {
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

			AntlrUtils.addTextNode(phTree, chatTokens, p.getText());
		}
		
		@Visits
		public void visitCompoundWordMarker(CompoundWordMarker cm) {
			// add wk
			CommonTree wkTree = AntlrUtils.createToken(chatTokens, "WK_START");
			wkTree.setParent(pwTree);
			pwTree.addChild(wkTree);
			
			final String wkType = "cmp";
			CommonTree wkTypeTree = AntlrUtils.createToken(chatTokens, "WK_ATTR_TYPE");
			wkTypeTree.getToken().setText(wkType);
			wkTypeTree.setParent(wkTree);
			wkTree.addChild(wkTypeTree);
		}
		
		@Visits
		public void visitStressMarker(StressMarker ss) {
			// add ss
			CommonTree ssTree = AntlrUtils.createToken(chatTokens, "SS_START");
			ssTree.setParent(pwTree);
			pwTree.addChild(ssTree);

			CommonTree ssTypeTree = AntlrUtils.createToken(chatTokens, "SS_ATTR_TYPE");
			if(ss.getType() == StressType.PRIMARY) {
				ssTypeTree.getToken().setText("1");
			} else if(ss.getType() == StressType.SECONDARY) {
				ssTypeTree.getToken().setText("2");
			} else {
				// something strange is happening...
				ssTypeTree.getToken().setText("1");
			}
			
			ssTypeTree.setParent(ssTree);
			ssTree.addChild(ssTypeTree);
		}
		
	}
	
}
