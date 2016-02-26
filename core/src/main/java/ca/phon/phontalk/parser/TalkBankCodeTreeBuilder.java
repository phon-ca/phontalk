package ca.phon.phontalk.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.runtime.tree.CommonTree;

/**
 * Builds ANTLR trees for various CHAT codes which
 * have been enclosed inside parenthesis within the
 * Orthography, Markers, Errors, and Repetition tiers
 * of the record. This includes the elements k, error,
 * and r for TalkBank.
 */
public class TalkBankCodeTreeBuilder {

	private final static Logger LOGGER = Logger.getLogger(Phon2XmlTreeBuilder.class.getName());
	
	private static final AntlrTokens chatTokens = new AntlrTokens("Chat.tokens");
	
	/**
	 * Handle data in parenthesis.
	 * 
	 */
	public void handleParentheticData(CommonTree tree, String d) {
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
					addGenericElement(tree, eleName, eleData);
				}
			}
		}
	}
	
	/**
	 * Add a happening element
	 */
	public void addHappening(CommonTree parent, String data) {
		CommonTree hNode = 
			AntlrUtils.createToken(chatTokens, "HAPPENING_START");
		hNode.setParent(parent);
		parent.addChild(hNode);
		
		addTextNode(hNode, data);
	}
	
	/**
	 * Phon: (eleName,attr=val: data)
	 * 
	 * @param parent
	 * @param eleName
	 * @param eleData may be <code>null</code>
	 */
	public void addGenericElement(CommonTree parent, String eleName, String eleData) {
		String[] eleparts = eleName.split(",");
		eleName = eleparts[0].trim();
		
		String tokenName = eleName
				.replaceAll("-", "_")
				.replaceAll("\\p{Space}", "_")
				.toUpperCase();
		String startTokenName = tokenName + "_START";
		CommonTree eleTree = AntlrUtils.createToken(chatTokens, startTokenName);
		eleTree.setParent(parent);
		parent.addChild(eleTree);
		
		for(int i = 1; i < eleparts.length; i++) {
			// setup attributes
			String keyVal[] = eleparts[i].split("=");
			// assign 'type' attribute by default
			String attrName = (keyVal.length == 1 ? keyVal[0] : "type").trim();
			String attrVal = (keyVal.length == 2 ? keyVal[1] : keyVal[0]).trim();
			
			String attrTokenName = tokenName + "_ATTR_" + attrName.toUpperCase();
			CommonTree attrToken = AntlrUtils.createToken(chatTokens, attrTokenName);
			attrToken.getToken().setText(attrVal);
			eleTree.addChild(attrToken);
			attrToken.setParent(eleTree);
		}
		
		if(eleData != null && eleData.trim().length() > 0) {
			addTextNode(eleTree, eleData.trim());
		}
	}
	
	/**
	 * Add an overlap element
	 */
	public void addOverlap(CommonTree parent, String ovdata) {
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
	 * Add an error element
	 */
	public void addError(CommonTree parent, String data) {
		CommonTree eNode = 
			AntlrUtils.createToken(chatTokens, "ERROR_START");
		eNode.setParent(parent);
		parent.addChild(eNode);
		
		addTextNode(eNode, data);
	}
	
	public void addInternalMedia(CommonTree parent, String data) {
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
	public void addAction(CommonTree parent, String data) {
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
	public void addLinker(CommonTree parent, String lkType) {
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
	public void addPause(CommonTree parent, String data) {
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
	public void addMarker(CommonTree parent, String data) {
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
	
	public void addOverlapPoint(CommonTree parent, String data) {
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
	public void addGa(CommonTree parent, String type, String data) {
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
	public void addRepetition(CommonTree parent, String times) {
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
	 * add a text node
	 */
	public void addTextNode(CommonTree parent, String data) {
		CommonTree txtNode = 
			AntlrUtils.createToken(chatTokens, "TEXT");
		txtNode.getToken().setText(data);
		txtNode.setParent(parent);
		parent.addChild(txtNode);
	}
}
