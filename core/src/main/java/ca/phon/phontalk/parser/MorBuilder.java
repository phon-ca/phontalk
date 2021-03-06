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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.runtime.tree.CommonTree;

import ca.phon.phontalk.PhonTalkMessage;

/**
 * Utility class for building morphology trees
 * given strings coded for the CHAT/Phon %mor/Morphology
 * tiers.
 * 
 * This class will build an antlr3 {@link CommonTree} which
 * can be used with the chat tree walkers.
 *
 */
public class MorBuilder {

	private final AntlrTokens talkbankTokens = 
			new AntlrTokens("TalkBank2AST.tokens");
	
	/**
	 * Attach the given GRASP tree to the 
	 * given MOR tree.
	 * 
	 * @param graspTree
	 * @param morTree
	 * @return the reference to the provided 
	 *  morTree
	 */
	@SuppressWarnings("unchecked")
	public CommonTree attachGrasp(CommonTree morTree, CommonTree graspTree) {
		final Integer prevTypes[] = 
			{ 
				talkbankTokens.getTokenType("GRA_ATTR_OMITTED"),
				talkbankTokens.getTokenType("GRA_ATTR_TYPE"),
				talkbankTokens.getTokenType("MW_START"),
				talkbankTokens.getTokenType("MWC_START"),
				talkbankTokens.getTokenType("MT_START"),
				talkbankTokens.getTokenType("MENX_START"),
				talkbankTokens.getTokenType("GRA_START")
			};
		final List<Integer> checkTypes = Arrays.asList(prevTypes);
		int insertionPt = -1;
		for(int i = 0; i < morTree.getChildCount(); i++) {
			final CommonTree child = (CommonTree)morTree.getChild(i);
			if(checkTypes.contains(child.getToken().getType())) {
				insertionPt = i+1;
			}
		}
		if(insertionPt >= 0) {
			morTree.insertChild(insertionPt, graspTree);
			morTree.freshenParentAndChildIndexes();
		}
		return morTree;
	}
	
	/**
	 * Build a GRASP (%gra) tree to attach to mor.
	 * 
	 * 
	 * @param gra, must be a string of 4 values separated by pipes (i.e., 1|2|3|4)
	 * @return the grasp tree
	 */
	public CommonTree buildGraspTree(String gra) 
		throws IllegalArgumentException {
		
		final CommonTree graTree = AntlrUtils.createToken(talkbankTokens, "GRA_START");
		final CommonTree graTypeTree = AntlrUtils.createToken(talkbankTokens, "GRA_ATTR_TYPE");
		graTypeTree.getToken().setText("gra");
		graTree.addChild(graTypeTree);
		graTypeTree.setParent(graTree);
		
		final String graParts[] = gra.split("\\|");
		if(graParts.length != 3) {
			throw new IllegalArgumentException("Invalid GRASP string '" + gra + "'");
		}
		
		final String idxVal = graParts[0];
		final CommonTree idxTree = AntlrUtils.createToken(talkbankTokens, "GRA_ATTR_INDEX");
		idxTree.getToken().setText(idxVal);
		idxTree.setParent(graTree);
		graTree.addChild(idxTree);
		
		final String headVal = graParts[1];
		final CommonTree headTree = AntlrUtils.createToken(talkbankTokens, "GRA_ATTR_HEAD");
		headTree.getToken().setText(headVal);
		headTree.setParent(graTree);
		graTree.addChild(headTree);
		
		final String relVal = graParts[2];
		final CommonTree relTree = AntlrUtils.createToken(talkbankTokens, "GRA_ATTR_RELATION");
		relTree.getToken().setText(relVal);
		relTree.setParent(graTree);
		graTree.addChild(relTree);
		
		return graTree;
	}
	
	/**
	 * Build a common tree for the given mor
	 * string.
	 * 
	 * @param mor
	 * @return the generated CommonTree
	 * @throws PhonTalkMessage if the string cannot be parsed
	 */
	public CommonTree buildMorTree(String mor) 
		throws IllegalArgumentException {
		
		if(mor.length() == 0) {
			return null;
		}
		// create a new string buffer, this buffer
		// will be modified as pieces of the mor string
		// are processed
		final StringBuffer morBuffer = new StringBuffer(mor);
		
		// create a parent mor node
		final CommonTree morTree = AntlrUtils.createToken(talkbankTokens, "MOR_START");
		
		// first check to see if we need to add the 'omitted'
		// attribute
		final CommonTree omitted = omitted(morTree, morBuffer);
		if(omitted != null) {
			omitted.setParent(morTree);
			morTree.addChild(omitted);
		}
		
		// check for mor prefixes
		final List<CommonTree> morpres = morpre(morTree, morBuffer);
		final List<CommonTree> morposts = morpost(morTree, morBuffer);
		
		final List<CommonTree> morenxs = morenx(morTree, morBuffer);
		
		final CommonTree choiceTree = morchoice(morTree, morBuffer);
		choiceTree.setParent(morTree);
		morTree.addChild(choiceTree);
		
		for(CommonTree menx:morenxs) {
			menx.setParent(morTree);
			morTree.addChild(menx);
		}
		
		for(CommonTree morpre:morpres) {
			morpre.setParent(morTree);
			morTree.addChild(morpre);
		}
		
		for(CommonTree morpost:morposts) {
			morpost.setParent(morTree);
			morTree.addChild(morpost);
		}
		
		return morTree;
	}
	
	private CommonTree omitted(CommonTree tree, StringBuffer buffer) {
		CommonTree retVal = null;
		
		int zeroIdx = -1;
		int lastIndexOfPipe = buffer.lastIndexOf("|");
		
		if(buffer.charAt(0) == '0') 
			zeroIdx = 0;
		else if((lastIndexOfPipe >= 0 && lastIndexOfPipe < buffer.length()-1 && buffer.charAt(lastIndexOfPipe+1) == '0')) 
			zeroIdx = lastIndexOfPipe+1;
		if(zeroIdx >= 0) {
			// add attribute node to tree
			final CommonTree omittedNode = 
					AntlrUtils.createToken(talkbankTokens, "MOR_ATTR_OMITTED");
			omittedNode.getToken().setText("true");
			
			buffer.delete(zeroIdx, zeroIdx+1);
			
			retVal = omittedNode;
		}
		return retVal;
	}
	
	private List<CommonTree> morpre(CommonTree tree, StringBuffer buffer)
			throws IllegalArgumentException {
		final List<CommonTree> retVal = new ArrayList<CommonTree>();
		final int morStart = buffer.lastIndexOf("$");
		if(morStart >= 0) {
			final String presection = buffer.substring(0, morStart);
			final String morpres[] = presection.split("\\$");
			for(String morpre:morpres) {
				// create a new mor pre tree
				final CommonTree morPreTree = buildMorTree(morpre);
				morPreTree.getToken().setType(talkbankTokens.getTokenType("MOR_PRE_START"));
				retVal.add(morPreTree);
			}
			
			buffer.delete(0, morStart+1);
		}
		return retVal;
	}
	
	private List<CommonTree> morpost(CommonTree tree, StringBuffer buffer) 
			throws IllegalArgumentException {
		final List<CommonTree> retVal = new ArrayList<CommonTree>();
		final int morEnd = buffer.indexOf("~");
		if(morEnd >= 0) {
			final String postsection = buffer.substring(morEnd+1);
			final String morposts[] = postsection.split("~");
			for(String morpost:morposts) {
				final CommonTree morPostTree = 
						buildMorTree(morpost);
				morPostTree.getToken().setType(talkbankTokens.getTokenType("MOR_POST_START"));
				retVal.add(morPostTree);
			}
			
			
			buffer.delete(morEnd, buffer.length());
		}
		return retVal;
	}
	
	private List<CommonTree> morenx(CommonTree tree, StringBuffer buffer) {
		final List<CommonTree> retVal = new ArrayList<CommonTree>();
		final int morEnd = buffer.indexOf("=");
		if(morEnd >= 0) {
			final String menxsection = buffer.substring(morEnd+1);
			final String menxvals[] = menxsection.split("=");
			for(String menxval:menxvals) {
				final CommonTree menxTree = 
						AntlrUtils.createToken(talkbankTokens, "MENX_START");
				retVal.add(menxTree);
				AntlrUtils.addTextNode(menxTree, talkbankTokens, menxval);
			}
			
			buffer.delete(morEnd, buffer.length());
		}
		return retVal;
	}
	
	private CommonTree morchoice(CommonTree tree, StringBuffer buffer) {
		CommonTree retVal = null;
		// the buffer should now be cut down to just one
		// of mw, mt, or mwc
		if(buffer.indexOf("+") >= 0) {
			// mwc
			retVal = mwc(tree, buffer);
		} else if(buffer.length() == 1 || buffer.toString().startsWith("(")) {
			if(buffer.charAt(0) == '.'
					|| buffer.charAt(0) == '!'
					|| buffer.charAt(0) == '?'
					|| buffer.charAt(0) == '(') {
				retVal = mt(tree, buffer);
			} else {
				retVal = mw(tree, buffer);
			}
		} else {
			retVal = mw(tree, buffer);
		}
		return retVal;
	}
	
	private CommonTree mwc(CommonTree tree, StringBuffer buffer) {
		final CommonTree retVal = AntlrUtils.createToken(talkbankTokens, "MWC_START");
		
		final List<CommonTree> mpfxs = mpfx(tree, buffer);
		for(CommonTree mpfx:mpfxs) {
			retVal.addChild(mpfx);
			mpfx.setParent(retVal);
		}
		
		final int mwcPosEnd = buffer.indexOf("|");
		final String mwcPosVal = buffer.substring(0, mwcPosEnd);
		buffer.delete(0, mwcPosEnd+1);
		final CommonTree pos = pos(retVal, new StringBuffer(mwcPosVal));
		pos.setParent(retVal);
		retVal.addChild(pos);
		
		final String mwsection = buffer.toString();
		final String mws[] = mwsection.split("[+]");
		for(String mw:mws) {
			if(mw.length() == 0) continue;
			final CommonTree mwTree = mw(tree, new StringBuffer(mw));
			mwTree.setParent(retVal);
			retVal.addChild(mwTree);
		}
		return retVal;
	}
	
	private CommonTree mw(CommonTree tree, StringBuffer buffer) {
		final CommonTree retVal =
				AntlrUtils.createToken(talkbankTokens, "MW_START");
		
		// split at '|' first
		final String mwParts[] = buffer.toString().split("\\|");
		if(mwParts.length == 2) {
			final StringBuffer pfxPos = new StringBuffer(mwParts[0]);
			final StringBuffer stemMk = new StringBuffer(mwParts[1]);
			
			final List<CommonTree> mpfxs = mpfx(tree, pfxPos);
			final List<CommonTree> mks = mk(tree, stemMk);
			
			final CommonTree pos = pos(tree, pfxPos);
			final CommonTree stem = stem(tree, stemMk);	
			
			for(CommonTree mpfxTree:mpfxs) {
				mpfxTree.setParent(retVal);
				retVal.addChild(mpfxTree);
			}
			
			pos.setParent(retVal);
			retVal.addChild(pos);
			
			stem.setParent(retVal);
			retVal.addChild(stem);
			
			for(CommonTree mkTree:mks) {
				mkTree.setParent(retVal);
				retVal.addChild(mkTree);
			}
		}
		
		return retVal;
	}
	
	/**
	 * Buffer should be just the 'pos' portion.  i.e., 'POS_C(:POS_S)*'
	 * @param tree
	 * @param buffer
	 * @return
	 */
	private CommonTree pos(CommonTree tree, StringBuffer buffer) {
		final CommonTree retVal = AntlrUtils.createToken(talkbankTokens, "POS_START");
		
		final String possection = buffer.toString();
		final String[] posvals = possection.split(":");
		final CommonTree posCTree = AntlrUtils.createToken(talkbankTokens, "C_START");
		AntlrUtils.addTextNode(posCTree, talkbankTokens, posvals[0]);
		posCTree.setParent(retVal);
		retVal.addChild(posCTree);
		
		for(int i = 1; i < posvals.length; i++) {
			final CommonTree posSTree = AntlrUtils.createToken(talkbankTokens, "S_START");
			AntlrUtils.addTextNode(posSTree, talkbankTokens, posvals[i]);
			posSTree.setParent(retVal);
			retVal.addChild(posSTree);
		}
			
		return retVal;
	}
	
	private CommonTree stem(CommonTree tree, StringBuffer buffer) {
		final CommonTree retVal = AntlrUtils.createToken(talkbankTokens, "STEM_START");
		AntlrUtils.addTextNode(retVal, talkbankTokens, buffer.toString());
		buffer.setLength(0);
		return retVal;
	}
	
	private List<CommonTree> mk(CommonTree tree, StringBuffer buffer) {
		final List<CommonTree> retVal = new ArrayList<CommonTree>();
		final String fullString = buffer.toString();
		final String regex = "[-&:]";
		final String parts[] = fullString.split(regex);
		if(parts.length > 1) {
			
			final Pattern pattern = Pattern.compile(regex);
			final Matcher m = pattern.matcher(fullString);
			
			final int mkLocs[] = new int[parts.length];
			int idx = 1;
			while(m.find()) {
				mkLocs[idx++] = m.start();
			}
			
			for(int i = 1; i < parts.length; i++) {
				final String mkVal = parts[i];
				
				final char mkchar = fullString.charAt(mkLocs[i]);
				String mkType = "";
				if(mkchar == '-') {
					mkType = "sfx";
				} else if(mkchar == '&') {
					mkType = "sfxf";
				} else if(mkchar == ':') {
					mkType = "mc";
				}
				
				final CommonTree mkTree = 
						AntlrUtils.createToken(talkbankTokens, "MK_START");
				final CommonTree mkTypeTree = 
						AntlrUtils.createToken(talkbankTokens, "MK_ATTR_TYPE");
				mkTypeTree.getToken().setText(mkType);
				mkTypeTree.setParent(mkTree);
				mkTree.addChild(mkTypeTree);
				AntlrUtils.addTextNode(mkTree, talkbankTokens, mkVal);
				
				retVal.add(mkTree);
			}
			
			buffer.delete(parts[0].length(), buffer.length());
		}
		return retVal;
	}
	
	private List<CommonTree> mpfx(CommonTree tree, StringBuffer buffer) {
		final List<CommonTree> retVal = new ArrayList<CommonTree>();
		final int mpfxEnd = buffer.lastIndexOf("#");
		if(mpfxEnd >= 0) {
			final String mpfxsection = buffer.substring(0, mpfxEnd);
			final String mpfxvals[] = mpfxsection.split("#");
			for(String mpfxval:mpfxvals) {
				final CommonTree mpfxTree = 
						AntlrUtils.createToken(talkbankTokens, "MPFX_START");
				AntlrUtils.addTextNode(mpfxTree, talkbankTokens, mpfxval);
				retVal.add(mpfxTree);
			}
			buffer.delete(0, mpfxEnd+1);
		}
		return retVal;
	}
	
	private CommonTree mt(CommonTree tree, StringBuffer buffer) {
		final CommonTree retVal = 
				AntlrUtils.createToken(talkbankTokens, "MT_START");
		String type = "mt"; // default
		switch(buffer.charAt(0)) {
		case '!':
			type = "e";
			break;
			
		case '?':
			type = "q";
			break;
			
		case '.':
			type = "p";
			break;
			
		default:
			type = "mt";
			break;
		}
		final CommonTree mtType =
				AntlrUtils.createToken(talkbankTokens, "MT_ATTR_TYPE");
		
		final String txt = buffer.toString();
		final String typeRegex = "\\(mt:(.+)\\)";
		final Pattern pattern = Pattern.compile(typeRegex);
		final Matcher matcher = pattern.matcher(txt);
		if(type.equals("mt")) {
			type = (matcher.matches() ? matcher.group(1) : txt);
		}
		
		mtType.getToken().setText(type);
		mtType.setParent(retVal);
		retVal.addChild(mtType);
		
		return retVal;
	}
}
