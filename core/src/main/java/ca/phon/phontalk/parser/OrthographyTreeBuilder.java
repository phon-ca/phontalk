package ca.phon.phontalk.parser;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

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
import ca.phon.orthography.WordPrefixType;
import ca.phon.orthography.WordSuffixType;
import ca.phon.visitor.VisitorAdapter;
import ca.phon.visitor.annotation.Visits;

/**
 * Build contents of phonetic group tree from
 * phon Orthography
 *
 */
public class OrthographyTreeBuilder extends VisitorAdapter<OrthoElement> {
	
	private final static Logger LOGGER = Logger.getLogger(OrthographyTreeBuilder.class.getName());
	
	private static final AntlrTokens talkbankTokens = new AntlrTokens("TalkBank2AST.tokens");

	private Stack<CommonTree> uttNodeStack = new Stack<CommonTree>();
	
	private Stack<CommonTree> nodeStack = new Stack<CommonTree>();
	
	private boolean attachToLastChild = false;
	
	private Orthography ortho;
	
	private CommonTree terminator;
	
	public void buildTree(Stack<CommonTree> uttNodeStack, CommonTree parent, Orthography ortho) {
		this.uttNodeStack = uttNodeStack;
		nodeStack.push(uttNodeStack.get(0));
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
	public void visitWordnet(OrthoWordnet wordnet) {
		visit(wordnet.getWord1());
		String wkType = 
				(wordnet.getMarker() == OrthoWordnetMarker.COMPOUND ? "cmp" : "cli");
		insertWordnet((CommonTree)nodeStack.peek().getChild(nodeStack.peek().getChildCount()-1), wkType);
		attachToLastChild = true;
		visit(wordnet.getWord2());
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
				AntlrUtils.createToken(talkbankTokens, "W_START");
			wParent.setParent(parentNode);
			parentNode.addChild(wParent);
		}
		
		if(word.getSuffix() != null) {
			WordSuffixType suffixType = word.getSuffix().getType();
			if(suffixType == WordSuffixType.SEPARATED_PREFIX) {
				CommonTree spTree =
						AntlrUtils.createToken(talkbankTokens, "W_ATTR_SEPARATED_PREFIX");
				spTree.getToken().setText("true");
				spTree.setParent(wParent);
				wParent.addChild(spTree);
			} else if(suffixType == WordSuffixType.USER_SPECIAL_FORM) {
				CommonTree usfTree =
						AntlrUtils.createToken(talkbankTokens, "W_ATTR_USER_SPECIAL_FORM");
				usfTree.getToken().setText(word.getSuffix().getCode());
				usfTree.setParent(wParent);
				wParent.addChild(usfTree);
			} else {
				CommonTree formTypeNode = 
						AntlrUtils.createToken(talkbankTokens, "W_ATTR_FORMTYPE");
				formTypeNode.getToken().setText(suffixType.getDisplayName());
				formTypeNode.setParent(wParent);
				wParent.addChild(formTypeNode);
			}
		}
		
		if(word.getPrefix() != null) {
			WordPrefixType prefixType = word.getPrefix().getType();
			CommonTree typeNode = 
					AntlrUtils.createToken(talkbankTokens, "W_ATTR_TYPE");
			typeNode.getToken().setText(prefixType.getDisplayName());
			typeNode.setParent(wParent);
			wParent.addChild(typeNode);
		}
		
		if(word.isUntranscribed()) {
			CommonTree utTree =
					AntlrUtils.createToken(talkbankTokens, "W_ATTR_UNTRANSCRIBED");
			utTree.getToken().setText(word.getUntranscribedType().getDisplayName());
			utTree.setParent(wParent);
			wParent.addChild(utTree);
		}
		
		String addWord = word.getWord();
		String val = "";
		for(char c:addWord.toCharArray()) {
			// deal with shortenings
			if(c == '<') {
				if(val.length() > 0)
					addTextNode(wParent, val);
				val = "";
			} else if(c == '>') {
				if(val.length() > 0)
					addShortening(wParent, val);
				val = "";
			} else {
				val += c;
			}
		}
		addTextNode(wParent, val);
	}
	
	/**
	 * Add a shortenting element
	 */
	private void addShortening(CommonTree parent, String data) {
		CommonTree shNode = 
			AntlrUtils.createToken(talkbankTokens, "SHORTENING_START");
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
		
		TalkBankCodeTreeBuilder chatCodeBuilder = new TalkBankCodeTreeBuilder();
		
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
				AntlrUtils.createToken(talkbankTokens, "E_START");
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
					chatCodeBuilder.addHappening(eNode, htxt);
				} else {
					chatCodeBuilder.addHappening(eNode, evtData);
				}
			}
		}
		
		for(String subEle:subData) {
			chatCodeBuilder.handleParentheticData(eNode, subEle);
		}
	}
	
	
	
	
	
	
	
	/**
	 * Add other spoken event
	 * 
	 */
	private void addOtherSpokenEvent(CommonTree parent, String speaker, String data) {
		CommonTree oseNode =
				AntlrUtils.createToken(talkbankTokens, "OTHERSPOKENEVENT_START");
		oseNode.setParent(parent);
		parent.addChild(oseNode);
		
		CommonTree whoNode =
				AntlrUtils.createToken(talkbankTokens, "OTHERSPOKENEVENT_ATTR_WHO");
		whoNode.getToken().setText(speaker);
		whoNode.setParent(oseNode);
		oseNode.addChild(whoNode);
		
		CommonTree saidNode =
				AntlrUtils.createToken(talkbankTokens, "OTHERSPOKENEVENT_ATTR_SAID");
		saidNode.getToken().setText(data);
		saidNode.setParent(oseNode);
		oseNode.addChild(saidNode);
	}
	
	/**
	 * add a text node
	 */
	private void addTextNode(CommonTree parent, String data) {
		CommonTree txtNode = 
			AntlrUtils.createToken(talkbankTokens, "TEXT");
		txtNode.getToken().setText(data);
		txtNode.setParent(parent);
		parent.addChild(txtNode);
	}
	
	@Visits
	public void visitComment(OrthoComment comment) {
		TalkBankCodeTreeBuilder chatCodeBuilder = new TalkBankCodeTreeBuilder();
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
				addTerminator(uttNodeStack.get(0), data);
			} else if(type.equals("replacement")) { 

				CommonTree parentNode = nodeStack.peek();
				// find the last 'w' node
				int wTokenType = talkbankTokens.getTokenType("W_START");
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
					wNode = AntlrUtils.createToken(talkbankTokens, "W_START");
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
					wNode = AntlrUtils.createToken(talkbankTokens, "W_START");
					wNode.setParent(parentNode);
					parentNode.addChild(wNode);
					attachToLastChild = true;
				}
				addLangs(wNode, data);
			} else {
				chatCodeBuilder.handleParentheticData(nodeStack.peek(), w);
			}
		} else {
			chatCodeBuilder.handleParentheticData(nodeStack.peek(), w);
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
			AntlrUtils.createToken(talkbankTokens, "LANGS_START");
		langsNode.setParent(parent);
		parent.addChild(langsNode);
		
		CommonTree dataNode = null;
		if(langsType.equals("single")) {
			dataNode = 
			AntlrUtils.createToken(talkbankTokens, "SINGLE_START");
		} else if(langsType.equals("multiple")) {
			dataNode = 
			AntlrUtils.createToken(talkbankTokens, "MULTIPLE_START");
		} else if(langsType.equals("ambiguous")) {
			dataNode =
			AntlrUtils.createToken(talkbankTokens, "AMBIGUOUS_START");
		} else {
			// just make it ambiguous
			dataNode =
			AntlrUtils.createToken(talkbankTokens, "AMBIGUOUS_START");
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
			AntlrUtils.createToken(talkbankTokens, "REPLACEMENT_START");
		rNode.setParent(parent);
		parent.addChild(rNode);
		
		try {
			OrthographyTreeBuilder innerBuilder = new OrthographyTreeBuilder();
			Orthography replacementOrtho = Orthography.parseOrthography(data);
			CommonTree fakeU = AntlrUtils.createToken(talkbankTokens, "U_START");
			Stack<CommonTree> fakeUStack = new Stack<>();
			fakeUStack.push(fakeU);
			innerBuilder.buildTree(fakeUStack, rNode, replacementOrtho);
		} catch (ParseException e) {
			LOGGER.warning(e.getLocalizedMessage());
		}
		
		
//		String[] wEles = data.split("\\p{Space}");
//		for(String wEle:wEles) {
//			CommonTree wNode = 
//				AntlrUtils.createToken(talkbankTokens, "W_START");
//			wNode.setParent(rNode);
//			rNode.addChild(wNode);
//			
//			addTextNode(wNode, wEle);
//		}
	}
	
	
	
	/**
	 * wk
	 */
	private void insertWordnet(CommonTree parent, String type) {
		CommonTree wkNode =
			AntlrUtils.createToken(talkbankTokens, "WK_START");
		wkNode.setParent(parent);
		
		CommonTree typeNode =
			AntlrUtils.createToken(talkbankTokens, "WK_ATTR_TYPE");
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
			addTerminator(uttNodeStack.get(0), "e");
			break;
			
		case QUESTION:
			addTerminator(uttNodeStack.get(0), "q");
			break;
			
		case PERIOD:
			addTerminator(uttNodeStack.get(0), "p");
			break;
			
		case OPEN_BRACE:
			long numPunctAndWords =
				StreamSupport.stream(ortho.spliterator(), true)
					.filter( (ele) -> (ele instanceof OrthoPunct) || (ele instanceof OrthoWord) )
					.count();
			if(numPunctAndWords == 1) {
				// create a super-<g> node
				CommonTree superG = 
						new CommonTree(new CommonToken(talkbankTokens.getTokenType("G_START")));
				superG.setParent(uttNodeStack.peek());
				uttNodeStack.peek().addChild(superG);
				
				uttNodeStack.push(superG);
			} else {
				CommonTree parentNode = nodeStack.peek();
				if(attachToLastChild) {
					parentNode = 
						(CommonTree)parentNode.getChild(parentNode.getChildCount()-1);
					attachToLastChild = false;
				}
				CommonTree gNode = 
					AntlrUtils.createToken(talkbankTokens, "G_START");
				gNode.setParent(parentNode);
				parentNode.addChild(gNode);
				
				// push new group onto stack
				nodeStack.push(gNode);
			}
			break;
			
		case CLOSE_BRACE:
			numPunctAndWords =
				StreamSupport.stream(ortho.spliterator(), true)
					.filter( (ele) -> (ele instanceof OrthoPunct) || (ele instanceof OrthoWord) )
					.count();
			if(numPunctAndWords == 1) {
				// pop the group from the uttstack
	    		uttNodeStack.pop();
	    		nodeStack.pop();
			} else {
				nodeStack.pop();
			}
			break;
			
		default:
		};
	}
	
	private void addTagMarker(CommonTree parentNode, String type) {
    	CommonTree tgTree = AntlrUtils.createToken(talkbankTokens, "TAGMARKER_START");
    	
    	CommonTree tgTypeTree = AntlrUtils.createToken(talkbankTokens, "TAGMARKER_ATTR_TYPE");
    	tgTypeTree.getToken().setText(type);
    	tgTree.addChild(tgTypeTree);
    	tgTypeTree.setParent(tgTree);
    	
    	parentNode.addChild(tgTree);
    	tgTree.setParent(parentNode);
	}
	
	public CommonTree getTerminator() {
		return this.terminator;
	}
	
	/**
	 * Add a terminator
	 */
	public void addTerminator(CommonTree parent, String type) {
		CommonTree tNode =
			AntlrUtils.createToken(talkbankTokens, "T_START");
		tNode.setParent(parent);
		parent.addChild(tNode);
		
		CommonTree ttNode = 
			AntlrUtils.createToken(talkbankTokens, "T_ATTR_TYPE");
		ttNode.getToken().setText(type);
		ttNode.setParent(tNode);
		tNode.addChild(ttNode);
		
		terminator = tNode;
	}
	
}
