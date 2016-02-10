package ca.phon.phontalk.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.tree.CommonTree;

import ca.phon.orthography.OrthoComment;
import ca.phon.orthography.OrthoElement;
import ca.phon.orthography.OrthoEvent;
import ca.phon.orthography.OrthoPunct;
import ca.phon.orthography.OrthoWord;
import ca.phon.orthography.OrthoWordnet;
import ca.phon.orthography.OrthoWordnetMarker;
import ca.phon.orthography.Orthography;
import ca.phon.visitor.VisitorAdapter;
import ca.phon.visitor.annotation.Visits;

/**
 * Build contents of phonetic group tree from
 * phon Orthography
 *
 */
public class OrthographyTreeBuilder extends VisitorAdapter<OrthoElement> {
	
	private final static Logger LOGGER = Logger.getLogger(OrthographyTreeBuilder.class.getName());
	
	private static final AntlrTokens chatTokens = new AntlrTokens("Chat.tokens");

	private Stack<CommonTree> uttNodeStack = new Stack<CommonTree>();
	
	private Stack<CommonTree> nodeStack = new Stack<CommonTree>();
	
	private boolean attachToLastChild = false;
	
	private Orthography ortho;
	
	private CommonTree uttNode;
	
	public void buildTree(CommonTree uttNode, CommonTree parent, Orthography ortho) {
		uttNodeStack.push(uttNode);
		nodeStack.push(uttNode);
		if(uttNode != parent && parent != null)
			nodeStack.push(parent);
		attachToLastChild = false;
		this.ortho = ortho;
		ortho.accept(this);
	}
	
	@Override
	public void fallbackVisit(OrthoElement obj) {
		LOGGER.severe("Unknown element type " + obj.getClass() + " for " + obj.text());
	}
	
	@Visits
	public void visitWord(OrthoWord word) {
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
		
		if(word.getSuffix() != null) {
			String fullType = 
					word.getSuffix().getDisplayName();
			
			CommonTree formTypeNode = 
					AntlrUtils.createToken(chatTokens, "W_ATTR_FORMTYPE");
			formTypeNode.getToken().setText(fullType);
			formTypeNode.setParent(wParent);
			wParent.addChild(formTypeNode);
		}
		
		if(word.getPrefix() != null) {
			String wType = 
					word.getPrefix().getDisplayName();
			
			CommonTree typeNode = 
					AntlrUtils.createToken(chatTokens, "W_ATTR_TYPE");
			typeNode.getToken().setText(wType);
			typeNode.setParent(wParent);
			wParent.addChild(typeNode);
		}
		
		String addWord = word.getWord();
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
	
	@Visits
	public void visitEvent(OrthoEvent event) {
		CommonTree parentNode = nodeStack.peek();
		insertEvent(parentNode, event.getData());
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
			addGa(tree, "actions", data.substring(5).trim());
		} else if(data.startsWith("=?")) {
			addGa(tree, "alternative", data.substring(2).trim());
		} else if(data.startsWith("%")) {
			addGa(tree, "comments", data.substring(1).trim());
		}  else if(data.startsWith("=!")) {
			addGa(tree, "paralinguistics", data.substring(2).trim());
		} else if(data.startsWith("=")) {
			addGa(tree, "explanation", data.substring(1).trim());
		} else if(data.startsWith("%sdi:")) {
			addGa(tree, "standard for dialect", data.substring(5).trim());
		} else if(data.startsWith("%sch:")) {
			addGa(tree, "standard for child", data.substring(5).trim());
		} else if(data.startsWith("%xxx:")) {
			addGa(tree, "standard for unclear source", data.substring(5).trim());
		}
		
		// repeats
		else if(data.matches("x\\p{Space}?[0-9]+")) {
			addRepetition(tree, data.substring(1).trim());
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
					LOGGER.warning("Unsupported data " + data);
				}
			}
		}
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
	 * Add an error element
	 */
	private void addError(CommonTree parent, String data) {
		CommonTree eNode = 
			AntlrUtils.createToken(chatTokens, "ERROR_START");
		eNode.setParent(parent);
		parent.addChild(eNode);
		
		addTextNode(eNode, data);
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
			LOGGER.warning("Invalid pause type '" + data + "'");
		}
		
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
			LOGGER.warning("Invalid marker type '" + data + "'");
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
	 * add a text node
	 */
	private void addTextNode(CommonTree parent, String data) {
		CommonTree txtNode = 
			AntlrUtils.createToken(chatTokens, "TEXT");
		txtNode.getToken().setText(data);
		txtNode.setParent(parent);
		parent.addChild(txtNode);
	}
	
	@Visits
	public void visitComment(OrthoComment comment) {
		String w = comment.toString();
		// COMMENTS - including CHAT coding
		Pattern commentPattern = Pattern.compile("\\(([^:]+):(.*)\\)");
		Matcher m = commentPattern.matcher(w);
		if(m.matches()) {
			String type = m.group(1);
			String data = m.group(2);
			
			// some tags need special handling...
			// TERMINATOR
			if (type.equals("t")) { 
				addTerminator(uttNode, data);
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
//				int wTokenType = tokens.getTokenType("W_START");
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
	
	@Visits
	public void visitWordnet(OrthoWordnet wordnet) {
		visitWord(wordnet.getWord1());
		String wkType = 
				(wordnet.getMarker() == OrthoWordnetMarker.COMPOUND ? "cmp" : "cli");
		insertWordnet(nodeStack.peek(), wkType);
		visitWord(wordnet.getWord2());
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
	
	@Visits
	public void visitPunct(OrthoPunct punct) {
		switch(punct.getType()) {
		
		case COMMA:
			addTagMarker(nodeStack.peek(), "comma");
			break;
		
		case DOUBLE_DAGGER:
			addTagMarker(nodeStack.peek(), "tag");
			break;
			
		case DOUBLE_COMMA:
			addTagMarker(nodeStack.peek(), "vocative");
			break;
			
		case EXCLAMATION:
			addTerminator(uttNode, "e");
			break;
			
		case QUESTION:
			addTerminator(uttNode, "q");
			break;
			
		case PERIOD:
			addTerminator(uttNode, "p");
			break;
			
		case OPEN_BRACE:
			if(ortho.length() == 1) {
				// create a super-<g> node
				CommonTree superG = 
						new CommonTree(new CommonToken(chatTokens.getTokenType("G_START")));
				superG.setParent(uttNode);
				uttNode.addChild(superG);
				uttNode = superG;
				
				uttNodeStack.push(superG);
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
			break;
			
		case CLOSE_BRACE:
			if(ortho.length() == 1) {
				// pop the group from the uttstack
	    		uttNodeStack.pop();
	    		uttNode = uttNodeStack.peek();
			}
			nodeStack.pop();
			break;
			
		default:
		};
	}
	
	private void addTagMarker(CommonTree parentNode, String type) {
    	CommonTree tgTree = AntlrUtils.createToken(chatTokens, "TAGMARKER_START");
    	
    	CommonTree tgTypeTree = AntlrUtils.createToken(chatTokens, "TAGMARKER_ATTR_TYPE");
    	tgTypeTree.getToken().setText(type);
    	tgTree.addChild(tgTypeTree);
    	tgTypeTree.setParent(tgTree);
    	
    	parentNode.addChild(tgTree);
    	tgTree.setParent(parentNode);
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
