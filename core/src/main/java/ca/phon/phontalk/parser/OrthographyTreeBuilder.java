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
import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Build contents of phonetic group tree from
 * phon Orthography
 *
 */
public class OrthographyTreeBuilder extends VisitorAdapter<OrthoElement> {
	
	private final static Logger LOGGER = Logger.getLogger(OrthographyTreeBuilder.class.getName());

	private final TalkBankCodeTreeBuilder treeBuilder = new TalkBankCodeTreeBuilder();

	private final AntlrTokens talkbankTokens = new AntlrTokens("TalkBank2AST.tokens");

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
		treeBuilder.addWordnet((CommonTree)nodeStack.peek().getChild(nodeStack.peek().getChildCount()-1), wkType);
		attachToLastChild = true;
		visit(wordnet.getWord2());
	}
	
	@Visits
	public void visitWord(OrthoWord word) {
		CommonTree parentNode = nodeStack.peek();

		if(word.getWord().matches("[“”]")) {
			treeBuilder.addQuotation(parentNode, word.getWord());
			return;
		}

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
				wParent.insertChild(0, spTree);
			} else if(suffixType == WordSuffixType.USER_SPECIAL_FORM) {
				CommonTree usfTree =
						AntlrUtils.createToken(talkbankTokens, "W_ATTR_USER_SPECIAL_FORM");
				usfTree.getToken().setText(word.getSuffix().getCode());
				usfTree.setParent(wParent);
				wParent.insertChild(0, usfTree);
			} else {
				CommonTree formTypeNode = 
						AntlrUtils.createToken(talkbankTokens, "W_ATTR_FORMTYPE");
				formTypeNode.getToken().setText(suffixType.getDisplayName());
				formTypeNode.setParent(wParent);
				wParent.insertChild(0, formTypeNode);

				if(word.getSuffix().getFormSuffix() != null) {
					CommonTree formSuffixNode =
							AntlrUtils.createToken(talkbankTokens, "W_ATTR_FORMSUFFIX");
					formSuffixNode.getToken().setText("-" + word.getSuffix().getFormSuffix());
					formSuffixNode.setParent(wParent);
					wParent.insertChild(0, formSuffixNode);
				}


				if(word.getSuffix().getCode() != null) {
					// TODO
				}

			}
		}
		
		if(word.getPrefix() != null) {
			WordPrefixType prefixType = word.getPrefix().getType();
			CommonTree typeNode = 
					AntlrUtils.createToken(talkbankTokens, "W_ATTR_TYPE");
			typeNode.getToken().setText(prefixType.getDisplayName());
			typeNode.setParent(wParent);
			wParent.insertChild(0, typeNode);
		}
		
		if(word.isUntranscribed()) {
			CommonTree utTree =
					AntlrUtils.createToken(talkbankTokens, "W_ATTR_UNTRANSCRIBED");
			utTree.getToken().setText(word.getUntranscribedType().getDisplayName());
			utTree.setParent(wParent);
			wParent.insertChild(0, utTree);
		}
		
		String addWord = word.getWord();
		String val = "";
		boolean inCaDelim = false;
		boolean inUnderline = false;
		char[] charArray = addWord.toCharArray();
		for(int i = 0; i < charArray.length; i++) {
			char c = charArray[i];
			// deal with shortenings
			if(c == '<') {
				if(val.length() > 0)
					treeBuilder.addTextNode(wParent, val);
				val = "";
			} else if(c == '>') {
				if (val.length() > 0)
					treeBuilder.addShortening(wParent, val);
				val = "";
			} else if(c == '“' || c == '”') {
				if(val.length() > 0) {
					treeBuilder.addTextNode(wParent, val);
					val = "";
				}
				treeBuilder.addQuotation(wParent, c+"");
			// underline
			} else if(c == '\u2500') {
				if (val.length() > 0) {
					treeBuilder.addTextNode(wParent, val);
					treeBuilder.addUnderline(wParent, !inUnderline);
				} else {
					treeBuilder.addUnderlineBefore((CommonTree) wParent, !inUnderline);
				}
				inUnderline = !inUnderline;
				val = "";
			// blocking or pause
			} else if(c == '^') {
				if(val.length() > 0) {
					treeBuilder.addTextNode(wParent, val);
					val = "";
				}
				if(i == 0) {
					treeBuilder.addProsody(wParent, "blocking");
				} else {
					treeBuilder.addProsody(wParent, "pause");
				}
			// separator or drawl
			} else if(c == ':') {
				if(val.length() > 0) {
					treeBuilder.addTextNode(wParent, val);
					val = "";
				}
				if(i == 0) {
					treeBuilder.addSeparator(wParent, c+"");
				} else {
					treeBuilder.addProsody(wParent, c+"");
				}
			// separators
			} else if(c == ';' || c == '\u21d7'
				   || c == '\u2197' || c == '\u2192' || c == '\u2198'
				   || c == '\u21d8' || c == '\u221e' || c == '\u2261') {
				if(val.length() > 0) {
					treeBuilder.addTextNode(wParent, val);
					val = "";
				}
				if(i == 0) {
					parentNode.getChildren().remove(parentNode.getChildCount()-1);
				}
				treeBuilder.addSeparator((CommonTree) wParent.getParent(), c+"");
				if(i < charArray.length - 1) {
					// new word node after separator
					wParent =
							AntlrUtils.createToken(talkbankTokens, "W_START");
					wParent.setParent(parentNode);
					parentNode.addChild(wParent);
				}
			// overlap-points (inside words)
			} else if(c == '⌈' || c == '⌊' || c == '⌉' || c == '⌋') {
				if(val.length() > 0) {
					treeBuilder.addTextNode(wParent, val);
					val = "";
				}
				treeBuilder.addOverlapPoint(wParent, c+"");
			// ca-element
			} else if (c == '\u2260' || c == '\u223e' || c == '\u2219'
					|| c == '\u1f29' || c == '\u2193' || c == '\u21bb'
					|| c == '\u2191' || c == '\u02c8' || c == '\u02cc') {
				if(val.length() > 0) {
					treeBuilder.addTextNode(wParent, val);
					val = "";
				}
				treeBuilder.addCaElement(wParent, c + "");
			// ca-delimiter
			} else if(c == '\u264b' || c == '\u204e' || c == '\u2206'
				   || c == '\u2594' || c == '\u25c9' || c == '\u2581'
				   || c == '\u00a7' || c == '\u21ab' || c == '\u222e'
				   || c == '\u2207' || c == '\u263a' || c == '\u00b0'
				   || c == '\u2047' || c == '\u222c' || c == '\u03ab') {
				if(val.length() > 0) {
					treeBuilder.addTextNode(wParent, val);
					val = "";
				}
				treeBuilder.addCaDelimiter(wParent, inCaDelim ? "end" : "begin", c+"");
				inCaDelim = !inCaDelim;
			} else {
				val += c;
			}
		}
		if(val.length() > 0)
			treeBuilder.addTextNode(wParent, val);

		if(word.getSuffix() != null && word.getSuffix().getPos() != null) {
			treeBuilder.addPos(wParent, word.getSuffix().getPos());
		}
	}

	@Visits
	public void visitEvent(OrthoEvent event) {
		CommonTree parentNode = nodeStack.peek();
		insertEvent(parentNode, (event.getType() != null ? event.getType() + ":" : "") + event.getData());
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
			final Pattern osePattern = Pattern.compile("(\\w+)=(.+)");
			final Matcher oseMatcher = osePattern.matcher(evtData);
			
			if(oseMatcher.matches()) {
				final String participantId = oseMatcher.group(1);
				final String oseWord = oseMatcher.group(2);
				treeBuilder.addOtherSpokenEvent(eNode, participantId, oseWord);
			} else {
				// everything is a 'happening'
				if(evtData.startsWith("=")) {
					// insert happening
					final String htxt = evtData.substring(1);
					chatCodeBuilder.addHappening(eNode, htxt);
				} else {
					chatCodeBuilder.addHappening(eNode, evtData.trim());
				}
			}
		}
		
		for(String subEle:subData) {
			chatCodeBuilder.handleParentheticData(eNode, subEle);
		}
	}

	@Visits
	public void visitComment(OrthoComment comment) {
		TalkBankCodeTreeBuilder chatCodeBuilder = new TalkBankCodeTreeBuilder();
		String commentText = comment.toString();

		// COMMENTS - including CHAT coding
		Pattern commentPattern = Pattern.compile("\\(([^:]+):(.*)\\)");
		Matcher m = commentPattern.matcher(commentText);
		if(m.matches()) {
			String type = m.group(1);
			String text = m.group(2);
			
			// some tags need special handling...
			// TERMINATOR
			if (type.equals("t")) {
				terminator = treeBuilder.addTerminator(uttNodeStack.get(0), text);
			} else if(type.equals("replacement") || type.equals("replacement-real")) {

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
				treeBuilder.addReplacement(wNode, type.endsWith("-real"), text);
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
				treeBuilder.addLangs(wNode, text);
			} else {
				chatCodeBuilder.handleParentheticData(nodeStack.peek(), commentText);
			}
		} else {
			final CommonTree tree = chatCodeBuilder.handleParentheticData(nodeStack.peek(), commentText);
			if(tree.getToken().getType() == talkbankTokens.getTokenType("T_START")) {
				terminator = tree;
			}
		}
	}

	@Visits
	public void visitPunct(OrthoPunct punct) {
		switch(punct.getType()) {
		
		case COMMA:
			treeBuilder.addTagMarker(nodeStack.peek(), "comma");
			break;
		
		case DOUBLE_DAGGER:
			treeBuilder.addTagMarker(nodeStack.peek(), "tag");
			break;
			
		case DOUBLE_COMMA:
			treeBuilder.addTagMarker(nodeStack.peek(), "vocative");
			break;
			
		case EXCLAMATION:
			terminator = treeBuilder.addTerminator(uttNodeStack.get(0), "e");
			break;
			
		case QUESTION:
			terminator = treeBuilder.addTerminator(uttNodeStack.get(0), "q");
			break;
			
		case PERIOD:
			terminator = treeBuilder.addTerminator(uttNodeStack.get(0), "p");
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
	
	public CommonTree getTerminator() {
		return this.terminator;
	}

}
