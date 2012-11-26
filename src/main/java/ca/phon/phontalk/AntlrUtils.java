package ca.phon.phontalk;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.tree.CommonTree;

import ca.phon.phontalk.parser.AntlrTokens;

/**
 * Utility methods for working with antlr3 {@link CommonTree}s.
 *
 */
public class AntlrUtils {
	
	/**
	 * Create a new common tree node
	 * with the given token type.
	 */
	public static CommonTree createToken(AntlrTokens tokens, String tokenName) {
		return createToken(tokens.getTokenType(tokenName));
	}
	
	public static CommonTree createToken(int tokenType) {
		return new CommonTree(new CommonToken(tokenType));
	}
	
	/**
	 * add a text node
	 */
	public static void addTextNode(CommonTree parent, AntlrTokens tokens, String data) {
		CommonTree txtNode = 
			AntlrUtils.createToken(tokens, "TEXT");
		txtNode.getToken().setText(data);
		txtNode.setParent(parent);
		parent.addChild(txtNode);
	}
}
