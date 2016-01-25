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

	private final AntlrTokens chatTokens = new AntlrTokens("Chat.tokens");
	
	/**
	 * Build the alignment tree for the given {@link PhoneMap}
	 * 
	 */
	public CommonTree buildAlignmentTree(PhoneMap pm) {
		CommonTree alignNode = AntlrUtils.createToken(chatTokens, "ALIGN_START");

		final IPATranscript targetSoundPhones = pm.getTargetRep().stripDiacritics().audiblePhones();
		final IPATranscript actualSoundPhones = pm.getActualRep().stripDiacritics().audiblePhones();

		// add alignment columns
		int lastTIdx = 0;
		int lastAIdx = 0;

		for(int i = 0; i < pm.getAlignmentLength(); i++) {
			CommonTree colTree = AntlrUtils.createToken(chatTokens, "COL_START");
			colTree.setParent(alignNode);
			alignNode.addChild(colTree);

			final IPAElement tP = pm.getTopAlignmentElements().get(i);
			final IPAElement aP = pm.getBottomAlignmentElements().get(i);

			if(tP != null) {

				int tpIdx = targetSoundPhones.indexOf(tP);

				if(tpIdx < 0) {
					LOGGER.warning("Invalid position ref for phone '" + tP
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

				int apIdx = actualSoundPhones.indexOf(aP);

				if(apIdx < 0) {
					LOGGER.warning("Invalid position ref for phone '" + aP
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
