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
import ca.phon.ipa.IPAElement;
import ca.phon.ipa.IPATranscript;
import ca.phon.ipa.StressMarker;
import ca.phon.ipa.StressType;
import ca.phon.syllable.SyllabificationInfo;
import ca.phon.syllable.SyllableConstituentType;
import ca.phon.visitor.VisitorAdapter;
import ca.phon.visitor.annotation.Visits;

/**
 * Helper class for building CHAT trees for the 'pho' element in
 * TalkBank.
 */
public class PhoTreeBuilder {
	
	private final static Logger LOGGER = Logger.getLogger(PhoTreeBuilder.class.getName());
	
	private final AntlrTokens talkbankTokens = new AntlrTokens("TalkBank2AST.tokens");
	
	public CommonTree buildPhoTree(String eleName, IPATranscript ipa) {
		final CommonTree retVal = AntlrUtils.createToken(talkbankTokens, eleName.toUpperCase() + "_START");
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
		
		int phIdx = 0;
		for(IPATranscript word:ipa.words()) {
			final CommonTree pwTree = AntlrUtils.createToken(talkbankTokens, "PW_START");
			pwTrees.add(pwTree);
			
			ElementVisitor visitor = new ElementVisitor(pwTree, phIdx);
			word.accept(visitor);
			phIdx = visitor.phIdx;
		}
		
		return pwTrees;
	}
	
	public class ElementVisitor extends VisitorAdapter<IPAElement> {
		
		CommonTree pwTree;
		
		private int phIdx = 0;
		
		private SyllableConstituentType lastType = SyllableConstituentType.UNKNOWN;
		
		public ElementVisitor(CommonTree tree, int phIdx) {
			pwTree = tree;
			this.phIdx = phIdx;
		}

		@Override
		public void fallbackVisit(IPAElement p) {
			// add ph
			String phId = "ph" + (phIdx++);
			CommonTree phTree = AntlrUtils.createToken(talkbankTokens, "PH_START");
			phTree.setParent(pwTree);
			pwTree.addChild(phTree);

			CommonTree phIdTree = AntlrUtils.createToken(talkbankTokens, "PH_ATTR_ID");
			phIdTree.getToken().setText(phId);
			phIdTree.setParent(phTree);
			phTree.addChild(phIdTree);

			CommonTree scTree = AntlrUtils.createToken(talkbankTokens, "PH_ATTR_SCTYPE");
			scTree.getToken().setText(p.getScType().getIdentifier());
			scTree.setParent(phTree);
			phTree.addChild(scTree);
			
			if(lastType == SyllableConstituentType.NUCLEUS &&
					p.getScType() == SyllableConstituentType.NUCLEUS
					&& p.getExtension(SyllabificationInfo.class).isDiphthongMember()) {
				CommonTree hiatusTree = AntlrUtils.createToken(talkbankTokens, "PH_ATTR_HIATUS");
				hiatusTree.getToken().setText("false");
				hiatusTree.setParent(phTree);
				phTree.addChild(hiatusTree);
			}

			lastType = p.getScType();
			
			AntlrUtils.addTextNode(phTree, talkbankTokens, p.getText());
		}
		
		@Visits
		public void visitCompoundWordMarker(CompoundWordMarker cm) {
			// add wk
			CommonTree wkTree = AntlrUtils.createToken(talkbankTokens, "WK_START");
			wkTree.setParent(pwTree);
			pwTree.addChild(wkTree);
			
			final String wkType = "cmp";
			CommonTree wkTypeTree = AntlrUtils.createToken(talkbankTokens, "WK_ATTR_TYPE");
			wkTypeTree.getToken().setText(wkType);
			wkTypeTree.setParent(wkTree);
			wkTree.addChild(wkTypeTree);
		}
		
		@Visits
		public void visitStressMarker(StressMarker ss) {
			// add ss
			CommonTree ssTree = AntlrUtils.createToken(talkbankTokens, "SS_START");
			ssTree.setParent(pwTree);
			pwTree.addChild(ssTree);

			CommonTree ssTypeTree = AntlrUtils.createToken(talkbankTokens, "SS_ATTR_TYPE");
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
