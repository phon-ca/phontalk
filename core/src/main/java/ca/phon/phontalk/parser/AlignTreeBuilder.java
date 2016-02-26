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

import java.util.logging.Logger;

import org.antlr.runtime.tree.CommonTree;

import ca.phon.ipa.IPAElement;
import ca.phon.ipa.IPATranscript;
import ca.phon.ipa.alignment.PhoneMap;

public class AlignTreeBuilder {
	
	private final Logger LOGGER = Logger.getLogger(AlignTreeBuilder.class.getName());

	private final AntlrTokens talkbankTokens = new AntlrTokens("TalkBank2AST.tokens");
	
	/**
	 * Build the alignment tree for the given {@link PhoneMap}
	 * 
	 */
	public CommonTree buildAlignmentTree(PhoneMap pm) {
		CommonTree alignNode = AntlrUtils.createToken(talkbankTokens, "ALIGN_START");

		// add alignment columns
		int targetIdx = 0;
		int actualIdx = 0;

		for(int i = 0; i < pm.getAlignmentLength(); i++) {
			CommonTree colTree = AntlrUtils.createToken(talkbankTokens, "COL_START");
			colTree.setParent(alignNode);
			alignNode.addChild(colTree);

			final IPAElement tP = pm.getTopAlignmentElements().get(i);
			final IPAElement aP = pm.getBottomAlignmentElements().get(i);

			if(tP != null) {
				CommonTree modelTree = AntlrUtils.createToken(talkbankTokens, "MODELREF_START");
				String modelRef = "ph" + (targetIdx++);
				AntlrUtils.addTextNode(modelTree, talkbankTokens, modelRef);
				modelTree.setParent(colTree);
				colTree.addChild(modelTree);
			}

			if(aP != null) {
				CommonTree actualTree = AntlrUtils.createToken(talkbankTokens, "ACTUALREF_START");
				String actualRef = "ph" + (actualIdx++);
				AntlrUtils.addTextNode(actualTree, talkbankTokens, actualRef);
				actualTree.setParent(colTree);
				colTree.addChild(actualTree);
			}
		}
		
		return alignNode;
	}
	
}
