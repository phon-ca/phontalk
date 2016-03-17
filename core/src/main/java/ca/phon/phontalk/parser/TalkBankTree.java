package ca.phon.phontalk.parser;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;

public class TalkBankTree extends CommonTree {
	
	private final static AntlrTokens TOKEN_MAP = new AntlrTokens("Talkbank2AST.tokens");

	public TalkBankTree() {
		super();
	}

	public TalkBankTree(CommonTree node) {
		super(node);
	}

	public TalkBankTree(Token t) {
		super(t);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		if(getToken().getText() != null) {
			sb.append(super.toString());
		} else {
			sb.append(TOKEN_MAP.getTokenName(super.getToken().getType()));
			
			for(int i = 0; i < getChildCount(); i++) {
				if(i == 0) {
					sb.append("{");
				} else if(i == getChildCount()-1) {
					sb.append("}");
				}
				if(i > 0) sb.append(",");
				sb.append(getChild(i).toString());
			}
		}
		
		return sb.toString();
	}

}
