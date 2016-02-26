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

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.tree.CommonTree;

import ca.phon.ipa.IPATranscript;
import ca.phon.ipa.alignment.PhoneMap;
import ca.phon.orthography.Orthography;
import ca.phon.session.AgeFormatter;
import ca.phon.session.Comment;
import ca.phon.session.CommentEnum;
import ca.phon.session.DateFormatter;
import ca.phon.session.Group;
import ca.phon.session.MediaSegment;
import ca.phon.session.Participant;
import ca.phon.session.Record;
import ca.phon.session.Session;
import ca.phon.session.Sex;
import ca.phon.session.Tier;
import ca.phon.session.TierDescription;

/**
 * This class is responsible for turning Phon sessions
 * into chat ANTLR trees.
 *
 */
public class Phon2XmlTreeBuilder {
	
	private final static Logger LOGGER = Logger.getLogger(Phon2XmlTreeBuilder.class.getName());
	
	private static final AntlrTokens chatTokens = new AntlrTokens("Chat.tokens");
	
	private Pattern langPattern = 
		Pattern.compile("([a-zA-Z]{3}(-[a-zA-Z0-9]{1,8})*\\p{Space}?)+");
	
	private int recordIdx = 0;
	
	private Map<String, String> tierNameMap = new HashMap<String, String>();
	
	private final static String chatTierNames[] = {
		"addressee",
		"actions",
		"situation",
		"intonation",
		"explanation",
		"alternative",
		"coding",
		"cohesion",
		"english translation",
		"errcoding",
		"flows",
		"target gloss",
		"gesture",
		"language",
		"ort",
		"paralinguistics",
		"SALT",
		"speech act",
		"time stamp",
		"translation",
		"extension",
		"GRASP",
		"Morphology",
		"uttlan"
	};
	
	/**
	 * Construct the ANTLR Chat tree from the given Phon ITranscript.
	 * 
	 * @param t
	 * @return
	 * @throws TreeBuilderException
	 */
	public CommonTree buildTree(Session session) 
		throws TreeBuilderException {
		CommonTree retVal = AntlrUtils.createToken(chatTokens, "CHAT_START");
		retVal.setParent(null);
		
		recordIdx = 0;
		
		// create tier name map
		// user-defined tiers are only allowed 6 chars
		for(TierDescription tierDesc:session.getUserTiers()) {
			setupTierNameMap(tierDesc);
		}
		
		setupHeaderData(retVal, session);
		setupParticipants(retVal, session);
		insertDate(retVal, session);
		processTranscript(retVal, session);
		
		return retVal;
	}
	
	private void setupTierNameMap(TierDescription tierDesc) {
		String tierName = tierDesc.getName();
		
		// use given name if tier is one of the default chat tiers
		boolean isChatTier = false;
		for(String chatTier:chatTierNames)
			if(tierName.equalsIgnoreCase(chatTier))
				isChatTier = true;
		
		// if less than six chars, use the tier name
		if(isChatTier || tierName.length() <= 6) {
			tierNameMap.put(tierName, tierName.toLowerCase());
		} else {
			String[] tierWords = tierName.split("\\p{Space}");
			
			String tierMapName = "";
			if(tierWords.length == 1) {
				tierMapName = tierWords[0].substring(0, 6); // we know we enough data from the if
			} else if(tierWords.length == 2) {
				// take the start of each word
				tierMapName += (tierWords[0].length() >= 3 ? tierWords[0].substring(0, 3) : tierWords[0]);
				tierMapName += (tierWords[1].length() >= 3 ? tierWords[1].substring(0, 3) : tierWords[1]);
			} else if(tierWords.length > 2) {
				tierMapName += (tierWords[0].length() >= 2 ? tierWords[0].substring(0, 2) : tierWords[0]);
				tierMapName += (tierWords[1].length() >= 2 ? tierWords[1].substring(0, 2) : tierWords[1]);
				tierMapName += (tierWords[2].length() >= 2 ? tierWords[2].substring(0, 2) : tierWords[2]);
			}
			tierMapName = tierMapName.toLowerCase();
			
			int idx = 0;
			while(tierNameMap.containsValue(tierMapName)) {
				tierMapName = tierMapName.substring(0, 5) + (++idx);
			}
			
			tierNameMap.put(tierName, tierMapName);
			
			LOGGER.warning("Using name '" + tierMapName + "' for tier '" + tierName + "'");
		}
	}
	
	/**
	 * Insert the date comment into the xml
	 */
	private void insertDate(CommonTree tree, Session t) {
		// date needs to be in CHAT format: dd-MMM-yyyy e.g., 1-Jul-1865
		// we need to use the US locale to get the correct format
		DateTimeFormatter formatter = 
				DateTimeFormatter.ofPattern("dd-MMM-yyyy");
		LocalDate tDate = t.getDate();
		
		String chatDateStr = formatter.format(tDate).toUpperCase();
		
		CommonTree commentTree = AntlrUtils.createToken(chatTokens, "COMMENT_START");
		commentTree.setParent(tree);
		tree.addChild(commentTree);

		CommonTree attrTree = AntlrUtils.createToken(chatTokens, "COMMENT_ATTR_TYPE");
		attrTree.getToken().setText("Date");
		attrTree.setParent(commentTree);
		commentTree.addChild(attrTree);

		addTextNode(commentTree, chatDateStr);
	}
	
	/**
	 * Setup transcript header data.
	 */
	private void setupHeaderData(CommonTree tree, Session t) {
		// media
		if(t.getMediaLocation() != null
				&& t.getMediaLocation().length() > 0) {
			CommonTree node = AntlrUtils.createToken(chatTokens, "CHAT_ATTR_MEDIA");
			
			File mediaFile = new File(t.getMediaLocation());
			String mediaName = mediaFile.getName();
			
			int extIdx = mediaName.lastIndexOf('.');
			String fname = 
				(extIdx >= 0 ? mediaName.substring(0, extIdx) : mediaName);
//			String fname = t.getMediaLocation().substring(0,
//					t.getMediaLocation().lastIndexOf('.'));
			String fext = 
				(extIdx >= 0 ? mediaName.substring(extIdx+1) : "");
			
			int slashLocation = fname.lastIndexOf('\\');
			if(slashLocation >= 0) {
				fname = fname.substring(slashLocation+1);
			}
			
			node.getToken().setText(fname);
			node.setParent(tree);
			tree.addChild(node);
			
			CommonTree node2 = AntlrUtils.createToken(chatTokens, "CHAT_ATTR_MEDIATYPES");
			if(fext.equals("aif") || fext.equals("aiff") ||
					fext.equals("wav")) {
				node2.getToken().setText("audio");
			} else {
				node2.getToken().setText("video");
			}
			node2.setParent(tree);
			tree.addChild(node2);
		}
		
		// language
		if(t.getLanguage() != null) {
			String lang = t.getLanguage();
			
			boolean langGood = true;
			
			String[] splitLangs = lang.split(",");
			for(String sl:splitLangs) {
				Matcher m = langPattern.matcher(sl.trim());
				langGood &= m.matches();
			}
//			Matcher m = langPattern.matcher(lang);
			if(!langGood) {
				LOGGER.warning("Language '" + lang + "' does not match pattern [a-zA-Z]{3}(-[a-zA-Z0-9]{1,8})*");
				LOGGER.warning("Setting language to 'xxx'");
				lang = "xxx";
			}
			
			CommonTree node = AntlrUtils.createToken(chatTokens, "CHAT_ATTR_LANG");
			node.setParent(tree);
			node.getToken().setText(lang.replaceAll(",", " "));
			tree.addChild(node);
			
		}
		
		// corpus
		CommonTree cNode = AntlrUtils.createToken(chatTokens, "CHAT_ATTR_CORPUS");
		cNode.getToken().setText(t.getCorpus());
		cNode.setParent(tree);
		tree.addChild(cNode);
		
		/**
		 * @Deprecated CHAT ID attribute
		 */
		/*
		{
			CommonTree idNode = AntlrUtils.createToken(chatTokens, "CHAT_ATTR_ID");
			idNode.getToken().setText(t.getName());
			idNode.setParent(tree);
			tree.addChild(idNode);
		}
		*/
		
		// date
		String dString = DateFormatter.dateTimeToString(t.getDate());
		CommonTree dateNode = AntlrUtils.createToken(chatTokens, "CHAT_ATTR_DATE");
		dateNode.getToken().setText(dString);
		dateNode.setParent(tree);
		tree.addChild(dateNode);
		
		// version
		CommonTree vNode = AntlrUtils.createToken(chatTokens, "CHAT_ATTR_VERSION");
		vNode.getToken().setText("2.1.0");
		vNode.setParent(tree);
		tree.addChild(vNode);
	}
	
	/**
	 * Participants
	 */
	private void setupParticipants(CommonTree tree, Session t) {
		// parent participantS node
		CommonTree parent = 
			AntlrUtils.createToken(chatTokens, "PARTICIPANTS_START");
		parent.setParent(tree);
		tree.addChild(parent);
		
		for(Participant p:t.getParticipants()) {
			CommonTree pNode =
				AntlrUtils.createToken(chatTokens, "PARTICIPANT_START");
			parent.addChild(pNode);
			pNode.setParent(parent);
			
			String partId = p.getId();
			// change p0 to CHI
//			if(partId.equalsIgnoreCase("p0")) {
//				partId = "CHI";
//			}
			CommonTree pId = 
				AntlrUtils.createToken(chatTokens, "PARTICIPANT_ATTR_ID");
			pId.getToken().setText(partId);
			pId.setParent(pNode);
			pNode.addChild(pId);
			
			if(p.getName() != null && p.getName().length() > 0) {
				CommonTree pName = 
					AntlrUtils.createToken(chatTokens, "PARTICIPANT_ATTR_NAME");
				pName.getToken().setText(p.getName().replaceAll("\\p{Space}", "_"));
				pName.setParent(pNode);
				pNode.addChild(pName);
			}
			
			String role = "Target_Child";
			if(p.getRole() != null) {
				role = p.getRole().getTitle().replaceAll("\\p{Space}", "_");
			}
				CommonTree pRole = 
					AntlrUtils.createToken(chatTokens, "PARTICIPANT_ATTR_ROLE");
				pRole.getToken().setText(role);
				pRole.setParent(pNode);
				pNode.addChild(pRole);
			
			
			if(p.getLanguage() != null && p.getLanguage().length() > 0) {
				String langs[] = p.getLanguage().split(",");
				String langString = "";
				for(String lang:langs) langString += (langString.length() > 0 ? " " : "") + lang.trim();
				Matcher m = langPattern.matcher(langString);
				if(!m.matches()) {
					LOGGER.warning("Participant " + partId + " language '" + langString + "' does not match pattern " + langPattern.pattern());
				} else {
					CommonTree pLang = 
						AntlrUtils.createToken(chatTokens, "PARTICIPANT_ATTR_LANGUAGE");
					pLang.getToken().setText(langString);
					pLang.setParent(pNode);
					pNode.addChild(pLang);
				}
			}
			
			Period age = p.getAge(t.getDate());
			if(age != null && !age.isNegative() && !age.isZero()) {
				CommonTree pAge = 
					AntlrUtils.createToken(chatTokens, "PARTICIPANT_ATTR_AGE");
				
				pAge.getToken().setText(String.format("P%dY%dM%dD", age.getYears(), age.getMonths(), age.getDays()));
				pAge.setParent(pNode);
				pNode.addChild(pAge);
			}
			
			if(p.getBirthDate() != null) {
				DateFormatter dateFormat = new DateFormatter();
				CommonTree pBday = 
					AntlrUtils.createToken(chatTokens, "PARTICIPANT_ATTR_BIRTHDAY");
				pBday.getToken().setText(dateFormat.format(p.getBirthDate()));
				pBday.setParent(pNode);
				pNode.addChild(pBday);
			}
			
			if(p.getGroup() != null && p.getGroup().length() > 0) {
				CommonTree pGroup =
					AntlrUtils.createToken(chatTokens, "PARTICIPANT_ATTR_GROUP");
				pGroup.getToken().setText(p.getGroup());
				pGroup.setParent(pNode);
				pNode.addChild(pGroup);
			}
			
			if(p.getSex() != null) {
				CommonTree pSex =
					AntlrUtils.createToken(chatTokens, "PARTICIPANT_ATTR_SEX");
				String sexType = 
						(p.getSex() == Sex.MALE ? "male" : "female");
				pSex.getToken().setText(sexType);
				pSex.setParent(pNode);
				pNode.addChild(pSex);
			}
			
			if(p.getSES() != null && p.getSES().length() > 0) {
				CommonTree pSes = 
					AntlrUtils.createToken(chatTokens, "PARTICIPANT_ATTR_SES");
				pSes.getToken().setText(p.getSES());
				pSes.setParent(pNode);
				pNode.addChild(pSes);
			}
		}
	}
	
	/**
	 * Handle transcript data
	 */
	private void processTranscript(CommonTree tree, Session t) 
		throws TreeBuilderException {
		Stack<CommonTree> treeStack = new Stack<>();
		treeStack.push(tree);
		// go through the transcript contents
		for(Record record:t.getRecords()) {
			CommonTree parent = treeStack.peek();
			List<Comment> endComments = new ArrayList<>();
			for(int cIdx = 0; cIdx < record.getNumberOfComments(); cIdx++) {
				// check for lazy-gem
				Comment comment = record.getComment(cIdx);
				if(comment.getType() == CommentEnum.LazyGem) {
					insertLazyGem(parent, comment);
				} else if(comment.getType() == CommentEnum.BeginGem) { 
					insertBeginGem(parent, comment);
				} else if(comment.getType() == CommentEnum.EndGem) {
					endComments.add(comment);
				} else if(comment.getType() == CommentEnum.BeginTcu) { 
					CommonTree newParent = createTcu(parent, comment);
					treeStack.push(newParent);
					parent = newParent;
				} else if(comment.getType() == CommentEnum.EndTcu) {
					endComments.add(comment);
				} else if(comment.getType() == CommentEnum.Date) {
					// date is inserted before processing session
				} else if(comment.getType() == CommentEnum.Code && comment.getValue().startsWith("pid ")) {
					insertPid(parent, comment.getValue());
				} else {					
					insertComment(parent, comment);
				}
			}
			
			try {
				insertRecord(parent, record);
			} catch (TreeBuilderException e) {
				throw new TreeBuilderException(
						"Record #" + (recordIdx+1) + " " + e.getMessage());
			}
			
			for(Comment comment:endComments) {
				if(comment.getType() == CommentEnum.EndGem) {
					insertEndGem(parent, comment);
				} else if(comment.getType() == CommentEnum.EndTcu) {
					treeStack.pop();
				}
			}
			recordIdx++;
		}
	}
	
	/**
	 * Insert a comment
	 * @param tree
	 * @param c
	 */
	private void insertComment(CommonTree tree, Comment c) {
		CommonTree cNode = 
			AntlrUtils.createToken(chatTokens, "COMMENT_START");
		cNode.setParent(tree);
		tree.addChild(cNode);
		
		CommonTree typeNode =
			AntlrUtils.createToken(chatTokens, "COMMENT_ATTR_TYPE");
		typeNode.setParent(cNode);
		typeNode.getToken().setText(c.getType().getTitle());
		cNode.addChild(typeNode);
		
		CommonTree textNode = 
			AntlrUtils.createToken(chatTokens, "TEXT");
		textNode.getToken().setText(c.getValue());
		textNode.setParent(cNode);
		cNode.addChild(textNode);
		
		tree.addChild(cNode);
	}
	
	/**
	 * Insert pid.  This is currently added as a comment to Phon
	 * sessions.
	 * 
	 * @param tree
	 * @param pid
	 */
	private void insertPid(CommonTree parent, String pid) {
		CommonTree pidAttrNode = 
				AntlrUtils.createToken(chatTokens, "CHAT_ATTR_PID");
		pidAttrNode.getToken().setText(pid.substring(4));
		pidAttrNode.setParent(parent);
		parent.insertChild(0, pidAttrNode);
//		parent.addChild(pidAttrNode);
	}
	
	/**
	 * Insert lazy gem
	 */
	private void insertLazyGem(CommonTree parent, Comment c) {
		CommonTree lazyGemNode = 
			AntlrUtils.createToken(chatTokens, "LAZY_GEM_START");
		lazyGemNode.setParent(parent);
		parent.addChild(lazyGemNode);
		
		CommonTree lazyGemLabel = 
			AntlrUtils.createToken(chatTokens, "LAZY_GEM_ATTR_LABEL");
		lazyGemLabel.getToken().setText(c.getValue());
		lazyGemLabel.setParent(lazyGemNode);
		lazyGemNode.addChild(lazyGemLabel);
	}
	
	/**
	 * Insert begin gem
	 */
	private void insertBeginGem(CommonTree parent, Comment c) {
		CommonTree beginGemNode = 
			AntlrUtils.createToken(chatTokens, "BEGIN_GEM_START");
		beginGemNode.setParent(parent);
		parent.addChild(beginGemNode);
		
		CommonTree beginGemLabel = 
			AntlrUtils.createToken(chatTokens, "BEGIN_GEM_ATTR_LABEL");
		beginGemLabel.getToken().setText(c.getValue());
		beginGemLabel.setParent(beginGemNode);
		beginGemNode.addChild(beginGemLabel);
	}
	
	/**
	 * Insert end gem
	 */
	private void insertEndGem(CommonTree parent, Comment c) {
		CommonTree endGemNode = 
			AntlrUtils.createToken(chatTokens, "END_GEM_START");
		endGemNode.setParent(parent);
		parent.addChild(endGemNode);
		
		CommonTree endGemLabel = 
			AntlrUtils.createToken(chatTokens, "END_GEM_ATTR_LABEL");
		endGemLabel.getToken().setText(c.getValue());
		endGemLabel.setParent(endGemNode);
		endGemNode.addChild(endGemLabel);
	}
	
	/**
	 * Insert begin tcu
	 */
	private CommonTree createTcu(CommonTree parent, Comment c) {
		CommonTree beginTcuNode = 
			AntlrUtils.createToken(chatTokens, "TCU_START");
		beginTcuNode.setParent(parent);
		parent.addChild(beginTcuNode);
		
		return beginTcuNode;
	}
	
	/**
	 * Insert a record.
	 */
	private void insertRecord(CommonTree tree, Record utt) 
		throws TreeBuilderException {
		CommonTree uNode = 
			AntlrUtils.createToken(chatTokens, "U_START");
		uNode.setParent(tree);
		
		CommonTree whoNode = 
			AntlrUtils.createToken(chatTokens, "U_ATTR_WHO");
		
		if(utt.getSpeaker() == null) {
			whoNode.getToken().setText("CHI");
		} else
			whoNode.getToken().setText(utt.getSpeaker().getId());

		whoNode.setParent(uNode);
		uNode.addChild(whoNode);
		
		// terminator type
		String termType = "p";
		
		Stack<CommonTree> uttNodeStack = new Stack<CommonTree>();
		uttNodeStack.push(uNode);
		
		final OrthographyTreeBuilder orthoBuilder = new OrthographyTreeBuilder();
		
		for(int gIdx = 0; gIdx < utt.numberOfGroups(); gIdx++) {
			final Group group = utt.getGroup(gIdx);
			
			CommonTree uttNode = uttNodeStack.peek();
			
			// we only add <pg> elements when
			// there is phonetic data to add
			IPATranscript tRep = group.getIPATarget();
			boolean hasTarget = (tRep.length() > 0);
			IPATranscript aRep = group.getIPAActual();
			boolean hasActual = (aRep.length() > 0);

			Stack<CommonTree> nodeStack = new Stack<CommonTree>();
			if(uttNode != uNode) {
				nodeStack.push(uNode);
			}
			nodeStack.push(uttNode);
			if(hasTarget || hasActual) {
				CommonTree grpNode =
					AntlrUtils.createToken(chatTokens, "PG_START");
				grpNode.setParent(uttNode);
				uttNode.addChild(grpNode);
				nodeStack.push(grpNode);
			}
			
			Orthography ortho = group.getOrthography();
			orthoBuilder.buildTree(uttNodeStack, nodeStack.peek(), ortho);
			
			if(gIdx == utt.numberOfGroups() - 1) {
				if(orthoBuilder.getTerminator() == null) {
					orthoBuilder.addTerminator(uNode, "p");
				}
			}
			
			if(hasTarget || hasActual) {
				CommonTree grpNode = nodeStack.peek();

				// model form
				if(hasTarget) {
					CommonTree targetNode =
							AntlrUtils.createToken(chatTokens, "MODEL_START");
					targetNode.setParent(grpNode);
					grpNode.addChild(targetNode);

					// syllabification
					addSyllabification(targetNode, tRep);
				}

				if(hasActual) {
					CommonTree actualNode =
							AntlrUtils.createToken(chatTokens, "ACTUAL_START");
					actualNode.setParent(grpNode);
					grpNode.addChild(actualNode);

					// syllabification
					addSyllabification(actualNode, aRep);
				}

				// alignment only makes sense when we have model+actual
				if(hasActual && hasTarget && group.getPhoneAlignment() != null) {
					addAlignment(grpNode, group.getPhoneAlignment());
				}
			}
			
		}
		
		if(AntlrUtils.findChildrenWithType(uNode, chatTokens.getTokenType("T_START")) == null) {
			// add the terminator
			addTerminator(uNode, termType);
		}
		
		// check if the utterance has a 'postcode' dependent tier
		Tier<String> postcodeTier = 
			utt.getTier("Postcode", String.class);
		if(postcodeTier != null) {
			addPostcode(uNode, postcodeTier);
		}

		// utterance language tier
		Tier<String> uttLangTier =
				utt.getTier("uttlan", String.class);
		if(uttLangTier != null) {
			CommonTree langAttrTree = AntlrUtils.createToken(chatTokens, "U_ATTR_LANG");
			langAttrTree.setParent(uNode);
			uNode.insertChild(0, langAttrTree);
			langAttrTree.getToken().setText(uttLangTier.getGroup(0));
		}
		
		// add media
		if(utt.getSegment() != null && utt.getSegment().getGroup(0) != null) {
			addMedia(uNode, utt.getSegment().getGroup(0));
		}

		// process dependent tiers
		// mor and grasp first
		if(utt.getExtraTierNames().contains("Morphology")) {
			try {
				processMorphology(utt, uNode);
				
				if(utt.getExtraTierNames().contains("GRASP")) {
					processGRASP(utt, uNode);
				}
			} catch (IllegalArgumentException e) {
				final TreeBuilderException treeBuilderException = new TreeBuilderException(e);
				treeBuilderException.setUtt(utt);
				throw treeBuilderException;
			}
		}
		List<String> handeledTiers = new ArrayList<String>();
		// add mor tiers to already handeled list
		handeledTiers.add("Morphology");
		handeledTiers.add("trn");
		handeledTiers.add("GRASP");
		handeledTiers.add("Postcode");
		handeledTiers.add("uttlan");
		handeledTiers.add("Markers");
		handeledTiers.add("Errors");
		handeledTiers.add("Repetition");
		for(String depTierName:utt.getExtraTierNames()) {
			Tier<String> depTier = utt.getTier(depTierName, String.class);
			
			if(handeledTiers.contains(depTierName)) continue;
			handeledTiers.add(depTierName);
			
			if(depTier.isGrouped()) {
				boolean hasData = 
						(depTier.toString().replaceAll("\\[", "").replaceAll("\\]", "").trim().length() > 0);
				
				if(hasData) {
					CommonTree depTierNode =
							AntlrUtils.createToken(chatTokens, "A_START");
					depTierNode.setParent(uNode);
					
					CommonTree typeNode =
						AntlrUtils.createToken(chatTokens, "A_ATTR_TYPE");
					typeNode.getToken().setText("extension");
					
					typeNode.setParent(depTierNode);
					depTierNode.addChild(typeNode);
					
					CommonTree flavorNode = 
						AntlrUtils.createToken(chatTokens, "A_ATTR_FLAVOR");
					String tName = tierNameMap.get(depTierName);
					flavorNode.getToken().setText(tName);
					flavorNode.setParent(depTierNode);
					depTierNode.addChild(flavorNode);
					
					uNode.addChild(depTierNode);
					
					addTextNode(depTierNode, depTier.toString());
				}
			} else {
				String tierVal = depTier.getGroup(0).trim();
				if(tierVal.length() == 0) continue;
			
				CommonTree depTierNode =
					AntlrUtils.createToken(chatTokens, "A_START");
				depTierNode.setParent(uNode);
				
				CommonTree typeNode =
					AntlrUtils.createToken(chatTokens, "A_ATTR_TYPE");
				
				if(depTierName.equalsIgnoreCase("addressee")
					|| depTierName.equalsIgnoreCase("actions")
					|| depTierName.equalsIgnoreCase("situation")
					|| depTierName.equalsIgnoreCase("intonation")
					|| depTierName.equalsIgnoreCase("explanation")
					|| depTierName.equalsIgnoreCase("alternative")
					|| depTierName.equalsIgnoreCase("coding")
					|| depTierName.equalsIgnoreCase("cohesion")
					|| depTierName.equalsIgnoreCase("english translation")
					|| depTierName.equalsIgnoreCase("errcoding")
					|| depTierName.equalsIgnoreCase("flows")
					|| depTierName.equalsIgnoreCase("target gloss")
					|| depTierName.equalsIgnoreCase("gesture")
					|| depTierName.equalsIgnoreCase("language")
					|| depTierName.equalsIgnoreCase("paralinguistics")
					|| depTierName.equalsIgnoreCase("SALT")
					|| depTierName.equalsIgnoreCase("speech act")
					|| depTierName.equalsIgnoreCase("time stamp")
				) {
					typeNode.getToken().setText(depTierName.toLowerCase());
				} else if(depTierName.equalsIgnoreCase("translation")) {
					typeNode.getToken().setText("english translation");
				} else {
					typeNode.getToken().setText("extension");
	//				continue;
				}
				uNode.addChild(depTierNode);
				
				typeNode.setParent(depTierNode);
				depTierNode.addChild(typeNode);
				
				if(typeNode.getToken().getText().equalsIgnoreCase("extension")) {
					CommonTree flavorNode = 
						AntlrUtils.createToken(chatTokens, "A_ATTR_FLAVOR");
					String tName = tierNameMap.get(depTierName);
					flavorNode.getToken().setText(tName);
					flavorNode.setParent(depTierNode);
					depTierNode.addChild(flavorNode);
				}
				
				addTextNode(depTierNode, tierVal);
			}
		}
		
		// add Markers, Error and Repetition tiers (if available)
		// make sure our tiers match expected syntax (i.e., all markers are in parenthesis)
		final String regex = "\\([^)]*\\)(\\p{Space}?(\\([^)]*\\)))*";
		final Pattern pattern = Pattern.compile(regex);
		final String subRegex = "(\\([^)]*\\))";
		
		final TalkBankCodeTreeBuilder chatCodeBuilder = new TalkBankCodeTreeBuilder();
		
		Tier<String> markersTier = utt.getTier("Markers", String.class);
		if(markersTier != null) {
			final String tierVal = markersTier.getGroup(0);
			final Matcher matcher = pattern.matcher(tierVal);
			if(!matcher.matches()) {
				LOGGER.severe("Tier 'Markers' is has incorrect syntax: " + tierVal);
			} else {
				final Pattern subPattern = Pattern.compile(subRegex);
				final Matcher subMatcher = subPattern.matcher(tierVal);
				
				while(subMatcher.find()) {
					final String markerVal = subMatcher.group(1);
					chatCodeBuilder.handleParentheticData(uNode, markerVal);
				}
			}
		}
		
		Tier<String> errorsTier = utt.getTier("Errors", String.class);
		if(errorsTier != null) {
			final String tierVal = errorsTier.getGroup(0);
			final Matcher matcher = pattern.matcher(tierVal);
			if(!matcher.matches()) {
				LOGGER.severe("Tier 'Errors' is has incorrect syntax: " + tierVal);
			} else {
				final Pattern subPattern = Pattern.compile(subRegex);
				final Matcher subMatcher = subPattern.matcher(tierVal);
				
				while(subMatcher.find()) {
					final String errorVal = subMatcher.group(1);
					chatCodeBuilder.handleParentheticData(uNode, errorVal);
				}
			}
		}
		
		Tier<String> repTier = utt.getTier("Repetition", String.class);
		if(repTier != null) {
			final String tierVal = repTier.getGroup(0);
			final Matcher matcher = pattern.matcher(tierVal);
			if(!matcher.matches()) {
				LOGGER.severe("Tier 'Repetition' is has incorrect syntax: " + tierVal);
			} else {
				final Pattern subPattern = Pattern.compile(subRegex);
				final Matcher subMatcher = subPattern.matcher(tierVal);
				
				while(subMatcher.find()) {
					final String repVal = subMatcher.group(1);
					chatCodeBuilder.handleParentheticData(uNode, repVal);
				}
			}
		}
		
		// notes
		if(utt.getNotes() != null && utt.getNotes().getGroup(0).length() > 0) {
			CommonTree notesNode =
				AntlrUtils.createToken(chatTokens, "A_START");
			notesNode.setParent(uNode);
			uNode.addChild(notesNode);
			
			CommonTree typeNode = 
				AntlrUtils.createToken(chatTokens, "A_ATTR_TYPE");
			typeNode.getToken().setText("comments");
			typeNode.setParent(notesNode);
			notesNode.addChild(typeNode);
			
			addTextNode(notesNode, utt.getNotes().getGroup(0));
		}
		
		tree.addChild(uNode);
	}
	
	/**
	 * Process morphology and grasp tiers and add data to the
	 * given tree.
	 * 
	 * @param utt
	 * @param tree
	 */
	private List<CommonTree> processMorphology(Record utt, CommonTree uNode) 
		throws IllegalArgumentException {
		
		final List<CommonTree> retVal = new ArrayList<CommonTree>();
		
		// final all word sub-trees
		// word trees from utterance
		final List<CommonTree> allWordTrees =
				AntlrUtils.findAllChildrenWithType(uNode, chatTokens, "W_START", "TAGMARKER_START", "T_START");
		final List<CommonTree> wordTrees = new ArrayList<>();

		for(CommonTree wordTree:allWordTrees) {
			if(wordTree.getToken().getType() == chatTokens.getTokenType("W_START")) {
				List<CommonTree> replNodes = 
						AntlrUtils.findAllChildrenWithType(wordTree, chatTokens, "REPLACEMENT_START");
				if(replNodes.size() > 0) {
					// don't add this tree
					continue;
				} else {
					// get text node
					List<CommonTree> textNodes = AntlrUtils.findAllChildrenWithType(wordTree, chatTokens, "TEXT");
					if(textNodes.size() > 0) {
						CommonTree textNode = textNodes.get(0);
						String wordText = textNode.getText();
						
						// exclude xxx, yyy, and pauses
						if(wordText.equals("xxx") 
								|| wordText.equals("yyy")
								|| wordText.matches("\\(\\.+\\)")) continue;
						
						// exclude fragments
						List<CommonTree> typeNodes = AntlrUtils.findAllChildrenWithType(wordTree, chatTokens, "W_ATTR_TYPE");
						if(typeNodes.size() > 0) {
							String typeText = typeNodes.get(0).getText();
							if(typeText.equals("fragment")) continue;
						}
						
						wordTrees.add(wordTree);
					}
				}
			} else {
				wordTrees.add(wordTree);
			}
		}
		
		// get a listing of all mordata elements
		final Tier<String> morTier = utt.getTier("Morphology", String.class);
		final List<String> morWrdVals = new ArrayList<String>();
		for(String grpVal:morTier) {
			// split by space
			final String[] wrdVals = grpVal.split("\\s+");
			
			String cWrd = null;
			for(String wrdVal:wrdVals) {
				if(wrdVal.startsWith("(") && !wrdVal.endsWith(")")) {
					cWrd = wrdVal;
				} else if(cWrd != null) {
					cWrd += " " + wrdVal;
					if(wrdVal.endsWith(")")) {
						morWrdVals.add(cWrd);
					}
				} else {
					morWrdVals.add(wrdVal);
				}
			}
		}
		
		// create a mor tree for each word value and attempt to attach it to
		// the next available word tree
		final MorBuilder mb = new MorBuilder();
		int wordTreeIdx = 0;
		for(String morWrdVal:morWrdVals) {
			final CommonTree mortree = mb.buildMorTree(morWrdVal);
			if(mortree == null) {
				continue;
			}
			final CommonTree morTypeTree = AntlrUtils.createToken(chatTokens, "MOR_ATTR_TYPE");
			morTypeTree.getToken().setText("mor");
			morTypeTree.setParent(mortree);
			mortree.insertChild(0, morTypeTree);
			mortree.freshenParentAndChildIndexes();
			retVal.add(mortree);
			
			final CommonTree wTree = 
					(wordTreeIdx < wordTrees.size() ? wordTrees.get(wordTreeIdx) : null);
			wordTreeIdx++;
			if(wTree == null) {
				throw new IllegalArgumentException("one-to-one alignment error: mor");
			}
			
			final List<CommonTree> morOmittedTrees = AntlrUtils.findChildrenWithType(mortree, chatTokens, "MOR_ATTR_OMITTED");
			final boolean morOmitted = 
					(morOmittedTrees.size() > 0 && Boolean.parseBoolean(morOmittedTrees.get(0).getToken().getText()));
			final List<CommonTree> wTypeTrees = AntlrUtils.findAllChildrenWithType(wTree, chatTokens, "W_ATTR_TYPE");
			final boolean wOmitted = 
					(wTypeTrees.size() > 0 && wTypeTrees.get(0).getToken().getText().equals("omission"));
			
			if(wOmitted ^ morOmitted) {
				throw new IllegalArgumentException("one-to-one alignment error: mor omission mismatch ");
			} else {
				wTree.addChild(mortree);
				mortree.setParent(wTree);
			}
		}
		
		return retVal;
	}
	
	/**
	 * Process the GRASP tier and attach the generated trees
	 * to the given mor trees.
	 * 
	 * @param utt
	 * @param mortrees
	 */
	private void processGRASP(Record utt, CommonTree uNode) 
		throws IllegalArgumentException {
		final MorBuilder mb = new MorBuilder();
		final List<CommonTree> mortrees =  
				AntlrUtils.findAllChildrenWithType(uNode, chatTokens, "MOR_START", "MOR_PRE_START", "MOR_POST_START");
		final Tier<String> graTier = utt.getTier("GRASP", String.class);
		final List<CommonTree> graTrees = new ArrayList<CommonTree>();
		for(String grpTierVal:graTier) {
			final String wrdVals[] = grpTierVal.split("\\s+");
			for(String wrdVal:wrdVals) {
				if(wrdVal.trim().length() == 0) continue;
				final CommonTree graTree = mb.buildGraspTree(wrdVal);
				graTrees.add(graTree);
			}
		}
		
		if(mortrees.size() != graTrees.size()) {
			throw new IllegalArgumentException("one-to-one alignment error: gra");
		}
		
		for(int i = 0; i < mortrees.size(); i++) {
			final CommonTree mortree = mortrees.get(i);
			final CommonTree gratree = graTrees.get(i);
			
			mb.attachGrasp(mortree, gratree);
		}
	}

	/*
	 * align
	 */
	private void addAlignment(CommonTree parent, PhoneMap pm) {
		final AlignTreeBuilder alignTreeBuilder = new AlignTreeBuilder();
		final CommonTree alignNode = alignTreeBuilder.buildAlignmentTree(pm);
		alignNode.setParent(parent);
		parent.addChild(alignNode);
	}

	/*
	 * sb
	 */
	private void addSyllabification(CommonTree parent, IPATranscript phoRep) {
		final PhoTreeBuilder phoTreeBuilder = new PhoTreeBuilder();
		final List<CommonTree> pwTrees = phoTreeBuilder.buildPwTrees(phoRep);
		for(CommonTree pwTree:pwTrees) {
			parent.addChild(pwTree);
			pwTree.setParent(parent);
		}
	}
	
	/**
	 * add a text node
	 */
	private void addTextNode(CommonTree parent, String data) {
		CommonTree txtNode = 
			AntlrUtils.createToken(chatTokens, "TEXT");
		txtNode.getToken().setText(data);
		txtNode.setParent(parent);
		parent.addChild(txtNode);
	}

	/**
	 * Add a postcode element
	 */
	private void addPostcode(CommonTree parent, Tier<String> data) {
		CommonTree pcNode = 
			AntlrUtils.createToken(chatTokens, "POSTCODE_START");
		pcNode.setParent(parent);
		parent.addChild(pcNode);
		
		addTextNode(pcNode, data.getGroup(0));
	}
	
	/**
	 * Add a media element
	 */
	private void addMedia(CommonTree parent, MediaSegment media) {
		// don't add media if len = 0
		float len = media.getEndValue() - media.getStartValue();
		if(len == 0.0f) {
			return;
		}

		CommonTree mediaNode = 
			AntlrUtils.createToken(chatTokens, "MEDIA_START");
		mediaNode.setParent(parent);
		parent.addChild(mediaNode);
		
		// we need to convert our units to s from ms
		float startS = media.getStartValue() / 1000.0f;
		float endS = media.getEndValue() / 1000.0f;
		
		CommonTree unitNode =
			AntlrUtils.createToken(chatTokens, "MEDIA_ATTR_UNIT");
		unitNode.getToken().setText("s");
		unitNode.setParent(mediaNode);
		mediaNode.addChild(unitNode);
		
		CommonTree startNode = 
			AntlrUtils.createToken(chatTokens, "MEDIA_ATTR_START");
		startNode.getToken().setText(""+startS);
		startNode.setParent(mediaNode);
		mediaNode.addChild(startNode);
		
		CommonTree endNode =
			AntlrUtils.createToken(chatTokens, "MEDIA_ATTR_END");
		endNode.getToken().setText(""+endS);
		endNode.setParent(mediaNode);
		mediaNode.addChild(endNode);
	}
	
	/**
	 * Add a terminator
	 */
	private void addTerminator(CommonTree parent, String type) {
		CommonTree tNode =
			AntlrUtils.createToken(chatTokens, "T_START");
		tNode.setParent(parent);
		parent.addChild(tNode);
		
		CommonTree ttNode = 
			AntlrUtils.createToken(chatTokens, "T_ATTR_TYPE");
		ttNode.getToken().setText(type);
		ttNode.setParent(tNode);
		tNode.addChild(ttNode);
	}
	
}
