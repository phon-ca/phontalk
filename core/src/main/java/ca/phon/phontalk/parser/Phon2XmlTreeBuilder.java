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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.tree.CommonTree;

import ca.phon.alignment.PhoneMap;
import ca.phon.application.transcript.CommentEnum;
import ca.phon.application.transcript.Form;
import ca.phon.application.transcript.IComment;
import ca.phon.application.transcript.IDepTierDesc;
import ca.phon.application.transcript.IDependentTier;
import ca.phon.application.transcript.IMedia;
import ca.phon.application.transcript.IParticipant;
import ca.phon.application.transcript.IPhoneticRep;
import ca.phon.application.transcript.ITranscript;
import ca.phon.application.transcript.IUtterance;
import ca.phon.application.transcript.IWord;
import ca.phon.application.transcript.IWordGroup;
import ca.phon.application.transcript.Sex;
import ca.phon.application.transcript.TranscriptElement;
import ca.phon.application.transcript.TranscriptUtils;
import ca.phon.phone.Phone;
import ca.phon.system.logger.PhonLogger;
import ca.phon.util.PhonDateFormat;
import ca.phon.util.PhonDuration;
import ca.phon.util.PhonDurationFormat;
import ca.phon.util.StringUtils;
/**
 * This class is responsible for turning Phon sessions
 * into chat ANTLR trees.
 *
 */
public class Phon2XmlTreeBuilder {
	
	private static final AntlrTokens chatTokens = new AntlrTokens("Chat.tokens");
	
	/*
	 * Word pattern
	 * Words have 3 sections <type>&<data>@<formType> 
	 */
	private Pattern wordPattern = 
		Pattern.compile("([^@]+)(?:@([^@]+))?");
	
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
	public CommonTree buildTree(ITranscript t) 
		throws TreeBuilderException {
		CommonTree retVal = AntlrUtils.createToken(chatTokens, "CHAT_START");
		retVal.setParent(null);
		
		recordIdx = 0;
		
		// create tier name map
		// user-defined tiers are only allowed 6 chars
		for(IDepTierDesc tierDesc:t.getDependentTiers()) {
			setupTierNameMap(tierDesc);
		}
		for(IDepTierDesc tierDesc:t.getWordAlignedTiers()) {
			setupTierNameMap(tierDesc);
		}
		
		setupHeaderData(retVal, t);
		setupParticipants(retVal, t);
		processTranscript(retVal, t);
		
		return retVal;
	}
	
	private void setupTierNameMap(IDepTierDesc tierDesc) {
		String tierName = tierDesc.getTierName();
		
		// use given name if tier is one of the default chat tiers
		boolean isChatTier = false;
		for(String chatTier:chatTierNames)
			if(tierName.equalsIgnoreCase(chatTier))
				isChatTier = true;
		
		// if less than six chars, use the tier name
		if(isChatTier || tierName.length() <= 6) {
//			if(tierName.equals("ort")) {
//				tierNameMap.put(tierName, "orthography");
//			} else {
				tierNameMap.put(tierName, tierName.toLowerCase());
//			}
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
			
			PhonLogger.warning("Using name '" + tierMapName + "' for tier '" + tierName + "'");
		}
	}
	
	/**
	 * Insert the date comment into the xml
	 */
	private void insertDate(CommonTree tree, ITranscript t) {
//		PhonDateFormat pdf = new PhonDateFormat(PhonDateFormat.YEAR_LONG);
//		String dString = pdf.format(t.getDate());
		// date needs to be in CHAT format: dd-MMM-yyyy e.g., 1-Jul-1865
		// we need to use the US locale to get the correct format
		SimpleDateFormat chatFormat =
				new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
		
		Calendar tDate = (Calendar)t.getDate().clone();
		
		// set timezone so that date is correct when printed
		tDate.setTimeZone(TimeZone.getDefault());
		
		String chatDateStr = 
				chatFormat.format(tDate.getTime()).toUpperCase();
		
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
	private void setupHeaderData(CommonTree tree, ITranscript t) {
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
				Matcher m = langPattern.matcher(StringUtils.strip(sl));
				langGood &= m.matches();
			}
//			Matcher m = langPattern.matcher(lang);
			if(!langGood) {
				PhonLogger.warning("Language '" + lang + "' does not match pattern [a-zA-Z]{3}(-[a-zA-Z0-9]{1,8})*");
				PhonLogger.warning("Setting language to 'xxx'");
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
		
		// id
		CommonTree idNode = AntlrUtils.createToken(chatTokens, "CHAT_ATTR_ID");
		idNode.getToken().setText(t.getID());
		idNode.setParent(tree);
		tree.addChild(idNode);
		
		// date
		PhonDateFormat pdf = new PhonDateFormat(PhonDateFormat.YEAR_LONG);
		String dString = pdf.format(t.getDate());
		CommonTree dateNode = AntlrUtils.createToken(chatTokens, "CHAT_ATTR_DATE");
		dateNode.getToken().setText(dString);
		dateNode.setParent(tree);
		tree.addChild(dateNode);
		
		// version
		CommonTree vNode = AntlrUtils.createToken(chatTokens, "CHAT_ATTR_VERSION");
		vNode.getToken().setText("2.0.2");
		vNode.setParent(tree);
		tree.addChild(vNode);
	}
	
	/**
	 * Participants
	 */
	private void setupParticipants(CommonTree tree, ITranscript t) {
		// parent participantS node
		CommonTree parent = 
			AntlrUtils.createToken(chatTokens, "PARTICIPANTS_START");
		parent.setParent(tree);
		
		for(IParticipant p:t.getParticipants()) {
			CommonTree pNode =
				AntlrUtils.createToken(chatTokens, "PARTICIPANT_START");
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
			if(p.getRole() != null && p.getRole().length() > 0) {
				role = p.getRole().replaceAll("\\p{Space}", "_");
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
					PhonLogger.warning("Participant " + partId + " language '" + langString + "' does not match pattern " + langPattern.pattern());
				} else {
					CommonTree pLang = 
						AntlrUtils.createToken(chatTokens, "PARTICIPANT_ATTR_LANGUAGE");
					pLang.getToken().setText(langString);
					pLang.setParent(pNode);
					pNode.addChild(pLang);
				}
			}
			
			PhonDuration age = p.getAge(t.getDate());
			if(!age.isNegative() && !age.isZero()) {
				PhonDurationFormat pdf = new PhonDurationFormat(PhonDurationFormat.XML_FORMAT);
				CommonTree pAge = 
					AntlrUtils.createToken(chatTokens, "PARTICIPANT_ATTR_AGE");
				
				pAge.getToken().setText(pdf.format(age));
				pAge.setParent(pNode);
				pNode.addChild(pAge);
				
				PhonDateFormat dateFormat = new PhonDateFormat(PhonDateFormat.YEAR_LONG);
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
			
			parent.addChild(pNode);
		}
		tree.addChild(parent);
	}
	
	/**
	 * Handle transcript data
	 */
	private void processTranscript(CommonTree tree, ITranscript t) 
		throws TreeBuilderException {
		// go through the transcript contents
		boolean dateCommentInserted = false;
		for(TranscriptElement<Object> tele:t.getTranscriptElements()) {
			if(tele.getValue() instanceof IComment) {
				// check for lazy-gem
				IComment comment = (IComment)tele.getValue();
				if(comment.getType().toString().equalsIgnoreCase("LazyGem")) {
					insertLazyGem(tree, comment);
				} else if(comment.getType() == CommentEnum.Date) {
					insertDate(tree, t);
					dateCommentInserted = true;
				} else if(comment.getType() == CommentEnum.Code && comment.getValue().startsWith("pid ")) {
					insertPid(tree, comment.getValue());
				} else {					
					insertComment(tree, (IComment)tele.getValue());
				}
			} else if(tele.getValue() instanceof IUtterance) {
				if(!dateCommentInserted) {
					insertDate(tree, t);
					dateCommentInserted = true;
				}
				try {
					insertRecord(tree, (IUtterance)tele.getValue());
				} catch (TreeBuilderException e) {
					throw new TreeBuilderException(
							"Record #" + (recordIdx+1) + " " + e.getMessage());
				}
				recordIdx++;
			}
			//break; // debug to just get one record
		}
	}
	
	/**
	 * Insert a comment
	 * @param tree
	 * @param c
	 */
	private void insertComment(CommonTree tree, IComment c) {
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
	private void insertLazyGem(CommonTree parent, IComment c) {
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
	 * Handle data in parenthesis.
	 * 
	 */
	private void handleParentheticData(CommonTree tree, String d) {
		if(!d.startsWith("(") || !d.endsWith(")")) 
			throw new IllegalArgumentException(d);
		
		String data = d.substring(1, d.length()-1);
		
		// attempt to find the utterance parent
		CommonTree uttNode = null;
		int uttTokenType = chatTokens.getTokenType("U_START");
		CommonTree cNode = (CommonTree)tree;
		while(cNode != null) {
			if(cNode.getToken().getType() == uttTokenType) {
				uttNode = cNode;
				break;
			}
			cNode = (CommonTree)cNode.getParent();
		}
		
		// handle special cases
		
		// overlaps
		if(data.matches("<[0-9]*") || data.matches(">[0-9]*")) 
		{
			addOverlap(tree, data);
		} 
		
		// linkers
		else if(data.equals("+\"")
				|| data.equals("+^") 
				|| data.equals("+<")
				|| data.equals("+,")
				|| data.equals("++")
				|| data.equals("+\u224b")
				|| data.equals("+\u2248")) 
		{
			addLinker(uttNode, data);
		}
		
		// pauses
		else if(data.equals(".")
				|| data.equals("..")
				|| data.equals("...")
				|| data.matches("pause:[0-9]+"))
		{
			addPause(tree, data);
		}
		
		// makers
		else if(data.equals("!")
				|| data.equals("!!")
				|| data.equals("?") 
				|| data.equals("/")
				|| data.equals("//")
				|| data.equals("///")
				|| data.equals("/?")
				|| data.equals("/-"))
		{
			addMarker(tree, data);
		}

		// ga
		else if(data.startsWith("%act:")) {
			addGa(tree, "actions", StringUtils.strip(data.substring(5)));
		} else if(data.startsWith("=?")) {
			addGa(tree, "alternative", StringUtils.strip(data.substring(2)));
		} else if(data.startsWith("%")) {
			addGa(tree, "comments", StringUtils.strip(data.substring(1)));
		}  else if(data.startsWith("=!")) {
			addGa(tree, "paralinguistics", StringUtils.strip(data.substring(2)));
		} else if(data.startsWith("=")) {
			addGa(tree, "explanation", StringUtils.strip(data.substring(1)));
		} else if(data.startsWith("%sdi:")) {
			addGa(tree, "standard for dialect", StringUtils.strip(data.substring(5)));
		} else if(data.startsWith("%sch:")) {
			addGa(tree, "standard for child", StringUtils.strip(data.substring(5)));
		} else if(data.startsWith("%xxx:")) {
			addGa(tree, "standard for unclear source", StringUtils.strip(data.substring(5)));
		}
		
		// repeats
		else if(data.matches("x\\p{Space}?[0-9]+")) {
			addRepetition(tree, StringUtils.strip(data.substring(1)));
		}
		
		// everything else
		else {
			int cIndex = data.indexOf(':');
			if(cIndex >= 0) {
				String eleName = data.substring(0, cIndex);
				String eleData = data.substring(cIndex+1);
			
				if(eleName.equals("happening")) {
					addHappening(tree, eleData);
				} else if(eleName.equals("action")) {
					addAction(tree, eleData);
				} else if(eleName.equals("error")) {
					addError(tree, eleData);
				} else if(eleName.equals("internal-media")) {
					addInternalMedia(tree, eleData);
				} else if(eleName.equals("overlap-point")) {
					addOverlapPoint(tree, eleData);
				} else {
					PhonLogger.warning("Unsupported data " + data);
				}
			}
		}
	}
	
	/**
	 * Insert a record.
	 */
	private void insertRecord(CommonTree tree, IUtterance utt) 
		throws TreeBuilderException {
		CommonTree uNode = 
			AntlrUtils.createToken(chatTokens, "U_START");
		uNode.setParent(tree);
		
		CommonTree whoNode = 
			AntlrUtils.createToken(chatTokens, "U_ATTR_WHO");
		
		if(utt.getSpeaker() == null) {
			whoNode.getToken().setText("p0");
		} else
			whoNode.getToken().setText(utt.getSpeaker().getId());

		whoNode.setParent(uNode);
		uNode.addChild(whoNode);
		
		// terminator type
		String termType = "p";
		
		Stack<CommonTree> uttNodeStack = new Stack<CommonTree>();
		uttNodeStack.push(uNode);
		
		for(IWord wObj:utt.getWords()) {
			IWordGroup wGrp = (IWordGroup)wObj;
			
			CommonTree uttNode = uttNodeStack.peek();
			
//			// if we only have a '{' inside the group
//			// create a new 'global' <g> element.
//			// This element will take place of the uttNode
//			// until the closing '}' is found
//			// example utt:  [{] [yyy] [(overlap:>)] [} .]
//			// would translate to: <g><pg>yyy</pg><overlap type=">"/></g>
//			if(wGrp.getWord().equals("{")) {
//				
//			}

			// we only add <pg> elements when
			// there is phonetic data to add
			IPhoneticRep tRep = wGrp.getPhoneticRepresentation(Form.Target);
			boolean hasTarget = (tRep != null && StringUtils.strip(tRep.getTranscription()).length() > 0);
			IPhoneticRep aRep = wGrp.getPhoneticRepresentation(Form.Actual);
			boolean hasActual = (aRep != null && StringUtils.strip(aRep.getTranscription()).length() > 0);

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
			
			// break into words
			List<String> words = wGrp.getWords();
			String wordList[] = words.toArray(new String[0]);
			
			if(wordList.length == 0) {
				// setup 'empty' transcription
				wordList = new String[1];
				wordList[0] = "0";
			}
			
			// a flag to indicate the next item in the iteration
			// should be attached to the last child of nodeStack.peek()
			boolean attachToLastChild = false;
			for(int wIndex = 0; wIndex < wordList.length; wIndex++) {
				String w = wordList[wIndex];
				
				// EVENTS
				if(w.matches("\\*.*\\*")) {
					String eData = w.substring(1, w.length()-1);
					CommonTree parentNode = nodeStack.peek();
					
					insertEvent(parentNode, eData);
				// anything else
				} else if(w.matches("\\(.*\\)")) {
					
					// COMMENTS - including CHAT coding
					Pattern commentPattern = Pattern.compile("\\(([^:]+):(.*)\\)");
					Matcher m = commentPattern.matcher(w);
					if(m.matches()) {
						String type = m.group(1);
						String data = m.group(2);
						
						// some tags need special handling...
						// TERMINATOR
						if (type.equals("t")) { 
							termType = data;
						} else if(type.equals("replacement")) { 

							CommonTree parentNode = nodeStack.peek();
							// find the last 'w' node
							int wTokenType = chatTokens.getTokenType("W_START");
							CommonTree wNode = null;
							for(int cIndex = parentNode.getChildCount()-1; cIndex >= 0; cIndex--) {
								CommonTree cNode = (CommonTree)parentNode.getChild(cIndex);
								if(cNode.getToken().getType() == wTokenType) {
									wNode = cNode;
									break;
								}
							}
							if(wNode == null) {
								// create a new word
								wNode = AntlrUtils.createToken(chatTokens, "W_START");
								wNode.setParent(parentNode);
								parentNode.addChild(wNode);
								attachToLastChild = true;
							}
							addReplacement(wNode, data);
						} else if(type.equals("langs")) {

							CommonTree parentNode = nodeStack.peek();
							// find the last 'w' node
//							int wTokenType = tokens.getTokenType("W_START");
							CommonTree wNode = null;
							/*for(int cIndex = parentNode.getChildCount()-1; cIndex >= 0; cIndex--) {
								CommonTree cNode = (CommonTree)parentNode.getChild(cIndex);
								if(cNode.getToken().getType() == wTokenType) {
									wNode = cNode;
									break;
								}
							}*/
							if(wNode == null) {
								// create a new word
								wNode = AntlrUtils.createToken(chatTokens, "W_START");
								wNode.setParent(parentNode);
								parentNode.addChild(wNode);
								attachToLastChild = true;
							}
							addLangs(wNode, data);
						} else {
							handleParentheticData(nodeStack.peek(), w);
						}
					} else {
						handleParentheticData(nodeStack.peek(), w);
					}
				// handle forced creation of Talkbank <g> elements
				} else if(w.matches("\\{")) {
					if(wGrp.getWord().indexOf('}') < 0) {
						// create a super-<g> node
						CommonTree superG = new CommonTree(new CommonToken(chatTokens.getTokenType("G_START")));
						superG.setParent(uttNode);
						uttNode.addChild(superG);
						uttNode = superG;
						
						uttNodeStack.push(superG);
						// skip this word-group
						continue;
					} else {
						CommonTree parentNode = nodeStack.peek();
						if(attachToLastChild) {
							parentNode = 
								(CommonTree)parentNode.getChild(parentNode.getChildCount()-1);
							attachToLastChild = false;
						}
						CommonTree gNode = 
							AntlrUtils.createToken(chatTokens, "G_START");
						gNode.setParent(parentNode);
						parentNode.addChild(gNode);
						
						// push new group onto stack
						nodeStack.push(gNode);
					}
				// handle end-enclosure for <g> elements
			    } else if(w.matches("\\}")) {
//			    	// if we have a super-<g>
//			    	if(uttNode != realUttNode) {
//			    		// switch back uttNode
//			    		uttNode = realUttNode;
//			    		nodeStack.pop();
//			    		nodeStack.push(uttNode);
//			    	} else 
//				    	// pop group from stack
//				    	nodeStack.pop();
			    	if(wGrp.getWord().indexOf('{') < 0) {
			    		// pop the group from the uttstack
			    		uttNodeStack.pop();
//			    		System.out.println(TranscriptUtils.getTierValue(utt, "Orthography"));
			    		uttNode = uttNodeStack.peek();
			    	}
			    		nodeStack.pop();
			    // Terminators - should be at end but not checked here
			    } else if(w.matches("\\.")) {
			    	termType = "p";
			    } else if(w.matches("\\?")) {
				    termType = "q";
			    } else if(w.matches("!")) { 
			    	termType = "e";
			    // tagMarkers
			    } else if(w.matches("[,\u201e\u2021]")) {	
			    	CommonTree parentNode = nodeStack.peek();
			    	CommonTree tgTree = AntlrUtils.createToken(chatTokens, "TAGMARKER_START");
			    	String type = "comma";
			    	if(w.equals("\u201e")) {
			    		type = "tag";
			    	} else if(w.equals("\u2021")) {
			    		type = "vocative";
			    	}
			    	CommonTree tgTypeTree = AntlrUtils.createToken(chatTokens, "TAGMARKER_ATTR_TYPE");
			    	tgTypeTree.getToken().setText(type);
			    	tgTree.addChild(tgTypeTree);
			    	tgTypeTree.setParent(tgTree);
			    	
			    	parentNode.addChild(tgTree);
			    	tgTree.setParent(parentNode);
			    // compound words
			    } else if(w.matches("[+~]")) { 
			    	CommonTree parentNode = nodeStack.peek();
			    	CommonTree wNode = 
			    		(CommonTree)parentNode.getChild(parentNode.getChildCount()-1);
			    	insertWordnet(wNode, w.equals("+") ? "cmp" : "cli");
			    	attachToLastChild = true;
			    // 'normal' words - with possible prefix/suffix annotations
			    } else {
					CommonTree parentNode = nodeStack.peek();
					CommonTree wParent = null;
					if(attachToLastChild) {
						wParent = 
							(CommonTree)parentNode.getChild(parentNode.getChildCount()-1);
						attachToLastChild = false;
					} else {
						wParent = 
							AntlrUtils.createToken(chatTokens, "W_START");
						wParent.setParent(parentNode);
						parentNode.addChild(wParent);
					}
					
					String addWord = w;
					Matcher m = wordPattern.matcher(w);
					
					if(m.matches()) {
						String wData = m.group(1);
						String wFormType = m.group(2);
	
						addWord = wData;
						
						if(wFormType != null) {
							String fullType = wFormType;
							
							if(wFormType.equals("b")) {
								fullType = "babbling";
							} else if(wFormType.equals("c")) {
								fullType = "child-invented";
							} else if(wFormType.equals("d")) {
								fullType = "dialect";
							} else if(wFormType.equals("f")) {
								fullType = "family-specific";
							} else if(wFormType.equals("fp")) {
								fullType = "filled pause";
							} else if(wFormType.equals("fs")) {
								fullType = "filler syllable";
							} else if(wFormType.equals("g")) {
								fullType = "generic";
							} else if(wFormType.equals("i")) {
								fullType = "interjection";
							} else if(wFormType.equals("k")) {
								fullType = "kana";
							} else if(wFormType.equals("l")) {
								fullType = "letter";
							} else if(wFormType.equals("n")) {
								fullType = "neologism";
							} else if(wFormType.equals("nv")) {
								fullType = "no voice";
							} else if(wFormType.equals("o")) {
								fullType = "onomatopoeia";
							} else if(wFormType.equals("p")) {
								fullType = "phonology consistent";
							} else if(wFormType.equals("pm")) {
								fullType = "proto-morpheme";
							} else if(wFormType.equals("q")) {
								fullType = "quoted metareference";
							} else if(wFormType.equals("sas")) {
								fullType = "sign speech";
							} else if(wFormType.equals("si")) {
								fullType = "singing";
							} else if(wFormType.equals("sl")) {
								fullType = "signed language";
							} else if(wFormType.equals("t")) {
								fullType = "test";
							} else if(wFormType.equals("u")) {
								fullType = "UNIBET";
							} else if(wFormType.equals("x")) {
								fullType = "words to be excluded";
							} else if(wFormType.equals("wp")) {
								fullType = "word play";
							}
							
							CommonTree formTypeNode = 
								AntlrUtils.createToken(chatTokens, "W_ATTR_FORMTYPE");
							formTypeNode.getToken().setText(fullType);
							formTypeNode.setParent(wParent);
							wParent.addChild(formTypeNode);
						}
					} 
					
					// handle word prefixes
					String wType = null;
					int sIndex = 0;
					if(addWord.startsWith("0") && !addWord.equals("0")) {
						wType = "omission";
						sIndex = 1;
					} else if(addWord.startsWith("00") && !addWord.equals("00")) {
						wType = "ellipsis";
						sIndex = 2;
					} else if(addWord.startsWith("&") && !addWord.equals("&")) {
						wType = "fragment";
						sIndex =1;
					}
					
					if(wType != null) {
						CommonTree typeNode = 
							AntlrUtils.createToken(chatTokens, "W_ATTR_TYPE");
						typeNode.getToken().setText(wType);
						typeNode.setParent(wParent);
						wParent.addChild(typeNode);
						
						addWord = addWord.substring(sIndex);
					}
					
					// fix shortening and cmp characters
//					addWord = addWord.replaceAll("<", "(").replaceAll(">", ")");
//					if(addWord.contains("<") && addWord.contains(">")) {
						String val = "";
						for(char c:addWord.toCharArray()) {
							if(c == '<') {
								if(val.length() > 0)
									addTextNode(wParent, val);
								val = "";
							} else if(c == '>') {
								if(val.length() > 0)
									addShortening(wParent, val);
								val = "";
							} else if(c == '+' || c == '~') {
								if(val.length() > 0)
									addTextNode(wParent, val);
								CommonTree wkTree = AntlrUtils.createToken(chatTokens, "WK_START");
								wkTree.setParent(wParent);
								wParent.addChild(wkTree);

								CommonTree wkTypeTree = AntlrUtils.createToken(chatTokens, "WK_ATTR_TYPE");
								if(c == '+')
									wkTypeTree.getToken().setText("cmp");
								else
									wkTypeTree.getToken().setText("cli");
								wkTypeTree.setParent(wkTree);
								wkTree.addChild(wkTypeTree);
								val = "";
							}
							else {
								val += c;
							}
						}
						if(val.length() > 0)
							addTextNode(wParent, val);
				}
			}
			
			if(hasTarget || hasActual) {
				CommonTree grpNode = nodeStack.peek();
//				// insert ipa transcriptions

				// model form
				if(hasTarget) {
					CommonTree targetNode =
							AntlrUtils.createToken(chatTokens, "MODEL_START");
					targetNode.setParent(grpNode);
					targetNode.getToken().setText("MS");
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
				if(hasActual && hasTarget && wGrp.getPhoneAlignment() != null) {
					addAlignment(grpNode, wGrp.getPhoneAlignment());
				}
			}
			
		}
		
		// add the terminator
		addTerminator(uNode, termType);
		
		// check if the utterance has a 'postcode' dependent tier
		IDependentTier postcodeTier = 
			utt.getDependentTier("Postcode");
		if(postcodeTier != null) {
			addPostcode(uNode, postcodeTier.getTierValue());
		}

		IDependentTier uttLangTier =
				utt.getDependentTier("uttlan");
		if(uttLangTier != null) {
			CommonTree langAttrTree = AntlrUtils.createToken(chatTokens, "U_ATTR_LANG");
			langAttrTree.setParent(uNode);
			uNode.insertChild(0, langAttrTree);
			langAttrTree.getToken().setText(uttLangTier.getTierValue());
		}
		
		// add media
		if(utt.getMedia() != null) {
			addMedia(uNode, utt.getMedia());
		}
		
		// process dependent tiers
		// FLAT TIERS
		for(IDependentTier depTier:utt.getDependentTiers()) {
			
			if(depTier.getTierName().equals("Postcode")) {
				continue;
			}
			
			if(depTier.getTierName().equalsIgnoreCase("uttlan")) {
				continue;
			}
			
			if(StringUtils.strip(depTier.getTierValue()).length() == 0) continue;
			
			CommonTree depTierNode =
				AntlrUtils.createToken(chatTokens, "A_START");
			depTierNode.setParent(uNode);
			
			
			CommonTree typeNode =
				AntlrUtils.createToken(chatTokens, "A_ATTR_TYPE");
			
			String depTierName = depTier.getTierName();
			
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
				String tName = tierNameMap.get(depTier.getTierName());
				flavorNode.getToken().setText(tName);
				flavorNode.setParent(depTierNode);
				depTierNode.addChild(flavorNode);
			}
			
			addTextNode(depTierNode, depTier.getTierValue());
		}
		
		final List<String> grpDepTierNames = utt.getWordAlignedTierNames();
		if(grpDepTierNames.contains("Morphology")) {
			try {
				final List<CommonTree> mortrees = processMorphology(utt, uNode);
				
				if(grpDepTierNames.contains("GRASP")) {
					processGRASP(utt, uNode);
				}
			} catch (IllegalArgumentException e) {
				final TreeBuilderException treeBuilderException = new TreeBuilderException(e);
				treeBuilderException.setUtt(utt);
				throw treeBuilderException;
			}
		}
		
		// GROUP TIERS
		List<String> handeledTiers = new ArrayList<String>();
		// add mor tiers to already handeled list
		handeledTiers.add("Morphology");
		handeledTiers.add("trn");
		handeledTiers.add("GRASP");
		for(String grpDepTierName:utt.getWordAlignedTierNames()) {
			if(handeledTiers.contains(grpDepTierName))
				continue;
			
			handeledTiers.add(grpDepTierName);
			
			String grpDepTierVal = 
				StringUtils.strip(utt.getTierString(grpDepTierName));
			List<String> grpVals = StringUtils.extractedBracketedStrings(grpDepTierVal);
			
			// ensure we have some data to export
			boolean hasData = false;
			for(String grpVal:grpVals) {
				if(StringUtils.strip(grpVal).length() > 0) {
					hasData = true;
				}
			}
			
			String exportData = "";
			for(String grpVal:grpVals) {
				exportData += (exportData.length() > 0 ? " " : "") + "[" + 
					(grpVal.length() > 0 ? grpVal : " ") + "]";
			}
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
				String tName = tierNameMap.get(grpDepTierName);
				flavorNode.getToken().setText(tName);
				flavorNode.setParent(depTierNode);
				depTierNode.addChild(flavorNode);
				
				uNode.addChild(depTierNode);
				
				addTextNode(depTierNode, exportData);
			}
		}
		
		// notes
		if(utt.getNotes() != null && utt.getNotes().length() > 0) {
			CommonTree notesNode =
				AntlrUtils.createToken(chatTokens, "A_START");
			notesNode.setParent(uNode);
			uNode.addChild(notesNode);
			
			CommonTree typeNode = 
				AntlrUtils.createToken(chatTokens, "A_ATTR_TYPE");
			typeNode.getToken().setText("comments");
			typeNode.setParent(notesNode);
			notesNode.addChild(typeNode);
			
			addTextNode(notesNode, utt.getNotes());
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
	private List<CommonTree> processMorphology(IUtterance utt, CommonTree uNode) 
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
		final String morphologyTierValue = TranscriptUtils.getTierValue(utt, "Morphology");
		final List<String> morGrpVals = StringUtils.extractedBracketedStrings(morphologyTierValue);
		final List<String> morWrdVals = new ArrayList<String>();
		for(String grpVal:morGrpVals) {
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
	private void processGRASP(IUtterance utt, CommonTree uNode) 
		throws IllegalArgumentException {
		final MorBuilder mb = new MorBuilder();
		final List<CommonTree> mortrees =  
				AntlrUtils.findAllChildrenWithType(uNode, chatTokens, "MOR_START", "MOR_PRE_START", "MOR_POST_START");
		final String graTierVal = TranscriptUtils.getTierValue(utt, "GRASP");
		final List<String> grpTierVals = StringUtils.extractedBracketedStrings(graTierVal);
		final List<CommonTree> graTrees = new ArrayList<CommonTree>();
		for(String grpTierVal:grpTierVals) {
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

	private void insertEvent(CommonTree parent, String eData) {
		// event formats
		// 1) *something* - action
		// 2) *=something* - happening 
		// 3) *PART_ID=something* - otherSpokenEvent
		//
		// each of the above can be followed by one or more
		// or markers, overlaps, repetition, etc.
		final Pattern subElePattern = Pattern.compile("(\\(.*?\\))");
		final Matcher subEleMatcher = subElePattern.matcher(eData);
		
		int lastIdx = eData.length();
		final List<String> subData = new ArrayList<String>();
		while(subEleMatcher.find()) {
			final String eleData = subEleMatcher.group(1);
			subData.add(eleData);
			
			if(lastIdx > subEleMatcher.start()) {
				lastIdx = subEleMatcher.start();
			}
		}
		
		final CommonTree eNode = 
				AntlrUtils.createToken(chatTokens, "E_START");
		eNode.setParent(parent);
		parent.addChild(eNode);
		
		final String evtData = eData.substring(0, lastIdx);
		if(evtData.length() > 0) {
			final Pattern osePattern = Pattern.compile("(.+)=(.+)");
			final Matcher oseMatcher = osePattern.matcher(evtData);
			
			if(oseMatcher.matches()) {
				final String participantId = oseMatcher.group(1);
				final String oseWord = oseMatcher.group(2);
				addOtherSpokenEvent(eNode, participantId, oseWord);
			} else {
				// everything is a 'happening'
				if(evtData.startsWith("=")) {
					// insert happening
					final String htxt = evtData.substring(1);
					addHappening(eNode, htxt);
				} else {
					addHappening(eNode, evtData);
				}
			}
		}
		
		for(String subEle:subData) {
			handleParentheticData(eNode, subEle);
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
	private void addSyllabification(CommonTree parent, IPhoneticRep phoRep) {
		final List<Phone> phones = phoRep.getPhones();
		final PhoTreeBuilder phoTreeBuilder = new PhoTreeBuilder();
		final List<CommonTree> pwTrees = phoTreeBuilder.buildPwTrees(phones);
		for(CommonTree pwTree:pwTrees) {
			parent.addChild(pwTree);
			pwTree.setParent(parent);
		}
	}
	
	/**
	 * wk
	 */
	private void insertWordnet(CommonTree parent, String type) {
		CommonTree wkNode =
			AntlrUtils.createToken(chatTokens, "WK_START");
		wkNode.setParent(parent);
		
		CommonTree typeNode =
			AntlrUtils.createToken(chatTokens, "WK_ATTR_TYPE");
		typeNode.getToken().setText(type);
		typeNode.setParent(wkNode);
		wkNode.addChild(typeNode);
		
		parent.addChild(wkNode);
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
	private void addPostcode(CommonTree parent, String data) {
		CommonTree pcNode = 
			AntlrUtils.createToken(chatTokens, "POSTCODE_START");
		pcNode.setParent(parent);
		parent.addChild(pcNode);
		
		addTextNode(pcNode, data);
	}
	
	/**
	 * Add an error element
	 */
	private void addError(CommonTree parent, String data) {
		CommonTree eNode = 
			AntlrUtils.createToken(chatTokens, "ERROR_START");
		eNode.setParent(parent);
		parent.addChild(eNode);
		
		addTextNode(eNode, data);
	}
	
	/**
	 * Add a happening element
	 */
	private void addHappening(CommonTree parent, String data) {
		CommonTree hNode = 
			AntlrUtils.createToken(chatTokens, "HAPPENING_START");
		hNode.setParent(parent);
		parent.addChild(hNode);
		
		addTextNode(hNode, data);
	}
	
	
	/**
	 * Add other spoken event
	 * 
	 */
	private void addOtherSpokenEvent(CommonTree parent, String speaker, String data) {
		CommonTree oseNode =
				AntlrUtils.createToken(chatTokens, "OTHERSPOKENEVENT_START");
		oseNode.setParent(parent);
		parent.addChild(oseNode);
		
		CommonTree whoNode =
				AntlrUtils.createToken(chatTokens, "OTHERSPOKENEVENT_ATTR_WHO");
		whoNode.getToken().setText(speaker);
		whoNode.setParent(oseNode);
		oseNode.addChild(whoNode);
		
		CommonTree wNode =
				AntlrUtils.createToken(chatTokens, "W_START");
		wNode.setParent(oseNode);
		oseNode.addChild(wNode);
		
		addTextNode(wNode, data);
	}
	
	private void addInternalMedia(CommonTree parent, String data) {
		CommonTree imNode =
				AntlrUtils.createToken(chatTokens, "INTERNAL_MEDIA_START");
		imNode.setParent(parent);
		parent.addChild(imNode);
		
		String[] range = data.split("-");
		
		CommonTree startAttrNode=
				AntlrUtils.createToken(chatTokens, "INTERNAL_MEDIA_ATTR_START");
		startAttrNode.getToken().setText(range[0]);
		imNode.addChild(startAttrNode);
		startAttrNode.setParent(imNode);
		
		CommonTree endAttrNode =
				AntlrUtils.createToken(chatTokens, "INTERNAL_MEDIA_ATTR_END");
		endAttrNode.getToken().setText(range[1]);
		imNode.addChild(endAttrNode);
		endAttrNode.setParent(imNode);
		
		CommonTree unitNode =
				AntlrUtils.createToken(chatTokens, "INTERNAL_MEDIA_ATTR_UNIT");
		unitNode.getToken().setText("s");
		unitNode.setParent(imNode);
		imNode.addChild(unitNode);
	}
	
	private void addOverlapPoint(CommonTree parent, String data) {
		CommonTree opNode = 
				AntlrUtils.createToken(chatTokens, "OVERLAP_POINT_START");
		opNode.setParent(parent);
		parent.addChild(opNode);
		
		String[] attrs = data.split(",");
		
		int atrIdx = 0;
		if(attrs.length == 3) {
			CommonTree idxNode = 
					AntlrUtils.createToken(chatTokens, "OVERLAP_POINT_ATTR_INDEX");
			idxNode.getToken().setText(attrs[atrIdx++]);
			idxNode.setParent(opNode);
			opNode.addChild(idxNode);
		}
		
		if(attrs.length - atrIdx == 2) {
			CommonTree startEndNode =
					AntlrUtils.createToken(chatTokens, "OVERLAP_POINT_ATTR_START_END");
			startEndNode.getToken().setText(attrs[atrIdx++]);
			startEndNode.setParent(opNode);
			opNode.addChild(startEndNode);
			
			CommonTree topBtmNode =
					AntlrUtils.createToken(chatTokens, "OVERLAP_POINT_ATTR_TOP_BOTTOM");
			topBtmNode.getToken().setText(attrs[atrIdx++]);
			topBtmNode.setParent(opNode);
			opNode.addChild(topBtmNode);
		}
	}
	
	/**
	 * Add a replacement element
	 */
	private void addReplacement(CommonTree parent, String data) {
		CommonTree rNode = 
			AntlrUtils.createToken(chatTokens, "REPLACEMENT_START");
		rNode.setParent(parent);
		parent.addChild(rNode);
		
		String[] wEles = data.split("\\p{Space}");
		for(String wEle:wEles) {
			CommonTree wNode = 
				AntlrUtils.createToken(chatTokens, "W_START");
			wNode.setParent(rNode);
			rNode.addChild(wNode);
			
			addTextNode(wNode, wEle);
		}
	}
	
	/**
	 * Add an action element.
	 */
	private void addAction(CommonTree parent, String data) {
		CommonTree aNode =
			AntlrUtils.createToken(chatTokens, "ACTION_START");
		aNode.setParent(parent);
		parent.addChild(aNode);
		
		if(data == null) data = "";
		addTextNode(aNode, data);
	}
	
	/**
	 * Add a 'ga' element
	 */
	private void addGa(CommonTree parent, String type, String data) {
		CommonTree gaNode = 
			AntlrUtils.createToken(chatTokens, "GA_START");
		gaNode.setParent(parent);
		parent.addChild(gaNode);
		
		CommonTree gaTypeNode = 
			AntlrUtils.createToken(chatTokens, "GA_ATTR_TYPE");
		gaTypeNode.getToken().setText(type);
		gaTypeNode.setParent(gaNode);
		gaNode.addChild(gaTypeNode);
		
		addTextNode(gaNode, data);
	}
	
	/**
	 * Add a pause element
	 * 
	 */
	private void addPause(CommonTree parent, String data) {
		CommonTree pNode = 
			AntlrUtils.createToken(chatTokens, "PAUSE_START");
		pNode.setParent(parent);
		parent.addChild(pNode);
		
		if(data.equals(".")) {
			CommonTree slNode = 
				AntlrUtils.createToken(chatTokens, "PAUSE_ATTR_SYMBOLIC_LENGTH");
			slNode.getToken().setText("simple");
			slNode.setParent(pNode);
			pNode.addChild(slNode);
		} else if(data.equals("..")) {
			CommonTree slNode = 
				AntlrUtils.createToken(chatTokens, "PAUSE_ATTR_SYMBOLIC_LENGTH");
			slNode.getToken().setText("long");
			slNode.setParent(pNode);
			pNode.addChild(slNode);
			
		} else if(data.equals("...")) {
			CommonTree slNode = 
				AntlrUtils.createToken(chatTokens, "PAUSE_ATTR_SYMBOLIC_LENGTH");
			slNode.getToken().setText("very long");
			slNode.setParent(pNode);
			pNode.addChild(slNode);
		} else if(data.startsWith("pause:")) {
			String numStr = data.substring(data.indexOf(':')+1);
			CommonTree slNode = 
				AntlrUtils.createToken(chatTokens, "PAUSE_ATTR_LENGTH");
			slNode.getToken().setText(numStr);
			slNode.setParent(pNode);
			pNode.addChild(slNode);
		} else {
			PhonLogger.warning("Invalid pause type '" + data + "'");
		}
		
//		if(data.length() > 0) {
//			CommonTree slNode =
//				AntlrUtils.createToken(chatTokens, "PAUSE_ATTR_SYMBOLIC_LENGTH");
//			slNode.getToken().setText(data);
//			slNode.setParent(pNode);
//			pNode.addChild(slNode);
//		} else {
//			PhonLogger.warning("Invalid pause structure");
//		}
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
	
	/**
	 * Add a shortenting element
	 */
	private void addShortening(CommonTree parent, String data) {
		CommonTree shNode = 
			AntlrUtils.createToken(chatTokens, "SHORTENING_START");
		shNode.setParent(parent);
		addTextNode(shNode, data);
		
		parent.addChild(shNode);
	}
	
	/**
	 * Add a marker <k> element
	 */
	private void addMarker(CommonTree parent, String data) {
		String type = null;
		
		if(data.equals("!")) {
			type = "stressing";
		} else if(data.equals("!!")) {
			type = "contrastive stressing";
		} else if(data.equals("?")) {
			type = "best guess";
		} else if(data.equals("/")) {
			type = "retracing";
		} else if(data.equals("//")) {
			type = "retracing with correction";
		} else if(data.equals("///")) {
			type = "retracing reformulation";
		} else if(data.equals("/?")) {
			type = "retracing unclear";
		} else if(data.equals("/-")) {
			type = "false start";
		} else {
			PhonLogger.warning("Invalid marker type '" + data + "'");
		}
		
		if(type != null) {
			CommonTree markerNode = 
				AntlrUtils.createToken(chatTokens, "K_START");
			markerNode.setParent(parent);
			parent.addChild(markerNode);
			
			CommonTree typeNode = 
				AntlrUtils.createToken(chatTokens, "K_ATTR_TYPE");
			typeNode.getToken().setText(type);
			typeNode.setParent(markerNode);
			markerNode.addChild(typeNode);
		}
	}
	
	/**
	 * Add a repetition element
	 */
	private void addRepetition(CommonTree parent, String times) {
		CommonTree rNode =
			AntlrUtils.createToken(chatTokens, "R_START");
		rNode.setParent(parent);
		parent.addChild(rNode);
		
		CommonTree timesNode =
			AntlrUtils.createToken(chatTokens, "R_ATTR_TIMES");
		timesNode.getToken().setText(times);
		timesNode.setParent(rNode);
		rNode.addChild(timesNode);
	}
	
	/**
	 * Add an overlap element
	 */
	private void addOverlap(CommonTree parent, String ovdata) {
		CommonTree ovNode = 
			AntlrUtils.createToken(chatTokens, "OVERLAP_START");
		ovNode.setParent(parent);
		parent.addChild(ovNode);
		
		final Pattern overlapPattern = Pattern.compile("([<>])([0-9]*)");
		final Matcher matcher = overlapPattern.matcher(ovdata);
		
		if(matcher.matches()) {
			String ovType = matcher.group(1);
			String actualType = "";
			if(ovType.equals(">")) {
				actualType = "overlap follows";
			} else if(ovType.equals("<")) {
				actualType = "overlap precedes";
			}
			CommonTree typeNode = 
				AntlrUtils.createToken(chatTokens, "OVERLAP_ATTR_TYPE");
			typeNode.getToken().setText(actualType);
			typeNode.setParent(ovNode);
			ovNode.addChild(typeNode);
			
			if(matcher.group(2) != null && matcher.group(2).length() > 0) {
				String ovIndex = matcher.group(2);
				
				CommonTree indexNode =
					AntlrUtils.createToken(chatTokens, "OVERLAP_ATTR_INDEX");
				indexNode.getToken().setText(ovIndex);
				indexNode.setParent(ovNode);
				ovNode.addChild(indexNode);
			}
		}
	}
	
	/**
	 * Add a linker element
	 */
	private void addLinker(CommonTree parent, String lkType) {
		CommonTree lkNode = 
			AntlrUtils.createToken(chatTokens, "LINKER_START");
		lkNode.setParent(parent);
		
		if(parent.getChildCount() > 1) {
			
			int sIndex = 0;
			int cIndex = 0;
			CommonTree cNode = (CommonTree)parent.getChild(cIndex++);
			while(cNode != null && (cNode.getToken().getType() == chatTokens.getTokenType("U_ATTR_WHO")
					|| cNode.getToken().getType() == chatTokens.getTokenType("LINKER_START")))
			{
				sIndex++;
				cNode = (CommonTree)parent.getChild(cIndex++);
			}
			
			List<CommonTree> nodes = new ArrayList<CommonTree>();
			for(int i = sIndex; i < parent.getChildCount(); i++) {
				nodes.add((CommonTree)parent.getChild(i));
			}
			parent.replaceChildren(sIndex, parent.getChildCount()-1, lkNode);
			for(CommonTree c:nodes) parent.addChild(c);
		} else {
			parent.addChild(lkNode);
		}
		
		String actualType = "";
		if(lkType.equals("+\"")) {
			actualType = "quoted utterance next";
		} else if(lkType.equals("+^")) {
			actualType = "quick uptake";
		} else if(lkType.equals("+<")) {
			actualType = "lazy overlap mark";
		} else if(lkType.equals("+,")) {
			actualType = "self completion";
		} else if(lkType.equals("++")) {
			actualType = "other completion";
		} else if(lkType.equals("+0x224b")) {
			actualType = "TCU completion";
		} else if(lkType.equals("+0x2248")) {
			actualType = "no break completion";
		}
		CommonTree typeNode = 
			AntlrUtils.createToken(chatTokens, "LINKER_ATTR_TYPE");
		typeNode.getToken().setText(actualType);
		typeNode.setParent(lkNode);
		lkNode.addChild(typeNode);
	}
	
	/**
	 * Add a langs element
	 */
	private void addLangs(CommonTree parent, String data) {
		/*
		 * Langs format is:
		 *  (single|multiple|ambiguous),<lang data>
		 */
		int cIndex = data.indexOf(',');
		if(cIndex < 0) return;
		
		String langsType = data.substring(0, cIndex);
		String langsData = data.substring(cIndex+1);
		
		CommonTree langsNode = 
			AntlrUtils.createToken(chatTokens, "LANGS_START");
		langsNode.setParent(parent);
		parent.addChild(langsNode);
		
		CommonTree dataNode = null;
		if(langsType.equals("single")) {
			dataNode = 
			AntlrUtils.createToken(chatTokens, "SINGLE_START");
		} else if(langsType.equals("multiple")) {
			dataNode = 
			AntlrUtils.createToken(chatTokens, "MULTIPLE_START");
		} else if(langsType.equals("ambiguous")) {
			dataNode =
			AntlrUtils.createToken(chatTokens, "AMBIGUOUS_START");
		} else {
			// just make it ambiguous
			dataNode =
			AntlrUtils.createToken(chatTokens, "AMBIGUOUS_START");
		}
		addTextNode(dataNode, langsData);
		dataNode.setParent(langsNode);
		langsNode.addChild(dataNode);
	}
	
	/**
	 * Add a media element
	 */
	private void addMedia(CommonTree parent, IMedia media) {
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
	
}
