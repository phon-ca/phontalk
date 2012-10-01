package ca.phon.phontalk;

import ca.phon.exceptions.ParserException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
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
import org.antlr.runtime.tree.CommonTreeNodeStream;

import ca.phon.alignment.PhoneMap;

import ca.phon.application.project.IPhonProject;
import ca.phon.application.project.PhonProject;
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
import ca.phon.application.transcript.TranscriptElement;
import ca.phon.phone.Phone;
import ca.phon.phone.PhoneSequenceMatcher;
import ca.phon.phontalk.parser.ChatTokens;
import ca.phon.phontalk.parser.Phon2XmlWalker;
import ca.phon.syllable.Syllable;
import ca.phon.syllable.SyllableConstituentType;
import ca.phon.system.logger.PhonLogger;
import ca.phon.util.PhonDateFormat;
import ca.phon.util.PhonDuration;
import ca.phon.util.PhonDurationFormat;
import ca.phon.util.Range;
import ca.phon.util.StringUtils;
import java.text.SimpleDateFormat;
/**
 * This class is responsible for turning Phon sessions
 * into chat ANTLR trees.
 *
 */
public class PhonTreeBuilder {
	
	private static final ChatTokens tokens = new ChatTokens();
	
	/*
	 * Word pattern
	 * Words have 3 sections <type>&<data>@<formType> 
	 */
	private Pattern wordPattern = 
		Pattern.compile("([^@]+)(?:@([^@]+))?");
	
	private Pattern langPattern = 
		Pattern.compile("[a-zA-Z]{3}(-[a-zA-Z0-9]{1,8})*");
	
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
		"paralinguistics",
		"SALT",
		"speech act",
		"time stamp",
		"translation",
		"extension"
	};
	
	public CommonTree buildTree(ITranscript t) {
		CommonTree retVal = createToken("CHAT_START");
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
		
		CommonTree commentTree = createToken("COMMENT_START");
		commentTree.setParent(tree);
		tree.addChild(commentTree);

		CommonTree attrTree = createToken("COMMENT_ATTR_TYPE");
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
			CommonTree node = createToken("CHAT_ATTR_MEDIA");
			
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
			
			CommonTree node2 = createToken("CHAT_ATTR_MEDIATYPES");
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
			
			CommonTree node = createToken("CHAT_ATTR_LANG");
			node.setParent(tree);
			node.getToken().setText(lang.replaceAll(",", " "));
			tree.addChild(node);
			
		}
		
		// corpus
		CommonTree cNode = createToken("CHAT_ATTR_CORPUS");
		cNode.getToken().setText(t.getCorpus());
		cNode.setParent(tree);
		tree.addChild(cNode);
		
		// id
		CommonTree idNode = createToken("CHAT_ATTR_ID");
		idNode.getToken().setText(t.getID());
		idNode.setParent(tree);
		tree.addChild(idNode);
		
		// date
		PhonDateFormat pdf = new PhonDateFormat(PhonDateFormat.YEAR_LONG);
		String dString = pdf.format(t.getDate());
		CommonTree dateNode = createToken("CHAT_ATTR_DATE");
		dateNode.getToken().setText(dString);
		dateNode.setParent(tree);
		tree.addChild(dateNode);
	}
	
	/**
	 * Participants
	 */
	private void setupParticipants(CommonTree tree, ITranscript t) {
		// parent participantS node
		CommonTree parent = 
			createToken("PARTICIPANTS_START");
		parent.setParent(tree);
		
		for(IParticipant p:t.getParticipants()) {
			CommonTree pNode =
				createToken("PARTICIPANT_START");
			pNode.setParent(parent);
			
			String partId = p.getId();
			// change p0 to CHI
//			if(partId.equalsIgnoreCase("p0")) {
//				partId = "CHI";
//			}
			CommonTree pId = 
				createToken("PARTICIPANT_ATTR_ID");
			pId.getToken().setText(partId);
			pId.setParent(pNode);
			pNode.addChild(pId);
			
			if(p.getName() != null && p.getName().length() > 0) {
				CommonTree pName = 
					createToken("PARTICIPANT_ATTR_NAME");
				pName.getToken().setText(p.getName());
				pName.setParent(pNode);
				pNode.addChild(pName);
			}
			
			String role = "Target_Child";
			if(p.getRole() != null && p.getRole().length() > 0) {
				role = p.getRole();
			}
				CommonTree pRole = 
					createToken("PARTICIPANT_ATTR_ROLE");
				pRole.getToken().setText(role);
				pRole.setParent(pNode);
				pNode.addChild(pRole);
			
			
			if(p.getLanguage() != null && p.getLanguage().length() > 0) {
				
				Matcher m = langPattern.matcher(p.getLanguage());
				if(!m.matches()) {
					PhonLogger.warning("Participant " + partId + " language '" + p.getLanguage() + "' does not match pattern [a-zA-Z]{3}(-[a-zA-Z0-9]{1,8})*");
				} else {
					CommonTree pLang = 
						createToken("PARTICIPANT_ATTR_LANGUAGE");
					pLang.getToken().setText(p.getLanguage());
					pLang.setParent(pNode);
					pNode.addChild(pLang);
				}
			}
			
			PhonDuration age = p.getAge(t.getDate());
			if(!age.isNegative() && !age.isZero()) {
				PhonDurationFormat pdf = new PhonDurationFormat(PhonDurationFormat.XML_FORMAT);
				CommonTree pAge = 
					createToken("PARTICIPANT_ATTR_AGE");
				
				pAge.getToken().setText(pdf.format(age));
				pAge.setParent(pNode);
				pNode.addChild(pAge);
				
				PhonDateFormat dateFormat = new PhonDateFormat(PhonDateFormat.YEAR_LONG);
				CommonTree pBday = 
					createToken("PARTICIPANT_ATTR_BIRTHDAY");
				pBday.getToken().setText(dateFormat.format(p.getBirthDate()));
				pBday.setParent(pNode);
				pNode.addChild(pBday);
			}
			
			if(p.getGroup() != null && p.getGroup().length() > 0) {
				CommonTree pGroup =
					createToken("PARTICIPANT_ATTR_GROUP");
				pGroup.getToken().setText(p.getGroup());
				pGroup.setParent(pNode);
				pNode.addChild(pGroup);
			}
			
			if(p.getSex() != null) {
				CommonTree pSex =
					createToken("PARTICIPANT_ATTR_SEX");
				pSex.getToken().setText(p.getSex().toString().toLowerCase());
				pSex.setParent(pNode);
				pNode.addChild(pSex);
			}
			
			if(p.getSES() != null && p.getSES().length() > 0) {
				CommonTree pSes = 
					createToken("PARTICIPANT_ATTR_SES");
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
	private void processTranscript(CommonTree tree, ITranscript t) {
		// go through the transcript contents
		boolean dateCommentInserted = false;
		for(TranscriptElement<Object> tele:t.getTranscriptElements()) {
			if(tele.getValue() instanceof IComment) {
				// check for lazy-gem
				IComment comment = (IComment)tele.getValue();
				if(comment.getType().toString().equalsIgnoreCase("LazyGem")) {
					insertLazyGem(tree, comment);
				} else {
					if(((IComment)tele.getValue()).getType() == CommentEnum.Date) {
						insertDate(tree, t);
						dateCommentInserted = true;
					} else {
						insertComment(tree, (IComment)tele.getValue());
					}
				}
			} else if(tele.getValue() instanceof IUtterance) {
				if(!dateCommentInserted) {
					insertDate(tree, t);
					dateCommentInserted = true;
				}
//				System.out.println("Record: " + uttIdx++);
				insertRecord(tree, (IUtterance)tele.getValue());
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
			createToken("COMMENT_START");
		cNode.setParent(tree);
		tree.addChild(cNode);
		
		CommonTree typeNode =
			createToken("COMMENT_ATTR_TYPE");
		typeNode.setParent(cNode);
		typeNode.getToken().setText(c.getType().getTitle());
		cNode.addChild(typeNode);
		
		CommonTree textNode = 
			createToken("TEXT");
		textNode.getToken().setText(c.getValue());
		textNode.setParent(cNode);
		cNode.addChild(textNode);
		
		tree.addChild(cNode);
	}
	
	/**
	 * Insert lazy gem
	 */
	private void insertLazyGem(CommonTree parent, IComment c) {
		CommonTree lazyGemNode = 
			createToken("LAZY_GEM_START");
		lazyGemNode.setParent(parent);
		parent.addChild(lazyGemNode);
		
		CommonTree lazyGemLabel = 
			createToken("LAZY_GEM_ATTR_LABEL");
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
		int uttTokenType = tokens.getTokenType("U_START");
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
		if(data.equals("<") || data.equals(">")) 
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
		} else if(data.startsWith("=")) {
			addGa(tree, "explanation", StringUtils.strip(data.substring(1)));
		} else if(data.startsWith("!=")) {
			addGa(tree, "paralinguistics", StringUtils.strip(data.substring(2)));
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
				} else {
					PhonLogger.warning("Unsupport data " + data);
				}
			}
		}
	}
	
	/**
	 * Insert a record.
	 */
	private void insertRecord(CommonTree tree, IUtterance utt) {
		CommonTree uNode = 
			createToken("U_START");
		uNode.setParent(tree);
		
		CommonTree whoNode = 
			createToken("U_ATTR_WHO");
		
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
					createToken("PG_START");
				grpNode.setParent(uttNode);
				uttNode.addChild(grpNode);
				nodeStack.push(grpNode);
			}
			
			// break into words
			List<String> words = wGrp.getWords();
			/*
			 * We will be using (up to) 2 look ahead variables.
			 * Make sure we don't go out of bounds.
			 */
//			words.add("");
//			words.add("");
//			String la1, la2;
			String wordList[] = words.toArray(new String[0]);
			
			if(wordList.length == 0) {
				// attach a 'dummy' transcription
//				CommonTree parent = nodeStack.peek();
//				
//				CommonTree wNode = 
//					createToken("E_START");
//				wNode.setParent(parent);
//				parent.addChild(wNode);
//				
//				CommonTree typeNode = 
//					createToken("ACTION_START");
//				typeNode.setParent(wNode);
//				wNode.addChild(typeNode);
//				
//				addTextNode(typeNode, "");
				wordList = new String[1];
				wordList[0] = "0";
			}
			
			// a flag to indicat the next item in the iteration
			// should be attached to the last child of nodeStack.peek()
			boolean attachToLastChild = false;
			
			for(int wIndex = 0; wIndex < wordList.length; wIndex++) {
				String w = wordList[wIndex];

				
//				la1 = wordList[wIndex+1];
//				la2 = wordList[wIndex+2];
				
				if(w.matches("\\*.*\\*")) {
					// events have 2 formats:
					//  1) (label:data)*
					//  2) plain text
					String eData = w.substring(1, w.length()-1);
					CommonTree parentNode = nodeStack.peek();
					
					Pattern evtPattern = Pattern.compile("(\\(.*?\\)\\p{Space}?)+");
					Matcher evtMatcher = evtPattern.matcher(eData);
					
					CommonTree eNode = 
						createToken("E_START");
					eNode.setParent(parentNode);
					parentNode.addChild(eNode);
					
					if(evtMatcher.matches()) {
						// break into (.*) groups
						Pattern subElePattern = Pattern.compile("(\\(.*?\\))");
						Matcher subEleMatcher = subElePattern.matcher(eData);
						
						while(subEleMatcher.find()) {
							String subEleData = subEleMatcher.group(1);
							handleParentheticData(eNode, subEleData);
//							// break on first ':' and handle accordingly
//							int cIndex = subEleData.indexOf(':');
//							if(cIndex > 0) {
//								String eleName = subEleData.substring(1, cIndex);
//								String eleData = subEleData.substring(cIndex+1, subEleData.length()-1);
//								
//								if(eleName.equalsIgnoreCase("happening")) {
//									addHappening(eNode, eleData);
//								} else if(eleName.equalsIgnoreCase("action")) {
//									addAction(eNode, eleData);
//								} else if(eleName.equalsIgnoreCase("overlap")) {
//									addOverlap(eNode, eleData);
//								} else if(eleName.equalsIgnoreCase("ga")) {
//									String type = "comments";
//									if(eleData.startsWith("type=")) {
//										int comIdx = eleData.indexOf(',');
//										type = eleData.substring(5, comIdx);
//										String val = eleData.substring(comIdx+1);
//										addGa(eNode, type, val);
//									} else {
//										addGa(eNode, type, eleData);
//									}
//								} else {
//									PhonLogger.warning("Invalid event child " + eleName);
//								}
//							} else {
//								addGa(eNode, "comments", subEleData.substring(0, subEleData.length()-1));
//							}
							
						}
					} else {
						addGa(eNode, "comments", eData);
					}
					
					
				} else if(w.matches("\\(.*\\)")) {
					
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
							int wTokenType = tokens.getTokenType("W_START");
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
								wNode = createToken("W_START");
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
								wNode = createToken("W_START");
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
				} else if(w.matches("\\{")) {
					if(wGrp.getWord().indexOf('}') < 0) {
						// create a super-<g> node
						CommonTree superG = new CommonTree(new CommonToken(tokens.getTokenType("G_START")));
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
							createToken("G_START");
						gNode.setParent(parentNode);
						parentNode.addChild(gNode);
						
						// push new group onto stack
						nodeStack.push(gNode);
					}
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
			    	
			    } else if(w.matches("\\.")) {
			    	termType = "p";
			    } else if(w.matches("\\?")) {
				    termType = "q";
			    } else if(w.matches("!")) { 
			    	termType = "e";
			    } else if(w.matches("\\+")) { 
			    	CommonTree parentNode = nodeStack.peek();
			    	CommonTree wNode = 
			    		(CommonTree)parentNode.getChild(parentNode.getChildCount()-1);
			    	insertWordnet(wNode, "cmp");
			    	attachToLastChild = true;
			    } else {
					CommonTree parentNode = nodeStack.peek();
					CommonTree wParent = null;
					if(attachToLastChild) {
						wParent = 
							(CommonTree)parentNode.getChild(parentNode.getChildCount()-1);
						attachToLastChild = false;
					} else {
						wParent = 
							createToken("W_START");
						wParent.setParent(parentNode);
						parentNode.addChild(wParent);
					}
					
					String addWord = w;
					Matcher m = wordPattern.matcher(w);
					
					if(m.matches()) {
//						String wType = m.group(1);
						String wData = m.group(1);
						String wFormType = m.group(2);
	
						addWord = wData;
//						if(wType != null) {
//							CommonTree typeNode = 
//								createToken("W_ATTR_TYPE");
//							typeNode.getToken().setText(wType.replaceAll("_", " "));
//							typeNode.setParent(wParent);
//							wParent.addChild(typeNode);
//						}
						
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
								createToken("W_ATTR_FORMTYPE");
							formTypeNode.getToken().setText(fullType);
							formTypeNode.setParent(wParent);
							wParent.addChild(formTypeNode);
						}
						
//						if(wData != null) {
//							// fix shortening
//							wData = wData.replaceAll("<", "(");
//							wData = wData.replaceAll(">", ")");
//							addTextNode(wParent, wData);
//						} else {
//							PhonLogger.warning("Invalid orthography '" + w + "'");
//						}
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
							createToken("W_ATTR_TYPE");
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
								CommonTree wkTree = createToken("WK_START");
								wkTree.setParent(wParent);
								wParent.addChild(wkTree);

								CommonTree wkTypeTree = createToken("WK_ATTR_TYPE");
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
						
//						Pattern p = Pattern.compile("([^<]*)<([^>]*)>([^<>]*)");
//						Matcher wm = p.matcher(addWord);
//						
//						if(wm.matches()) {
//							String p1 = wm.group(1);
//							String p2 = wm.group(2);
//							String p3 = wm.group(3);
//							
//							if(StringUtils.strip(p1).length() > 0) {
//								addTextNode(wParent, p1);
//							}
//							
//							if(StringUtils.strip(p2).length() > 0) {
//								addShortening(wParent, p2);
//							}
//							
//							if(StringUtils.strip(p3).length() > 0) {
//								addTextNode(wParent, p3);
//							}
//						}
//					} else {
//						addTextNode(wParent, addWord);
//					}
				}
			}
			
			if(hasTarget || hasActual) {
				CommonTree grpNode = nodeStack.peek();
//				// insert ipa transcriptions
//				for(IPhoneticRep phoRep:wGrp.getPhoneticRepresentations()) {
//
//					if(phoRep.getPhones().size() == 0) continue;
//
//					CommonTree phoNode =
//						createToken("PHO_START");
//					phoNode.setParent(grpNode);
//
//					CommonTree formNode =
//						createToken("PHO_ATTR_FORM");
//					String formTxt =
//						(phoRep.getForm() == Form.Target ? "model" : "actual");
//					formNode.getToken().setText(formTxt);
//					formNode.setParent(phoNode);
//					phoNode.addChild(formNode);
//
//					CommonTree valNode =
//						createToken("PHO_ATTR_VALUE");
//					valNode.getToken().setText(phoRep.getTranscription());
//					valNode.setParent(phoNode);
//					phoNode.addChild(valNode);
//
//					grpNode.addChild(phoNode);
//				}

				// model form
				if(hasTarget) {
					CommonTree targetNode =
							createToken("MODEL_START");
					targetNode.setParent(grpNode);
					targetNode.getToken().setText("MS");
					grpNode.addChild(targetNode);

//					CommonTree valueNode =
//							createToken("VALUE_START");
//					valueNode.getToken().setText("VS");
//					addTextNode(valueNode, tRep.getTranscription());
//					valueNode.setParent(targetNode);
//					targetNode.addChild(valueNode);

					// syllabification
					addSyllabification(targetNode, tRep);
				}

				if(hasActual) {
					CommonTree actualNode =
							createToken("ACTUAL_START");
					actualNode.setParent(grpNode);
					grpNode.addChild(actualNode);
					
//					CommonTree valueNode =
//							createToken("VALUE_START");
//					addTextNode(valueNode, aRep.getTranscription());
//					valueNode.setParent(actualNode);
//					actualNode.addChild(valueNode);

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
		
		// add media
		if(utt.getMedia() != null) {
			addMedia(uNode, utt.getMedia());
		}
		
		for(IDependentTier depTier:utt.getDependentTiers()) {
			
			if(depTier.getTierName().equals("Postcode")) {
				continue;
			}
			
			if(StringUtils.strip(depTier.getTierValue()).length() == 0) continue;
			
			CommonTree depTierNode =
				createToken("A_START");
			depTierNode.setParent(uNode);
			
			
			CommonTree typeNode =
				createToken("A_ATTR_TYPE");
			
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
					createToken("A_ATTR_FLAVOR");
				String tName = tierNameMap.get(depTier.getTierName());
				flavorNode.getToken().setText(tName);
				flavorNode.setParent(depTierNode);
				depTierNode.addChild(flavorNode);
			}
			
			addTextNode(depTierNode, depTier.getTierValue());
		}
		
		List<String> handeledTiers = new ArrayList<String>();
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
					createToken("A_START");
				depTierNode.setParent(uNode);
				
				CommonTree typeNode =
					createToken("A_ATTR_TYPE");
				typeNode.getToken().setText("extension");
				
				typeNode.setParent(depTierNode);
				depTierNode.addChild(typeNode);
				
				CommonTree flavorNode = 
					createToken("A_ATTR_FLAVOR");
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
				createToken("A_START");
			notesNode.setParent(uNode);
			uNode.addChild(notesNode);
			
			CommonTree typeNode = 
				createToken("A_ATTR_TYPE");
			typeNode.getToken().setText("comments");
			typeNode.setParent(notesNode);
			notesNode.addChild(typeNode);
			
			addTextNode(notesNode, utt.getNotes());
		}
		
		tree.addChild(uNode);
	}

	/*
	 * align
	 */
	private void addAlignment(CommonTree parent, PhoneMap pm) {
		CommonTree alignNode = createToken("ALIGN_START");
		alignNode.setParent(parent);
		parent.addChild(alignNode);

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
			CommonTree colTree = createToken("COL_START");
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

				CommonTree modelTree = createToken("MODELREF_START");
				String modelRef = "ph" + tpIdx;
				addTextNode(modelTree, modelRef);
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

				CommonTree actualTree = createToken("ACTUALREF_START");
				String actualRef = "ph" + apIdx;
				addTextNode(actualTree, actualRef);
				actualTree.setParent(colTree);
				colTree.addChild(actualTree);

				lastAIdx = apIdx;
			}
		}
	}

	/*
	 * sb
	 */
	private void addSyllabification(CommonTree parent, IPhoneticRep phoRep) {
		List<Phone> phones = phoRep.getPhones();

		// split into words
		PhoneSequenceMatcher phonex = null;
		try {
			phonex =
					PhoneSequenceMatcher.compile("{}:-WordBoundaryMarker*");
		} catch (ParserException ex) {
			PhonLogger.warning(ex.toString());
		}

		int phIdx = 0;
		List<Range> wordRanges = phonex.findRanges(phones);
		for(Range wordRange:wordRanges) {
			CommonTree pwTree = createToken("PW_START");
			pwTree.setParent(parent);
			parent.addChild(pwTree);

			for(int pIndex:wordRange) {
				Phone p = phones.get(pIndex);

				if(p.getPhoneString().equals("+")) {
					// add wk
					CommonTree wkTree = createToken("WK_START");
					wkTree.setParent(pwTree);
					pwTree.addChild(wkTree);

					CommonTree wkTypeTree = createToken("WK_ATTR_TYPE");
					wkTypeTree.getToken().setText("cmp");
					wkTypeTree.setParent(wkTree);
					wkTree.addChild(wkTypeTree);
				} else if(p.getScType() == SyllableConstituentType.SyllableStressMarker) {
					// add ss
					CommonTree ssTree = createToken("SS_START");
					ssTree.setParent(pwTree);
					pwTree.addChild(ssTree);

					CommonTree ssTypeTree = createToken("SS_ATTR_TYPE");
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
					CommonTree phTree = createToken("PH_START");
					phTree.setParent(pwTree);
					pwTree.addChild(phTree);

					CommonTree phIdTree = createToken("PH_ATTR_ID");
					phIdTree.getToken().setText(phId);
					phIdTree.setParent(phTree);
					phTree.addChild(phIdTree);

					CommonTree scTree = createToken("PH_ATTR_SCTYPE");
					scTree.getToken().setText(p.getScType().getIdentifier());
					scTree.setParent(phTree);
					phTree.addChild(scTree);

					addTextNode(phTree, p.getPhoneString());
				}
			}
		}
	}
	
	/**
	 * wk
	 */
	private void insertWordnet(CommonTree parent, String type) {
//		if(lastEle == null) {
//			lastEle = new CommonTree(
//					new CommonToken(tokens.getTokenType("W_START")));
//			lastEle.setParent(grpNode);
//			grpNode.addChild(lastEle);
//		}
		CommonTree wkNode =
			createToken("WK_START");
		wkNode.setParent(parent);
		
		CommonTree typeNode =
			createToken("WK_ATTR_TYPE");
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
			createToken("TEXT");
		txtNode.getToken().setText(data);
		txtNode.setParent(parent);
		parent.addChild(txtNode);
	}

	/**
	 * Add a postcode element
	 */
	private void addPostcode(CommonTree parent, String data) {
		CommonTree pcNode = 
			createToken("POSTCODE_START");
		pcNode.setParent(parent);
		parent.addChild(pcNode);
		
		addTextNode(pcNode, data);
	}
	
	/**
	 * Add an error element
	 */
	private void addError(CommonTree parent, String data) {
		CommonTree eNode = 
			createToken("ERROR_START");
		eNode.setParent(parent);
		parent.addChild(eNode);
		
		addTextNode(eNode, data);
	}
	
	/**
	 * Add a happening element
	 */
	private void addHappening(CommonTree parent, String data) {
		CommonTree hNode = 
			createToken("HAPPENING_START");
		hNode.setParent(parent);
		parent.addChild(hNode);
		
		addTextNode(hNode, data);
	}
	
	/**
	 * Add a replacement element
	 */
	private void addReplacement(CommonTree parent, String data) {
		CommonTree rNode = 
			createToken("REPLACEMENT_START");
		rNode.setParent(parent);
		parent.addChild(rNode);
		
		String[] wEles = data.split("\\p{Space}");
		for(String wEle:wEles) {
			CommonTree wNode = 
				createToken("W_START");
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
			createToken("ACTION_START");
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
			createToken("GA_START");
		gaNode.setParent(parent);
		parent.addChild(gaNode);
		
		CommonTree gaTypeNode = 
			createToken("GA_ATTR_TYPE");
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
			createToken("PAUSE_START");
		pNode.setParent(parent);
		parent.addChild(pNode);
		
		if(data.equals(".")) {
			CommonTree slNode = 
				createToken("PAUSE_ATTR_SYMBOLIC_LENGTH");
			slNode.getToken().setText("simple");
			slNode.setParent(pNode);
			pNode.addChild(slNode);
		} else if(data.equals("..")) {
			CommonTree slNode = 
				createToken("PAUSE_ATTR_SYMBOLIC_LENGTH");
			slNode.getToken().setText("long");
			slNode.setParent(pNode);
			pNode.addChild(slNode);
			
		} else if(data.equals("...")) {
			CommonTree slNode = 
				createToken("PAUSE_ATTR_SYMBOLIC_LENGTH");
			slNode.getToken().setText("very long");
			slNode.setParent(pNode);
			pNode.addChild(slNode);
		} else if(data.startsWith("pause:")) {
			String numStr = data.substring(data.indexOf(':')+1);
			CommonTree slNode = 
				createToken("PAUSE_ATTR_LENGTH");
			slNode.getToken().setText(numStr);
			slNode.setParent(pNode);
			pNode.addChild(slNode);
		} else {
			PhonLogger.warning("Invalid pause type '" + data + "'");
		}
		
//		if(data.length() > 0) {
//			CommonTree slNode =
//				createToken("PAUSE_ATTR_SYMBOLIC_LENGTH");
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
			createToken("T_START");
		tNode.setParent(parent);
		parent.addChild(tNode);
		
		CommonTree ttNode = 
			createToken("T_ATTR_TYPE");
		ttNode.getToken().setText(type);
		ttNode.setParent(tNode);
		tNode.addChild(ttNode);
	}
	
	/**
	 * Add a shortenting element
	 */
	private void addShortening(CommonTree parent, String data) {
		CommonTree shNode = 
			createToken("SHORTENING_START");
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
			type = "contrasive stressing";
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
				createToken("K_START");
			markerNode.setParent(parent);
			parent.addChild(markerNode);
			
			CommonTree typeNode = 
				createToken("K_ATTR_TYPE");
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
			createToken("R_START");
		rNode.setParent(parent);
		parent.addChild(rNode);
		
		CommonTree timesNode =
			createToken("R_ATTR_TIMES");
		timesNode.getToken().setText(times);
		timesNode.setParent(rNode);
		rNode.addChild(timesNode);
	}
	
	/**
	 * Add an overlap element
	 */
	private void addOverlap(CommonTree parent, String ovType) {
		CommonTree ovNode = 
			createToken("OVERLAP_START");
		ovNode.setParent(parent);
		parent.addChild(ovNode);
		
		String actualType = "";
		if(ovType.equals(">")) {
			actualType = "overlap follows";
		} else if(ovType.equals("<")) {
			actualType = "overlap precedes";
		}
		CommonTree typeNode = 
			createToken("OVERLAP_ATTR_TYPE");
		typeNode.getToken().setText(actualType);
		typeNode.setParent(ovNode);
		ovNode.addChild(typeNode);
	}
	
	/**
	 * Add a linker element
	 */
	private void addLinker(CommonTree parent, String lkType) {
		CommonTree lkNode = 
			createToken("LINKER_START");
		lkNode.setParent(parent);
		
		if(parent.getChildCount() > 1) {
			
			int sIndex = 0;
			int cIndex = 0;
			CommonTree cNode = (CommonTree)parent.getChild(cIndex++);
			while(cNode != null && (cNode.getToken().getType() == tokens.getTokenType("U_ATTR_WHO")
					|| cNode.getToken().getType() == tokens.getTokenType("LINKER_START")))
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
			createToken("LINKER_ATTR_TYPE");
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
			createToken("LANGS_START");
		langsNode.setParent(parent);
		parent.addChild(langsNode);
		
		CommonTree dataNode = null;
		if(langsType.equals("single")) {
			dataNode = 
			createToken("SINGLE_START");
		} else if(langsType.equals("multiple")) {
			dataNode = 
			createToken("MULTIPLE_START");
		} else if(langsType.equals("ambiguous")) {
			dataNode =
			createToken("AMBIGUOUS_START");
		} else {
			// just make it ambiguous
			dataNode =
			createToken("AMBIGUOUS_START");
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
			createToken("MEDIA_START");
		mediaNode.setParent(parent);
		parent.addChild(mediaNode);
		
		// we need to convert our units to s from ms
		float startS = media.getStartValue() / 1000.0f;
		float endS = media.getEndValue() / 1000.0f;
		
		CommonTree unitNode =
			createToken("MEDIA_ATTR_UNIT");
		unitNode.getToken().setText("s");
		unitNode.setParent(mediaNode);
		mediaNode.addChild(unitNode);
		
		CommonTree startNode = 
			createToken("MEDIA_ATTR_START");
		startNode.getToken().setText(""+startS);
		startNode.setParent(mediaNode);
		mediaNode.addChild(startNode);
		
		CommonTree endNode =
			createToken("MEDIA_ATTR_END");
		endNode.getToken().setText(""+endS);
		endNode.setParent(mediaNode);
		mediaNode.addChild(endNode);
	}
	
	/**
	 * Createa  new common tree node
	 * with the given token type.
	 */
	private CommonTree createToken(String tokenName) {
		return createToken(tokens.getTokenType(tokenName));
	}
	
	private CommonTree createToken(int tokenType) {
		return new CommonTree(new CommonToken(tokenType));
	}
	
	public static void main(String[] args) throws Exception {
		
		IPhonProject project = 
			PhonProject.fromFile("/Users/ghedlund/Desktop/Ella.phon");
		ITranscript t =
			project.getTranscript("Ella", "Ella");
//		IUtterance utt = t.getUtterances().get(69);
		PhonTreeBuilder ptb = new PhonTreeBuilder();
		
		// check the tree for each record
		for(IUtterance utt:t.getUtterances()) {
			CommonTree ct = new CommonTree(
					new CommonToken(tokens.getTokenType("CHAT_START")));
			ptb.setupHeaderData(ct, t);
			ptb.setupParticipants(ct, t);
			ptb.insertRecord(ct, utt);
			
			// try to convert to a string, on fail print our tree
			try {
				CommonTreeNodeStream nodeStream = 
					new CommonTreeNodeStream(ct);
				Phon2XmlWalker walker = new Phon2XmlWalker(nodeStream);
				Phon2XmlWalker.chat_return ret = walker.chat();
				
				ret.st.toString();
			} catch (StackOverflowError err) {
				System.out.println(utt.getID());
//				err.printStackTrace();

//				printTree(ct, 0);
			} catch (IllegalArgumentException e) {
				System.out.println(utt.getID());
//				e.printStackTrace();
//				printTree(ct, 0);
			}
			
			BufferedWriter out;
			try {
				out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("tree.txt"), "UTF-8"));
				
				printTree(out, ct, 0);
				out.flush();
				out.close();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			break;
		}
		
	}
	
	public static void printTree(Writer writer, CommonTree tree, int tIdx) {
		String tabString = "  ";
		
		String out = tokens.getTokenName(tree.getToken().getType()) + ":" + tree.getToken().toString();
		for(int i = 0; i < tIdx; i++) System.out.print(tabString);
		try {
			writer.write("[" + tIdx + "]" + out + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(int cIndex = 0; cIndex < tree.getChildCount(); cIndex++) {
			printTree(writer, (CommonTree)tree.getChild(cIndex), tIdx+1);
		}
	}
}
